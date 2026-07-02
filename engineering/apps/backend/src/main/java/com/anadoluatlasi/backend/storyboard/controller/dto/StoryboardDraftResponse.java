package com.anadoluatlasi.backend.storyboard.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-003: Storyboard taslağı detay REST yanıt DTO'su.
 * Provider-agnostic, internal AI contract field'ları içermez.
 * FP-002 ScenarioDraftResponse pattern'ı ile tutarlıdır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoryboardDraftResponse {
    private UUID id;
    private UUID contentProjectId;
    private UUID sourceDraftId;
    private UUID sourceScenarioDraftId;
    private Integer sourceScenarioVersion;
    private Integer version;
    private String status;
    private String title;
    private String summary;
    private String targetPlatform;
    private String aspectRatio;
    private Integer estimatedDurationSeconds;
    private String visualStyleGuide;
    private String narrationStyle;
    private String reviewNotes;
    private String generatedBy;
    private String providerType;
    private String modelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime editedAt;
    private String createdBy;
    private String approvedBy;
    private String rejectedBy;
    private String editedBy;
    private List<StoryboardSceneResponse> scenes;
}
