package com.anadoluatlasi.backend.platformpackage.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FP-004: Platform paketi düzenleme istek DTO'su.
 *
 * <p>Status, version, source references, providerType, modelName, generatedBy,
 * raw prompt, API key, secret alanları YOKTUR — tasarım gereği.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPackageDraftEditRequest {

    // ── Common ──
    private String title;
    private String captionOrDescription;
    private String hashtagsJson;
    private String callToAction;
    private Integer targetDurationSeconds;
    private String aspectRatio;
    private String contentStructureNotes;
    private String sceneUsageNotes;
    private String reviewNotes;

    // ── YouTube Long-Form ──
    private String videoTitle;
    private String longDescription;
    private String chapterOutlineJson;
    private String pinnedCommentSuggestion;
    private String thumbnailTextSuggestion;

    // ── Short-Form / Reels ──
    private String shortHook;
    private String shortCaption;
    private String overlayTextSuggestionsJson;
    private String shortHashtagSetJson;
    private String shortCta;
    private String durationGuidance;

    // ── Scene Usage Edits ──
    private List<PlatformPackageSceneUsageEditRequest> sceneUsages;
}
