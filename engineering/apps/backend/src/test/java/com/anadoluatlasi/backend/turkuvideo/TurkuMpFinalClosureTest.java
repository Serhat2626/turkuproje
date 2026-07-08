package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MP-003 \u2014 Manual Production Enablement MVP Final Closure")
class TurkuMpFinalClosureTest {

    // All MP + RP classes
    private static final List<Class<?>> ALL_MP_RP_CLASSES = List.of(
            // RP
            TurkuNameStoryboardRequest.class, TurkuNameStoryboardRequestResult.class,
            TurkuNameStoryboardRequestPolicy.class, TurkuNameStoryboardRequestService.class,
            TurkuStoryboardDraftScene.class, TurkuStoryboardDraftResult.class, TurkuStoryboardDraftOrchestrator.class,
            TurkuSceneAssetPlanEntry.class, TurkuSceneAssetPlanResult.class, TurkuSceneAssetPlanner.class,
            TurkuAnimaticTimelineItem.class, TurkuAnimaticTimelinePlanResult.class, TurkuAnimaticTimelinePlanner.class,
            TurkuMvpReadinessResult.class, TurkuMvpReadinessService.class,
            // MP
            TurkuManualEnablementResult.class, TurkuManualEnablementService.class,
            TurkuProductionChecklistItem.class,
            TurkuWorkOrderPreviewResult.class, TurkuWorkOrderPreviewService.class);

    private static final List<Class<? extends Enum<?>>> ALL_ENUMS = List.of(
            TurkuNameStoryboardRequestStatus.class, TurkuNameStoryboardRequestIssueCode.class,
            TurkuStoryboardDraftStatus.class, TurkuStoryboardDraftIssueCode.class,
            TurkuSceneAssetPlanStatus.class, TurkuSceneAssetPlanIssueCode.class,
            TurkuAnimaticTimelinePlanStatus.class, TurkuAnimaticTimelinePlanIssueCode.class,
            TurkuMvpReadinessStatus.class, TurkuMvpReadinessIssueCode.class,
            TurkuManualEnablementStatus.class, TurkuManualEnablementIssueCode.class,
            TurkuManualStepType.class,
            TurkuWorkOrderPreviewStatus.class, TurkuWorkOrderPreviewIssueCode.class);

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

    private static final List<String> FORBIDDEN_STATUSES = List.of(
            "COMPLETED", "PUBLISHED", "PUBLICLY_AVAILABLE", "READY_FOR_PUBLISH",
            "READY_FOR_RENDER", "RENDERING", "GENERATED", "DELIVERED", "AUTO_APPROVED");

    // \u2500\u2500 A. Full Chain End-to-End \u2500\u2500
    @Nested @DisplayName("A. Full RP+MP Chain End-to-End")
    class FullChain {
        @Test @DisplayName("T\u00fcrk\u00fc name \u2192 work order ELIGIBLE")
        void endToEnd() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("\u00c7anakkale T\u00fcrk\u00fcs\u00fc", "Marmara", "HALK", "CINEMATIC", 5, 50_000, "MP4", 1920, 1080, 24));
            assertThat(req.getStatus()).isEqualTo(TurkuNameStoryboardRequestStatus.READY_FOR_STORYBOARD_GENERATION_BOUNDARY);

            var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
            assertThat(draft.getStatus()).isEqualTo(TurkuStoryboardDraftStatus.READY_FOR_ADMIN_PREVIEW);

            var plan = new TurkuSceneAssetPlanner().plan(draft);
            assertThat(plan.getOverallStatus()).isEqualTo(TurkuSceneAssetPlanStatus.READY_FOR_ASSET_GENERATION_BOUNDARY);

            var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
            assertThat(tl.getStatus()).isEqualTo(TurkuAnimaticTimelinePlanStatus.READY_FOR_ASSEMBLY_BOUNDARY);

            var readiness = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
            assertThat(readiness.getStatus()).isEqualTo(TurkuMvpReadinessStatus.READY_FOR_MANUAL_REVIEW);

            var enablement = new TurkuManualEnablementService().evaluate(readiness);
            assertThat(enablement.getStatus()).isEqualTo(TurkuManualEnablementStatus.ELIGIBLE_FOR_MANUAL_PREP);

            var workOrder = new TurkuWorkOrderPreviewService().generate(enablement);
            assertThat(workOrder.getStatus()).isEqualTo(TurkuWorkOrderPreviewStatus.ELIGIBLE_FOR_MANUAL_PREPARATION);
            assertThat(workOrder.getChecklistItems()).hasSizeGreaterThanOrEqualTo(6);
            assertThat(workOrder.getCompletedItems()).isEqualTo(workOrder.getTotalItems());
        }

        @Test @DisplayName("all orchestrators deterministic fake")
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

        @Test @DisplayName("checklist labels only, work order non-executable")
        void labelsOnly() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("Test", null, null, null, 3, 30_000, "MP4", 1920, 1080, 24));
            var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
            var plan = new TurkuSceneAssetPlanner().plan(draft);
            var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
            var r = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
            var e = new TurkuManualEnablementService().evaluate(r);
            var wo = new TurkuWorkOrderPreviewService().generate(e);
            for (var item : wo.getChecklistItems()) {
                assertThat(item.getLabel()).isInstanceOf(String.class).isNotBlank();
                assertThat(item.getInstruction()).isInstanceOf(String.class).isNotBlank();
            }
            for (var step : wo.getWorkOrderStepLabels())
                assertThat(step).isInstanceOf(String.class).isNotBlank();
        }
    }

    // \u2500\u2500 B. Forbidden Field Sweep (20 classes) \u2500\u2500
    @Nested @DisplayName("B. Forbidden Field Sweep (20 RP+MP classes)")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields across all classes")
        void sweep() {
            for (var c : ALL_MP_RP_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN_FIELDS)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // \u2500\u2500 C. No Forbidden Statuses (15 enums) \u2500\u2500
    @Nested @DisplayName("C. No Forbidden Statuses (15 enums)")
    class NoForbiddenStatuses {
        @Test @DisplayName("no COMPLETED/PUBLISHED/RENDER/GENERATED/DELIVERED as exact status")
        void noForbidden() {
            for (var enumClass : ALL_ENUMS) {
                for (var v : enumClass.getEnumConstants()) {
                    for (var forbidden : FORBIDDEN_STATUSES)
                        assertThat(v.name()).as("%s.%s", enumClass.getSimpleName(), v.name())
                                .isNotEqualTo(forbidden);
                }
            }
        }
    }

    // \u2500\u2500 D. Endpoint Unchanged \u2500\u2500
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
            assertThat(post + get).as("total").isEqualTo(46);
            assertThat(post).as("POST").isEqualTo(26);
            assertThat(get).as("GET").isEqualTo(20);
        }
        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }

    // \u2500\u2500 E. ProcessBuilder / Runtime \u2500\u2500
    @Nested @DisplayName("E. ProcessBuilder / Runtime (20 classes)")
    class ProcessCheck {
        @Test @DisplayName("no ProcessBuilder/Runtime in RP+MP classes")
        void clean() {
            for (var c : ALL_MP_RP_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName())
                        .doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // \u2500\u2500 F. Chain Rejection \u2500\u2500
    @Nested @DisplayName("F. Chain Rejection Propagation")
    class Rejection {
        @Test @DisplayName("null readiness -> enablement NOT_ELIGIBLE -> work order BLOCKED")
        void fullReject() {
            var e = new TurkuManualEnablementService().evaluate(null);
            assertThat(e.getStatus()).isEqualTo(TurkuManualEnablementStatus.NOT_ELIGIBLE);
            var wo = new TurkuWorkOrderPreviewService().generate(e);
            assertThat(wo.getStatus()).isEqualTo(TurkuWorkOrderPreviewStatus.BLOCKED_BY_SAFETY);
        }
    }

    // \u2500\u2500 G. Copyright Safety \u2500\u2500
    @Nested @DisplayName("G. Copyright Safety")
    class Copyright {
        @Test @DisplayName("chain preserves copyright-safe intent")
        void intent() {
            var req = new TurkuNameStoryboardRequestService().createRequest(
                    new TurkuNameStoryboardRequest("Test", null, null, null, 3, 30_000, "MP4", 1920, 1080, 24));
            assertThat(req.getCopyrightSafeIntentSummary()).contains("telif");
            var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
            var plan = new TurkuSceneAssetPlanner().plan(draft);
            var tl = new TurkuAnimaticTimelinePlanner().plan(plan);
            var r = new TurkuMvpReadinessService().assess(req, draft, plan, tl);
            assertThat(r.isCopyrightSafeIntentPresent()).isTrue();
            var e = new TurkuManualEnablementService().evaluate(r);
            assertThat(e.isCopyrightSafeIntentPresent()).isTrue();
        }
    }

    // \u2500\u2500 H. Enum Inventory \u2500\u2500
    @Nested @DisplayName("H. Enum Inventory Snapshot")
    class EnumInventory {
        @Test @DisplayName("MP enum sizes")
        void mpSizes() {
            assertThat(TurkuManualEnablementStatus.values()).hasSize(5);
            assertThat(TurkuManualEnablementIssueCode.values()).hasSize(8);
            assertThat(TurkuManualStepType.values()).hasSize(6);
            assertThat(TurkuWorkOrderPreviewStatus.values()).hasSize(5);
            assertThat(TurkuWorkOrderPreviewIssueCode.values()).hasSize(6);
        }
        @Test @DisplayName("RP enum sizes")
        void rpSizes() {
            assertThat(TurkuNameStoryboardRequestStatus.values()).hasSize(6);
            assertThat(TurkuStoryboardDraftStatus.values()).hasSize(7);
            assertThat(TurkuSceneAssetPlanStatus.values()).hasSize(7);
            assertThat(TurkuAnimaticTimelinePlanStatus.values()).hasSize(6);
            assertThat(TurkuMvpReadinessStatus.values()).hasSize(5);
        }
    }
}
