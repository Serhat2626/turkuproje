package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-029 Task 01: Internal smoke request summary.
 * Opaque refs + counts only. No lyrics/audio/video content.
 */
public final class TurkuInternalSmokeRequest {
    private final String projectRef;
    private final String jobRef;
    private final String reviewRef;
    private final String queueItemRef;
    private final int sceneCount;
    private final int assetCount;
    private final String format;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;

    public TurkuInternalSmokeRequest(String projectRef, String jobRef, String reviewRef,
            String queueItemRef, int sceneCount, int assetCount,
            String format, int durationSeconds, int fps, int width, int height) {
        this.projectRef = projectRef;
        this.jobRef = jobRef;
        this.reviewRef = reviewRef;
        this.queueItemRef = queueItemRef;
        this.sceneCount = Math.max(0, sceneCount);
        this.assetCount = Math.max(0, assetCount);
        this.format = format != null ? format : "mp4";
        this.durationSeconds = Math.max(1, Math.min(600, durationSeconds));
        this.fps = Math.max(1, Math.min(60, fps));
        this.width = Math.max(1, Math.min(3840, width));
        this.height = Math.max(1, Math.min(2160, height));
    }

    public String getProjectRef() { return projectRef; }
    public String getJobRef() { return jobRef; }
    public String getReviewRef() { return reviewRef; }
    public String getQueueItemRef() { return queueItemRef; }
    public int getSceneCount() { return sceneCount; }
    public int getAssetCount() { return assetCount; }
    public String getFormat() { return format; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
