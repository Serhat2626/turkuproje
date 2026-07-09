package com.anadoluatlasi.backend.turkuvideo;

public enum TurkuLiveGenerationStatus {
    NOT_STARTED,
    FAKE_RESULT_AVAILABLE,
    REAL_PROVIDER_RESULT_AVAILABLE,
    BLOCKED_BY_KILL_SWITCH,
    BLOCKED_BY_OPERATOR_GATE,
    BLOCKED_BY_PROVIDER_READINESS,
    FAILED_SAFE
}
