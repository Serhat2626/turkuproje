package com.anadoluatlasi.backend.review;

/**
 * FP-013 Task 03: Safe preview modes for admin review context.
 *
 * <p>No PUBLIC_URL, SIGNED_URL, CDN, RAW_PAYLOAD, BASE64, PROVIDER_URL,
 * STORAGE_KEY, EXPORT, PUBLISH values.</p>
 */
public enum AdminPreviewMode {

    /** Text-based content project summary. */
    TEXT_SUMMARY,

    /** Image asset preview (availability/label only — no bytes/URL). */
    IMAGE_ASSET,

    /** Multi-platform package summary. */
    PACKAGE_SUMMARY,

    /** Storyboard draft summary with scene info. */
    STORYBOARD_SUMMARY,

    /** Scenario draft summary with beat info. */
    SCENARIO_SUMMARY,

    /** Preview is not available for this target. */
    UNAVAILABLE
}
