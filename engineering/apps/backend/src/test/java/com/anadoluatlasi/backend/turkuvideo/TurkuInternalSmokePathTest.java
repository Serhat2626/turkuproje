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
 * FP-029 Task 01 — Internal E2E Video Smoke Path.
 */
@DisplayName("FP-029 Task 01 — E2E Smoke Path")
class TurkuInternalSmokePathTest {

    private TurkuInternalSmokeOrchestrator orchestrator;
    private TurkuInternalRenderReadinessSnapshot readySnapshot;
    private TurkuInternalRenderSafetyChainSnapshot safeChain;
    private TurkuInternalSmokeRequest validRequest;

    @BeforeEach
    void setUp() {
        var gate = new TurkuFfmpegExecutionGate(
                TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                TurkuFfmpegExecutionEnvironment.LOCAL, true);
        orchestrator = new TurkuInternalSmokeOrchestrator(gate);
        readySnapshot = new TurkuInternalRenderReadinessSnapshot(
                TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
        safeChain = new TurkuInternalRenderSafetyChainSnapshot(
                true, true, true, true, "STUB", "DISABLED", "VERIFIED");
        validRequest = new TurkuInternalSmokeRequest(
                "project-ref", "job-ref", "review-ref", "queue-ref",
                5, 10, "mp4", 120, 30, 1920, 1080);
    }

    // ── A. Happy Path ──
    @Nested @DisplayName("A. Happy Path")
    class HappyPath {
        @Test @DisplayName("candidate created with all prerequisites")
        void candidateCreated() {
            var r = orchestrator.execute(validRequest, readySnapshot, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.CANDIDATE_CREATED);
            assertThat(r.getOutputRef()).startsWith("internal-ffmpeg-output:");
            assertThat(r.getPlan()).isNotNull();
            assertThat(r.getPlan().isBoundsValid()).isTrue();
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
        }
        @Test @DisplayName("plan has correct specs")
        void planSpecs() {
            var r = orchestrator.execute(validRequest, readySnapshot, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getPlan().getFormat()).isEqualTo("mp4");
            assertThat(r.getPlan().getSceneCount()).isEqualTo(5);
            assertThat(r.getPlan().getAssetCount()).isEqualTo(10);
        }
    }

    // ── B. FP-025 Readiness Required ──
    @Nested @DisplayName("B. FP-025 Readiness Required")
    class ReadinessRequired {
        @Test @DisplayName("denied without readiness")
        void noReadiness() {
            var r = orchestrator.execute(validRequest, null, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalSmokePathReasonCode.READINESS_NOT_MET);
        }
        @Test @DisplayName("denied with blocked readiness")
        void blockedReadiness() {
            var blocked = new TurkuInternalRenderReadinessSnapshot(
                    TurkuInternalRenderReadinessStatus.BLOCKED_BY_CONFIG, List.of());
            var r = orchestrator.execute(validRequest, blocked, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.DENIED);
        }
        @Test @DisplayName("denied with unsafe chain")
        void unsafeChain() {
            var unsafe = new TurkuInternalRenderSafetyChainSnapshot(
                    false, true, true, false, "X", "Y", "Z");
            var r = orchestrator.execute(validRequest, readySnapshot, unsafe,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.DENIED);
        }
    }

    // ── C. FP-026 Review Required ──
    @Nested @DisplayName("C. FP-026 Review Required")
    class ReviewRequired {
        @Test @DisplayName("denied without review approval")
        void noReview() {
            var r = orchestrator.execute(validRequest, readySnapshot, safeChain,
                    TurkuInternalRenderReviewState.IN_REVIEW,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalSmokePathReasonCode.REVIEW_NOT_APPROVED);
        }
        @Test @DisplayName("denied with rejected review")
        void rejected() {
            var r = orchestrator.execute(validRequest, readySnapshot, safeChain,
                    TurkuInternalRenderReviewState.REJECTED_BY_HUMAN_REVIEW,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.DENIED);
        }
    }

    // ── D. FP-028 Gate Required ──
    @Nested @DisplayName("D. FP-028 Gate Required")
    class GateRequired {
        @Test @DisplayName("denied in STAGING")
        void staging() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.STAGING, true);
            var orch = new TurkuInternalSmokeOrchestrator(gate);
            var r = orch.execute(validRequest, readySnapshot, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalSmokePathReasonCode.GATE_DENIED);
        }
        @Test @DisplayName("denied in PROD")
        void prod() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.PROD, true);
            var r = new TurkuInternalSmokeOrchestrator(gate).execute(validRequest, readySnapshot, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.DENIED);
        }
        @Test @DisplayName("denied when DISABLED")
        void disabled() {
            var r = new TurkuInternalSmokeOrchestrator().execute(validRequest, readySnapshot, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.DENIED);
        }
        @Test @DisplayName("allowed in DEV")
        void dev() {
            var gate = new TurkuFfmpegExecutionGate(TurkuFfmpegRealExecutionMode.REAL_DEV_TEST_ONLY,
                    TurkuFfmpegExecutionEnvironment.DEV, true);
            var r = new TurkuInternalSmokeOrchestrator(gate).execute(validRequest, readySnapshot, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.CANDIDATE_CREATED);
        }
    }

    // ── E. Bounds Validation ──
    @Nested @DisplayName("E. Bounds Validation")
    class Bounds {
        @Test @DisplayName("invalid format rejected")
        void invalidFormat() {
            var req = new TurkuInternalSmokeRequest("p", "j", "r", "q", 5, 10, "avi", 120, 30, 1920, 1080);
            var r = orchestrator.execute(req, readySnapshot, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalSmokePathReasonCode.BOUNDS_INVALID);
        }
        @Test @DisplayName("webm valid")
        void webmValid() {
            var req = new TurkuInternalSmokeRequest("p", "j", "r", "q", 3, 6, "webm", 60, 24, 1280, 720);
            var r = orchestrator.execute(req, readySnapshot, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalSmokePathStatus.CANDIDATE_CREATED);
        }
        @Test @DisplayName("request clamps duration/fps/res")
        void clamps() {
            var req = new TurkuInternalSmokeRequest("p", "j", "r", "q", 1, 1, "mp4", 9999, 999, 9999, 9999);
            assertThat(req.getDurationSeconds()).isEqualTo(600);
            assertThat(req.getFps()).isEqualTo(60);
            assertThat(req.getWidth()).isEqualTo(3840);
            assertThat(req.getHeight()).isEqualTo(2160);
        }
    }

    // ── F. Enum Inventory ──
    @Nested @DisplayName("F. Enum Inventory")
    class Enums {
        @Test @DisplayName("10 smoke statuses")
        void statuses() { assertThat(TurkuInternalSmokePathStatus.values()).hasSize(10); }
        @Test @DisplayName("14 smoke reason codes")
        void reasons() { assertThat(TurkuInternalSmokePathReasonCode.values()).hasSize(14); }
    }

    // ── G. Forbidden Exposure ──
    @Nested @DisplayName("G. Forbidden Exposure")
    class ForbiddenExposure {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "workspacePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "providerPayload",
                "secret", "apiKey", "downloadUrl", "publishUrl", "exportUrl");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuInternalSmokePathStatus.class, TurkuInternalSmokePathReasonCode.class,
                TurkuInternalSmokeRequest.class, TurkuInternalSmokeRenderPlan.class,
                TurkuInternalSmokePathResult.class, TurkuInternalSmokeOrchestrator.class);

        @Test @DisplayName("6 classes x 24 forbidden = 0 violations")
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
