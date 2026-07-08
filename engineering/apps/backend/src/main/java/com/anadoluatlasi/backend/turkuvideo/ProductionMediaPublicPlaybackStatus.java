package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-007: Controlled public playback statuses.
 * No COMPLETED/PUBLISHED/PUBLICLY_AVAILABLE/AUTO_APPROVED.
 */
public enum ProductionMediaPublicPlaybackStatus {
    CREATED_INTERNAL_ONLY,
    DELIVERY_GATE_ACCEPTED,
    CONTROLLED_PLAYBACK_HANDLE_CREATED,
    READY_FOR_CONTROLLED_PUBLIC_PLAYBACK,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
