package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-026 Task 02: Human review gate states.
 * APPROVED_FOR_INTERNAL_NEXT_STEP is internal-only — NOT publish/export/download.
 */
public enum TurkuInternalRenderReviewState {
    REVIEW_NOT_REQUIRED,
    REVIEW_REQUIRED,
    READY_FOR_REVIEW,
    IN_REVIEW,
    APPROVED_FOR_INTERNAL_NEXT_STEP,
    REJECTED_BY_HUMAN_REVIEW,
    BLOCKED_BY_SAFETY,
    EXPIRED_REVIEW;

    public boolean isTerminal() {
        return this == APPROVED_FOR_INTERNAL_NEXT_STEP
                || this == REJECTED_BY_HUMAN_REVIEW
                || this == BLOCKED_BY_SAFETY
                || this == EXPIRED_REVIEW;
    }
}
