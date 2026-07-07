package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-028 Task 01: Safe execution boundary result.
 * No stdout/stderr/raw log/command line/paths/binary.
 */
public final class TurkuFfmpegExecutionBoundaryResult {
    private final TurkuFfmpegBoundaryResultStatus status;
    private final TurkuFfmpegBoundaryReasonCode reasonCode;
    private final TurkuFfmpegRealExecutionMode mode;
    private final TurkuFfmpegExecutionEnvironment environment;
    private final String jobRef;
    private final String queueItemRef;
    private final String reviewRef;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    private TurkuFfmpegExecutionBoundaryResult(TurkuFfmpegBoundaryResultStatus status,
            TurkuFfmpegBoundaryReasonCode reasonCode, TurkuFfmpegRealExecutionMode mode,
            TurkuFfmpegExecutionEnvironment environment,
            String jobRef, String queueItemRef, String reviewRef) {
        this.status = status;
        this.reasonCode = reasonCode;
        this.mode = mode;
        this.environment = environment;
        this.jobRef = jobRef;
        this.queueItemRef = queueItemRef;
        this.reviewRef = reviewRef;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "NO_PUBLIC_DELIVERY", "NO_AUTO_APPROVE");
    }

    public static TurkuFfmpegExecutionBoundaryResult denied(TurkuFfmpegBoundaryReasonCode reason,
            TurkuFfmpegRealExecutionMode mode, TurkuFfmpegExecutionEnvironment env) {
        return new TurkuFfmpegExecutionBoundaryResult(TurkuFfmpegBoundaryResultStatus.DENIED,
                reason, mode, env, null, null, null);
    }

    public static TurkuFfmpegExecutionBoundaryResult safe(TurkuFfmpegBoundaryResultStatus status,
            TurkuFfmpegBoundaryReasonCode reason, TurkuFfmpegRealExecutionMode mode,
            TurkuFfmpegExecutionEnvironment env, String jobRef, String queueRef, String reviewRef) {
        return new TurkuFfmpegExecutionBoundaryResult(status, reason, mode, env, jobRef, queueRef, reviewRef);
    }

    public TurkuFfmpegBoundaryResultStatus getStatus() { return status; }
    public TurkuFfmpegBoundaryReasonCode getReasonCode() { return reasonCode; }
    public TurkuFfmpegRealExecutionMode getMode() { return mode; }
    public TurkuFfmpegExecutionEnvironment getEnvironment() { return environment; }
    public String getJobRef() { return jobRef; }
    public String getQueueItemRef() { return queueItemRef; }
    public String getReviewRef() { return reviewRef; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
