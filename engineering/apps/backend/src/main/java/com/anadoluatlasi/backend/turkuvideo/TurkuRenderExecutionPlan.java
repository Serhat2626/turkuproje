package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-022 Task 03: Render execution plan metadata.
 * No real FFmpeg/cloud commands. No secrets. No file paths.
 */
public final class TurkuRenderExecutionPlan {
    private final UUID planId;
    private final int sceneCount;
    private final double totalDurationSeconds;
    private final String targetResolution;
    private final int targetFps;
    private final String targetFormat;
    private final double estimatedRenderTimeSeconds;
    private final String rendererAlias;
    private final String renderMode;

    public TurkuRenderExecutionPlan(UUID planId, int sceneCount, double totalDurationSeconds,
                                     String targetResolution, int targetFps, String targetFormat,
                                     double estimatedRenderTimeSeconds, String rendererAlias,
                                     String renderMode) {
        this.planId = planId;
        this.sceneCount = sceneCount;
        this.totalDurationSeconds = totalDurationSeconds;
        this.targetResolution = targetResolution;
        this.targetFps = targetFps;
        this.targetFormat = targetFormat;
        this.estimatedRenderTimeSeconds = estimatedRenderTimeSeconds;
        this.rendererAlias = rendererAlias;
        this.renderMode = renderMode;
    }

    public UUID getPlanId() { return planId; }
    public int getSceneCount() { return sceneCount; }
    public double getTotalDurationSeconds() { return totalDurationSeconds; }
    public String getTargetResolution() { return targetResolution; }
    public int getTargetFps() { return targetFps; }
    public String getTargetFormat() { return targetFormat; }
    public double getEstimatedRenderTimeSeconds() { return estimatedRenderTimeSeconds; }
    public String getRendererAlias() { return rendererAlias; }
    public String getRenderMode() { return renderMode; }
}
