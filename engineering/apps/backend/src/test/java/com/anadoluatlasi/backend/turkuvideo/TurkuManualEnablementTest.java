package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MP-001 \u2014 Manual Production Enablement Design + Safety Boundary")
class TurkuManualEnablementTest {

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

    private static final List<Class<?>> MP001_CLASSES = List.of(
            TurkuManualEnablementResult.class, TurkuManualEnablementService.class);

    private TurkuManualEnablementService service;

    @BeforeEach void setUp() { service = new TurkuManualEnablementService(); }

    private TurkuMvpReadinessResult readyReadiness() {
        var reqSvc = new TurkuNameStoryboardRequestService();
        var req = reqSvc.createRequest(new TurkuNameStoryboardRequest(
                "\u00c7anakkale T\u00fcrk\u00fcs\u00fc", "Marmara", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
        var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
        var plan = new TurkuSceneAssetPlanner().plan(draft);
        var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
        return new TurkuMvpReadinessService().assess(req, draft, plan, tl);
    }

    private TurkuMvpReadinessResult notReadyReadiness() {
        return new TurkuMvpReadinessService().assess(null, null, null, null);
    }

    // \u2500\u2500 A. Eligible \u2500\u2500
    @Nested @DisplayName("A. Eligible for Manual Prep")
    class Eligible {
        @Test @DisplayName("ready readiness -> ELIGIBLE_FOR_MANUAL_PREP")
        void eligible() {
            var r = service.evaluate(readyReadiness());
            assertThat(r.getStatus()).isEqualTo(TurkuManualEnablementStatus.ELIGIBLE_FOR_MANUAL_PREP);
            assertThat(r.getBlockedReasons()).isEmpty();
            assertThat(r.getSafeTurkuTitleLabel()).isEqualTo("\u00c7anakkale T\u00fcrk\u00fcs\u00fc");
            assertThat(r.getOperatorSummary()).contains("uygun");
        }
        @Test @DisplayName("manual steps are label strings")
        void labelsOnly() {
            var r = service.evaluate(readyReadiness());
            assertThat(r.getManualStepLabels()).isNotEmpty();
            for (var s : r.getManualStepLabels())
                assertThat(s).isInstanceOf(String.class).isNotBlank();
        }
        @Test @DisplayName("safety prerequisites present")
        void prereqs() {
            var r = service.evaluate(readyReadiness());
            assertThat(r.getSafetyPrerequisites()).hasSizeGreaterThanOrEqualTo(4);
        }
        @Test @DisplayName("all checklist true")
        void checklist() {
            var r = service.evaluate(readyReadiness());
            assertThat(r.isCopyrightSafeIntentPresent()).isTrue();
            assertThat(r.isAllScenesPlanned()).isTrue();
            assertThat(r.isTimelineComplete()).isTrue();
            assertThat(r.isNoBlockedScenes()).isTrue();
        }
    }

    // \u2500\u2500 B. Not Eligible \u2500\u2500
    @Nested @DisplayName("B. Not Eligible / Blocked")
    class NotEligible {
        @Test @DisplayName("null readiness -> NOT_ELIGIBLE")
        void nullReadiness() {
            var r = service.evaluate(null);
            assertThat(r.getStatus()).isEqualTo(TurkuManualEnablementStatus.NOT_ELIGIBLE);
            assertThat(r.getBlockedReasons()).contains(TurkuManualEnablementIssueCode.READINESS_MISSING);
        }
        @Test @DisplayName("not-ready readiness -> MANUAL_REVIEW_REQUIRED")
        void notReady() {
            var r = service.evaluate(notReadyReadiness());
            assertThat(r.getStatus()).isIn(
                    TurkuManualEnablementStatus.MANUAL_REVIEW_REQUIRED,
                    TurkuManualEnablementStatus.BLOCKED_BY_SAFETY);
        }
    }

    // \u2500\u2500 C. Forbidden Sweep \u2500\u2500
    @Nested @DisplayName("C. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : MP001_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // \u2500\u2500 D. No Publish/Final Statuses \u2500\u2500
    @Nested @DisplayName("D. No COMPLETED/PUBLISHED/RENDER/PUBLISH")
    class NoPublish {
        @Test @DisplayName("status enum safe")
        void status() {
            for (var v : TurkuManualEnablementStatus.values())
                assertThat(v.name()).doesNotContain("COMPLETED").doesNotContain("PUBLISHED")
                        .doesNotContain("AUTO_APPROVED").doesNotContain("READY_FOR_PUBLISH")
                        .doesNotContain("PUBLICLY_AVAILABLE").doesNotContain("READY_FOR_RENDER");
        }
        @Test @DisplayName("exactly 5 statuses")
        void count() { assertThat(TurkuManualEnablementStatus.values()).hasSize(5); }
        @Test @DisplayName("exactly 8 issue codes")
        void issues() { assertThat(TurkuManualEnablementIssueCode.values()).hasSize(8); }
        @Test @DisplayName("exactly 6 manual step types")
        void steps() { assertThat(TurkuManualStepType.values()).hasSize(6); }
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
            for (var c : MP001_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // \u2500\u2500 G. Manual Step Labels Content \u2500\u2500
    @Nested @DisplayName("G. Manual Step Labels Content")
    class StepLabels {
        @Test @DisplayName("eligible labels mention manual actions")
        void eligibleLabels() {
            var r = service.evaluate(readyReadiness());
            String all = String.join(" ", r.getManualStepLabels());
            assertThat(all).doesNotContainIgnoringCase("auto");
            assertThat(all).doesNotContainIgnoringCase("execute");
        }
        @Test @DisplayName("step type enum labels are non-executable")
        void stepTypes() {
            for (var t : TurkuManualStepType.values())
                assertThat(t.name()).doesNotContain("EXECUTE").doesNotContain("AUTO")
                        .doesNotContain("PUBLISH").doesNotContain("DEPLOY");
        }
    }

    // \u2500\u2500 H. Operator Summary Safety \u2500\u2500
    @Nested @DisplayName("H. Operator Summary Safety")
    class SummarySafety {
        @Test @DisplayName("summary no forbidden text")
        void safe() {
            var r = service.evaluate(readyReadiness());
            String summary = r.getOperatorSummary();
            for (var f : FORBIDDEN)
                assertThat(summary).doesNotContain(f);
        }
    }
}
