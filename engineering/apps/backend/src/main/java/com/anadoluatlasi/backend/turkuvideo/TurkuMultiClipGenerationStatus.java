package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-039: Multi-clip generation status.
 * No READY_FOR_PUBLISH. No COMPLETED.
 */
public enum TurkuMultiClipGenerationStatus {
    CREATED_INTERNAL_ONLY,
    GATED_FOR_LOCAL_MANUAL_RUN,
    GENERATING_SYNTHETIC_CLIPS,
    GENERATED_CLIPS_VERIFIED,
    REJECTED_UNSAFE,
    FAILED_SAFE,
    GATE_DENIED,
    MANUAL_FLAG_NOT_ENABLED,
    PROCESS_DISABLED,
    FFMPEG_UNAVAILABLE,
    JOB_NOT_READY
}
