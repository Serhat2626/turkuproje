package com.anadoluatlasi.backend.imagegen.service;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * FP-011 Task 03: Internal storage readiness result.
 *
 * <p>Safe internal-only model — never exposed via DTO/API/endpoint.
 * Contains only safe status categories and messages.</p>
 *
 * <p>Forbidden fields:</p>
 * <ul>
 *   <li>accessKey, secretKey, token — NEVER</li>
 *   <li>providerUrl, providerOutputUrl — NEVER</li>
 *   <li>storageObjectKey, bucketPath — NEVER</li>
 *   <li>publicUrl, signedUrl, cdnUrl — NEVER</li>
 *   <li>base64, rawPayload — NEVER</li>
 *   <li>rawException, stackTrace — NEVER</li>
 * </ul>
 */
@Data
@Builder
public class StorageReadinessResult {

    /** Overall readiness status. */
    private final ReadinessStatus overallStatus;

    /** Per-component readiness breakdown. */
    private final List<ComponentReadiness> componentStatuses;

    /** Safe human-readable summary. */
    private final String safeSummary;

    /** When this check was performed. */
    private final Instant checkedAt;

    /**
     * Overall readiness statuses.
     */
    public enum ReadinessStatus {
        /** All enabled components are valid and ready. */
        READY,
        /** All production storage features are disabled (default/local). */
        DISABLED,
        /** Required config/dependency is missing for enabled feature. */
        NOT_READY,
        /** Enabled component has invalid/incomplete config. */
        MISCONFIGURED,
        /** Core storage ready but optional component disabled. */
        DEGRADED
    }

    /**
     * Per-component readiness entry.
     */
    @Data
    @Builder
    public static class ComponentReadiness {
        private final ConfigValidationResult.ConfigComponent component;
        private final ReadinessStatus status;
        private final String safeMessage;

        /**
         * Safe toString — no secrets/config values.
         */
        @Override
        public String toString() {
            return "ComponentReadiness{component=" + component +
                    ", status=" + status +
                    ", safeMessage='" + safeMessage + "'}";
        }
    }

    /**
     * Safe toString — no secrets/config values.
     */
    @Override
    public String toString() {
        return "StorageReadinessResult{overallStatus=" + overallStatus +
                ", componentCount=" + (componentStatuses != null ? componentStatuses.size() : 0) +
                ", safeSummary='" + safeSummary + "'" +
                ", checkedAt=" + checkedAt + "}";
    }
}
