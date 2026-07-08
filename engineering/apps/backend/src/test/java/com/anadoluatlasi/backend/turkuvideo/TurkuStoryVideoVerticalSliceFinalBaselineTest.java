package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-048 — Internal Türkü Story-to-Video Vertical Slice Final Baseline.
 * Locks FP-034→FP-047. No new capability.
 */
@DisplayName("FP-048 — Vertical Slice Final Baseline")
class TurkuStoryVideoVerticalSliceFinalBaselineTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "storageKey", "publicUrl", "signedUrl", "cdnUrl",
            "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "lyrics", "scriptBody", "promptText", "copyrightedContent");

    // FP-046 + FP-047 classes
    private static final List<Class<?>> FP046_047_CLASSES = List.of(
            TurkuStoryboardRenderAdapterStatus.class,
            TurkuStoryboardRenderAdapterIssueCode.class,
            TurkuStoryboardSceneRenderInput.class,
            TurkuStoryboardRenderPlanAdapterResult.class,
            TurkuStoryboardRenderPlanAdapterService.class,
            TurkuStoryboardInternalRenderChainStatus.class,
            TurkuStoryboardInternalRenderChainIssueCode.class,
            TurkuStoryboardInternalRenderChainFixture.class,
            TurkuStoryboardInternalRenderChainSummary.class,
            TurkuStoryboardInternalRenderChainHarness.class);

    // FP-046/047 enums
    private static final List<Class<? extends Enum<?>>> FP046_047_ENUMS = List.of(
            TurkuStoryboardRenderAdapterStatus.class,
            TurkuStoryboardRenderAdapterIssueCode.class,
            TurkuStoryboardInternalRenderChainStatus.class,
            TurkuStoryboardInternalRenderChainIssueCode.class);

    // ── A. Forbidden Field Sweep (FP-046/047) ──
    @Nested @DisplayName("A. Forbidden Fields — FP-046/047")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden field violations across 10 classes")
        void sweep() {
            for (Class<?> c : FP046_047_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── B. No Public Delivery in FP-046/047 Enums ──
    @Nested @DisplayName("B. No Public Delivery Statuses")
    class NoPublish {
        @Test @DisplayName("no READY_FOR_PUBLISH")
        void noPublish() {
            for (var e : FP046_047_ENUMS)
                assertThat(Arrays.stream(e.getEnumConstants())
                        .noneMatch(v -> v.name().contains("PUBLISH")))
                        .as(e.getSimpleName()).isTrue();
        }
        @Test @DisplayName("no COMPLETED")
        void noCompleted() {
            for (var e : FP046_047_ENUMS)
                assertThat(Arrays.stream(e.getEnumConstants())
                        .noneMatch(v -> v.name().equals("COMPLETED")))
                        .as(e.getSimpleName()).isTrue();
        }
        @Test @DisplayName("no AUTO_APPROVED")
        void noAutoApproved() {
            for (var e : FP046_047_ENUMS)
                assertThat(Arrays.stream(e.getEnumConstants())
                        .noneMatch(v -> v.name().equals("AUTO_APPROVED")))
                        .as(e.getSimpleName()).isTrue();
        }
    }

    // ── C. ProcessBuilder Boundary ──
    @Nested @DisplayName("C. ProcessBuilder Boundary")
    class ProcessBoundary {
        @Test @DisplayName("no ProcessBuilder in FP-046/047 classes")
        void noProcess() {
            for (Class<?> c : FP046_047_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName())
                        .doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // ── D. Storyboard Adapter Metadata-only ──
    @Nested @DisplayName("D. Storyboard Adapter Metadata-only")
    class AdapterSafe {
        @Test @DisplayName("valid adapter → READY_FOR_INTERNAL_RENDER_CHAIN")
        void adapts() {
            var svc = new TurkuStoryboardRenderPlanAdapterService();
            var input = new TurkuStoryboardSceneRenderInput("s1", 1, 2, 24, 640, 360, "mp4");
            var result = svc.adapt("sb", "st", List.of(input));
            assertThat(result.getStatus()).isEqualTo(TurkuStoryboardRenderAdapterStatus.READY_FOR_INTERNAL_RENDER_CHAIN);
            assertThat(result.isInternalOnly()).isTrue();
        }
        @Test @DisplayName("adapter result toString safe")
        void toStringSafe() {
            var svc = new TurkuStoryboardRenderPlanAdapterService();
            var input = new TurkuStoryboardSceneRenderInput("s1", 1, 2, 24, 640, 360, "mp4");
            var str = svc.adapt("sb", "st", List.of(input)).toString();
            assertThat(str).contains("internalOnly=true");
            for (String f : FORBIDDEN) assertThat(str).doesNotContain(f);
        }
    }

    // ── E. Türkü Chain Fixture → ADMIN_METADATA_VISIBLE ──
    @Nested @DisplayName("E. Türkü Chain E2E")
    class ChainE2E {
        @Test @DisplayName("3-scene turkuFixture → ADMIN_METADATA_VISIBLE")
        void e2e() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(3);
            var harness = new TurkuStoryboardInternalRenderChainHarness();
            var result = harness.execute(fixture);
            assertThat(result.getStatus()).isEqualTo(TurkuStoryboardInternalRenderChainStatus.ADMIN_METADATA_VISIBLE);
            assertThat(result.isInternalOnly()).isTrue();
            assertThat(result.isAdminMetadataVisible()).isTrue();
            assertThat(result.isStitchedArtifactPresent()).isTrue();
            assertThat(result.getReviewPackageStatus()).isEqualTo(TurkuRenderReviewPackageStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
        }
        @Test @DisplayName("chain summary toString safe")
        void toStringSafe() {
            var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(2);
            var str = new TurkuStoryboardInternalRenderChainHarness().execute(fixture).toString();
            assertThat(str).contains("internalOnly=true");
            for (String f : FORBIDDEN) assertThat(str).doesNotContain(f);
        }
    }

    // ── F. Enum Size Inventory ──
    @Nested @DisplayName("F. Enum Inventory")
    class EnumInventory {
        @Test @DisplayName("FP-046 adapter: 6 statuses")
        void adapterStatuses() { assertThat(TurkuStoryboardRenderAdapterStatus.values()).hasSize(6); }
        @Test @DisplayName("FP-046 adapter: 14 issue codes")
        void adapterIssues() { assertThat(TurkuStoryboardRenderAdapterIssueCode.values()).hasSize(14); }
        @Test @DisplayName("FP-047 chain: 10 statuses")
        void chainStatuses() { assertThat(TurkuStoryboardInternalRenderChainStatus.values()).hasSize(10); }
        @Test @DisplayName("FP-047 chain: 10 issue codes")
        void chainIssues() { assertThat(TurkuStoryboardInternalRenderChainIssueCode.values()).hasSize(10); }
    }

    // ── G. Input Scene Safety ──
    @Nested @DisplayName("G. Input Scene Safety")
    class InputSafe {
        @Test @DisplayName("scene input has no forbidden fields")
        void fields() {
            Set<String> fields = Arrays.stream(TurkuStoryboardSceneRenderInput.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) assertThat(fields).doesNotContain(f);
        }
        @Test @DisplayName("scene input toString safe")
        void toStr() {
            var str = new TurkuStoryboardSceneRenderInput("s1", 1, 2, 24, 640, 360, "mp4").toString();
            for (String f : FORBIDDEN) assertThat(str).doesNotContain(f);
        }
    }

    // ── H. Endpoint Inventory ──
    @Nested @DisplayName("H. Endpoint Inventory (46 = 26 POST + 20 GET)")
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
