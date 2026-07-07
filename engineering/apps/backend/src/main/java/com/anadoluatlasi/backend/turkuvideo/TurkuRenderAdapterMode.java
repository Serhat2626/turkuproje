package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-023 Task 02: Render adapter mode.
 * Determines which render gateway is active.
 */
public enum TurkuRenderAdapterMode {
    STUB,
    FFMPEG_DISABLED,
    FFMPEG_CONFIG_MISSING,
    FFMPEG_READY,
    FFMPEG_FAILED_SAFE
}
