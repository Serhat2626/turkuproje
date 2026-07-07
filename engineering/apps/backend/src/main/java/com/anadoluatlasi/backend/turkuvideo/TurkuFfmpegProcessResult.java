package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-030 Task 01: Safe process result. No stdout/stderr/rawLog/path.
 */
public final class TurkuFfmpegProcessResult {
    private final TurkuFfmpegProcessStatus status;
    private final TurkuFfmpegProcessReasonCode reasonCode;
    private final String outputRef;
    private final long durationMs;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    private TurkuFfmpegProcessResult(TurkuFfmpegProcessStatus status,
            TurkuFfmpegProcessReasonCode reasonCode, String outputRef, long durationMs) {
        this.status = status;
        this.reasonCode = reasonCode;
        this.outputRef = outputRef;
        this.durationMs = durationMs;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "NO_PUBLIC_URL", "NO_AUTO_APPROVE");
    }

    public static TurkuFfmpegProcessResult denied(TurkuFfmpegProcessReasonCode reason) {
        return new TurkuFfmpegProcessResult(TurkuFfmpegProcessStatus.GATE_DENIED, reason, null, 0);
    }

    public static TurkuFfmpegProcessResult invalid(TurkuFfmpegProcessReasonCode reason) {
        return new TurkuFfmpegProcessResult(TurkuFfmpegProcessStatus.REQUEST_INVALID, reason, null, 0);
    }

    public static TurkuFfmpegProcessResult completed(String outputRef, long durationMs) {
        return new TurkuFfmpegProcessResult(TurkuFfmpegProcessStatus.PROCESS_COMPLETED_SAFE,
                TurkuFfmpegProcessReasonCode.PROCESS_COMPLETED_SAFE, outputRef, durationMs);
    }

    public static TurkuFfmpegProcessResult timedOut(long durationMs) {
        return new TurkuFfmpegProcessResult(TurkuFfmpegProcessStatus.PROCESS_TIMED_OUT,
                TurkuFfmpegProcessReasonCode.PROCESS_TIMED_OUT_SAFE, null, durationMs);
    }

    public static TurkuFfmpegProcessResult failed() {
        return new TurkuFfmpegProcessResult(TurkuFfmpegProcessStatus.PROCESS_FAILED_SAFE,
                TurkuFfmpegProcessReasonCode.PROCESS_FAILED_INTERNAL, null, 0);
    }

    public static TurkuFfmpegProcessResult skipped() {
        return new TurkuFfmpegProcessResult(TurkuFfmpegProcessStatus.PROCESS_SKIPPED,
                TurkuFfmpegProcessReasonCode.PROCESS_SKIPPED_SAFE, null, 0);
    }

    public TurkuFfmpegProcessStatus getStatus() { return status; }
    public TurkuFfmpegProcessReasonCode getReasonCode() { return reasonCode; }
    public String getOutputRef() { return outputRef; }
    public long getDurationMs() { return durationMs; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
