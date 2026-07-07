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
 * FP-030 Task 02 — Real FFmpeg Smoke Run Service.
 */
@DisplayName("FP-030 Task 02 — FFmpeg Smoke Run")
class TurkuFfmpegSmokeRunTest {

    private TurkuFfmpegProcessRequest validReq;
    private TurkuInternalRenderReadinessSnapshot ready;
    private TurkuInternalRenderSafetyChainSnapshot safeChain;

    @BeforeEach
    void setUp() {
        validReq = new TurkuFfmpegProcessRequest("job-ref", "mp4", 60, 120, 30, 1920, 1080, 500);
        ready = new TurkuInternalRenderReadinessSnapshot(
                TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
        safeChain = new TurkuInternalRenderSafetyChainSnapshot(
                true, true, true, true, "STUB", "DISABLED", "VERIFIED");
    }

    // ── A. Default Disabled ──
    @Nested @DisplayName("A. Default Disabled")
    class Defaults {
        @Test @DisplayName("default service skips disabled")
        void defaultSkips() {
            var svc = new TurkuFfmpegSmokeRunService();
            var r = svc.run(validReq, ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegSmokeRunStatus.EXECUTION_SKIPPED_DISABLED);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
        }
    }

    // ── B. Prerequisites ──
    @Nested @DisplayName("B. Prerequisites")
    class Prerequisites {
        @Test @DisplayName("no readiness denied")
        void noReadiness() {
            assertThat(new TurkuFfmpegSmokeRunService().run(validReq, null, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegSmokeRunStatus.READINESS_DENIED);
        }
        @Test @DisplayName("unsafe chain denied")
        void unsafeChain() {
            var unsafe = new TurkuInternalRenderSafetyChainSnapshot(
                    false, true, true, false, "X", "Y", "Z");
            assertThat(new TurkuFfmpegSmokeRunService().run(validReq, ready, unsafe,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegSmokeRunStatus.READINESS_DENIED);
        }
        @Test @DisplayName("review not approved denied")
        void reviewDenied() {
            assertThat(new TurkuFfmpegSmokeRunService().run(validReq, ready, safeChain,
                    TurkuInternalRenderReviewState.IN_REVIEW,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegSmokeRunStatus.REVIEW_DENIED);
        }
        @Test @DisplayName("disabled binary skipped unavailable")
        void binaryUnavailable() {
            assertThat(new TurkuFfmpegSmokeRunService().run(validReq, ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.DISABLED_BINARY).getStatus())
                    .isEqualTo(TurkuFfmpegSmokeRunStatus.EXECUTION_SKIPPED_UNAVAILABLE);
        }
    }

    // ── C. Gate Denied ──
    @Nested @DisplayName("C. Gate Denied")
    class GateDenied {
        @Test @DisplayName("STAGING denied via runner gate")
        void staging() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.STAGING, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, true);
            var r = new TurkuFfmpegSmokeRunService(runner).run(validReq, ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegSmokeRunStatus.GATE_DENIED);
        }
        @Test @DisplayName("PROD denied")
        void prod() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.PROD, true);
            assertThat(new TurkuFfmpegSmokeRunService(new TurkuFfmpegLocalDevProcessRunner(gate, true))
                    .run(validReq, ready, safeChain,
                            TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                            TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegSmokeRunStatus.GATE_DENIED);
        }
    }

    // ── D. Fake Runner Success ──
    @Nested @DisplayName("D. Fake Runner (LOCAL + processEnabled=false → skipped)")
    class FakeRunner {
        @Test @DisplayName("processEnabled=false skips at service level")
        void fakeSkips() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var runner = new TurkuFfmpegLocalDevProcessRunner(gate, false);
            var r = new TurkuFfmpegSmokeRunService(runner).run(validReq, ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegSmokeRunStatus.EXECUTION_SKIPPED_DISABLED);
        }
    }

    // ── E. Enum Inventory ──
    @Nested @DisplayName("E. Enum Inventory")
    class Enums {
        @Test @DisplayName("12 smoke run statuses")
        void statuses() { assertThat(TurkuFfmpegSmokeRunStatus.values()).hasSize(12); }
    }

    // ── F. ProcessBuilder Inventory ──
    @Nested @DisplayName("F. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("service has no ProcessBuilder")
        void serviceClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegSmokeRunService.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("result has no ProcessBuilder")
        void resultClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegSmokeRunResult.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
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
                TurkuFfmpegSmokeRunStatus.class, TurkuFfmpegSmokeRunResult.class,
                TurkuFfmpegSmokeRunService.class);

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
