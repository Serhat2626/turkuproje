package com.anadoluatlasi.backend.review;

/**
 * FP-013 Task 02: Sources that can trigger review handoff.
 *
 * <p>No PUBLISH/EXPORT/PUBLIC_DELIVERY/CDN/SIGNED_URL/READY_FOR_PUBLISH values.</p>
 */
public enum ReviewHandoffSource {

    /** Content project generation completed. */
    CONTENT_PROJECT_GENERATION,

    /** Scenario generation completed. */
    SCENARIO_GENERATION,

    /** Storyboard generation completed. */
    STORYBOARD_GENERATION,

    /** Publishing package generation completed. */
    PACKAGE_GENERATION,

    /** Image generation completed. */
    IMAGE_GENERATION,

    /** Storage handoff completed (asset stored successfully). */
    STORAGE_HANDOFF
}
