package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-040: Clip stitching result. Metadata-only.
 */
public final class TurkuClipStitchingResult {
    private final String opaqueStitchRef;
    private final TurkuClipStitchingStatus status;
    private final List<TurkuClipStitchingIssueCode> issueCodes;
    private final int inputClipCount;
    private final int totalDurationSeconds;
    private final int maxWidth;
    private final int maxHeight;
    private final int maxFps;
    private final long estimatedOutputSizeBytes;
    private final String outputFormat;
    private final String opaqueOutputRef;
    private final boolean internalOnly;
    private final boolean realExecutionPerformed;
    private final boolean stitchedArtifactVerified;

    public TurkuClipStitchingResult(String opaqueStitchRef, TurkuClipStitchingStatus status,
            List<TurkuClipStitchingIssueCode> issues, int inputClipCount,
            int totalDurationSeconds, int maxWidth, int maxHeight, int maxFps,
            long estimatedOutputSizeBytes, String outputFormat, String opaqueOutputRef,
            boolean realExecutionPerformed, boolean stitchedArtifactVerified) {
        this.opaqueStitchRef = opaqueStitchRef;
        this.status = status;
        this.issueCodes = issues != null ? List.copyOf(issues) : List.of();
        this.inputClipCount = inputClipCount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxFps = maxFps;
        this.estimatedOutputSizeBytes = estimatedOutputSizeBytes;
        this.outputFormat = outputFormat;
        this.opaqueOutputRef = opaqueOutputRef;
        this.internalOnly = true;
        this.realExecutionPerformed = realExecutionPerformed;
        this.stitchedArtifactVerified = stitchedArtifactVerified;
    }

    public static TurkuClipStitchingResult gateDenied(TurkuClipStitchingStatus reason) {
        return new TurkuClipStitchingResult(null, reason, List.of(),
                0, 0, 0, 0, 0, 0, null, null, false, false);
    }

    public static TurkuClipStitchingResult rejected(String ref,
            List<TurkuClipStitchingIssueCode> issues) {
        return new TurkuClipStitchingResult(ref, TurkuClipStitchingStatus.REJECTED_UNSAFE,
                issues, 0, 0, 0, 0, 0, 0, null, null, false, false);
    }

    public String getOpaqueStitchRef() { return opaqueStitchRef; }
    public TurkuClipStitchingStatus getStatus() { return status; }
    public List<TurkuClipStitchingIssueCode> getIssueCodes() { return issueCodes; }
    public int getInputClipCount() { return inputClipCount; }
    public int getTotalDurationSeconds() { return totalDurationSeconds; }
    public int getMaxWidth() { return maxWidth; }
    public int getMaxHeight() { return maxHeight; }
    public int getMaxFps() { return maxFps; }
    public long getEstimatedOutputSizeBytes() { return estimatedOutputSizeBytes; }
    public String getOutputFormat() { return outputFormat; }
    public String getOpaqueOutputRef() { return opaqueOutputRef; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isRealExecutionPerformed() { return realExecutionPerformed; }
    public boolean isStitchedArtifactVerified() { return stitchedArtifactVerified; }

    @Override
    public String toString() {
        return "StitchingResult{ref=" + opaqueStitchRef
                + ", status=" + status
                + ", clips=" + inputClipCount
                + ", duration=" + totalDurationSeconds + "s"
                + ", verified=" + stitchedArtifactVerified
                + ", real=" + realExecutionPerformed + "}";
    }
}
