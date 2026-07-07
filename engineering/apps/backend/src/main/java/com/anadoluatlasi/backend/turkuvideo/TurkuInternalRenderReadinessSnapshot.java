package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-025 Task 03: Readiness snapshot.
 * Safe, bounded, no forbidden exposure.
 */
public final class TurkuInternalRenderReadinessSnapshot {
    private final TurkuInternalRenderReadinessStatus status;
    private final List<TurkuInternalRenderReadinessIssue> issues;
    private final boolean humanReviewRequired;
    private final boolean autoApproved;
    private final boolean publicDeliveryAvailable;
    private final List<String> safetyFlags;

    public TurkuInternalRenderReadinessSnapshot(TurkuInternalRenderReadinessStatus status,
                                                  List<TurkuInternalRenderReadinessIssue> issues) {
        this.status = status;
        this.issues = issues != null ? List.copyOf(issues) : List.of();
        this.humanReviewRequired = true;
        this.autoApproved = false;
        this.publicDeliveryAvailable = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "HUMAN_REVIEW_REQUIRED");
    }

    public TurkuInternalRenderReadinessStatus getStatus() { return status; }
    public List<TurkuInternalRenderReadinessIssue> getIssues() { return issues; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
