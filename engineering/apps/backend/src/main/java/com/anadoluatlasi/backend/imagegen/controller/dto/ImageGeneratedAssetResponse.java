package com.anadoluatlasi.backend.imagegen.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-007: Generated image asset safe response DTO.
 *
 * <p>Review lifecycle ile birlikte güvenli asset metadata döner.
 * Raw provider data, internal prompt, publish/export status döndürülmez.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageGeneratedAssetResponse {
    private UUID id;
    private UUID imageGenerationRequestId;
    private UUID imageGenerationResultId;
    private UUID imageMediaAssetId;
    private UUID contentProjectId;
    private UUID visualAssetPlanId;
    private UUID visualAssetItemId;
    private String status;
    private String safePromptSummary;
    private String qualitySignalsSummary;
    private String safetyFlagsSummary;
    private boolean previewAvailable;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private ImageMediaAssetResponse mediaAsset;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
