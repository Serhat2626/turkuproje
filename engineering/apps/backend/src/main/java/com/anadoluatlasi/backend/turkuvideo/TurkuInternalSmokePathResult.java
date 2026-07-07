package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-029 Task 01: Smoke path result.
 */
public final class TurkuInternalSmokePathResult {
    private final TurkuInternalSmokePathStatus status;
    private final TurkuInternalSmokePathReasonCode reasonCode;
    private final String outputRef;
    private final TurkuInternalSmokeRenderPlan plan;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    private TurkuInternalSmokePathResult(TurkuInternalSmokePathStatus status,
            TurkuInternalSmokePathReasonCode reasonCode, String outputRef,
            TurkuInternalSmokeRenderPlan plan) {
        this.status = status;
        this.reasonCode = reasonCode;
        this.outputRef = outputRef;
        this.plan = plan;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "NO_PUBLIC_URL", "NO_AUTO_APPROVE");
    }

    public static TurkuInternalSmokePathResult denied(TurkuInternalSmokePathReasonCode reason) {
        return new TurkuInternalSmokePathResult(TurkuInternalSmokePathStatus.DENIED, reason, null, null);
    }

    public static TurkuInternalSmokePathResult completed(String outputRef, TurkuInternalSmokeRenderPlan plan) {
        return new TurkuInternalSmokePathResult(TurkuInternalSmokePathStatus.SMOKE_COMPLETE_INTERNAL,
                TurkuInternalSmokePathReasonCode.SMOKE_COMPLETE_SAFE, outputRef, plan);
    }

    public static TurkuInternalSmokePathResult candidateCreated(String outputRef, TurkuInternalSmokeRenderPlan plan) {
        return new TurkuInternalSmokePathResult(TurkuInternalSmokePathStatus.CANDIDATE_CREATED,
                TurkuInternalSmokePathReasonCode.CANDIDATE_CREATED_SAFE, outputRef, plan);
    }

    public TurkuInternalSmokePathStatus getStatus() { return status; }
    public TurkuInternalSmokePathReasonCode getReasonCode() { return reasonCode; }
    public String getOutputRef() { return outputRef; }
    public TurkuInternalSmokeRenderPlan getPlan() { return plan; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
