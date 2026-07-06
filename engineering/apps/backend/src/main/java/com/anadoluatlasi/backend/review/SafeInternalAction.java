package com.anadoluatlasi.backend.review;

/**
 * FP-014 Task 02: Safe internal-only next action hints for approved review targets.
 *
 * <p>These actions are internal administrative hints. They do NOT trigger
 * publish, export, public delivery, CDN delivery, or signed URL generation.</p>
 *
 * <p>Forbidden values that do NOT exist:</p>
 * <ul>
 *   <li>PUBLISH</li>
 *   <li>EXPORT</li>
 *   <li>PUBLIC_DELIVERY</li>
 *   <li>CREATE_PUBLIC_URL</li>
 *   <li>CREATE_SIGNED_URL</li>
 *   <li>CDN_DELIVERY</li>
 *   <li>USER_MEDIA_LIBRARY</li>
 *   <li>READY_FOR_PUBLISH</li>
 *   <li>PUBLISHED</li>
 *   <li>EXPORTED</li>
 *   <li>COMPLETED</li>
 * </ul>
 */
public enum SafeInternalAction {

    /** View safe admin preview of the approved artifact. Read-only. */
    VIEW_SAFE_PREVIEW,

    /** View approval audit details and timeline. Read-only. */
    REVIEW_APPROVAL_DETAILS,

    /** Send approved artifact back for re-review. State change. */
    REQUEST_INTERNAL_REVISION,

    /**
     * Informational marker: artifact may be considered for future export.
     * This action is NON-EXECUTING — it does NOT trigger export, create URLs,
     * create delivery artifacts, or make content externally accessible.
     */
    PREPARE_FOR_FUTURE_EXPORT,

    /**
     * Informational marker: mark artifact as an internal ready candidate.
     * This is metadata-only — no publish, export, or external side effect.
     */
    MARK_AS_INTERNAL_READY_CANDIDATE
}
