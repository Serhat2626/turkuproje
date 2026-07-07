package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-032: Safe local-only FFmpeg availability checker.
 * Does NOT use ProcessBuilder directly.
 * Delegates to TurkuFfmpegLocalDevProcessRunner (the ONLY allowed ProcessBuilder class).
 */
public class TurkuFfmpegLocalAvailabilityChecker {

    private final TurkuFfmpegExecutionEnvironment environment;
    private final boolean checkEnabled;
    private final TurkuFfmpegLocalDevProcessRunner runner;

    public TurkuFfmpegLocalAvailabilityChecker(TurkuFfmpegExecutionEnvironment environment,
            boolean checkEnabled, TurkuFfmpegLocalDevProcessRunner runner) {
        this.environment = environment != null ? environment : TurkuFfmpegExecutionEnvironment.UNKNOWN;
        this.checkEnabled = checkEnabled;
        this.runner = runner;
    }

    public TurkuFfmpegLocalAvailabilityChecker(TurkuFfmpegExecutionEnvironment environment, boolean checkEnabled) {
        this(environment, checkEnabled, null);
    }

    public TurkuFfmpegLocalAvailabilityChecker() {
        this(TurkuFfmpegExecutionEnvironment.UNKNOWN, false, null);
    }

    /**
     * Check FFmpeg availability safely. No path/output exposure.
     * Delegates probe to the allowed runner boundary class.
     */
    public TurkuFfmpegLocalAvailabilityStatus check() {
        if (!checkEnabled)
            return TurkuFfmpegLocalAvailabilityStatus.DISABLED;

        if (environment != TurkuFfmpegExecutionEnvironment.LOCAL)
            return TurkuFfmpegLocalAvailabilityStatus.UNSUPPORTED_ENVIRONMENT;

        // Delegate to runner for probe — no direct ProcessBuilder here
        if (runner == null)
            return TurkuFfmpegLocalAvailabilityStatus.UNAVAILABLE;

        var probeReq = new TurkuFfmpegProcessRequest("probe", "mp4", 10, 1, 1, 1, 1, 1);
        var result = runner.execute(probeReq, TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);

        return switch (result.getStatus()) {
            case PROCESS_COMPLETED_SAFE -> TurkuFfmpegLocalAvailabilityStatus.AVAILABLE;
            case PROCESS_SKIPPED -> TurkuFfmpegLocalAvailabilityStatus.DISABLED;
            case GATE_DENIED -> TurkuFfmpegLocalAvailabilityStatus.UNSUPPORTED_ENVIRONMENT;
            default -> TurkuFfmpegLocalAvailabilityStatus.UNAVAILABLE;
        };
    }

    public boolean isCheckEnabled() { return checkEnabled; }
}
