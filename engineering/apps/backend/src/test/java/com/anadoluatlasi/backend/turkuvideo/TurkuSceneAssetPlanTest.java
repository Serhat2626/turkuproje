package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RP-003 \u2014 Safe Scene Asset Plan + Visual Brief Boundary")
class TurkuSceneAssetPlanTest {

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

    private static final List<Class<?>> RP003_CLASSES = List.of(
            TurkuSceneAssetPlanEntry.class,
            TurkuSceneAssetPlanResult.class,
            TurkuSceneAssetPlanner.class);

    private TurkuSceneAssetPlanner planner;

    @BeforeEach void setUp() { planner = new TurkuSceneAssetPlanner(); }

    private TurkuStoryboardDraftResult validDraft() {
        var reqService = new TurkuNameStoryboardRequestService();
        var req = reqService.createRequest(new TurkuNameStoryboardRequest(
                "\u00c7anakkale T\u00fcrk\u00fcs\u00fc", "Marmara", "HALK", "CINEMATIC",
                4, 40_000, "MP4", 1920, 1080, 24));
        return new TurkuStoryboardDraftOrchestrator().orchestrate(req);
    }

    // \u2500\u2500 A. Valid Plan \u2500\u2500
    @Nested @DisplayName("A. Valid Plan")
    class ValidPlan {
        @Test @DisplayName("draft creates READY_FOR_ASSET_GENERATION_BOUNDARY with entries")
        void valid() {
            var r = planner.plan(validDraft());
            assertThat(r.getOverallStatus()).isEqualTo(TurkuSceneAssetPlanStatus.READY_FOR_ASSET_GENERATION_BOUNDARY);
            assertThat(r.getOpaquePlanRef()).startsWith("plan:");
            assertThat(r.getPlannerMode()).isEqualTo("DETERMINISTIC_FAKE_TEST_DOUBLE");
            assertThat(r.getEntries()).hasSize(4);
            assertThat(r.getTotalPlannedScenes()).isEqualTo(4);
            assertThat(r.getBlockedSceneCount()).isEqualTo(0);
        }
        @Test @DisplayName("each entry has safe visual brief")
        void briefSafe() {
            var r = planner.plan(validDraft());
            for (var e : r.getEntries()) {
                assertThat(e.getVisualBriefSummary()).isNotBlank();
                assertThat(e.getShotType()).isNotBlank();
                assertThat(e.getFraming()).isNotBlank();
                assertThat(e.getAtmosphere()).isNotBlank();
                assertThat(e.getHistoricalCulturalStyle()).isNotBlank();
                assertThat(e.getSafetyClassification()).isEqualTo("SAFE");
                assertThat(e.getBlockedReason()).isNull();
            }
        }
        @Test @DisplayName("visual brief is summary not provider prompt")
        void notPrompt() {
            var r = planner.plan(validDraft());
            for (var e : r.getEntries()) {
                assertThat(e.getVisualBriefSummary()).doesNotContain("negative_prompt");
                assertThat(e.getVisualBriefSummary()).doesNotContain("cfg_scale");
                assertThat(e.getVisualBriefSummary()).doesNotContain("sampler");
                assertThat(e.getVisualBriefSummary()).startsWith("Sahne");
            }
        }
    }

    // \u2500\u2500 B. Rejection Cases \u2500\u2500
    @Nested @DisplayName("B. Rejection Cases")
    class Rejections {
        @Test @DisplayName("null draft") void nullDraft() {
            assertThat(planner.plan(null).getOverallIssueCodes())
                    .contains(TurkuSceneAssetPlanIssueCode.DRAFT_MISSING);
        }
        @Test @DisplayName("not-ready draft") void notReady() {
            var rejected = TurkuStoryboardDraftResult.rejected(TurkuStoryboardDraftIssueCode.REQUEST_MISSING);
            assertThat(planner.plan(rejected).getOverallIssueCodes())
                    .contains(TurkuSceneAssetPlanIssueCode.DRAFT_NOT_READY);
        }
        @Test @DisplayName("empty scenes") void noScenes() {
            var draft = new TurkuStoryboardDraftResult(
                    TurkuStoryboardDraftStatus.READY_FOR_ADMIN_PREVIEW, List.of(),
                    "d:1", "r:1", "Test", "intent", "FAKE", List.of(), 0, 0, "MP4", 1920, 1080, 24);
            assertThat(planner.plan(draft).getOverallIssueCodes())
                    .contains(TurkuSceneAssetPlanIssueCode.SCENE_MISSING);
        }
    }

    // \u2500\u2500 C. Blocked Scene \u2500\u2500
    @Nested @DisplayName("C. Blocked Scene Handling")
    class BlockedScene {
        @Test @DisplayName("empty purpose blocks scene")
        void emptyPurpose() {
            var scene = new TurkuStoryboardDraftScene(1, "", "huzurlu", "ova", 10000);
            var draft = new TurkuStoryboardDraftResult(
                    TurkuStoryboardDraftStatus.READY_FOR_ADMIN_PREVIEW, List.of(),
                    "d:1", "r:1", "Test", "intent", "FAKE", List.of(scene), 1, 10000, "MP4", 1920, 1080, 24);
            var r = planner.plan(draft);
            assertThat(r.getBlockedSceneCount()).isEqualTo(1);
            assertThat(r.getEntries().get(0).getStatus()).isEqualTo(TurkuSceneAssetPlanStatus.BLOCKED_CONTENT_UNSAFE);
            assertThat(r.getEntries().get(0).getBlockedReason()).isNotBlank();
        }
    }

    // \u2500\u2500 D. Forbidden Sweep \u2500\u2500
    @Nested @DisplayName("D. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields") void sweep() {
            for (var c : RP003_CLASSES) {
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
            for (var v : TurkuSceneAssetPlanStatus.values())
                assertThat(v.name()).doesNotContain("COMPLETED").doesNotContain("PUBLISHED")
                        .doesNotContain("AUTO_APPROVED").doesNotContain("READY_FOR_PUBLISH")
                        .doesNotContain("PUBLICLY_AVAILABLE");
        }
        @Test @DisplayName("exactly 7 statuses")
        void count() { assertThat(TurkuSceneAssetPlanStatus.values()).hasSize(7); }
        @Test @DisplayName("exactly 7 issue codes")
        void issues() { assertThat(TurkuSceneAssetPlanIssueCode.values()).hasSize(7); }
    }

    // \u2500\u2500 F. Deterministic Test-Double \u2500\u2500
    @Nested @DisplayName("F. Deterministic Fake/Test-Double")
    class Deterministic {
        @Test @DisplayName("planner mode") void mode() {
            assertThat(planner.plan(validDraft()).getPlannerMode())
                    .isEqualTo("DETERMINISTIC_FAKE_TEST_DOUBLE");
        }
        @Test @DisplayName("same input same output") void det() {
            var d = validDraft();
            var r1 = planner.plan(d);
            var r2 = planner.plan(d);
            assertThat(r1.getEntries().get(0).getVisualBriefSummary())
                    .isEqualTo(r2.getEntries().get(0).getVisualBriefSummary());
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
            var r = TurkuSceneAssetPlanResult.rejected(TurkuSceneAssetPlanIssueCode.INTERNAL_ERROR);
            assertThat(r.getOverallStatus()).isEqualTo(TurkuSceneAssetPlanStatus.REJECTED_UNSAFE);
            assertThat(r.getEntries()).isEmpty();
        }
        @Test @DisplayName("failed factory") void fail() {
            var r = TurkuSceneAssetPlanResult.failed(TurkuSceneAssetPlanIssueCode.INTERNAL_ERROR);
            assertThat(r.getOverallStatus()).isEqualTo(TurkuSceneAssetPlanStatus.FAILED_SAFE);
        }
        @Test @DisplayName("no ProcessBuilder/Runtime") void clean() {
            for (var c : RP003_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
