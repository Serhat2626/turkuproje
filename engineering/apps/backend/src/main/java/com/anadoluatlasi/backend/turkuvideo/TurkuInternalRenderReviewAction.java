package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-026 Task 02: Explicit review actions.
 * No AUTO_APPROVE / UPDATE_STATUS / SET_STATUS / PATCH_STATUS / PUBLISH / EXPORT / DOWNLOAD.
 */
public enum TurkuInternalRenderReviewAction {
    REQUIRE_HUMAN_REVIEW,
    MARK_READY_FOR_REVIEW,
    START_HUMAN_REVIEW,
    APPROVE_FOR_INTERNAL_NEXT_STEP,
    REJECT_HUMAN_REVIEW,
    BLOCK_FOR_SAFETY,
    EXPIRE_HUMAN_REVIEW
}
