package com.anadoluatlasi.backend.turkuvideo;

public enum TurkuLiveProviderStatus {
    DISABLED_BY_DEFAULT,
    CREDENTIAL_PRESENT,
    BLOCKED_BY_KILL_SWITCH,
    WAITING_FOR_OPERATOR_ENABLEMENT,
    FAILED_SAFE
}
