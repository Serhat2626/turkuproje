package com.anadoluatlasi.backend.turkuvideo;

public enum TurkuLiveRenderStatus {
    NOT_STARTED,
    FAKE_RENDER_AVAILABLE,
    REAL_RENDER_AVAILABLE_INTERNAL,
    BLOCKED_BY_KILL_SWITCH,
    BLOCKED_BY_OPERATOR_GATE,
    BLOCKED_BY_ARTIFACT_READINESS,
    FAILED_SAFE
}
