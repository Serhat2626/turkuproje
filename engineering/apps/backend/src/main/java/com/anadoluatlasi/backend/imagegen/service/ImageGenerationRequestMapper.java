package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.controller.dto.ImageGenerationRequestResponse;
import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationRequest;

/**
 * FP-007: ImageGenerationRequest entity → safe response DTO dönüşümü.
 *
 * <p>Yalnızca güvenli alanlar döner. Raw provider payload/response,
 * internal full prompt, API key/secret, storage key döndürülmez.</p>
 */
public final class ImageGenerationRequestMapper {

    private ImageGenerationRequestMapper() {
    }

    /**
     * Entity → safe response DTO.
     */
    public static ImageGenerationRequestResponse toResponse(ImageGenerationRequest entity) {
        return ImageGenerationRequestResponse.builder()
                .id(entity.getId())
                .contentProjectId(entity.getContentProjectId())
                .visualAssetPlanId(entity.getVisualAssetPlanId())
                .visualAssetItemId(entity.getVisualAssetItemId())
                .itemType(entity.getItemType() != null ? entity.getItemType().name() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .providerType(entity.getProviderType())
                .modelName(entity.getModelName())
                .safePromptSummary(entity.getSafePromptSummary())
                .errorCode(entity.getErrorCode() != null ? entity.getErrorCode().name() : null)
                .errorMessage(entity.getErrorMessage())
                .correlationId(entity.getCorrelationId())
                .requestedBy(entity.getRequestedBy())
                .requestedAt(entity.getRequestedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
