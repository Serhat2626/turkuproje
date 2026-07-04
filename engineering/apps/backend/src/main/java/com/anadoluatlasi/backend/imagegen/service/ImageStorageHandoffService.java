package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.repository.ImageMediaAssetRepository;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageAdapter;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageWriteRequest;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageWriteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-008: Storage handoff orchestration service.
 *
 * <p>Validated image bytes'ı validation → storage → metadata update
 * akışıyla işler. GeneratedAsset review status'unu değiştirmez.</p>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>GeneratedAsset'i APPROVED yapmaz</li>
 *   <li>GeneratedAsset'i REJECTED yapmaz</li>
 *   <li>Review decision oluşturmaz</li>
 *   <li>Public/signed/CDN URL üretmez</li>
 *   <li>Provider output download yapmaz</li>
 *   <li>Publish/export yapmaz</li>
 *   <li>READY_FOR_PUBLISH kullanmaz</li>
 *   <li>Generic status update sağlamaz</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageHandoffService {

    private final ImageMediaAssetRepository mediaAssetRepository;
    private final ImageMediaValidationService validationService;
    private final ImageStorageAdapter storageAdapter;

    /**
     * Storage handoff başlatır: validate → store → update metadata.
     *
     * <p>GeneratedAsset review status'u değiştirilmez.
     * Automatic approval yok.</p>
     *
     * @param request handoff request (image bytes + metadata)
     * @return safe handoff result (no storage key / URL exposure)
     */
    @Transactional
    public ImageStorageHandoffResult executeHandoff(ImageStorageHandoffRequest request) {
        UUID mediaAssetId = request.getMediaAssetId();
        String correlationId = request.getCorrelationId();

        log.info("FP-008 storage handoff starting: mediaAssetId={}, correlationId={}",
                mediaAssetId, correlationId);

        // 1. Load MediaAsset
        ImageMediaAsset mediaAsset = mediaAssetRepository.findById(mediaAssetId).orElse(null);
        if (mediaAsset == null) {
            log.warn("FP-008 storage handoff failed: media asset not found, mediaAssetId={}, correlationId={}",
                    mediaAssetId, correlationId);
            return failureResult(request, ImageStorageErrorCode.UNKNOWN_STORAGE_HANDOFF_FAILURE,
                    "İlgili medya kaydı bulunamadı");
        }

        // 2. Set status → STORAGE_VALIDATING
        mediaAsset.setStorageHandoffStatus(ImageStorageHandoffStatus.STORAGE_VALIDATING);
        mediaAsset.setPreviewStatus(ImagePreviewStatus.PREVIEW_UNAVAILABLE);
        mediaAssetRepository.save(mediaAsset);

        // 3. Validate
        ImageMediaValidationResult validationResult = validationService.validate(
                request.getImageBytes(), correlationId);

        if (!validationResult.isValid()) {
            log.warn("FP-008 storage handoff validation failed: errorCode={}, mediaAssetId={}, correlationId={}",
                    validationResult.getErrorCode(), mediaAssetId, correlationId);
            return applyFailure(mediaAsset, request,
                    validationResult.getErrorCode(), validationResult.getErrorMessage());
        }

        // 4. Write to storage
        ImageStorageWriteRequest writeRequest = ImageStorageWriteRequest.builder()
                .mediaAssetId(mediaAssetId)
                .generatedAssetId(request.getGeneratedAssetId())
                .correlationId(correlationId)
                .mimeType(validationResult.getDetectedMimeType())
                .width(validationResult.getWidth())
                .height(validationResult.getHeight())
                .fileSizeBytes(validationResult.getFileSizeBytes())
                .checksumSha256(validationResult.getChecksumSha256())
                .imageBytes(request.getImageBytes())
                .requestedAt(LocalDateTime.now())
                .build();

        ImageStorageWriteResult writeResult;
        try {
            writeResult = storageAdapter.write(writeRequest);
        } catch (Exception e) {
            log.warn("FP-008 storage adapter write exception: mediaAssetId={}, correlationId={}",
                    mediaAssetId, correlationId);
            return applyFailure(mediaAsset, request,
                    ImageStorageErrorCode.STORAGE_WRITE_FAILED,
                    "Storage yazma sırasında beklenmeyen hata oluştu");
        }

        if (!writeResult.isSuccess()) {
            log.warn("FP-008 storage write failed: errorCode={}, mediaAssetId={}, correlationId={}",
                    writeResult.getErrorCode(), mediaAssetId, correlationId);
            ImageStorageErrorCode writeErrorCode = writeResult.getErrorCode() != null
                    ? writeResult.getErrorCode()
                    : ImageStorageErrorCode.STORAGE_WRITE_FAILED;
            String writeErrorMessage = writeResult.getErrorMessage() != null
                    ? writeResult.getErrorMessage()
                    : "Storage yazma başarısız";
            return applyFailure(mediaAsset, request, writeErrorCode, writeErrorMessage);
        }

        // 5. Update MediaAsset safe metadata — success
        LocalDateTime storedAt = LocalDateTime.now();

        mediaAsset.setMimeType(validationResult.getDetectedMimeType());
        mediaAsset.setWidth(validationResult.getWidth());
        mediaAsset.setHeight(validationResult.getHeight());
        mediaAsset.setFileSizeBytes(validationResult.getFileSizeBytes());
        mediaAsset.setChecksumSha256(validationResult.getChecksumSha256());

        // Internal storage reference — not admin-facing
        mediaAsset.setStorageProvider(writeResult.getStorageProviderAlias());
        mediaAsset.setStorageObjectKey(writeResult.getStorageObjectKey());

        mediaAsset.setStorageHandoffStatus(ImageStorageHandoffStatus.STORAGE_STORED);
        mediaAsset.setPreviewStatus(ImagePreviewStatus.PREVIEW_AVAILABLE);
        mediaAsset.setStorageErrorCode(null);
        mediaAsset.setStorageErrorMessage(null);
        mediaAsset.setStoredAt(storedAt);

        mediaAssetRepository.save(mediaAsset);

        log.info("FP-008 storage handoff success: mediaAssetId={}, mimeType={}, size={}, {}x{}, correlationId={}",
                mediaAssetId, validationResult.getDetectedMimeType(),
                validationResult.getFileSizeBytes(),
                validationResult.getWidth(), validationResult.getHeight(),
                correlationId);

        return ImageStorageHandoffResult.builder()
                .success(true)
                .mediaAssetId(mediaAssetId)
                .generatedAssetId(request.getGeneratedAssetId())
                .storageHandoffStatus(ImageStorageHandoffStatus.STORAGE_STORED)
                .previewStatus(ImagePreviewStatus.PREVIEW_AVAILABLE)
                .mimeType(validationResult.getDetectedMimeType())
                .width(validationResult.getWidth())
                .height(validationResult.getHeight())
                .fileSizeBytes(validationResult.getFileSizeBytes())
                .checksumSha256(validationResult.getChecksumSha256())
                .storedAt(storedAt)
                .errorCode(null)
                .errorMessage(null)
                .correlationId(correlationId)
                .build();
    }

    // ── Internal Helpers ────────────────────────────────────────────

    private ImageStorageHandoffResult applyFailure(ImageMediaAsset mediaAsset,
                                                    ImageStorageHandoffRequest request,
                                                    ImageStorageErrorCode errorCode,
                                                    String safeMessage) {
        mediaAsset.setStorageHandoffStatus(ImageStorageHandoffStatus.STORAGE_FAILED);
        mediaAsset.setPreviewStatus(ImagePreviewStatus.PREVIEW_UNAVAILABLE);
        mediaAsset.setStorageErrorCode(errorCode);
        mediaAsset.setStorageErrorMessage(safeMessage);
        mediaAsset.setStoredAt(null);
        mediaAssetRepository.save(mediaAsset);

        return failureResult(request, errorCode, safeMessage);
    }

    private ImageStorageHandoffResult failureResult(ImageStorageHandoffRequest request,
                                                     ImageStorageErrorCode errorCode,
                                                     String safeMessage) {
        return ImageStorageHandoffResult.builder()
                .success(false)
                .mediaAssetId(request.getMediaAssetId())
                .generatedAssetId(request.getGeneratedAssetId())
                .storageHandoffStatus(ImageStorageHandoffStatus.STORAGE_FAILED)
                .previewStatus(ImagePreviewStatus.PREVIEW_UNAVAILABLE)
                .errorCode(errorCode)
                .errorMessage(safeMessage)
                .correlationId(request.getCorrelationId())
                .build();
    }
}
