package com.anadoluatlasi.backend.visualasset.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-006: Visual Asset Plan detay REST yanıt DTO'su.
 * Provider-agnostic, internal AI contract field'ları içermez.
 *
 * <p>Forbidden fields: rawPrompt, internalPrompt, providerRequest,
 * providerResponse, providerPayload, apiKey, secret, imageUrl,
 * objectKey, bucket, mediaAssetId, generatedAssetId.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisualAssetPlanResponse {
    private UUID id;
    private UUID sourceStoryboardDraftId;
    private UUID contentProjectId;
    private Integer version;
    private String status;
    private String title;
    private String summary;
    private String overallVisualStyle;
    private String aspectRatioStrategy;
    private String culturalAuthenticityGuidance;
    private String characterContinuityGuidance;
    private String locationContinuityGuidance;
    private String colorPaletteGuidance;
    private String reviewNotes;
    private String generatedBy;
    private String providerType;
    private String modelName;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
    private String rejectionReason;
    private LocalDateTime editedAt;
    private String editedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SceneVisualPlanResponse> scenePlans;
    private ThumbnailPlanResponse thumbnailPlan;
}
