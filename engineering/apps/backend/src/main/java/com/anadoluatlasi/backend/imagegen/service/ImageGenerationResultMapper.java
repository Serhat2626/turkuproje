package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.controller.dto.ImageGenerationResultResponse;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationResult;

/**
 * FP-007: ImageGenerationResult entity → safe response DTO dönüşümü.
 *
 * <p>providerRequestId admin-facing DTO'ya dahil edilmez.
 * Raw provider payload/response, full prompt, signed URL döndürülmez.</p>
 */
public final class ImageGenerationResultMapper {

    private ImageGenerationResultMapper() {
    }

    /**
     * Entity → safe response DTO. providerRequestId intentionally excluded.
     */
    public static ImageGenerationResultResponse toResponse(ImageGenerationResult entity) {
        return ImageGenerationResultResponse.builder()
                .id(entity.getId())
                .imageGenerationRequestId(entity.getImageGenerationRequestId())
                .visualAssetPlanId(entity.getVisualAssetPlanId())
                .visualAssetItemId(entity.getVisualAssetItemId())
                .providerType(entity.getProviderType())
                .modelName(entity.getModelName())
                // providerRequestId intentionally excluded — internal-only
                .generationDurationMs(entity.getGenerationDurationMs())
                .outputCount(entity.getOutputCount())
                .mimeType(entity.getMimeType())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .fileSizeBytes(entity.getFileSizeBytes())
                .checksumSha256(entity.getChecksumSha256())
                .errorCode(entity.getErrorCode() != null ? entity.getErrorCode().name() : null)
                .errorMessage(entity.getErrorMessage())
                .correlationId(entity.getCorrelationId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
