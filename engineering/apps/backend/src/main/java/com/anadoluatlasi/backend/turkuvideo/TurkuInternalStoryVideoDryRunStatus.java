package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-044: Internal story-to-video dry run status.
 * No READY_FOR_PUBLISH. No COMPLETED. No public delivery.
 */
public enum TurkuInternalStoryVideoDryRunStatus {
    CREATED_INTERNAL_ONLY,
    STORY_SCENES_PLANNED,
    MULTI_SCENE_JOB_READY,
    SYNTHETIC_CLIPS_GENERATED,
    SYNTHETIC_CLIPS_STITCHED,
    REVIEW_PACKAGE_READY_INTERNAL_ONLY,
    ADMIN_METADATA_VISIBLE,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
