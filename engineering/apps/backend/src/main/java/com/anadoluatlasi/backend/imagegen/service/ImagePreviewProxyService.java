package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.repository.ImageGeneratedAssetRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageMediaAssetRepository;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageAdapter;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageReadRequest;
import com.anadoluatlasi.backend.imagegen.storage.ImageStorageReadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * FP-008: Backend-controlled preview proxy service.
 *
 * <p>Storage'dan image bytes okuyarak backend üzerinden proxy eder.
 * Public/signed/CDN URL üretmez. storageObjectKey expose etmez.
 * GeneratedAsset/MediaAsset status değiştirmez.</p>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>Public/signed/CDN URL üretmez</li>
 *   <li>Storage key expose etmez</li>
 *   <li>Base64 response döndürmez</li>
 *   <li>GeneratedAsset status değiştirmez</li>
 *   <li>MediaAsset status değiştirmez</li>
 *   <li>Review decision oluşturmaz</li>
 *   <li>Automatic approval yapmaz</li>
 *   <li>Provider output download yapmaz</li>
 *   <li>Publish/export yapmaz</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImagePreviewProxyService {

    private final ImageGeneratedAssetRepository generatedAssetRepository;
    private final ImageMediaAssetRepository mediaAssetRepository;
    private final ImageStorageAdapter storageAdapter;

    /** Safe image MIME types allowed for preview */
    private static final Set<String> ALLOWED_PREVIEW_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp"
    );

    /**
     * Backend-controlled preview proxy.
     *
     * <p>GeneratedAsset → linked MediaAsset → storage read → image bytes.
     * Status değiştirmez, review decision oluşturmaz.</p>
     *
     * @param generatedAssetId generated asset identifier
     * @return preview result with image bytes or safe error
     */
    @Transactional(readOnly = true)
    public ImagePreviewProxyResult getPreview(UUID generatedAssetId) {

        log.debug("FP-008 preview proxy requested: generatedAssetId={}", generatedAssetId);

        // 1. Find GeneratedAsset
        ImageGeneratedAsset generatedAsset = generatedAssetRepository
                .findById(generatedAssetId).orElse(null);
        if (generatedAsset == null) {
            log.warn("FP-008 preview proxy: generated asset not found, generatedAssetId={}",
                    generatedAssetId);
            return previewError(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE,
                    "İlgili üretilmiş görsel bulunamadı");
        }

        // 2. Find linked MediaAsset
        UUID mediaAssetId = generatedAsset.getImageMediaAssetId();
        if (mediaAssetId == null) {
            log.warn("FP-008 preview proxy: no linked media asset, generatedAssetId={}",
                    generatedAssetId);
            return previewError(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE,
                    "Görselin medya kaydı bulunamadı");
        }

        ImageMediaAsset mediaAsset = mediaAssetRepository.findById(mediaAssetId).orElse(null);
        if (mediaAsset == null) {
            log.warn("FP-008 preview proxy: media asset not found, mediaAssetId={}",
                    mediaAssetId);
            return previewError(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE,
                    "Medya kaydı bulunamadı");
        }

        // 3. Check eligibility
        if (mediaAsset.getStorageHandoffStatus() != ImageStorageHandoffStatus.STORAGE_STORED) {
            log.info("FP-008 preview proxy: storage not ready, status={}, mediaAssetId={}",
                    mediaAsset.getStorageHandoffStatus(), mediaAssetId);
            return previewError(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE,
                    "Görsel henüz storage'a aktarılmamış");
        }

        if (mediaAsset.getPreviewStatus() != ImagePreviewStatus.PREVIEW_AVAILABLE) {
            log.info("FP-008 preview proxy: preview unavailable, previewStatus={}, mediaAssetId={}",
                    mediaAsset.getPreviewStatus(), mediaAssetId);
            return previewError(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE,
                    "Önizleme mevcut değil");
        }

        // 4. Check storage key
        String storageObjectKey = mediaAsset.getStorageObjectKey();
        if (storageObjectKey == null || storageObjectKey.isBlank()) {
            log.warn("FP-008 preview proxy: missing storage key, mediaAssetId={}", mediaAssetId);
            return previewError(ImageStorageErrorCode.STORAGE_READ_FAILED,
                    "Depolama referansı eksik");
        }

        // 5. Validate MIME type
        String mimeType = mediaAsset.getMimeType();
        if (mimeType == null || !ALLOWED_PREVIEW_MIME_TYPES.contains(mimeType)) {
            log.warn("FP-008 preview proxy: unsupported MIME type, mimeType={}, mediaAssetId={}",
                    mimeType, mediaAssetId);
            return previewError(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE,
                    "Desteklenmeyen dosya formatı");
        }

        // 6. Read from storage — internal, no URL exposure
        ImageStorageReadResult readResult;
        try {
            readResult = storageAdapter.read(ImageStorageReadRequest.builder()
                    .storageObjectKey(storageObjectKey)
                    .mediaAssetId(mediaAssetId)
                    .generatedAssetId(generatedAssetId)
                    .build());
        } catch (Exception e) {
            log.warn("FP-008 preview proxy: storage read exception, mediaAssetId={}", mediaAssetId);
            return previewError(ImageStorageErrorCode.STORAGE_READ_FAILED,
                    "Depolama okuma sırasında hata oluştu");
        }

        if (!readResult.isSuccess()) {
            log.warn("FP-008 preview proxy: storage read failed, errorCode={}, mediaAssetId={}",
                    readResult.getErrorCode(), mediaAssetId);
            ImageStorageErrorCode readErrorCode = readResult.getErrorCode() != null
                    ? readResult.getErrorCode()
                    : ImageStorageErrorCode.STORAGE_READ_FAILED;
            return previewError(readErrorCode,
                    readResult.getErrorMessage() != null
                            ? readResult.getErrorMessage()
                            : "Depolama okuma başarısız");
        }

        if (readResult.getImageBytes() == null || readResult.getImageBytes().length == 0) {
            log.warn("FP-008 preview proxy: empty bytes from storage, mediaAssetId={}", mediaAssetId);
            return previewError(ImageStorageErrorCode.PREVIEW_NOT_AVAILABLE,
                    "Önizleme içeriği boş");
        }

        log.info("FP-008 preview proxy success: generatedAssetId={}, mimeType={}, size={}",
                generatedAssetId, mimeType, readResult.getImageBytes().length);

        // 7. Return bytes — controller will stream, no base64
        return ImagePreviewProxyResult.builder()
                .success(true)
                .imageBytes(readResult.getImageBytes())
                .mimeType(mimeType)
                .fileSizeBytes(readResult.getImageBytes().length)
                .errorCode(null)
                .errorMessage(null)
                .build();
    }

    // ── Internal ────────────────────────────────────────────────────

    private ImagePreviewProxyResult previewError(ImageStorageErrorCode errorCode,
                                                  String safeMessage) {
        return ImagePreviewProxyResult.builder()
                .success(false)
                .imageBytes(null)
                .mimeType(null)
                .fileSizeBytes(0)
                .errorCode(errorCode)
                .errorMessage(safeMessage)
                .build();
    }
}
