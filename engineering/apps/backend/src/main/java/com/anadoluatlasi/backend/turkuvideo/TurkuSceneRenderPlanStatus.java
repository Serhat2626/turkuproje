package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-037: Scene render plan status.
 * No READY_FOR_PUBLISH. No COMPLETED.
 */
public enum TurkuSceneRenderPlanStatus {
    CREATED_INTERNAL_ONLY,
    VALIDATED_FOR_INTERNAL_RENDER_ONLY,
    READY_FOR_BUNDLE_ASSEMBLY,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
