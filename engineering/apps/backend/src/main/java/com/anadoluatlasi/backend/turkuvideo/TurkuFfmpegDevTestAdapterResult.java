package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-028 Task 02: Adapter result wrapping gate result + output ref.
 */
public final class TurkuFfmpegDevTestAdapterResult {
    private final boolean allowed;
    private final TurkuFfmpegExecutionBoundaryResult gateResult;
    private final String outputRef;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    private TurkuFfmpegDevTestAdapterResult(boolean allowed,
            TurkuFfmpegExecutionBoundaryResult gateResult, String outputRef) {
        this.allowed = allowed;
        this.gateResult = gateResult;
        this.outputRef = outputRef;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "NO_PUBLIC_URL", "NO_AUTO_APPROVE");
    }

    public static TurkuFfmpegDevTestAdapterResult denied(TurkuFfmpegExecutionBoundaryResult gate) {
        return new TurkuFfmpegDevTestAdapterResult(false, gate, null);
    }

    public static TurkuFfmpegDevTestAdapterResult completed(
            TurkuFfmpegExecutionBoundaryResult gate, TurkuFfmpegInternalOutputRef ref) {
        return new TurkuFfmpegDevTestAdapterResult(true, gate, ref.toOpaqueRef());
    }

    public boolean isAllowed() { return allowed; }
    public TurkuFfmpegExecutionBoundaryResult getGateResult() { return gateResult; }
    public String getOutputRef() { return outputRef; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
