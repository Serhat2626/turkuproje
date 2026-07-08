package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-044: Internal story-to-video dry run issue codes.
 */
public enum TurkuInternalStoryVideoDryRunIssueCode {
    NONE,
    SCENE_PLAN_MISSING,
    SCENE_PLAN_INVALID,
    MULTI_SCENE_JOB_FAILED,
    CLIP_GENERATION_FAILED,
    STITCHING_FAILED,
    LIFECYCLE_VERIFICATION_FAILED,
    BUNDLE_MISSING,
    REVIEW_PACKAGE_MISSING,
    REVIEW_PACKAGE_REJECTED,
    ADMIN_VISIBILITY_UNAVAILABLE,
    SAFETY_VIOLATION
}
