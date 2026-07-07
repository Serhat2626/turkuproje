package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-023 Task 04: Render artifact safety report.
 * Immutable safety flags. No file/URL/secret exposure.
 */
public final class TurkuRenderArtifactSafetyReport {
    private final UUID artifactId;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final boolean notPublishReady;
    private final boolean notExportReady;
    private final TurkuRenderArtifactStatus status;
    private final TurkuRenderAdapterMode rendererMode;
    private final List<String> safetyFlags;
    private final List<String> reviewFlags;

    public TurkuRenderArtifactSafetyReport(UUID artifactId,
                                             TurkuRenderArtifactStatus status,
                                             TurkuRenderAdapterMode rendererMode) {
        this.artifactId = artifactId;
        this.humanReviewRequired = true;
        this.copyrightReviewRequired = true;
        this.autoApproved = false;
        this.notPublishReady = true;
        this.notExportReady = true;
        this.status = status;
        this.rendererMode = rendererMode;
        this.safetyFlags = List.of(
                "NO_DOWNLOAD", "NO_EXPORT", "NO_PUBLISH",
                "NO_PUBLIC_DELIVERY", "NO_BINARY_EXPOSURE",
                "OPAQUE_REF_ONLY", "INTERNAL_ONLY");
        this.reviewFlags = List.of(
                "HUMAN_REVIEW_REQUIRED", "COPYRIGHT_REVIEW_REQUIRED",
                "NOT_PUBLISH_READY", "NOT_EXPORT_READY");
    }

    public UUID getArtifactId() { return artifactId; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public boolean isNotPublishReady() { return notPublishReady; }
    public boolean isNotExportReady() { return notExportReady; }
    public TurkuRenderArtifactStatus getStatus() { return status; }
    public TurkuRenderAdapterMode getRendererMode() { return rendererMode; }
    public List<String> getSafetyFlags() { return safetyFlags; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
