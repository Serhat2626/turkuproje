package com.anadoluatlasi.backend.review;

/**
 * FP-012 Task 02: Safe human/admin review action types.
 *
 * <p>Each action type maps to a specific allowed state transition.
 * No publish/export/auto-approve actions exist.</p>
 *
 * <p>Forbidden action concepts that do NOT exist:</p>
 * <ul>
 *   <li>PUBLISH</li>
 *   <li>EXPORT</li>
 *   <li>MARK_COMPLETED</li>
 *   <li>READY_FOR_PUBLISH</li>
 *   <li>AUTO_APPROVE</li>
 *   <li>GENERIC_STATUS_UPDATE</li>
 * </ul>
 */
public enum ReviewActionType {

    /** Submit content for human review. */
    SUBMIT_FOR_REVIEW,

    /** Reviewer claims and starts reviewing. */
    START_REVIEW,

    /** Reviewer requests modifications. */
    REQUEST_CHANGES,

    /** Reviewer approves for internal use only — NOT publish/export. */
    APPROVE_FOR_INTERNAL_USE,

    /** Reviewer rejects content. */
    REJECT,

    /** Return content to draft state for rework. */
    RETURN_TO_DRAFT,

    /** Add a review note — does NOT change workflow state. */
    ADD_REVIEW_NOTE,

    /**
     * FP-015 Task 02: Request internal revision for an approved item.
     * Narrow action — NOT a generic status update.
     * Only allowed from APPROVED_FOR_INTERNAL_USE (transition) or CHANGES_REQUESTED (note append).
     * Does NOT publish, export, or auto-approve.
     */
    REQUEST_INTERNAL_REVISION
}
