package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-032: Actual local smoke execution evidence result.
 * No stdout/stderr/path/command exposure.
 */
public final class TurkuFfmpegActualRunEvidenceResult {
    private final TurkuFfmpegActualRunEvidenceStatus status;
    private final TurkuFfmpegLocalAvailabilityStatus availabilityStatus;
    private final String outputRef;
    private final TurkuFfmpegFixtureMetadata fixtureMetadata;
    private final long durationMs;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    private TurkuFfmpegActualRunEvidenceResult(TurkuFfmpegActualRunEvidenceStatus status,
            TurkuFfmpegLocalAvailabilityStatus availability,
            String outputRef, TurkuFfmpegFixtureMetadata metadata, long durationMs) {
        this.status = status;
        this.availabilityStatus = availability;
        this.outputRef = outputRef;
        this.fixtureMetadata = metadata;
        this.durationMs = durationMs;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "LOCAL_ONLY", "NO_PUBLISH",
                "NO_EXPORT", "NO_DOWNLOAD", "NO_PUBLIC_URL", "NO_AUTO_APPROVE");
    }

    public static TurkuFfmpegActualRunEvidenceResult unavailable() {
        return new TurkuFfmpegActualRunEvidenceResult(
                TurkuFfmpegActualRunEvidenceStatus.FFMPEG_UNAVAILABLE,
                TurkuFfmpegLocalAvailabilityStatus.UNAVAILABLE, null, null, 0);
    }

    public static TurkuFfmpegActualRunEvidenceResult disabled() {
        return new TurkuFfmpegActualRunEvidenceResult(
                TurkuFfmpegActualRunEvidenceStatus.REAL_LOCAL_RUN_SKIPPED_DISABLED,
                TurkuFfmpegLocalAvailabilityStatus.DISABLED, null, null, 0);
    }

    public static TurkuFfmpegActualRunEvidenceResult unsupportedEnv() {
        return new TurkuFfmpegActualRunEvidenceResult(
                TurkuFfmpegActualRunEvidenceStatus.REAL_LOCAL_RUN_SKIPPED_UNSUPPORTED_ENVIRONMENT,
                TurkuFfmpegLocalAvailabilityStatus.UNSUPPORTED_ENVIRONMENT, null, null, 0);
    }

    public static TurkuFfmpegActualRunEvidenceResult gateDenied() {
        return new TurkuFfmpegActualRunEvidenceResult(
                TurkuFfmpegActualRunEvidenceStatus.GATE_DENIED, null, null, null, 0);
    }

    public static TurkuFfmpegActualRunEvidenceResult succeeded(String outputRef,
            TurkuFfmpegFixtureMetadata metadata, long durationMs) {
        return new TurkuFfmpegActualRunEvidenceResult(
                TurkuFfmpegActualRunEvidenceStatus.REAL_LOCAL_RUN_SUCCEEDED,
                TurkuFfmpegLocalAvailabilityStatus.AVAILABLE, outputRef, metadata, durationMs);
    }

    public static TurkuFfmpegActualRunEvidenceResult failedSafe() {
        return new TurkuFfmpegActualRunEvidenceResult(
                TurkuFfmpegActualRunEvidenceStatus.REAL_LOCAL_RUN_FAILED_SAFE,
                TurkuFfmpegLocalAvailabilityStatus.AVAILABLE, null, null, 0);
    }

    public TurkuFfmpegActualRunEvidenceStatus getStatus() { return status; }
    public TurkuFfmpegLocalAvailabilityStatus getAvailabilityStatus() { return availabilityStatus; }
    public String getOutputRef() { return outputRef; }
    public TurkuFfmpegFixtureMetadata getFixtureMetadata() { return fixtureMetadata; }
    public long getDurationMs() { return durationMs; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
