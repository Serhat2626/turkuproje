package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RPE-004 — Execution Package + Manual Gate Boundary")
class TurkuExecutionPackageTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret", "apiKey", "credential", "password",
            "lyrics", "scriptBody", "promptText", "copyrightedContent",
            "unsafeHtml", "html", "providerPayload", "providerPrompt", "rawPrompt");

    private static final List<Class<?>> RPE004_CLASSES = List.of(
            TurkuManualGate.class, TurkuExecutionPackageResult.class,
            TurkuExecutionPackageService.class);

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED",
            "PROVIDER_ENABLED", "IMAGE_GENERATION_READY");

    private TurkuExecutionPackageService service;

    @BeforeEach void setUp() { service = new TurkuExecutionPackageService(); }

    private TurkuWorkOrderPreviewResult eligibleWorkOrder() {
        var req = new TurkuNameStoryboardRequestService().createRequest(
                new TurkuNameStoryboardRequest("Canakkale Turkusu", "Marmara", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
        var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
        var plan = new TurkuSceneAssetPlanner().plan(draft);
        var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
        var r = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
        var e = new TurkuManualEnablementService().evaluate(r);
        return new TurkuWorkOrderPreviewService().generate(e);
    }

    private TurkuProviderConfigResult providerConfig() {
        var gate = new TurkuExecutionGateService().evaluate(eligibleWorkOrder());
        return new TurkuProviderConfigService().evaluate(gate);
    }

    private TurkuRenderPipelineConfigResult renderConfig() {
        return new TurkuRenderPipelineConfigService().evaluate(providerConfig());
    }

    // A. Full Chain Assembly
    @Nested @DisplayName("A. Full Chain Assembly")
    class FullChain {
        @Test @DisplayName("assembles WAITING_FOR_MANUAL_GATE")
        void assembles() {
            var r = service.assemble(eligibleWorkOrder(), providerConfig(), renderConfig());
            assertThat(r.getStatus()).isEqualTo(TurkuExecutionPackageStatus.WAITING_FOR_MANUAL_GATE);
            assertThat(r.getOpaquePackageRef()).startsWith("epkg:");
            assertThat(r.getSafeTurkuTitleLabel()).isEqualTo("Canakkale Turkusu");
        }
        @Test @DisplayName("provider summary present")
        void provider() {
            var r = service.assemble(eligibleWorkOrder(), providerConfig(), renderConfig());
            assertThat(r.getProviderConfigSummary()).contains("NOT_CONFIGURED");
        }
        @Test @DisplayName("render summary present")
        void render() {
            var r = service.assemble(eligibleWorkOrder(), providerConfig(), renderConfig());
            assertThat(r.getRenderConfigSummary()).contains("NOT_CONFIGURED");
        }
    }

    // B. All Capabilities Disabled
    @Nested @DisplayName("B. All Capabilities Disabled")
    class Disabled {
        @Test @DisplayName("8 capabilities all disabled")
        void allDisabled() {
            var r = service.assemble(eligibleWorkOrder(), providerConfig(), renderConfig());
            assertThat(r.getExecutionCapabilities()).hasSize(8);
            for (var c : r.getExecutionCapabilities()) {
                assertThat(c.isEnabled()).isFalse();
                assertThat(c.getDisabledReason()).contains("RPE-004");
            }
        }
    }

    // C. Manual Gates
    @Nested @DisplayName("C. Manual Gates")
    class Gates {
        @Test @DisplayName("5 gates all not passed")
        void gates() {
            var r = service.assemble(eligibleWorkOrder(), providerConfig(), renderConfig());
            assertThat(r.getManualGates()).hasSize(5);
            for (var g : r.getManualGates()) {
                assertThat(g.isPassed()).isFalse();
                assertThat(g.getGateLabel()).isNotBlank();
                assertThat(g.getInstruction()).isNotBlank();
            }
        }
    }

    // D. Future Steps Labels
    @Nested @DisplayName("D. Future Steps Labels")
    class Steps {
        @Test @DisplayName("5 steps labels only")
        void labels() {
            var r = service.assemble(eligibleWorkOrder(), providerConfig(), renderConfig());
            assertThat(r.getFutureExecutionStepLabels()).hasSize(5);
            for (var s : r.getFutureExecutionStepLabels())
                assertThat(s).isInstanceOf(String.class).isNotBlank();
        }
    }

    // E. Blocked
    @Nested @DisplayName("E. Blocked / Missing")
    class Blocked {
        @Test @DisplayName("null work order -> BLOCKED_BY_SAFETY")
        void nullWo() {
            var r = service.assemble(null, providerConfig(), renderConfig());
            assertThat(r.getStatus()).isEqualTo(TurkuExecutionPackageStatus.BLOCKED_BY_SAFETY);
            assertThat(r.getBlockedReasons()).contains(TurkuExecutionPackageIssueCode.WORK_ORDER_NOT_ELIGIBLE);
        }
        @Test @DisplayName("null provider -> BLOCKED_BY_SAFETY")
        void nullProvider() {
            var r = service.assemble(eligibleWorkOrder(), null, renderConfig());
            assertThat(r.getStatus()).isEqualTo(TurkuExecutionPackageStatus.BLOCKED_BY_SAFETY);
        }
        @Test @DisplayName("null render -> BLOCKED_BY_SAFETY")
        void nullRender() {
            var r = service.assemble(eligibleWorkOrder(), providerConfig(), null);
            assertThat(r.getStatus()).isEqualTo(TurkuExecutionPackageStatus.BLOCKED_BY_SAFETY);
        }
    }

    // F. Forbidden Sweep
    @Nested @DisplayName("F. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : RPE004_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // G. No Forbidden Statuses
    @Nested @DisplayName("G. No Forbidden Statuses")
    class NoForbidden {
        @Test @DisplayName("package status safe")
        void safe() {
            for (var v : TurkuExecutionPackageStatus.values())
                for (var f : FORBIDDEN_STATUSES)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("exactly 5 statuses")
        void count() { assertThat(TurkuExecutionPackageStatus.values()).hasSize(5); }
        @Test @DisplayName("exactly 6 issue codes")
        void issues() { assertThat(TurkuExecutionPackageIssueCode.values()).hasSize(6); }
    }

    // H. Endpoint Unchanged
    @Nested @DisplayName("H. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // I. ProcessBuilder / Runtime
    @Nested @DisplayName("I. ProcessBuilder / Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime")
        void clean() {
            for (var c : RPE004_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // J. Operator Summary Safety
    @Nested @DisplayName("J. Operator Summary Safety")
    class Summary {
        @Test @DisplayName("summary no forbidden text")
        void safe() {
            var r = service.assemble(eligibleWorkOrder(), providerConfig(), renderConfig());
            for (var f : FORBIDDEN) assertThat(r.getOperatorExecutionSummary()).doesNotContain(f);
        }
    }
}
