package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-033: Manual local FFmpeg run evidence status.
 */
public enum TurkuFfmpegManualRunEvidenceStatus {
    NOT_ATTEMPTED,
    MANUAL_FLAG_NOT_ENABLED,
    PROCESS_DISABLED,
    GATE_DENIED,
    ENVIRONMENT_DENIED,
    FFMPEG_UNAVAILABLE,
    REAL_LOCAL_EXECUTION_STARTED,
    REAL_LOCAL_EXECUTION_SUCCEEDED,
    REAL_LOCAL_EXECUTION_FAILED_SAFE,
    VERIFICATION_METADATA_CREATED
}
