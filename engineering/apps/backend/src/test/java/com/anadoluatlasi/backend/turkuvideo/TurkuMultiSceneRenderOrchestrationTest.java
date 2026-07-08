package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-038 — Multi-Scene Internal Render Orchestration.
 */
@DisplayName("FP-038 — Multi-Scene Render Orchestration")
class TurkuMultiSceneRenderOrchestrationTest {

    private TurkuMultiSceneRenderOrchestrationService service;
    private TurkuSceneRenderPlanService planService;

    @BeforeEach
    void setUp() {
        service = new TurkuMultiSceneRenderOrchestrationService();
        planService = new TurkuSceneRenderPlanService();
    }

    private TurkuSceneRenderPlan validPlan(int order) {
        return planService.validate("scene-" + order, "plan-" + order, order, 3, 24, 640, 360,
                List.of(new TurkuSceneRenderArtifactExpectation(
                        TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true)));
    }

    // ── A. Valid Single Scene ──
    @Nested @DisplayName("A. Valid Single Scene Job")
    class SingleScene {
        @Test @DisplayName("one scene → READY_FOR_LOCAL_MANUAL_RENDER")
        void ready() {
            var job = service.buildJobPlan("job-1", List.of(validPlan(1)));
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.READY_FOR_LOCAL_MANUAL_RENDER);
            assertThat(job.getSceneCount()).isEqualTo(1);
            assertThat(job.getTotalExpectedDurationSeconds()).isEqualTo(3);
            assertThat(job.isInternalOnly()).isTrue();
            assertThat(job.isPublicDeliveryAvailable()).isFalse();
            assertThat(job.isAutoApproved()).isFalse();
        }
    }

    // ── B. Valid Multi-Scene ──
    @Nested @DisplayName("B. Valid Multi-Scene Job")
    class MultiScene {
        @Test @DisplayName("3 scenes → READY, sorted, aggregated")
        void threeScenes() {
            var job = service.buildJobPlan("job-2", List.of(validPlan(3), validPlan(1), validPlan(2)));
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.READY_FOR_LOCAL_MANUAL_RENDER);
            assertThat(job.getSceneCount()).isEqualTo(3);
            assertThat(job.getTotalExpectedDurationSeconds()).isEqualTo(9);
            assertThat(job.getMaxWidth()).isEqualTo(640);
            assertThat(job.getMaxFps()).isEqualTo(24);
            assertThat(job.getRequiredArtifactCount()).isEqualTo(3);
            // Check sorted order
            var orders = job.getScenes().stream().map(TurkuMultiSceneRenderSceneRef::getSceneOrder).toList();
            assertThat(orders).containsExactly(1, 2, 3);
        }
    }

    // ── C. Empty Scene List ──
    @Nested @DisplayName("C. Empty Scene List")
    class EmptyList {
        @Test @DisplayName("empty → REJECTED_UNSAFE")
        void empty() {
            var job = service.buildJobPlan("job-e", List.of());
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.REJECTED_UNSAFE);
            assertThat(job.getIssueCodes()).contains(TurkuMultiSceneRenderJobIssueCode.EMPTY_SCENE_LIST);
        }
    }

    // ── D. Too Many Scenes ──
    @Nested @DisplayName("D. Too Many Scenes")
    class TooMany {
        @Test @DisplayName(">20 → REJECTED_UNSAFE")
        void tooMany() {
            var plans = IntStream.rangeClosed(1, 21).mapToObj(i -> validPlan(i)).toList();
            var job = service.buildJobPlan("job-tm", plans);
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.REJECTED_UNSAFE);
            assertThat(job.getIssueCodes()).contains(TurkuMultiSceneRenderJobIssueCode.TOO_MANY_SCENES);
        }
    }

    // ── E. Non-Contiguous Order ──
    @Nested @DisplayName("E. Non-Contiguous Scene Order")
    class NonContiguous {
        @Test @DisplayName("1,3 → REJECTED_UNSAFE")
        void gap() {
            var job = service.buildJobPlan("job-nc", List.of(validPlan(1), validPlan(3)));
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.REJECTED_UNSAFE);
            assertThat(job.getIssueCodes()).contains(TurkuMultiSceneRenderJobIssueCode.SCENE_ORDER_NOT_CONTIGUOUS);
        }
    }

    // ── F. Duplicate Scene Order ──
    @Nested @DisplayName("F. Duplicate Scene Order")
    class DupOrder {
        @Test @DisplayName("1,1 → REJECTED_UNSAFE")
        void dupOrder() {
            var job = service.buildJobPlan("job-do", List.of(validPlan(1), validPlan(1)));
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.REJECTED_UNSAFE);
            assertThat(job.getIssueCodes()).contains(TurkuMultiSceneRenderJobIssueCode.SCENE_ORDER_DUPLICATE);
        }
    }

    // ── G. Duplicate Scene Ref ──
    @Nested @DisplayName("G. Duplicate Scene Ref")
    class DupRef {
        @Test @DisplayName("same scene ref, diff order → REJECTED_UNSAFE")
        void dupRef() {
            var p1 = planService.validate("scene-same", "plan-1", 1, 3, 24, 640, 360,
                    List.of(new TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true)));
            var p2 = planService.validate("scene-same", "plan-2", 2, 3, 24, 640, 360,
                    List.of(new TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true)));
            var job = service.buildJobPlan("job-dr", List.of(p1, p2));
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.REJECTED_UNSAFE);
            assertThat(job.getIssueCodes()).contains(TurkuMultiSceneRenderJobIssueCode.SCENE_REF_DUPLICATE);
        }
    }

    // ── H. Total Duration Exceeds ──
    @Nested @DisplayName("H. Total Duration Exceeds Limit")
    class TotalDuration {
        @Test @DisplayName("13x5s = 65s > 60s → REJECTED_UNSAFE")
        void over() {
            var plans = IntStream.rangeClosed(1, 13).mapToObj(i ->
                    planService.validate("s-" + i, "p-" + i, i, 5, 24, 640, 360,
                            List.of(new TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true)))
            ).toList();
            var job = service.buildJobPlan("job-td", plans);
            assertThat(job.getIssueCodes()).contains(TurkuMultiSceneRenderJobIssueCode.TOTAL_DURATION_EXCEEDS_LIMIT);
        }
    }

    // ── I. Rejected Scene Plan ──
    @Nested @DisplayName("I. Rejected Scene Plan")
    class RejectedScene {
        @Test @DisplayName("rejected scene → REJECTED_UNSAFE job")
        void rejected() {
            var bad = planService.validate("s", "p", 1, 6, 24, 640, 360,
                    List.of(new TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true)));
            assertThat(bad.getStatus()).isEqualTo(TurkuSceneRenderPlanStatus.REJECTED_UNSAFE);
            var job = service.buildJobPlan("job-rs", List.of(bad));
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.REJECTED_UNSAFE);
            assertThat(job.getIssueCodes()).contains(TurkuMultiSceneRenderJobIssueCode.SCENE_REJECTED);
        }
    }

    // ── J. Failed Scene Plan ──
    @Nested @DisplayName("J. Failed Scene Plan")
    class FailedScene {
        @Test @DisplayName("failed scene → FAILED_SAFE job")
        void failed() {
            var failPlan = new TurkuSceneRenderPlan("s-fail", "p-fail", 1, 3, 24, 640, 360,
                    List.of(new TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true)),
                    TurkuSceneRenderPlanStatus.FAILED_SAFE, List.of());
            var job = service.buildJobPlan("job-fs", List.of(failPlan));
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.FAILED_SAFE);
        }
    }

    // ── K. Missing Primary Video ──
    @Nested @DisplayName("K. Missing Primary Video Expectation")
    class MissingPrimary {
        @Test @DisplayName("no primary → REJECTED_UNSAFE")
        void noPrimary() {
            var plan = new TurkuSceneRenderPlan("s", "p", 1, 3, 24, 640, 360,
                    List.of(new TurkuSceneRenderArtifactExpectation(TurkuRenderBundleArtifactRole.POSTER_FRAME, false)),
                    TurkuSceneRenderPlanStatus.READY_FOR_BUNDLE_ASSEMBLY, List.of());
            var job = service.buildJobPlan("job-mp", List.of(plan));
            assertThat(job.getIssueCodes()).contains(TurkuMultiSceneRenderJobIssueCode.MISSING_PRIMARY_VIDEO);
        }
    }

    // ── L. Missing Refs ──
    @Nested @DisplayName("L. Missing Opaque Refs")
    class MissingRefs {
        @Test @DisplayName("null job ref → issue")
        void nullJob() {
            var job = service.buildJobPlan(null, List.of(validPlan(1)));
            assertThat(job.getIssueCodes()).contains(TurkuMultiSceneRenderJobIssueCode.JOB_REF_MISSING);
        }
    }

    // ── M. toString Safety ──
    @Nested @DisplayName("M. toString Safety")
    class ToStringSafety {
        @Test @DisplayName("safe toString")
        void safe() {
            var job = service.buildJobPlan("job-ts", List.of(validPlan(1)));
            assertThat(job.toString()).doesNotContain("filePath", "absolutePath", "stdout", "stderr",
                    "commandLine", "storageKey", "publicUrl", "base64", "rawLog",
                    "token", "JWT", "session", "principal", "lyrics", "scriptBody");
        }
    }

    // ── N. Enum Inventory ──
    @Nested @DisplayName("N. Enum Inventory")
    class Enums {
        @Test @DisplayName("6 job statuses")
        void statuses() { assertThat(TurkuMultiSceneRenderJobStatus.values()).hasSize(6); }
        @Test @DisplayName("17 job issue codes")
        void issues() { assertThat(TurkuMultiSceneRenderJobIssueCode.values()).hasSize(17); }
    }

    // ── O. ProcessBuilder Inventory ──
    @Nested @DisplayName("O. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("orchestration service no ProcessBuilder")
        void serviceClean() {
            Set<String> types = Arrays.stream(TurkuMultiSceneRenderOrchestrationService.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("job plan no ProcessBuilder")
        void planClean() {
            Set<String> types = Arrays.stream(TurkuMultiSceneRenderJobPlan.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
    }

    // ── P. Forbidden Exposure ──
    @Nested @DisplayName("P. Forbidden Exposure")
    class Forbidden {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
                "token", "jwt", "session", "principal", "authorizationHeader",
                "lyrics", "scriptBody", "promptText", "copyrightedContent");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuMultiSceneRenderJobStatus.class, TurkuMultiSceneRenderJobIssueCode.class,
                TurkuMultiSceneRenderSceneRef.class, TurkuMultiSceneRenderJobPlan.class,
                TurkuMultiSceneRenderOrchestrationService.class);

        @Test @DisplayName("5 classes x 29 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── Q. Endpoint Inventory ──
    @Nested @DisplayName("Q. Endpoint Inventory (44 = 25 POST + 19 GET)")
    class Endpoints {
        @Test @DisplayName("44 = 25 POST + 19 GET")
        void total() {
            long post = count(AdminReviewController.class, PostMapping.class)
                    + count(AdminPipelineController.class, PostMapping.class)
                    + count(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + count(TurkuVideoProjectController.class, PostMapping.class);
            long get = count(AdminReviewController.class, GetMapping.class)
                    + count(AdminPipelineController.class, GetMapping.class)
                    + count(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + count(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post + get).isEqualTo(44);
            assertThat(post).isEqualTo(25);
            assertThat(get).isEqualTo(19);
        }
        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }
}
