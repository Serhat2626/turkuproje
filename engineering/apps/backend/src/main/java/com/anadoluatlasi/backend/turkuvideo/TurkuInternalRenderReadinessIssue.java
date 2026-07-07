package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-025 Task 03: Internal render readiness issue codes.
 * Safe bounded codes only — no raw messages.
 */
public enum TurkuInternalRenderReadinessIssue {
    CONFIG_DISABLED,
    CONFIG_INVALID,
    WORKSPACE_REF_MISSING,
    WORKSPACE_REF_INVALID,
    EXECUTION_BOUNDARY_DISABLED,
    EXECUTION_PLAN_UNSAFE,
    ARTIFACT_REF_MISSING,
    ARTIFACT_REF_INVALID,
    ARTIFACT_VERIFICATION_FAILED,
    HUMAN_REVIEW_REQUIRED,
    PUBLIC_DELIVERY_FORBIDDEN,
    AUTO_APPROVAL_FORBIDDEN,
    UNSAFE_INPUT_REJECTED
}
