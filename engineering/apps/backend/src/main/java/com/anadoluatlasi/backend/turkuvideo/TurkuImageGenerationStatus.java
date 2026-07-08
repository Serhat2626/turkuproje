package com.anadoluatlasi.backend.turkuvideo;

public enum TurkuImageGenerationStatus {
    NOT_STARTED,
    FAKE_RESULT_AVAILABLE,
    BLOCKED_BY_SAFETY,
    WAITING_FOR_MANUAL_GATE,
    FAILED_SAFE
}
