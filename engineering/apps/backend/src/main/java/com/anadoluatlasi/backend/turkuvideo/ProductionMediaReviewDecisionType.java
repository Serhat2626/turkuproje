package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-005: Human review decision types. Internal-only.
 */
public enum ProductionMediaReviewDecisionType {
    APPROVE_FOR_INTERNAL_DELIVERY_PREP,
    REJECT_FOR_INTERNAL_REVISION,
    REQUEST_INTERNAL_FIX,
    MARK_UNSAFE_INTERNAL_ONLY
}
