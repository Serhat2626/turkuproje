package com.anadoluatlasi.backend.storyboard.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FP-003: Storyboard taslağı düzenleme istek DTO'su.
 * FP-002 ScenarioDraftEditRequest pattern'ı ile tutarlıdır.
 *
 * <p>Client status, version, provider fields gönderemez.
 * Raw prompt/provider payload alanı içermez.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryboardDraftEditRequest {
    private String title;
    private String summary;
    private String targetPlatform;
    private String aspectRatio;
    private Integer estimatedDurationSeconds;
    private String visualStyleGuide;
    private String narrationStyle;
    private String reviewNotes;
    private List<StoryboardSceneEditRequest> scenes;
}
