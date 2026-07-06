package com.anadoluatlasi.backend.review;

/**
 * FP-016 Task 02: Safe internal-only pipeline stages for aggregation view.
 *
 * <p>Represents derived pipeline position based on review state,
 * internal readiness, and revision request status.</p>
 *
 * <p>These stages are internal operational indicators only.
 * They do NOT imply readiness for publication, export, or public delivery.</p>
 *
 * <p>Forbidden values that do NOT exist:</p>
 * <ul>
 *   <li>READY_FOR_PUBLISH</li>
 *   <li>PUBLISHABLE</li>
 *   <li>PUBLISHED</li>
 *   <li>EXPORTABLE</li>
 *   <li>EXPORTED</li>
 *   <li>CDN_READY</li>
 *   <li>PUBLIC_DELIVERY</li>
 *   <li>SIGNED_URL_READY</li>
 * </ul>
 */
public enum SafePipelineStage {

    /** Artifact exists, not yet submitted for review. */
    GENERATED,

    /** Review submitted, awaiting reviewer. */
    REVIEW_PENDING,

    /** Reviewer actively reviewing. */
    IN_REVIEW,

    /** Reviewer requested changes (pre-approval path). */
    CHANGES_REQUESTED,

    /** Approved for internal use — readiness not yet confirmed READY. */
    INTERNALLY_APPROVED,

    /** Approved + internal readiness confirmed READY. */
    INTERNAL_READY,

    /** Previously approved, then revision requested. */
    INTERNAL_REVISION_REQUESTED,

    /** Rejected by reviewer. */
    REJECTED,

    /** Target missing, unsafe, or otherwise unavailable. */
    UNAVAILABLE
}
