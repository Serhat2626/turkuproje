package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-028 Task 01: FFmpeg binary policy.
 * Opaque label only — no raw path exposure.
 */
public enum TurkuFfmpegBinaryPolicy {
    SYSTEM_FFMPEG_LOCAL,
    TEST_FIXTURE_FFMPEG,
    DISABLED_BINARY;

    public boolean isAvailable() {
        return this != DISABLED_BINARY;
    }
}
