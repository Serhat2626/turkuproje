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
 * FP-040 — Local-only Synthetic Clip Stitching Experiment.
 */
@DisplayName("FP-040 — Clip Stitching Experiment")
class TurkuClipStitchingTest {

    private TurkuClipStitchingHarness harness;

    @BeforeEach
    void setUp() { harness = new TurkuClipStitchingHarness(); }

    private TurkuClipStitchingInputClipRef clip(int order) {
        return new TurkuClipStitchingInputClipRef(
                "clip-" + order, order, 3, 24, 640, 360, 1024 * 1024, "mp4", true);
    }

    private List<TurkuClipStitchingInputClipRef> clips(int count) {
        var list = new ArrayList<TurkuClipStitchingInputClipRef>();
        for (int i = 1; i <= count; i++) list.add(clip(i));
        return list;
    }

    // ── A. Fake Stitch 3 Clips ──
    @Nested @DisplayName("A. Fake Stitch 3 Verified Clips")
    class FakeStitch {
        @Test @DisplayName("3 clips → STITCHED_ARTIFACT_VERIFIED")
        void ok() {
            var result = harness.fakeStitch(clips(3));
            assertThat(result.getStatus()).isEqualTo(TurkuClipStitchingStatus.STITCHED_ARTIFACT_VERIFIED);
            assertThat(result.getInputClipCount()).isEqualTo(3);
            assertThat(result.getTotalDurationSeconds()).isEqualTo(9);
            assertThat(result.getMaxWidth()).isEqualTo(640);
            assertThat(result.getMaxFps()).isEqualTo(24);
            assertThat(result.isInternalOnly()).isTrue();
            assertThat(result.isRealExecutionPerformed()).isFalse();
            assertThat(result.isStitchedArtifactVerified()).isTrue();
            assertThat(result.getOpaqueOutputRef()).isNotNull();
        }
    }

    // ── B. Harness Disabled ──
    @Nested @DisplayName("B. Harness Disabled by Default")
    class Disabled {
        @Test @DisplayName("disabled → MANUAL_FLAG_NOT_ENABLED")
        void disabled() {
            var result = harness.attempt(clips(3), TurkuFfmpegBinaryPolicy.TEST_FIXTURE_FFMPEG);
            assertThat(result.getStatus()).isEqualTo(TurkuClipStitchingStatus.MANUAL_FLAG_NOT_ENABLED);
        }
    }

    // ── C. Gate: FFmpeg Unavailable ──
    @Nested @DisplayName("C. Gate: FFmpeg Unavailable")
    class FfmpegGate {
        @Test @DisplayName("null → FFMPEG_UNAVAILABLE")
        void nul() {
            var h = new TurkuClipStitchingHarness(null, true);
            assertThat(h.attempt(clips(3), null).getStatus())
                    .isEqualTo(TurkuClipStitchingStatus.FFMPEG_UNAVAILABLE);
        }
        @Test @DisplayName("DISABLED_BINARY → FFMPEG_UNAVAILABLE")
        void dis() {
            var h = new TurkuClipStitchingHarness(null, true);
            assertThat(h.attempt(clips(3), TurkuFfmpegBinaryPolicy.DISABLED_BINARY).getStatus())
                    .isEqualTo(TurkuClipStitchingStatus.FFMPEG_UNAVAILABLE);
        }
    }

    // ── D. Too Few Clips ──
    @Nested @DisplayName("D. Too Few Clips")
    class TooFew {
        @Test @DisplayName("1 clip → REJECTED_UNSAFE")
        void one() {
            var result = harness.fakeStitch(clips(1));
            assertThat(result.getStatus()).isEqualTo(TurkuClipStitchingStatus.REJECTED_UNSAFE);
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.TOO_FEW_CLIPS);
        }
        @Test @DisplayName("empty → REJECTED_UNSAFE")
        void empty() {
            var result = harness.fakeStitch(List.of());
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.TOO_FEW_CLIPS);
        }
    }

    // ── E. Too Many Clips ──
    @Nested @DisplayName("E. Too Many Clips")
    class TooMany {
        @Test @DisplayName("11 clips → REJECTED_UNSAFE")
        void eleven() {
            var result = harness.fakeStitch(clips(11));
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.TOO_MANY_CLIPS);
        }
    }

    // ── F. Unverified Clip ──
    @Nested @DisplayName("F. Unverified Clip")
    class Unverified {
        @Test @DisplayName("unverified clip → REJECTED_UNSAFE")
        void unverified() {
            var bad = new TurkuClipStitchingInputClipRef("clip-2", 2, 3, 24, 640, 360, 1024*1024, "mp4", false);
            var result = harness.fakeStitch(List.of(clip(1), bad));
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.CLIP_NOT_VERIFIED);
        }
    }

    // ── G. Duplicate Clip Ref ──
    @Nested @DisplayName("G. Duplicate Clip Ref")
    class DupRef {
        @Test @DisplayName("duplicate ref → REJECTED_UNSAFE")
        void dup() {
            var result = harness.fakeStitch(List.of(clip(1), clip(1)));
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.DUPLICATE_CLIP_REF);
        }
    }

    // ── H. Over Duration ──
    @Nested @DisplayName("H. Over Duration")
    class Duration {
        @Test @DisplayName("clip > 5s → CLIP_DURATION_EXCEEDED")
        void perClip() {
            var over = new TurkuClipStitchingInputClipRef("c-over", 2, 6, 24, 640, 360, 1024*1024, "mp4", true);
            var result = harness.fakeStitch(List.of(clip(1), over));
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.CLIP_DURATION_EXCEEDED);
        }
    }

    // ── I. Over FPS ──
    @Nested @DisplayName("I. Over FPS")
    class Fps {
        @Test @DisplayName("31fps → CLIP_FPS_EXCEEDED")
        void overFps() {
            var over = new TurkuClipStitchingInputClipRef("c-fps", 2, 3, 31, 640, 360, 1024*1024, "mp4", true);
            var result = harness.fakeStitch(List.of(clip(1), over));
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.CLIP_FPS_EXCEEDED);
        }
    }

    // ── J. Over Resolution ──
    @Nested @DisplayName("J. Over Resolution")
    class Resolution {
        @Test @DisplayName("1281 width → CLIP_RESOLUTION_EXCEEDED")
        void overW() {
            var over = new TurkuClipStitchingInputClipRef("c-w", 2, 3, 24, 1281, 360, 1024*1024, "mp4", true);
            var result = harness.fakeStitch(List.of(clip(1), over));
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.CLIP_RESOLUTION_EXCEEDED);
        }
    }

    // ── K. Unsupported Format ──
    @Nested @DisplayName("K. Unsupported Format")
    class Format {
        @Test @DisplayName("avi → FORMAT_UNSUPPORTED")
        void avi() {
            var bad = new TurkuClipStitchingInputClipRef("c-avi", 2, 3, 24, 640, 360, 1024*1024, "avi", true);
            var result = harness.fakeStitch(List.of(clip(1), bad));
            assertThat(result.getIssueCodes()).contains(TurkuClipStitchingIssueCode.FORMAT_UNSUPPORTED);
        }
    }

    // ── L. FP-035 Verification ──
    @Nested @DisplayName("L. FP-035 Lifecycle Verification")
    class Lifecycle {
        @Test @DisplayName("stitched artifact verified through FP-035")
        void verified() {
            // Use 2-clip × 2s = 4s total, within FP-035's 5s per-artifact limit
            var c1 = new TurkuClipStitchingInputClipRef("c-1", 1, 2, 24, 640, 360, 1024*1024, "mp4", true);
            var c2 = new TurkuClipStitchingInputClipRef("c-2", 2, 2, 24, 640, 360, 1024*1024, "mp4", true);
            var result = harness.fakeStitch(List.of(c1, c2));
            assertThat(result.isStitchedArtifactVerified()).isTrue();
            // Also verify output ref through lifecycle service directly
            var lcService = new TurkuRenderArtifactLifecycleService();
            var report = lcService.verify(result.getOpaqueOutputRef(), "mp4",
                    4, 24, 640, 360, 2L * 1024 * 1024);
            assertThat(report.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.PASSED);
        }
    }

    // ── M. FP-036 Bundle Attach ──
    @Nested @DisplayName("M. FP-036 Bundle Attach")
    class BundleAttach {
        @Test @DisplayName("stitched output attaches to bundle")
        void attach() {
            // Use 2-clip × 2s = 4s, within FP-035's 5s limit
            var c1 = new TurkuClipStitchingInputClipRef("c-1", 1, 2, 24, 640, 360, 1024*1024, "mp4", true);
            var c2 = new TurkuClipStitchingInputClipRef("c-2", 2, 2, 24, 640, 360, 1024*1024, "mp4", true);
            var result = harness.fakeStitch(List.of(c1, c2));
            var lcService = new TurkuRenderArtifactLifecycleService();
            var report = lcService.verify(result.getOpaqueOutputRef(), "mp4",
                    4, 24, 640, 360, 2L * 1024 * 1024);
            var bundleRef = TurkuRenderBundleArtifactRef.fromLifecycleReport(report,
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP);
            var bundleService = new TurkuRenderBundleManifestService();
            var bundle = bundleService.build("bundle-stitch", List.of(bundleRef));
            assertThat(bundle.getStatus()).isEqualTo(TurkuRenderBundleStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
        }
    }

    // ── N. toString Safety ──
    @Nested @DisplayName("N. toString Safety")
    class ToStringSafety {
        @Test @DisplayName("result toString safe")
        void safe() {
            var str = harness.fakeStitch(clips(2)).toString();
            assertThat(str).doesNotContain("filePath", "absolutePath", "stdout", "stderr",
                    "commandLine", "storageKey", "publicUrl", "base64", "rawLog",
                    "token", "JWT", "session", "principal", "lyrics", "scriptBody");
        }
    }

    // ── O. Enum Inventory ──
    @Nested @DisplayName("O. Enum Inventory")
    class Enums {
        @Test @DisplayName("12 stitching statuses")
        void statuses() { assertThat(TurkuClipStitchingStatus.values()).hasSize(12); }
        @Test @DisplayName("15 stitching issue codes")
        void issues() { assertThat(TurkuClipStitchingIssueCode.values()).hasSize(15); }
    }

    // ── P. ProcessBuilder Inventory ──
    @Nested @DisplayName("P. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("harness no ProcessBuilder")
        void harnessClean() {
            Set<String> types = Arrays.stream(TurkuClipStitchingHarness.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("result no ProcessBuilder")
        void resultClean() {
            Set<String> types = Arrays.stream(TurkuClipStitchingResult.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
    }

    // ── Q. Forbidden Exposure ──
    @Nested @DisplayName("Q. Forbidden Exposure")
    class Forbidden {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
                "token", "jwt", "session", "principal", "authorizationHeader",
                "lyrics", "scriptBody", "promptText", "copyrightedContent");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuClipStitchingStatus.class, TurkuClipStitchingIssueCode.class,
                TurkuClipStitchingInputClipRef.class, TurkuClipStitchingResult.class,
                TurkuClipStitchingHarness.class);

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

    // ── R. Endpoint Inventory ──
    @Nested @DisplayName("R. Endpoint Inventory (44 = 25 POST + 19 GET)")
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
