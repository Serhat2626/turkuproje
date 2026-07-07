package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-030 Task 02: First real smoke run service.
 * Validates full chain → delegates to TurkuFfmpegLocalDevProcessRunner.
 * No ProcessBuilder in THIS class.
 */
public class TurkuFfmpegSmokeRunService {

    private final TurkuFfmpegLocalDevProcessRunner processRunner;

    public TurkuFfmpegSmokeRunService(TurkuFfmpegLocalDevProcessRunner processRunner) {
        this.processRunner = processRunner != null ? processRunner : new TurkuFfmpegLocalDevProcessRunner();
    }

    public TurkuFfmpegSmokeRunService() {
        this(new TurkuFfmpegLocalDevProcessRunner());
    }

    /**
     * Run first real smoke path with full chain validation.
     */
    public TurkuFfmpegSmokeRunResult run(
            TurkuFfmpegProcessRequest request,
            TurkuInternalRenderReadinessSnapshot readiness,
            TurkuInternalRenderSafetyChainSnapshot safetyChain,
            TurkuInternalRenderReviewState reviewState,
            TurkuFfmpegBinaryPolicy binaryPolicy) {

        // 1. FP-025 readiness
        if (readiness == null || readiness.getStatus() != TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY)
            return TurkuFfmpegSmokeRunResult.denied(TurkuFfmpegSmokeRunStatus.READINESS_DENIED);

        // 2. FP-025 safety chain
        if (safetyChain == null || !safetyChain.isConfigValid() || !safetyChain.isWorkspaceSafe()
                || !safetyChain.isExecutionBoundarySafe() || !safetyChain.isArtifactVerified())
            return TurkuFfmpegSmokeRunResult.denied(TurkuFfmpegSmokeRunStatus.READINESS_DENIED);

        // 3. FP-026 review
        if (reviewState != TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP)
            return TurkuFfmpegSmokeRunResult.denied(TurkuFfmpegSmokeRunStatus.REVIEW_DENIED);

        // 4. Request validation
        if (request == null || !request.isValid())
            return TurkuFfmpegSmokeRunResult.denied(TurkuFfmpegSmokeRunStatus.PROCESS_DENIED);

        // 5. Binary check
        if (binaryPolicy == null || !binaryPolicy.isAvailable())
            return TurkuFfmpegSmokeRunResult.skippedUnavailable();

        // 6. Process runner disabled?
        if (!processRunner.isProcessEnabled())
            return TurkuFfmpegSmokeRunResult.skippedDisabled();

        // 7. Delegate to process runner (gate inside)
        var processResult = processRunner.execute(request, binaryPolicy);

        // 8. Map process result
        return switch (processResult.getStatus()) {
            case PROCESS_COMPLETED_SAFE -> {
                var candidate = new TurkuFfmpegVerificationCandidate(
                        processResult.getOutputRef(), request.getJobRef(), request.getFormat(),
                        request.getDurationSeconds(), request.getFps(),
                        request.getWidth(), request.getHeight(), request.getMaxOutputSizeMB());
                yield TurkuFfmpegSmokeRunResult.completed(processResult.getOutputRef(),
                        candidate, processResult.getDurationMs());
            }
            case GATE_DENIED -> TurkuFfmpegSmokeRunResult.denied(TurkuFfmpegSmokeRunStatus.GATE_DENIED);
            case PROCESS_SKIPPED -> TurkuFfmpegSmokeRunResult.skippedDisabled();
            case PROCESS_TIMED_OUT, PROCESS_FAILED_SAFE -> TurkuFfmpegSmokeRunResult.failedSafe();
            default -> TurkuFfmpegSmokeRunResult.failedSafe();
        };
    }
}
