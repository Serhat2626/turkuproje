-- ============================================================
-- Anadolu Atlası - V3: FP-001 Content Pipeline Tables
-- ============================================================
-- Referans: docs/05-engineering/21-content-pipeline-coding-tasks.md
-- Kapsam: FP-001 AI Assisted Content Creation entity alanları
-- ============================================================

-- ============================================================
-- 1. KNOWLEDGE DOMAIN - FP-001 Alanları
-- ============================================================

-- KnowledgeItem'a FP-001 için gerekli alanları ekle
ALTER TABLE knowledge_items ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE knowledge_items ADD COLUMN IF NOT EXISTS category VARCHAR(100);
ALTER TABLE knowledge_items ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE knowledge_items ADD COLUMN IF NOT EXISTS district VARCHAR(100);
ALTER TABLE knowledge_items ADD COLUMN IF NOT EXISTS location_text VARCHAR(500);
ALTER TABLE knowledge_items ADD COLUMN IF NOT EXISTS notes TEXT;

-- ============================================================
-- 2. CONTENT DOMAIN - FP-001 Alanları
-- ============================================================

-- ContentProject'e FP-001 için gerekli alanları ekle
ALTER TABLE content_projects ADD COLUMN IF NOT EXISTS content_purpose VARCHAR(50);
ALTER TABLE content_projects ADD COLUMN IF NOT EXISTS target_audience VARCHAR(50);
ALTER TABLE content_projects ADD COLUMN IF NOT EXISTS tone VARCHAR(50);
ALTER TABLE content_projects ADD COLUMN IF NOT EXISTS format_hint VARCHAR(255);
ALTER TABLE content_projects ADD COLUMN IF NOT EXISTS length_hint VARCHAR(255);
ALTER TABLE content_projects ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE content_projects ADD COLUMN IF NOT EXISTS last_error_type VARCHAR(50);
ALTER TABLE content_projects ADD COLUMN IF NOT EXISTS last_error_message TEXT;

-- content_type ve target_platform artık nullable (FP-001 projelerinde kullanılmayabilir)
ALTER TABLE content_projects ALTER COLUMN content_type DROP NOT NULL;
ALTER TABLE content_projects ALTER COLUMN target_platform DROP NOT NULL;
ALTER TABLE content_projects ALTER COLUMN title DROP NOT NULL;
ALTER TABLE content_projects ALTER COLUMN slug DROP NOT NULL;

-- ============================================================
-- 3. CONTENT DRAFT - Yeni Tablo
-- ============================================================

CREATE TABLE content_drafts (
    id UUID PRIMARY KEY,
    content_project_id UUID NOT NULL,
    title VARCHAR(500),
    body TEXT,
    summary TEXT,
    status VARCHAR(50) NOT NULL,
    generated_by VARCHAR(50) NOT NULL,
    warnings_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_content_drafts_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_content_drafts_project_id ON content_drafts(content_project_id);
CREATE INDEX idx_content_drafts_status ON content_drafts(status);

-- ============================================================
-- 4. AI GENERATION REQUEST - Yeni Tablo
-- ============================================================

CREATE TABLE ai_generation_requests (
    id UUID PRIMARY KEY,
    content_project_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    request_payload_json TEXT,
    response_payload_json TEXT,
    error_type VARCHAR(50),
    error_message TEXT,
    provider_name VARCHAR(100),
    model_name VARCHAR(100),
    duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_ai_gen_requests_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_ai_gen_requests_project_id ON ai_generation_requests(content_project_id);
CREATE INDEX idx_ai_gen_requests_status ON ai_generation_requests(status);
