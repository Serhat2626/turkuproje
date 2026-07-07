package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-024 Task 02: Render config mode.
 * Represents the resolved state of render configuration.
 */
public enum TurkuRenderConfigMode {
    STUB_ONLY,
    FFMPEG_DISABLED,
    FFMPEG_CONFIG_MISSING,
    FFMPEG_CONFIG_INVALID,
    FFMPEG_CONFIG_VALID,
    FFMPEG_FAILED_SAFE
}
