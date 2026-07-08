package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RP-004 \u2014 Safe Animatic Timeline Plan Boundary")
class TurkuAnimaticTimelinePlanTest {

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

    private static final List<Class<?>> RP004_CLASSES = List.of(
            TurkuAnimaticTimelineItem.class,
            TurkuAnimaticTimelinePlanResult.class,
            TurkuAnimaticTimelinePlanner.class);

    private TurkuAnimaticTimelinePlanner planner;

    @BeforeEach void setUp() { planner = new TurkuAnimaticTimelinePlanner(); }

    private TurkuSceneAssetPlanResult validAssetPlan() {
        var reqSvc = new TurkuNameStoryboardRequestService();
        var req = reqSvc.createRequest(new TurkuNameStoryboardRequest(
                "\u00c7anakkale T\u00fcrk\u00fcs\u00fc", "Marmara", "HALK", "CINEMATIC",
                4, 40_000, "MP4", 1920, 1080, 24));
        var draft = new TurkuStoryboardDraftOrchestrator().orchestrate(req);
        return new TurkuSceneAssetPlanner().plan(draft);
    }

    // \u2500\u2500 A. Valid Timeline \u2500\u2500
    @Nested @DisplayName("A. Valid Timeline")
    class ValidTimeline {
        @Test @DisplayName("creates READY_FOR_ASSEMBLY_BOUNDARY with items")
        void valid() {
            var r = planner.plan(validAssetPlan());
            assertThat(r.getStatus()).isEqualTo(TurkuAnimaticTimelinePlanStatus.READY_FOR_ASSEMBLY_BOUNDARY);
            assertThat(r.getOpaqueTimelineRef()).startsWith("timeline:");
            assertThat(r.getPlannerMode()).isEqualTo("DETERMINISTIC_FAKE_TEST_DOUBLE");
            assertThat(r.getItems()).hasSize(4);
            assertThat(r.getTotalItems()).isEqualTo(4);
            assertThat(r.getTotalDurationMs()).isGreaterThan(0);
        }
        @Test @DisplayName("items have valid metadata")
        void itemMeta() {
            var r = planner.plan(validAssetPlan());
            var item = r.getItems().get(0);
            assertThat(item.getSceneNumber()).isEqualTo(1);
            assertThat(item.getStartMs()).isEqualTo(0);
            assertThat(item.getDurationMs()).isGreaterThan(0);
            assertThat(item.getTransitionType()).isNotBlank();
            assertThat(item.getCameraMovement()).isNotBlank();
            assertThat(item.getVisualPacingSummary()).isNotBlank();
            assertThat(item.getNarrativeBeatSummary()).isNotBlank();
        }
        @Test @DisplayName("items are sequential")
        void sequential() {
            var r = planner.plan(validAssetPlan());
            int prevEnd = 0;
            for (var item : r.getItems()) {
                assertThat(item.getStartMs()).isEqualTo(prevEnd);
                prevEnd = item.getStartMs() + item.getDurationMs();
            }
            assertThat(prevEnd).isEqualTo(r.getTotalDurationMs());
        }
    }

    // \u2500\u2500 B. Rejection Cases \u2500\u2500
    @Nested @DisplayName("B. Rejection Cases")
    class Rejections {
        @Test @DisplayName("null plan") void nullPlan() {
            assertThat(planner.plan(null).getIssueCodes())
                    .contains(TurkuAnimaticTimelinePlanIssueCode.ASSET_PLAN_MISSING);
        }
        @Test @DisplayName("not-ready plan") void notReady() {
            var rej = TurkuSceneAssetPlanResult.rejected(TurkuSceneAssetPlanIssueCode.DRAFT_MISSING);
            assertThat(planner.plan(rej).getIssueCodes())
                    .contains(TurkuAnimaticTimelinePlanIssueCode.ASSET_PLAN_NOT_READY);
        }
        @Test @DisplayName("no safe entries") void noEntries() {
            var entry = new TurkuSceneAssetPlanEntry(1, TurkuSceneAssetPlanStatus.BLOCKED_CONTENT_UNSAFE,
                    List.of(), null, null, null, null, null, "BLOCKED", "test");
            var plan = new TurkuSceneAssetPlanResult(
                    TurkuSceneAssetPlanStatus.READY_FOR_ASSET_GENERATION_BOUNDARY, List.of(),
                    "p:1", "d:1", "Test", "FAKE", List.of(entry), 1, 1);
            assertThat(planner.plan(plan).getIssueCodes())
                    .contains(TurkuAnimaticTimelinePlanIssueCode.NO_ENTRIES);
        }
        @Test @DisplayName("missing draft ref") void noDraft() {
            var entry = new TurkuSceneAssetPlanEntry(1, TurkuSceneAssetPlanStatus.READY_FOR_ASSET_GENERATION_BOUNDARY,
                    List.of(), "brief", "WIDE", "frame", "atm", "style", "SAFE", null);
            var plan = new TurkuSceneAssetPlanResult(
                    TurkuSceneAssetPlanStatus.READY_FOR_ASSET_GENERATION_BOUNDARY, List.of(),
                    "p:1", null, "Test", "FAKE", List.of(entry), 1, 0);
            assertThat(planner.plan(plan).getIssueCodes())
                    .contains(TurkuAnimaticTimelinePlanIssueCode.DRAFT_REF_MISSING);
        }
    }

    // \u2500\u2500 C. Copyright Safety \u2500\u2500
    @Nested @DisplayName("C. Copyright Safety")
    class CopyrightSafety {
        @Test @DisplayName("items have no lyrics/audio references")
        void noLyrics() {
            var r = planner.plan(validAssetPlan());
            for (var item : r.getItems()) {
                String all = item.getVisualPacingSummary() + item.getNarrativeBeatSummary();
                assertThat(all).doesNotContainIgnoringCase("lyrics");
                assertThat(all).doesNotContainIgnoringCase("audio");
                assertThat(all).doesNotContainIgnoringCase("melody");
            }
        }
    }

    // \u2500\u2500 D. Forbidden Sweep \u2500\u2500
    @Nested @DisplayName("D. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields") void sweep() {
            for (var c : RP004_CLASSES) {
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
            for (var v : TurkuAnimaticTimelinePlanStatus.values())
                assertThat(v.name()).doesNotContain("COMPLETED").doesNotContain("PUBLISHED")
                        .doesNotContain("AUTO_APPROVED").doesNotContain("READY_FOR_PUBLISH")
                        .doesNotContain("PUBLICLY_AVAILABLE");
        }
        @Test @DisplayName("exactly 6 statuses")
        void count() { assertThat(TurkuAnimaticTimelinePlanStatus.values()).hasSize(6); }
        @Test @DisplayName("exactly 6 issue codes")
        void issues() { assertThat(TurkuAnimaticTimelinePlanIssueCode.values()).hasSize(6); }
    }

    // \u2500\u2500 F. Deterministic Test-Double \u2500\u2500
    @Nested @DisplayName("F. Deterministic Fake/Test-Double")
    class Deterministic {
        @Test @DisplayName("planner mode") void mode() {
            assertThat(planner.plan(validAssetPlan()).getPlannerMode())
                    .isEqualTo("DETERMINISTIC_FAKE_TEST_DOUBLE");
        }
        @Test @DisplayName("same input same output") void det() {
            var ap = validAssetPlan();
            var r1 = planner.plan(ap);
            var r2 = planner.plan(ap);
            assertThat(r1.getItems().get(0).getTransitionType())
                    .isEqualTo(r2.getItems().get(0).getTransitionType());
        }
    }

    // \u2500\u2500 G. Endpoint Unchanged \u2500\u2500
    @Nested @DisplayName("G. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // \u2500\u2500 H. Factories + ProcessBuilder \u2500\u2500
    @Nested @DisplayName("H. Factories + ProcessBuilder")
    class Factories {
        @Test @DisplayName("rejected factory") void rej() {
            var r = TurkuAnimaticTimelinePlanResult.rejected(TurkuAnimaticTimelinePlanIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(TurkuAnimaticTimelinePlanStatus.REJECTED_UNSAFE);
            assertThat(r.getItems()).isEmpty();
        }
        @Test @DisplayName("failed factory") void fail() {
            var r = TurkuAnimaticTimelinePlanResult.failed(TurkuAnimaticTimelinePlanIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(TurkuAnimaticTimelinePlanStatus.FAILED_SAFE);
        }
        @Test @DisplayName("no ProcessBuilder/Runtime") void clean() {
            for (var c : RP004_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
