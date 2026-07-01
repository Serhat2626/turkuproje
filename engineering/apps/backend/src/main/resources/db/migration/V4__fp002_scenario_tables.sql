-- ============================================================
-- Anadolu Atlası - V4: FP-002 Scenario Generation Tables
-- ============================================================
-- Referans: docs/05-engineering/37-fp002-engineering-design-and-task-breakdown.md
-- Kapsam: FP-002 Scenario Generation Workflow entity alanları
-- ============================================================

-- ============================================================
-- 1. SCENARIO DRAFT - Ana Tablo
-- ============================================================

CREATE TABLE scenario_drafts (
    id                          UUID PRIMARY KEY,
    content_project_id          UUID NOT NULL,
    source_draft_id             UUID NOT NULL,
    version                     INTEGER NOT NULL,
    status                      VARCHAR(50) NOT NULL,
    title                       VARCHAR(500),
    summary                     TEXT,
    target_audience             VARCHAR(100),
    cultural_objective          TEXT,
    tone                        VARCHAR(100),
    estimated_duration_seconds  INTEGER,
    setting                     VARCHAR(500),
    characters                  TEXT,
    review_notes                TEXT,
    generated_by                VARCHAR(50) NOT NULL,
    provider_type               VARCHAR(50),
    model_name                  VARCHAR(100),
    warnings_json               TEXT,
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP,
    approved_at                 TIMESTAMP,
    rejected_at                 TIMESTAMP,
    edited_at                   TIMESTAMP,
    created_by                  VARCHAR(100),
    approved_by                 VARCHAR(100),
    rejected_by                 VARCHAR(100),
    edited_by                   VARCHAR(100),
    CONSTRAINT fk_scenario_drafts_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_scenario_drafts_source_draft
        FOREIGN KEY (source_draft_id)
        REFERENCES content_drafts(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_scenario_drafts_project_id ON scenario_drafts(content_project_id);
CREATE INDEX idx_scenario_drafts_source_draft_id ON scenario_drafts(source_draft_id);
CREATE INDEX idx_scenario_drafts_status ON scenario_drafts(status);
CREATE INDEX idx_scenario_drafts_project_version ON scenario_drafts(content_project_id, version);

-- ============================================================
-- 2. SCENARIO BEAT - Alt Tablo
-- ============================================================

CREATE TABLE scenario_beats (
    id                          UUID PRIMARY KEY,
    scenario_draft_id           UUID NOT NULL,
    type                        VARCHAR(50) NOT NULL,
    label                       VARCHAR(100) NOT NULL,
    body                        TEXT NOT NULL,
    order_index                 INTEGER NOT NULL,
    estimated_duration_seconds  INTEGER,
    CONSTRAINT fk_scenario_beats_scenario_draft
        FOREIGN KEY (scenario_draft_id)
        REFERENCES scenario_drafts(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_scenario_beats_draft_id ON scenario_beats(scenario_draft_id);
