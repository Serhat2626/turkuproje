package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.*;
import com.anadoluatlasi.backend.imagegen.provider.ImageProviderGenerationResult;
import com.anadoluatlasi.backend.imagegen.repository.ImageGeneratedAssetRepository;
import com.anadoluatlasi.backend.imagegen.repository.ImageMediaAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FP-007: Başarılı generation sonrası MediaAsset ve GeneratedAsset handoff servisi.
 *
 * <p>Sorumluluklar:</p>
 * <ul>
 *   <li>ImageMediaAsset metadata kaydı oluşturmak</li>
 *   <li>ImageGeneratedAsset kaydı oluşturmak (status: GENERATED_PENDING_REVIEW)</li>
 *   <li>Lineage alanlarını korumak</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>Storage upload/download yapmaz</li>
 *   <li>Image binary/base64 üretmez</li>
 *   <li>Public/signed/CDN URL üretmez</li>
 *   <li>Review approve/reject yapmaz</li>
 *   <li>APPROVED/REJECTED/READY_FOR_PUBLISH status atamaz</li>
 *   <li>GeneratedAssetReview oluşturmaz</li>
 *   <li>Raw provider payload/response saklamaz</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGeneratedAssetHandoffService {

    private final ImageMediaAssetRepository mediaAssetRepository;
    private final ImageGeneratedAssetRepository generatedAssetRepository;

    /**
     * Başarılı generation result sonrası MediaAsset ve GeneratedAsset oluşturur.
     *
     * @param request  source generation request
     * @param result   saved generation result entity
     * @param providerResult normalized provider result
     * @return created ImageGeneratedAsset entity
     */
    @Transactional
    public ImageGeneratedAsset createAssetHandoff(ImageGenerationRequest request,
                                                   ImageGenerationResult result,
                                                   ImageProviderGenerationResult providerResult) {

        // 1. ImageMediaAsset metadata oluştur — gerçek dosya/storage yok
        ImageMediaAsset mediaAsset = ImageMediaAsset.builder()
                .assetKind(ImageMediaAssetKind.GENERATED_IMAGE)
                .mimeType(providerResult.getMimeType())
                .width(providerResult.getWidth())
                .height(providerResult.getHeight())
                .fileSizeBytes(providerResult.getFileSizeBytes())
                .checksumSha256(providerResult.getChecksumSha256())
                // storageProvider/storageBucket/storageObjectKey null — storage henüz yok
                .build();

        mediaAsset = mediaAssetRepository.save(mediaAsset);

        log.info("ImageMediaAsset oluşturuldu: mediaAssetId={}, kind={}, mimeType={}",
                mediaAsset.getId(), mediaAsset.getAssetKind(), mediaAsset.getMimeType());

        // 2. ImageGeneratedAsset oluştur — GENERATED_PENDING_REVIEW
        ImageGeneratedAsset generatedAsset = ImageGeneratedAsset.builder()
                .imageGenerationRequestId(request.getId())
                .imageGenerationResultId(result.getId())
                .imageMediaAssetId(mediaAsset.getId())
                .contentProjectId(request.getContentProjectId())
                .visualAssetPlanId(request.getVisualAssetPlanId())
                .visualAssetItemId(request.getVisualAssetItemId())
                .status(GeneratedAssetStatus.GENERATED_PENDING_REVIEW)
                .safePromptSummary(request.getSafePromptSummary())
                .qualitySignalsSummary(providerResult.getQualitySignalsSummary())
                .safetyFlagsSummary(providerResult.getSafetyFlagsSummary())
                .build();

        generatedAsset = generatedAssetRepository.save(generatedAsset);

        log.info("ImageGeneratedAsset oluşturuldu: generatedAssetId={}, requestId={}, status={}",
                generatedAsset.getId(), request.getId(), generatedAsset.getStatus());

        return generatedAsset;
    }
}
