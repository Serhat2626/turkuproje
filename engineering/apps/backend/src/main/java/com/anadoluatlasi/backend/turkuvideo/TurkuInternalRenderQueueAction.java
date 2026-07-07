package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-027 Task 02: Explicit queue actions.
 * No UPDATE_STATUS/SET_STATUS/PATCH_STATUS/PUBLISH/EXPORT/DOWNLOAD/AUTO_APPROVE.
 */
public enum TurkuInternalRenderQueueAction {
    MARK_QUEUE_READY,
    ENQUEUE_INTERNAL,
    DISPATCH_INTERNAL,
    MARK_IN_PROGRESS,
    MARK_RETRY_WAIT,
    MARK_SUCCEEDED,
    MARK_FAILED,
    CANCEL_INTERNAL,
    EXPIRE_INTERNAL
}
