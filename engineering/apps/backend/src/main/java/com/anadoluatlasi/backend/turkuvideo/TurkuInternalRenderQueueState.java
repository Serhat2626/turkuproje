package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-027 Task 02: Internal render queue states.
 */
public enum TurkuInternalRenderQueueState {
    NOT_QUEUED,
    QUEUE_READY,
    QUEUED_INTERNAL,
    DISPATCHING_INTERNAL,
    IN_PROGRESS_INTERNAL,
    RETRY_WAIT_INTERNAL,
    SUCCEEDED_INTERNAL,
    FAILED_INTERNAL,
    CANCELLED_INTERNAL,
    EXPIRED_INTERNAL;

    public boolean isTerminal() {
        return this == SUCCEEDED_INTERNAL
                || this == FAILED_INTERNAL
                || this == CANCELLED_INTERNAL
                || this == EXPIRED_INTERNAL;
    }
}
