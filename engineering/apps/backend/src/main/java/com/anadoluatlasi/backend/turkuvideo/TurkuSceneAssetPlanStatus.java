package com.anadoluatlasi.backend.turkuvideo;

/**
 * RP-003: Scene asset plan statuses.
 */
public enum TurkuSceneAssetPlanStatus {
    CREATED_INTERNAL_ONLY,
    DRAFT_ACCEPTED,
    VISUAL_BRIEF_GENERATED,
    READY_FOR_ASSET_GENERATION_BOUNDARY,
    BLOCKED_CONTENT_UNSAFE,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
