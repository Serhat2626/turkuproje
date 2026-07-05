package com.anadoluatlasi.backend.review;

/**
 * FP-012 Task 02: Safe actor roles for review workflow.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>CREATOR may submit or return draft work, but must NOT self-approve</li>
 *   <li>REVIEWER may start review, request changes, approve for internal use, reject, add notes</li>
 *   <li>ADMIN may perform reviewer actions, but cannot bypass publish/export gates (they don't exist)</li>
 *   <li>SYSTEM must NOT approve, reject, publish, export, or perform human review actions</li>
 * </ul>
 */
public enum ReviewActorRole {

    /** Content creator — can submit and return to draft, cannot self-approve. */
    CREATOR,

    /** Content reviewer — can review, approve for internal use, reject. */
    REVIEWER,

    /** Admin — same as reviewer for FP-012; no publish/export/completed. */
    ADMIN,

    /**
     * System/automated — can only set AI_GENERATED conceptually.
     * Must NOT approve, reject, publish, export, or perform human review actions.
     */
    SYSTEM;

    /**
     * Whether this role can perform human review actions (approve/reject/request changes).
     */
    public boolean canPerformReview() {
        return this == REVIEWER || this == ADMIN;
    }

    /**
     * Whether this role can submit content for review.
     */
    public boolean canSubmitForReview() {
        return this == CREATOR || this == REVIEWER || this == ADMIN;
    }

    /**
     * Whether this role can add review notes.
     */
    public boolean canAddReviewNote() {
        return this != SYSTEM;
    }
}
