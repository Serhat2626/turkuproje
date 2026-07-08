package com.anadoluatlasi.backend.turkuvideo;

public enum TurkuMvpReadinessStatus {
    NOT_READY,
    READY_FOR_MANUAL_REVIEW,
    BLOCKED_BY_SAFETY,
    PARTIALLY_READY,
    FAILED_SAFE
}
