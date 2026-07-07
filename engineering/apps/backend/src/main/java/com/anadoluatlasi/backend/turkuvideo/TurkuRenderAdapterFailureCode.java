package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-023 Task 02: Render adapter failure codes.
 * Covers config, input, process, output, and security failures.
 */
public enum TurkuRenderAdapterFailureCode {
    RENDER_PROVIDER_DISABLED,
    RENDER_CONFIG_MISSING,
    RENDER_INPUT_MISSING,
    RENDER_TIMEOUT,
    RENDER_PROCESS_FAILED,
    RENDER_OUTPUT_REJECTED,
    RENDER_SECURITY_VIOLATION,
    RENDER_FAILED_SAFE
}
