package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-035: Metadata-only lifecycle verification report.
 * Separate from FP-024's TurkuRenderArtifactVerificationReport.
 * No path/command/stdout/stderr/binary/provider/storage/URL exposure.
 */
public final class TurkuRenderArtifactLifecycleReport {
    private final String opaqueArtifactRef;
    private final TurkuRenderArtifactFormat format;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final TurkuRenderArtifactVerificationStatus verificationStatus;
    private final List<TurkuRenderArtifactIssueCode> issueCodes;

    public TurkuRenderArtifactLifecycleReport(String opaqueArtifactRef,
            TurkuRenderArtifactFormat format, int durationSeconds, int fps,
            int width, int height, long sizeBytes,
            TurkuRenderArtifactVerificationStatus status,
            List<TurkuRenderArtifactIssueCode> issues) {
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.format = format != null ? format : TurkuRenderArtifactFormat.UNKNOWN;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.verificationStatus = status;
        this.issueCodes = issues != null ? List.copyOf(issues) : List.of();
    }

    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public TurkuRenderArtifactFormat getFormat() { return format; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
    public TurkuRenderArtifactVerificationStatus getVerificationStatus() { return verificationStatus; }
    public List<TurkuRenderArtifactIssueCode> getIssueCodes() { return issueCodes; }

    @Override
    public String toString() {
        return "LifecycleReport{ref=" + opaqueArtifactRef
                + ", format=" + format
                + ", status=" + verificationStatus
                + ", issues=" + issueCodes + "}";
    }
}
