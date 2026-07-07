package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-030 Task 02: Smoke run result. No stdout/stderr/path/command.
 */
public final class TurkuFfmpegSmokeRunResult {
    private final TurkuFfmpegSmokeRunStatus status;
    private final String outputRef;
    private final TurkuFfmpegVerificationCandidate verificationCandidate;
    private final long durationMs;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    private TurkuFfmpegSmokeRunResult(TurkuFfmpegSmokeRunStatus status,
            String outputRef, TurkuFfmpegVerificationCandidate candidate, long durationMs) {
        this.status = status;
        this.outputRef = outputRef;
        this.verificationCandidate = candidate;
        this.durationMs = durationMs;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "NO_PUBLIC_URL", "NO_AUTO_APPROVE");
    }

    public static TurkuFfmpegSmokeRunResult denied(TurkuFfmpegSmokeRunStatus status) {
        return new TurkuFfmpegSmokeRunResult(status, null, null, 0);
    }

    public static TurkuFfmpegSmokeRunResult skippedDisabled() {
        return new TurkuFfmpegSmokeRunResult(TurkuFfmpegSmokeRunStatus.EXECUTION_SKIPPED_DISABLED, null, null, 0);
    }

    public static TurkuFfmpegSmokeRunResult skippedUnavailable() {
        return new TurkuFfmpegSmokeRunResult(TurkuFfmpegSmokeRunStatus.EXECUTION_SKIPPED_UNAVAILABLE, null, null, 0);
    }

    public static TurkuFfmpegSmokeRunResult failedSafe() {
        return new TurkuFfmpegSmokeRunResult(TurkuFfmpegSmokeRunStatus.EXECUTION_FAILED_SAFE, null, null, 0);
    }

    public static TurkuFfmpegSmokeRunResult completed(String outputRef,
            TurkuFfmpegVerificationCandidate candidate, long durationMs) {
        return new TurkuFfmpegSmokeRunResult(TurkuFfmpegSmokeRunStatus.EXECUTION_COMPLETED_SAFE,
                outputRef, candidate, durationMs);
    }

    public static TurkuFfmpegSmokeRunResult candidateCreated(String outputRef,
            TurkuFfmpegVerificationCandidate candidate) {
        return new TurkuFfmpegSmokeRunResult(TurkuFfmpegSmokeRunStatus.VERIFICATION_CANDIDATE_CREATED,
                outputRef, candidate, 0);
    }

    public TurkuFfmpegSmokeRunStatus getStatus() { return status; }
    public String getOutputRef() { return outputRef; }
    public TurkuFfmpegVerificationCandidate getVerificationCandidate() { return verificationCandidate; }
    public long getDurationMs() { return durationMs; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
