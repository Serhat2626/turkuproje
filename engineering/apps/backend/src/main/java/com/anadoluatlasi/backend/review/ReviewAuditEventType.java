package com.anadoluatlasi.backend.review;

/**
 * FP-012 Task 02: Safe audit event types for review workflow.
 *
 * <p>Forbidden event names that do NOT exist:</p>
 * <ul>
 *   <li>PUBLISHED</li>
 *   <li>EXPORTED</li>
 *   <li>COMPLETED</li>
 *   <li>READY_FOR_PUBLISH</li>
 *   <li>PUBLIC_URL_CREATED</li>
 *   <li>SIGNED_URL_CREATED</li>
 *   <li>AUTO_APPROVED</li>
 * </ul>
 */
public enum ReviewAuditEventType {

    /** Content submitted for human review. */
    REVIEW_SUBMITTED,

    /** Reviewer claimed and started review. */
    REVIEW_STARTED,

    /** Reviewer requested modifications. */
    CHANGES_REQUESTED,

    /** Reviewer approved for internal use only — NOT publish/export. */
    INTERNAL_APPROVAL_GRANTED,

    /** Reviewer rejected content. */
    REVIEW_REJECTED,

    /** Content returned to draft state. */
    RETURNED_TO_DRAFT,

    /** Review note added (no state change). */
    REVIEW_NOTE_ADDED,

    /** Forbidden transition was attempted and rejected. */
    REVIEW_TRANSITION_REJECTED
}
