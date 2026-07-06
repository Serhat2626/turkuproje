package com.anadoluatlasi.backend.review;

/**
 * FP-017 Task 02: Safe non-mutating operational control hints.
 *
 * <p>These hints suggest safe next steps to the admin. They are
 * informational only — the observability layer does NOT execute
 * any of these actions. Execution requires the admin to use the
 * appropriate existing action endpoint.</p>
 *
 * <p>Forbidden values that do NOT exist:</p>
 * <ul>
 *   <li>PUBLISH</li>
 *   <li>EXPORT</li>
 *   <li>APPROVE</li>
 *   <li>AUTO_FIX</li>
 *   <li>REGENERATE</li>
 *   <li>PROVIDER_RETRY</li>
 *   <li>STORAGE_RETRY</li>
 *   <li>CREATE_PUBLIC_URL</li>
 *   <li>CREATE_SIGNED_URL</li>
 *   <li>CDN_DELIVERY</li>
 * </ul>
 */
public enum SafeOperationalControlHint {

    /** Suggest admin refresh the pipeline overview. */
    REFRESH_OVERVIEW,

    /** Suggest admin re-check internal readiness for a target. */
    RECHECK_READINESS,

    /** Suggest admin view safe preview for a target. */
    VIEW_SAFE_PREVIEW,

    /** Suggest admin review pipeline detail for a target. */
    REVIEW_PIPELINE_DETAIL,

    /** Suggest admin flag item for manual investigation. */
    REQUEST_MANUAL_INVESTIGATION
}
