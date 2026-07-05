package com.anadoluatlasi.backend.imagegen.storage;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import com.anadoluatlasi.backend.imagegen.service.ImageStorageErrorNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-009 Task 08: S3-compatible private image storage adapter shell.
 *
 * <p>This is a shell implementation that will be completed in Task 09.
 * Currently returns a safe failure indicating storage is not yet configured,
 * while proving the adapter boundary, result safety, and error sanitization.</p>
 *
 * <p>Security rules:</p>
 * <ul>
 *   <li>No public URL generation</li>
 *   <li>No signed URL generation</li>
 *   <li>No CDN URL generation</li>
 *   <li>storageObjectKey is internal-only — never admin/user-facing</li>
 *   <li>No raw storage provider response exposed</li>
 *   <li>No credentials/config values logged or exposed</li>
 *   <li>All errors sanitized via ImageStorageErrorNormalizer</li>
 * </ul>
 *
 * <p>This bean is NOT registered as @Component — it will be conditionally
 * activated via configuration when S3 is enabled (Task 09).</p>
 */
@Slf4j
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements ImageStorageAdapter {

    private static final String PROVIDER_ALIAS = "s3";

    private final S3PrivateStorageProperties properties;

    @Override
    public ImageStorageWriteResult write(ImageStorageWriteRequest request) {
        // Shell: S3 write not yet implemented — return safe failure
        // Task 09 will implement real S3 SDK call
        if (!properties.isEnabled()) {
            log.warn("[S3_STORAGE] S3 storage is disabled. correlationId={}",
                    request != null ? request.getCorrelationId() : "unknown");
            return ImageStorageWriteResult.builder()
                    .success(false)
                    .storageProviderAlias(PROVIDER_ALIAS)
                    .errorCode(ImageStorageErrorCode.STORAGE_WRITE_FAILED)
                    .errorMessage(ImageStorageErrorNormalizer.safeMessage(
                            ImageStorageErrorCode.STORAGE_WRITE_FAILED,
                            "S3 depolama henüz yapılandırılmamış"))
                    .storedAt(LocalDateTime.now())
                    .build();
        }

        // TODO Task 09: Implement real S3 SDK write
        // 1. Build S3 object key from pathPrefix + asset context
        // 2. PutObject with validated bytes
        // 3. Return safe result with internal key only
        String objectKey = generateObjectKey(request);

        log.info("[S3_STORAGE] S3 write shell called (not yet implemented). correlationId={}",
                request != null ? request.getCorrelationId() : "unknown");

        return ImageStorageWriteResult.builder()
                .success(false)
                .storageProviderAlias(PROVIDER_ALIAS)
                .storageObjectKey(objectKey)
                .errorCode(ImageStorageErrorCode.STORAGE_WRITE_FAILED)
                .errorMessage(ImageStorageErrorNormalizer.safeMessage(
                        ImageStorageErrorCode.STORAGE_WRITE_FAILED,
                        "S3 yazma henüz uygulanmadı"))
                .storedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public ImageStorageReadResult read(ImageStorageReadRequest request) {
        // Shell: S3 read not yet implemented — return safe failure
        // Task 09/10 will implement real S3 SDK read for preview proxy
        log.warn("[S3_STORAGE] S3 read shell called (not yet implemented). correlationId={}",
                request != null ? request.getCorrelationId() : "unknown");

        return ImageStorageReadResult.builder()
                .success(false)
                .errorCode(ImageStorageErrorCode.STORAGE_READ_FAILED)
                .errorMessage(ImageStorageErrorNormalizer.safeMessage(
                        ImageStorageErrorCode.STORAGE_READ_FAILED,
                        "S3 okuma henüz uygulanmadı"))
                .build();
    }

    @Override
    public String getProviderAlias() {
        return PROVIDER_ALIAS;
    }

    /**
     * Generate internal storage object key.
     * Key is backend-internal only — never exposed to admin/user.
     *
     * Format: {pathPrefix}{mediaAssetId}/{uuid}
     */
    String generateObjectKey(ImageStorageWriteRequest request) {
        String prefix = properties.getPathPrefix() != null ? properties.getPathPrefix() : "imagegen/";
        String assetId = request != null && request.getMediaAssetId() != null
                ? request.getMediaAssetId().toString() : "unknown";
        String uniqueId = UUID.randomUUID().toString();
        return prefix + assetId + "/" + uniqueId;
    }
}
