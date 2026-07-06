package com.anadoluatlasi.backend.review;

/**
 * FP-014 Task 02: Safe availability states for internal approval readiness.
 *
 * <p>Indicates whether an artifact is internally ready after review,
 * without implying publication, export, or public delivery.</p>
 *
 * <p>Forbidden values that do NOT exist:</p>
 * <ul>
 *   <li>PUBLISHABLE</li>
 *   <li>EXPORTABLE</li>
 *   <li>PUBLICLY_AVAILABLE</li>
 *   <li>CDN_READY</li>
 *   <li>SIGNED_URL_READY</li>
 * </ul>
 */
public enum InternalApprovalReadinessAvailability {

    /** Artifact is APPROVED_FOR_INTERNAL_USE — internally ready. */
    READY,

    /** Artifact review is not yet complete — not approved. */
    NOT_APPROVED,

    /** Artifact was rejected — not internally ready. */
    REJECTED,

    /** Target review record was not found. */
    MISSING_TARGET,

    /** Target is not safe for readiness evaluation. */
    UNSAFE_TARGET,

    /** Preview is not available for this target. */
    PREVIEW_UNAVAILABLE,

    /** Generic unavailable state. */
    UNAVAILABLE
}
