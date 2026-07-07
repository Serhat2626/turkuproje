package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-020 Task 02: Safe provider failure codes.
 *
 * <p>No credential/auth failure codes exposed to frontend.
 * All codes are safe for admin display.</p>
 */
public enum TurkuProviderFailureCode {

    PROVIDER_TIMEOUT,
    PROVIDER_RATE_LIMIT,
    PROVIDER_CONTENT_POLICY_VIOLATION,
    PROVIDER_INTERNAL_ERROR,
    PROMPT_INJECTION_DETECTED,
    OUTPUT_SANITIZATION_FAILED,
    UNSAFE_CONTENT_DETECTED,
    UNKNOWN_PROVIDER_ERROR
}
