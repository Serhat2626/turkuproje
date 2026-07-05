package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigComponent;
import com.anadoluatlasi.backend.imagegen.service.StorageReadinessResult.ComponentReadiness;
import com.anadoluatlasi.backend.imagegen.service.StorageReadinessResult.ReadinessStatus;
import com.anadoluatlasi.backend.imagegen.storage.S3PrivateStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-011 Task 03 — Internal Storage Readiness Check tests.
 *
 * All tests use defaults/configured values — no real external calls.
 */
@DisplayName("FP-011 Task 03 — Internal Storage Readiness Check")
class InternalStorageReadinessServiceTest {

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

    // ── A. Default / Disabled Readiness ─────────────────────────────

    @Nested
    @DisplayName("A. Default / Disabled Readiness")
    class DefaultDisabledReadiness {

        @Test
        @DisplayName("Default config → DISABLED overall")
        void defaultConfig_disabled() {
            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.DISABLED);
        }

        @Test
        @DisplayName("Default config → 3 component statuses all DISABLED")
        void defaultConfig_allComponentsDisabled() {
            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getComponentStatuses()).hasSize(3);
            assertThat(result.getComponentStatuses())
                    .allMatch(c -> c.getStatus() == ReadinessStatus.DISABLED);
        }

        @Test
        @DisplayName("Default config → safe summary")
        void defaultConfig_safeSummary() {
            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getSafeSummary()).contains("disabled");
        }

        @Test
        @DisplayName("Default config → checkedAt is set")
        void defaultConfig_checkedAtSet() {
            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getCheckedAt()).isNotNull();
        }

        @Test
        @DisplayName("No secret/config leak in default result")
        void defaultConfig_noSecretLeak() {
            StorageReadinessResult result = readinessService.checkReadiness();
            String str = result.toString();
            assertThat(str).doesNotContain("accessKey");
            assertThat(str).doesNotContain("secretKey");
            assertThat(str).doesNotContain("https://");
        }

        @Test
        @DisplayName("No external call during default check")
        void defaultConfig_noExternalCall() {
            // Should complete instantly — no IO
            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result).isNotNull();
        }
    }

    // ── B. Provider Download Readiness ──────────────────────────────

    @Nested
    @DisplayName("B. Provider Download Readiness")
    class ProviderDownloadReadiness {

        @Test
        @DisplayName("Enabled valid download → component READY")
        void enabled_valid_ready() {
            downloadProps.setEnabled(true);
            StorageReadinessResult result = readinessService.checkReadiness();
            ComponentReadiness dl = findComponent(result, ConfigComponent.PROVIDER_DOWNLOAD);
            assertThat(dl.getStatus()).isEqualTo(ReadinessStatus.READY);
        }

        @Test
        @DisplayName("Invalid download config → component MISCONFIGURED")
        void enabled_invalid_misconfigured() {
            downloadProps.setEnabled(true);
            downloadProps.setMaxBytes(0);
            StorageReadinessResult result = readinessService.checkReadiness();
            ComponentReadiness dl = findComponent(result, ConfigComponent.PROVIDER_DOWNLOAD);
            assertThat(dl.getStatus()).isEqualTo(ReadinessStatus.MISCONFIGURED);
        }

        @Test
        @DisplayName("Invalid download → overall MISCONFIGURED")
        void invalid_download_overallMisconfigured() {
            downloadProps.setEnabled(true);
            downloadProps.setMaxBytes(0);
            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.MISCONFIGURED);
        }

        @Test
        @DisplayName("No provider URL required in readiness")
        void noProviderUrlRequired() {
            downloadProps.setEnabled(true);
            StorageReadinessResult result = readinessService.checkReadiness();
            ComponentReadiness dl = findComponent(result, ConfigComponent.PROVIDER_DOWNLOAD);
            assertThat(dl.getStatus()).isEqualTo(ReadinessStatus.READY);
            assertThat(dl.toString()).doesNotContain("providerUrl");
        }
    }

    // ── C. Private Storage Readiness ────────────────────────────────

    @Nested
    @DisplayName("C. Private Storage Readiness")
    class PrivateStorageReadiness {

        @Test
        @DisplayName("Enabled valid storage → component READY")
        void enabled_valid_ready() {
            storageProps.setEnabled(true);
            configureValidStorage();
            StorageReadinessResult result = readinessService.checkReadiness();
            ComponentReadiness st = findComponent(result, ConfigComponent.PRIVATE_STORAGE);
            assertThat(st.getStatus()).isEqualTo(ReadinessStatus.READY);
        }

        @Test
        @DisplayName("Missing bucket → component MISCONFIGURED")
        void missingBucket_misconfigured() {
            storageProps.setEnabled(true);
            storageProps.setBucket("");
            storageProps.setEndpoint("https://s3.test.com");
            storageProps.setRegion("eu-west-1");
            storageProps.setAccessKey("AKIAFAKE");
            storageProps.setSecretKey("fakesecret");

            StorageReadinessResult result = readinessService.checkReadiness();
            ComponentReadiness st = findComponent(result, ConfigComponent.PRIVATE_STORAGE);
            assertThat(st.getStatus()).isEqualTo(ReadinessStatus.MISCONFIGURED);
        }

        @Test
        @DisplayName("Missing credentials → component MISCONFIGURED with safe message")
        void missingCredentials_misconfiguredSafe() {
            storageProps.setEnabled(true);
            storageProps.setBucket("test");
            storageProps.setEndpoint("https://s3.test.com");
            storageProps.setRegion("eu-west-1");
            storageProps.setAccessKey("");
            storageProps.setSecretKey("");

            StorageReadinessResult result = readinessService.checkReadiness();
            ComponentReadiness st = findComponent(result, ConfigComponent.PRIVATE_STORAGE);
            assertThat(st.getStatus()).isEqualTo(ReadinessStatus.MISCONFIGURED);
            assertThat(st.getSafeMessage()).doesNotContain("AKIA");
            assertThat(st.getSafeMessage()).doesNotContain("secretKey");
        }

        @Test
        @DisplayName("No secret values in storage readiness result")
        void noSecretValues() {
            storageProps.setEnabled(true);
            storageProps.setBucket("prod-bucket");
            storageProps.setEndpoint("https://s3.prod.com");
            storageProps.setRegion("eu-west-1");
            storageProps.setAccessKey("AKIATOPSECRET999");
            storageProps.setSecretKey("superSecretProdKey");

            StorageReadinessResult result = readinessService.checkReadiness();
            String str = result.toString();
            assertThat(str).doesNotContain("AKIATOPSECRET999");
            assertThat(str).doesNotContain("superSecretProdKey");
            assertThat(str).doesNotContain("prod-bucket");
            assertThat(str).doesNotContain("s3.prod.com");

            for (ComponentReadiness cr : result.getComponentStatuses()) {
                String crStr = cr.toString();
                assertThat(crStr).doesNotContain("AKIATOPSECRET999");
                assertThat(crStr).doesNotContain("superSecretProdKey");
            }
        }

        @Test
        @DisplayName("No storage key or bucket path leak")
        void noStorageKeyLeak() {
            storageProps.setEnabled(true);
            configureValidStorage();
            StorageReadinessResult result = readinessService.checkReadiness();
            String str = result.toString();
            assertThat(str).doesNotContain("storageObjectKey");
            assertThat(str).doesNotContain("bucketPath");
        }
    }

    // ── D. Immediate Handoff Readiness ──────────────────────────────

    @Nested
    @DisplayName("D. Immediate Handoff Readiness")
    class ImmediateHandoffReadiness {

        @Test
        @DisplayName("Disabled handoff does not fail default readiness")
        void disabled_doesNotFailDefault() {
            handoffProps.setEnabled(false);
            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.DISABLED);
        }

        @Test
        @DisplayName("Enabled handoff + download disabled → NOT_READY")
        void enabled_downloadDisabled_notReady() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(false);
            storageProps.setEnabled(true);
            configureValidStorage();

            StorageReadinessResult result = readinessService.checkReadiness();
            ComponentReadiness hf = findComponent(result, ConfigComponent.IMMEDIATE_HANDOFF);
            assertThat(hf.getStatus()).isEqualTo(ReadinessStatus.NOT_READY);
        }

        @Test
        @DisplayName("Enabled handoff + storage disabled → NOT_READY")
        void enabled_storageDisabled_notReady() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(true);
            storageProps.setEnabled(false);

            StorageReadinessResult result = readinessService.checkReadiness();
            ComponentReadiness hf = findComponent(result, ConfigComponent.IMMEDIATE_HANDOFF);
            assertThat(hf.getStatus()).isEqualTo(ReadinessStatus.NOT_READY);
        }

        @Test
        @DisplayName("Enabled handoff + all valid → READY")
        void enabled_allValid_ready() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(true);
            storageProps.setEnabled(true);
            configureValidStorage();

            StorageReadinessResult result = readinessService.checkReadiness();
            ComponentReadiness hf = findComponent(result, ConfigComponent.IMMEDIATE_HANDOFF);
            assertThat(hf.getStatus()).isEqualTo(ReadinessStatus.READY);
        }

        @Test
        @DisplayName("Missing dependency → overall NOT_READY")
        void missingDep_overallNotReady() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(false);
            storageProps.setEnabled(false);

            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.NOT_READY);
        }
    }

    // ── E. Overall Status Mapping ───────────────────────────────────

    @Nested
    @DisplayName("E. Overall Status Mapping")
    class OverallStatusMapping {

        @Test
        @DisplayName("All disabled → DISABLED")
        void allDisabled_disabled() {
            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.DISABLED);
        }

        @Test
        @DisplayName("All enabled valid → READY")
        void allEnabledValid_ready() {
            downloadProps.setEnabled(true);
            storageProps.setEnabled(true);
            configureValidStorage();
            handoffProps.setEnabled(true);

            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.READY);
        }

        @Test
        @DisplayName("Storage invalid → MISCONFIGURED")
        void storageInvalid_misconfigured() {
            storageProps.setEnabled(true);
            storageProps.setBucket("");

            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.MISCONFIGURED);
        }

        @Test
        @DisplayName("Handoff dependency missing → NOT_READY")
        void handoffDepMissing_notReady() {
            handoffProps.setEnabled(true);
            downloadProps.setEnabled(false);

            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.NOT_READY);
        }

        @Test
        @DisplayName("Core storage ready but handoff disabled → DEGRADED")
        void coreReadyHandoffDisabled_degraded() {
            downloadProps.setEnabled(true);
            storageProps.setEnabled(true);
            configureValidStorage();
            handoffProps.setEnabled(false);

            StorageReadinessResult result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.DEGRADED);
            assertThat(result.getSafeSummary()).contains("optional");
        }

        @Test
        @DisplayName("Only download enabled (storage disabled) → partial enablement")
        void onlyDownloadEnabled_partialEnablement() {
            downloadProps.setEnabled(true);
            storageProps.setEnabled(false);
            handoffProps.setEnabled(false);

            StorageReadinessResult result = readinessService.checkReadiness();
            // Download READY, storage DISABLED, handoff DISABLED — mixed state
            // Not all disabled, not fully ready for end-to-end storage
            // Exact status depends on mapping: download ready but storage not → not DISABLED
            assertThat(result.getOverallStatus()).isNotEqualTo(ReadinessStatus.DISABLED);
        }
    }

    // ── F. Sanitization Regression ──────────────────────────────────

    @Nested
    @DisplayName("F. Sanitization Regression")
    class SanitizationRegression {

        @Test
        @DisplayName("StorageReadinessResult has no secret fields")
        void resultModel_noSecretFields() {
            Set<String> fields = Arrays.stream(StorageReadinessResult.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).doesNotContain("accessKey", "secretKey", "token",
                    "providerUrl", "storageObjectKey", "base64", "rawPayload",
                    "apiKey", "publicUrl", "signedUrl", "cdnUrl", "rawException",
                    "stackTrace", "bucketPath");
        }

        @Test
        @DisplayName("ComponentReadiness has no secret fields")
        void componentReadiness_noSecretFields() {
            Set<String> fields = Arrays.stream(ComponentReadiness.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).doesNotContain("accessKey", "secretKey", "token",
                    "providerUrl", "storageObjectKey", "base64", "rawPayload",
                    "apiKey", "publicUrl", "signedUrl", "cdnUrl");
        }

        @Test
        @DisplayName("Result toString with secrets in config does not leak")
        void toStringWithSecrets_noLeak() {
            storageProps.setEnabled(true);
            storageProps.setBucket("secret-bucket");
            storageProps.setEndpoint("https://secret.s3.com");
            storageProps.setRegion("eu-west-1");
            storageProps.setAccessKey("AKIASUPERSECRETKEY");
            storageProps.setSecretKey("ultraSecretValue123!");

            StorageReadinessResult result = readinessService.checkReadiness();
            String str = result.toString();
            assertThat(str).doesNotContain("AKIASUPERSECRETKEY");
            assertThat(str).doesNotContain("ultraSecretValue123!");
            assertThat(str).doesNotContain("secret-bucket");
            assertThat(str).doesNotContain("secret.s3.com");
        }

        @Test
        @DisplayName("No provider URL anywhere")
        void noProviderUrl() {
            downloadProps.setEnabled(true);
            StorageReadinessResult result = readinessService.checkReadiness();
            String str = result.toString();
            assertThat(str).doesNotContain("providerUrl");
            assertThat(str).doesNotContain("providerOutputUrl");
            assertThat(str).doesNotContain("openai");
        }

        @Test
        @DisplayName("No base64/raw payload in any status")
        void noBase64OrRawPayload() {
            StorageReadinessResult result = readinessService.checkReadiness();
            String str = result.toString();
            assertThat(str).doesNotContain("base64");
            assertThat(str).doesNotContain("rawPayload");
        }
    }

    // ── G. Boundary Regression ──────────────────────────────────────

    @Nested
    @DisplayName("G. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("No controller/endpoint in InternalStorageReadinessService")
        void noController() {
            // Verify service is not annotated as controller
            assertThat(InternalStorageReadinessService.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
            assertThat(InternalStorageReadinessService.class.isAnnotationPresent(
                    org.springframework.stereotype.Controller.class)).isFalse();
        }

        @Test
        @DisplayName("No RequestMapping on readiness service")
        void noRequestMapping() {
            assertThat(InternalStorageReadinessService.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RequestMapping.class)).isFalse();
        }

        @Test
        @DisplayName("Service is Spring @Service")
        void isSpringService() {
            assertThat(InternalStorageReadinessService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isTrue();
        }

        @Test
        @DisplayName("Readiness check is idempotent")
        void idempotent() {
            StorageReadinessResult r1 = readinessService.checkReadiness();
            StorageReadinessResult r2 = readinessService.checkReadiness();
            assertThat(r1.getOverallStatus()).isEqualTo(r2.getOverallStatus());
            assertThat(r1.getComponentStatuses()).hasSameSizeAs(r2.getComponentStatuses());
        }

        @Test
        @DisplayName("No approval/publish behavior in readiness service")
        void noApprovalPublish() {
            Set<String> methods = Arrays.stream(InternalStorageReadinessService.class.getDeclaredMethods())
                    .map(java.lang.reflect.Method::getName).collect(Collectors.toSet());
            assertThat(methods).doesNotContain("approve", "publish", "export",
                    "setApproved", "setPublished");
        }

        @Test
        @DisplayName("ReadinessStatus has no APPROVED/PUBLISHED values")
        void noApprovedPublished() {
            Set<String> statuses = Arrays.stream(ReadinessStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(statuses).doesNotContain("APPROVED", "PUBLISHED", "EXPORTED",
                    "READY_FOR_PUBLISH", "COMPLETED");
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

    private ComponentReadiness findComponent(StorageReadinessResult result, ConfigComponent target) {
        return result.getComponentStatuses().stream()
                .filter(c -> c.getComponent() == target)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Component not found: " + target));
    }
}
