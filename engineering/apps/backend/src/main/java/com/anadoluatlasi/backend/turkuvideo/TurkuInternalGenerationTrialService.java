package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-029 Task 02: Internal generation trial service.
 * Validates full safety chain → produces simulated/attempted trial result.
 * No ProcessBuilder. Real execution delegated to FP-028 DevTestAdapter.
 */
public class TurkuInternalGenerationTrialService {

    private final TurkuFfmpegDevTestAdapter adapter;

    public TurkuInternalGenerationTrialService(TurkuFfmpegDevTestAdapter adapter) {
        this.adapter = adapter != null ? adapter : new TurkuFfmpegDevTestAdapter();
    }

    public TurkuInternalGenerationTrialService() {
        this(new TurkuFfmpegDevTestAdapter());
    }

    /**
     * Run internal generation trial with full FP-025/026/028 chain validation.
     */
    public TurkuInternalTrialResult runTrial(
            TurkuInternalSmokePathResult smokeResult,
            TurkuInternalRenderReadinessSnapshot readiness,
            TurkuInternalRenderReviewState reviewState,
            TurkuFfmpegBinaryPolicy binaryPolicy) {

        // 1. Smoke path must have produced a candidate
        if (smokeResult == null || smokeResult.getStatus() != TurkuInternalSmokePathStatus.CANDIDATE_CREATED)
            return TurkuInternalTrialResult.denied(TurkuInternalTrialReasonCode.NOT_ATTEMPTED);

        // 2. FP-025 readiness
        if (readiness == null || readiness.getStatus() != TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY)
            return TurkuInternalTrialResult.denied(TurkuInternalTrialReasonCode.READINESS_NOT_MET);

        // 3. FP-026 review
        if (reviewState != TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP)
            return TurkuInternalTrialResult.denied(TurkuInternalTrialReasonCode.REVIEW_NOT_APPROVED);

        // 4. Plan bounds
        var plan = smokeResult.getPlan();
        if (plan == null || !plan.isBoundsValid())
            return TurkuInternalTrialResult.planInvalid();

        // 5. Delegate to FP-028 adapter (gate inside)
        var adapterResult = adapter.execute(binaryPolicy,
                "trial-job", null, null);

        if (!adapterResult.isAllowed())
            return TurkuInternalTrialResult.denied(TurkuInternalTrialReasonCode.GATE_DENIED_MODE);

        // 6. Create verification candidate
        var outputRef = adapterResult.getOutputRef();
        var candidate = new TurkuFfmpegVerificationCandidate(
                outputRef, "trial-job", plan.getFormat(),
                plan.getDurationSeconds(), plan.getFps(),
                plan.getWidth(), plan.getHeight(), plan.getEstimatedSizeMB());

        // 7. Determine execution type from gate mode
        var gateMode = adapterResult.getGateResult().getMode();
        if (gateMode == TurkuFfmpegRealExecutionMode.DRY_RUN
                || gateMode == TurkuFfmpegRealExecutionMode.SIMULATED)
            return TurkuInternalTrialResult.simulated(outputRef, candidate);

        // REAL_DEV_TEST_ONLY — trial attempted (no actual ProcessBuilder in this class)
        return TurkuInternalTrialResult.attempted(outputRef, candidate);
    }
}
