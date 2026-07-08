package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-046: Storyboard render plan adapter status.
 * No READY_FOR_PUBLISH. No COMPLETED. No AUTO_APPROVED.
 */
public enum TurkuStoryboardRenderAdapterStatus {
    CREATED_INTERNAL_ONLY,
    STORYBOARD_METADATA_ACCEPTED,
    SCENE_RENDER_PLANS_CREATED,
    READY_FOR_INTERNAL_RENDER_CHAIN,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
