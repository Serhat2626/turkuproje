-- ============================================================================
-- FP-008 Task 05: Add storage handoff fields to fp007_image_media_assets
-- Purpose: Storage handoff lifecycle status, preview status, error tracking,
--          and storage completion timestamp for safe storage handoff pipeline.
--
-- No provider URL, signed URL, public URL, CDN URL, base64, binary,
-- raw provider/storage payload, API key/secret, internal prompt,
-- READY_FOR_PUBLISH, or publish/export status columns added.
--
-- Storage status is NOT review/approval status.
-- Preview status is NOT approval status.
-- ============================================================================

-- Storage handoff lifecycle status
ALTER TABLE fp007_image_media_assets
    ADD COLUMN storage_handoff_status VARCHAR(30);

-- Preview availability status
ALTER TABLE fp007_image_media_assets
    ADD COLUMN preview_status VARCHAR(30);

-- Normalized storage error code (null on success)
ALTER TABLE fp007_image_media_assets
    ADD COLUMN storage_error_code VARCHAR(60);

-- Safe normalized error message (null on success)
ALTER TABLE fp007_image_media_assets
    ADD COLUMN storage_error_message TEXT;

-- Storage completion timestamp (null until stored)
ALTER TABLE fp007_image_media_assets
    ADD COLUMN stored_at TIMESTAMP;

-- ── Indexes ─────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_fp007ima_storage_handoff_status
    ON fp007_image_media_assets(storage_handoff_status);

CREATE INDEX IF NOT EXISTS idx_fp007ima_preview_status
    ON fp007_image_media_assets(preview_status);
