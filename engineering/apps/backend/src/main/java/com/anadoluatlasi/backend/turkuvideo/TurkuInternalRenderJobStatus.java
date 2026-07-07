package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-025 Task 02: Internal render job lifecycle states.
 * Bounded — no publish/export/public delivery states.
 */
public enum TurkuInternalRenderJobStatus {
    DRAFT,
    READY_FOR_INTERNAL_RENDER,
    QUEUED_INTERNAL,
    RUNNING_INTERNAL,
    SUCCEEDED_INTERNAL,
    FAILED_INTERNAL,
    CANCELLED_INTERNAL,
    EXPIRED_INTERNAL;

    public boolean isTerminal() {
        return this == FAILED_INTERNAL
                || this == CANCELLED_INTERNAL
                || this == EXPIRED_INTERNAL;
    }
}
