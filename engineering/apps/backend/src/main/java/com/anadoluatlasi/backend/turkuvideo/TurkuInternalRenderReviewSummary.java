package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-026 Task 02: Safe review summary DTO.
 * Internal-only. No forbidden exposure fields.
 */
public final class TurkuInternalRenderReviewSummary {
    private final String reviewRef;
    private final String jobRef;
    private final TurkuInternalRenderReviewState reviewState;
    private final TurkuInternalRenderReviewReasonCode reasonCode;
    private final boolean humanReviewRequired;
    private final boolean autoApproved;
    private final boolean publicDeliveryAvailable;
    private final boolean internalOnly;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final List<String> safetyFlags;

    public TurkuInternalRenderReviewSummary(String reviewRef, String jobRef,
            TurkuInternalRenderReviewState reviewState,
            TurkuInternalRenderReviewReasonCode reasonCode) {
        this.reviewRef = reviewRef;
        this.jobRef = jobRef;
        this.reviewState = reviewState;
        this.reasonCode = reasonCode;
        this.humanReviewRequired = true;
        this.autoApproved = false;
        this.publicDeliveryAvailable = false;
        this.internalOnly = true;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "HUMAN_REVIEW_REQUIRED", "NO_AUTO_APPROVE");
    }

    public String getReviewRef() { return reviewRef; }
    public String getJobRef() { return jobRef; }
    public TurkuInternalRenderReviewState getReviewState() { return reviewState; }
    public TurkuInternalRenderReviewReasonCode getReasonCode() { return reasonCode; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
