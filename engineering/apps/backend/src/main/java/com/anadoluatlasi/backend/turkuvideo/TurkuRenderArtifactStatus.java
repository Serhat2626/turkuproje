package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-023 Task 04: Render artifact status.
 */
public enum TurkuRenderArtifactStatus {
    CREATED,
    VALIDATED,
    RETENTION_ACTIVE,
    CLEANUP_ELIGIBLE,
    CLEANUP_PLANNED,
    CLEANED,
    REJECTED
}
