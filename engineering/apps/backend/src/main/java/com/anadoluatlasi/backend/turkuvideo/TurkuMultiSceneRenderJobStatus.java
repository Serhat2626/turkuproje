package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-038: Multi-scene render job status.
 * No READY_FOR_PUBLISH. No COMPLETED.
 */
public enum TurkuMultiSceneRenderJobStatus {
    CREATED_INTERNAL_ONLY,
    SCENES_ATTACHED,
    VALIDATED_FOR_INTERNAL_RENDER_ONLY,
    READY_FOR_LOCAL_MANUAL_RENDER,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
