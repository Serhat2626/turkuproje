package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-046: Storyboard render plan adapter issue codes.
 */
public enum TurkuStoryboardRenderAdapterIssueCode {
    NONE,
    STORYBOARD_REF_MISSING,
    STORY_REF_MISSING,
    NO_SCENES,
    TOO_MANY_SCENES,
    SCENE_ORDER_INVALID,
    SCENE_ORDER_DUPLICATE,
    SCENE_REF_MISSING,
    DURATION_EXCEEDS_LIMIT,
    FPS_EXCEEDS_LIMIT,
    WIDTH_EXCEEDS_LIMIT,
    HEIGHT_EXCEEDS_LIMIT,
    MISSING_PRIMARY_VIDEO_EXPECTATION,
    SCENE_PLAN_VALIDATION_FAILED
}
