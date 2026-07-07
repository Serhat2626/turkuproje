package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-029 Task 02: Internal video generation trial status.
 */
public enum TurkuInternalTrialStatus {
    NOT_ATTEMPTED,
    GATE_DENIED,
    PLAN_INVALID,
    TRIAL_READY,
    EXECUTION_SKIPPED,
    EXECUTION_SIMULATED,
    EXECUTION_ATTEMPTED_DEV_TEST,
    VERIFICATION_CANDIDATE_CREATED,
    TRIAL_FAILED_SAFE
}
