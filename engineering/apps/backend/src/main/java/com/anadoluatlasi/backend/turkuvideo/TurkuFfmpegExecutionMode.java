package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-024 Task 04: FFmpeg execution mode.
 */
public enum TurkuFfmpegExecutionMode {
    DISABLED,
    CONFIG_MISSING,
    CONFIG_INVALID,
    DRY_RUN,
    SIMULATED_SUCCESS,
    FAILED_SAFE
}
