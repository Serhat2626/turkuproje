package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-024 Task 05: Artifact verification report.
 * Immutable. Safe metadata only — no file paths, binary, secrets.
 */
public final class TurkuRenderArtifactVerificationReport {
    private final String artifactRef;
    private final TurkuRenderArtifactVerificationStatus status;
    private final String rendererMode;
    private final String expectedFormat;
    private final String safeTraceRef;
    private final List<TurkuRenderArtifactVerificationIssue> issues;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final List<String> safetyFlags;

    public TurkuRenderArtifactVerificationReport(
            String artifactRef,
            TurkuRenderArtifactVerificationStatus status,
            String rendererMode, String expectedFormat,
            List<TurkuRenderArtifactVerificationIssue> issues) {
        this.artifactRef = artifactRef;
        this.status = status;
        this.rendererMode = rendererMode;
        this.expectedFormat = expectedFormat;
        this.safeTraceRef = "verification-trace:" + UUID.randomUUID();
        this.issues = issues != null ? List.copyOf(issues) : List.of();
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.safetyFlags = List.of(
                "NO_PUBLISH", "NO_EXPORT", "NO_DOWNLOAD",
                "INTERNAL_ONLY", "HUMAN_REVIEW_REQUIRED");
    }

    public String getArtifactRef() { return artifactRef; }
    public TurkuRenderArtifactVerificationStatus getStatus() { return status; }
    public String getRendererMode() { return rendererMode; }
    public String getExpectedFormat() { return expectedFormat; }
    public String getSafeTraceRef() { return safeTraceRef; }
    public List<TurkuRenderArtifactVerificationIssue> getIssues() { return issues; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
