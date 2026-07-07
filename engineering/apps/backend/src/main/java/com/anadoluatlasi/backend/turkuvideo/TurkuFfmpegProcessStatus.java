package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-030 Task 01: Process execution status.
 */
public enum TurkuFfmpegProcessStatus {
    NOT_STARTED,
    GATE_DENIED,
    REQUEST_INVALID,
    PROCESS_READY,
    PROCESS_STARTED,
    PROCESS_COMPLETED_SAFE,
    PROCESS_TIMED_OUT,
    PROCESS_FAILED_SAFE,
    PROCESS_SKIPPED
}
