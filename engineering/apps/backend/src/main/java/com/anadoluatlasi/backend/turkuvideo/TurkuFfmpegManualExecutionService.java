package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-031 Task 01: Local-only manual execution service.
 * Validates full gate chain, delegates to TurkuFfmpegLocalDevProcessRunner.
 * No ProcessBuilder in THIS class.
 */
public class TurkuFfmpegManualExecutionService {

    private final TurkuFfmpegLocalDevProcessRunner processRunner;

    public TurkuFfmpegManualExecutionService(TurkuFfmpegLocalDevProcessRunner runner) {
        this.processRunner = runner != null ? runner : new TurkuFfmpegLocalDevProcessRunner();
    }

    public TurkuFfmpegManualExecutionService() {
        this(new TurkuFfmpegLocalDevProcessRunner());
    }

    public TurkuFfmpegManualExecutionResult execute(
            TurkuFfmpegManualExecutionRequest request,
            TurkuInternalRenderReadinessSnapshot readiness,
            TurkuInternalRenderSafetyChainSnapshot safetyChain,
            TurkuInternalRenderReviewState reviewState,
            TurkuFfmpegBinaryPolicy binaryPolicy,
            TurkuFfmpegExecutionEnvironment environment) {

        // 1. Policy check
        if (request == null || request.getPolicy() != TurkuFfmpegManualExecutionPolicy.LOCAL_ONLY_MANUAL)
            return TurkuFfmpegManualExecutionResult.denied(
                    TurkuFfmpegManualExecutionStatus.POLICY_DISABLED,
                    TurkuFfmpegManualExecutionReasonCode.POLICY_NOT_LOCAL_MANUAL);

        // 2. Manual confirmation
        if (!request.isManualExecutionConfirmed())
            return TurkuFfmpegManualExecutionResult.denied(
                    TurkuFfmpegManualExecutionStatus.MANUAL_CONFIRMATION_MISSING,
                    TurkuFfmpegManualExecutionReasonCode.MANUAL_NOT_CONFIRMED);

        // 3. Process enabled
        if (!request.isProcessEnabled())
            return TurkuFfmpegManualExecutionResult.denied(
                    TurkuFfmpegManualExecutionStatus.PROCESS_DISABLED,
                    TurkuFfmpegManualExecutionReasonCode.PROCESS_NOT_ENABLED);

        // 4. Environment must be LOCAL
        if (environment == null || environment != TurkuFfmpegExecutionEnvironment.LOCAL)
            return TurkuFfmpegManualExecutionResult.denied(
                    TurkuFfmpegManualExecutionStatus.ENVIRONMENT_DENIED,
                    TurkuFfmpegManualExecutionReasonCode.ENVIRONMENT_NOT_LOCAL);

        // 5. Binary
        if (binaryPolicy == null || !binaryPolicy.isAvailable())
            return TurkuFfmpegManualExecutionResult.denied(
                    TurkuFfmpegManualExecutionStatus.BINARY_UNAVAILABLE,
                    TurkuFfmpegManualExecutionReasonCode.BINARY_NOT_AVAILABLE);

        // 6. FP-025 readiness
        if (readiness == null || readiness.getStatus() != TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY)
            return TurkuFfmpegManualExecutionResult.denied(
                    TurkuFfmpegManualExecutionStatus.READINESS_DENIED,
                    TurkuFfmpegManualExecutionReasonCode.READINESS_NOT_MET);

        // 7. FP-025 safety chain
        if (safetyChain == null || !safetyChain.isConfigValid() || !safetyChain.isWorkspaceSafe()
                || !safetyChain.isExecutionBoundarySafe() || !safetyChain.isArtifactVerified())
            return TurkuFfmpegManualExecutionResult.denied(
                    TurkuFfmpegManualExecutionStatus.READINESS_DENIED,
                    TurkuFfmpegManualExecutionReasonCode.READINESS_NOT_MET);

        // 8. FP-026 review
        if (reviewState != TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP)
            return TurkuFfmpegManualExecutionResult.denied(
                    TurkuFfmpegManualExecutionStatus.REVIEW_DENIED,
                    TurkuFfmpegManualExecutionReasonCode.REVIEW_NOT_APPROVED);

        // 9. Format validation
        if (!request.isFormatValid())
            return TurkuFfmpegManualExecutionResult.denied(
                    TurkuFfmpegManualExecutionStatus.GATE_DENIED,
                    TurkuFfmpegManualExecutionReasonCode.GATE_DENIED_MODE);

        // 10. Runner disabled → fake skip
        if (!processRunner.isProcessEnabled())
            return TurkuFfmpegManualExecutionResult.skipped();

        // 11. Delegate to process runner
        var processReq = new TurkuFfmpegProcessRequest(request.getJobRef(), request.getFormat(),
                request.getTimeoutSeconds(), request.getDurationSeconds(), request.getFps(),
                request.getWidth(), request.getHeight(), request.getMaxOutputSizeMB());
        var processResult = processRunner.execute(processReq, binaryPolicy);

        // 12. Map result
        return switch (processResult.getStatus()) {
            case PROCESS_COMPLETED_SAFE -> {
                var outputRef = processResult.getOutputRef();
                var metadata = new TurkuFfmpegFixtureMetadata(outputRef, request.getFormat(),
                        request.getDurationSeconds(), request.getFps(),
                        request.getWidth(), request.getHeight(), request.getMaxOutputSizeMB());
                if (metadata.isBoundsValid())
                    yield TurkuFfmpegManualExecutionResult.verificationPassed(outputRef, metadata);
                else
                    yield TurkuFfmpegManualExecutionResult.verificationFailed();
            }
            case GATE_DENIED -> TurkuFfmpegManualExecutionResult.denied(
                    TurkuFfmpegManualExecutionStatus.GATE_DENIED,
                    TurkuFfmpegManualExecutionReasonCode.GATE_DENIED_MODE);
            case PROCESS_SKIPPED -> TurkuFfmpegManualExecutionResult.skipped();
            default -> TurkuFfmpegManualExecutionResult.failedSafe();
        };
    }
}
