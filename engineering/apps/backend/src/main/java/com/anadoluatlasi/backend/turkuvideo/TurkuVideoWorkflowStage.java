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

    /** 3-Stage Pipeline: Kaynaklı araştırma tamamlandı */
    RESEARCH_COMPLETED,

    /** 3-Stage Pipeline: Hikâye taslağı oluşturuldu */
    STORY_DRAFT_COMPLETED,

    /** Story context package prepared (nihai hikâye hazır) */
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

    /** Render job execution in progress */
    RENDER_JOB_RUNNING,

    /** Render completed internally — NOT publish/export ready */
    RENDER_JOB_COMPLETED_INTERNAL,

    /** Render failed safely — no partial output exposed */
    RENDER_JOB_FAILED_SAFE,

    /** Internal render artifact metadata available — no download/export */
    INTERNAL_RENDER_ARTIFACT_READY,

    /** Internal preview package assembled */
    INTERNAL_PREVIEW_READY,

    /** Admin requested revision at any stage */
    NEEDS_REVISION,

    /** Blocked by policy, copyright, or error */
    BLOCKED,

    /** Project archived (not deleted) */
    ARCHIVED
}
