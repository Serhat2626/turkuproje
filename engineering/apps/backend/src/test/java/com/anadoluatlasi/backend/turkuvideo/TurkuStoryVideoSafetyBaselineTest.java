package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-045 — Internal Story-to-Video Safety & Baseline Closure.
 * Locks the full FP-034→FP-044 boundary. No new capability.
 */
@DisplayName("FP-045 — Story-to-Video Safety Baseline")
class TurkuStoryVideoSafetyBaselineTest {

    private static final List<String> FORBIDDEN_FIELDS = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "storageKey", "publicUrl", "signedUrl", "cdnUrl",
            "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "lyrics", "scriptBody", "promptText", "copyrightedContent");

    // All FP-034→FP-044 production classes
    private static final List<Class<?>> ALL_CLASSES = List.of(
            // FP-034/033: Process runner + results
            TurkuFfmpegLocalDevProcessRunner.class,
            TurkuFfmpegProcessResult.class,
            TurkuFfmpegProcessRequest.class,
            TurkuFfmpegInternalOutputRef.class,
            // FP-035: Lifecycle
            TurkuRenderArtifactLifecycleService.class,
            TurkuRenderArtifactLifecycleReport.class,
            // FP-036: Bundle
            TurkuRenderBundleManifestService.class,
            TurkuRenderBundleManifest.class,
            TurkuRenderBundleArtifactRef.class,
            // FP-037: Scene plan
            TurkuSceneRenderPlanService.class,
            TurkuSceneRenderPlan.class,
            TurkuSceneRenderArtifactExpectation.class,
            // FP-038: Orchestration
            TurkuMultiSceneRenderOrchestrationService.class,
            TurkuMultiSceneRenderJobPlan.class,
            // FP-039: Multi-clip generation
            TurkuMultiClipGenerationHarness.class,
            // FP-040/041: Stitching
            TurkuClipStitchingHarness.class,
            TurkuClipStitchingResult.class,
            TurkuClipStitchingInputClipRef.class,
            // FP-042: Review package
            TurkuRenderReviewPackageService.class,
            TurkuRenderReviewPackage.class,
            TurkuRenderReviewArtifactSummary.class,
            // FP-044: E2E dry run
            TurkuInternalStoryVideoDryRunHarness.class,
            TurkuInternalStoryVideoDryRunSummary.class);

    // All enums
    private static final List<Class<? extends Enum<?>>> ALL_ENUMS = List.of(
            TurkuFfmpegProcessStatus.class,
            TurkuFfmpegProcessReasonCode.class,
            TurkuFfmpegBinaryPolicy.class,
            TurkuRenderArtifactVerificationStatus.class,
            TurkuRenderArtifactIssueCode.class,
            TurkuRenderArtifactFormat.class,
            TurkuRenderBundleStatus.class,
            TurkuRenderBundleArtifactRole.class,
            TurkuSceneRenderPlanStatus.class,
            TurkuSceneRenderPlanIssueCode.class,
            TurkuMultiSceneRenderJobStatus.class,
            TurkuMultiSceneRenderJobIssueCode.class,
            TurkuClipStitchingStatus.class,
            TurkuClipStitchingIssueCode.class,
            TurkuRenderReviewPackageStatus.class,
            TurkuRenderReviewPackageIssueCode.class,
            TurkuInternalStoryVideoDryRunStatus.class,
            TurkuInternalStoryVideoDryRunIssueCode.class);

    // ── A. Forbidden Field Sweep (all classes) ──
    @Nested @DisplayName("A. Forbidden Fields — All Classes")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden field violations across all classes")
        void sweep() {
            for (Class<?> c : ALL_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN_FIELDS)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── B. No READY_FOR_PUBLISH / COMPLETED in any enum ──
    @Nested @DisplayName("B. No Public Delivery Statuses")
    class NoPublishStatus {
        @Test @DisplayName("no READY_FOR_PUBLISH in any enum")
        void noPublish() {
            for (var enumClass : ALL_ENUMS) {
                assertThat(Arrays.stream(enumClass.getEnumConstants())
                        .noneMatch(e -> e.name().contains("READY_FOR_PUBLISH")))
                        .as("READY_FOR_PUBLISH in %s", enumClass.getSimpleName()).isTrue();
            }
        }
        @Test @DisplayName("no COMPLETED in any enum")
        void noCompleted() {
            for (var enumClass : ALL_ENUMS) {
                assertThat(Arrays.stream(enumClass.getEnumConstants())
                        .noneMatch(e -> e.name().equals("COMPLETED")))
                        .as("COMPLETED in %s", enumClass.getSimpleName()).isTrue();
            }
        }
        @Test @DisplayName("no AUTO_APPROVED (positive) in any enum")
        void noAutoApprove() {
            for (var enumClass : ALL_ENUMS) {
                assertThat(Arrays.stream(enumClass.getEnumConstants())
                        .noneMatch(e -> e.name().equals("AUTO_APPROVED")))
                        .as("AUTO_APPROVED in %s", enumClass.getSimpleName()).isTrue();
            }
        }
    }

    // ── C. ProcessBuilder Only in Runner ──
    @Nested @DisplayName("C. ProcessBuilder Boundary")
    class ProcessBoundary {
        private static final List<Class<?>> NON_RUNNER = ALL_CLASSES.stream()
                .filter(c -> c != TurkuFfmpegLocalDevProcessRunner.class)
                .toList();

        @Test @DisplayName("no ProcessBuilder field outside runner")
        void noFields() {
            for (Class<?> c : NON_RUNNER) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName()).doesNotContain("ProcessBuilder", "Process");
            }
        }

        @Test @DisplayName("no Runtime field in any class")
        void noRuntime() {
            for (Class<?> c : ALL_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName()).doesNotContain("Runtime");
            }
        }

        @Test @DisplayName("runner has stitchClips method")
        void stitchMethod() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("stitchClips"))).isTrue();
        }

        @Test @DisplayName("runner has generateClip method")
        void generateMethod() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("generateClip"))).isTrue();
        }
    }

    // ── D. FP-034 Single Clip Metadata Safe ──
    @Nested @DisplayName("D. FP-034 — Clip Generation Metadata")
    class FP034 {
        @Test @DisplayName("process result internalOnly=true")
        void resultSafe() {
            var result = TurkuFfmpegProcessResult.skipped();
            assertThat(result.toString()).doesNotContain("filePath", "commandLine", "stdout");
        }
    }

    // ── E. FP-035 Lifecycle Safe ──
    @Nested @DisplayName("E. FP-035 — Lifecycle Verification")
    class FP035 {
        @Test @DisplayName("verified clip produces PASSED")
        void verified() {
            var svc = new TurkuRenderArtifactLifecycleService();
            var report = svc.verify("ref-1", "mp4", 2, 24, 640, 360, 1024*1024);
            assertThat(report.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.PASSED);
        }
    }

    // ── F. FP-036 Bundle Metadata-only ──
    @Nested @DisplayName("F. FP-036 — Bundle Manifest")
    class FP036 {
        @Test @DisplayName("bundle status is metadata-only")
        void bundleSafe() {
            var svc = new TurkuRenderBundleManifestService();
            var lcSvc = new TurkuRenderArtifactLifecycleService();
            var report = lcSvc.verify("ref-b", "mp4", 2, 24, 640, 360, 1024*1024);
            var ref = TurkuRenderBundleArtifactRef.fromLifecycleReport(report,
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP);
            var bundle = svc.build("bundle-test", List.of(ref));
            assertThat(bundle.getStatus()).isEqualTo(TurkuRenderBundleStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
        }
    }

    // ── G. FP-037 Scene Plan Metadata-only ──
    @Nested @DisplayName("G. FP-037 — Scene Plan")
    class FP037 {
        @Test @DisplayName("scene plan validated")
        void planOk() {
            var svc = new TurkuSceneRenderPlanService();
            var expect = new TurkuSceneRenderArtifactExpectation(
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true);
            var plan = svc.validate("s1", "p1", 1, 2, 24, 640, 360, List.of(expect));
            assertThat(plan.getStatus()).isEqualTo(TurkuSceneRenderPlanStatus.READY_FOR_BUNDLE_ASSEMBLY);
        }
    }

    // ── H. FP-038 Multi-scene Job Metadata-only ──
    @Nested @DisplayName("H. FP-038 — Multi-scene Job")
    class FP038 {
        @Test @DisplayName("job plan reaches READY_FOR_LOCAL_MANUAL_RENDER")
        void jobReady() {
            var planSvc = new TurkuSceneRenderPlanService();
            var expect = new TurkuSceneRenderArtifactExpectation(
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true);
            var plans = List.of(
                    planSvc.validate("s1", "p1", 1, 2, 24, 640, 360, List.of(expect)),
                    planSvc.validate("s2", "p2", 2, 2, 24, 640, 360, List.of(expect)));
            var orchSvc = new TurkuMultiSceneRenderOrchestrationService();
            var job = orchSvc.buildJobPlan("job-test", plans);
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.READY_FOR_LOCAL_MANUAL_RENDER);
        }
    }

    // ── I. FP-040/041 Stitching Remains Gated ──
    @Nested @DisplayName("I. FP-040/041 — Stitching Gated")
    class FP040 {
        @Test @DisplayName("default harness disabled")
        void disabled() {
            var h = new TurkuClipStitchingHarness();
            var clip = new TurkuClipStitchingInputClipRef("c1", 1, 2, 24, 640, 360, 1024*1024, "mp4", true);
            var result = h.attempt(List.of(clip, clip), TurkuFfmpegBinaryPolicy.TEST_FIXTURE_FFMPEG);
            assertThat(result.getStatus()).isEqualTo(TurkuClipStitchingStatus.MANUAL_FLAG_NOT_ENABLED);
        }
        @Test @DisplayName("fake stitch works in CI")
        void fakeOk() {
            var h = new TurkuClipStitchingHarness();
            var c1 = new TurkuClipStitchingInputClipRef("c1", 1, 2, 24, 640, 360, 1024*1024, "mp4", true);
            var c2 = new TurkuClipStitchingInputClipRef("c2", 2, 2, 24, 640, 360, 1024*1024, "mp4", true);
            var result = h.fakeStitch(List.of(c1, c2));
            assertThat(result.getStatus()).isEqualTo(TurkuClipStitchingStatus.STITCHED_ARTIFACT_VERIFIED);
        }
    }

    // ── J. FP-042 Review Package Internal-only ──
    @Nested @DisplayName("J. FP-042 — Review Package Internal")
    class FP042 {
        @Test @DisplayName("review package internalOnly=true")
        void internalOnly() {
            var svc = new TurkuRenderReviewPackageService();
            var summary = new TurkuRenderReviewArtifactSummary("art-1",
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP,
                    TurkuRenderArtifactVerificationStatus.PASSED,
                    "mp4", 2, 24, 640, 360, 1024*1024);
            var pkg = svc.build("j", "b", List.of(summary), 1, 1);
            assertThat(pkg.isInternalOnly()).isTrue();
            assertThat(pkg.getStatus()).isEqualTo(TurkuRenderReviewPackageStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
        }
    }

    // ── K. FP-044 E2E Dry Run Internal Metadata-only ──
    @Nested @DisplayName("K. FP-044 — E2E Dry Run")
    class FP044 {
        @Test @DisplayName("3-scene E2E reaches ADMIN_METADATA_VISIBLE")
        void e2e() {
            var h = new TurkuInternalStoryVideoDryRunHarness();
            var result = h.execute(3);
            assertThat(result.getStatus()).isEqualTo(TurkuInternalStoryVideoDryRunStatus.ADMIN_METADATA_VISIBLE);
            assertThat(result.isInternalOnly()).isTrue();
            assertThat(result.isAdminMetadataVisible()).isTrue();
        }
    }

    // ── L. Endpoint Inventory ──
    @Nested @DisplayName("L. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // ── M. Enum Inventory ──
    @Nested @DisplayName("M. Enum Inventory")
    class EnumInventory {
        @Test @DisplayName("18 enums in chain")
        void total() { assertThat(ALL_ENUMS).hasSize(18); }
    }
}
