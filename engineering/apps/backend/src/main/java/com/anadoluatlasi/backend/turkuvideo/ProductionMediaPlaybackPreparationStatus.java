package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-003: Internal playback preparation statuses. No PUBLIC/CDN_READY/PUBLISHED.
 */
public enum ProductionMediaPlaybackPreparationStatus {
    CREATED_INTERNAL_ONLY,
    PRIVATE_STORAGE_REF_ACCEPTED,
    PLAYBACK_METADATA_PREPARED_INTERNAL_ONLY,
    READY_FOR_INTERNAL_PREVIEW_ONLY,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
