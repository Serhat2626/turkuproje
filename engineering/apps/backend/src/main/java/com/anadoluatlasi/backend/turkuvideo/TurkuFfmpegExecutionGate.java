package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-028 Task 01: FFmpeg execution gate.
 * Determines whether real FFmpeg execution is allowed.
 * Default: DISABLED (fail-closed).
 */
public class TurkuFfmpegExecutionGate {

    private final TurkuFfmpegRealExecutionMode mode;
    private final TurkuFfmpegExecutionEnvironment environment;
    private final boolean internalDevTestEnabled;

    public TurkuFfmpegExecutionGate() {
        this(TurkuFfmpegRealExecutionMode.DISABLED,
                TurkuFfmpegExecutionEnvironment.UNKNOWN, false);
    }

    public TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode mode,
            TurkuFfmpegExecutionEnvironment environment, boolean internalDevTestEnabled) {
        this.mode = mode != null ? mode : TurkuFfmpegRealExecutionMode.DISABLED;
        this.environment = environment != null ? environment : TurkuFfmpegExecutionEnvironment.UNKNOWN;
        this.internalDevTestEnabled = internalDevTestEnabled;
    }

    /**
     * Evaluate whether execution should proceed and return a safe boundary result.
     */
    public TurkuFfmpegExecutionBoundaryResult evaluate(
            TurkuFfmpegBinaryPolicy binaryPolicy,
            String jobRef, String queueRef, String reviewRef) {
        // 1. Mode disabled
        if (mode == TurkuFfmpegRealExecutionMode.DISABLED)
            return TurkuFfmpegExecutionBoundaryResult.denied(
                    TurkuFfmpegBoundaryReasonCode.MODE_DISABLED, mode, environment);

        // 2. Binary unavailable
        if (binaryPolicy == null || !binaryPolicy.isAvailable())
            return TurkuFfmpegExecutionBoundaryResult.denied(
                    TurkuFfmpegBoundaryReasonCode.BINARY_UNAVAILABLE, mode, environment);

        // 3. DRY_RUN mode
        if (mode == TurkuFfmpegRealExecutionMode.DRY_RUN)
            return TurkuFfmpegExecutionBoundaryResult.safe(
                    TurkuFfmpegBoundaryResultStatus.DRY_RUN_COMPLETE,
                    TurkuFfmpegBoundaryReasonCode.DRY_RUN_SAFE,
                    mode, environment, jobRef, queueRef, reviewRef);

        // 4. SIMULATED mode
        if (mode == TurkuFfmpegRealExecutionMode.SIMULATED)
            return TurkuFfmpegExecutionBoundaryResult.safe(
                    TurkuFfmpegBoundaryResultStatus.SIMULATED_COMPLETE,
                    TurkuFfmpegBoundaryReasonCode.SIMULATED_SAFE,
                    mode, environment, jobRef, queueRef, reviewRef);

        // 5. REAL_DEV_TEST_ONLY — check environment
        if (!environment.isRealExecutionAllowed())
            return TurkuFfmpegExecutionBoundaryResult.denied(
                    TurkuFfmpegBoundaryReasonCode.ENVIRONMENT_DENIED, mode, environment);

        // 6. Explicit internal flag required
        if (!internalDevTestEnabled)
            return TurkuFfmpegExecutionBoundaryResult.denied(
                    TurkuFfmpegBoundaryReasonCode.INTERNAL_FLAG_NOT_SET, mode, environment);

        // 7. All gates passed — real dev/test execution allowed
        return TurkuFfmpegExecutionBoundaryResult.safe(
                TurkuFfmpegBoundaryResultStatus.EXECUTED_INTERNAL,
                TurkuFfmpegBoundaryReasonCode.EXECUTED_INTERNAL_SAFE,
                mode, environment, jobRef, queueRef, reviewRef);
    }

    public TurkuFfmpegRealExecutionMode getMode() { return mode; }
    public TurkuFfmpegExecutionEnvironment getEnvironment() { return environment; }
    public boolean isInternalDevTestEnabled() { return internalDevTestEnabled; }
}
