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
 * FP-033 — Manual Local FFmpeg Run Evidence.
 * All tests use fake runner (disabled). No FFmpeg required.
 */
@DisplayName("FP-033 — Manual Local FFmpeg Run Evidence")
class TurkuFfmpegManualRunEvidenceTest {

    private static TurkuFfmpegProcessRequest validReq() {
        return new TurkuFfmpegProcessRequest("job-ref", "mp4", 60, 120, 30, 1920, 1080, 500);
    }

    // ── A. Default Disabled ──
    @Nested @DisplayName("A. Default Disabled")
    class Defaults {
        @Test @DisplayName("default harness disabled")
        void defaultDisabled() {
            var h = new TurkuFfmpegManualRunHarness();
            assertThat(h.isManualRunEnabled()).isFalse();
        }
        @Test @DisplayName("default returns MANUAL_FLAG_NOT_ENABLED")
        void defaultDenied() {
            var r = new TurkuFfmpegManualRunHarness().attempt(validReq(),
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegManualRunEvidenceStatus.MANUAL_FLAG_NOT_ENABLED);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
        }
    }

    // ── B. Manual Flag ──
    @Nested @DisplayName("B. Manual Flag Checks")
    class ManualFlags {
        @Test @DisplayName("enabled but runner disabled → PROCESS_DISABLED")
        void runnerDisabled() {
            var h = new TurkuFfmpegManualRunHarness(new TurkuFfmpegLocalDevProcessRunner(), true);
            assertThat(h.attempt(validReq(), TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegManualRunEvidenceStatus.PROCESS_DISABLED);
        }
        @Test @DisplayName("binary unavailable → FFMPEG_UNAVAILABLE")
        void binaryUnavail() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, true);
            var h = new TurkuFfmpegManualRunHarness(runner, true);
            assertThat(h.attempt(validReq(), TurkuFfmpegBinaryPolicy.DISABLED_BINARY).getStatus())
                    .isEqualTo(TurkuFfmpegManualRunEvidenceStatus.FFMPEG_UNAVAILABLE);
        }
    }

    // ── C. Gate Denials ──
    @Nested @DisplayName("C. Gate Denials")
    class Gates {
        @Test @DisplayName("STAGING denied")
        void staging() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.STAGING, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, true);
            var h = new TurkuFfmpegManualRunHarness(runner, true);
            assertThat(h.attempt(validReq(), TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegManualRunEvidenceStatus.GATE_DENIED);
        }
        @Test @DisplayName("PROD denied")
        void prod() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.PROD, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, true);
            assertThat(new TurkuFfmpegManualRunHarness(runner, true)
                    .attempt(validReq(), TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegManualRunEvidenceStatus.GATE_DENIED);
        }
        @Test @DisplayName("UNKNOWN denied")
        void unknown() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.UNKNOWN, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, true);
            assertThat(new TurkuFfmpegManualRunHarness(runner, true)
                    .attempt(validReq(), TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegManualRunEvidenceStatus.GATE_DENIED);
        }
    }

    // ── D. Fake Local Success ──
    @Nested @DisplayName("D. LOCAL + processEnabled=true (real runner, real FFmpeg if available)")
    class LocalSuccess {
        @Test @DisplayName("LOCAL enabled runner attempts real execution")
        void localEnabled() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, true);
            var h = new TurkuFfmpegManualRunHarness(runner, true);
            var r = h.attempt(validReq(), TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            // May succeed or fail-safe depending on FFmpeg availability
            assertThat(r.getStatus()).isIn(
                    TurkuFfmpegManualRunEvidenceStatus.REAL_LOCAL_EXECUTION_SUCCEEDED,
                    TurkuFfmpegManualRunEvidenceStatus.REAL_LOCAL_EXECUTION_FAILED_SAFE);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
        }
    }

    // ── E. Enum Inventory ──
    @Nested @DisplayName("E. Enum Inventory")
    class Enums {
        @Test @DisplayName("10 evidence statuses")
        void statuses() { assertThat(TurkuFfmpegManualRunEvidenceStatus.values()).hasSize(10); }
    }

    // ── F. ProcessBuilder Inventory ──
    @Nested @DisplayName("F. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("harness no ProcessBuilder")
        void harnessClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegManualRunHarness.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("result no ProcessBuilder")
        void resultClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegManualRunEvidenceResult.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("runner HAS executeProcess (only allowed)")
        void runnerHas() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("executeProcess"))).isTrue();
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
                TurkuFfmpegManualRunEvidenceStatus.class, TurkuFfmpegManualRunEvidenceResult.class,
                TurkuFfmpegManualRunHarness.class);

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
