package com.anadoluatlasi.backend.turkuvideo;

/**
 * RP-002: Safe storyboard draft statuses.
 */
public enum TurkuStoryboardDraftStatus {
    CREATED_INTERNAL_ONLY,
    REQUEST_ACCEPTED,
    DRAFT_ORCHESTRATION_STARTED,
    SCENES_GENERATED_SAFE,
    READY_FOR_ADMIN_PREVIEW,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
