package com.anadoluatlasi.backend.visualasset.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-006: Scene Visual Plan REST yanıt DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SceneVisualPlanResponse {
    private UUID id;
    private UUID sourceStoryboardSceneId;
    private Integer sceneNumber;
    private String sourceBeatType;
    private String sceneTitle;
    private String turkishVisualContext;
    private String englishImagePrompt;
    private String negativePrompt;
    private String styleDirective;
    private String aspectRatio;
    private String compositionNotes;
    private String cameraFraming;
    private String lightingNotes;
    private String colorPalette;
    private String mood;
    private String location;
    private String characters;
    private String culturalDetails;
    private String textOverlayGuidance;
    private String imageGenerationToolNotes;
    private String safetyNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
