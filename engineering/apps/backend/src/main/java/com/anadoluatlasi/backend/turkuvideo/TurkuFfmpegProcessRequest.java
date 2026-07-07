package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-030 Task 01: Safe process request. No command/path exposure.
 */
public final class TurkuFfmpegProcessRequest {
    private final String jobRef;
    private final String format;
    private final int timeoutSeconds;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long maxOutputSizeMB;

    public TurkuFfmpegProcessRequest(String jobRef, String format,
            int timeoutSeconds, int durationSeconds, int fps, int width, int height, long maxOutputSizeMB) {
        this.jobRef = jobRef;
        this.format = format != null ? format : "mp4";
        this.timeoutSeconds = Math.max(10, Math.min(300, timeoutSeconds));
        this.durationSeconds = Math.max(1, Math.min(600, durationSeconds));
        this.fps = Math.max(1, Math.min(60, fps));
        this.width = Math.max(1, Math.min(3840, width));
        this.height = Math.max(1, Math.min(2160, height));
        this.maxOutputSizeMB = Math.max(1, Math.min(1000, maxOutputSizeMB));
    }

    public boolean isValid() {
        return ("mp4".equalsIgnoreCase(format) || "webm".equalsIgnoreCase(format))
                && timeoutSeconds >= 10 && timeoutSeconds <= 300
                && durationSeconds >= 1 && durationSeconds <= 600
                && fps >= 1 && fps <= 60
                && width >= 1 && width <= 3840
                && height >= 1 && height <= 2160
                && maxOutputSizeMB >= 1 && maxOutputSizeMB <= 1000;
    }

    public String getJobRef() { return jobRef; }
    public String getFormat() { return format; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getMaxOutputSizeMB() { return maxOutputSizeMB; }
}
