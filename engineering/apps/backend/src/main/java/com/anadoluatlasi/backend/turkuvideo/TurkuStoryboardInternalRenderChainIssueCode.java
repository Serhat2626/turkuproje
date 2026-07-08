package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-047: Storyboard internal render chain issue codes.
 */
public enum TurkuStoryboardInternalRenderChainIssueCode {
    NONE,
    ADAPTER_FAILED,
    SCENE_PLAN_FAILED,
    MULTI_SCENE_JOB_FAILED,
    CLIP_GENERATION_FAILED,
    STITCHING_FAILED,
    LIFECYCLE_VERIFICATION_FAILED,
    REVIEW_PACKAGE_FAILED,
    ADMIN_VISIBILITY_UNAVAILABLE,
    SAFETY_VIOLATION
}
