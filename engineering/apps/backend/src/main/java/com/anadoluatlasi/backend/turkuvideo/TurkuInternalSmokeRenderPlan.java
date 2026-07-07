package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-029 Task 01: Smoke render plan summary.
 * No commandLine/shellCommand/executablePath/inputPath/outputPath.
 */
public final class TurkuInternalSmokeRenderPlan {
    private final String format;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long estimatedSizeMB;
    private final int sceneCount;
    private final int assetCount;
    private final boolean boundsValid;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final List<String> safetyFlags;

    public TurkuInternalSmokeRenderPlan(String format, int durationSeconds, int fps,
            int width, int height, long estimatedSizeMB, int sceneCount, int assetCount) {
        this.format = format;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.estimatedSizeMB = estimatedSizeMB;
        this.sceneCount = sceneCount;
        this.assetCount = assetCount;
        boolean formatOk = "mp4".equalsIgnoreCase(format) || "webm".equalsIgnoreCase(format);
        this.boundsValid = formatOk
                && durationSeconds >= 1 && durationSeconds <= 600
                && fps >= 1 && fps <= 60
                && width >= 1 && width <= 3840
                && height >= 1 && height <= 2160
                && estimatedSizeMB >= 0 && estimatedSizeMB <= 1000;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "NO_PUBLIC_URL", "METADATA_ONLY");
    }

    public String getFormat() { return format; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getEstimatedSizeMB() { return estimatedSizeMB; }
    public int getSceneCount() { return sceneCount; }
    public int getAssetCount() { return assetCount; }
    public boolean isBoundsValid() { return boundsValid; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
