package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-027 Task 02: Safe queue summary DTO.
 */
public final class TurkuInternalRenderQueueSummary {
    private final String queueItemRef;
    private final String jobRef;
    private final String reviewRef;
    private final TurkuInternalRenderQueueState queueState;
    private final TurkuInternalRenderQueueReasonCode reasonCode;
    private final int attemptNumber;
    private final int maxAttempts;
    private final boolean retriable;
    private final boolean expired;
    private final boolean humanReviewRequired;
    private final boolean autoApproved;
    private final boolean publicDeliveryAvailable;
    private final boolean internalOnly;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final List<String> safetyFlags;

    public TurkuInternalRenderQueueSummary(String queueItemRef, String jobRef, String reviewRef,
            TurkuInternalRenderQueueState queueState, TurkuInternalRenderQueueReasonCode reasonCode,
            int attemptNumber, int maxAttempts, boolean retriable, boolean expired) {
        this.queueItemRef = queueItemRef;
        this.jobRef = jobRef;
        this.reviewRef = reviewRef;
        this.queueState = queueState;
        this.reasonCode = reasonCode;
        this.attemptNumber = attemptNumber;
        this.maxAttempts = maxAttempts;
        this.retriable = retriable;
        this.expired = expired;
        this.humanReviewRequired = true;
        this.autoApproved = false;
        this.publicDeliveryAvailable = false;
        this.internalOnly = true;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "HUMAN_REVIEW_REQUIRED", "NO_AUTO_APPROVE");
    }

    public String getQueueItemRef() { return queueItemRef; }
    public String getJobRef() { return jobRef; }
    public String getReviewRef() { return reviewRef; }
    public TurkuInternalRenderQueueState getQueueState() { return queueState; }
    public TurkuInternalRenderQueueReasonCode getReasonCode() { return reasonCode; }
    public int getAttemptNumber() { return attemptNumber; }
    public int getMaxAttempts() { return maxAttempts; }
    public boolean isRetriable() { return retriable; }
    public boolean isExpired() { return expired; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
