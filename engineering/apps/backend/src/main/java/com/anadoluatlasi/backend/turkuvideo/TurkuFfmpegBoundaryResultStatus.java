package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-028 Task 01: Execution boundary result status.
 */
public enum TurkuFfmpegBoundaryResultStatus {
    NOT_EXECUTED,
    DRY_RUN_COMPLETE,
    SIMULATED_COMPLETE,
    EXECUTED_INTERNAL,
    FAILED_SAFE,
    DENIED
}
