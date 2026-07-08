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
 * FP-047 — Storyboard Internal Render Chain E2E Fixture.
 */
@DisplayName("FP-047 — Storyboard Internal Render Chain")
class TurkuStoryboardInternalRenderChainTest {

    private TurkuStoryboardInternalRenderChainHarness harness;

    @BeforeEach
    void setUp() { harness = new TurkuStoryboardInternalRenderChainHarness(); }

    // ── A. Full Chain → ADMIN_METADATA_VISIBLE ──
    @Nested @DisplayName("A. Full Türkü Chain")
    class FullChain {
        @Test @DisplayName("3-scene Türkü fixture → ADMIN_METADATA_VISIBLE")
        void threeScenes() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(3);
            var result = harness.execute(fixture);
            assertThat(result.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.ADMIN_METADATA_VISIBLE);
            assertThat(result.getSceneCount()).isEqualTo(3);
            assertThat(result.isStitchedArtifactPresent()).isTrue();
            assertThat(result.getReviewPackageStatus()).isEqualTo(TurkuRenderReviewPackageStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
            assertThat(result.isAdminMetadataVisible()).isTrue();
            assertThat(result.isInternalOnly()).isTrue();
            assertThat(result.getIssueCodes()).isEmpty();
        }

        @Test @DisplayName("1-scene fixture succeeds")
        void oneScene() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(1);
            var result = harness.execute(fixture);
            assertThat(result.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.ADMIN_METADATA_VISIBLE);
        }

        @Test @DisplayName("5-scene fixture succeeds")
        void fiveScenes() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(5);
            var result = harness.execute(fixture);
            assertThat(result.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.ADMIN_METADATA_VISIBLE);
            assertThat(result.getSceneCount()).isEqualTo(5);
            assertThat(result.getGeneratedClipCount()).isEqualTo(5);
        }
    }

    // ── B. Adapter Failure ──
    @Nested @DisplayName("B. Adapter Failure")
    class AdapterFail {
        @Test @DisplayName("null fixture → REJECTED_UNSAFE")
        void nullFixture() {
            var result = harness.execute(null);
            assertThat(result.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE);
            assertThat(result.getIssueCodes()).contains(TurkuStoryboardInternalRenderChainIssueCode.ADAPTER_FAILED);
        }

        @Test @DisplayName("empty scenes → REJECTED_UNSAFE")
        void emptyScenes() {
            var fixture = new TurkuStoryboardInternalRenderChainFixture("sb", "st", List.of());
            var result = harness.execute(fixture);
            assertThat(result.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE);
        }

        @Test @DisplayName("missing storyboard ref → REJECTED_UNSAFE")
        void missingRef() {
            var input = new TurkuStoryboardSceneRenderInput("s1", 1, 2, 24, 640, 360, "mp4");
            var fixture = new TurkuStoryboardInternalRenderChainFixture(null, "st", List.of(input));
            var result = harness.execute(fixture);
            assertThat(result.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE);
        }
    }

    // ── C. Scene Plan Failure ──
    @Nested @DisplayName("C. Scene Plan Failure")
    class ScenePlanFail {
        @Test @DisplayName("over-duration scene → REJECTED_UNSAFE")
        void overDuration() {
            var input = new TurkuStoryboardSceneRenderInput("s1", 1, 99, 24, 640, 360, "mp4");
            var fixture = new TurkuStoryboardInternalRenderChainFixture("sb", "st", List.of(input));
            var result = harness.execute(fixture);
            assertThat(result.getStatus()).isIn(
                    TurkuStoryboardInternalRenderChainStatus.REJECTED_UNSAFE,
                    TurkuStoryboardInternalRenderChainStatus.FAILED_SAFE);
        }
    }

    // ── D. Metadata Aggregation ──
    @Nested @DisplayName("D. Metadata Aggregation")
    class Metadata {
        @Test @DisplayName("aggregates correct")
        void aggregates() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(3);
            var result = harness.execute(fixture);
            assertThat(result.getTotalDurationSeconds()).isGreaterThan(0);
            assertThat(result.getMaxWidth()).isEqualTo(640);
            assertThat(result.getMaxHeight()).isEqualTo(360);
            assertThat(result.getMaxFps()).isEqualTo(24);
            assertThat(result.getTotalSizeBytes()).isGreaterThan(0);
        }

        @Test @DisplayName("opaque refs present")
        void refs() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(2);
            var result = harness.execute(fixture);
            assertThat(result.getOpaqueChainRef()).startsWith("chain:");
            assertThat(result.getOpaqueStoryboardRef()).isNotNull();
            assertThat(result.getOpaqueJobRef()).isNotNull();
            assertThat(result.getOpaqueBundleRef()).isNotNull();
            assertThat(result.getOpaqueReviewPackageRef()).isNotNull();
        }
    }

    // ── E. toString Safety ──
    @Nested @DisplayName("E. toString Safety")
    class ToStringSafety {
        @Test @DisplayName("safe toString")
        void safe() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(2);
            var str = harness.execute(fixture).toString();
            assertThat(str).doesNotContain("filePath", "absolutePath", "stdout", "stderr",
                    "commandLine", "storageKey", "publicUrl", "base64", "rawLog",
                    "token", "JWT", "session", "principal", "lyrics", "scriptBody");
            assertThat(str).contains("internalOnly=true");
        }
    }

    // ── F. Enum Inventory ──
    @Nested @DisplayName("F. Enum Inventory")
    class Enums {
        @Test @DisplayName("10 chain statuses")
        void statuses() { assertThat(TurkuStoryboardInternalRenderChainStatus.values()).hasSize(10); }
        @Test @DisplayName("10 chain issue codes")
        void issues() { assertThat(TurkuStoryboardInternalRenderChainIssueCode.values()).hasSize(10); }
    }

    // ── G. Forbidden Exposure ──
    @Nested @DisplayName("G. Forbidden Exposure")
    class Forbidden {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
                "token", "jwt", "session", "principal", "authorizationHeader",
                "lyrics", "scriptBody", "promptText", "copyrightedContent");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuStoryboardInternalRenderChainStatus.class,
                TurkuStoryboardInternalRenderChainIssueCode.class,
                TurkuStoryboardInternalRenderChainFixture.class,
                TurkuStoryboardInternalRenderChainSummary.class,
                TurkuStoryboardInternalRenderChainHarness.class);

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

    // ── H. ProcessBuilder Inventory ──
    @Nested @DisplayName("H. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("harness no ProcessBuilder")
        void clean() {
            Set<String> types = Arrays.stream(TurkuStoryboardInternalRenderChainHarness.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
    }

    // ── I. No Public Delivery Statuses ──
    @Nested @DisplayName("I. No Public Delivery Statuses")
    class NoPublish {
        @Test @DisplayName("no READY_FOR_PUBLISH")
        void noPublish() {
            assertThat(Arrays.stream(TurkuStoryboardInternalRenderChainStatus.values())
                    .noneMatch(s -> s.name().contains("PUBLISH"))).isTrue();
        }
        @Test @DisplayName("no COMPLETED")
        void noCompleted() {
            assertThat(Arrays.stream(TurkuStoryboardInternalRenderChainStatus.values())
                    .noneMatch(s -> s.name().equals("COMPLETED"))).isTrue();
        }
        @Test @DisplayName("no AUTO_APPROVED")
        void noAutoApproved() {
            assertThat(Arrays.stream(TurkuStoryboardInternalRenderChainStatus.values())
                    .noneMatch(s -> s.name().equals("AUTO_APPROVED"))).isTrue();
        }
    }

    // ── J. Fixture Factory ──
    @Nested @DisplayName("J. Fixture Factory")
    class Factory {
        @Test @DisplayName("turkuFixture creates correct scenes")
        void factory() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(4);
            assertThat(fixture.getSceneCount()).isEqualTo(4);
            assertThat(fixture.getOpaqueStoryboardRef()).contains("turku");
            assertThat(fixture.getOpaqueStoryRef()).contains("turku");
            assertThat(fixture.getSceneInputs()).allMatch(
                    s -> s.getOpaqueSceneRef().contains("turku"));
        }
    }

    // ── K. Endpoint Inventory ──
    @Nested @DisplayName("K. Endpoint Inventory (46 = 26 POST + 20 GET)")
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
