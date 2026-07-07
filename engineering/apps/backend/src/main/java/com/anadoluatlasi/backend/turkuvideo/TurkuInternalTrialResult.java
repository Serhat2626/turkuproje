package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-029 Task 02: Trial result — safe wrapper.
 */
public final class TurkuInternalTrialResult {
    private final TurkuInternalTrialStatus status;
    private final TurkuInternalTrialReasonCode reasonCode;
    private final String outputRef;
    private final TurkuFfmpegVerificationCandidate verificationCandidate;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    private TurkuInternalTrialResult(TurkuInternalTrialStatus status,
            TurkuInternalTrialReasonCode reasonCode, String outputRef,
            TurkuFfmpegVerificationCandidate candidate) {
        this.status = status;
        this.reasonCode = reasonCode;
        this.outputRef = outputRef;
        this.verificationCandidate = candidate;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "NO_PUBLIC_URL", "NO_AUTO_APPROVE");
    }

    public static TurkuInternalTrialResult denied(TurkuInternalTrialReasonCode reason) {
        return new TurkuInternalTrialResult(TurkuInternalTrialStatus.GATE_DENIED, reason, null, null);
    }

    public static TurkuInternalTrialResult skipped(TurkuInternalTrialReasonCode reason) {
        return new TurkuInternalTrialResult(TurkuInternalTrialStatus.EXECUTION_SKIPPED, reason, null, null);
    }

    public static TurkuInternalTrialResult simulated(String outputRef, TurkuFfmpegVerificationCandidate candidate) {
        return new TurkuInternalTrialResult(TurkuInternalTrialStatus.EXECUTION_SIMULATED,
                TurkuInternalTrialReasonCode.EXECUTION_SIMULATED_SAFE, outputRef, candidate);
    }

    public static TurkuInternalTrialResult attempted(String outputRef, TurkuFfmpegVerificationCandidate candidate) {
        return new TurkuInternalTrialResult(TurkuInternalTrialStatus.EXECUTION_ATTEMPTED_DEV_TEST,
                TurkuInternalTrialReasonCode.EXECUTION_ATTEMPTED_SAFE, outputRef, candidate);
    }

    public static TurkuInternalTrialResult candidateCreated(String outputRef, TurkuFfmpegVerificationCandidate candidate) {
        return new TurkuInternalTrialResult(TurkuInternalTrialStatus.VERIFICATION_CANDIDATE_CREATED,
                TurkuInternalTrialReasonCode.CANDIDATE_CREATED_SAFE, outputRef, candidate);
    }

    public static TurkuInternalTrialResult planInvalid() {
        return new TurkuInternalTrialResult(TurkuInternalTrialStatus.PLAN_INVALID,
                TurkuInternalTrialReasonCode.PLAN_BOUNDS_INVALID, null, null);
    }

    public TurkuInternalTrialStatus getStatus() { return status; }
    public TurkuInternalTrialReasonCode getReasonCode() { return reasonCode; }
    public String getOutputRef() { return outputRef; }
    public TurkuFfmpegVerificationCandidate getVerificationCandidate() { return verificationCandidate; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
