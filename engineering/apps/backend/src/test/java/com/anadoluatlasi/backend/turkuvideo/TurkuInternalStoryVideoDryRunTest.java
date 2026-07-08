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

import static org.assertj.core.api.Assertions.*;

/**
 * FP-044 — End-to-End Internal Story-to-Video Dry Run.
 */
@DisplayName("FP-044 — E2E Internal Story-to-Video Dry Run")
class TurkuInternalStoryVideoDryRunTest {

    private TurkuInternalStoryVideoDryRunHarness harness;

    @BeforeEach
    void setUp() { harness = new TurkuInternalStoryVideoDryRunHarness(); }

    // ── A. Full Dry Run → ADMIN_METADATA_VISIBLE ──
    @Nested @DisplayName("A. Full E2E Dry Run")
    class FullRun {
        @Test @DisplayName("3-scene dry run → ADMIN_METADATA_VISIBLE")
        void threeScenes() {
            var result = harness.execute(3);
            assertThat(result.getStatus()).isEqualTo(TurkuInternalStoryVideoDryRunStatus.ADMIN_METADATA_VISIBLE);
            assertThat(result.getSceneCount()).isEqualTo(3);
            assertThat(result.getGeneratedClipCount()).isEqualTo(3);
            assertThat(result.isStitchedArtifactPresent()).isTrue();
            assertThat(result.getReviewPackageStatus()).isEqualTo(TurkuRenderReviewPackageStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
            assertThat(result.isAdminMetadataVisible()).isTrue();
            assertThat(result.isInternalOnly()).isTrue();
            assertThat(result.getIssueCodes()).isEmpty();
        }

        @Test @DisplayName("1-scene dry run succeeds")
        void oneScene() {
            var result = harness.execute(1);
            assertThat(result.getStatus()).isEqualTo(TurkuInternalStoryVideoDryRunStatus.ADMIN_METADATA_VISIBLE);
            assertThat(result.getSceneCount()).isEqualTo(1);
            assertThat(result.getGeneratedClipCount()).isGreaterThanOrEqualTo(2); // min 2 for stitching
        }

        @Test @DisplayName("5-scene dry run succeeds")
        void fiveScenes() {
            var result = harness.execute(5);
            assertThat(result.getStatus()).isEqualTo(TurkuInternalStoryVideoDryRunStatus.ADMIN_METADATA_VISIBLE);
            assertThat(result.getSceneCount()).isEqualTo(5);
            assertThat(result.getGeneratedClipCount()).isEqualTo(5);
        }
    }

    // ── B. Rejected Scene Plan ──
    @Nested @DisplayName("B. Rejected Scene Plan")
    class RejectedPlan {
        @Test @DisplayName("0 scenes → REJECTED_UNSAFE")
        void zero() {
            var result = harness.execute(0);
            assertThat(result.getStatus()).isEqualTo(TurkuInternalStoryVideoDryRunStatus.REJECTED_UNSAFE);
            assertThat(result.getIssueCodes()).contains(TurkuInternalStoryVideoDryRunIssueCode.SCENE_PLAN_INVALID);
        }

        @Test @DisplayName("21 scenes → REJECTED_UNSAFE")
        void tooMany() {
            var result = harness.execute(21);
            assertThat(result.getStatus()).isEqualTo(TurkuInternalStoryVideoDryRunStatus.REJECTED_UNSAFE);
        }
    }

    // ── C. Metadata Aggregation ──
    @Nested @DisplayName("C. Metadata Aggregation")
    class Metadata {
        @Test @DisplayName("3-scene aggregates correct")
        void aggregates() {
            var result = harness.execute(3);
            assertThat(result.getTotalDurationSeconds()).isGreaterThan(0);
            assertThat(result.getMaxWidth()).isEqualTo(640);
            assertThat(result.getMaxHeight()).isEqualTo(360);
            assertThat(result.getMaxFps()).isEqualTo(24);
            assertThat(result.getTotalSizeBytes()).isGreaterThan(0);
        }

        @Test @DisplayName("opaque refs present")
        void refs() {
            var result = harness.execute(2);
            assertThat(result.getOpaqueDryRunRef()).startsWith("dry-run:");
            assertThat(result.getOpaqueJobRef()).isNotNull();
            assertThat(result.getOpaqueBundleRef()).isNotNull();
            assertThat(result.getOpaqueReviewPackageRef()).isNotNull();
        }
    }

    // ── D. toString Safety ──
    @Nested @DisplayName("D. toString Safety")
    class ToStringSafety {
        @Test @DisplayName("safe toString")
        void safe() {
            var str = harness.execute(2).toString();
            assertThat(str).doesNotContain("filePath", "absolutePath", "stdout", "stderr",
                    "commandLine", "storageKey", "publicUrl", "base64", "rawLog",
                    "token", "JWT", "session", "principal", "lyrics", "scriptBody");
            assertThat(str).contains("internalOnly=true");
        }
    }

    // ── E. Enum Inventory ──
    @Nested @DisplayName("E. Enum Inventory")
    class Enums {
        @Test @DisplayName("9 dry run statuses")
        void statuses() { assertThat(TurkuInternalStoryVideoDryRunStatus.values()).hasSize(9); }
        @Test @DisplayName("12 dry run issue codes")
        void issues() { assertThat(TurkuInternalStoryVideoDryRunIssueCode.values()).hasSize(12); }
    }

    // ── F. Forbidden Exposure ──
    @Nested @DisplayName("F. Forbidden Exposure")
    class Forbidden {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
                "token", "jwt", "session", "principal", "authorizationHeader",
                "lyrics", "scriptBody", "promptText", "copyrightedContent");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuInternalStoryVideoDryRunStatus.class,
                TurkuInternalStoryVideoDryRunIssueCode.class,
                TurkuInternalStoryVideoDryRunSummary.class,
                TurkuInternalStoryVideoDryRunHarness.class);

        @Test @DisplayName("4 classes x 29 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── G. ProcessBuilder Inventory ──
    @Nested @DisplayName("G. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("harness no ProcessBuilder")
        void clean() {
            Set<String> types = Arrays.stream(TurkuInternalStoryVideoDryRunHarness.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
    }

    // ── H. No Public Delivery Statuses ──
    @Nested @DisplayName("H. No Public Delivery Statuses")
    class NoPublish {
        @Test @DisplayName("no READY_FOR_PUBLISH")
        void noPublish() {
            assertThat(Arrays.stream(TurkuInternalStoryVideoDryRunStatus.values())
                    .noneMatch(s -> s.name().contains("PUBLISH"))).isTrue();
        }
        @Test @DisplayName("no COMPLETED")
        void noCompleted() {
            assertThat(Arrays.stream(TurkuInternalStoryVideoDryRunStatus.values())
                    .noneMatch(s -> s.name().equals("COMPLETED"))).isTrue();
        }
    }

    // ── I. Full Chain Integration ──
    @Nested @DisplayName("I. Full Chain Integration")
    class ChainIntegration {
        @Test @DisplayName("FP-037 scene plan used")
        void scenePlan() {
            assertThat(Arrays.stream(TurkuInternalStoryVideoDryRunHarness.class.getDeclaredFields())
                    .anyMatch(f -> f.getType() == TurkuSceneRenderPlanService.class)).isTrue();
        }
        @Test @DisplayName("FP-038 orchestration used")
        void orchestration() {
            assertThat(Arrays.stream(TurkuInternalStoryVideoDryRunHarness.class.getDeclaredFields())
                    .anyMatch(f -> f.getType() == TurkuMultiSceneRenderOrchestrationService.class)).isTrue();
        }
        @Test @DisplayName("FP-040 stitching used")
        void stitching() {
            assertThat(Arrays.stream(TurkuInternalStoryVideoDryRunHarness.class.getDeclaredFields())
                    .anyMatch(f -> f.getType() == TurkuClipStitchingHarness.class)).isTrue();
        }
        @Test @DisplayName("FP-042 review used")
        void review() {
            assertThat(Arrays.stream(TurkuInternalStoryVideoDryRunHarness.class.getDeclaredFields())
                    .anyMatch(f -> f.getType() == TurkuRenderReviewPackageService.class)).isTrue();
        }
    }

    // ── J. Endpoint Inventory ──
    @Nested @DisplayName("J. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test @DisplayName("46 = 26 POST + 20 GET")
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
            assertThat(post).isEqualTo(26);
            assertThat(get).isEqualTo(20);
        }
        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }
}
