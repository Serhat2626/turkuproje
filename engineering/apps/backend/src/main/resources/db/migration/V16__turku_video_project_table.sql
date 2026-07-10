-- ============================================================================
-- V16: TurkuVideoProject Table — Türkü Video Proje Ana Tablosu
-- ============================================================================
-- Hibrit strateji: temel alanlar columnar, plan/manifest objeleri JSONB.
-- PostgreSQL 12+ compatible. UUID generated application-side.
-- No public URL, signed URL, CDN URL, credential, or secret columns.
-- ============================================================================

CREATE TABLE IF NOT EXISTS turku_video_projects (
    id                          UUID PRIMARY KEY,
    turku_name                  VARCHAR(200) NOT NULL,
    region                      VARCHAR(100),
    source_notes                VARCHAR(500),
    mood                        VARCHAR(200),
    target_duration_minutes     INTEGER,
    narration_style             VARCHAR(100),
    visual_style                VARCHAR(100),
    workflow_stage              VARCHAR(50) NOT NULL,
    source_confidence           VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    needs_human_review          BOOLEAN NOT NULL DEFAULT TRUE,
    copyright_review_required   BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_actor_reference  VARCHAR(100),
    provider_mode               VARCHAR(50) NOT NULL DEFAULT 'DETERMINISTIC_STUB',

    -- Hibrit JSONB alanları (plan/manifest objeleri)
    story_package_json          JSONB,
    scenario_plan_json          JSONB,
    storyboard_plan_json        JSONB,
    scene_asset_plan_json       JSONB,
    image_batch_json            JSONB,
    composition_plan_json       JSONB,
    render_manifest_json        JSONB,
    render_job_json             JSONB,

    -- Auditing
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Indexes
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_tvp_workflow_stage ON turku_video_projects(workflow_stage);
CREATE INDEX IF NOT EXISTS idx_tvp_turku_name ON turku_video_projects(turku_name);
CREATE INDEX IF NOT EXISTS idx_tvp_source_confidence ON turku_video_projects(source_confidence);
CREATE INDEX IF NOT EXISTS idx_tvp_provider_mode ON turku_video_projects(provider_mode);
CREATE INDEX IF NOT EXISTS idx_tvp_created_at ON turku_video_projects(created_at);
CREATE INDEX IF NOT EXISTS idx_tvp_needs_review ON turku_video_projects(needs_human_review);
