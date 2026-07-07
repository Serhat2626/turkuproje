package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 02: Türkü video project workflow stages.
 *
 * <p>Controlled state machine for internal video production workflow.
 * No forbidden states (PUBLISHED, EXPORTED, etc.) exist in this enum.</p>
 */
public enum TurkuVideoWorkflowStage {

    /** Project bootstrapped, no generation yet */
    DRAFT_CREATED,

    /** Story context package prepared */
    STORY_CONTEXT_PREPARED,

    /** Scenario/narrative beats prepared */
    SCENARIO_PREPARED,

    /** Scene-by-scene storyboard prepared */
    STORYBOARD_PREPARED,

    /** Per-scene asset plan prepared */
    SCENE_ASSET_PLAN_PREPARED,

    /** Image generation batch prepared */
    IMAGE_BATCH_PREPARED,

    /** All scene images generated successfully */
    IMAGE_BATCH_COMPLETED,

    /** Some images generated, some failed */
    IMAGE_BATCH_PARTIAL,

    /** Video composition plan assembled from storyboard + images */
    COMPOSITION_PLAN_PREPARED,

    /** Render manifest prepared from composition plan */
    RENDER_MANIFEST_PREPARED,

    /** Internal render job prepared from manifest */
    RENDER_JOB_PREPARED,

    /** Internal preview package assembled */
    INTERNAL_PREVIEW_READY,

    /** Admin requested revision at any stage */
    NEEDS_REVISION,

    /** Blocked by policy, copyright, or error */
    BLOCKED,

    /** Project archived (not deleted) */
    ARCHIVED
}
