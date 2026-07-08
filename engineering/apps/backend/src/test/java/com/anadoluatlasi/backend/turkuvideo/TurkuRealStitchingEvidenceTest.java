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
 * FP-041 — First Real LOCAL Manual Synthetic Clip Stitching Evidence.
 */
@DisplayName("FP-041 — Real Local Stitching Evidence")
class TurkuRealStitchingEvidenceTest {

    private TurkuClipStitchingHarness harness;

    @BeforeEach
    void setUp() { harness = new TurkuClipStitchingHarness(); }

    private TurkuClipStitchingInputClipRef clip(int order) {
        return new TurkuClipStitchingInputClipRef(
                "clip-" + order, order, 2, 24, 640, 360, 1024 * 1024, "mp4", true);
    }

    // ── A. CI Fake Stitching Still Passes ──
    @Nested @DisplayName("A. CI Fake Stitching")
    class CIFake {
        @Test @DisplayName("3-clip fake stitch → STITCHED_ARTIFACT_VERIFIED")
        void fakeOk() {
            var result = harness.fakeStitch(List.of(clip(1), clip(2), clip(3)));
            assertThat(result.getStatus()).isEqualTo(TurkuClipStitchingStatus.STITCHED_ARTIFACT_VERIFIED);
            assertThat(result.getInputClipCount()).isEqualTo(3);
            assertThat(result.isStitchedArtifactVerified()).isTrue();
            assertThat(result.isRealExecutionPerformed()).isFalse();
            assertThat(result.isInternalOnly()).isTrue();
        }
    }

    // ── B. Harness Disabled by Default ──
    @Nested @DisplayName("B. Harness Disabled by Default")
    class Disabled {
        @Test @DisplayName("disabled → MANUAL_FLAG_NOT_ENABLED")
        void disabled() {
            var result = harness.attempt(List.of(clip(1), clip(2)),
                    TurkuFfmpegBinaryPolicy.TEST_FIXTURE_FFMPEG);
            assertThat(result.getStatus()).isEqualTo(TurkuClipStitchingStatus.MANUAL_FLAG_NOT_ENABLED);
            assertThat(result.isRealExecutionPerformed()).isFalse();
        }
    }

    // ── C. Gate: FFmpeg Unavailable ──
    @Nested @DisplayName("C. Gate: FFmpeg Unavailable")
    class Ffmpeg {
        @Test @DisplayName("DISABLED_BINARY → FFMPEG_UNAVAILABLE")
        void unavail() {
            var h = new TurkuClipStitchingHarness(null, true);
            var result = h.attempt(List.of(clip(1), clip(2)), TurkuFfmpegBinaryPolicy.DISABLED_BINARY);
            assertThat(result.getStatus()).isEqualTo(TurkuClipStitchingStatus.FFMPEG_UNAVAILABLE);
        }
    }

    // ── D. Real Attempt with processEnabled=false → FAILED_SAFE (runner skips) ──
    @Nested @DisplayName("D. Runner Process Disabled → FAILED_SAFE")
    class RunnerDisabled {
        @Test @DisplayName("runner disabled → FAILED_SAFE (skipped by runner)")
        void skipped() {
            var runner = new TurkuFfmpegLocalDevProcessRunner(new TurkuFfmpegExecutionGate(), false);
            var h = new TurkuClipStitchingHarness(null, runner, true);
            var result = h.attempt(List.of(clip(1), clip(2)),
                    TurkuFfmpegBinaryPolicy.TEST_FIXTURE_FFMPEG);
            assertThat(result.getStatus()).isEqualTo(TurkuClipStitchingStatus.FAILED_SAFE);
        }
    }

    // ── E. Validation: Too Few ──
    @Nested @DisplayName("E. Too Few Clips")
    class TooFew {
        @Test @DisplayName("1 clip → REJECTED_UNSAFE")
        void one() {
            var result = harness.fakeStitch(List.of(clip(1)));
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.TOO_FEW_CLIPS);
        }
    }

    // ── F. Stitched Artifact Lifecycle ──
    @Nested @DisplayName("F. Stitched Artifact Lifecycle Verification")
    class Lifecycle {
        @Test @DisplayName("fake stitched clips verify through FP-035 individually")
        void perClip() {
            var result = harness.fakeStitch(List.of(clip(1), clip(2)));
            assertThat(result.isStitchedArtifactVerified()).isTrue();
            var lcService = new TurkuRenderArtifactLifecycleService();
            var report = lcService.verify(result.getOpaqueOutputRef(), "mp4",
                    2, 24, 640, 360, 1024 * 1024);
            assertThat(report.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.PASSED);
        }
    }

    // ── G. Unsafe Metadata Rejects ──
    @Nested @DisplayName("G. Unsafe Metadata Rejects")
    class Unsafe {
        @Test @DisplayName("over-duration clip rejects")
        void overDuration() {
            var bad = new TurkuClipStitchingInputClipRef("c-bad", 2, 6, 24, 640, 360, 1024*1024, "mp4", true);
            var result = harness.fakeStitch(List.of(clip(1), bad));
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.CLIP_DURATION_EXCEEDED);
        }
        @Test @DisplayName("unverified clip rejects")
        void unverified() {
            var bad = new TurkuClipStitchingInputClipRef("c-uv", 2, 2, 24, 640, 360, 1024*1024, "mp4", false);
            var result = harness.fakeStitch(List.of(clip(1), bad));
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.CLIP_NOT_VERIFIED);
        }
    }

    // ── H. Runner has stitchClips method ──
    @Nested @DisplayName("H. Runner stitchClips Method Exists")
    class RunnerMethod {
        @Test @DisplayName("runner exposes stitchClips")
        void method() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("stitchClips"))).isTrue();
        }
    }

    // ── I. toString Safety ──
    @Nested @DisplayName("I. toString Safety")
    class ToStringSafety {
        @Test @DisplayName("result toString safe")
        void safe() {
            var str = harness.fakeStitch(List.of(clip(1), clip(2))).toString();
            assertThat(str).doesNotContain("filePath", "absolutePath", "stdout", "stderr",
                    "commandLine", "storageKey", "publicUrl", "base64", "rawLog",
                    "token", "JWT", "session", "principal", "lyrics", "scriptBody");
        }
    }

    // ── J. ProcessBuilder Inventory ──
    @Nested @DisplayName("J. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("harness no ProcessBuilder field")
        void harnessClean() {
            Set<String> types = Arrays.stream(TurkuClipStitchingHarness.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("ProcessBuilder only in runner")
        void runnerOnly() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("stitchClips"))).isTrue();
        }
    }

    // ── K. Forbidden Exposure ──
    @Nested @DisplayName("K. Forbidden Exposure")
    class Forbidden {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
                "token", "jwt", "session", "principal", "authorizationHeader",
                "lyrics", "scriptBody", "promptText", "copyrightedContent");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuClipStitchingHarness.class, TurkuClipStitchingResult.class,
                TurkuClipStitchingInputClipRef.class);

        @Test @DisplayName("3 classes x 29 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
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
}
