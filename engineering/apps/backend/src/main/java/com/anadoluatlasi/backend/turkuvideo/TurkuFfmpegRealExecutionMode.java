package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-028 Task 01: FFmpeg real execution mode.
 * Default DISABLED (fail-closed).
 */
public enum TurkuFfmpegRealExecutionMode {
    DISABLED,
    DRY_RUN,
    SIMULATED,
    REAL_DEV_TEST_ONLY
}
