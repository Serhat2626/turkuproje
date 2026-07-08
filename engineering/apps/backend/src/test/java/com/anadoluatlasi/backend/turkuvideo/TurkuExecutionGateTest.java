package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RPE-001 \u2014 Real Production Execution Design + Safety Gate Boundary")
class TurkuExecutionGateTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html", "audioFile", "videoFile", "mediaFile",
            "modelInput", "modelOutput", "providerPayload", "providerPrompt", "rawPrompt");

    private static final List<Class<?>> RPE001_CLASSES = List.of(
            TurkuExecutionCapability.class, TurkuExecutionGateResult.class,
            TurkuExecutionGateService.class);

    private TurkuExecutionGateService service;

    @BeforeEach void setUp() { service = new TurkuExecutionGateService(); }

    private TurkuWorkOrderPreviewResult eligibleWorkOrder() {
        var req = new TurkuNameStoryboardRequestService().createRequest(
                new TurkuNameStoryboardRequest("\u00c7anakkale T\u00fcrk\u00fcs\u00fc", "Marmara", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
        var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
        var plan = new TurkuSceneAssetPlanner().plan(draft);
        var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
        var readiness = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
        var enablement = new TurkuManualEnablementService().evaluate(readiness);
        return new TurkuWorkOrderPreviewService().generate(enablement);
    }

    // \u2500\u2500 A. Execution Not Enabled \u2500\u2500
    @Nested @DisplayName("A. Execution Not Enabled")
    class NotEnabled {
        @Test @DisplayName("eligible work order -> EXECUTION_NOT_ENABLED")
        void notEnabled() {
            var r = service.evaluate(eligibleWorkOrder());
            assertThat(r.getStatus()).isEqualTo(TurkuExecutionGateStatus.EXECUTION_NOT_ENABLED);
            assertThat(r.getOpaqueExecutionRef()).startsWith("exec:");
            assertThat(r.getSafeTurkuTitleLabel()).isEqualTo("\u00c7anakkale T\u00fcrk\u00fcs\u00fc");
        }
        @Test @DisplayName("all capabilities disabled")
        void allDisabled() {
            var r = service.evaluate(eligibleWorkOrder());
            assertThat(r.getCapabilities()).hasSizeGreaterThanOrEqualTo(8);
            for (var c : r.getCapabilities()) {
                assertThat(c.isEnabled()).isFalse();
                assertThat(c.getDisabledReason()).contains("RPE-001");
            }
        }
        @Test @DisplayName("operator gates present")
        void gates() {
            var r = service.evaluate(eligibleWorkOrder());
            assertThat(r.getRequiredOperatorGates()).hasSizeGreaterThanOrEqualTo(4);
            for (var g : r.getRequiredOperatorGates())
                assertThat(g).isInstanceOf(String.class).isNotBlank();
        }
        @Test @DisplayName("future steps are labels only")
        void labelsOnly() {
            var r = service.evaluate(eligibleWorkOrder());
            assertThat(r.getFutureExecutionStepLabels()).isNotEmpty();
            for (var s : r.getFutureExecutionStepLabels())
                assertThat(s).isInstanceOf(String.class).isNotBlank();
        }
    }

    // \u2500\u2500 B. Blocked \u2500\u2500
    @Nested @DisplayName("B. Blocked / Missing")
    class Blocked {
        @Test @DisplayName("null work order -> BLOCKED_BY_SAFETY")
        void nullWo() {
            var r = service.evaluate(null);
            assertThat(r.getStatus()).isEqualTo(TurkuExecutionGateStatus.BLOCKED_BY_SAFETY);
            assertThat(r.getBlockedReasons()).contains(TurkuExecutionGateIssueCode.WORK_ORDER_MISSING);
        }
        @Test @DisplayName("not-eligible work order -> BLOCKED_BY_SAFETY")
        void notEligible() {
            var wo = new TurkuWorkOrderPreviewService().generate(null);
            var r = service.evaluate(wo);
            assertThat(r.getStatus()).isEqualTo(TurkuExecutionGateStatus.BLOCKED_BY_SAFETY);
            assertThat(r.getBlockedReasons()).contains(TurkuExecutionGateIssueCode.WORK_ORDER_NOT_ELIGIBLE);
        }
    }

    // \u2500\u2500 C. Forbidden Sweep \u2500\u2500
    @Nested @DisplayName("C. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : RPE001_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // \u2500\u2500 D. No Forbidden Statuses \u2500\u2500
    @Nested @DisplayName("D. No COMPLETED/PUBLISHED/RENDER/GENERATED/DELIVERED")
    class NoPublish {
        @Test @DisplayName("status enum safe")
        void status() {
            var forbidden = List.of("COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
                    "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED");
            for (var v : TurkuExecutionGateStatus.values())
                for (var f : forbidden)
                    assertThat(v.name()).isNotEqualTo(f);
        }
        @Test @DisplayName("exactly 5 statuses")
        void count() { assertThat(TurkuExecutionGateStatus.values()).hasSize(5); }
        @Test @DisplayName("exactly 7 issue codes")
        void issues() { assertThat(TurkuExecutionGateIssueCode.values()).hasSize(7); }
    }

    // \u2500\u2500 E. Endpoint Unchanged \u2500\u2500
    @Nested @DisplayName("E. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // \u2500\u2500 F. ProcessBuilder / Runtime \u2500\u2500
    @Nested @DisplayName("F. ProcessBuilder / Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime")
        void clean() {
            for (var c : RPE001_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // \u2500\u2500 G. Audit Summary \u2500\u2500
    @Nested @DisplayName("G. Audit Summary Safety")
    class Audit {
        @Test @DisplayName("summary no forbidden text")
        void safe() {
            var r = service.evaluate(eligibleWorkOrder());
            for (var f : FORBIDDEN) assertThat(r.getAuditDecisionSummary()).doesNotContain(f);
        }
        @Test @DisplayName("summary mentions etkinle\u015ftirilmedi")
        void content() {
            var r = service.evaluate(eligibleWorkOrder());
            assertThat(r.getAuditDecisionSummary()).contains("etkinle\u015ftirilmedi");
        }
    }

    // \u2500\u2500 H. Capability Labels \u2500\u2500
    @Nested @DisplayName("H. Capability Labels Safety")
    class CapLabels {
        @Test @DisplayName("labels no forbidden text")
        void safe() {
            var r = service.evaluate(eligibleWorkOrder());
            for (var c : r.getCapabilities())
                for (var f : FORBIDDEN) assertThat(c.getCapabilityLabel()).doesNotContain(f);
        }
    }
}
