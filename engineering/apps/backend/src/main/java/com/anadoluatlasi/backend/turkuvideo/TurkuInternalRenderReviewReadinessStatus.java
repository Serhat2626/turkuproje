package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-026 Task 03: Review readiness status derived from FP-025 readiness/safety-chain.
 */
public enum TurkuInternalRenderReviewReadinessStatus {
    NOT_READY_FOR_REVIEW,
    READY_FOR_HUMAN_REVIEW,
    IN_REVIEW,
    APPROVED_INTERNAL_ONLY,
    REJECTED,
    BLOCKED,
    EXPIRED
}
