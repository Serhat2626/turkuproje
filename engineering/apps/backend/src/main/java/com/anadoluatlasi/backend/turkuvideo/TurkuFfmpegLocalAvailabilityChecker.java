package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-032: Safe local-only FFmpeg availability checker.
 * Does NOT expose executable path, stdout, or stderr.
 * Uses ProcessBuilder only via delegation to the allowed boundary class pattern.
 */
public class TurkuFfmpegLocalAvailabilityChecker {

    private final TurkuFfmpegExecutionEnvironment environment;
    private final boolean checkEnabled;

    public TurkuFfmpegLocalAvailabilityChecker(TurkuFfmpegExecutionEnvironment environment, boolean checkEnabled) {
        this.environment = environment != null ? environment : TurkuFfmpegExecutionEnvironment.UNKNOWN;
        this.checkEnabled = checkEnabled;
    }

    public TurkuFfmpegLocalAvailabilityChecker() {
        this(TurkuFfmpegExecutionEnvironment.UNKNOWN, false);
    }

    /**
     * Check FFmpeg availability safely. No path/output exposure.
     */
    public TurkuFfmpegLocalAvailabilityStatus check() {
        if (!checkEnabled)
            return TurkuFfmpegLocalAvailabilityStatus.DISABLED;

        if (environment != TurkuFfmpegExecutionEnvironment.LOCAL)
            return TurkuFfmpegLocalAvailabilityStatus.UNSUPPORTED_ENVIRONMENT;

        // Probe via ProcessBuilder — stdout consumed, not exposed
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().readAllBytes(); // consume, do not expose
            int exit = p.waitFor();
            return exit == 0
                    ? TurkuFfmpegLocalAvailabilityStatus.AVAILABLE
                    : TurkuFfmpegLocalAvailabilityStatus.UNAVAILABLE;
        } catch (Exception e) {
            return TurkuFfmpegLocalAvailabilityStatus.UNAVAILABLE;
        }
    }

    public boolean isCheckEnabled() { return checkEnabled; }
}
