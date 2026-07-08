package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-036: Internal render bundle status.
 * No READY_FOR_PUBLISH. No COMPLETED. No generic status update.
 */
public enum TurkuRenderBundleStatus {
    CREATED_INTERNAL_ONLY,
    ARTIFACTS_ATTACHED,
    ARTIFACTS_VERIFIED,
    READY_FOR_INTERNAL_REVIEW_ONLY,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
