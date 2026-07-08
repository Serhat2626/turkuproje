package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RP-006 \u2014 RP Planning MVP Safety Closure + Manual Handoff Baseline")
class TurkuRpPlanningMvpSafetyClosureTest {

    // RP-series classes
    private static final List<Class<?>> RP_ALL_CLASSES = List.of(
            // RP-001
            TurkuNameStoryboardRequest.class, TurkuNameStoryboardRequestResult.class,
            TurkuNameStoryboardRequestPolicy.class, TurkuNameStoryboardRequestService.class,
            // RP-002
            TurkuStoryboardDraftScene.class, TurkuStoryboardDraftResult.class,
            TurkuStoryboardDraftOrchestrator.class,
            // RP-003
            TurkuSceneAssetPlanEntry.class, TurkuSceneAssetPlanResult.class,
            TurkuSceneAssetPlanner.class,
            // RP-004
            TurkuAnimaticTimelineItem.class, TurkuAnimaticTimelinePlanResult.class,
            TurkuAnimaticTimelinePlanner.class,
            // RP-005
            TurkuMvpReadinessResult.class, TurkuMvpReadinessService.class);

    private static final List<String> FORBIDDEN_FIELDS = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "storageKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html", "audioFile", "videoFile", "mediaFile",
            "modelInput", "modelOutput", "providerPayload", "providerPrompt", "rawPrompt");

    private static final List<Class<? extends Enum<?>>> RP_ENUMS = List.of(
            TurkuNameStoryboardRequestStatus.class, TurkuNameStoryboardRequestIssueCode.class,
            TurkuStoryboardDraftStatus.class, TurkuStoryboardDraftIssueCode.class,
            TurkuSceneAssetPlanStatus.class, TurkuSceneAssetPlanIssueCode.class,
            TurkuAnimaticTimelinePlanStatus.class, TurkuAnimaticTimelinePlanIssueCode.class,
            TurkuMvpReadinessStatus.class, TurkuMvpReadinessIssueCode.class);

    // \u2500\u2500 A. Full Chain End-to-End \u2500\u2500
    @Nested @DisplayName("A. Full RP Chain End-to-End")
    class FullChain {
        @Test @DisplayName("T\u00fcrk\u00fc name \u2192 readiness READY_FOR_MANUAL_REVIEW")
        void endToEnd() {
            var reqSvc = new TurkuNameStoryboardRequestService();
            var req = reqSvc.createRequest(new TurkuNameStoryboardRequest(
                    "\u00c7anakkale T\u00fcrk\u00fcs\u00fc", "Marmara", "HALK", "CINEMATIC", 5, 50_000, "MP4", 1920, 1080, 24));
            assertThat(req.getStatus()).isEqualTo(TurkuNameStoryboardRequestStatus.READY_FOR_STORYBOARD_GENERATION_BOUNDARY);

            var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
            assertThat(draft.getStatus()).isEqualTo(TurkuStoryboardDraftStatus.READY_FOR_ADMIN_PREVIEW);
            assertThat(draft.getScenes()).hasSize(5);

            var assetPlan = new TurkuSceneAssetPlanner().plan(draft);
            assertThat(assetPlan.getOverallStatus()).isEqualTo(TurkuSceneAssetPlanStatus.READY_FOR_ASSET_GENERATION_BOUNDARY);
            assertThat(assetPlan.getEntries()).hasSize(5);

            var timeline = new TurkuAnimaticTimelinePlanner().plan(assetPlan);
            assertThat(timeline.getStatus()).isEqualTo(TurkuAnimaticTimelinePlanStatus.READY_FOR_ASSEMBLY_BOUNDARY);
            assertThat(timeline.getItems()).hasSize(5);

            var readiness = new TurkuMvpReadinessService().assess(req, draft, assetPlan, timeline);
            assertThat(readiness.getStatus()).isEqualTo(TurkuMvpReadinessStatus.READY_FOR_MANUAL_REVIEW);
            assertThat(readiness.getBlockingIssues()).isEmpty();
            assertThat(readiness.isCopyrightSafeIntentPresent()).isTrue();
            assertThat(readiness.isAllScenesPlanned()).isTrue();
            assertThat(readiness.isTimelineComplete()).isTrue();
            assertThat(readiness.isNoBlockedScenes()).isTrue();
        }

        @Test @DisplayName("next steps are labels only (strings)")
        void labelsOnly() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("Test", null, null, null, 3, 30_000, "MP4", 1920, 1080, 24));
            var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
            var plan = new TurkuSceneAssetPlanner().plan(draft);
            var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
            var r = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
            for (var label : r.getNextAllowedManualStepLabels()) {
                assertThat(label).isInstanceOf(String.class).isNotBlank();
            }
        }

        @Test @DisplayName("all orchestrators use DETERMINISTIC_FAKE_TEST_DOUBLE")
        void deterministicMode() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("Test", null, null, null, 3, 30_000, "MP4", 1920, 1080, 24));
            var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
            assertThat(draft.getOrchestrationMode()).isEqualTo("DETERMINISTIC_FAKE_TEST_DOUBLE");
            var plan = new TurkuSceneAssetPlanner().plan(draft);
            assertThat(plan.getPlannerMode()).isEqualTo("DETERMINISTIC_FAKE_TEST_DOUBLE");
            var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
            assertThat(tl.getPlannerMode()).isEqualTo("DETERMINISTIC_FAKE_TEST_DOUBLE");
        }
    }

    // \u2500\u2500 B. Forbidden Field Sweep (all 15 RP classes) \u2500\u2500
    @Nested @DisplayName("B. Forbidden Field Sweep (15 RP classes)")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields across all RP classes")
        void sweep() {
            for (var c : RP_ALL_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN_FIELDS)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // \u2500\u2500 C. No Publish/Completed Statuses (10 enums) \u2500\u2500
    @Nested @DisplayName("C. No COMPLETED/PUBLISHED/AUTO_APPROVED (10 enums)")
    class NoPublishStatuses {
        @Test @DisplayName("no forbidden status values")
        void noForbidden() {
            for (var enumClass : RP_ENUMS) {
                for (var v : enumClass.getEnumConstants()) {
                    assertThat(v.name()).as("%s.%s", enumClass.getSimpleName(), v.name())
                            .doesNotContain("COMPLETED")
                            .doesNotContain("PUBLISHED")
                            .doesNotContain("AUTO_APPROVED")
                            .doesNotContain("READY_FOR_PUBLISH")
                            .doesNotContain("PUBLICLY_AVAILABLE");
                }
            }
        }

        @Test @DisplayName("enum sizes inventory")
        void enumSizes() {
            assertThat(TurkuNameStoryboardRequestStatus.values()).hasSize(6);
            assertThat(TurkuNameStoryboardRequestIssueCode.values()).hasSize(11);
            assertThat(TurkuStoryboardDraftStatus.values()).hasSize(7);
            assertThat(TurkuStoryboardDraftIssueCode.values()).hasSize(8);
            assertThat(TurkuSceneAssetPlanStatus.values()).hasSize(7);
            assertThat(TurkuSceneAssetPlanIssueCode.values()).hasSize(7);
            assertThat(TurkuAnimaticTimelinePlanStatus.values()).hasSize(6);
            assertThat(TurkuAnimaticTimelinePlanIssueCode.values()).hasSize(6);
            assertThat(TurkuMvpReadinessStatus.values()).hasSize(5);
            assertThat(TurkuMvpReadinessIssueCode.values()).hasSize(10);
        }
    }

    // \u2500\u2500 D. Endpoint Inventory \u2500\u2500
    @Nested @DisplayName("D. Endpoint Inventory (46 = 26 POST + 20 GET)")
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
            assertThat(post + get).as("total endpoints").isEqualTo(46);
            assertThat(post).as("POST").isEqualTo(26);
            assertThat(get).as("GET").isEqualTo(20);
        }
        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }

    // \u2500\u2500 E. ProcessBuilder / Runtime \u2500\u2500
    @Nested @DisplayName("E. ProcessBuilder / Runtime")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime in RP classes")
        void clean() {
            for (var c : RP_ALL_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName())
                        .doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // \u2500\u2500 F. Copyright Safety \u2500\u2500
    @Nested @DisplayName("F. Copyright Safety Across Chain")
    class CopyrightSafety {
        @Test @DisplayName("request has copyright-safe intent")
        void intent() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("Test", null, null, null, 3, 30_000, "MP4", 1920, 1080, 24));
            assertThat(req.getCopyrightSafeIntentSummary()).contains("telif");
            assertThat(req.getInspirationMode()).isEqualTo("TURKU_TITLE_INSPIRATION");
        }

        @Test @DisplayName("draft scenes contain no lyrics/audio references")
        void draftScenesSafe() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("Test", null, null, null, 3, 30_000, "MP4", 1920, 1080, 24));
            var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
            for (var s : draft.getScenes()) {
                String all = s.getPurpose() + s.getMood() + s.getVisualDirection();
                assertThat(all).doesNotContainIgnoringCase("lyrics");
                assertThat(all).doesNotContainIgnoringCase("melody");
            }
        }

        @Test @DisplayName("asset plan briefs are summaries not provider prompts")
        void briefsNotPrompts() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("Test", null, null, null, 3, 30_000, "MP4", 1920, 1080, 24));
            var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
            var plan = new TurkuSceneAssetPlanner().plan(draft);
            for (var e : plan.getEntries()) {
                assertThat(e.getVisualBriefSummary()).doesNotContain("negative_prompt");
                assertThat(e.getVisualBriefSummary()).doesNotContain("cfg_scale");
            }
        }
    }

    // \u2500\u2500 G. Chain Rejection Propagation \u2500\u2500
    @Nested @DisplayName("G. Chain Rejection Propagation")
    class RejectionPropagation {
        @Test @DisplayName("rejected request -> readiness NOT_READY")
        void rejReq() {
            var rej = TurkuNameStoryboardRequestResult.rejected(TurkuNameStoryboardRequestIssueCode.TITLE_EMPTY);
            var r = new TurkuMvpReadinessService().assess(rej, null, null, null);
            assertThat(r.getStatus()).isIn(TurkuMvpReadinessStatus.NOT_READY, TurkuMvpReadinessStatus.BLOCKED_BY_SAFETY);
        }

        @Test @DisplayName("all null -> NOT_READY with 4+ issues")
        void allNull() {
            var r = new TurkuMvpReadinessService().assess(null, null, null, null);
            assertThat(r.getStatus()).isEqualTo(TurkuMvpReadinessStatus.NOT_READY);
            assertThat(r.getBlockingIssues()).hasSizeGreaterThanOrEqualTo(4);
        }
    }

    // \u2500\u2500 H. Factory Methods \u2500\u2500
    @Nested @DisplayName("H. Factory Methods Safety")
    class Factories {
        @Test @DisplayName("all rejected factories produce empty collections")
        void rejectedEmpty() {
            assertThat(TurkuNameStoryboardRequestResult.rejected(TurkuNameStoryboardRequestIssueCode.INTERNAL_ERROR)
                    .getIssueCodes()).isNotEmpty();
            assertThat(TurkuStoryboardDraftResult.rejected(TurkuStoryboardDraftIssueCode.INTERNAL_ERROR)
                    .getScenes()).isEmpty();
            assertThat(TurkuSceneAssetPlanResult.rejected(TurkuSceneAssetPlanIssueCode.INTERNAL_ERROR)
                    .getEntries()).isEmpty();
            assertThat(TurkuAnimaticTimelinePlanResult.rejected(TurkuAnimaticTimelinePlanIssueCode.INTERNAL_ERROR)
                    .getItems()).isEmpty();
        }

        @Test @DisplayName("all failed factories produce safe status")
        void failedSafe() {
            assertThat(TurkuNameStoryboardRequestResult.failed(TurkuNameStoryboardRequestIssueCode.INTERNAL_ERROR)
                    .getStatus()).isEqualTo(TurkuNameStoryboardRequestStatus.FAILED_SAFE);
            assertThat(TurkuStoryboardDraftResult.failed(TurkuStoryboardDraftIssueCode.INTERNAL_ERROR)
                    .getStatus()).isEqualTo(TurkuStoryboardDraftStatus.FAILED_SAFE);
            assertThat(TurkuSceneAssetPlanResult.failed(TurkuSceneAssetPlanIssueCode.INTERNAL_ERROR)
                    .getOverallStatus()).isEqualTo(TurkuSceneAssetPlanStatus.FAILED_SAFE);
            assertThat(TurkuAnimaticTimelinePlanResult.failed(TurkuAnimaticTimelinePlanIssueCode.INTERNAL_ERROR)
                    .getStatus()).isEqualTo(TurkuAnimaticTimelinePlanStatus.FAILED_SAFE);
        }
    }
}
