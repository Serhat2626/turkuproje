package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RPE-002 \u2014 Real Image Provider Configuration Boundary")
class TurkuProviderConfigTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret", "apiKey", "credential", "password",
            "envValue", "configSecret", "credentialPath",
            "lyrics", "scriptBody", "promptText", "copyrightedContent",
            "unsafeHtml", "html", "providerPayload", "providerPrompt", "rawPrompt",
            "modelInput", "modelOutput");

    private static final List<Class<?>> RPE002_CLASSES = List.of(
            TurkuCredentialVaultBoundary.class, TurkuProviderConfigResult.class,
            TurkuProviderConfigService.class);

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED",
            "PROVIDER_ENABLED", "IMAGE_GENERATION_READY");

    private TurkuProviderConfigService service;

    @BeforeEach void setUp() { service = new TurkuProviderConfigService(); }

    private TurkuExecutionGateResult gateNotEnabled() {
        var req = new TurkuNameStoryboardRequestService().createRequest(
                new TurkuNameStoryboardRequest("\u00c7anakkale", "Marmara", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
        var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
        var plan = new TurkuSceneAssetPlanner().plan(draft);
        var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
        var r = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
        var e = new TurkuManualEnablementService().evaluate(r);
        var wo = new TurkuWorkOrderPreviewService().generate(e);
        return new TurkuExecutionGateService().evaluate(wo);
    }

    // A. Default NOT_CONFIGURED
    @Nested @DisplayName("A. Default NOT_CONFIGURED")
    class Default {
        @Test @DisplayName("gate -> NOT_CONFIGURED")
        void notConfigured() {
            var r = service.evaluate(gateNotEnabled());
            assertThat(r.getStatus()).isEqualTo(TurkuProviderConfigStatus.NOT_CONFIGURED);
            assertThat(r.getOpaqueConfigRef()).startsWith("pcfg:");
            assertThat(r.isExecutionEnabled()).isFalse();
        }
        @Test @DisplayName("provider type DETERMINISTIC_FAKE")
        void fake() {
            var r = service.evaluate(gateNotEnabled());
            assertThat(r.getProviderType()).isEqualTo(TurkuImageProviderType.DETERMINISTIC_FAKE);
        }
        @Test @DisplayName("capabilities listed")
        void caps() {
            var r = service.evaluate(gateNotEnabled());
            assertThat(r.getCapabilityLabels()).hasSizeGreaterThanOrEqualTo(4);
        }
    }

    // B. Credential Vault
    @Nested @DisplayName("B. Credential Vault Boundary")
    class Vault {
        @Test @DisplayName("vault placeholder only")
        void vaultPlaceholder() {
            var r = service.evaluate(gateNotEnabled());
            assertThat(r.getVaultBoundary()).isNotNull();
            assertThat(r.getVaultBoundary().isCredentialPresent()).isFalse();
            assertThat(r.getVaultBoundary().getVaultLabel()).isNotBlank();
            assertThat(r.getVaultBoundary().getPresenceStatusLabel()).contains("mevcut de\u011fil");
        }
    }

    // C. Selection/Fallback
    @Nested @DisplayName("C. Selection/Fallback Policy")
    class Policy {
        @Test @DisplayName("selection not configured")
        void selection() {
            var r = service.evaluate(gateNotEnabled());
            assertThat(r.getSelectionPolicySummary()).contains("yap\u0131land\u0131r\u0131lmad\u0131");
        }
        @Test @DisplayName("fallback not configured")
        void fallback() {
            var r = service.evaluate(gateNotEnabled());
            assertThat(r.getFallbackPolicySummary()).contains("yap\u0131land\u0131r\u0131lmad\u0131");
        }
    }

    // D. Blocked
    @Nested @DisplayName("D. Blocked / Missing")
    class Blocked {
        @Test @DisplayName("null gate -> BLOCKED_BY_SAFETY")
        void nullGate() {
            var r = service.evaluate(null);
            assertThat(r.getStatus()).isEqualTo(TurkuProviderConfigStatus.BLOCKED_BY_SAFETY);
            assertThat(r.getIssues()).contains(TurkuProviderConfigIssueCode.INTERNAL_ERROR);
        }
    }

    // E. Forbidden Field Sweep
    @Nested @DisplayName("E. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields across RPE-002 classes")
        void sweep() {
            for (var c : RPE002_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // F. No Forbidden Statuses
    @Nested @DisplayName("F. No Forbidden Statuses")
    class NoForbidden {
        @Test @DisplayName("config status safe")
        void configStatus() {
            for (var v : TurkuProviderConfigStatus.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("exactly 5 config statuses")
        void count() { assertThat(TurkuProviderConfigStatus.values()).hasSize(5); }
        @Test @DisplayName("exactly 6 issue codes")
        void issues() { assertThat(TurkuProviderConfigIssueCode.values()).hasSize(6); }
        @Test @DisplayName("exactly 3 provider types")
        void types() { assertThat(TurkuImageProviderType.values()).hasSize(3); }
    }

    // G. Endpoint Unchanged
    @Nested @DisplayName("G. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test @DisplayName("total unchanged")
        void total() {
            long post = count(AdminReviewController.class, PostMapping.class)
                    + count(AdminPipelineController.class, PostMapping.class)
                    + count(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + count(TurkuVideoProjectController.class, PostMapping.class);
            long get = count(AdminReviewController.class, GetMapping.class)
                    + count(AdminPipelineController.class, GetMapping.class)
                    + count(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + count(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post + get).isEqualTo(46);
        }
        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }

    // H. ProcessBuilder / Runtime
    @Nested @DisplayName("H. ProcessBuilder / Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime")
        void clean() {
            for (var c : RPE002_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // I. Safety Validation Summary
    @Nested @DisplayName("I. Safety Validation Summary")
    class Safety {
        @Test @DisplayName("summary no forbidden text")
        void safe() {
            var r = service.evaluate(gateNotEnabled());
            for (var f : FORBIDDEN)
                assertThat(r.getSafetyValidationSummary()).doesNotContain(f);
        }
        @Test @DisplayName("summary mentions devre d\u0131\u015f\u0131")
        void content() {
            var r = service.evaluate(gateNotEnabled());
            assertThat(r.getSafetyValidationSummary()).contains("devre d\u0131\u015f\u0131");
        }
    }
}
