package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-025 Task 02: Explicit lifecycle actions.
 * No generic UPDATE_STATUS / SET_STATUS / PATCH_STATUS.
 */
public enum TurkuInternalRenderJobAction {
    VALIDATE_FOR_INTERNAL_RENDER,
    ENQUEUE_INTERNAL_RENDER,
    START_INTERNAL_RENDER,
    COMPLETE_INTERNAL_RENDER,
    FAIL_INTERNAL_RENDER,
    CANCEL_INTERNAL_RENDER,
    EXPIRE_INTERNAL_RENDER
}
