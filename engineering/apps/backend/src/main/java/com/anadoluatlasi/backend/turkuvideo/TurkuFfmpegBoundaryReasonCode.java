package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-028 Task 01: Execution boundary reason codes.
 */
public enum TurkuFfmpegBoundaryReasonCode {
    DISABLED_BY_DEFAULT,
    DRY_RUN_SAFE,
    SIMULATED_SAFE,
    EXECUTED_INTERNAL_SAFE,
    ENVIRONMENT_DENIED,
    MODE_DISABLED,
    BINARY_UNAVAILABLE,
    INTERNAL_FLAG_NOT_SET,
    REVIEW_NOT_APPROVED,
    DISPATCH_NOT_READY,
    BOUNDS_EXCEEDED,
    TIMEOUT_EXCEEDED,
    PUBLIC_DELIVERY_FORBIDDEN,
    AUTO_APPROVAL_FORBIDDEN
}
