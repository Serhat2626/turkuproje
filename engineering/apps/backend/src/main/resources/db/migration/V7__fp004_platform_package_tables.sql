-- FP-004: Multi-Platform Content Packaging — Tables
-- Creates platform_package_drafts and platform_package_scene_usages tables.

-- gen_random_uuid() requires pgcrypto on PostgreSQL < 13
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── platform_package_drafts ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS platform_package_drafts (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_project_id              UUID NOT NULL REFERENCES content_projects(id),
    source_storyboard_draft_id      UUID NOT NULL REFERENCES storyboard_drafts(id),
    package_type                    VARCHAR(30) NOT NULL,
    status                          VARCHAR(20) NOT NULL,
    version                         INTEGER NOT NULL DEFAULT 1,

    -- Common package fields
    title                           VARCHAR(255) NOT NULL,
    caption_or_description          TEXT NOT NULL,
    hashtags_json                   TEXT,
    call_to_action                  VARCHAR(500),
    target_duration_seconds         INTEGER,
    aspect_ratio                    VARCHAR(10),
    content_structure_notes         TEXT,
    scene_usage_notes               TEXT,
    review_notes                    TEXT,

    -- YouTube Long-form metadata
    video_title                     VARCHAR(255),
    long_description                TEXT,
    chapter_outline_json            TEXT,
    pinned_comment_suggestion       TEXT,
    thumbnail_text_suggestion       VARCHAR(100),
    tags_json                       TEXT,

    -- Short-form / Reels metadata
    short_hook                      VARCHAR(500),
    short_caption                   TEXT,
    overlay_text_suggestions_json   TEXT,
    short_hashtag_set_json          TEXT,
    short_cta                       VARCHAR(500),
    duration_guidance               VARCHAR(100),

    -- AI metadata
    provider_type                   VARCHAR(20),
    model_name                      VARCHAR(50),
    generated_by                    VARCHAR(30),

    -- Review lifecycle
    edited_at                       TIMESTAMP,
    edited_by                       VARCHAR(100),
    approved_at                     TIMESTAMP,
    approved_by                     VARCHAR(100),
    rejected_at                     TIMESTAMP,
    rejected_by                     VARCHAR(100),
    rejection_reason                TEXT,

    -- Audit (BaseEntity)
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── platform_package_scene_usages ───────────────────────────────────

CREATE TABLE IF NOT EXISTS platform_package_scene_usages (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    platform_package_draft_id       UUID NOT NULL REFERENCES platform_package_drafts(id) ON DELETE CASCADE,
    scene_number                    INTEGER NOT NULL,
    source_storyboard_scene_id      UUID REFERENCES storyboard_scenes(id),
    usage_type                      VARCHAR(20) NOT NULL,
    included                        BOOLEAN NOT NULL DEFAULT true,
    adaptation_note                 TEXT,

    -- Audit (BaseEntity)
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Indexes ─────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_ppd_content_project
    ON platform_package_drafts(content_project_id);

CREATE INDEX IF NOT EXISTS idx_ppd_source_storyboard
    ON platform_package_drafts(source_storyboard_draft_id);

CREATE INDEX IF NOT EXISTS idx_ppd_package_type
    ON platform_package_drafts(package_type);

CREATE INDEX IF NOT EXISTS idx_ppd_status
    ON platform_package_drafts(status);

CREATE INDEX IF NOT EXISTS idx_ppd_project_type_status
    ON platform_package_drafts(content_project_id, package_type, status);

CREATE INDEX IF NOT EXISTS idx_ppsu_draft
    ON platform_package_scene_usages(platform_package_draft_id);
