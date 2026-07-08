package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-042: Render review package issue codes.
 */
public enum TurkuRenderReviewPackageIssueCode {
    NONE,
    NO_PRIMARY_ARTIFACT,
    ARTIFACT_NOT_VERIFIED,
    ARTIFACT_REJECTED,
    ARTIFACT_FAILED,
    ARTIFACT_ROLE_UNKNOWN,
    TOO_MANY_ARTIFACTS,
    DUPLICATE_ARTIFACT_REF,
    MISSING_ARTIFACT_REF,
    LIFECYCLE_STATUS_INVALID,
    BUNDLE_REF_MISSING,
    METADATA_INCOMPLETE
}
