package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-039: Multi-clip generation result. Metadata-only, safe.
 * No path/command/binary/provider/URL exposure.
 */
public final class TurkuMultiClipGenerationResult {
    private final String opaqueJobRef;
    private final String opaqueClipRunRef;
    private final TurkuMultiClipGenerationStatus status;
    private final List<TurkuMultiClipGenerationIssueCode> issueCodes;
    private final int sceneCount;
    private final int generatedClipCount;
    private final int verifiedClipCount;
    private final int rejectedClipCount;
    private final int totalDurationSeconds;
    private final int maxWidth;
    private final int maxHeight;
    private final int maxFps;
    private final long aggregateSizeBytes;
    private final List<String> opaqueClipRefs;
    private final boolean internalOnly;
    private final boolean realExecutionPerformed;

    public TurkuMultiClipGenerationResult(String opaqueJobRef, String opaqueClipRunRef,
            TurkuMultiClipGenerationStatus status, List<TurkuMultiClipGenerationIssueCode> issues,
            int sceneCount, int generatedClipCount, int verifiedClipCount, int rejectedClipCount,
            int totalDurationSeconds, int maxWidth, int maxHeight, int maxFps,
            long aggregateSizeBytes, List<String> opaqueClipRefs, boolean realExecutionPerformed) {
        this.opaqueJobRef = opaqueJobRef;
        this.opaqueClipRunRef = opaqueClipRunRef;
        this.status = status;
        this.issueCodes = issues != null ? List.copyOf(issues) : List.of();
        this.sceneCount = sceneCount;
        this.generatedClipCount = generatedClipCount;
        this.verifiedClipCount = verifiedClipCount;
        this.rejectedClipCount = rejectedClipCount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxFps = maxFps;
        this.aggregateSizeBytes = aggregateSizeBytes;
        this.opaqueClipRefs = opaqueClipRefs != null ? List.copyOf(opaqueClipRefs) : List.of();
        this.internalOnly = true;
        this.realExecutionPerformed = realExecutionPerformed;
    }

    public static TurkuMultiClipGenerationResult gateDenied(String jobRef,
            TurkuMultiClipGenerationStatus reason) {
        return new TurkuMultiClipGenerationResult(jobRef, null, reason, List.of(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), false);
    }

    public static TurkuMultiClipGenerationResult failedSafe(String jobRef,
            List<TurkuMultiClipGenerationIssueCode> issues) {
        return new TurkuMultiClipGenerationResult(jobRef, null,
                TurkuMultiClipGenerationStatus.FAILED_SAFE, issues,
                0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), false);
    }

    public String getOpaqueJobRef() { return opaqueJobRef; }
    public String getOpaqueClipRunRef() { return opaqueClipRunRef; }
    public TurkuMultiClipGenerationStatus getStatus() { return status; }
    public List<TurkuMultiClipGenerationIssueCode> getIssueCodes() { return issueCodes; }
    public int getSceneCount() { return sceneCount; }
    public int getGeneratedClipCount() { return generatedClipCount; }
    public int getVerifiedClipCount() { return verifiedClipCount; }
    public int getRejectedClipCount() { return rejectedClipCount; }
    public int getTotalDurationSeconds() { return totalDurationSeconds; }
    public int getMaxWidth() { return maxWidth; }
    public int getMaxHeight() { return maxHeight; }
    public int getMaxFps() { return maxFps; }
    public long getAggregateSizeBytes() { return aggregateSizeBytes; }
    public List<String> getOpaqueClipRefs() { return opaqueClipRefs; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isRealExecutionPerformed() { return realExecutionPerformed; }

    @Override
    public String toString() {
        return "MultiClipResult{job=" + opaqueJobRef
                + ", status=" + status
                + ", scenes=" + sceneCount
                + ", generated=" + generatedClipCount
                + ", verified=" + verifiedClipCount
                + ", rejected=" + rejectedClipCount
                + ", real=" + realExecutionPerformed + "}";
    }
}
