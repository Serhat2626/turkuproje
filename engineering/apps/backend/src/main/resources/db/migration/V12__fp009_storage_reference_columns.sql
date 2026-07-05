-- ============================================================================
-- FP-009 Task 08: Add private storage reference columns to fp007_image_media_assets
-- Purpose: Storage provider/bucket/key columns required for real private storage.
--
-- These columns are defined on the ImageMediaAsset entity but were missing
-- from V11 migration. They are internal-only — NEVER admin/user-facing.
--
-- No provider URL, signed URL, public URL, CDN URL, base64, binary,
-- raw provider/storage payload, API key/secret, internal prompt,
-- READY_FOR_PUBLISH, COMPLETED, or publish/export status columns added.
--
-- storageObjectKey is NOT a user/admin-facing field.
-- storageProvider is NOT a user/admin-facing field.
-- storageBucket is NOT a user/admin-facing field.
-- ============================================================================

-- Private storage provider alias (e.g. "s3", "minio", "mock")
ALTER TABLE fp007_image_media_assets
    ADD COLUMN storage_provider VARCHAR(50);

-- Private storage bucket/container name (internal-only)
ALTER TABLE fp007_image_media_assets
    ADD COLUMN storage_bucket VARCHAR(255);

-- Private storage object key/path (internal-only — NEVER admin/user DTO)
ALTER TABLE fp007_image_media_assets
    ADD COLUMN storage_object_key TEXT;
