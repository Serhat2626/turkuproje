package com.anadoluatlasi.backend.visualasset.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-006: Scene Visual Plan düzenleme istek DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneVisualPlanEditRequest {
    private String englishImagePrompt;
    private String turkishVisualContext;
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
}
