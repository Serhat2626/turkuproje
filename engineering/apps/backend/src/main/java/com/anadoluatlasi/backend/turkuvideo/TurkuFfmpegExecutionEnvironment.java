package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-028 Task 01: Execution environment.
 * Real FFmpeg allowed only in LOCAL/DEV/TEST.
 */
public enum TurkuFfmpegExecutionEnvironment {
    LOCAL,
    DEV,
    TEST,
    STAGING,
    PROD,
    UNKNOWN;

    public boolean isRealExecutionAllowed() {
        return this == LOCAL || this == DEV || this == TEST;
    }
}
