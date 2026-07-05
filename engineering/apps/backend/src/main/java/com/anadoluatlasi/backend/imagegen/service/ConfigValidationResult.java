package com.anadoluatlasi.backend.imagegen.service;

import lombok.Builder;
import lombok.Data;

/**
 * FP-011 Task 02: Safe config validation result.
 *
 * <p>Internal-only model for runtime configuration validation.
 * Never exposed via DTO/API. Contains only safe categories — no secret values.</p>
 *
 * <p>Forbidden fields:</p>
 * <ul>
 *   <li>accessKey, secretKey, token — NEVER</li>
 *   <li>providerUrl — NEVER</li>
 *   <li>storageObjectKey — NEVER</li>
 *   <li>raw config value — NEVER</li>
 *   <li>raw exception — NEVER</li>
 *   <li>stack trace — NEVER</li>
 * </ul>
 */
@Data
@Builder
public class ConfigValidationResult {

    /** Which config component was validated. */
    private final ConfigComponent component;

    /** Whether the config is valid for the requested operational state. */
    private final boolean valid;

    /** Current enablement/readiness state. */
    private final ConfigReadiness readiness;

    /** Severity of validation finding. */
    private final ConfigSeverity severity;

    /** Safe human-readable message — NEVER contains secrets/config values. */
    private final String safeMessage;

    /** Safe error code category — NEVER contains secrets/config values. */
    private final String safeErrorCode;

    /**
     * Config component categories.
     */
    public enum ConfigComponent {
        PROVIDER_DOWNLOAD,
        PRIVATE_STORAGE,
        IMMEDIATE_HANDOFF
    }

    /**
     * Config readiness states.
     */
    public enum ConfigReadiness {
        READY,
        DISABLED,
        INVALID,
        DEPENDENCY_MISSING
    }

    /**
     * Validation severity.
     */
    public enum ConfigSeverity {
        INFO,
        WARNING,
        ERROR
    }

    /**
     * Safe toString — never includes secret/config values.
     */
    @Override
    public String toString() {
        return "ConfigValidationResult{" +
                "component=" + component +
                ", valid=" + valid +
                ", readiness=" + readiness +
                ", severity=" + severity +
                ", safeMessage='" + safeMessage + '\'' +
                ", safeErrorCode='" + safeErrorCode + '\'' +
                '}';
    }
}
