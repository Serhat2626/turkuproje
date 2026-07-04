package com.anadoluatlasi.backend.visualasset.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-006: Visual Asset Plan düzenleme istek DTO'su.
 *
 * <p>Client status, version, provider fields gönderemez.
 * Raw prompt/provider payload alanı içermez.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualAssetPlanEditRequest {
    private String title;
    private String summary;
    private String overallVisualStyle;
    private String aspectRatioStrategy;
    private String culturalAuthenticityGuidance;
    private String characterContinuityGuidance;
    private String locationContinuityGuidance;
    private String colorPaletteGuidance;
    private String reviewNotes;
}
