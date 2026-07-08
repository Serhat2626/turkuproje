package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-002: Upload pipeline statuses. Private-only. No PUBLIC/CDN_READY/PUBLISHED.
 */
public enum ProductionMediaStorageUploadStatus {
    CREATED_INTERNAL_ONLY,
    VERIFIED_ARTIFACT_ACCEPTED,
    PRIVATE_STORAGE_HANDOFF_STARTED,
    PRIVATE_STORAGE_REF_CREATED,
    UPLOAD_METADATA_VERIFIED,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
