package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-019 Task 04: Response for asset plan preparation.
 */
public final class TurkuAssetPreparationResponse {
    private final UUID projectId;
    private final TurkuVideoWorkflowStage newStage;
    private final String safeMessage;
    private final int assetCount;
    private final boolean needsHumanReview;
    private final boolean copyrightReviewRequired;
    private final List<String> reviewFlags;

    public TurkuAssetPreparationResponse(UUID projectId, TurkuVideoWorkflowStage newStage,
                                          String safeMessage, int assetCount,
                                          boolean needsHumanReview, boolean copyrightReviewRequired,
                                          List<String> reviewFlags) {
        this.projectId = projectId;
        this.newStage = newStage;
        this.safeMessage = safeMessage;
        this.assetCount = assetCount;
        this.needsHumanReview = needsHumanReview;
        this.copyrightReviewRequired = copyrightReviewRequired;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
    }

    public UUID getProjectId() { return projectId; }
    public TurkuVideoWorkflowStage getNewStage() { return newStage; }
    public String getSafeMessage() { return safeMessage; }
    public int getAssetCount() { return assetCount; }
    public boolean isNeedsHumanReview() { return needsHumanReview; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
