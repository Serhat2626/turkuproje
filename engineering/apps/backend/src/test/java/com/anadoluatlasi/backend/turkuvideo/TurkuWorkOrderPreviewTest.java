package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MP-002 \u2014 Manual Production Checklist + Gated Work Order Preview")
class TurkuWorkOrderPreviewTest {

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

    private static final List<Class<?>> MP002_CLASSES = List.of(
            TurkuProductionChecklistItem.class,
            TurkuWorkOrderPreviewResult.class,
            TurkuWorkOrderPreviewService.class);

    private TurkuWorkOrderPreviewService service;

    @BeforeEach void setUp() { service = new TurkuWorkOrderPreviewService(); }

    private TurkuManualEnablementResult eligibleEnablement() {
        var reqSvc = new TurkuNameStoryboardRequestService();
        var req = reqSvc.createRequest(new TurkuNameStoryboardRequest(
                "\u00c7anakkale T\u00fcrk\u00fcs\u00fc", "Marmara", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
        var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
        var plan = new TurkuSceneAssetPlanner().plan(draft);
        var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
        var readiness = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
        return new TurkuManualEnablementService().evaluate(readiness);
    }

    private TurkuManualEnablementResult notEligibleEnablement() {
        return new TurkuManualEnablementService().evaluate(null);
    }

    // \u2500\u2500 A. Eligible Work Order \u2500\u2500
    @Nested @DisplayName("A. Eligible Work Order Preview")
    class Eligible {
        @Test @DisplayName("eligible enablement -> ELIGIBLE_FOR_MANUAL_PREPARATION")
        void eligible() {
            var r = service.generate(eligibleEnablement());
            assertThat(r.getStatus()).isEqualTo(TurkuWorkOrderPreviewStatus.ELIGIBLE_FOR_MANUAL_PREPARATION);
            assertThat(r.getBlockedReasons()).isEmpty();
            assertThat(r.getOpaqueWorkOrderRef()).startsWith("wo:");
            assertThat(r.getSafeTurkuTitleLabel()).isEqualTo("\u00c7anakkale T\u00fcrk\u00fcs\u00fc");
        }
        @Test @DisplayName("checklist has items")
        void checklist() {
            var r = service.generate(eligibleEnablement());
            assertThat(r.getChecklistItems()).hasSizeGreaterThanOrEqualTo(6);
            assertThat(r.getTotalItems()).isGreaterThanOrEqualTo(6);
        }
        @Test @DisplayName("all items met")
        void allMet() {
            var r = service.generate(eligibleEnablement());
            assertThat(r.getCompletedItems()).isEqualTo(r.getTotalItems());
            assertThat(r.getBlockedItems()).isEqualTo(0);
            for (var item : r.getChecklistItems()) {
                assertThat(item.isPrerequisiteMet()).isTrue();
                assertThat(item.getBlockedReason()).isNull();
            }
        }
        @Test @DisplayName("work order steps are labels")
        void labelsOnly() {
            var r = service.generate(eligibleEnablement());
            assertThat(r.getWorkOrderStepLabels()).isNotEmpty();
            for (var s : r.getWorkOrderStepLabels())
                assertThat(s).isInstanceOf(String.class).isNotBlank();
        }
    }

    // \u2500\u2500 B. Blocked \u2500\u2500
    @Nested @DisplayName("B. Blocked / Not Eligible")
    class Blocked {
        @Test @DisplayName("null enablement -> BLOCKED_BY_SAFETY")
        void nullEnablement() {
            var r = service.generate(null);
            assertThat(r.getStatus()).isEqualTo(TurkuWorkOrderPreviewStatus.BLOCKED_BY_SAFETY);
            assertThat(r.getBlockedReasons()).contains(TurkuWorkOrderPreviewIssueCode.ENABLEMENT_MISSING);
        }
        @Test @DisplayName("not-eligible enablement -> BLOCKED_BY_SAFETY")
        void notEligible() {
            var r = service.generate(notEligibleEnablement());
            assertThat(r.getStatus()).isEqualTo(TurkuWorkOrderPreviewStatus.BLOCKED_BY_SAFETY);
            assertThat(r.getBlockedReasons()).contains(TurkuWorkOrderPreviewIssueCode.ENABLEMENT_NOT_ELIGIBLE);
        }
    }

    // \u2500\u2500 C. Checklist Instructions \u2500\u2500
    @Nested @DisplayName("C. Checklist Instructions Safety")
    class Instructions {
        @Test @DisplayName("instructions are Turkish safe text")
        void safeTurkish() {
            var r = service.generate(eligibleEnablement());
            for (var item : r.getChecklistItems()) {
                assertThat(item.getLabel()).isNotBlank();
                assertThat(item.getInstruction()).isNotBlank();
                for (var f : FORBIDDEN)
                    assertThat(item.getInstruction()).doesNotContain(f);
            }
        }
    }

    // \u2500\u2500 D. Forbidden Sweep \u2500\u2500
    @Nested @DisplayName("D. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : MP002_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // \u2500\u2500 E. No Publish/Final Statuses \u2500\u2500
    @Nested @DisplayName("E. No COMPLETED/PUBLISHED/RENDER/GENERATED")
    class NoPublish {
        @Test @DisplayName("status enum safe")
        void status() {
            for (var v : TurkuWorkOrderPreviewStatus.values())
                assertThat(v.name()).doesNotContain("COMPLETED").doesNotContain("PUBLISHED")
                        .doesNotContain("AUTO_APPROVED").doesNotContain("READY_FOR_PUBLISH")
                        .doesNotContain("PUBLICLY_AVAILABLE").doesNotContain("READY_FOR_RENDER")
                        .doesNotContain("RENDERING").doesNotContain("GENERATED").doesNotContain("DELIVERED");
        }
        @Test @DisplayName("exactly 5 statuses")
        void count() { assertThat(TurkuWorkOrderPreviewStatus.values()).hasSize(5); }
        @Test @DisplayName("exactly 6 issue codes")
        void issues() { assertThat(TurkuWorkOrderPreviewIssueCode.values()).hasSize(6); }
    }

    // \u2500\u2500 F. Endpoint Unchanged \u2500\u2500
    @Nested @DisplayName("F. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // \u2500\u2500 G. ProcessBuilder / Runtime \u2500\u2500
    @Nested @DisplayName("G. ProcessBuilder / Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime")
        void clean() {
            for (var c : MP002_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // \u2500\u2500 H. Operator Summary \u2500\u2500
    @Nested @DisplayName("H. Operator Summary Safety")
    class Summary {
        @Test @DisplayName("summary no forbidden text")
        void safe() {
            var r = service.generate(eligibleEnablement());
            for (var f : FORBIDDEN) assertThat(r.getOperatorSummary()).doesNotContain(f);
        }
        @Test @DisplayName("summary contains tamamland\u0131")
        void content() {
            var r = service.generate(eligibleEnablement());
            assertThat(r.getOperatorSummary()).contains("tamamland");
        }
    }
}
