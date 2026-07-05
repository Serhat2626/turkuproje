package com.anadoluatlasi.backend.review;

/**
 * FP-012 Task 04: Safe review target types.
 *
 * <p>Identifies what kind of content item is under review.</p>
 *
 * <p>Forbidden target types that do NOT exist:</p>
 * <ul>
 *   <li>PUBLIC_MEDIA</li>
 *   <li>CDN_ASSET</li>
 *   <li>EXPORT_FILE</li>
 *   <li>PUBLISHED_CONTENT</li>
 * </ul>
 */
public enum ReviewTargetType {

    /** Content project (FP-001). */
    CONTENT_PROJECT,

    /** Scenario / script draft (FP-002). */
    SCENARIO,

    /** Storyboard draft (FP-003). */
    STORYBOARD,

    /** Platform package (FP-004). */
    PACKAGE,

    /** Image generation asset (FP-007). */
    IMAGE_ASSET
}
