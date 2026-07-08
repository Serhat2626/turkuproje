package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-049: Safe admin response for internal story-to-video dry-run trigger.
 * Metadata-only. No paths/commands/logs/URLs.
 */
public final class TurkuInternalDryRunTriggerResponse {
    private final String opaqueChainRef;
    private final String opaqueStoryboardRef;
    private final TurkuStoryboardInternalRenderChainStatus status;
    private final List<TurkuStoryboardInternalRenderChainIssueCode> issueCodes;
    private final int sceneCount;
    private final int generatedClipCount;
    private final boolean stitchedArtifactPresent;
    private final String reviewPackageStatus;
    private final boolean adminMetadataVisible;
    private final int totalDurationSeconds;
    private final int maxWidth;
    private final int maxHeight;
    private final int maxFps;
    private final long totalSizeBytes;
    private final boolean internalOnly;

    public TurkuInternalDryRunTriggerResponse(TurkuStoryboardInternalRenderChainSummary summary) {
        this.opaqueChainRef = summary.getOpaqueChainRef();
        this.opaqueStoryboardRef = summary.getOpaqueStoryboardRef();
        this.status = summary.getStatus();
        this.issueCodes = summary.getIssueCodes();
        this.sceneCount = summary.getSceneCount();
        this.generatedClipCount = summary.getGeneratedClipCount();
        this.stitchedArtifactPresent = summary.isStitchedArtifactPresent();
        this.reviewPackageStatus = summary.getReviewPackageStatus() != null
                ? summary.getReviewPackageStatus().name() : null;
        this.adminMetadataVisible = summary.isAdminMetadataVisible();
        this.totalDurationSeconds = summary.getTotalDurationSeconds();
        this.maxWidth = summary.getMaxWidth();
        this.maxHeight = summary.getMaxHeight();
        this.maxFps = summary.getMaxFps();
        this.totalSizeBytes = summary.getTotalSizeBytes();
        this.internalOnly = true;
    }

    public String getOpaqueChainRef() { return opaqueChainRef; }
    public String getOpaqueStoryboardRef() { return opaqueStoryboardRef; }
    public TurkuStoryboardInternalRenderChainStatus getStatus() { return status; }
    public List<TurkuStoryboardInternalRenderChainIssueCode> getIssueCodes() { return issueCodes; }
    public int getSceneCount() { return sceneCount; }
    public int getGeneratedClipCount() { return generatedClipCount; }
    public boolean isStitchedArtifactPresent() { return stitchedArtifactPresent; }
    public String getReviewPackageStatus() { return reviewPackageStatus; }
    public boolean isAdminMetadataVisible() { return adminMetadataVisible; }
    public int getTotalDurationSeconds() { return totalDurationSeconds; }
    public int getMaxWidth() { return maxWidth; }
    public int getMaxHeight() { return maxHeight; }
    public int getMaxFps() { return maxFps; }
    public long getTotalSizeBytes() { return totalSizeBytes; }
    public boolean isInternalOnly() { return internalOnly; }
}
