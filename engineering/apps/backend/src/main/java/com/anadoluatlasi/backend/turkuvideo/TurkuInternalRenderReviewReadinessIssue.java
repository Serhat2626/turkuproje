package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-026 Task 03: Review readiness issue codes.
 */
public enum TurkuInternalRenderReviewReadinessIssue {
    JOB_NOT_READY,
    READINESS_BLOCKED,
    ARTIFACT_VERIFICATION_FAILED,
    HUMAN_REVIEW_REQUIRED,
    PUBLIC_DELIVERY_FORBIDDEN,
    AUTO_APPROVAL_FORBIDDEN,
    UNSAFE_INPUT_REJECTED
}
