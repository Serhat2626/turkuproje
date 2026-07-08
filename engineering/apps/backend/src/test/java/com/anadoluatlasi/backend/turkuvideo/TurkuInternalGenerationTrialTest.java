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
 * FP-029 Task 02 — Internal Generation Trial.
 */
@DisplayName("FP-029 Task 02 — Internal Generation Trial")
class TurkuInternalGenerationTrialTest {

    private TurkuInternalSmokePathResult validSmoke;
    private TurkuInternalRenderReadinessSnapshot readySnapshot;

    @BeforeEach
    void setUp() {
        var plan = new TurkuInternalSmokeRenderPlan("mp4", 120, 30, 1920, 1080, 500, 5, 10);
        var outputRef = TurkuFfmpegInternalOutputRef.create().toOpaqueRef();
        validSmoke = TurkuInternalSmokePathResult.candidateCreated(outputRef, plan);
        readySnapshot = new TurkuInternalRenderReadinessSnapshot(
                TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
    }

    // ── A. Happy Path LOCAL ──
    @Nested @DisplayName("A. Happy Path LOCAL")
    class HappyPath {
        @Test @DisplayName("REAL_DEV_TEST_ONLY LOCAL produces attempted")
        void localAttempted() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var svc = new TurkuInternalGenerationTrialService(new TurkuFfmpegDevTestAdapter(gate));
            var r = svc.runTrial(validSmoke, readySnapshot,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalTrialStatus.EXECUTION_ATTEMPTED_DEV_TEST);
            assertThat(r.getOutputRef()).startsWith("internal-ffmpeg-output:");
            assertThat(r.getVerificationCandidate()).isNotNull();
            assertThat(r.getVerificationCandidate().isBoundsValid()).isTrue();
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
        }
        @Test @DisplayName("DRY_RUN produces simulated")
        void dryRunSimulated() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.DRY_RUN,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var r = new TurkuInternalGenerationTrialService(new TurkuFfmpegDevTestAdapter(gate))
                    .runTrial(validSmoke, readySnapshot,
                            TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                            TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalTrialStatus.EXECUTION_SIMULATED);
        }
        @Test @DisplayName("DEV allowed")
        void devAllowed() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.DEV, true);
            var r = new TurkuInternalGenerationTrialService(new TurkuFfmpegDevTestAdapter(gate))
                    .runTrial(validSmoke, readySnapshot,
                            TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                            TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalTrialStatus.EXECUTION_ATTEMPTED_DEV_TEST);
        }
    }

    // ── B. Gate Denied ──
    @Nested @DisplayName("B. Gate Denied")
    class GateDenied {
        @Test @DisplayName("STAGING denied")
        void staging() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.STAGING, true);
            var r = new TurkuInternalGenerationTrialService(new TurkuFfmpegDevTestAdapter(gate))
                    .runTrial(validSmoke, readySnapshot,
                            TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                            TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalTrialStatus.GATE_DENIED);
        }
        @Test @DisplayName("PROD denied")
        void prod() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.PROD, true);
            var r = new TurkuInternalGenerationTrialService(new TurkuFfmpegDevTestAdapter(gate))
                    .runTrial(validSmoke, readySnapshot,
                            TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                            TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalTrialStatus.GATE_DENIED);
        }
        @Test @DisplayName("UNKNOWN denied")
        void unknown() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.UNKNOWN, true);
            assertThat(new TurkuInternalGenerationTrialService(new TurkuFfmpegDevTestAdapter(gate))
                    .runTrial(validSmoke, readySnapshot,
                            TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                            TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuInternalTrialStatus.GATE_DENIED);
        }
        @Test @DisplayName("DISABLED denied")
        void disabled() {
            assertThat(new TurkuInternalGenerationTrialService().runTrial(validSmoke, readySnapshot,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL).getStatus())
                    .isEqualTo(TurkuInternalTrialStatus.GATE_DENIED);
        }
    }

    // ── C. Prerequisites ──
    @Nested @DisplayName("C. Prerequisites")
    class Prerequisites {
        @Test @DisplayName("no smoke result denied")
        void noSmoke() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var r = new TurkuInternalGenerationTrialService(new TurkuFfmpegDevTestAdapter(gate))
                    .runTrial(null, readySnapshot,
                            TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                            TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalTrialStatus.GATE_DENIED);
        }
        @Test @DisplayName("no readiness denied")
        void noReadiness() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var r = new TurkuInternalGenerationTrialService(new TurkuFfmpegDevTestAdapter(gate))
                    .runTrial(validSmoke, null,
                            TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                            TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalTrialStatus.GATE_DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalTrialReasonCode.READINESS_NOT_MET);
        }
        @Test @DisplayName("review not approved denied")
        void reviewDenied() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.LOCAL, true);
            var r = new TurkuInternalGenerationTrialService(new TurkuFfmpegDevTestAdapter(gate))
                    .runTrial(validSmoke, readySnapshot,
                            TurkuInternalRenderReviewState.IN_REVIEW,
                            TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalTrialStatus.GATE_DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalTrialReasonCode.REVIEW_NOT_APPROVED);
        }
    }

    // ── D. Enum Inventory ──
    @Nested @DisplayName("D. Enum Inventory")
    class Enums {
        @Test @DisplayName("9 trial statuses")
        void statuses() { assertThat(TurkuInternalTrialStatus.values()).hasSize(9); }
        @Test @DisplayName("16 trial reason codes")
        void reasons() { assertThat(TurkuInternalTrialReasonCode.values()).hasSize(16); }
    }

    // ── E. Forbidden Exposure ──
    @Nested @DisplayName("E. Forbidden Exposure")
    class ForbiddenExposure {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuInternalTrialStatus.class, TurkuInternalTrialReasonCode.class,
                TurkuInternalTrialResult.class, TurkuInternalGenerationTrialService.class);

        @Test @DisplayName("4 classes x 19 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── F. No ProcessBuilder ──
    @Nested @DisplayName("F. No ProcessBuilder")
    class NoProcess {
        @Test @DisplayName("service no ProcessBuilder")
        void svc() {
            Set<String> types = Arrays.stream(TurkuInternalGenerationTrialService.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
    }

    // ── G. Endpoint Inventory ──
    @Nested @DisplayName("G. Endpoint Inventory (46 = 26 POST + 20 GET)")
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
