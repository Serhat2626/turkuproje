package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-027 Task 02: Retry reason codes.
 */
public enum TurkuInternalRenderRetryReasonCode {
    EXECUTION_FAILED_RETRIABLE,
    TIMEOUT_RETRIABLE,
    TRANSIENT_ERROR_RETRIABLE,
    MAX_RETRIES_EXHAUSTED,
    NOT_RETRIABLE_PERMANENT,
    CANCELLED_NO_RETRY,
    EXPIRED_NO_RETRY
}
