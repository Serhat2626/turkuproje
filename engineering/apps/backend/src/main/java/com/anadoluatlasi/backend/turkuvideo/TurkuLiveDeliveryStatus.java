package com.anadoluatlasi.backend.turkuvideo;

public enum TurkuLiveDeliveryStatus {
    NOT_ELIGIBLE,
    PENDING_HUMAN_REVIEW,
    ELIGIBLE_FOR_CONTROLLED_DELIVERY,
    BLOCKED_BY_REVIEW,
    BLOCKED_BY_SAFETY,
    FAILED_SAFE
}
