package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-024 Task 04: FFmpeg execution failure code.
 */
public enum TurkuFfmpegExecutionFailureCode {
    EXECUTION_DISABLED,
    CONFIG_MISSING,
    CONFIG_INVALID,
    WORKSPACE_INVALID,
    FILE_REF_INVALID,
    COMMAND_PLAN_INVALID,
    COMMAND_PLAN_UNSAFE,
    TIMEOUT_EXCEEDED,
    LIMITS_EXCEEDED,
    DRY_RUN_FAILED,
    SIMULATED_FAILURE,
    SAFETY_VIOLATION,
    GENERAL_FAILURE
}
