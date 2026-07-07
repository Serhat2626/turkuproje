package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-031 Task 01: Safe manual execution request. No command/path fields.
 */
public final class TurkuFfmpegManualExecutionRequest {
    private final String jobRef;
    private final String queueItemRef;
    private final String reviewRef;
    private final TurkuFfmpegManualExecutionPolicy policy;
    private final boolean manualExecutionConfirmed;
    private final boolean processEnabled;
    private final String format;
    private final int timeoutSeconds;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long maxOutputSizeMB;

    public TurkuFfmpegManualExecutionRequest(String jobRef, String queueItemRef, String reviewRef,
            TurkuFfmpegManualExecutionPolicy policy, boolean manualExecutionConfirmed, boolean processEnabled,
            String format, int timeoutSeconds, int durationSeconds, int fps, int width, int height, long maxOutputSizeMB) {
        this.jobRef = jobRef;
        this.queueItemRef = queueItemRef;
        this.reviewRef = reviewRef;
        this.policy = policy != null ? policy : TurkuFfmpegManualExecutionPolicy.DISABLED;
        this.manualExecutionConfirmed = manualExecutionConfirmed;
        this.processEnabled = processEnabled;
        this.format = format != null ? format : "mp4";
        this.timeoutSeconds = Math.max(10, Math.min(300, timeoutSeconds));
        this.durationSeconds = Math.max(1, Math.min(600, durationSeconds));
        this.fps = Math.max(1, Math.min(60, fps));
        this.width = Math.max(1, Math.min(3840, width));
        this.height = Math.max(1, Math.min(2160, height));
        this.maxOutputSizeMB = Math.max(1, Math.min(1000, maxOutputSizeMB));
    }

    public boolean isFormatValid() {
        return "mp4".equalsIgnoreCase(format) || "webm".equalsIgnoreCase(format);
    }

    public String getJobRef() { return jobRef; }
    public String getQueueItemRef() { return queueItemRef; }
    public String getReviewRef() { return reviewRef; }
    public TurkuFfmpegManualExecutionPolicy getPolicy() { return policy; }
    public boolean isManualExecutionConfirmed() { return manualExecutionConfirmed; }
    public boolean isProcessEnabled() { return processEnabled; }
    public String getFormat() { return format; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getMaxOutputSizeMB() { return maxOutputSizeMB; }
}
