package com.anadoluatlasi.backend.turkuvideo;

import java.util.Set;

/**
 * FP-024 Task 05: Artifact verification policy.
 * Defines allowed formats, bounds for verification checks.
 */
public final class TurkuRenderArtifactVerificationPolicy {

    public static final Set<String> ALLOWED_FORMATS = Set.of("mp4", "webm");
    public static final double MAX_DURATION_SECONDS = 600.0;
    public static final int MAX_SIZE_MB = 1000;
    public static final int MAX_RESOLUTION_W = 3840;
    public static final int MAX_RESOLUTION_H = 2160;
    public static final int MAX_FPS = 60;

    private final Set<String> allowedFormats;
    private final double maxDurationSeconds;
    private final int maxSizeMb;
    private final int maxResolutionW;
    private final int maxResolutionH;
    private final int maxFps;

    public TurkuRenderArtifactVerificationPolicy(Set<String> allowedFormats,
                                                   double maxDurationSeconds, int maxSizeMb,
                                                   int maxResolutionW, int maxResolutionH, int maxFps) {
        this.allowedFormats = allowedFormats != null ? Set.copyOf(allowedFormats) : ALLOWED_FORMATS;
        this.maxDurationSeconds = maxDurationSeconds;
        this.maxSizeMb = maxSizeMb;
        this.maxResolutionW = maxResolutionW;
        this.maxResolutionH = maxResolutionH;
        this.maxFps = maxFps;
    }

    public TurkuRenderArtifactVerificationPolicy() {
        this(ALLOWED_FORMATS, MAX_DURATION_SECONDS, MAX_SIZE_MB,
                MAX_RESOLUTION_W, MAX_RESOLUTION_H, MAX_FPS);
    }

    public Set<String> getAllowedFormats() { return allowedFormats; }
    public double getMaxDurationSeconds() { return maxDurationSeconds; }
    public int getMaxSizeMb() { return maxSizeMb; }
    public int getMaxResolutionW() { return maxResolutionW; }
    public int getMaxResolutionH() { return maxResolutionH; }
    public int getMaxFps() { return maxFps; }
}
