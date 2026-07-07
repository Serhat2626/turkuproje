package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-021 Task 02: Safe response DTO for composition plan preparation.
 *
 * <p>No lyrics, no rendered video, no downloadable file, no public/signed/CDN URL,
 * no storage key, no base64/binary, no raw provider payload.</p>
 */
public final class TurkuCompositionPlanResponse {

    private final UUID projectId;
    private final UUID compositionId;
    private final TurkuVideoWorkflowStage currentStage;
    private final int sceneCount;
    private final int totalDurationSeconds;
    private final boolean hasTimingConflict;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final List<String> reviewFlags;
    private final String summary;

    public TurkuCompositionPlanResponse(UUID projectId, UUID compositionId,
                                         TurkuVideoWorkflowStage currentStage,
                                         int sceneCount, int totalDurationSeconds,
                                         boolean hasTimingConflict,
                                         boolean humanReviewRequired,
                                         boolean copyrightReviewRequired,
                                         List<String> reviewFlags,
                                         String summary) {
        this.projectId = projectId;
        this.compositionId = compositionId;
        this.currentStage = currentStage;
        this.sceneCount = sceneCount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.hasTimingConflict = hasTimingConflict;
        this.humanReviewRequired = humanReviewRequired;
        this.copyrightReviewRequired = copyrightReviewRequired;
        this.autoApproved = false;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
        this.summary = summary;
    }

    public UUID getProjectId() { return projectId; }
    public UUID getCompositionId() { return compositionId; }
    public TurkuVideoWorkflowStage getCurrentStage() { return currentStage; }
    public int getSceneCount() { return sceneCount; }
    public int getTotalDurationSeconds() { return totalDurationSeconds; }
    public boolean isHasTimingConflict() { return hasTimingConflict; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getReviewFlags() { return reviewFlags; }
    public String getSummary() { return summary; }
}
