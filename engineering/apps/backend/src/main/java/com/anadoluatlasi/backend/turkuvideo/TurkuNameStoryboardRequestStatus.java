package com.anadoluatlasi.backend.turkuvideo;

/**
 * RP-001: Storyboard request statuses. No COMPLETED/PUBLISHED/AUTO_APPROVED.
 */
public enum TurkuNameStoryboardRequestStatus {
    CREATED_INTERNAL_ONLY,
    TURKU_NAME_ACCEPTED,
    COPYRIGHT_SAFE_INTENT_CREATED,
    READY_FOR_STORYBOARD_GENERATION_BOUNDARY,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
