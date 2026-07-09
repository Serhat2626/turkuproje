package com.anadoluatlasi.backend.turkuvideo;

public enum TurkuLiveStorageStatus {
    NOT_STORED,
    FAKE_STORED,
    REAL_STORED_INTERNAL,
    BLOCKED_BY_SAFETY,
    WAITING_FOR_MANUAL_GATE,
    FAILED_SAFE
}
