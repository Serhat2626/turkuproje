package com.anadoluatlasi.backend.review;

/**
 * FP-013 Task 03: Safe preview availability states.
 *
 * <p>Does not expose provider/storage internals in messages.</p>
 */
public enum AdminPreviewAvailability {

    /** Preview content is available and safe to display. */
    AVAILABLE,

    /** Preview is not available (generic). */
    UNAVAILABLE,

    /** Target is not a reviewable type. */
    NOT_REVIEWABLE,

    /** Target entity was not found. */
    MISSING_TARGET,

    /** Target exists but preview cannot be safely rendered. */
    UNSAFE_TARGET,

    /** Storage is not available (no internal detail exposed). */
    STORAGE_UNAVAILABLE,

    /** Provider is not available (no internal detail exposed). */
    PROVIDER_UNAVAILABLE
}
