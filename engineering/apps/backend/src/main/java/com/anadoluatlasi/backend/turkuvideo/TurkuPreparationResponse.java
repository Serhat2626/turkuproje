package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-019 Task 03: Unified response for prepare-story/scenario/storyboard.
 * Safe fields only. No provider data, no public URLs, no credentials.
 */
public final class TurkuPreparationResponse {

    private final UUID projectId;
    private final TurkuVideoWorkflowStage newStage;
    private final String safeMessage;
    private final TurkuSourceConfidence sourceConfidence;
    private final boolean needsHumanReview;
    private final boolean copyrightReviewRequired;
    private final List<String> reviewFlags;

    public TurkuPreparationResponse(UUID projectId, TurkuVideoWorkflowStage newStage,
                                     String safeMessage, TurkuSourceConfidence sourceConfidence,
                                     boolean needsHumanReview, boolean copyrightReviewRequired,
                                     List<String> reviewFlags) {
        this.projectId = projectId;
        this.newStage = newStage;
        this.safeMessage = safeMessage;
        this.sourceConfidence = sourceConfidence;
        this.needsHumanReview = needsHumanReview;
        this.copyrightReviewRequired = copyrightReviewRequired;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
    }

    public UUID getProjectId() { return projectId; }
    public TurkuVideoWorkflowStage getNewStage() { return newStage; }
    public String getSafeMessage() { return safeMessage; }
    public TurkuSourceConfidence getSourceConfidence() { return sourceConfidence; }
    public boolean isNeedsHumanReview() { return needsHumanReview; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
