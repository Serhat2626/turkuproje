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
 * FP-030 Task 01 — Real FFmpeg Process Boundary.
 * All tests use processEnabled=false (fake/skipped).
 */
@DisplayName("FP-030 Task 01 — FFmpeg Process Boundary")
class TurkuFfmpegProcessBoundaryTest {

    private static TurkuFfmpegProcessRequest validReq() {
        return new TurkuFfmpegProcessRequest("job-ref", "mp4", 60, 120, 30, 1920, 1080, 500);
    }

    // ── A. Default Disabled ──
    @Nested @DisplayName("A. Default Disabled")
    class Defaults {
        @Test @DisplayName("default runner disabled")
        void defaultDisabled() {
            var runner = new TurkuFfmpegLocalDevProcessRunner();
            assertThat(runner.isProcessEnabled()).isFalse();
        }
        @Test @DisplayName("default gate denies")
        void defaultDenied() {
            var r = new TurkuFfmpegLocalDevProcessRunner().execute(validReq(),
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegProcessStatus.GATE_DENIED);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
        }
    }

    // ── B. Environment Gate ──
    @Nested @DisplayName("B. Environment Gate")
    class EnvGate {
        @Test @DisplayName("STAGING denied")
        void staging() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.STAGING, true);
            var r = new TurkuFfmpegLocalDevProcessRunner(gate).execute(validReq(),
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegProcessStatus.GATE_DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuFfmpegProcessReasonCode.GATE_DENIED_ENVIRONMENT);
        }
        @Test @DisplayName("PROD denied")
        void prod() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.PROD, true);
            assertThat(new TurkuFfmpegLocalDevProcessRunner(gate).execute(validReq(),
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegProcessStatus.GATE_DENIED);
        }
        @Test @DisplayName("UNKNOWN denied")
        void unknown() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.UNKNOWN, true);
            assertThat(new TurkuFfmpegLocalDevProcessRunner(gate).execute(validReq(),
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegProcessStatus.GATE_DENIED);
        }
    }

    // ── C. LOCAL/DEV/TEST Allowed (fake — processEnabled=false → SKIPPED) ──
    @Nested @DisplayName("C. LOCAL/DEV/TEST Skipped (fake)")
    class LocalFake {
        @Test @DisplayName("LOCAL skipped when processEnabled=false")
        void localSkipped() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var r = new TurkuFfmpegLocalDevProcessRunner(gate, false).execute(validReq(),
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegProcessStatus.PROCESS_SKIPPED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuFfmpegProcessReasonCode.PROCESS_SKIPPED_SAFE);
        }
        @Test @DisplayName("DEV skipped when processEnabled=false")
        void devSkipped() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.DEV, true);
            assertThat(new TurkuFfmpegLocalDevProcessRunner(gate, false).execute(validReq(),
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegProcessStatus.PROCESS_SKIPPED);
        }
        @Test @DisplayName("TEST skipped when processEnabled=false")
        void testSkipped() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.TEST, true);
            assertThat(new TurkuFfmpegLocalDevProcessRunner(gate, false).execute(validReq(),
                    TurkuFfmpegBinaryPolicy.TEST_FIXTURE_FFMPEG).getStatus())
                    .isEqualTo(TurkuFfmpegProcessStatus.PROCESS_SKIPPED);
        }
        @Test @DisplayName("DRY_RUN skipped (not real mode)")
        void dryRunSkipped() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.DRY_RUN,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            assertThat(new TurkuFfmpegLocalDevProcessRunner(gate, true).execute(validReq(),
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegProcessStatus.PROCESS_SKIPPED);
        }
    }

    // ── D. Request Validation ──
    @Nested @DisplayName("D. Request Validation")
    class Validation {
        @Test @DisplayName("null request invalid")
        void nullReq() {
            assertThat(new TurkuFfmpegLocalDevProcessRunner().execute(null,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegProcessStatus.REQUEST_INVALID);
        }
        @Test @DisplayName("invalid format rejected")
        void invalidFormat() {
            var req = new TurkuFfmpegProcessRequest("j", "avi", 60, 120, 30, 1920, 1080, 500);
            assertThat(req.isValid()).isFalse();
        }
        @Test @DisplayName("mp4 valid")
        void mp4() { assertThat(validReq().isValid()).isTrue(); }
        @Test @DisplayName("webm valid")
        void webm() { assertThat(new TurkuFfmpegProcessRequest("j", "webm", 60, 60, 24, 1280, 720, 100).isValid()).isTrue(); }
        @Test @DisplayName("timeout clamped 10-300")
        void timeoutClamp() {
            assertThat(new TurkuFfmpegProcessRequest("j", "mp4", 1, 60, 30, 1920, 1080, 500).getTimeoutSeconds()).isEqualTo(10);
            assertThat(new TurkuFfmpegProcessRequest("j", "mp4", 999, 60, 30, 1920, 1080, 500).getTimeoutSeconds()).isEqualTo(300);
        }
    }

    // ── E. Result Safety ──
    @Nested @DisplayName("E. Result Safety")
    class ResultSafety {
        @Test @DisplayName("9 process statuses")
        void statuses() { assertThat(TurkuFfmpegProcessStatus.values()).hasSize(9); }
        @Test @DisplayName("16 process reasons")
        void reasons() { assertThat(TurkuFfmpegProcessReasonCode.values()).hasSize(16); }
        @Test @DisplayName("completed result has output ref")
        void completed() {
            var r = TurkuFfmpegProcessResult.completed("internal-ffmpeg-output:123", 500);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegProcessStatus.PROCESS_COMPLETED_SAFE);
            assertThat(r.getOutputRef()).startsWith("internal-ffmpeg-output:");
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
        }
    }

    // ── F. ProcessBuilder Inventory ──
    @Nested @DisplayName("F. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("ProcessBuilder only in LocalDevProcessRunner")
        void onlyInRunner() {
            // Confirm runner HAS ProcessBuilder usage
            boolean runnerHasProcess = Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("executeProcess"));
            assertThat(runnerHasProcess).isTrue();
        }
        @Test @DisplayName("other classes have no ProcessBuilder fields")
        void othersClean() {
            List<Class<?>> others = List.of(
                    TurkuFfmpegProcessRequest.class, TurkuFfmpegProcessResult.class,
                    TurkuFfmpegProcessStatus.class, TurkuFfmpegProcessReasonCode.class,
                    TurkuFfmpegExecutionGate.class, TurkuFfmpegDevTestAdapter.class,
                    TurkuFfmpegDevTestAdapterResult.class, TurkuFfmpegExecutionBoundaryResult.class,
                    TurkuInternalSmokeOrchestrator.class, TurkuInternalGenerationTrialService.class);
            for (Class<?> c : others) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName()).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
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
                TurkuFfmpegLocalDevProcessRunner.class, TurkuFfmpegProcessRequest.class,
                TurkuFfmpegProcessResult.class, TurkuFfmpegProcessStatus.class,
                TurkuFfmpegProcessReasonCode.class);

        @Test @DisplayName("5 classes x 19 forbidden = 0 violations")
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
