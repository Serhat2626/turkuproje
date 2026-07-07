package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-026 Task 03: Review readiness snapshot derived from FP-025 readiness/safety-chain.
 */
public final class TurkuInternalRenderReviewReadinessSnapshot {
    private final TurkuInternalRenderReviewReadinessStatus status;
    private final List<TurkuInternalRenderReviewReadinessIssue> issues;
    private final boolean humanReviewRequired;
    private final boolean autoApproved;
    private final boolean publicDeliveryAvailable;
    private final boolean internalOnly;
    private final List<String> safetyFlags;

    public TurkuInternalRenderReviewReadinessSnapshot(
            TurkuInternalRenderReviewReadinessStatus status,
            List<TurkuInternalRenderReviewReadinessIssue> issues) {
        this.status = status;
        this.issues = issues != null ? List.copyOf(issues) : List.of();
        this.humanReviewRequired = true;
        this.autoApproved = false;
        this.publicDeliveryAvailable = false;
        this.internalOnly = true;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "HUMAN_REVIEW_REQUIRED", "NO_AUTO_APPROVE");
    }

    /**
     * Derive review readiness from FP-025 readiness + safety-chain snapshots.
     */
    public static TurkuInternalRenderReviewReadinessSnapshot fromFP025(
            TurkuInternalRenderReadinessSnapshot readiness,
            TurkuInternalRenderSafetyChainSnapshot safetyChain) {

        List<TurkuInternalRenderReviewReadinessIssue> issues = new ArrayList<>();
        issues.add(TurkuInternalRenderReviewReadinessIssue.HUMAN_REVIEW_REQUIRED);
        issues.add(TurkuInternalRenderReviewReadinessIssue.PUBLIC_DELIVERY_FORBIDDEN);
        issues.add(TurkuInternalRenderReviewReadinessIssue.AUTO_APPROVAL_FORBIDDEN);

        boolean jobReady = readiness != null
                && readiness.getStatus() == TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY;
        boolean chainSafe = safetyChain != null
                && safetyChain.isConfigValid()
                && safetyChain.isWorkspaceSafe()
                && safetyChain.isExecutionBoundarySafe()
                && safetyChain.isArtifactVerified();

        if (!jobReady) {
            issues.add(TurkuInternalRenderReviewReadinessIssue.JOB_NOT_READY);
            if (readiness != null && readiness.getStatus() != TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY) {
                issues.add(TurkuInternalRenderReviewReadinessIssue.READINESS_BLOCKED);
            }
        }
        if (!chainSafe) {
            if (safetyChain != null && !safetyChain.isArtifactVerified()) {
                issues.add(TurkuInternalRenderReviewReadinessIssue.ARTIFACT_VERIFICATION_FAILED);
            }
        }

        TurkuInternalRenderReviewReadinessStatus status;
        if (jobReady && chainSafe) {
            status = TurkuInternalRenderReviewReadinessStatus.READY_FOR_HUMAN_REVIEW;
        } else {
            status = TurkuInternalRenderReviewReadinessStatus.NOT_READY_FOR_REVIEW;
        }

        return new TurkuInternalRenderReviewReadinessSnapshot(status, issues);
    }

    public TurkuInternalRenderReviewReadinessStatus getStatus() { return status; }
    public List<TurkuInternalRenderReviewReadinessIssue> getIssues() { return issues; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isInternalOnly() { return internalOnly; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
