package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigComponent;
import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigReadiness;
import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigSeverity;
import com.anadoluatlasi.backend.imagegen.storage.S3PrivateStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * FP-011 Task 02: Runtime config validation for production storage enablement.
 *
 * <p>Validates provider download, S3/private storage, and immediate handoff
 * configuration at runtime. Returns safe validation results — NEVER exposes
 * secrets, config values, provider URLs, or storage keys.</p>
 *
 * <p>This service:</p>
 * <ul>
 *   <li>Does NOT make real external calls</li>
 *   <li>Does NOT fail startup by itself — caller decides</li>
 *   <li>Does NOT expose secrets/config values in results or logs</li>
 *   <li>Does NOT create endpoints</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeStorageConfigValidator {

    private final ProviderOutputDownloadProperties downloadProperties;
    private final S3PrivateStorageProperties storageProperties;
    private final ImmediateStorageHandoffProperties handoffProperties;

    /**
     * Validate all storage-related config groups.
     *
     * @return list of safe validation results — one per config component
     */
    public List<ConfigValidationResult> validateAll() {
        List<ConfigValidationResult> results = new ArrayList<>();
        results.add(validateProviderDownload());
        results.add(validatePrivateStorage());
        results.add(validateImmediateHandoff());

        // Safe logging — categories only, NEVER secrets/values
        for (ConfigValidationResult r : results) {
            if (r.isValid()) {
                log.info("[CONFIG_VALIDATION] {}: readiness={}", r.getComponent(), r.getReadiness());
            } else {
                log.warn("[CONFIG_VALIDATION] {}: readiness={}, severity={}, code={}",
                        r.getComponent(), r.getReadiness(), r.getSeverity(), r.getSafeErrorCode());
            }
        }

        return results;
    }

    /**
     * Validate provider download config.
     */
    public ConfigValidationResult validateProviderDownload() {
        if (!downloadProperties.isEnabled()) {
            return ConfigValidationResult.builder()
                    .component(ConfigComponent.PROVIDER_DOWNLOAD)
                    .valid(true)
                    .readiness(ConfigReadiness.DISABLED)
                    .severity(ConfigSeverity.INFO)
                    .safeMessage("Provider download is disabled")
                    .build();
        }

        // Validate timeout values
        if (downloadProperties.getConnectTimeoutMs() <= 0) {
            return invalidResult(ConfigComponent.PROVIDER_DOWNLOAD,
                    "PROVIDER_DOWNLOAD_CONFIGURATION_INVALID",
                    "Provider download connect timeout must be positive");
        }
        if (downloadProperties.getReadTimeoutMs() <= 0) {
            return invalidResult(ConfigComponent.PROVIDER_DOWNLOAD,
                    "PROVIDER_DOWNLOAD_CONFIGURATION_INVALID",
                    "Provider download read timeout must be positive");
        }
        if (downloadProperties.getConnectTimeoutMs() > 60_000) {
            return invalidResult(ConfigComponent.PROVIDER_DOWNLOAD,
                    "PROVIDER_DOWNLOAD_CONFIGURATION_INVALID",
                    "Provider download connect timeout exceeds safe limit");
        }
        if (downloadProperties.getReadTimeoutMs() > 300_000) {
            return invalidResult(ConfigComponent.PROVIDER_DOWNLOAD,
                    "PROVIDER_DOWNLOAD_CONFIGURATION_INVALID",
                    "Provider download read timeout exceeds safe limit");
        }

        // Validate max bytes
        if (downloadProperties.getMaxBytes() <= 0) {
            return invalidResult(ConfigComponent.PROVIDER_DOWNLOAD,
                    "PROVIDER_DOWNLOAD_CONFIGURATION_INVALID",
                    "Provider download max bytes must be positive");
        }
        if (downloadProperties.getMaxBytes() > 100 * 1024 * 1024) { // 100MB
            return invalidResult(ConfigComponent.PROVIDER_DOWNLOAD,
                    "PROVIDER_DOWNLOAD_CONFIGURATION_INVALID",
                    "Provider download max bytes exceeds safe limit");
        }

        // Validate redirects
        if (downloadProperties.getMaxRedirects() < 0) {
            return invalidResult(ConfigComponent.PROVIDER_DOWNLOAD,
                    "PROVIDER_DOWNLOAD_CONFIGURATION_INVALID",
                    "Provider download max redirects must be non-negative");
        }
        if (downloadProperties.getMaxRedirects() > 10) {
            return invalidResult(ConfigComponent.PROVIDER_DOWNLOAD,
                    "PROVIDER_DOWNLOAD_CONFIGURATION_INVALID",
                    "Provider download max redirects exceeds safe limit");
        }

        // Validate allowed content types
        if (downloadProperties.getAllowedContentTypes() == null
                || downloadProperties.getAllowedContentTypes().isEmpty()) {
            return invalidResult(ConfigComponent.PROVIDER_DOWNLOAD,
                    "PROVIDER_DOWNLOAD_CONFIGURATION_INVALID",
                    "Provider download allowed content types must not be empty");
        }

        return ConfigValidationResult.builder()
                .component(ConfigComponent.PROVIDER_DOWNLOAD)
                .valid(true)
                .readiness(ConfigReadiness.READY)
                .severity(ConfigSeverity.INFO)
                .safeMessage("Provider download configuration is valid")
                .build();
    }

    /**
     * Validate S3/private storage config.
     */
    public ConfigValidationResult validatePrivateStorage() {
        if (!storageProperties.isEnabled()) {
            return ConfigValidationResult.builder()
                    .component(ConfigComponent.PRIVATE_STORAGE)
                    .valid(true)
                    .readiness(ConfigReadiness.DISABLED)
                    .severity(ConfigSeverity.INFO)
                    .safeMessage("Private storage is disabled")
                    .build();
        }

        // Delegate to existing production validation — returns safe message
        String validationError = storageProperties.validateForProduction();
        if (validationError != null) {
            return invalidResult(ConfigComponent.PRIVATE_STORAGE,
                    "STORAGE_CONFIGURATION_INVALID",
                    validationError);
        }

        return ConfigValidationResult.builder()
                .component(ConfigComponent.PRIVATE_STORAGE)
                .valid(true)
                .readiness(ConfigReadiness.READY)
                .severity(ConfigSeverity.INFO)
                .safeMessage("Private storage configuration is valid")
                .build();
    }

    /**
     * Validate immediate handoff config and its dependencies.
     */
    public ConfigValidationResult validateImmediateHandoff() {
        if (!handoffProperties.isEnabled()) {
            return ConfigValidationResult.builder()
                    .component(ConfigComponent.IMMEDIATE_HANDOFF)
                    .valid(true)
                    .readiness(ConfigReadiness.DISABLED)
                    .severity(ConfigSeverity.INFO)
                    .safeMessage("Immediate handoff is disabled")
                    .build();
        }

        // Dependency: provider download must be enabled
        if (!downloadProperties.isEnabled()) {
            return ConfigValidationResult.builder()
                    .component(ConfigComponent.IMMEDIATE_HANDOFF)
                    .valid(false)
                    .readiness(ConfigReadiness.DEPENDENCY_MISSING)
                    .severity(ConfigSeverity.ERROR)
                    .safeErrorCode("IMMEDIATE_HANDOFF_DEPENDENCY_MISSING")
                    .safeMessage("Immediate handoff is enabled but provider download is disabled")
                    .build();
        }

        // Dependency: private storage must be enabled
        if (!storageProperties.isEnabled()) {
            return ConfigValidationResult.builder()
                    .component(ConfigComponent.IMMEDIATE_HANDOFF)
                    .valid(false)
                    .readiness(ConfigReadiness.DEPENDENCY_MISSING)
                    .severity(ConfigSeverity.ERROR)
                    .safeErrorCode("IMMEDIATE_HANDOFF_DEPENDENCY_MISSING")
                    .safeMessage("Immediate handoff is enabled but private storage is disabled")
                    .build();
        }

        // Dependency configs must be valid
        ConfigValidationResult downloadResult = validateProviderDownload();
        if (!downloadResult.isValid()) {
            return ConfigValidationResult.builder()
                    .component(ConfigComponent.IMMEDIATE_HANDOFF)
                    .valid(false)
                    .readiness(ConfigReadiness.DEPENDENCY_MISSING)
                    .severity(ConfigSeverity.ERROR)
                    .safeErrorCode("IMMEDIATE_HANDOFF_DEPENDENCY_MISSING")
                    .safeMessage("Immediate handoff is enabled but provider download configuration is invalid")
                    .build();
        }

        ConfigValidationResult storageResult = validatePrivateStorage();
        if (!storageResult.isValid()) {
            return ConfigValidationResult.builder()
                    .component(ConfigComponent.IMMEDIATE_HANDOFF)
                    .valid(false)
                    .readiness(ConfigReadiness.DEPENDENCY_MISSING)
                    .severity(ConfigSeverity.ERROR)
                    .safeErrorCode("IMMEDIATE_HANDOFF_DEPENDENCY_MISSING")
                    .safeMessage("Immediate handoff is enabled but private storage configuration is invalid")
                    .build();
        }

        return ConfigValidationResult.builder()
                .component(ConfigComponent.IMMEDIATE_HANDOFF)
                .valid(true)
                .readiness(ConfigReadiness.READY)
                .severity(ConfigSeverity.INFO)
                .safeMessage("Immediate handoff configuration is valid with dependencies")
                .build();
    }

    // ── Private helpers ────────────────────────────────────────────

    private ConfigValidationResult invalidResult(ConfigComponent component,
                                                  String safeErrorCode,
                                                  String safeMessage) {
        return ConfigValidationResult.builder()
                .component(component)
                .valid(false)
                .readiness(ConfigReadiness.INVALID)
                .severity(ConfigSeverity.ERROR)
                .safeErrorCode(safeErrorCode)
                .safeMessage(safeMessage)
                .build();
    }
}
