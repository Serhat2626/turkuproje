package com.anadoluatlasi.backend.platformpackage.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-004: Platform paket taslağı detay yanıt DTO'su.
 * Scene usages dahildir. Internal AI contract/raw prompt/secret expose edilmez.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPackageDraftResponse {
    // ── Identity ──
    private UUID id;
    private UUID contentProjectId;
    private UUID sourceStoryboardDraftId;
    private String packageType;
    private String status;
    private Integer version;

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
    private String tagsJson;

    // ── Short-Form / Reels ──
    private String shortHook;
    private String shortCaption;
    private String overlayTextSuggestionsJson;
    private String shortHashtagSetJson;
    private String shortCta;
    private String durationGuidance;

    // ── AI Metadata (safe) ──
    private String providerType;
    private String modelName;
    private String generatedBy;

    // ── Review Lifecycle ──
    private LocalDateTime editedAt;
    private String editedBy;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
    private String rejectionReason;

    // ── Timestamps ──
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Scene Usages ──
    private List<PlatformPackageSceneUsageResponse> sceneUsages;
}
