package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-039: Multi-clip generation issue codes.
 */
public enum TurkuMultiClipGenerationIssueCode {
    NONE,
    GATE_CHECK_FAILED,
    JOB_NOT_READY,
    CLIP_GENERATION_FAILED,
    CLIP_VERIFICATION_FAILED,
    CLIP_DURATION_EXCEEDED,
    CLIP_FPS_EXCEEDED,
    CLIP_RESOLUTION_EXCEEDED,
    CLIP_SIZE_EXCEEDED,
    CLIP_FORMAT_UNSUPPORTED,
    PARTIAL_GENERATION_FAILURE,
    ALL_CLIPS_FAILED
}
