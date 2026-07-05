package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigComponent;
import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigReadiness;
import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigSeverity;
import com.anadoluatlasi.backend.imagegen.storage.S3PrivateStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-011 Task 02 — Runtime Config Validation & Startup Safety.
 *
 * All tests use defaults/configured values — no real external calls.
 */
@DisplayName("FP-011 Task 02 — Runtime Config Validation")
class RuntimeStorageConfigValidatorTest {

    private ProviderOutputDownloadProperties downloadProps;
    private S3PrivateStorageProperties storageProps;
    private ImmediateStorageHandoffProperties handoffProps;
    private RuntimeStorageConfigValidator validator;

    @BeforeEach
    void setUp() {
        downloadProps = new ProviderOutputDownloadProperties();
        storageProps = new S3PrivateStorageProperties();
        handoffProps = new ImmediateStorageHandoffProperties();
        validator = new RuntimeStorageConfigValidator(downloadProps, storageProps, handoffProps);
    }

    // ── A. Provider Download Config Validation ──────────────────────

    @Nested
    @DisplayName("A. Provider Download Config Validation")
    class ProviderDownloadValidation {

        @Test
        @DisplayName("Disabled config → valid, DISABLED readiness")
        void disabled_passesAsDisabled() {
            downloadProps.setEnabled(false);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getReadiness()).isEqualTo(ConfigReadiness.DISABLED);
            assertThat(result.getComponent()).isEqualTo(ConfigComponent.PROVIDER_DOWNLOAD);
        }

        @Test
        @DisplayName("Enabled valid config → valid, READY")
        void enabled_validConfig_ready() {
            downloadProps.setEnabled(true);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getReadiness()).isEqualTo(ConfigReadiness.READY);
        }

        @Test
        @DisplayName("Empty allowed content types → invalid")
        void enabled_emptyContentTypes_invalid() {
            downloadProps.setEnabled(true);
            downloadProps.setAllowedContentTypes(Set.of());
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getSafeErrorCode()).isEqualTo("PROVIDER_DOWNLOAD_CONFIGURATION_INVALID");
            assertThat(result.getSafeMessage()).contains("content types");
        }

        @Test
        @DisplayName("Null allowed content types → invalid")
        void enabled_nullContentTypes_invalid() {
            downloadProps.setEnabled(true);
            downloadProps.setAllowedContentTypes(null);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("Zero connect timeout → invalid")
        void enabled_zeroConnectTimeout_invalid() {
            downloadProps.setEnabled(true);
            downloadProps.setConnectTimeoutMs(0);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getSafeMessage()).contains("connect timeout");
        }

        @Test
        @DisplayName("Negative read timeout → invalid")
        void enabled_negativeReadTimeout_invalid() {
            downloadProps.setEnabled(true);
            downloadProps.setReadTimeoutMs(-1);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("Excessive connect timeout → invalid")
        void enabled_excessiveConnectTimeout_invalid() {
            downloadProps.setEnabled(true);
            downloadProps.setConnectTimeoutMs(120_000);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getSafeMessage()).contains("safe limit");
        }

        @Test
        @DisplayName("Excessive read timeout → invalid")
        void enabled_excessiveReadTimeout_invalid() {
            downloadProps.setEnabled(true);
            downloadProps.setReadTimeoutMs(600_000);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("Zero max bytes → invalid")
        void enabled_zeroMaxBytes_invalid() {
            downloadProps.setEnabled(true);
            downloadProps.setMaxBytes(0);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getSafeMessage()).contains("max bytes");
        }

        @Test
        @DisplayName("Excessive max bytes → invalid")
        void enabled_excessiveMaxBytes_invalid() {
            downloadProps.setEnabled(true);
            downloadProps.setMaxBytes(200 * 1024 * 1024);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("Negative redirects → invalid")
        void enabled_negativeRedirects_invalid() {
            downloadProps.setEnabled(true);
            downloadProps.setMaxRedirects(-1);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("Excessive redirects → invalid")
        void enabled_excessiveRedirects_invalid() {
            downloadProps.setEnabled(true);
            downloadProps.setMaxRedirects(50);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("No provider URL required at startup")
        void noProviderUrlRequired() {
            downloadProps.setEnabled(true);
            ConfigValidationResult result = validator.validateProviderDownload();
            // Valid without any provider URL — it's transient runtime data
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Error messages contain no URL/secret/raw values")
        void errorMessages_noSecrets() {
            downloadProps.setEnabled(true);
            downloadProps.setMaxBytes(0);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.getSafeMessage()).doesNotContain("https://");
            assertThat(result.getSafeMessage()).doesNotContain("apiKey");
            assertThat(result.getSafeMessage()).doesNotContain("secretKey");
            assertThat(result.toString()).doesNotContain("https://");
        }
    }

    // ── B. S3/Private Storage Config Validation ─────────────────────

    @Nested
    @DisplayName("B. S3/Private Storage Config Validation")
    class PrivateStorageValidation {

        @Test
        @DisplayName("Disabled config → valid, DISABLED")
        void disabled_passesAsDisabled() {
            storageProps.setEnabled(false);
            ConfigValidationResult result = validator.validatePrivateStorage();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getReadiness()).isEqualTo(ConfigReadiness.DISABLED);
        }

        @Test
        @DisplayName("Enabled valid config → valid, READY")
        void enabled_validConfig_ready() {
            storageProps.setEnabled(true);
            storageProps.setBucket("test-bucket");
            storageProps.setEndpoint("https://s3.test.com");
            storageProps.setRegion("eu-west-1");
            storageProps.setAccessKey("AKIAFAKEKEY");
            storageProps.setSecretKey("fakesecretvalue");

            ConfigValidationResult result = validator.validatePrivateStorage();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getReadiness()).isEqualTo(ConfigReadiness.READY);
        }

        @Test
        @DisplayName("Enabled missing bucket → invalid")
        void enabled_missingBucket_invalid() {
            storageProps.setEnabled(true);
            storageProps.setBucket("");
            storageProps.setEndpoint("https://s3.test.com");
            storageProps.setRegion("eu-west-1");
            storageProps.setAccessKey("AKIAFAKE");
            storageProps.setSecretKey("fakesecret");

            ConfigValidationResult result = validator.validatePrivateStorage();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getSafeErrorCode()).isEqualTo("STORAGE_CONFIGURATION_INVALID");
            assertThat(result.getSafeMessage()).contains("bucket");
        }

        @Test
        @DisplayName("Enabled missing credentials → invalid with safe message")
        void enabled_missingCredentials_invalidSafe() {
            storageProps.setEnabled(true);
            storageProps.setBucket("test-bucket");
            storageProps.setEndpoint("https://s3.test.com");
            storageProps.setRegion("eu-west-1");
            storageProps.setAccessKey("");
            storageProps.setSecretKey("");

            ConfigValidationResult result = validator.validatePrivateStorage();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getSafeMessage()).contains("credentials");
            assertThat(result.getSafeMessage()).doesNotContain("AKIAFAKE");
            assertThat(result.getSafeMessage()).doesNotContain("fakesecret");
        }

        @Test
        @DisplayName("Secret values never appear in validation result or toString")
        void secretValues_neverInResult() {
            storageProps.setEnabled(true);
            storageProps.setBucket("");
            storageProps.setAccessKey("AKIATOPSECRET123");
            storageProps.setSecretKey("super-secret-key-value-xyz");

            ConfigValidationResult result = validator.validatePrivateStorage();
            String str = result.toString();
            assertThat(str).doesNotContain("AKIATOPSECRET123");
            assertThat(str).doesNotContain("super-secret-key-value-xyz");
        }

        @Test
        @DisplayName("S3PrivateStorageProperties toString remains safe")
        void s3Props_toStringSafe() {
            storageProps.setAccessKey("AKIATOPSECRET");
            storageProps.setSecretKey("supersecretvalue");
            String str = storageProps.toString();
            assertThat(str).doesNotContain("AKIATOPSECRET");
            assertThat(str).doesNotContain("supersecretvalue");
        }
    }

    // ── C. Immediate Handoff Dependency Validation ──────────────────

    @Nested
    @DisplayName("C. Immediate Handoff Dependency Validation")
    class ImmediateHandoffValidation {

        @Test
        @DisplayName("Disabled → valid, DISABLED")
        void disabled_passesAsDisabled() {
            handoffProps.setEnabled(false);
            ConfigValidationResult result = validator.validateImmediateHandoff();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getReadiness()).isEqualTo(ConfigReadiness.DISABLED);
        }

        @Test
        @DisplayName("Enabled + download disabled → DEPENDENCY_MISSING")
        void enabled_downloadDisabled_depMissing() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(false);
            storageProps.setEnabled(true);
            configureValidStorage();

            ConfigValidationResult result = validator.validateImmediateHandoff();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getReadiness()).isEqualTo(ConfigReadiness.DEPENDENCY_MISSING);
            assertThat(result.getSafeMessage()).contains("provider download is disabled");
        }

        @Test
        @DisplayName("Enabled + storage disabled → DEPENDENCY_MISSING")
        void enabled_storageDisabled_depMissing() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(true);
            storageProps.setEnabled(false);

            ConfigValidationResult result = validator.validateImmediateHandoff();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getReadiness()).isEqualTo(ConfigReadiness.DEPENDENCY_MISSING);
            assertThat(result.getSafeMessage()).contains("private storage is disabled");
        }

        @Test
        @DisplayName("Enabled + both deps enabled + valid → READY")
        void enabled_validDependencies_ready() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(true);
            storageProps.setEnabled(true);
            configureValidStorage();

            ConfigValidationResult result = validator.validateImmediateHandoff();
            assertThat(result.isValid()).isTrue();
            assertThat(result.getReadiness()).isEqualTo(ConfigReadiness.READY);
        }

        @Test
        @DisplayName("Enabled + download enabled but invalid → DEPENDENCY_MISSING")
        void enabled_downloadInvalid_depMissing() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(true);
            downloadProps.setMaxBytes(0); // invalid
            storageProps.setEnabled(true);
            configureValidStorage();

            ConfigValidationResult result = validator.validateImmediateHandoff();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getReadiness()).isEqualTo(ConfigReadiness.DEPENDENCY_MISSING);
        }

        @Test
        @DisplayName("Enabled + storage enabled but invalid → DEPENDENCY_MISSING")
        void enabled_storageInvalid_depMissing() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(true);
            storageProps.setEnabled(true);
            storageProps.setBucket(""); // invalid

            ConfigValidationResult result = validator.validateImmediateHandoff();
            assertThat(result.isValid()).isFalse();
            assertThat(result.getReadiness()).isEqualTo(ConfigReadiness.DEPENDENCY_MISSING);
        }

        @Test
        @DisplayName("No provider URL/storage key in result")
        void noProviderUrlOrStorageKey() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(false);
            ConfigValidationResult result = validator.validateImmediateHandoff();
            String str = result.toString();
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("storageObjectKey");
            assertThat(str).doesNotContain("bucket");
        }
    }

    // ── D. Default / Startup Behavior ───────────────────────────────

    @Nested
    @DisplayName("D. Default / Startup Behavior")
    class DefaultBehavior {

        @Test
        @DisplayName("Default properties are all disabled")
        void defaults_allDisabled() {
            assertThat(downloadProps.isEnabled()).isFalse();
            assertThat(storageProps.isEnabled()).isFalse();
            assertThat(handoffProps.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Default validation does not fail — all DISABLED")
        void defaults_allPassAsDisabled() {
            List<ConfigValidationResult> results = validator.validateAll();
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(ConfigValidationResult::isValid);
            assertThat(results).allMatch(r -> r.getReadiness() == ConfigReadiness.DISABLED);
        }

        @Test
        @DisplayName("No real network call during validation")
        void noRealNetworkCall() {
            // All configs enabled with valid values — still no network call
            downloadProps.setEnabled(true);
            storageProps.setEnabled(true);
            configureValidStorage();
            handoffProps.setEnabled(true);

            // This should complete immediately — no real S3/HTTP call
            List<ConfigValidationResult> results = validator.validateAll();
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(ConfigValidationResult::isValid);
        }
    }

    // ── E. Sanitization Regression ──────────────────────────────────

    @Nested
    @DisplayName("E. Sanitization Regression")
    class SanitizationRegression {

        @Test
        @DisplayName("ConfigValidationResult has no secret fields")
        void resultModel_noSecretFields() {
            Set<String> fields = Arrays.stream(ConfigValidationResult.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).doesNotContain("accessKey", "secretKey", "token",
                    "providerUrl", "storageObjectKey", "base64", "rawPayload",
                    "apiKey", "publicUrl", "signedUrl", "cdnUrl");
        }

        @Test
        @DisplayName("ConfigValidationResult toString never exposes secrets")
        void result_toString_noSecrets() {
            ConfigValidationResult r = ConfigValidationResult.builder()
                    .component(ConfigComponent.PRIVATE_STORAGE)
                    .valid(false)
                    .readiness(ConfigReadiness.INVALID)
                    .severity(ConfigSeverity.ERROR)
                    .safeErrorCode("STORAGE_CONFIGURATION_INVALID")
                    .safeMessage("S3 credentials are not configured")
                    .build();
            String str = r.toString();
            assertThat(str).doesNotContain("AKIA");
            assertThat(str).doesNotContain("https://");
        }

        @Test
        @DisplayName("Validation result for storage with fake keys has no key values")
        void storageValidation_fakeKeys_noValues() {
            storageProps.setEnabled(true);
            storageProps.setBucket("mybucket");
            storageProps.setEndpoint("https://s3.prod.com");
            storageProps.setRegion("eu-west-1");
            storageProps.setAccessKey("AKIATOPSECRETFAKE");
            storageProps.setSecretKey("superFakeSecret123xyz");

            ConfigValidationResult result = validator.validatePrivateStorage();
            String str = result.toString();
            assertThat(str).doesNotContain("AKIATOPSECRETFAKE");
            assertThat(str).doesNotContain("superFakeSecret123xyz");
            assertThat(str).doesNotContain("mybucket");
            assertThat(str).doesNotContain("s3.prod.com");
        }

        @Test
        @DisplayName("No provider URL anywhere in any validation result")
        void noProviderUrl_anywhere() {
            downloadProps.setEnabled(true);
            ConfigValidationResult result = validator.validateProviderDownload();
            assertThat(result.toString()).doesNotContain("providerUrl");
            assertThat(result.toString()).doesNotContain("providerOutputUrl");
        }
    }

    // ── F. Existing Behavior Regression ─────────────────────────────

    @Nested
    @DisplayName("F. Existing Behavior Regression")
    class ExistingBehaviorRegression {

        @Test
        @DisplayName("Provider download remains disabled by default")
        void providerDownload_disabled() {
            assertThat(new ProviderOutputDownloadProperties().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("S3 adapter remains disabled by default")
        void s3_disabled() {
            assertThat(new S3PrivateStorageProperties().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Immediate handoff remains disabled by default")
        void immediateHandoff_disabled() {
            assertThat(new ImmediateStorageHandoffProperties().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("No approval/publish behavior in validator")
        void noApprovalPublish() {
            Set<String> fields = Arrays.stream(RuntimeStorageConfigValidator.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).doesNotContain("approvalService", "publishService", "exportService");
        }
    }

    // ── G. ValidateAll Integration ──────────────────────────────────

    @Nested
    @DisplayName("G. ValidateAll Integration")
    class ValidateAllIntegration {

        @Test
        @DisplayName("ValidateAll returns exactly 3 results")
        void validateAll_three() {
            List<ConfigValidationResult> results = validator.validateAll();
            assertThat(results).hasSize(3);
        }

        @Test
        @DisplayName("ValidateAll covers all components")
        void validateAll_allComponents() {
            List<ConfigValidationResult> results = validator.validateAll();
            Set<ConfigComponent> components = results.stream()
                    .map(ConfigValidationResult::getComponent)
                    .collect(Collectors.toSet());
            assertThat(components).containsExactlyInAnyOrder(
                    ConfigComponent.PROVIDER_DOWNLOAD,
                    ConfigComponent.PRIVATE_STORAGE,
                    ConfigComponent.IMMEDIATE_HANDOFF
            );
        }

        @Test
        @DisplayName("ValidateAll with mixed valid/invalid configs")
        void validateAll_mixed() {
            downloadProps.setEnabled(true); // valid defaults
            storageProps.setEnabled(true);
            storageProps.setBucket(""); // invalid
            handoffProps.setEnabled(false); // disabled

            List<ConfigValidationResult> results = validator.validateAll();

            ConfigValidationResult download = results.stream()
                    .filter(r -> r.getComponent() == ConfigComponent.PROVIDER_DOWNLOAD).findFirst().orElseThrow();
            assertThat(download.isValid()).isTrue();
            assertThat(download.getReadiness()).isEqualTo(ConfigReadiness.READY);

            ConfigValidationResult storage = results.stream()
                    .filter(r -> r.getComponent() == ConfigComponent.PRIVATE_STORAGE).findFirst().orElseThrow();
            assertThat(storage.isValid()).isFalse();
            assertThat(storage.getReadiness()).isEqualTo(ConfigReadiness.INVALID);

            ConfigValidationResult handoff = results.stream()
                    .filter(r -> r.getComponent() == ConfigComponent.IMMEDIATE_HANDOFF).findFirst().orElseThrow();
            assertThat(handoff.isValid()).isTrue();
            assertThat(handoff.getReadiness()).isEqualTo(ConfigReadiness.DISABLED);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private void configureValidStorage() {
        storageProps.setBucket("test-bucket");
        storageProps.setEndpoint("https://s3.test.com");
        storageProps.setRegion("eu-west-1");
        storageProps.setAccessKey("AKIAFAKEKEY");
        storageProps.setSecretKey("fakesecretvalue");
    }
}
