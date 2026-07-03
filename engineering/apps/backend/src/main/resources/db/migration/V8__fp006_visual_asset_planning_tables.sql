-- FP-006: Visual Asset Planning & Image Generation Handoff
-- Creates tables for visual asset plans, scene visual plans, and thumbnail plans.
-- Source: APPROVED StoryboardDraft. Prompt planning text only — no image generation, no media storage.
-- PostgreSQL 12 compatible: no gen_random_uuid() defaults. UUID generated application-side.

-- Parent table: visual asset plan per storyboard
CREATE TABLE IF NOT EXISTS visual_asset_plans (
    id                              UUID PRIMARY KEY,
    content_project_id              UUID NOT NULL REFERENCES content_projects(id),
    source_storyboard_draft_id      UUID NOT NULL REFERENCES storyboard_drafts(id),
    version                         INTEGER NOT NULL,
    status                          VARCHAR(20) NOT NULL,
    title                           VARCHAR(500),
    summary                         TEXT,
    overall_visual_style            TEXT,
    aspect_ratio_strategy           VARCHAR(100),
    cultural_authenticity_guidance   TEXT,
    character_continuity_guidance    TEXT,
    location_continuity_guidance    TEXT,
    color_palette_guidance          TEXT,
    review_notes                    TEXT,
    generated_by                    VARCHAR(50) NOT NULL,
    provider_type                   VARCHAR(50),
    model_name                      VARCHAR(100),
    approved_at                     TIMESTAMP,
    approved_by                     VARCHAR(100),
    rejected_at                     TIMESTAMP,
    rejected_by                     VARCHAR(100),
    rejection_reason                TEXT,
    edited_at                       TIMESTAMP,
    edited_by                       VARCHAR(100),
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vap_content_project ON visual_asset_plans(content_project_id);
CREATE INDEX IF NOT EXISTS idx_vap_source_storyboard ON visual_asset_plans(source_storyboard_draft_id);
CREATE INDEX IF NOT EXISTS idx_vap_status ON visual_asset_plans(status);
CREATE INDEX IF NOT EXISTS idx_vap_project_status ON visual_asset_plans(content_project_id, status);

-- Child table: scene visual plan per storyboard scene
CREATE TABLE IF NOT EXISTS scene_visual_plans (
    id                              UUID PRIMARY KEY,
    visual_asset_plan_id            UUID NOT NULL REFERENCES visual_asset_plans(id) ON DELETE CASCADE,
    source_storyboard_scene_id      UUID REFERENCES storyboard_scenes(id),
    scene_number                    INTEGER NOT NULL,
    source_beat_type                VARCHAR(50),
    scene_title                     VARCHAR(500),
    english_image_prompt            TEXT NOT NULL,
    turkish_visual_context          TEXT NOT NULL,
    negative_prompt                 TEXT,
    style_directive                 VARCHAR(200),
    aspect_ratio                    VARCHAR(20),
    composition_notes               TEXT,
    camera_framing                  VARCHAR(200),
    lighting_notes                  TEXT,
    color_palette                   TEXT,
    mood                            VARCHAR(200),
    location                        TEXT,
    characters                      TEXT,
    cultural_details                TEXT,
    text_overlay_guidance           TEXT,
    image_generation_tool_notes     TEXT,
    safety_notes                    TEXT,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_svp_plan ON scene_visual_plans(visual_asset_plan_id);
CREATE INDEX IF NOT EXISTS idx_svp_scene ON scene_visual_plans(source_storyboard_scene_id);

-- Child table: thumbnail plan (one-to-one with visual asset plan)
CREATE TABLE IF NOT EXISTS thumbnail_plans (
    id                              UUID PRIMARY KEY,
    visual_asset_plan_id            UUID NOT NULL UNIQUE REFERENCES visual_asset_plans(id) ON DELETE CASCADE,
    source_storyboard_scene_id      UUID REFERENCES storyboard_scenes(id),
    source_scene_number             INTEGER,
    thumbnail_prompt                TEXT NOT NULL,
    negative_prompt                 TEXT,
    text_overlay_guidance           TEXT,
    composition_notes               TEXT,
    emotional_hook                  VARCHAR(500),
    aspect_ratio                    VARCHAR(20),
    style_directive                 VARCHAR(200),
    color_palette                   TEXT,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tp_plan ON thumbnail_plans(visual_asset_plan_id);
