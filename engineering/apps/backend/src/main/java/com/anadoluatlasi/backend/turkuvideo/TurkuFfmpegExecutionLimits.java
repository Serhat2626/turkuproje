package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-023 Task 02: FFmpeg execution limits.
 * Bounded defaults — no unbounded execution.
 */
public final class TurkuFfmpegExecutionLimits {
    public static final int DEFAULT_TIMEOUT_SECONDS = 300;
    public static final int MAX_TIMEOUT_SECONDS = 600;
    public static final int DEFAULT_MAX_DURATION_SECONDS = 600;
    public static final int MAX_DURATION_SECONDS = 900;
    public static final int DEFAULT_MAX_FILE_SIZE_MB = 500;
    public static final int MAX_FILE_SIZE_MB = 1024;
    public static final int DEFAULT_MAX_WIDTH = 1920;
    public static final int DEFAULT_MAX_HEIGHT = 1080;
    public static final int MAX_WIDTH = 3840;
    public static final int MAX_HEIGHT = 2160;
    public static final int DEFAULT_MAX_FPS = 30;
    public static final int MAX_FPS = 60;

    private final int timeoutSeconds;
    private final int maxDurationSeconds;
    private final int maxFileSizeMb;
    private final int maxWidth;
    private final int maxHeight;
    private final int maxFps;

    public TurkuFfmpegExecutionLimits() {
        this(DEFAULT_TIMEOUT_SECONDS, DEFAULT_MAX_DURATION_SECONDS,
             DEFAULT_MAX_FILE_SIZE_MB, DEFAULT_MAX_WIDTH, DEFAULT_MAX_HEIGHT, DEFAULT_MAX_FPS);
    }

    public TurkuFfmpegExecutionLimits(int timeoutSeconds, int maxDurationSeconds,
                                       int maxFileSizeMb, int maxWidth, int maxHeight, int maxFps) {
        this.timeoutSeconds = Math.min(Math.max(timeoutSeconds, 1), MAX_TIMEOUT_SECONDS);
        this.maxDurationSeconds = Math.min(Math.max(maxDurationSeconds, 1), MAX_DURATION_SECONDS);
        this.maxFileSizeMb = Math.min(Math.max(maxFileSizeMb, 1), MAX_FILE_SIZE_MB);
        this.maxWidth = Math.min(Math.max(maxWidth, 1), MAX_WIDTH);
        this.maxHeight = Math.min(Math.max(maxHeight, 1), MAX_HEIGHT);
        this.maxFps = Math.min(Math.max(maxFps, 1), MAX_FPS);
    }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public int getMaxDurationSeconds() { return maxDurationSeconds; }
    public int getMaxFileSizeMb() { return maxFileSizeMb; }
    public int getMaxWidth() { return maxWidth; }
    public int getMaxHeight() { return maxHeight; }
    public int getMaxFps() { return maxFps; }
}
