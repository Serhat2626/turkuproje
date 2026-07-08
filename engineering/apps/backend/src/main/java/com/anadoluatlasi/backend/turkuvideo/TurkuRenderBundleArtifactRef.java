package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-036: Safe artifact reference within a bundle. Metadata-only.
 * No path/command/binary/provider/URL exposure.
 */
public final class TurkuRenderBundleArtifactRef {
    private final String opaqueArtifactRef;
    private final TurkuRenderBundleArtifactRole role;
    private final TurkuRenderArtifactFormat format;
    private final TurkuRenderArtifactVerificationStatus verificationStatus;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;

    public TurkuRenderBundleArtifactRef(String opaqueArtifactRef,
            TurkuRenderBundleArtifactRole role, TurkuRenderArtifactFormat format,
            TurkuRenderArtifactVerificationStatus verificationStatus,
            int durationSeconds, int fps, int width, int height, long sizeBytes) {
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.role = role != null ? role : TurkuRenderBundleArtifactRole.UNKNOWN;
        this.format = format != null ? format : TurkuRenderArtifactFormat.UNKNOWN;
        this.verificationStatus = verificationStatus != null ? verificationStatus
                : TurkuRenderArtifactVerificationStatus.NOT_VERIFIED;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
    }

    /**
     * Build from FP-035 lifecycle report.
     */
    public static TurkuRenderBundleArtifactRef fromLifecycleReport(
            TurkuRenderArtifactLifecycleReport report, TurkuRenderBundleArtifactRole role) {
        if (report == null) return new TurkuRenderBundleArtifactRef(null,
                TurkuRenderBundleArtifactRole.UNKNOWN, TurkuRenderArtifactFormat.UNKNOWN,
                TurkuRenderArtifactVerificationStatus.NOT_VERIFIED, 0, 0, 0, 0, 0);
        return new TurkuRenderBundleArtifactRef(report.getOpaqueArtifactRef(), role,
                report.getFormat(), report.getVerificationStatus(),
                report.getDurationSeconds(), report.getFps(),
                report.getWidth(), report.getHeight(), report.getSizeBytes());
    }

    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public TurkuRenderBundleArtifactRole getRole() { return role; }
    public TurkuRenderArtifactFormat getFormat() { return format; }
    public TurkuRenderArtifactVerificationStatus getVerificationStatus() { return verificationStatus; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }

    public boolean isVerified() {
        return verificationStatus == TurkuRenderArtifactVerificationStatus.PASSED
                || verificationStatus == TurkuRenderArtifactVerificationStatus.VERIFIED;
    }

    public boolean isRejected() {
        return verificationStatus == TurkuRenderArtifactVerificationStatus.REJECTED_FORMAT
                || verificationStatus == TurkuRenderArtifactVerificationStatus.REJECTED_DURATION
                || verificationStatus == TurkuRenderArtifactVerificationStatus.REJECTED_FPS
                || verificationStatus == TurkuRenderArtifactVerificationStatus.REJECTED_RESOLUTION
                || verificationStatus == TurkuRenderArtifactVerificationStatus.REJECTED_SIZE
                || verificationStatus == TurkuRenderArtifactVerificationStatus.REJECTED_MULTIPLE;
    }
}
