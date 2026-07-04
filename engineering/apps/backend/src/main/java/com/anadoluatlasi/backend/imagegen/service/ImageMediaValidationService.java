package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Set;

/**
 * FP-008: In-memory image byte validation service.
 *
 * <p>Kendisine verilen byte array üzerinde MIME, boyut, dimension
 * ve checksum validation yapar. Provider URL download, storage
 * upload/download, preview veya review workflow ile ilişkisi yoktur.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Provider URL ile ilişkisi yoktur — download bu service'in sorumluluğu değildir.</li>
 *   <li>Storage upload/read ile ilişkisi yoktur.</li>
 *   <li>Raw binary log'a yazılmaz.</li>
 *   <li>Error message safe normalized olarak döner.</li>
 *   <li>Validation sonucu approval kararı değildir.</li>
 * </ul>
 */
@Service
public class ImageMediaValidationService {

    private static final Logger log = LoggerFactory.getLogger(ImageMediaValidationService.class);

    // ── Configuration Constants ─────────────────────────────────────

    /** Allowed MIME types — sadece image/png, image/jpeg, image/webp */
    static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp"
    );

    /** Max file size — 10 MB */
    static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    // ── PNG/JPEG magic bytes ────────────────────────────────────────

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] WEBP_RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_WEBP = {0x57, 0x45, 0x42, 0x50};

    // ── Public API ──────────────────────────────────────────────────

    /**
     * Verilen byte array'i image olarak doğrular.
     *
     * @param mediaBytes     image binary content
     * @param correlationId  opsiyonel trace identifier
     * @return validation sonucu — safe metadata only
     */
    public ImageMediaValidationResult validate(byte[] mediaBytes, String correlationId) {

        // 1. Null check
        if (mediaBytes == null) {
            return failResult(ImageStorageErrorCode.MEDIA_VALIDATION_FAILED,
                    "Doğrulanacak medya verisi bulunamadı", correlationId);
        }

        // 2. Zero-byte check
        if (mediaBytes.length == 0) {
            return failResult(ImageStorageErrorCode.MEDIA_VALIDATION_FAILED,
                    "Boş dosya reddedildi", correlationId);
        }

        // 3. File size check
        if (mediaBytes.length > MAX_FILE_SIZE_BYTES) {
            return failResult(ImageStorageErrorCode.MEDIA_TOO_LARGE,
                    "Dosya boyutu izin verilen limiti aşıyor", correlationId);
        }

        // 4. MIME detection via magic bytes
        String detectedMime = detectMimeType(mediaBytes);
        if (detectedMime == null || !ALLOWED_MIME_TYPES.contains(detectedMime)) {
            return failResult(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE,
                    "Desteklenmeyen medya türü", correlationId);
        }

        // 5. Image dimension extraction + corrupt check
        int width = 0;
        int height = 0;

        // WebP dimension extraction via header (ImageIO may not support webp)
        if ("image/webp".equals(detectedMime)) {
            int[] dims = extractWebpDimensions(mediaBytes);
            if (dims == null) {
                return failResult(ImageStorageErrorCode.MEDIA_DIMENSION_INVALID,
                        "Image boyutları okunamadı", correlationId);
            }
            width = dims[0];
            height = dims[1];
        } else {
            // PNG/JPEG — use ImageIO
            try (ByteArrayInputStream bis = new ByteArrayInputStream(mediaBytes)) {
                BufferedImage img = ImageIO.read(bis);
                if (img == null) {
                    return failResult(ImageStorageErrorCode.MEDIA_DIMENSION_INVALID,
                            "Bozuk veya okunamayan image dosyası", correlationId);
                }
                width = img.getWidth();
                height = img.getHeight();
            } catch (IOException e) {
                log.warn("FP-008 image dimension extraction failed, correlationId={}", correlationId);
                return failResult(ImageStorageErrorCode.MEDIA_DIMENSION_INVALID,
                        "Image boyutları okunamadı", correlationId);
            }
        }

        if (width <= 0 || height <= 0) {
            return failResult(ImageStorageErrorCode.MEDIA_DIMENSION_INVALID,
                    "Geçersiz image boyutları", correlationId);
        }

        // 6. Checksum calculation
        String checksum;
        try {
            checksum = calculateSha256(mediaBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("FP-008 SHA-256 algorithm not available, correlationId={}", correlationId);
            return failResult(ImageStorageErrorCode.CHECKSUM_CALCULATION_FAILED,
                    "Checksum hesaplanamadı", correlationId);
        }

        // 7. Success
        log.info("FP-008 media validation passed: mimeType={}, size={}, {}x{}, correlationId={}",
                detectedMime, mediaBytes.length, width, height, correlationId);

        return ImageMediaValidationResult.builder()
                .valid(true)
                .detectedMimeType(detectedMime)
                .fileSizeBytes(mediaBytes.length)
                .width(width)
                .height(height)
                .checksumSha256(checksum)
                .errorCode(null)
                .errorMessage(null)
                .validatedAt(LocalDateTime.now())
                .correlationId(correlationId)
                .build();
    }

    // ── MIME Detection ──────────────────────────────────────────────

    /**
     * Magic bytes ile actual MIME type tespit eder.
     * Content-Type header'a güvenmez.
     */
    String detectMimeType(byte[] data) {
        if (data == null || data.length < 12) {
            return null;
        }

        if (startsWith(data, PNG_MAGIC)) {
            return "image/png";
        }

        if (startsWith(data, JPEG_MAGIC)) {
            return "image/jpeg";
        }

        if (startsWith(data, WEBP_RIFF) && regionMatches(data, 8, WEBP_WEBP)) {
            return "image/webp";
        }

        return null;
    }

    // ── WebP Dimension Extraction ───────────────────────────────────

    /**
     * WebP VP8 header'dan width/height okur.
     * ImageIO webp desteklemeyebilir — header-level extraction.
     *
     * @return [width, height] or null if unreadable
     */
    int[] extractWebpDimensions(byte[] data) {
        // Minimum VP8 header: RIFF(4) + size(4) + WEBP(4) + VP8 (4) + chunk(4) + ...
        if (data.length < 30) {
            return null;
        }

        // VP8 lossy: bytes 12-15 = "VP8 "
        if (data[12] == 'V' && data[13] == 'P' && data[14] == '8' && data[15] == ' ') {
            // VP8 bitstream starts at offset 20 (after chunk size at 16-19)
            // Frame tag at offset 23-25 (3 bytes), then width/height
            if (data.length < 30) return null;
            // Check VP8 frame sync code: 0x9D 0x01 0x2A
            if (data[23] == (byte) 0x9D && data[24] == (byte) 0x01 && data[25] == (byte) 0x2A) {
                int w = (data[26] & 0xFF) | ((data[27] & 0x3F) << 8);
                int h = (data[28] & 0xFF) | ((data[29] & 0x3F) << 8);
                return (w > 0 && h > 0) ? new int[]{w, h} : null;
            }
        }

        // VP8L lossless: bytes 12-15 = "VP8L"
        if (data[12] == 'V' && data[13] == 'P' && data[14] == '8' && data[15] == 'L') {
            if (data.length < 25) return null;
            // Signature byte at 20, then 4 bytes of packed width/height at 21
            int bits = (data[21] & 0xFF)
                    | ((data[22] & 0xFF) << 8)
                    | ((data[23] & 0xFF) << 16)
                    | ((data[24] & 0xFF) << 24);
            int w = (bits & 0x3FFF) + 1;
            int h = ((bits >> 14) & 0x3FFF) + 1;
            return (w > 0 && h > 0) ? new int[]{w, h} : null;
        }

        // VP8X extended: bytes 12-15 = "VP8X"
        if (data[12] == 'V' && data[13] == 'P' && data[14] == '8' && data[15] == 'X') {
            if (data.length < 30) return null;
            // Canvas width at bytes 24-26 (little-endian, 24-bit) + 1
            // Canvas height at bytes 27-29 (little-endian, 24-bit) + 1
            int w = ((data[24] & 0xFF) | ((data[25] & 0xFF) << 8) | ((data[26] & 0xFF) << 16)) + 1;
            int h = ((data[27] & 0xFF) | ((data[28] & 0xFF) << 8) | ((data[29] & 0xFF) << 16)) + 1;
            return (w > 0 && h > 0) ? new int[]{w, h} : null;
        }

        return null;
    }

    // ── Checksum ────────────────────────────────────────────────────

    /**
     * SHA-256 hash hesaplar, hex string döner.
     */
    static String calculateSha256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return HexFormat.of().formatHex(hash);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private ImageMediaValidationResult failResult(ImageStorageErrorCode errorCode,
                                                   String safeMessage,
                                                   String correlationId) {
        log.warn("FP-008 media validation failed: errorCode={}, correlationId={}", errorCode, correlationId);
        return ImageMediaValidationResult.builder()
                .valid(false)
                .detectedMimeType(null)
                .fileSizeBytes(0)
                .width(0)
                .height(0)
                .checksumSha256(null)
                .errorCode(errorCode)
                .errorMessage(safeMessage)
                .validatedAt(LocalDateTime.now())
                .correlationId(correlationId)
                .build();
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private boolean regionMatches(byte[] data, int offset, byte[] pattern) {
        if (data.length < offset + pattern.length) return false;
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) return false;
        }
        return true;
    }
}
