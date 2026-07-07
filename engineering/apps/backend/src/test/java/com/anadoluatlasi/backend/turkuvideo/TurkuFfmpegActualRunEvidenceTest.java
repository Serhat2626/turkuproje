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
 * FP-032 — Actual Local FFmpeg Smoke Execution Evidence.
 * All tests use default checker/runner (disabled). No FFmpeg required.
 */
@DisplayName("FP-032 — Actual Local Smoke Execution Evidence")
class TurkuFfmpegActualRunEvidenceTest {

    private TurkuInternalRenderReadinessSnapshot ready;
    private TurkuInternalRenderSafetyChainSnapshot safeChain;

    @BeforeEach
    void setUp() {
        ready = new TurkuInternalRenderReadinessSnapshot(
                TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
        safeChain = new TurkuInternalRenderSafetyChainSnapshot(
                true, true, true, true, "STUB", "DISABLED", "VERIFIED");
    }

    private TurkuFfmpegManualExecutionRequest req() {
        return new TurkuFfmpegManualExecutionRequest("job-ref", "queue-ref", "review-ref",
                TurkuFfmpegManualExecutionPolicy.LOCAL_ONLY_MANUAL, true, true,
                "mp4", 60, 120, 30, 1920, 1080, 500);
    }

    // ── A. Default Disabled ──
    @Nested @DisplayName("A. Default Disabled")
    class Defaults {
        @Test @DisplayName("default service returns disabled")
        void defaultDisabled() {
            var svc = new TurkuFfmpegActualRunEvidenceService();
            var r = svc.attempt(req(), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegActualRunEvidenceStatus.REAL_LOCAL_RUN_SKIPPED_DISABLED);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
        }
        @Test @DisplayName("default checker disabled")
        void checkerDisabled() {
            var checker = new TurkuFfmpegLocalAvailabilityChecker();
            assertThat(checker.isCheckEnabled()).isFalse();
            assertThat(checker.check()).isEqualTo(TurkuFfmpegLocalAvailabilityStatus.DISABLED);
        }
    }

    // ── B. Availability Checker ──
    @Nested @DisplayName("B. Availability Checker")
    class AvailabilityChecker {
        @Test @DisplayName("UNKNOWN env → UNSUPPORTED")
        void unknownEnv() {
            var checker = new TurkuFfmpegLocalAvailabilityChecker(
                    TurkuFfmpegExecutionEnvironment.UNKNOWN, true);
            assertThat(checker.check()).isEqualTo(TurkuFfmpegLocalAvailabilityStatus.UNSUPPORTED_ENVIRONMENT);
        }
        @Test @DisplayName("STAGING env → UNSUPPORTED")
        void stagingEnv() {
            var checker = new TurkuFfmpegLocalAvailabilityChecker(
                    TurkuFfmpegExecutionEnvironment.STAGING, true);
            assertThat(checker.check()).isEqualTo(TurkuFfmpegLocalAvailabilityStatus.UNSUPPORTED_ENVIRONMENT);
        }
        @Test @DisplayName("PROD env → UNSUPPORTED")
        void prodEnv() {
            var checker = new TurkuFfmpegLocalAvailabilityChecker(
                    TurkuFfmpegExecutionEnvironment.PROD, true);
            assertThat(checker.check()).isEqualTo(TurkuFfmpegLocalAvailabilityStatus.UNSUPPORTED_ENVIRONMENT);
        }
        @Test @DisplayName("disabled → DISABLED")
        void disabled() {
            var checker = new TurkuFfmpegLocalAvailabilityChecker(
                    TurkuFfmpegExecutionEnvironment.LOCAL, false);
            assertThat(checker.check()).isEqualTo(TurkuFfmpegLocalAvailabilityStatus.DISABLED);
        }
    }

    // ── C. Evidence Service with Disabled Checker ──
    @Nested @DisplayName("C. Evidence with Disabled Checker")
    class EvidenceDisabled {
        @Test @DisplayName("disabled checker → SKIPPED_DISABLED")
        void skippedDisabled() {
            var checker = new TurkuFfmpegLocalAvailabilityChecker(
                    TurkuFfmpegExecutionEnvironment.LOCAL, false);
            var svc = new TurkuFfmpegActualRunEvidenceService(checker, new TurkuFfmpegManualExecutionService());
            assertThat(svc.attempt(req(), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegActualRunEvidenceStatus.REAL_LOCAL_RUN_SKIPPED_DISABLED);
        }
        @Test @DisplayName("unsupported env → SKIPPED_UNSUPPORTED")
        void skippedUnsupported() {
            var checker = new TurkuFfmpegLocalAvailabilityChecker(
                    TurkuFfmpegExecutionEnvironment.STAGING, true);
            var svc = new TurkuFfmpegActualRunEvidenceService(checker, new TurkuFfmpegManualExecutionService());
            assertThat(svc.attempt(req(), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegActualRunEvidenceStatus.REAL_LOCAL_RUN_SKIPPED_UNSUPPORTED_ENVIRONMENT);
        }
    }

    // ── D. Enum Inventory ──
    @Nested @DisplayName("D. Enum Inventory")
    class Enums {
        @Test @DisplayName("4 availability statuses")
        void avail() { assertThat(TurkuFfmpegLocalAvailabilityStatus.values()).hasSize(4); }
        @Test @DisplayName("12 evidence statuses")
        void evidence() { assertThat(TurkuFfmpegActualRunEvidenceStatus.values()).hasSize(12); }
    }

    // ── E. ProcessBuilder Inventory ──
    @Nested @DisplayName("E. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("evidence service no ProcessBuilder")
        void serviceClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegActualRunEvidenceService.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("evidence result no ProcessBuilder")
        void resultClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegActualRunEvidenceResult.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("runner HAS executeProcess")
        void runnerHas() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("executeProcess"))).isTrue();
        }
        @Test @DisplayName("checker has NO ProcessBuilder")
        void checkerClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegLocalAvailabilityChecker.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
    }

    // ── F. Forbidden Exposure ──
    @Nested @DisplayName("F. Forbidden Exposure")
    class ForbiddenExposure {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuFfmpegLocalAvailabilityStatus.class, TurkuFfmpegLocalAvailabilityChecker.class,
                TurkuFfmpegActualRunEvidenceStatus.class, TurkuFfmpegActualRunEvidenceResult.class,
                TurkuFfmpegActualRunEvidenceService.class);

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

    // ── G. Endpoint Inventory ──
    @Nested @DisplayName("G. Endpoint Inventory (44 = 25 POST + 19 GET)")
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
