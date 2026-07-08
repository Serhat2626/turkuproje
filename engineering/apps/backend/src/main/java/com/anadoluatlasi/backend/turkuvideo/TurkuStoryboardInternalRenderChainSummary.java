package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-047: Storyboard internal render chain summary. Metadata-only.
 * No public delivery. No paths/commands/logs. No auto-approval.
 */
public final class TurkuStoryboardInternalRenderChainSummary {
    private final String opaqueChainRef;
    private final String opaqueStoryboardRef;
    private final String opaqueJobRef;
    private final String opaqueBundleRef;
    private final String opaqueReviewPackageRef;
    private final TurkuStoryboardInternalRenderChainStatus status;
    private final List<TurkuStoryboardInternalRenderChainIssueCode> issueCodes;
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

    public TurkuStoryboardInternalRenderChainSummary(String opaqueChainRef,
            String opaqueStoryboardRef, String opaqueJobRef,
            String opaqueBundleRef, String opaqueReviewPackageRef,
            TurkuStoryboardInternalRenderChainStatus status,
            List<TurkuStoryboardInternalRenderChainIssueCode> issueCodes,
            int sceneCount, int generatedClipCount,
            boolean stitchedArtifactPresent,
            TurkuRenderReviewPackageStatus reviewPackageStatus,
            boolean adminMetadataVisible,
            int totalDurationSeconds, int maxWidth, int maxHeight, int maxFps,
            long totalSizeBytes) {
        this.opaqueChainRef = opaqueChainRef;
        this.opaqueStoryboardRef = opaqueStoryboardRef;
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

    public String getOpaqueChainRef() { return opaqueChainRef; }
    public String getOpaqueStoryboardRef() { return opaqueStoryboardRef; }
    public String getOpaqueJobRef() { return opaqueJobRef; }
    public String getOpaqueBundleRef() { return opaqueBundleRef; }
    public String getOpaqueReviewPackageRef() { return opaqueReviewPackageRef; }
    public TurkuStoryboardInternalRenderChainStatus getStatus() { return status; }
    public List<TurkuStoryboardInternalRenderChainIssueCode> getIssueCodes() { return issueCodes; }
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
        return "ChainSummary{ref=" + opaqueChainRef
                + ", status=" + status
                + ", scenes=" + sceneCount
                + ", clips=" + generatedClipCount
                + ", stitched=" + stitchedArtifactPresent
                + ", review=" + reviewPackageStatus
                + ", adminVisible=" + adminMetadataVisible
                + ", internalOnly=" + internalOnly + "}";
    }
}
