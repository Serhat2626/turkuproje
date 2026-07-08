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
 * FP-034 — First Actual Local FFmpeg Video Clip Generation.
 * CI tests use fake runner. Test D attempts real clip if FFmpeg available.
 */
@DisplayName("FP-034 — First Actual Video Clip Generation")
class TurkuFfmpegClipGenerationTest {

    private static TurkuFfmpegProcessRequest clipReq() {
        return new TurkuFfmpegProcessRequest("clip-ref", "mp4", 30, 3, 24, 640, 360, 50);
    }

    // ── A. Default Disabled ──
    @Nested @DisplayName("A. Default Disabled")
    class Defaults {
        @Test @DisplayName("default harness disabled")
        void defaultDisabled() {
            var h = new TurkuFfmpegClipGenerationHarness();
            assertThat(h.isClipGenerationEnabled()).isFalse();
        }
        @Test @DisplayName("default returns MANUAL_FLAG_NOT_ENABLED")
        void defaultDenied() {
            var r = new TurkuFfmpegClipGenerationHarness().attempt(clipReq(),
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegClipGenerationStatus.MANUAL_FLAG_NOT_ENABLED);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
            assertThat(r.getSafetyFlags()).contains("SYNTHETIC_INPUT_ONLY", "NO_COPYRIGHTED_MEDIA");
        }
    }

    // ── B. Gate Checks ──
    @Nested @DisplayName("B. Gate Checks")
    class Gates {
        @Test @DisplayName("enabled but runner disabled → PROCESS_DISABLED")
        void runnerDisabled() {
            var h = new TurkuFfmpegClipGenerationHarness(new TurkuFfmpegLocalDevProcessRunner(), true);
            assertThat(h.attempt(clipReq(), TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegClipGenerationStatus.PROCESS_DISABLED);
        }
        @Test @DisplayName("binary unavailable → FFMPEG_UNAVAILABLE")
        void binaryUnavail() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, true);
            assertThat(new TurkuFfmpegClipGenerationHarness(runner, true)
                    .attempt(clipReq(), TurkuFfmpegBinaryPolicy.DISABLED_BINARY).getStatus())
                    .isEqualTo(TurkuFfmpegClipGenerationStatus.FFMPEG_UNAVAILABLE);
        }
        @Test @DisplayName("STAGING denied")
        void staging() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.STAGING, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, true);
            assertThat(new TurkuFfmpegClipGenerationHarness(runner, true)
                    .attempt(clipReq(), TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegClipGenerationStatus.GATE_DENIED);
        }
        @Test @DisplayName("PROD denied")
        void prod() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.PROD, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, true);
            assertThat(new TurkuFfmpegClipGenerationHarness(runner, true)
                    .attempt(clipReq(), TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegClipGenerationStatus.GATE_DENIED);
        }
    }

    // ── C. Metadata Verification ──
    @Nested @DisplayName("C. Metadata Verification")
    class Verification {
        @Test @DisplayName("mp4 3s 24fps 640x360 valid")
        void validClip() {
            var m = new TurkuFfmpegFixtureMetadata("ref", "mp4", 3, 24, 640, 360, 50);
            assertThat(m.isBoundsValid()).isTrue();
        }
        @Test @DisplayName("webm 5s 30fps 1280x720 valid")
        void validWebm() {
            assertThat(new TurkuFfmpegFixtureMetadata("ref", "webm", 5, 30, 1280, 720, 50).isBoundsValid()).isTrue();
        }
        @Test @DisplayName("avi invalid")
        void aviInvalid() {
            assertThat(new TurkuFfmpegFixtureMetadata("ref", "avi", 3, 24, 640, 360, 50).isBoundsValid()).isFalse();
        }
    }

    // ── D. Real Local Clip Generation (accepts success or safe-fail) ──
    @Nested @DisplayName("D. LOCAL + processEnabled=true (real clip if FFmpeg available)")
    class RealClip {
        @Test @DisplayName("LOCAL enabled runner attempts real clip generation")
        void localClip() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, true);
            var h = new TurkuFfmpegClipGenerationHarness(runner, true);
            var r = h.attempt(clipReq(), TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            // Real FFmpeg generates clip or fails safe (no libx264 etc.)
            assertThat(r.getStatus()).isIn(
                    TurkuFfmpegClipGenerationStatus.VERIFICATION_PASSED,
                    TurkuFfmpegClipGenerationStatus.CLIP_GENERATION_SUCCEEDED,
                    TurkuFfmpegClipGenerationStatus.CLIP_GENERATION_FAILED_SAFE);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
        }
    }

    // ── E. Enum Inventory ──
    @Nested @DisplayName("E. Enum Inventory")
    class Enums {
        @Test @DisplayName("12 clip statuses")
        void statuses() { assertThat(TurkuFfmpegClipGenerationStatus.values()).hasSize(12); }
    }

    // ── F. ProcessBuilder Inventory ──
    @Nested @DisplayName("F. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("harness no ProcessBuilder")
        void harnessClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegClipGenerationHarness.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("result no ProcessBuilder")
        void resultClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegClipGenerationResult.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("runner HAS generateClip")
        void runnerHas() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("generateClip"))).isTrue();
        }
    }

    // ── G. Forbidden Exposure ──
    @Nested @DisplayName("G. Forbidden Exposure")
    class ForbiddenExposure {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuFfmpegClipGenerationStatus.class, TurkuFfmpegClipGenerationResult.class,
                TurkuFfmpegClipGenerationHarness.class);

        @Test @DisplayName("3 classes x 19 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── H. Endpoint Inventory ──
    @Nested @DisplayName("H. Endpoint Inventory (44 = 25 POST + 19 GET)")
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
