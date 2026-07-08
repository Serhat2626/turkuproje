package com.anadoluatlasi.backend.turkuvideo;

public enum TurkuExecutionGateStatus {
    EXECUTION_NOT_ENABLED,
    BLOCKED_BY_SAFETY,
    WAITING_FOR_OPERATOR_GATE,
    ELIGIBLE_FOR_FUTURE_EXECUTION_REVIEW,
    FAILED_SAFE
}
