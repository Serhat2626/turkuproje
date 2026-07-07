package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-031 Task 02: Actual local smoke run harness.
 * Wraps ManualExecutionService with real-local-run status mapping.
 * No ProcessBuilder in THIS class.
 */
public class TurkuFfmpegActualLocalRunHarness {

    private final TurkuFfmpegManualExecutionService manualService;

    public TurkuFfmpegActualLocalRunHarness(TurkuFfmpegManualExecutionService service) {
        this.manualService = service != null ? service : new TurkuFfmpegManualExecutionService();
    }

    public TurkuFfmpegActualLocalRunHarness() {
        this(new TurkuFfmpegManualExecutionService());
    }

    /**
     * Attempt actual local smoke run. Maps result to real-local-run statuses.
     */
    public TurkuFfmpegManualExecutionResult attempt(
            TurkuFfmpegManualExecutionRequest request,
            TurkuInternalRenderReadinessSnapshot readiness,
            TurkuInternalRenderSafetyChainSnapshot safetyChain,
            TurkuInternalRenderReviewState reviewState,
            TurkuFfmpegBinaryPolicy binaryPolicy,
            TurkuFfmpegExecutionEnvironment environment) {

        // Delegate to manual execution service
        var result = manualService.execute(request, readiness, safetyChain,
                reviewState, binaryPolicy, environment);

        // Map to real-local-run statuses
        return switch (result.getStatus()) {
            case EXECUTION_COMPLETED_SAFE, VERIFICATION_PASSED ->
                TurkuFfmpegManualExecutionResult.denied(
                        TurkuFfmpegManualExecutionStatus.REAL_LOCAL_RUN_SUCCEEDED,
                        TurkuFfmpegManualExecutionReasonCode.REAL_LOCAL_SUCCEEDED_SAFE);
            case EXECUTION_FAILED_SAFE ->
                TurkuFfmpegManualExecutionResult.denied(
                        TurkuFfmpegManualExecutionStatus.REAL_LOCAL_RUN_FAILED_SAFE,
                        TurkuFfmpegManualExecutionReasonCode.REAL_LOCAL_FAILED_SAFE);
            case BINARY_UNAVAILABLE ->
                TurkuFfmpegManualExecutionResult.denied(
                        TurkuFfmpegManualExecutionStatus.REAL_LOCAL_RUN_SKIPPED_UNAVAILABLE,
                        TurkuFfmpegManualExecutionReasonCode.REAL_LOCAL_SKIPPED_BINARY_UNAVAILABLE);
            case EXECUTION_SKIPPED_FAKE ->
                TurkuFfmpegManualExecutionResult.denied(
                        TurkuFfmpegManualExecutionStatus.REAL_LOCAL_RUN_ATTEMPTED,
                        TurkuFfmpegManualExecutionReasonCode.REAL_LOCAL_ATTEMPTED_SAFE);
            default -> result; // pass through denials unchanged
        };
    }
}
