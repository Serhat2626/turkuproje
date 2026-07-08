package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-035: Internal artifact lifecycle status for LOCAL-only generated render artifacts.
 */
public enum TurkuRenderArtifactLifecycleStatus {
    CREATED_INTERNAL_ONLY,
    GENERATED_TEMPORARY,
    VERIFIED_METADATA_ONLY,
    CLEANUP_SCHEDULED_OR_REGISTERED,
    CLEANUP_CONFIRMED_OR_BEST_EFFORT,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
