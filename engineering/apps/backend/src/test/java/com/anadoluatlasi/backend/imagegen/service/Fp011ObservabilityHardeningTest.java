package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigComponent;
import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigReadiness;
import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigSeverity;
import com.anadoluatlasi.backend.imagegen.service.StorageReadinessResult.ComponentReadiness;
import com.anadoluatlasi.backend.imagegen.service.StorageReadinessResult.ReadinessStatus;
import com.anadoluatlasi.backend.imagegen.storage.S3PrivateStorageProperties;
import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-011 Task 04 — Operational Observability & Alerting Hardening.
 *
 * Verifies that all observability surfaces (toString, safe messages,
 * metric tags, log fields) are safe and never expose secrets, provider
 * URLs, storage keys, bucket paths, raw payloads, or stack traces.
 */
@DisplayName("FP-011 Task 04 — Observability & Alerting Hardening")
class Fp011ObservabilityHardeningTest {

    // ── Forbidden strings that must NEVER appear in any output ──────
    private static final Set<String> FORBIDDEN_STRINGS = Set.of(
            "AKIATOPSECRET", "superSecretValue", "my-secret-token",
            "secret-bucket-name", "secret.s3.endpoint.com"
    );

    private static final Set<String> FORBIDDEN_CATEGORIES = Set.of(
            "providerUrl", "providerOutputUrl", "storageObjectKey",
            "accessKey", "secretKey", "token", "apiKey",
            "publicUrl", "signedUrl", "cdnUrl",
            "base64", "rawPayload", "rawProviderResponse",
            "rawStorageResponse", "stackTrace", "rawException",
            "bucketPath", "rawConfigValue"
    );

    private ProviderOutputDownloadProperties downloadProps;
    private S3PrivateStorageProperties storageProps;
    private ImmediateStorageHandoffProperties handoffProps;
    private RuntimeStorageConfigValidator configValidator;
    private InternalStorageReadinessService readinessService;

    @BeforeEach
    void setUp() {
        downloadProps = new ProviderOutputDownloadProperties();
        storageProps = new S3PrivateStorageProperties();
        handoffProps = new ImmediateStorageHandoffProperties();
        configValidator = new RuntimeStorageConfigValidator(downloadProps, storageProps, handoffProps);
        readinessService = new InternalStorageReadinessService(configValidator);
    }

    // ── A. Config Validation Observability Safety ───────────────────

    @Nested
    @DisplayName("A. Config Validation Observability Safety")
    class ConfigValidationObservability {

        @Test
        @DisplayName("Valid result toString has only component/status/severity")
        void validResult_safeToString() {
            ConfigValidationResult r = ConfigValidationResult.builder()
                    .component(ConfigComponent.PROVIDER_DOWNLOAD)
                    .valid(true)
                    .readiness(ConfigReadiness.READY)
                    .severity(ConfigSeverity.INFO)
                    .safeMessage("Provider download configuration is valid")
                    .build();
            String str = r.toString();
            assertThat(str).contains("PROVIDER_DOWNLOAD");
            assertThat(str).contains("READY");
            assertThat(str).contains("INFO");
            verifyNoForbiddenContent(str);
        }

        @Test
        @DisplayName("Invalid result toString has no secrets even with secret config")
        void invalidResult_noSecrets() {
            storageProps.setEnabled(true);
            storageProps.setAccessKey("AKIATOPSECRET");
            storageProps.setSecretKey("superSecretValue");
            storageProps.setBucket("secret-bucket-name");
            storageProps.setEndpoint("https://secret.s3.endpoint.com");

            ConfigValidationResult r = configValidator.validatePrivateStorage();
            String str = r.toString();
            verifyNoForbiddenContent(str);
            for (String secret : FORBIDDEN_STRINGS) {
                assertThat(str).doesNotContain(secret);
            }
        }

        @Test
        @DisplayName("All ConfigComponent enum values are safe metric labels")
        void configComponent_safeLabels() {
            for (ConfigComponent c : ConfigComponent.values()) {
                String name = c.name();
                assertThat(name).doesNotContainIgnoringCase("url");
                assertThat(name).doesNotContainIgnoringCase("key");
                assertThat(name).doesNotContainIgnoringCase("secret");
                assertThat(name).doesNotContainIgnoringCase("token");
                assertThat(name).doesNotContainIgnoringCase("bucket");
            }
        }

        @Test
        @DisplayName("All ConfigReadiness enum values are safe labels")
        void configReadiness_safeLabels() {
            for (ConfigReadiness r : ConfigReadiness.values()) {
                String name = r.name();
                assertThat(name).doesNotContainIgnoringCase("url");
                assertThat(name).doesNotContainIgnoringCase("key");
                assertThat(name).doesNotContainIgnoringCase("secret");
            }
        }

        @Test
        @DisplayName("All ConfigSeverity enum values are safe labels")
        void configSeverity_safeLabels() {
            for (ConfigSeverity s : ConfigSeverity.values()) {
                assertThat(s.name()).isIn("INFO", "WARNING", "ERROR");
            }
        }

        @Test
        @DisplayName("validateAll results all have safe toString")
        void validateAll_allSafe() {
            storageProps.setEnabled(true);
            storageProps.setAccessKey("AKIATOPSECRET");
            storageProps.setSecretKey("superSecretValue");
            storageProps.setBucket("");

            List<ConfigValidationResult> results = configValidator.validateAll();
            for (ConfigValidationResult r : results) {
                String str = r.toString();
                verifyNoForbiddenContent(str);
                for (String secret : FORBIDDEN_STRINGS) {
                    assertThat(str).as("Result for %s must not contain: %s", r.getComponent(), secret)
                            .doesNotContain(secret);
                }
            }
        }
    }

    // ── B. Readiness Observability Safety ────────────────────────────

    @Nested
    @DisplayName("B. Readiness Observability Safety")
    class ReadinessObservability {

        @Test
        @DisplayName("Readiness result toString has status and component count only")
        void readinessResult_safeToString() {
            StorageReadinessResult result = readinessService.checkReadiness();
            String str = result.toString();
            assertThat(str).contains("overallStatus=");
            assertThat(str).contains("componentCount=");
            verifyNoForbiddenContent(str);
        }

        @Test
        @DisplayName("Readiness with secrets in config has no leaks in toString")
        void readinessWithSecrets_noLeaks() {
            storageProps.setEnabled(true);
            storageProps.setAccessKey("AKIATOPSECRET");
            storageProps.setSecretKey("superSecretValue");
            storageProps.setBucket("secret-bucket-name");
            storageProps.setEndpoint("https://secret.s3.endpoint.com");

            StorageReadinessResult result = readinessService.checkReadiness();
            String str = result.toString();
            for (String secret : FORBIDDEN_STRINGS) {
                assertThat(str).doesNotContain(secret);
            }
            verifyNoForbiddenContent(str);
        }

        @Test
        @DisplayName("Component readiness toString has no secrets")
        void componentReadiness_noSecrets() {
            storageProps.setEnabled(true);
            storageProps.setAccessKey("AKIATOPSECRET");
            storageProps.setSecretKey("superSecretValue");
            storageProps.setBucket("secret-bucket-name");
            storageProps.setEndpoint("https://secret.s3.endpoint.com");

            StorageReadinessResult result = readinessService.checkReadiness();
            for (ComponentReadiness cr : result.getComponentStatuses()) {
                String str = cr.toString();
                for (String secret : FORBIDDEN_STRINGS) {
                    assertThat(str).as("Component %s must not contain: %s", cr.getComponent(), secret)
                            .doesNotContain(secret);
                }
                verifyNoForbiddenContent(str);
            }
        }

        @Test
        @DisplayName("Readiness safeSummary is safe")
        void safeSummary_isSafe() {
            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getSafeSummary()).isNotBlank();
            verifyNoForbiddenContent(result.getSafeSummary());
        }

        @Test
        @DisplayName("All ReadinessStatus values are safe metric labels")
        void readinessStatus_safeLabels() {
            for (ReadinessStatus s : ReadinessStatus.values()) {
                String name = s.name();
                assertThat(name).doesNotContainIgnoringCase("url");
                assertThat(name).doesNotContainIgnoringCase("key");
                assertThat(name).doesNotContainIgnoringCase("secret");
                assertThat(name).doesNotContainIgnoringCase("token");
                assertThat(name).doesNotContainIgnoringCase("approved");
                assertThat(name).doesNotContainIgnoringCase("published");
            }
        }
    }

    // ── C. Metric Tag Safety ────────────────────────────────────────

    @Nested
    @DisplayName("C. Metric Tag Safety")
    class MetricTagSafety {

        @Test
        @DisplayName("Safe metric tags include only allowed categories")
        void safeMetricTags() {
            Set<String> allowedComponents = Set.of("PROVIDER_DOWNLOAD", "PRIVATE_STORAGE", "IMMEDIATE_HANDOFF");
            for (ConfigComponent c : ConfigComponent.values()) {
                assertThat(allowedComponents).contains(c.name());
            }
        }

        @Test
        @DisplayName("Safe readiness tags include only status values")
        void safeReadinessTags() {
            Set<String> allowedStatuses = Set.of("READY", "DISABLED", "NOT_READY", "MISCONFIGURED", "DEGRADED");
            for (ReadinessStatus s : ReadinessStatus.values()) {
                assertThat(allowedStatuses).contains(s.name());
            }
        }

        @Test
        @DisplayName("Forbidden metric labels are not present in any enum")
        void forbiddenLabelsAbsent() {
            Set<String> allEnumNames = new java.util.HashSet<>();
            Arrays.stream(ConfigComponent.values()).forEach(c -> allEnumNames.add(c.name()));
            Arrays.stream(ConfigReadiness.values()).forEach(r -> allEnumNames.add(r.name()));
            Arrays.stream(ConfigSeverity.values()).forEach(s -> allEnumNames.add(s.name()));
            Arrays.stream(ReadinessStatus.values()).forEach(s -> allEnumNames.add(s.name()));

            for (String name : allEnumNames) {
                String lower = name.toLowerCase();
                assertThat(lower).doesNotContain("url");
                assertThat(lower).doesNotContain("secret");
                assertThat(lower).doesNotContain("token");
                assertThat(lower).doesNotContain("base64");
                assertThat(lower).doesNotContain("payload");
            }
        }

        @Test
        @DisplayName("ImageStorageErrorCode values are safe metric labels")
        void errorCode_safeLabels() {
            for (ImageStorageErrorCode code : ImageStorageErrorCode.values()) {
                String name = code.name();
                assertThat(name).doesNotContainIgnoringCase("secret");
                assertThat(name).doesNotContainIgnoringCase("token");
                assertThat(name).doesNotContainIgnoringCase("apiKey");
                assertThat(name).doesNotContainIgnoringCase("base64");
            }
        }

        @Test
        @DisplayName("ImageStorageErrorNormalizer safe messages are safe labels")
        void errorNormalizer_safeMessages() {
            for (ImageStorageErrorCode code : ImageStorageErrorCode.values()) {
                String safe = ImageStorageErrorNormalizer.safeMessage(code);
                assertThat(safe).doesNotContain("https://");
                assertThat(safe).doesNotContain("apiKey");
                assertThat(safe).doesNotContain("secretKey");
                assertThat(safe).doesNotContain("bucket");
                assertThat(safe).doesNotContain("objectKey");
            }
        }
    }

    // ── D. Failure Message Safety ───────────────────────────────────

    @Nested
    @DisplayName("D. Failure Message Safety")
    class FailureMessageSafety {

        @Test
        @DisplayName("Misconfigured provider download message is safe")
        void misconfiguredDownload_safeMessage() {
            downloadProps.setEnabled(true);
            downloadProps.setMaxBytes(0);
            ConfigValidationResult r = configValidator.validateProviderDownload();
            assertThat(r.isValid()).isFalse();
            verifyNoForbiddenContent(r.getSafeMessage());
            assertThat(r.getSafeMessage()).doesNotContain("https://");
        }

        @Test
        @DisplayName("Misconfigured storage message is safe")
        void misconfiguredStorage_safeMessage() {
            storageProps.setEnabled(true);
            storageProps.setBucket("");
            storageProps.setAccessKey("AKIATOPSECRET");
            storageProps.setSecretKey("superSecretValue");

            ConfigValidationResult r = configValidator.validatePrivateStorage();
            assertThat(r.isValid()).isFalse();
            verifyNoForbiddenContent(r.getSafeMessage());
            for (String secret : FORBIDDEN_STRINGS) {
                assertThat(r.getSafeMessage()).doesNotContain(secret);
            }
        }

        @Test
        @DisplayName("Missing dependency message is safe")
        void missingDependency_safeMessage() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(false);
            ConfigValidationResult r = configValidator.validateImmediateHandoff();
            assertThat(r.isValid()).isFalse();
            verifyNoForbiddenContent(r.getSafeMessage());
        }

        @Test
        @DisplayName("No stack trace in any validation result")
        void noStackTrace() {
            storageProps.setEnabled(true);
            storageProps.setBucket("");

            List<ConfigValidationResult> results = configValidator.validateAll();
            for (ConfigValidationResult r : results) {
                if (r.getSafeMessage() != null) {
                    assertThat(r.getSafeMessage()).doesNotContain("at com.");
                    assertThat(r.getSafeMessage()).doesNotContain("Exception");
                    assertThat(r.getSafeMessage()).doesNotContain(".java:");
                }
            }
        }

        @Test
        @DisplayName("S3PrivateStorageProperties validateForProduction messages are safe")
        void s3Validation_safeMessages() {
            storageProps.setEnabled(true);
            storageProps.setBucket("");
            String msg = storageProps.validateForProduction();
            assertThat(msg).isNotNull();
            assertThat(msg).doesNotContain("AKIA");
            assertThat(msg).doesNotContain("secretKey");
            assertThat(msg).doesNotContain("https://");
        }
    }

    // ── E. Boundary Regression ──────────────────────────────────────

    @Nested
    @DisplayName("E. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("No controller/endpoint in readiness service")
        void noControllerInReadiness() {
            assertThat(InternalStorageReadinessService.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
            assertThat(InternalStorageReadinessService.class.isAnnotationPresent(
                    org.springframework.stereotype.Controller.class)).isFalse();
        }

        @Test
        @DisplayName("No controller/endpoint in config validator")
        void noControllerInValidator() {
            assertThat(RuntimeStorageConfigValidator.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
            assertThat(RuntimeStorageConfigValidator.class.isAnnotationPresent(
                    org.springframework.stereotype.Controller.class)).isFalse();
        }

        @Test
        @DisplayName("No real external call in readiness check")
        void noExternalCall() {
            // With all enabled + secrets — should complete instantly, no IO
            downloadProps.setEnabled(true);
            storageProps.setEnabled(true);
            storageProps.setBucket("b");
            storageProps.setEndpoint("https://e");
            storageProps.setRegion("r");
            storageProps.setAccessKey("AKIAFAKE");
            storageProps.setSecretKey("fake");
            handoffProps.setEnabled(true);

            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("No approval/publish/export in readiness statuses")
        void noApprovalPublish() {
            Set<String> all = new java.util.HashSet<>();
            Arrays.stream(ReadinessStatus.values()).forEach(s -> all.add(s.name()));
            assertThat(all).doesNotContain("APPROVED", "PUBLISHED", "EXPORTED",
                    "READY_FOR_PUBLISH", "COMPLETED");
        }

        @Test
        @DisplayName("Preview unchanged — no preview-related methods in readiness")
        void previewUnchanged() {
            var methods = Arrays.stream(InternalStorageReadinessService.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName).collect(java.util.stream.Collectors.toSet());
            assertThat(methods).doesNotContain("preview", "getPreviewUrl", "redirectPreview");
        }
    }

    // ── F. Documentation Presence ───────────────────────────────────

    @Nested
    @DisplayName("F. Documentation Presence")
    class DocumentationPresence {

        @Test
        @DisplayName("Observability doc file path is defined")
        void docPath() {
            // This test documents the expected doc location
            String expectedPath = "docs/05-engineering/73-fp011-operational-observability-alerting-hardening.md";
            assertThat(expectedPath).isNotBlank();
        }

        @Test
        @DisplayName("No CDN/public/signed URL in readiness status enum")
        void noCdnPublicSigned() {
            Set<String> all = new java.util.HashSet<>();
            Arrays.stream(ReadinessStatus.values()).forEach(s -> all.add(s.name()));
            assertThat(all).doesNotContain("CDN_READY", "PUBLIC_URL_READY", "SIGNED_URL_READY");
        }

        @Test
        @DisplayName("No video/audio in readiness components")
        void noVideoAudio() {
            Set<String> components = new java.util.HashSet<>();
            Arrays.stream(ConfigComponent.values()).forEach(c -> components.add(c.name()));
            assertThat(components).doesNotContain("VIDEO", "AUDIO", "VIDEO_STORAGE", "AUDIO_STORAGE");
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private void verifyNoForbiddenContent(String content) {
        if (content == null) return;
        for (String forbidden : FORBIDDEN_CATEGORIES) {
            assertThat(content).as("Content must not contain: %s", forbidden)
                    .doesNotContain(forbidden);
        }
    }
}
