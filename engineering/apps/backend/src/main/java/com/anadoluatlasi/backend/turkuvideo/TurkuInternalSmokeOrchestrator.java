package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-029 Task 01: E2E smoke orchestrator.
 * Validates FP-025 readiness, FP-026 review, FP-027 dispatch, FP-028 gate.
 * No ProcessBuilder. Plan/candidate only.
 */
public class TurkuInternalSmokeOrchestrator {

    private final TurkuFfmpegExecutionGate ffmpegGate;

    public TurkuInternalSmokeOrchestrator(TurkuFfmpegExecutionGate gate) {
        this.ffmpegGate = gate != null ? gate : new TurkuFfmpegExecutionGate();
    }

    public TurkuInternalSmokeOrchestrator() {
        this(new TurkuFfmpegExecutionGate());
    }

    /**
     * Execute internal smoke path with full safety chain validation.
     */
    public TurkuInternalSmokePathResult execute(
            TurkuInternalSmokeRequest request,
            TurkuInternalRenderReadinessSnapshot readiness,
            TurkuInternalRenderSafetyChainSnapshot safetyChain,
            TurkuInternalRenderReviewState reviewState,
            TurkuFfmpegBinaryPolicy binaryPolicy) {

        // 1. FP-025 readiness
        if (readiness == null || readiness.getStatus() != TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY)
            return TurkuInternalSmokePathResult.denied(TurkuInternalSmokePathReasonCode.READINESS_NOT_MET);

        // 2. FP-025 safety chain
        if (safetyChain == null || !safetyChain.isConfigValid() || !safetyChain.isWorkspaceSafe()
                || !safetyChain.isExecutionBoundarySafe() || !safetyChain.isArtifactVerified())
            return TurkuInternalSmokePathResult.denied(TurkuInternalSmokePathReasonCode.READINESS_NOT_MET);

        // 3. FP-026 human review
        if (reviewState != TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP)
            return TurkuInternalSmokePathResult.denied(TurkuInternalSmokePathReasonCode.REVIEW_NOT_APPROVED);

        // 4. FP-028 FFmpeg gate
        var gateResult = ffmpegGate.evaluate(binaryPolicy,
                request.getJobRef(), request.getQueueItemRef(), request.getReviewRef());
        if (gateResult.getStatus() == TurkuFfmpegBoundaryResultStatus.DENIED)
            return TurkuInternalSmokePathResult.denied(TurkuInternalSmokePathReasonCode.GATE_DENIED);

        // 5. Build plan
        var plan = new TurkuInternalSmokeRenderPlan(
                request.getFormat(), request.getDurationSeconds(), request.getFps(),
                request.getWidth(), request.getHeight(), 500,
                request.getSceneCount(), request.getAssetCount());
        if (!plan.isBoundsValid())
            return TurkuInternalSmokePathResult.denied(TurkuInternalSmokePathReasonCode.BOUNDS_INVALID);

        // 6. Create candidate with opaque output ref
        var outputRef = TurkuFfmpegInternalOutputRef.create();
        return TurkuInternalSmokePathResult.candidateCreated(outputRef.toOpaqueRef(), plan);
    }
}
