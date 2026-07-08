package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-044: Internal story-to-video dry run summary. Metadata-only.
 * No public delivery. No paths/commands/logs. No auto-approval.
 */
public final class TurkuInternalStoryVideoDryRunSummary {
    private final String opaqueDryRunRef;
    private final String opaqueJobRef;
    private final String opaqueBundleRef;
    private final String opaqueReviewPackageRef;
    private final TurkuInternalStoryVideoDryRunStatus status;
    private final List<TurkuInternalStoryVideoDryRunIssueCode> issueCodes;
    private final int sceneCount;
    private final int generatedClipCount;
    private final boolean stitchedArtifactPresent;
    private final TurkuRenderReviewPackageStatus reviewPackageStatus;
    private final boolean adminMetadataVisible;
    private final int totalDurationSeconds;
    private final int maxWidth;
    private final int maxHeight;
    private final int maxFps;
    private final long totalSizeBytes;
    private final boolean internalOnly;

    public TurkuInternalStoryVideoDryRunSummary(String opaqueDryRunRef,
            String opaqueJobRef, String opaqueBundleRef, String opaqueReviewPackageRef,
            TurkuInternalStoryVideoDryRunStatus status,
            List<TurkuInternalStoryVideoDryRunIssueCode> issueCodes,
            int sceneCount, int generatedClipCount,
            boolean stitchedArtifactPresent,
            TurkuRenderReviewPackageStatus reviewPackageStatus,
            boolean adminMetadataVisible,
            int totalDurationSeconds, int maxWidth, int maxHeight, int maxFps,
            long totalSizeBytes) {
        this.opaqueDryRunRef = opaqueDryRunRef;
        this.opaqueJobRef = opaqueJobRef;
        this.opaqueBundleRef = opaqueBundleRef;
        this.opaqueReviewPackageRef = opaqueReviewPackageRef;
        this.status = status;
        this.issueCodes = issueCodes != null ? List.copyOf(issueCodes) : List.of();
        this.sceneCount = sceneCount;
        this.generatedClipCount = generatedClipCount;
        this.stitchedArtifactPresent = stitchedArtifactPresent;
        this.reviewPackageStatus = reviewPackageStatus;
        this.adminMetadataVisible = adminMetadataVisible;
        this.totalDurationSeconds = totalDurationSeconds;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxFps = maxFps;
        this.totalSizeBytes = totalSizeBytes;
        this.internalOnly = true;
    }

    public String getOpaqueDryRunRef() { return opaqueDryRunRef; }
    public String getOpaqueJobRef() { return opaqueJobRef; }
    public String getOpaqueBundleRef() { return opaqueBundleRef; }
    public String getOpaqueReviewPackageRef() { return opaqueReviewPackageRef; }
    public TurkuInternalStoryVideoDryRunStatus getStatus() { return status; }
    public List<TurkuInternalStoryVideoDryRunIssueCode> getIssueCodes() { return issueCodes; }
    public int getSceneCount() { return sceneCount; }
    public int getGeneratedClipCount() { return generatedClipCount; }
    public boolean isStitchedArtifactPresent() { return stitchedArtifactPresent; }
    public TurkuRenderReviewPackageStatus getReviewPackageStatus() { return reviewPackageStatus; }
    public boolean isAdminMetadataVisible() { return adminMetadataVisible; }
    public int getTotalDurationSeconds() { return totalDurationSeconds; }
    public int getMaxWidth() { return maxWidth; }
    public int getMaxHeight() { return maxHeight; }
    public int getMaxFps() { return maxFps; }
    public long getTotalSizeBytes() { return totalSizeBytes; }
    public boolean isInternalOnly() { return internalOnly; }

    @Override
    public String toString() {
        return "DryRun{ref=" + opaqueDryRunRef
                + ", status=" + status
                + ", scenes=" + sceneCount
                + ", clips=" + generatedClipCount
                + ", stitched=" + stitchedArtifactPresent
                + ", review=" + reviewPackageStatus
                + ", adminVisible=" + adminMetadataVisible
                + ", internalOnly=" + internalOnly + "}";
    }
}
