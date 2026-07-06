package com.anadoluatlasi.backend.review;

/**
 * FP-017 Task 02: Safe internal-only diagnostic types for workflow observability.
 *
 * <p>Represents categories of items needing admin attention in the
 * internal workflow. These are observational only — they do NOT trigger
 * any workflow action, publish, export, or provider/storage operation.</p>
 *
 * <p>Forbidden values that do NOT exist:</p>
 * <ul>
 *   <li>PUBLISH_FAILED</li>
 *   <li>EXPORT_FAILED</li>
 *   <li>CDN_FAILED</li>
 *   <li>PROVIDER_ERROR</li>
 *   <li>STORAGE_ERROR</li>
 *   <li>SIGNED_URL_ERROR</li>
 * </ul>
 */
public enum SafeWorkflowDiagnosticType {

    /** Review item not updated within staleness threshold. */
    STALE_REVIEW,

    /** Target's preview is not available. */
    PREVIEW_UNAVAILABLE,

    /** Internal readiness cannot be determined. */
    READINESS_UNAVAILABLE,

    /** Pipeline aggregation cannot determine stage. */
    PIPELINE_UNAVAILABLE,

    /** Revision has been requested on an approved item. */
    REVISION_REQUESTED,

    /** Target entity is missing from review records. */
    MISSING_TARGET,

    /** Internal workflow is blocked (e.g., multiple blocking reasons). */
    BLOCKED_INTERNAL_WORKFLOW,

    /** Visibility is partially degraded for this item. */
    DEGRADED_VISIBILITY
}
