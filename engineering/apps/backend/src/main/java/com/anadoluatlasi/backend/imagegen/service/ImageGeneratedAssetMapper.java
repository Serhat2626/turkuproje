package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.controller.dto.ImageGeneratedAssetResponse;
import com.anadoluatlasi.backend.imagegen.controller.dto.ImageMediaAssetResponse;
import com.anadoluatlasi.backend.imagegen.entity.ImageGeneratedAsset;
import com.anadoluatlasi.backend.imagegen.entity.ImageMediaAsset;

/**
 * FP-007: ImageGeneratedAsset entity → safe response DTO dönüşümü.
 *
 * <p>Raw provider payload, internal prompt, storage key, publish/export
 * status döndürülmez. Nested media asset varsa safe mapping yapılır.</p>
 */
public final class ImageGeneratedAssetMapper {

    private ImageGeneratedAssetMapper() {
    }

    public static ImageGeneratedAssetResponse toResponse(ImageGeneratedAsset entity) {
        return toResponse(entity, null);
    }

    public static ImageGeneratedAssetResponse toResponse(ImageGeneratedAsset entity,
                                                          ImageMediaAsset mediaAsset) {
        ImageMediaAssetResponse mediaResponse = mediaAsset != null
                ? ImageMediaAssetMapper.toResponse(mediaAsset)
                : null;

        return ImageGeneratedAssetResponse.builder()
                .id(entity.getId())
                .imageGenerationRequestId(entity.getImageGenerationRequestId())
                .imageGenerationResultId(entity.getImageGenerationResultId())
                .imageMediaAssetId(entity.getImageMediaAssetId())
                .contentProjectId(entity.getContentProjectId())
                .visualAssetPlanId(entity.getVisualAssetPlanId())
                .visualAssetItemId(entity.getVisualAssetItemId())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .safePromptSummary(entity.getSafePromptSummary())
                .qualitySignalsSummary(entity.getQualitySignalsSummary())
                .safetyFlagsSummary(entity.getSafetyFlagsSummary())
                .previewAvailable(entity.getImageMediaAssetId() != null)
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .rejectionReason(entity.getRejectionReason())
                .mediaAsset(mediaResponse)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
