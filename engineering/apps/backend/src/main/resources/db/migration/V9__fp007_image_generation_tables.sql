-- FP-007: Image Generation & Media Asset Pipeline
-- Creates tables for image generation requests, results, generated assets,
-- image media assets, and generated asset reviews.
-- Source: APPROVED VisualAssetPlan. Controlled image generation with mandatory human review.
-- PostgreSQL 12 compatible: no gen_random_uuid() defaults. UUID generated application-side.
-- No raw provider payload/response columns. No API key/secret columns.
-- No internal full prompt columns. No public/signed/CDN URL columns.
-- No READY_FOR_PUBLISH status. No COMPLETED status. GENERATED_PENDING_REVIEW is the safe intermediate state.

-- ============================================================================
-- Table: image_generation_requests
-- Purpose: Tracks image generation requests tied to APPROVED VisualAssetPlan items.
-- ============================================================================
CREATE TABLE IF NOT EXISTS image_generation_requests (
    id                      UUID PRIMARY KEY,
    content_project_id      UUID NOT NULL,
    visual_asset_plan_id    UUID NOT NULL,
    visual_asset_item_id    UUID NOT NULL,
    item_type               VARCHAR(30)  NOT NULL,
    status                  VARCHAR(40)  NOT NULL,
    idempotency_key         VARCHAR(255) NOT NULL,
    provider_type           VARCHAR(50),
    model_name              VARCHAR(100),
    safe_prompt_summary     TEXT,
    error_code              VARCHAR(50),
    error_message           TEXT,
    correlation_id          UUID,
    requested_by            VARCHAR(100),
    requested_at            TIMESTAMP,
    completed_at            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint for idempotency
ALTER TABLE image_generation_requests
    ADD CONSTRAINT uq_igr_idempotency_key UNIQUE (idempotency_key);

-- FK to visual_asset_plans (APPROVED plan source)
ALTER TABLE image_generation_requests
    ADD CONSTRAINT fk_igr_visual_asset_plan
    FOREIGN KEY (visual_asset_plan_id) REFERENCES visual_asset_plans(id);

-- FK to content_projects
ALTER TABLE image_generation_requests
    ADD CONSTRAINT fk_igr_content_project
    FOREIGN KEY (content_project_id) REFERENCES content_projects(id);

-- Indexes for common lookups
CREATE INDEX IF NOT EXISTS idx_igr_visual_asset_plan    ON image_generation_requests(visual_asset_plan_id);
CREATE INDEX IF NOT EXISTS idx_igr_visual_asset_item    ON image_generation_requests(visual_asset_item_id);
CREATE INDEX IF NOT EXISTS idx_igr_content_project      ON image_generation_requests(content_project_id);
CREATE INDEX IF NOT EXISTS idx_igr_status               ON image_generation_requests(status);
CREATE INDEX IF NOT EXISTS idx_igr_correlation           ON image_generation_requests(correlation_id);
CREATE INDEX IF NOT EXISTS idx_igr_plan_status           ON image_generation_requests(visual_asset_plan_id, status);

-- ============================================================================
-- Table: image_generation_results
-- Purpose: Normalized provider generation result — no raw provider payload/response.
-- ============================================================================
CREATE TABLE IF NOT EXISTS image_generation_results (
    id                              UUID PRIMARY KEY,
    image_generation_request_id     UUID NOT NULL,
    visual_asset_plan_id            UUID NOT NULL,
    visual_asset_item_id            UUID NOT NULL,
    provider_type                   VARCHAR(50),
    model_name                      VARCHAR(100),
    provider_request_id             VARCHAR(255),
    generation_duration_ms          BIGINT,
    output_count                    INTEGER,
    mime_type                       VARCHAR(50),
    width                           INTEGER,
    height                          INTEGER,
    file_size_bytes                 BIGINT,
    checksum_sha256                 VARCHAR(64),
    error_code                      VARCHAR(50),
    error_message                   TEXT,
    correlation_id                  UUID,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FK to image_generation_requests
ALTER TABLE image_generation_results
    ADD CONSTRAINT fk_igresult_request
    FOREIGN KEY (image_generation_request_id) REFERENCES image_generation_requests(id);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_igresult_request          ON image_generation_results(image_generation_request_id);
CREATE INDEX IF NOT EXISTS idx_igresult_plan             ON image_generation_results(visual_asset_plan_id);
CREATE INDEX IF NOT EXISTS idx_igresult_item             ON image_generation_results(visual_asset_item_id);
CREATE INDEX IF NOT EXISTS idx_igresult_correlation      ON image_generation_results(correlation_id);
CREATE INDEX IF NOT EXISTS idx_igresult_provider_req     ON image_generation_results(provider_request_id);

-- ============================================================================
-- Table: fp007_image_media_assets
-- Purpose: Storage reference and safe metadata for generated image files.
-- No public URL, signed URL, CDN URL, or publish/export status columns.
-- ============================================================================
CREATE TABLE IF NOT EXISTS fp007_image_media_assets (
    id                      UUID PRIMARY KEY,
    storage_provider        VARCHAR(50),
    storage_bucket          VARCHAR(255),
    storage_object_key      TEXT,
    original_file_name      VARCHAR(500),
    mime_type               VARCHAR(50),
    width                   INTEGER,
    height                  INTEGER,
    file_size_bytes         BIGINT,
    checksum_sha256         VARCHAR(64),
    asset_kind              VARCHAR(30) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_fp007ima_kind             ON fp007_image_media_assets(asset_kind);
CREATE INDEX IF NOT EXISTS idx_fp007ima_checksum         ON fp007_image_media_assets(checksum_sha256);

-- ============================================================================
-- Table: fp007_generated_assets
-- Purpose: AI-generated image asset with mandatory human review lifecycle.
-- Initial status must be GENERATED_PENDING_REVIEW. Never automatically APPROVED.
-- No READY_FOR_PUBLISH. No publish/export fields.
-- ============================================================================
CREATE TABLE IF NOT EXISTS fp007_generated_assets (
    id                              UUID PRIMARY KEY,
    image_generation_request_id     UUID NOT NULL,
    image_generation_result_id      UUID,
    image_media_asset_id            UUID,
    content_project_id              UUID NOT NULL,
    visual_asset_plan_id            UUID NOT NULL,
    visual_asset_item_id            UUID NOT NULL,
    status                          VARCHAR(40) NOT NULL,
    safe_prompt_summary             TEXT,
    quality_signals_summary         TEXT,
    safety_flags_summary            TEXT,
    reviewed_by                     VARCHAR(100),
    reviewed_at                     TIMESTAMP,
    rejection_reason                TEXT,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FKs
ALTER TABLE fp007_generated_assets
    ADD CONSTRAINT fk_fp007ga_request
    FOREIGN KEY (image_generation_request_id) REFERENCES image_generation_requests(id);

ALTER TABLE fp007_generated_assets
    ADD CONSTRAINT fk_fp007ga_result
    FOREIGN KEY (image_generation_result_id) REFERENCES image_generation_results(id);

ALTER TABLE fp007_generated_assets
    ADD CONSTRAINT fk_fp007ga_media_asset
    FOREIGN KEY (image_media_asset_id) REFERENCES fp007_image_media_assets(id);

ALTER TABLE fp007_generated_assets
    ADD CONSTRAINT fk_fp007ga_content_project
    FOREIGN KEY (content_project_id) REFERENCES content_projects(id);

ALTER TABLE fp007_generated_assets
    ADD CONSTRAINT fk_fp007ga_visual_asset_plan
    FOREIGN KEY (visual_asset_plan_id) REFERENCES visual_asset_plans(id);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_fp007ga_request           ON fp007_generated_assets(image_generation_request_id);
CREATE INDEX IF NOT EXISTS idx_fp007ga_result            ON fp007_generated_assets(image_generation_result_id);
CREATE INDEX IF NOT EXISTS idx_fp007ga_media_asset       ON fp007_generated_assets(image_media_asset_id);
CREATE INDEX IF NOT EXISTS idx_fp007ga_project           ON fp007_generated_assets(content_project_id);
CREATE INDEX IF NOT EXISTS idx_fp007ga_plan              ON fp007_generated_assets(visual_asset_plan_id);
CREATE INDEX IF NOT EXISTS idx_fp007ga_item              ON fp007_generated_assets(visual_asset_item_id);
CREATE INDEX IF NOT EXISTS idx_fp007ga_status            ON fp007_generated_assets(status);
CREATE INDEX IF NOT EXISTS idx_fp007ga_plan_status       ON fp007_generated_assets(visual_asset_plan_id, status);

-- ============================================================================
-- Table: fp007_generated_asset_reviews
-- Purpose: Audit record for each human review decision on a generated asset.
-- Decision is APPROVED or REJECTED. No automatic decisions.
-- ============================================================================
CREATE TABLE IF NOT EXISTS fp007_generated_asset_reviews (
    id                      UUID PRIMARY KEY,
    generated_asset_id      UUID NOT NULL,
    decision                VARCHAR(20) NOT NULL,
    rejection_reason        TEXT,
    reviewed_by             VARCHAR(100) NOT NULL,
    reviewed_at             TIMESTAMP NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- FK to fp007_generated_assets
ALTER TABLE fp007_generated_asset_reviews
    ADD CONSTRAINT fk_fp007gar_generated_asset
    FOREIGN KEY (generated_asset_id) REFERENCES fp007_generated_assets(id);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_fp007gar_generated_asset  ON fp007_generated_asset_reviews(generated_asset_id);
CREATE INDEX IF NOT EXISTS idx_fp007gar_decision         ON fp007_generated_asset_reviews(decision);
CREATE INDEX IF NOT EXISTS idx_fp007gar_reviewed_by      ON fp007_generated_asset_reviews(reviewed_by);
CREATE INDEX IF NOT EXISTS idx_fp007gar_reviewed_at      ON fp007_generated_asset_reviews(reviewed_at);
