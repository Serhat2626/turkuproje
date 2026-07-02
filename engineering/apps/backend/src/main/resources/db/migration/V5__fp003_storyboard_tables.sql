-- ============================================================
-- Anadolu Atlası - V5: FP-003 Storyboard Workflow Tables
-- ============================================================
-- Referans: docs/05-engineering/43-fp003-engineering-design-and-task-breakdown.md
-- Kapsam: FP-003 Storyboard Workflow entity alanları
-- ============================================================

-- ============================================================
-- 1. STORYBOARD DRAFT - Ana Tablo
-- ============================================================

CREATE TABLE storyboard_drafts (
    id                          UUID PRIMARY KEY,
    content_project_id          UUID NOT NULL,
    source_draft_id             UUID NOT NULL,
    source_scenario_draft_id    UUID NOT NULL,
    source_scenario_version     INTEGER NOT NULL,
    version                     INTEGER NOT NULL,
    status                      VARCHAR(50) NOT NULL,
    title                       VARCHAR(500),
    summary                     TEXT,
    target_platform             VARCHAR(100),
    aspect_ratio                VARCHAR(20),
    estimated_duration_seconds  INTEGER,
    visual_style_guide          TEXT,
    narration_style             VARCHAR(500),
    review_notes                TEXT,
    generated_by                VARCHAR(50) NOT NULL,
    provider_type               VARCHAR(50),
    model_name                  VARCHAR(100),
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP,
    approved_at                 TIMESTAMP,
    rejected_at                 TIMESTAMP,
    edited_at                   TIMESTAMP,
    created_by                  VARCHAR(100),
    approved_by                 VARCHAR(100),
    rejected_by                 VARCHAR(100),
    edited_by                   VARCHAR(100),
    CONSTRAINT fk_storyboard_drafts_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_storyboard_drafts_source_draft
        FOREIGN KEY (source_draft_id)
        REFERENCES content_drafts(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_storyboard_drafts_source_scenario
        FOREIGN KEY (source_scenario_draft_id)
        REFERENCES scenario_drafts(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_storyboard_drafts_project_id ON storyboard_drafts(content_project_id);
CREATE INDEX idx_storyboard_drafts_source_draft_id ON storyboard_drafts(source_draft_id);
CREATE INDEX idx_storyboard_drafts_source_scenario_id ON storyboard_drafts(source_scenario_draft_id);
CREATE INDEX idx_storyboard_drafts_status ON storyboard_drafts(status);
CREATE INDEX idx_storyboard_drafts_project_version ON storyboard_drafts(content_project_id, version);

-- ============================================================
-- 2. STORYBOARD SCENE - Alt Tablo
-- ============================================================

CREATE TABLE storyboard_scenes (
    id                          UUID PRIMARY KEY,
    storyboard_draft_id         UUID NOT NULL,
    scene_number                INTEGER NOT NULL,
    source_beat_type            VARCHAR(50) NOT NULL,
    source_beat_order           INTEGER NOT NULL,
    scene_title                 VARCHAR(500) NOT NULL,
    visual_description          TEXT NOT NULL,
    narration_text              TEXT,
    on_screen_text              TEXT,
    camera_direction            VARCHAR(500),
    transition_note             VARCHAR(200),
    mood                        VARCHAR(200),
    location                    VARCHAR(500),
    characters                  TEXT,
    estimated_duration_seconds  INTEGER NOT NULL,
    media_generation_notes      TEXT,
    CONSTRAINT fk_storyboard_scenes_storyboard_draft
        FOREIGN KEY (storyboard_draft_id)
        REFERENCES storyboard_drafts(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_storyboard_scenes_draft_id ON storyboard_scenes(storyboard_draft_id);
