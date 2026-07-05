package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigComponent;
import com.anadoluatlasi.backend.imagegen.service.StorageReadinessResult.ComponentReadiness;
import com.anadoluatlasi.backend.imagegen.service.StorageReadinessResult.ReadinessStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * FP-011 Task 03: Internal-only storage readiness check service.
 *
 * <p>Evaluates overall production storage readiness by combining
 * config validation results from {@link RuntimeStorageConfigValidator}.
 * Reports safe status categories only — NEVER exposes secrets, config values,
 * provider URLs, storage keys, bucket paths, or stack traces.</p>
 *
 * <p>This service:</p>
 * <ul>
 *   <li>Is internal-only — no public/admin endpoint</li>
 *   <li>Does NOT perform real external S3/provider calls</li>
 *   <li>Does NOT expose secrets/config values</li>
 *   <li>Reuses {@link RuntimeStorageConfigValidator} for config evaluation</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalStorageReadinessService {

    private final RuntimeStorageConfigValidator configValidator;

    /**
     * Check overall storage readiness.
     *
     * <p>Evaluates all config components and maps to safe overall status.
     * No real external calls are performed.</p>
     *
     * @return safe readiness result — never contains secrets
     */
    public StorageReadinessResult checkReadiness() {
        List<ConfigValidationResult> validationResults = configValidator.validateAll();

        List<ComponentReadiness> componentStatuses = new ArrayList<>();
        for (ConfigValidationResult vr : validationResults) {
            componentStatuses.add(mapToComponentReadiness(vr));
        }

        ReadinessStatus overall = computeOverallStatus(componentStatuses);
        String summary = computeSafeSummary(overall, componentStatuses);

        StorageReadinessResult result = StorageReadinessResult.builder()
                .overallStatus(overall)
                .componentStatuses(componentStatuses)
                .safeSummary(summary)
                .checkedAt(Instant.now())
                .build();

        // Safe logging — status categories only
        log.info("[STORAGE_READINESS] overall={}, components={}",
                result.getOverallStatus(), componentStatuses.size());
        for (ComponentReadiness cr : componentStatuses) {
            log.info("[STORAGE_READINESS] {}: {}", cr.getComponent(), cr.getStatus());
        }

        return result;
    }

    // ── Internal mapping ────────────────────────────────────────────

    private ComponentReadiness mapToComponentReadiness(ConfigValidationResult vr) {
        ReadinessStatus status;
        if (vr.isValid()) {
            status = vr.getReadiness() == ConfigValidationResult.ConfigReadiness.DISABLED
                    ? ReadinessStatus.DISABLED
                    : ReadinessStatus.READY;
        } else {
            status = vr.getReadiness() == ConfigValidationResult.ConfigReadiness.DEPENDENCY_MISSING
                    ? ReadinessStatus.NOT_READY
                    : ReadinessStatus.MISCONFIGURED;
        }

        return ComponentReadiness.builder()
                .component(vr.getComponent())
                .status(status)
                .safeMessage(vr.getSafeMessage())
                .build();
    }

    /**
     * Compute overall status from component readiness.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>Any MISCONFIGURED → overall MISCONFIGURED</li>
     *   <li>Any NOT_READY → overall NOT_READY</li>
     *   <li>All DISABLED → overall DISABLED</li>
     *   <li>All enabled READY → overall READY</li>
     *   <li>Core storage READY but optional handoff DISABLED → DEGRADED</li>
     * </ul>
     */
    private ReadinessStatus computeOverallStatus(List<ComponentReadiness> components) {
        // Check for any misconfigured — highest severity
        boolean anyMisconfigured = components.stream()
                .anyMatch(c -> c.getStatus() == ReadinessStatus.MISCONFIGURED);
        if (anyMisconfigured) {
            return ReadinessStatus.MISCONFIGURED;
        }

        // Check for any not-ready (dependency missing)
        boolean anyNotReady = components.stream()
                .anyMatch(c -> c.getStatus() == ReadinessStatus.NOT_READY);
        if (anyNotReady) {
            return ReadinessStatus.NOT_READY;
        }

        // Check if all disabled
        boolean allDisabled = components.stream()
                .allMatch(c -> c.getStatus() == ReadinessStatus.DISABLED);
        if (allDisabled) {
            return ReadinessStatus.DISABLED;
        }

        // Check if all non-disabled are READY
        boolean allEnabledReady = components.stream()
                .allMatch(c -> c.getStatus() == ReadinessStatus.READY
                        || c.getStatus() == ReadinessStatus.DISABLED);
        if (!allEnabledReady) {
            return ReadinessStatus.NOT_READY;
        }

        // DEGRADED: core download+storage ready but immediate handoff disabled
        ComponentReadiness downloadStatus = findComponent(components, ConfigComponent.PROVIDER_DOWNLOAD);
        ComponentReadiness storageStatus = findComponent(components, ConfigComponent.PRIVATE_STORAGE);
        ComponentReadiness handoffStatus = findComponent(components, ConfigComponent.IMMEDIATE_HANDOFF);

        boolean downloadReady = downloadStatus != null && downloadStatus.getStatus() == ReadinessStatus.READY;
        boolean storageReady = storageStatus != null && storageStatus.getStatus() == ReadinessStatus.READY;
        boolean handoffDisabled = handoffStatus != null && handoffStatus.getStatus() == ReadinessStatus.DISABLED;

        if (downloadReady && storageReady && handoffDisabled) {
            return ReadinessStatus.DEGRADED;
        }

        return ReadinessStatus.READY;
    }

    private ComponentReadiness findComponent(List<ComponentReadiness> components, ConfigComponent target) {
        return components.stream()
                .filter(c -> c.getComponent() == target)
                .findFirst()
                .orElse(null);
    }

    private String computeSafeSummary(ReadinessStatus overall, List<ComponentReadiness> components) {
        return switch (overall) {
            case READY -> "All enabled storage components are ready";
            case DISABLED -> "All production storage features are disabled";
            case NOT_READY -> "Required storage dependencies are not ready";
            case MISCONFIGURED -> "Storage configuration is invalid or incomplete";
            case DEGRADED -> "Core storage is ready but optional components are disabled";
        };
    }
}
