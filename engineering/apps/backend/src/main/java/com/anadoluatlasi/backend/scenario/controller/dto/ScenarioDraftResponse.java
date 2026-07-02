package com.anadoluatlasi.backend.scenario.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-002: Senaryo taslağı REST yanıt DTO'su.
 * Provider-agnostic, internal AI contract field'ları içermez.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScenarioDraftResponse {
    private UUID id;
    private UUID contentProjectId;
    private UUID sourceDraftId;
    private Integer version;
    private String status;
    private String title;
    private String summary;
    private String targetAudience;
    private String culturalObjective;
    private String tone;
    private Integer estimatedDurationSeconds;
    private String setting;
    private String characters;
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
    private List<ScenarioBeatResponse> beats;
}
