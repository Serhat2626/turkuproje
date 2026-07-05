package com.anadoluatlasi.backend.imagegen.storage;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FP-008: In-memory mock storage adapter.
 *
 * <p>External storage provider (S3/MinIO/GCS) kullanmaz. Validated image
 * byte'larını ConcurrentHashMap'te tutar. Test ve development amacıyla
 * kullanılır.</p>
 *
 * <p>Storage object key server-side üretilir — UUID-based, deterministic.
 * Admin/user-facing DTO'ya çıkmaz.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Public URL üretilmez.</li>
 *   <li>Signed URL üretilmez.</li>
 *   <li>CDN URL üretilmez.</li>
 *   <li>Base64 döndürülmez.</li>
 *   <li>External HTTP çağrısı yapılmaz.</li>
 *   <li>Storage object key loglanmaz.</li>
 * </ul>
 *
 * <p>FP-010 Task 03: Now uses @ConditionalOnMissingBean — active only when
 * S3ImageStorageAdapter is not enabled.</p>
 */
@Component
@ConditionalOnMissingBean(S3ImageStorageAdapter.class)
public class InMemoryImageStorageAdapter implements ImageStorageAdapter {

    private static final Logger log = LoggerFactory.getLogger(InMemoryImageStorageAdapter.class);
    private static final String PROVIDER_ALIAS = "mock";

    /**
     * In-memory storage — key: storageObjectKey, value: stored bytes.
     * Package-private for test visibility.
     */
    final Map<String, StoredObject> store = new ConcurrentHashMap<>();

    @Override
    public ImageStorageWriteResult write(ImageStorageWriteRequest request) {
        String correlationId = request != null ? request.getCorrelationId() : null;

        // 1. Null/empty validation
        if (request == null || request.getImageBytes() == null || request.getImageBytes().length == 0) {
            log.warn("FP-008 mock storage write failed: empty input, correlationId={}", correlationId);
            return writeFailure(ImageStorageErrorCode.STORAGE_WRITE_FAILED,
                    "Storage yazma başarısız — geçersiz girdi", correlationId);
        }

        try {
            // 2. Generate server-side storage object key
            String objectKey = generateObjectKey(request);

            // 3. Store in memory
            StoredObject obj = new StoredObject(
                    request.getImageBytes(),
                    request.getMimeType(),
                    request.getWidth(),
                    request.getHeight(),
                    request.getFileSizeBytes(),
                    request.getChecksumSha256()
            );
            store.put(objectKey, obj);

            log.info("FP-008 mock storage write success: mediaAssetId={}, size={}, correlationId={}",
                    request.getMediaAssetId(), request.getFileSizeBytes(), correlationId);

            // 4. Return success result
            return ImageStorageWriteResult.builder()
                    .success(true)
                    .storageProviderAlias(PROVIDER_ALIAS)
                    .storageObjectKey(objectKey)
                    .mimeType(request.getMimeType())
                    .width(request.getWidth())
                    .height(request.getHeight())
                    .fileSizeBytes(request.getFileSizeBytes())
                    .checksumSha256(request.getChecksumSha256())
                    .storedAt(LocalDateTime.now())
                    .errorCode(null)
                    .errorMessage(null)
                    .correlationId(correlationId)
                    .build();

        } catch (Exception e) {
            log.warn("FP-008 mock storage write unexpected failure, correlationId={}", correlationId);
            return writeFailure(ImageStorageErrorCode.STORAGE_WRITE_FAILED,
                    "Storage yazma sırasında beklenmeyen hata oluştu", correlationId);
        }
    }

    @Override
    public ImageStorageReadResult read(ImageStorageReadRequest request) {
        String correlationId = request != null ? request.getCorrelationId() : null;

        // 1. Null key check
        if (request == null || request.getStorageObjectKey() == null
                || request.getStorageObjectKey().isBlank()) {
            log.warn("FP-008 mock storage read failed: missing key, correlationId={}", correlationId);
            return readFailure(ImageStorageErrorCode.STORAGE_READ_FAILED,
                    "Storage okuma başarısız — geçersiz referans", correlationId);
        }

        // 2. Lookup
        StoredObject obj = store.get(request.getStorageObjectKey());
        if (obj == null) {
            log.warn("FP-008 mock storage read failed: object not found, correlationId={}", correlationId);
            return readFailure(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE,
                    "İstenen içerik bulunamadı", correlationId);
        }

        log.info("FP-008 mock storage read success: mediaAssetId={}, size={}, correlationId={}",
                request.getMediaAssetId(), obj.fileSizeBytes, correlationId);

        // 3. Return success
        return ImageStorageReadResult.builder()
                .success(true)
                .imageBytes(obj.bytes)
                .mimeType(obj.mimeType)
                .fileSizeBytes(obj.fileSizeBytes)
                .errorCode(null)
                .errorMessage(null)
                .correlationId(correlationId)
                .build();
    }

    @Override
    public String getProviderAlias() {
        return PROVIDER_ALIAS;
    }

    // ── Test Support ────────────────────────────────────────────────

    /** Test helper — clear all stored objects. */
    void clear() {
        store.clear();
    }

    /** Test helper — check if object exists. */
    boolean exists(String objectKey) {
        return store.containsKey(objectKey);
    }

    /** Test helper — get stored object count. */
    int size() {
        return store.size();
    }

    // ── Internal ────────────────────────────────────────────────────

    /**
     * Server-side object key generation.
     * Format: imagegen/{mediaAssetId}/{random-uuid}
     * Internal-only — admin/user DTO'ya çıkmaz.
     */
    private String generateObjectKey(ImageStorageWriteRequest request) {
        UUID mediaAssetId = request.getMediaAssetId();
        String prefix = mediaAssetId != null ? mediaAssetId.toString() : "unknown";
        return "imagegen/" + prefix + "/" + UUID.randomUUID();
    }

    private ImageStorageWriteResult writeFailure(ImageStorageErrorCode errorCode,
                                                  String safeMessage,
                                                  String correlationId) {
        return ImageStorageWriteResult.builder()
                .success(false)
                .storageProviderAlias(PROVIDER_ALIAS)
                .storageObjectKey(null)
                .errorCode(errorCode)
                .errorMessage(safeMessage)
                .correlationId(correlationId)
                .build();
    }

    private ImageStorageReadResult readFailure(ImageStorageErrorCode errorCode,
                                                String safeMessage,
                                                String correlationId) {
        return ImageStorageReadResult.builder()
                .success(false)
                .imageBytes(null)
                .mimeType(null)
                .fileSizeBytes(0)
                .errorCode(errorCode)
                .errorMessage(safeMessage)
                .correlationId(correlationId)
                .build();
    }

    // ── Internal stored object ──────────────────────────────────────

    record StoredObject(byte[] bytes, String mimeType, int width, int height,
                        long fileSizeBytes, String checksumSha256) {
    }
}
