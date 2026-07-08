package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-024 Task 05 + FP-035: Artifact verification status.
 */
public enum TurkuRenderArtifactVerificationStatus {
    PENDING,
    VERIFIED,
    FORMAT_REJECTED,
    BOUNDS_EXCEEDED,
    REF_UNSAFE,
    METADATA_INCOMPLETE,
    FAILED_SAFE,
    // FP-035 additions
    PASSED,
    REJECTED_FORMAT,
    REJECTED_DURATION,
    REJECTED_FPS,
    REJECTED_RESOLUTION,
    REJECTED_SIZE,
    REJECTED_MULTIPLE,
    NOT_VERIFIED
}
