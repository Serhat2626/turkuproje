package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-024 Task 05: Artifact verification status.
 */
public enum TurkuRenderArtifactVerificationStatus {
    PENDING,
    VERIFIED,
    FORMAT_REJECTED,
    BOUNDS_EXCEEDED,
    REF_UNSAFE,
    METADATA_INCOMPLETE,
    FAILED_SAFE
}
