package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.Set;

/**
 * FP-024 Task 02: Secure render config.
 * Represents a render config with safe API-visible output only.
 * Raw paths are stored internally for validation but never exposed via getters.
 * API-safe output uses opaque tokens only.
 */
public final class TurkuRenderSecureConfig {

    // Internal-only fields for validation (never exposed in API)
    private final boolean enabled;
    private final String executableToken;
    private final String workspaceToken;
    private final String tempToken;
    private final int maxTimeoutSeconds;
    private final int maxDurationSeconds;
    private final int maxOutputSizeMb;
    private final String maxResolution;
    private final int maxFps;
    private final Set<String> allowedFormats;
    private final Set<String> allowedCodecs;

    public TurkuRenderSecureConfig(boolean enabled, String executableToken,
                                    String workspaceToken, String tempToken,
                                    int maxTimeoutSeconds, int maxDurationSeconds,
                                    int maxOutputSizeMb, String maxResolution, int maxFps,
                                    Set<String> allowedFormats, Set<String> allowedCodecs) {
        this.enabled = enabled;
        this.executableToken = executableToken;
        this.workspaceToken = workspaceToken;
        this.tempToken = tempToken;
        this.maxTimeoutSeconds = maxTimeoutSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.maxOutputSizeMb = maxOutputSizeMb;
        this.maxResolution = maxResolution;
        this.maxFps = maxFps;
        this.allowedFormats = allowedFormats != null ? Set.copyOf(allowedFormats) : Set.of("mp4", "webm");
        this.allowedCodecs = allowedCodecs != null ? Set.copyOf(allowedCodecs) : Set.of("h264", "vp9");
    }

    /** Default disabled config. */
    public TurkuRenderSecureConfig() {
        this(false, "ffmpeg", "workspace", "temp",
                120, 600, 500, "1920x1080", 30,
                Set.of("mp4", "webm"), Set.of("h264", "vp9"));
    }

    public boolean isEnabled() { return enabled; }
    public String getExecutableToken() { return executableToken; }
    public String getWorkspaceToken() { return workspaceToken; }
    public String getTempToken() { return tempToken; }
    public int getMaxTimeoutSeconds() { return maxTimeoutSeconds; }
    public int getMaxDurationSeconds() { return maxDurationSeconds; }
    public int getMaxOutputSizeMb() { return maxOutputSizeMb; }
    public String getMaxResolution() { return maxResolution; }
    public int getMaxFps() { return maxFps; }
    public Set<String> getAllowedFormats() { return allowedFormats; }
    public Set<String> getAllowedCodecs() { return allowedCodecs; }
}
