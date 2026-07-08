package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-047: Storyboard internal render chain status.
 * No READY_FOR_PUBLISH. No COMPLETED. No AUTO_APPROVED.
 */
public enum TurkuStoryboardInternalRenderChainStatus {
    CREATED_INTERNAL_ONLY,
    STORYBOARD_ACCEPTED,
    SCENE_PLANS_CREATED,
    MULTI_SCENE_JOB_READY,
    SYNTHETIC_CLIPS_GENERATED,
    STITCHED_ARTIFACT_VERIFIED,
    REVIEW_PACKAGE_READY_INTERNAL_ONLY,
    ADMIN_METADATA_VISIBLE,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
