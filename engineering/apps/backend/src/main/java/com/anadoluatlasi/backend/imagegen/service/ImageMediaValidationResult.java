package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * FP-008: Media validation sonuç modeli.
 *
 * <p>In-memory image byte validation sonuçlarını taşır.
 * Raw binary, base64, provider URL, storage object key,
 * signed/public URL bilgileri bu modelde yer almaz.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Raw binary / base64 alanı yoktur.</li>
 *   <li>Provider URL alanı yoktur.</li>
 *   <li>Storage object key alanı yoktur.</li>
 *   <li>Signed / public URL alanı yoktur.</li>
 *   <li>Raw exception / stack trace alanı yoktur.</li>
 * </ul>
 */
@Data
@Builder
public class ImageMediaValidationResult {

    /** Validation başarılı mı */
    private final boolean valid;

    /** Tespit edilen gerçek MIME type (content sniffing) */
    private final String detectedMimeType;

    /** Dosya boyutu (bytes) */
    private final long fileSizeBytes;

    /** Image genişliği (pixel) — 0 if unreadable */
    private final int width;

    /** Image yüksekliği (pixel) — 0 if unreadable */
    private final int height;

    /** SHA-256 checksum (hex string) — null if calculation failed */
    private final String checksumSha256;

    /** Normalized error code — null if valid */
    private final ImageStorageErrorCode errorCode;

    /** Safe normalized error message — null if valid */
    private final String errorMessage;

    /** Validation zaman damgası */
    private final LocalDateTime validatedAt;

    /** End-to-end trace identifier — optional */
    private final String correlationId;
}
