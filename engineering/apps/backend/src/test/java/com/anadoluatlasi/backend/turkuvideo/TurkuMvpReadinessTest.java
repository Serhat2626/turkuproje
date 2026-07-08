package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RP-005 \u2014 Admin Story-to-Video MVP Readiness Package")
class TurkuMvpReadinessTest {

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

    private static final List<Class<?>> RP005_CLASSES = List.of(
            TurkuMvpReadinessResult.class,
            TurkuMvpReadinessService.class);

    private TurkuMvpReadinessService service;

    @BeforeEach void setUp() { service = new TurkuMvpReadinessService(); }

    private TurkuNameStoryboardRequestResult validRequest() {
        return new TurkuNameStoryboardRequestService().createRequest(
                new TurkuNameStoryboardRequest("\u00c7anakkale T\u00fcrk\u00fcs\u00fc", "Marmara", "HALK", "CINEMATIC", 4, 40_000, "MP4", 1920, 1080, 24));
    }
    private TurkuStoryboardDraftResult validDraft() {
        return new TurkuStoryboardDraftOrchestrator().orchestrate(validRequest());
    }
    private TurkuSceneAssetPlanResult validAssetPlan() {
        return new TurkuSceneAssetPlanner().plan(validDraft());
    }
    private TurkuAnimaticTimelinePlanResult validTimeline() {
        return new TurkuAnimaticTimelinePlanner().plan(validAssetPlan());
    }

    // \u2500\u2500 A. Full Chain Ready \u2500\u2500
    @Nested @DisplayName("A. Full Chain Ready")
    class FullChain {
        @Test @DisplayName("all components ready -> READY_FOR_MANUAL_REVIEW")
        void ready() {
            var r = service.assess(validRequest(), validDraft(), validAssetPlan(), validTimeline());
            assertThat(r.getStatus()).isEqualTo(TurkuMvpReadinessStatus.READY_FOR_MANUAL_REVIEW);
            assertThat(r.getBlockingIssues()).isEmpty();
            assertThat(r.getSafeTurkuTitleLabel()).isEqualTo("\u00c7anakkale T\u00fcrk\u00fcs\u00fc");
            assertThat(r.isCopyrightSafeIntentPresent()).isTrue();
            assertThat(r.isAllScenesPlanned()).isTrue();
            assertThat(r.isTimelineComplete()).isTrue();
            assertThat(r.isNoBlockedScenes()).isTrue();
        }
        @Test @DisplayName("next steps are labels only")
        void labels() {
            var r = service.assess(validRequest(), validDraft(), validAssetPlan(), validTimeline());
            assertThat(r.getNextAllowedManualStepLabels()).isNotEmpty();
            for (var label : r.getNextAllowedManualStepLabels())
                assertThat(label).isInstanceOf(String.class).isNotBlank();
        }
        @Test @DisplayName("summaries populated")
        void summaries() {
            var r = service.assess(validRequest(), validDraft(), validAssetPlan(), validTimeline());
            assertThat(r.getStoryboardReadinessSummary()).contains("sahne");
            assertThat(r.getAssetPlanReadinessSummary()).contains("planlanm");
            assertThat(r.getTimelineReadinessSummary()).contains("toplam");
        }
    }

    // \u2500\u2500 B. Missing Components \u2500\u2500
    @Nested @DisplayName("B. Missing Components")
    class Missing {
        @Test @DisplayName("null request -> NOT_READY")
        void noReq() {
            var r = service.assess(null, validDraft(), validAssetPlan(), validTimeline());
            assertThat(r.getStatus()).isEqualTo(TurkuMvpReadinessStatus.NOT_READY);
            assertThat(r.getBlockingIssues()).contains(TurkuMvpReadinessIssueCode.REQUEST_MISSING);
        }
        @Test @DisplayName("null storyboard -> NOT_READY")
        void noDraft() {
            var r = service.assess(validRequest(), null, validAssetPlan(), validTimeline());
            assertThat(r.getBlockingIssues()).contains(TurkuMvpReadinessIssueCode.STORYBOARD_MISSING);
        }
        @Test @DisplayName("null asset plan -> NOT_READY")
        void noPlan() {
            var r = service.assess(validRequest(), validDraft(), null, validTimeline());
            assertThat(r.getBlockingIssues()).contains(TurkuMvpReadinessIssueCode.ASSET_PLAN_MISSING);
        }
        @Test @DisplayName("null timeline -> NOT_READY")
        void noTl() {
            var r = service.assess(validRequest(), validDraft(), validAssetPlan(), null);
            assertThat(r.getBlockingIssues()).contains(TurkuMvpReadinessIssueCode.TIMELINE_MISSING);
        }
    }

    // \u2500\u2500 C. Blocked Scenes \u2500\u2500
    @Nested @DisplayName("C. Blocked Scenes")
    class Blocked {
        @Test @DisplayName("blocked scenes -> BLOCKED_BY_SAFETY or PARTIALLY_READY")
        void blockedScenes() {
            var entry = new TurkuSceneAssetPlanEntry(1, TurkuSceneAssetPlanStatus.BLOCKED_CONTENT_UNSAFE,
                    List.of(), null, null, null, null, null, "BLOCKED", "test");
            var safeEntry = new TurkuSceneAssetPlanEntry(2, TurkuSceneAssetPlanStatus.READY_FOR_ASSET_GENERATION_BOUNDARY,
                    List.of(), "brief", "WIDE", "frame", "atm", "style", "SAFE", null);
            var plan = new TurkuSceneAssetPlanResult(
                    TurkuSceneAssetPlanStatus.VISUAL_BRIEF_GENERATED, List.of(),
                    "p:1", "d:1", "Test", "FAKE", List.of(entry, safeEntry), 2, 1);
            var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
            var r = service.assess(validRequest(), validDraft(), plan, tl);
            assertThat(r.getBlockingIssues()).contains(TurkuMvpReadinessIssueCode.ASSET_PLAN_HAS_BLOCKED_SCENES);
            assertThat(r.isNoBlockedScenes()).isFalse();
        }
    }

    // \u2500\u2500 D. Forbidden Sweep \u2500\u2500
    @Nested @DisplayName("D. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields") void sweep() {
            for (var c : RP005_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // \u2500\u2500 E. No Publish Statuses \u2500\u2500
    @Nested @DisplayName("E. No COMPLETED/PUBLISHED/AUTO_APPROVED")
    class NoPublish {
        @Test @DisplayName("status enum safe") void status() {
            for (var v : TurkuMvpReadinessStatus.values())
                assertThat(v.name()).doesNotContain("COMPLETED").doesNotContain("PUBLISHED")
                        .doesNotContain("AUTO_APPROVED").doesNotContain("READY_FOR_PUBLISH")
                        .doesNotContain("PUBLICLY_AVAILABLE");
        }
        @Test @DisplayName("exactly 5 statuses")
        void count() { assertThat(TurkuMvpReadinessStatus.values()).hasSize(5); }
        @Test @DisplayName("exactly 10 issue codes")
        void issues() { assertThat(TurkuMvpReadinessIssueCode.values()).hasSize(10); }
    }

    // \u2500\u2500 F. Endpoint Unchanged \u2500\u2500
    @Nested @DisplayName("F. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test @DisplayName("total unchanged") void total() {
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

    // \u2500\u2500 G. Checklist \u2500\u2500
    @Nested @DisplayName("G. Safety Checklist")
    class Checklist {
        @Test @DisplayName("ready result checklist all true") void allTrue() {
            var r = service.assess(validRequest(), validDraft(), validAssetPlan(), validTimeline());
            assertThat(r.isCopyrightSafeIntentPresent()).isTrue();
            assertThat(r.isAllScenesPlanned()).isTrue();
            assertThat(r.isTimelineComplete()).isTrue();
            assertThat(r.isNoBlockedScenes()).isTrue();
        }
    }

    // \u2500\u2500 H. ProcessBuilder \u2500\u2500
    @Nested @DisplayName("H. ProcessBuilder/Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime") void clean() {
            for (var c : RP005_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
