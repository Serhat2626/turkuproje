package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-020 Task 02: Provider execution mode for Türkü video projects.
 *
 * <p>Controls whether generation uses deterministic stubs or real providers.
 * No forbidden modes (AUTO_APPROVED, PUBLISHED, etc.).</p>
 */
public enum TurkuProviderMode {

    /** Default. No external provider calls. FP-019 deterministic behavior. */
    DETERMINISTIC_STUB,

    /** Real provider enabled, output is draft pending human review. */
    PROVIDER_DRAFT,

    /** Provider explicitly disabled by admin. Stub output used. */
    PROVIDER_DISABLED,

    /** Provider was called but failed safely. No retry without admin action. */
    PROVIDER_FAILED_SAFE,

    /** Provider succeeded, output awaiting mandatory human review. */
    HUMAN_REVIEW_REQUIRED
}
