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
 * FP-039 — Multi-Clip Synthetic Generation Dry Run.
 */
@DisplayName("FP-039 — Multi-Clip Synthetic Generation")
class TurkuMultiClipGenerationTest {

    private TurkuMultiClipGenerationHarness harness;
    private TurkuSceneRenderPlanService planService;
    private TurkuMultiSceneRenderOrchestrationService orchService;

    @BeforeEach
    void setUp() {
        harness = new TurkuMultiClipGenerationHarness();
        planService = new TurkuSceneRenderPlanService();
        orchService = new TurkuMultiSceneRenderOrchestrationService();
    }

    private TurkuMultiSceneRenderJobPlan readyJob(int sceneCount) {
        var plans = new ArrayList<TurkuSceneRenderPlan>();
        for (int i = 1; i <= sceneCount; i++)
            plans.add(planService.validate("scene-" + i, "plan-" + i, i, 2, 24, 640, 360,
                    List.of(new TurkuSceneRenderArtifactExpectation(
                            TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true))));
        return orchService.buildJobPlan("job-test", plans);
    }

    // ── A. Fake Multi-Clip Generation ──
    @Nested @DisplayName("A. Fake Multi-Clip Generation")
    class FakeGeneration {
        @Test @DisplayName("3-scene fake → GENERATED_CLIPS_VERIFIED")
        void threeScenes() {
            var job = readyJob(3);
            assertThat(job.getStatus()).isEqualTo(TurkuMultiSceneRenderJobStatus.READY_FOR_LOCAL_MANUAL_RENDER);
            var result = harness.fakeGenerate(job);
            assertThat(result.getStatus()).isEqualTo(TurkuMultiClipGenerationStatus.GENERATED_CLIPS_VERIFIED);
            assertThat(result.getSceneCount()).isEqualTo(3);
            assertThat(result.getGeneratedClipCount()).isEqualTo(3);
            assertThat(result.getVerifiedClipCount()).isEqualTo(3);
            assertThat(result.getRejectedClipCount()).isEqualTo(0);
            assertThat(result.getOpaqueClipRefs()).hasSize(3);
            assertThat(result.isInternalOnly()).isTrue();
            assertThat(result.isRealExecutionPerformed()).isFalse();
        }
        @Test @DisplayName("2-scene fake verified through FP-035")
        void fakeVerified() {
            var job = readyJob(2);
            var result = harness.fakeGenerate(job);
            var lcService = new TurkuRenderArtifactLifecycleService();
            for (var ref : result.getOpaqueClipRefs()) {
                var report = lcService.verify(ref, "mp4", 2, 24, 640, 360, 1024 * 1024);
                assertThat(report.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.PASSED);
            }
        }
        @Test @DisplayName("fake clips attach to FP-036 bundle")
        void attachToBundle() {
            var job = readyJob(2);
            var result = harness.fakeGenerate(job);
            var bundleService = new TurkuRenderBundleManifestService();
            var lcService = new TurkuRenderArtifactLifecycleService();
            // Use first clip as primary
            var report = lcService.verify(result.getOpaqueClipRefs().get(0), "mp4", 2, 24, 640, 360, 1024 * 1024);
            var bundleRef = TurkuRenderBundleArtifactRef.fromLifecycleReport(report,
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP);
            var bundle = bundleService.build("bundle-test", List.of(bundleRef));
            assertThat(bundle.getStatus()).isEqualTo(TurkuRenderBundleStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
        }
    }

    // ── B. Harness Disabled by Default ──
    @Nested @DisplayName("B. Harness Disabled by Default")
    class Disabled {
        @Test @DisplayName("disabled → MANUAL_FLAG_NOT_ENABLED")
        void disabled() {
            var result = harness.attempt(readyJob(1), TurkuFfmpegBinaryPolicy.TEST_FIXTURE_FFMPEG);
            assertThat(result.getStatus()).isEqualTo(TurkuMultiClipGenerationStatus.MANUAL_FLAG_NOT_ENABLED);
            assertThat(result.isRealExecutionPerformed()).isFalse();
        }
    }

    // ── C. Gate: Process Disabled ──
    @Nested @DisplayName("C. Gate: Process Disabled")
    class ProcessDisabled {
        @Test @DisplayName("clip harness disabled → PROCESS_DISABLED")
        void processOff() {
            var clipHarness = new TurkuFfmpegClipGenerationHarness(new TurkuFfmpegLocalDevProcessRunner(), false);
            var h = new TurkuMultiClipGenerationHarness(clipHarness, null, null, true);
            var result = h.attempt(readyJob(1), TurkuFfmpegBinaryPolicy.TEST_FIXTURE_FFMPEG);
            assertThat(result.getStatus()).isEqualTo(TurkuMultiClipGenerationStatus.PROCESS_DISABLED);
        }
    }

    // ── D. Gate: FFmpeg Unavailable ──
    @Nested @DisplayName("D. Gate: FFmpeg Unavailable")
    class FfmpegUnavailable {
        @Test @DisplayName("null binary policy → FFMPEG_UNAVAILABLE")
        void noFfmpeg() {
            var clipHarness = new TurkuFfmpegClipGenerationHarness(new TurkuFfmpegLocalDevProcessRunner(), true);
            var h = new TurkuMultiClipGenerationHarness(clipHarness, null, null, true);
            var result = h.attempt(readyJob(1), null);
            assertThat(result.getStatus()).isEqualTo(TurkuMultiClipGenerationStatus.FFMPEG_UNAVAILABLE);
        }
        @Test @DisplayName("DISABLED policy → FFMPEG_UNAVAILABLE")
        void disabledPolicy() {
            var clipHarness = new TurkuFfmpegClipGenerationHarness(new TurkuFfmpegLocalDevProcessRunner(), true);
            var h = new TurkuMultiClipGenerationHarness(clipHarness, null, null, true);
            var result = h.attempt(readyJob(1), TurkuFfmpegBinaryPolicy.DISABLED_BINARY);
            assertThat(result.getStatus()).isEqualTo(TurkuMultiClipGenerationStatus.FFMPEG_UNAVAILABLE);
        }
    }

    // ── E. Gate: Job Not Ready ──
    @Nested @DisplayName("E. Gate: Job Not Ready")
    class JobNotReady {
        @Test @DisplayName("rejected job → JOB_NOT_READY")
        void rejected() {
            var badJob = orchService.buildJobPlan("bad", List.of());
            var clipHarness = new TurkuFfmpegClipGenerationHarness(new TurkuFfmpegLocalDevProcessRunner(), true);
            var h = new TurkuMultiClipGenerationHarness(clipHarness, null, null, true);
            var result = h.attempt(badJob, TurkuFfmpegBinaryPolicy.TEST_FIXTURE_FFMPEG);
            assertThat(result.getStatus()).isEqualTo(TurkuMultiClipGenerationStatus.JOB_NOT_READY);
        }
        @Test @DisplayName("null job → JOB_NOT_READY")
        void nullJob() {
            var clipHarness = new TurkuFfmpegClipGenerationHarness(new TurkuFfmpegLocalDevProcessRunner(), true);
            var h = new TurkuMultiClipGenerationHarness(clipHarness, null, null, true);
            var result = h.attempt(null, TurkuFfmpegBinaryPolicy.TEST_FIXTURE_FFMPEG);
            assertThat(result.getStatus()).isEqualTo(TurkuMultiClipGenerationStatus.JOB_NOT_READY);
        }
    }

    // ── F. Fake: Job Not Ready ──
    @Nested @DisplayName("F. Fake: Job Not Ready")
    class FakeNotReady {
        @Test @DisplayName("fake with bad job → JOB_NOT_READY")
        void fakeRejected() {
            var result = harness.fakeGenerate(orchService.buildJobPlan("x", List.of()));
            assertThat(result.getStatus()).isEqualTo(TurkuMultiClipGenerationStatus.JOB_NOT_READY);
        }
    }

    // ── G. Scene Request Validation ──
    @Nested @DisplayName("G. Scene Request Validation")
    class SceneReqValidation {
        @Test @DisplayName("valid request")
        void valid() {
            var req = new TurkuMultiClipGenerationSceneRequest("s", 1, 3, 24, 640, 360, "mp4");
            assertThat(req.isValid()).isTrue();
        }
        @Test @DisplayName("over duration → invalid")
        void overDuration() {
            assertThat(new TurkuMultiClipGenerationSceneRequest("s", 1, 6, 24, 640, 360, "mp4").isValid()).isFalse();
        }
        @Test @DisplayName("over fps → invalid")
        void overFps() {
            assertThat(new TurkuMultiClipGenerationSceneRequest("s", 1, 3, 31, 640, 360, "mp4").isValid()).isFalse();
        }
        @Test @DisplayName("over resolution → invalid")
        void overRes() {
            assertThat(new TurkuMultiClipGenerationSceneRequest("s", 1, 3, 24, 1281, 360, "mp4").isValid()).isFalse();
        }
    }

    // ── H. Aggregate Metadata ──
    @Nested @DisplayName("H. Aggregate Metadata")
    class Aggregates {
        @Test @DisplayName("fake aggregates are correct")
        void aggregates() {
            var result = harness.fakeGenerate(readyJob(3));
            assertThat(result.getTotalDurationSeconds()).isEqualTo(6); // 3×2s
            assertThat(result.getMaxWidth()).isEqualTo(640);
            assertThat(result.getMaxHeight()).isEqualTo(360);
            assertThat(result.getMaxFps()).isEqualTo(24);
            assertThat(result.getAggregateSizeBytes()).isEqualTo(3L * 1024 * 1024);
        }
    }

    // ── I. toString Safety ──
    @Nested @DisplayName("I. toString Safety")
    class ToStringSafety {
        @Test @DisplayName("result toString safe")
        void safe() {
            var str = harness.fakeGenerate(readyJob(2)).toString();
            assertThat(str).doesNotContain("filePath", "absolutePath", "stdout", "stderr",
                    "commandLine", "storageKey", "publicUrl", "base64", "rawLog",
                    "token", "JWT", "session", "principal", "lyrics", "scriptBody");
        }
    }

    // ── J. Enum Inventory ──
    @Nested @DisplayName("J. Enum Inventory")
    class Enums {
        @Test @DisplayName("11 generation statuses")
        void statuses() { assertThat(TurkuMultiClipGenerationStatus.values()).hasSize(11); }
        @Test @DisplayName("12 generation issue codes")
        void issues() { assertThat(TurkuMultiClipGenerationIssueCode.values()).hasSize(12); }
    }

    // ── K. ProcessBuilder Inventory ──
    @Nested @DisplayName("K. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("harness no ProcessBuilder")
        void harnessClean() {
            Set<String> types = Arrays.stream(TurkuMultiClipGenerationHarness.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("result no ProcessBuilder")
        void resultClean() {
            Set<String> types = Arrays.stream(TurkuMultiClipGenerationResult.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("runner only allowed")
        void runnerOnly() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("generateClip"))).isTrue();
        }
    }

    // ── L. Forbidden Exposure ──
    @Nested @DisplayName("L. Forbidden Exposure")
    class Forbidden {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
                "token", "jwt", "session", "principal", "authorizationHeader",
                "lyrics", "scriptBody", "promptText", "copyrightedContent");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuMultiClipGenerationStatus.class, TurkuMultiClipGenerationIssueCode.class,
                TurkuMultiClipGenerationSceneRequest.class, TurkuMultiClipGenerationResult.class,
                TurkuMultiClipGenerationHarness.class);

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

    // ── M. Endpoint Inventory ──
    @Nested @DisplayName("M. Endpoint Inventory (44 = 25 POST + 19 GET)")
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
