package com.anadoluatlasi.backend.visualasset.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-006: Thumbnail Plan düzenleme istek DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThumbnailPlanEditRequest {
    private String thumbnailPrompt;
    private String negativePrompt;
    private String textOverlayGuidance;
    private String compositionNotes;
    private String emotionalHook;
    private String aspectRatio;
    private String styleDirective;
    private String colorPalette;
}
