package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-019 Task 04: Response for image batch preparation/completion.
 * Safe internal references only — no base64, no URLs, no storage keys.
 */
public final class TurkuImageBatchResponse {
    private final UUID projectId;
    private final UUID batchId;
    private final TurkuImageBatchStatus batchStatus;
    private final TurkuVideoWorkflowStage newStage;
    private final String safeMessage;
    private final int requestCount;
    private final int resultCount;
    private final boolean needsHumanReview;
    private final boolean copyrightReviewRequired;
    private final List<String> reviewFlags;

    public TurkuImageBatchResponse(UUID projectId, UUID batchId,
                                    TurkuImageBatchStatus batchStatus,
                                    TurkuVideoWorkflowStage newStage,
                                    String safeMessage, int requestCount, int resultCount,
                                    boolean needsHumanReview, boolean copyrightReviewRequired,
                                    List<String> reviewFlags) {
        this.projectId = projectId;
        this.batchId = batchId;
        this.batchStatus = batchStatus;
        this.newStage = newStage;
        this.safeMessage = safeMessage;
        this.requestCount = requestCount;
        this.resultCount = resultCount;
        this.needsHumanReview = needsHumanReview;
        this.copyrightReviewRequired = copyrightReviewRequired;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
    }

    public UUID getProjectId() { return projectId; }
    public UUID getBatchId() { return batchId; }
    public TurkuImageBatchStatus getBatchStatus() { return batchStatus; }
    public TurkuVideoWorkflowStage getNewStage() { return newStage; }
    public String getSafeMessage() { return safeMessage; }
    public int getRequestCount() { return requestCount; }
    public int getResultCount() { return resultCount; }
    public boolean isNeedsHumanReview() { return needsHumanReview; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
