package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-021 Task 03: Render readiness status.
 */
public enum TurkuRenderReadinessStatus {
    /** All prerequisites met, internal render possible */
    READY,
    /** Some prerequisites missing or need human review */
    NEEDS_REVIEW,
    /** Critical prerequisites missing, render blocked */
    BLOCKED
}
