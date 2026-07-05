package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigComponent;
import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigReadiness;
import com.anadoluatlasi.backend.imagegen.service.ConfigValidationResult.ConfigSeverity;
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
 * FP-011 Task 05 — Closure Candidate Regression Tests.
 *
 * Final regression coverage for FP-011 closure readiness.
 */
@DisplayName("FP-011 Task 05 — Closure Candidate Regression")
class Fp011ClosureCandidateRegressionTest {

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

    // ── A. Component Model Completeness ─────────────────────────────

    @Nested
    @DisplayName("A. Component Model Completeness")
    class ComponentModelCompleteness {

        @Test
        @DisplayName("ConfigComponent has exactly 3 values")
        void configComponent_threeValues() {
            assertThat(ConfigComponent.values()).hasSize(3);
            assertThat(ConfigComponent.values()).containsExactlyInAnyOrder(
                    ConfigComponent.PROVIDER_DOWNLOAD,
                    ConfigComponent.PRIVATE_STORAGE,
                    ConfigComponent.IMMEDIATE_HANDOFF
            );
        }

        @Test
        @DisplayName("ConfigReadiness has exactly 4 values")
        void configReadiness_fourValues() {
            assertThat(ConfigReadiness.values()).hasSize(4);
        }

        @Test
        @DisplayName("ConfigSeverity has exactly 3 values")
        void configSeverity_threeValues() {
            assertThat(ConfigSeverity.values()).hasSize(3);
        }

        @Test
        @DisplayName("ReadinessStatus has exactly 5 values")
        void readinessStatus_fiveValues() {
            assertThat(ReadinessStatus.values()).hasSize(5);
            assertThat(ReadinessStatus.values()).containsExactlyInAnyOrder(
                    ReadinessStatus.READY,
                    ReadinessStatus.DISABLED,
                    ReadinessStatus.NOT_READY,
                    ReadinessStatus.MISCONFIGURED,
                    ReadinessStatus.DEGRADED
            );
        }
    }

    // ── B. No Forbidden Status/Enum Values ──────────────────────────

    @Nested
    @DisplayName("B. No Forbidden Status/Enum Values")
    class NoForbiddenStatusValues {

        @Test
        @DisplayName("No READY_FOR_PUBLISH in any enum")
        void noReadyForPublish() {
            assertNoEnumValue("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("No COMPLETED in any enum")
        void noCompleted() {
            assertNoEnumValue("COMPLETED");
        }

        @Test
        @DisplayName("No PUBLISHED in any enum")
        void noPublished() {
            assertNoEnumValue("PUBLISHED");
        }

        @Test
        @DisplayName("No EXPORTED in any enum")
        void noExported() {
            assertNoEnumValue("EXPORTED");
        }

        @Test
        @DisplayName("No APPROVED in any enum")
        void noApproved() {
            assertNoEnumValue("APPROVED");
        }

        private void assertNoEnumValue(String forbidden) {
            Set<String> all = new java.util.HashSet<>();
            Arrays.stream(ConfigComponent.values()).forEach(v -> all.add(v.name()));
            Arrays.stream(ConfigReadiness.values()).forEach(v -> all.add(v.name()));
            Arrays.stream(ConfigSeverity.values()).forEach(v -> all.add(v.name()));
            Arrays.stream(ReadinessStatus.values()).forEach(v -> all.add(v.name()));
            assertThat(all).doesNotContain(forbidden);
        }
    }

    // ── C. No Controller/Endpoint Regression ────────────────────────

    @Nested
    @DisplayName("C. No Controller/Endpoint Regression")
    class NoControllerEndpoint {

        @Test
        @DisplayName("RuntimeStorageConfigValidator is not a controller")
        void validator_notController() {
            assertNotController(RuntimeStorageConfigValidator.class);
        }

        @Test
        @DisplayName("InternalStorageReadinessService is not a controller")
        void readiness_notController() {
            assertNotController(InternalStorageReadinessService.class);
        }

        @Test
        @DisplayName("ConfigValidationResult is not a controller")
        void validationResult_notController() {
            assertNotController(ConfigValidationResult.class);
        }

        @Test
        @DisplayName("StorageReadinessResult is not a controller")
        void readinessResult_notController() {
            assertNotController(StorageReadinessResult.class);
        }

        private void assertNotController(Class<?> clazz) {
            assertThat(clazz.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
            assertThat(clazz.isAnnotationPresent(
                    org.springframework.stereotype.Controller.class)).isFalse();
            assertThat(clazz.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RequestMapping.class)).isFalse();
        }
    }

    // ── D. Model Field Safety ───────────────────────────────────────

    @Nested
    @DisplayName("D. Model Field Safety")
    class ModelFieldSafety {

        private static final Set<String> FORBIDDEN_FIELDS = Set.of(
                "accessKey", "secretKey", "token", "apiKey",
                "providerUrl", "providerOutputUrl",
                "storageObjectKey", "objectKey", "bucketPath",
                "publicUrl", "signedUrl", "cdnUrl",
                "base64", "rawPayload", "rawException", "stackTrace",
                "rawConfigValue"
        );

        @Test
        @DisplayName("ConfigValidationResult has no forbidden fields")
        void configValidationResult_safe() {
            assertNoForbiddenFields(ConfigValidationResult.class);
        }

        @Test
        @DisplayName("StorageReadinessResult has no forbidden fields")
        void storageReadinessResult_safe() {
            assertNoForbiddenFields(StorageReadinessResult.class);
        }

        @Test
        @DisplayName("StorageReadinessResult.ComponentReadiness has no forbidden fields")
        void componentReadiness_safe() {
            assertNoForbiddenFields(StorageReadinessResult.ComponentReadiness.class);
        }

        private void assertNoForbiddenFields(Class<?> clazz) {
            Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Class %s must not have field: %s", clazz.getSimpleName(), forbidden)
                        .doesNotContain(forbidden);
            }
        }
    }

    // ── E. Default Config Safety ────────────────────────────────────

    @Nested
    @DisplayName("E. Default Config Safety")
    class DefaultConfigSafety {

        @Test
        @DisplayName("All flags default to false")
        void allFlagsDefaultFalse() {
            assertThat(downloadProps.isEnabled()).isFalse();
            assertThat(storageProps.isEnabled()).isFalse();
            assertThat(handoffProps.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Default validation is all valid/disabled")
        void defaultValidation_allValid() {
            var results = configValidator.validateAll();
            assertThat(results).allMatch(ConfigValidationResult::isValid);
            assertThat(results).allMatch(r -> r.getReadiness() == ConfigReadiness.DISABLED);
        }

        @Test
        @DisplayName("Default readiness is DISABLED")
        void defaultReadiness_disabled() {
            var result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.DISABLED);
        }

        @Test
        @DisplayName("S3PrivateStorageProperties toString excludes secrets")
        void s3Props_toStringExcludesSecrets() {
            storageProps.setAccessKey("AKIASUPERSECRETFINAL");
            storageProps.setSecretKey("ultraFinalSecret999");
            String str = storageProps.toString();
            assertThat(str).doesNotContain("AKIASUPERSECRETFINAL");
            assertThat(str).doesNotContain("ultraFinalSecret999");
        }
    }

    // ── F. End-to-End Safety Sweep ──────────────────────────────────

    @Nested
    @DisplayName("F. End-to-End Safety Sweep")
    class EndToEndSafetySweep {

        @Test
        @DisplayName("Full validation + readiness with secrets produces no leaks")
        void fullFlow_noLeaks() {
            downloadProps.setEnabled(true);
            storageProps.setEnabled(true);
            storageProps.setBucket("final-secret-bucket");
            storageProps.setEndpoint("https://final.secret.endpoint.com");
            storageProps.setRegion("eu-west-1");
            storageProps.setAccessKey("AKIAFINALTEST123");
            storageProps.setSecretKey("finalTestSecretXYZ");
            handoffProps.setEnabled(true);

            // Run full validation
            var validationResults = configValidator.validateAll();
            for (var r : validationResults) {
                String str = r.toString();
                assertThat(str).doesNotContain("AKIAFINALTEST123");
                assertThat(str).doesNotContain("finalTestSecretXYZ");
                assertThat(str).doesNotContain("final-secret-bucket");
                assertThat(str).doesNotContain("final.secret.endpoint");
            }

            // Run full readiness
            var readiness = readinessService.checkReadiness();
            String rStr = readiness.toString();
            assertThat(rStr).doesNotContain("AKIAFINALTEST123");
            assertThat(rStr).doesNotContain("finalTestSecretXYZ");
            assertThat(rStr).doesNotContain("final-secret-bucket");

            for (var cr : readiness.getComponentStatuses()) {
                String crStr = cr.toString();
                assertThat(crStr).doesNotContain("AKIAFINALTEST123");
                assertThat(crStr).doesNotContain("finalTestSecretXYZ");
            }
        }

        @Test
        @DisplayName("No real external call in full flow")
        void noExternalCall_fullFlow() {
            downloadProps.setEnabled(true);
            storageProps.setEnabled(true);
            storageProps.setBucket("b");
            storageProps.setEndpoint("https://e");
            storageProps.setRegion("r");
            storageProps.setAccessKey("k");
            storageProps.setSecretKey("s");
            handoffProps.setEnabled(true);

            // Must complete instantly — no network
            var result = readinessService.checkReadiness();
            assertThat(result.getOverallStatus()).isEqualTo(ReadinessStatus.READY);
        }
    }
}
