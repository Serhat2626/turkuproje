package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-025 Task 03: Internal render readiness status.
 */
public enum TurkuInternalRenderReadinessStatus {
    NOT_READY,
    READY_INTERNAL_ONLY,
    BLOCKED_BY_CONFIG,
    BLOCKED_BY_WORKSPACE,
    BLOCKED_BY_EXECUTION_BOUNDARY,
    BLOCKED_BY_ARTIFACT_VERIFICATION,
    BLOCKED_BY_HUMAN_REVIEW
}
