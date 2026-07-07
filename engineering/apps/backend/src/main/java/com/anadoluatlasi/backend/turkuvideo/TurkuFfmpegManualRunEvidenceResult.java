package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-033: Manual local FFmpeg run evidence result.
 * No stdout/stderr/path/command exposure.
 */
public final class TurkuFfmpegManualRunEvidenceResult {
    private final TurkuFfmpegManualRunEvidenceStatus status;
    private final String outputRef;
    private final TurkuFfmpegFixtureMetadata fixtureMetadata;
    private final long durationMs;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    private TurkuFfmpegManualRunEvidenceResult(TurkuFfmpegManualRunEvidenceStatus status,
            String outputRef, TurkuFfmpegFixtureMetadata metadata, long durationMs) {
        this.status = status;
        this.outputRef = outputRef;
        this.fixtureMetadata = metadata;
        this.durationMs = durationMs;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "LOCAL_ONLY_MANUAL",
                "NO_PUBLISH", "NO_EXPORT", "NO_DOWNLOAD", "NO_PUBLIC_URL");
    }

    public static TurkuFfmpegManualRunEvidenceResult notAttempted(TurkuFfmpegManualRunEvidenceStatus reason) {
        return new TurkuFfmpegManualRunEvidenceResult(reason, null, null, 0);
    }

    public static TurkuFfmpegManualRunEvidenceResult succeeded(String outputRef,
            TurkuFfmpegFixtureMetadata metadata, long durationMs) {
        return new TurkuFfmpegManualRunEvidenceResult(
                TurkuFfmpegManualRunEvidenceStatus.REAL_LOCAL_EXECUTION_SUCCEEDED,
                outputRef, metadata, durationMs);
    }

    public static TurkuFfmpegManualRunEvidenceResult failedSafe() {
        return new TurkuFfmpegManualRunEvidenceResult(
                TurkuFfmpegManualRunEvidenceStatus.REAL_LOCAL_EXECUTION_FAILED_SAFE,
                null, null, 0);
    }

    public TurkuFfmpegManualRunEvidenceStatus getStatus() { return status; }
    public String getOutputRef() { return outputRef; }
    public TurkuFfmpegFixtureMetadata getFixtureMetadata() { return fixtureMetadata; }
    public long getDurationMs() { return durationMs; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
