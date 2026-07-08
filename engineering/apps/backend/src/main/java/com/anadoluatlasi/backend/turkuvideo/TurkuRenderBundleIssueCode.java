package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-036: Bundle-level issue codes.
 */
public enum TurkuRenderBundleIssueCode {
    NONE,
    MISSING_PRIMARY_VIDEO,
    DUPLICATE_PRIMARY_VIDEO,
    TOO_MANY_ARTIFACTS,
    TOTAL_SIZE_EXCEEDS_LIMIT,
    UNSUPPORTED_ROLE,
    ARTIFACT_REJECTED,
    ARTIFACT_FAILED,
    BUNDLE_REF_MISSING
}
