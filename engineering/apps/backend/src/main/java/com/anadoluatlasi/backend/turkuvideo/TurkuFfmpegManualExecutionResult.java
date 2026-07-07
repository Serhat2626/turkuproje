package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-031 Task 01: Safe manual execution result. No stdout/stderr/path/command.
 */
public final class TurkuFfmpegManualExecutionResult {
    private final TurkuFfmpegManualExecutionStatus status;
    private final TurkuFfmpegManualExecutionReasonCode reasonCode;
    private final String outputRef;
    private final TurkuFfmpegFixtureMetadata fixtureMetadata;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    private TurkuFfmpegManualExecutionResult(TurkuFfmpegManualExecutionStatus status,
            TurkuFfmpegManualExecutionReasonCode reasonCode, String outputRef,
            TurkuFfmpegFixtureMetadata metadata) {
        this.status = status;
        this.reasonCode = reasonCode;
        this.outputRef = outputRef;
        this.fixtureMetadata = metadata;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "LOCAL_ONLY_MANUAL", "NO_PUBLISH",
                "NO_EXPORT", "NO_DOWNLOAD", "NO_PUBLIC_URL", "NO_AUTO_APPROVE");
    }

    public static TurkuFfmpegManualExecutionResult denied(TurkuFfmpegManualExecutionStatus status,
            TurkuFfmpegManualExecutionReasonCode reason) {
        return new TurkuFfmpegManualExecutionResult(status, reason, null, null);
    }

    public static TurkuFfmpegManualExecutionResult skipped() {
        return new TurkuFfmpegManualExecutionResult(TurkuFfmpegManualExecutionStatus.EXECUTION_SKIPPED_FAKE,
                TurkuFfmpegManualExecutionReasonCode.EXECUTION_SKIPPED_SAFE, null, null);
    }

    public static TurkuFfmpegManualExecutionResult completed(String outputRef, TurkuFfmpegFixtureMetadata metadata) {
        return new TurkuFfmpegManualExecutionResult(TurkuFfmpegManualExecutionStatus.EXECUTION_COMPLETED_SAFE,
                TurkuFfmpegManualExecutionReasonCode.EXECUTION_COMPLETED_SAFE, outputRef, metadata);
    }

    public static TurkuFfmpegManualExecutionResult failedSafe() {
        return new TurkuFfmpegManualExecutionResult(TurkuFfmpegManualExecutionStatus.EXECUTION_FAILED_SAFE,
                TurkuFfmpegManualExecutionReasonCode.EXECUTION_FAILED_INTERNAL, null, null);
    }

    public static TurkuFfmpegManualExecutionResult verificationPassed(String outputRef, TurkuFfmpegFixtureMetadata metadata) {
        return new TurkuFfmpegManualExecutionResult(TurkuFfmpegManualExecutionStatus.VERIFICATION_PASSED,
                TurkuFfmpegManualExecutionReasonCode.VERIFICATION_PASSED_SAFE, outputRef, metadata);
    }

    public static TurkuFfmpegManualExecutionResult verificationFailed() {
        return new TurkuFfmpegManualExecutionResult(TurkuFfmpegManualExecutionStatus.VERIFICATION_FAILED,
                TurkuFfmpegManualExecutionReasonCode.VERIFICATION_BOUNDS_FAILED, null, null);
    }

    public TurkuFfmpegManualExecutionStatus getStatus() { return status; }
    public TurkuFfmpegManualExecutionReasonCode getReasonCode() { return reasonCode; }
    public String getOutputRef() { return outputRef; }
    public TurkuFfmpegFixtureMetadata getFixtureMetadata() { return fixtureMetadata; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
