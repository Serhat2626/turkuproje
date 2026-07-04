-- ============================================================================
-- FP-007 Task 10: Add review_status column to fp007_image_media_assets
-- Purpose: Track review lifecycle status for generated image media assets.
-- No public URL, signed URL, CDN URL, or publish/export status columns added.
-- ============================================================================

ALTER TABLE fp007_image_media_assets
    ADD COLUMN review_status VARCHAR(30);

-- Index for review status queries
CREATE INDEX IF NOT EXISTS idx_fp007ima_review_status ON fp007_image_media_assets(review_status);
