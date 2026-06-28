-- ============================================================
-- Anadolu Atlası - V2: İlk Domain Tabloları
-- ============================================================
-- Referans: docs/05-engineering/04-database-design.md
-- Kapsam: Knowledge, Content, AI, Media domain tabloları
-- ============================================================

-- ============================================================
-- 1. KNOWLEDGE DOMAIN
-- ============================================================

-- Kültürel bilgi varlığı
CREATE TABLE knowledge_items (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    summary TEXT,
    status VARCHAR(50) NOT NULL,
    language VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Bilgi kaynağı
CREATE TABLE source_references (
    id UUID PRIMARY KEY,
    knowledge_item_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    url TEXT,
    author VARCHAR(255),
    source_type VARCHAR(50) NOT NULL,
    reliability_level VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_source_references_knowledge_item
        FOREIGN KEY (knowledge_item_id)
        REFERENCES knowledge_items(id)
        ON DELETE CASCADE
);

-- Doğrulanmış veya doğrulanması gereken bilgi parçası
CREATE TABLE facts (
    id UUID PRIMARY KEY,
    knowledge_item_id UUID NOT NULL,
    source_reference_id UUID,
    statement TEXT NOT NULL,
    verification_status VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_facts_knowledge_item
        FOREIGN KEY (knowledge_item_id)
        REFERENCES knowledge_items(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_facts_source_reference
        FOREIGN KEY (source_reference_id)
        REFERENCES source_references(id)
        ON DELETE SET NULL
);

-- ============================================================
-- 2. CONTENT DOMAIN
-- ============================================================

-- İçerik projesi
CREATE TABLE content_projects (
    id UUID PRIMARY KEY,
    knowledge_item_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(50) NOT NULL,
    target_platform VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    language VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_content_projects_knowledge_item
        FOREIGN KEY (knowledge_item_id)
        REFERENCES knowledge_items(id)
        ON DELETE RESTRICT
);

-- Senaryo taslağı
CREATE TABLE script_drafts (
    id UUID PRIMARY KEY,
    content_project_id UUID NOT NULL,
    version INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_script_drafts_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_script_drafts_project_version
        UNIQUE (content_project_id, version)
);

-- Storyboard (sahne planı)
CREATE TABLE storyboards (
    id UUID PRIMARY KEY,
    content_project_id UUID NOT NULL,
    version INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_storyboards_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_storyboards_project_version
        UNIQUE (content_project_id, version)
);

-- Storyboard sahnesi
CREATE TABLE storyboard_scenes (
    id UUID PRIMARY KEY,
    storyboard_id UUID NOT NULL,
    scene_order INTEGER NOT NULL,
    duration_seconds INTEGER,
    narration_text TEXT,
    visual_description TEXT,
    mood VARCHAR(100),
    location VARCHAR(255),
    characters TEXT,
    camera_style VARCHAR(255),
    transition_note TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_storyboard_scenes_storyboard
        FOREIGN KEY (storyboard_id)
        REFERENCES storyboards(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_storyboard_scene_order
        UNIQUE (storyboard_id, scene_order)
);

-- ============================================================
-- 3. AI PLATFORM
-- ============================================================

-- Prompt şablonu
CREATE TABLE prompt_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uq_prompt_template_name_version
        UNIQUE (name, version)
);

-- AI üretim kayıtları
CREATE TABLE ai_generation_logs (
    id UUID PRIMARY KEY,
    content_project_id UUID,
    prompt_template_id UUID,
    provider VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    input_summary TEXT,
    output_summary TEXT,
    cost NUMERIC(12, 6),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_ai_generation_logs_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_ai_generation_logs_prompt_template
        FOREIGN KEY (prompt_template_id)
        REFERENCES prompt_templates(id)
        ON DELETE SET NULL
);

-- ============================================================
-- 4. MEDIA DOMAIN
-- ============================================================

-- Medya dosyası metadata
CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    asset_type VARCHAR(50) NOT NULL,
    storage_provider VARCHAR(50) NOT NULL,
    bucket VARCHAR(255) NOT NULL,
    object_key TEXT NOT NULL,
    file_name VARCHAR(500),
    mime_type VARCHAR(255),
    size_bytes BIGINT,
    duration_seconds INTEGER,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- AI veya manuel üretilmiş asset
CREATE TABLE generated_assets (
    id UUID PRIMARY KEY,
    media_asset_id UUID NOT NULL,
    content_project_id UUID,
    generator_type VARCHAR(50) NOT NULL,
    prompt TEXT,
    model VARCHAR(100),
    generation_status VARCHAR(50) NOT NULL,
    cost NUMERIC(12, 6),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_generated_assets_media_asset
        FOREIGN KEY (media_asset_id)
        REFERENCES media_assets(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_generated_assets_content_project
        FOREIGN KEY (content_project_id)
        REFERENCES content_projects(id)
        ON DELETE SET NULL
);

-- ============================================================
-- 5. INDEXES
-- ============================================================

CREATE INDEX idx_knowledge_items_type ON knowledge_items(type);
CREATE INDEX idx_knowledge_items_status ON knowledge_items(status);
CREATE INDEX idx_content_projects_status ON content_projects(status);
CREATE INDEX idx_content_projects_platform ON content_projects(target_platform);
CREATE INDEX idx_script_drafts_content_project ON script_drafts(content_project_id);
CREATE INDEX idx_storyboards_content_project ON storyboards(content_project_id);
CREATE INDEX idx_storyboard_scenes_storyboard ON storyboard_scenes(storyboard_id);
CREATE INDEX idx_ai_generation_logs_content_project ON ai_generation_logs(content_project_id);
CREATE INDEX idx_media_assets_asset_type ON media_assets(asset_type);
