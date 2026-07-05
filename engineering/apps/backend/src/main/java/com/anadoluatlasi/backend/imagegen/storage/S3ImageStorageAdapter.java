package com.anadoluatlasi.backend.imagegen.storage;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import com.anadoluatlasi.backend.imagegen.service.ImageStorageErrorNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-010 Task 03: S3-compatible private image storage adapter.
 *
 * <p>Real private storage write/read via {@link S3StorageClient} abstraction.
 * Active only when {@code imagegen.storage.s3.enabled=true}.</p>
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
 *   <li>Bucket/key never logged</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "imagegen.storage.s3.enabled", havingValue = "true")
public class S3ImageStorageAdapter implements ImageStorageAdapter {

    private static final String PROVIDER_ALIAS = "s3";

    private final S3PrivateStorageProperties properties;
    private final S3StorageClient storageClient;

    public S3ImageStorageAdapter(S3PrivateStorageProperties properties,
                                  S3StorageClient storageClient) {
        this.properties = properties;
        this.storageClient = storageClient;
    }

    @Override
    public ImageStorageWriteResult write(ImageStorageWriteRequest request) {
        String correlationId = request != null ? request.getCorrelationId() : null;

        // 1. Config validation
        String configError = properties.validateForProduction();
        if (configError != null) {
            log.warn("[S3_STORAGE] Config invalid for write. correlationId={}", correlationId);
            return writeFailure(ImageStorageErrorCode.STORAGE_WRITE_FAILED,
                    "Depolama yapılandırması eksik", correlationId);
        }

        // 2. Input validation
        if (request == null || request.getImageBytes() == null || request.getImageBytes().length == 0) {
            log.warn("[S3_STORAGE] Write failed: empty input. correlationId={}", correlationId);
            return writeFailure(ImageStorageErrorCode.STORAGE_WRITE_FAILED,
                    "Depolama yazma başarısız — geçersiz girdi", correlationId);
        }

        try {
            // 3. Generate internal object key
            String objectKey = generateObjectKey(request);

            // 4. Write to S3 via client abstraction
            storageClient.putObject(
                    properties.getBucket(),
                    objectKey,
                    request.getImageBytes(),
                    request.getMimeType()
            );

            // Safety: log only safe context, NEVER bucket/key/secret
            log.info("[S3_STORAGE] Write success. correlationId={}, provider={}, size={}",
                    correlationId, PROVIDER_ALIAS, request.getFileSizeBytes());

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
                    .correlationId(correlationId)
                    .build();

        } catch (S3StorageClient.S3StorageClientException e) {
            // Safety: use only safe message from exception, never raw SDK details
            log.warn("[S3_STORAGE] Write failed. correlationId={}, safeMessage={}",
                    correlationId, e.getSafeMessage());
            return writeFailure(ImageStorageErrorCode.STORAGE_WRITE_FAILED,
                    "Depolama yazma sırasında hata oluştu", correlationId);
        } catch (Exception e) {
            // Safety: never log raw exception that may contain bucket/key/credentials
            log.warn("[S3_STORAGE] Unexpected write failure. correlationId={}, exceptionType={}",
                    correlationId, e.getClass().getSimpleName());
            return writeFailure(ImageStorageErrorCode.STORAGE_WRITE_FAILED,
                    "Depolama yazma sırasında beklenmeyen hata oluştu", correlationId);
        }
    }

    @Override
    public ImageStorageReadResult read(ImageStorageReadRequest request) {
        String correlationId = request != null ? request.getCorrelationId() : null;

        // 1. Input validation
        if (request == null || request.getStorageObjectKey() == null
                || request.getStorageObjectKey().isBlank()) {
            log.warn("[S3_STORAGE] Read failed: missing key. correlationId={}", correlationId);
            return readFailure(ImageStorageErrorCode.STORAGE_READ_FAILED,
                    "Depolama okuma başarısız — geçersiz referans", correlationId);
        }

        // 2. Config validation
        String configError = properties.validateForProduction();
        if (configError != null) {
            log.warn("[S3_STORAGE] Config invalid for read. correlationId={}", correlationId);
            return readFailure(ImageStorageErrorCode.STORAGE_READ_FAILED,
                    "Depolama yapılandırması eksik", correlationId);
        }

        try {
            // 3. Read from S3 via client abstraction
            byte[] bytes = storageClient.getObject(
                    properties.getBucket(),
                    request.getStorageObjectKey()
            );

            if (bytes == null || bytes.length == 0) {
                log.warn("[S3_STORAGE] Read returned empty. correlationId={}", correlationId);
                return readFailure(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE,
                        "İstenen içerik bulunamadı", correlationId);
            }

            log.info("[S3_STORAGE] Read success. correlationId={}, size={}", correlationId, bytes.length);

            return ImageStorageReadResult.builder()
                    .success(true)
                    .imageBytes(bytes)
                    .mimeType(null) // MIME comes from entity metadata, not raw storage
                    .fileSizeBytes(bytes.length)
                    .correlationId(correlationId)
                    .build();

        } catch (S3StorageClient.S3StorageClientException e) {
            log.warn("[S3_STORAGE] Read failed. correlationId={}, safeMessage={}",
                    correlationId, e.getSafeMessage());
            return readFailure(ImageStorageErrorCode.STORAGE_READ_FAILED,
                    "Depolama okuma sırasında hata oluştu", correlationId);
        } catch (Exception e) {
            log.warn("[S3_STORAGE] Unexpected read failure. correlationId={}, exceptionType={}",
                    correlationId, e.getClass().getSimpleName());
            return readFailure(ImageStorageErrorCode.STORAGE_READ_FAILED,
                    "Depolama okuma sırasında beklenmeyen hata oluştu", correlationId);
        }
    }

    @Override
    public String getProviderAlias() {
        return PROVIDER_ALIAS;
    }

    // ── Internal ────────────────────────────────────────────────────

    /**
     * Generate internal storage object key.
     * Key is backend-internal only — never exposed to admin/user.
     * Format: {pathPrefix}{mediaAssetId}/{generatedAssetId}/{uuid}
     */
    String generateObjectKey(ImageStorageWriteRequest request) {
        String prefix = properties.getPathPrefix() != null ? properties.getPathPrefix() : "imagegen/";
        String assetId = request.getMediaAssetId() != null
                ? request.getMediaAssetId().toString() : "unknown";
        String genId = request.getGeneratedAssetId() != null
                ? request.getGeneratedAssetId().toString() : UUID.randomUUID().toString();
        return prefix + assetId + "/" + genId;
    }

    private ImageStorageWriteResult writeFailure(ImageStorageErrorCode errorCode,
                                                  String safeMessage,
                                                  String correlationId) {
        return ImageStorageWriteResult.builder()
                .success(false)
                .storageProviderAlias(PROVIDER_ALIAS)
                .errorCode(errorCode)
                .errorMessage(safeMessage)
                .correlationId(correlationId)
                .storedAt(LocalDateTime.now())
                .build();
    }

    private ImageStorageReadResult readFailure(ImageStorageErrorCode errorCode,
                                                String safeMessage,
                                                String correlationId) {
        return ImageStorageReadResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(safeMessage)
                .correlationId(correlationId)
                .build();
    }
}
