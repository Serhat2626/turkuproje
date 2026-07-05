package com.anadoluatlasi.backend.review;

/**
 * FP-012 Task 02: Safe internal review states for admin/human review workflow.
 *
 * <p>These states govern the human review lifecycle. AI-generated content
 * must pass through explicit human review before reaching
 * APPROVED_FOR_INTERNAL_USE.</p>
 *
 * <p>Critical rules:</p>
 * <ul>
 *   <li>APPROVED_FOR_INTERNAL_USE is NOT publish/export approval</li>
 *   <li>No automatic transitions to APPROVED_FOR_INTERNAL_USE</li>
 *   <li>REJECTED is terminal for the current version</li>
 *   <li>READY_FOR_PUBLISH, COMPLETED, PUBLISHED, EXPORTED do NOT exist</li>
 * </ul>
 */
public enum ReviewState {

    /** Content project created, no AI generation yet. */
    DRAFT,

    /** AI output produced — awaiting human review. */
    AI_GENERATED,

    /** Explicitly queued for reviewer attention. */
    NEEDS_REVIEW,

    /** Reviewer has claimed and started review. */
    IN_REVIEW,

    /** Reviewer requested modifications. */
    CHANGES_REQUESTED,

    /** Reviewer approved — for internal use only. NOT publish/export approval. */
    APPROVED_FOR_INTERNAL_USE,

    /** Reviewer rejected — terminal for this version. */
    REJECTED;

    /**
     * Whether this state is terminal (no further transitions allowed).
     */
    public boolean isTerminal() {
        return this == REJECTED;
    }

    /**
     * Whether this state requires explicit human action to proceed.
     */
    public boolean requiresHumanAction() {
        return this == NEEDS_REVIEW
                || this == IN_REVIEW
                || this == CHANGES_REQUESTED;
    }

    /**
     * Whether content is approved for internal use.
     * This is NOT publish/export approval.
     */
    public boolean isApprovedForInternalUse() {
        return this == APPROVED_FOR_INTERNAL_USE;
    }

    /**
     * Whether content has been rejected.
     */
    public boolean isRejected() {
        return this == REJECTED;
    }

    /**
     * Whether a review is actively in progress.
     */
    public boolean isReviewActive() {
        return this == IN_REVIEW;
    }

    /**
     * Whether this state represents AI-generated output not yet reviewed.
     * AI_GENERATED does NOT imply approval.
     */
    public boolean isAiGenerated() {
        return this == AI_GENERATED;
    }
}
