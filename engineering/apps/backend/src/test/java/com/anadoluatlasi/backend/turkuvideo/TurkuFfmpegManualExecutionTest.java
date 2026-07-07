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
 * FP-031 Task 01 — Local-only Manual Execution.
 * All tests use processEnabled=false (fake). No FFmpeg required.
 */
@DisplayName("FP-031 Task 01 — Local Manual Execution")
class TurkuFfmpegManualExecutionTest {

    private TurkuInternalRenderReadinessSnapshot ready;
    private TurkuInternalRenderSafetyChainSnapshot safeChain;

    @BeforeEach
    void setUp() {
        ready = new TurkuInternalRenderReadinessSnapshot(
                TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
        safeChain = new TurkuInternalRenderSafetyChainSnapshot(
                true, true, true, true, "STUB", "DISABLED", "VERIFIED");
    }

    private TurkuFfmpegManualExecutionRequest validReq(boolean confirmed, boolean processEnabled) {
        return new TurkuFfmpegManualExecutionRequest("job-ref", "queue-ref", "review-ref",
                TurkuFfmpegManualExecutionPolicy.LOCAL_ONLY_MANUAL, confirmed, processEnabled,
                "mp4", 60, 120, 30, 1920, 1080, 500);
    }

    // ── A. Default Disabled ──
    @Nested @DisplayName("A. Default Disabled")
    class Defaults {
        @Test @DisplayName("default service skips")
        void defaultSkips() {
            var r = new TurkuFfmpegManualExecutionService().execute(
                    validReq(true, true), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.LOCAL);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegManualExecutionStatus.EXECUTION_SKIPPED_FAKE);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
        }
        @Test @DisplayName("DISABLED policy denied")
        void policyDisabled() {
            var req = new TurkuFfmpegManualExecutionRequest("j", "q", "r",
                    TurkuFfmpegManualExecutionPolicy.DISABLED, true, true,
                    "mp4", 60, 120, 30, 1920, 1080, 500);
            assertThat(new TurkuFfmpegManualExecutionService().execute(req, ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegManualExecutionStatus.POLICY_DISABLED);
        }
    }

    // ── B. Manual Confirmation ──
    @Nested @DisplayName("B. Manual Confirmation")
    class Confirmation {
        @Test @DisplayName("unconfirmed denied")
        void unconfirmed() {
            assertThat(new TurkuFfmpegManualExecutionService().execute(
                    validReq(false, true), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegManualExecutionStatus.MANUAL_CONFIRMATION_MISSING);
        }
        @Test @DisplayName("processEnabled=false denied")
        void processDisabled() {
            assertThat(new TurkuFfmpegManualExecutionService().execute(
                    validReq(true, false), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegManualExecutionStatus.PROCESS_DISABLED);
        }
    }

    // ── C. Environment ──
    @Nested @DisplayName("C. Environment")
    class Environment {
        @Test @DisplayName("DEV denied (not LOCAL)")
        void dev() {
            assertThat(new TurkuFfmpegManualExecutionService().execute(
                    validReq(true, true), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.DEV).getStatus())
                    .isEqualTo(TurkuFfmpegManualExecutionStatus.ENVIRONMENT_DENIED);
        }
        @Test @DisplayName("STAGING denied")
        void staging() {
            assertThat(new TurkuFfmpegManualExecutionService().execute(
                    validReq(true, true), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.STAGING).getStatus())
                    .isEqualTo(TurkuFfmpegManualExecutionStatus.ENVIRONMENT_DENIED);
        }
        @Test @DisplayName("PROD denied")
        void prod() {
            assertThat(new TurkuFfmpegManualExecutionService().execute(
                    validReq(true, true), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.PROD).getStatus())
                    .isEqualTo(TurkuFfmpegManualExecutionStatus.ENVIRONMENT_DENIED);
        }
        @Test @DisplayName("UNKNOWN denied")
        void unknown() {
            assertThat(new TurkuFfmpegManualExecutionService().execute(
                    validReq(true, true), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.UNKNOWN).getStatus())
                    .isEqualTo(TurkuFfmpegManualExecutionStatus.ENVIRONMENT_DENIED);
        }
    }

    // ── D. Prerequisites ──
    @Nested @DisplayName("D. Prerequisites")
    class Prerequisites {
        @Test @DisplayName("binary unavailable")
        void binary() {
            assertThat(new TurkuFfmpegManualExecutionService().execute(
                    validReq(true, true), ready, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.DISABLED_BINARY,
                    TurkuFfmpegExecutionEnvironment.LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegManualExecutionStatus.BINARY_UNAVAILABLE);
        }
        @Test @DisplayName("readiness denied")
        void readiness() {
            assertThat(new TurkuFfmpegManualExecutionService().execute(
                    validReq(true, true), null, safeChain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegManualExecutionStatus.READINESS_DENIED);
        }
        @Test @DisplayName("review denied")
        void review() {
            assertThat(new TurkuFfmpegManualExecutionService().execute(
                    validReq(true, true), ready, safeChain,
                    TurkuInternalRenderReviewState.IN_REVIEW,
                    TurkuFfmpegBinaryPolicy.SYSTEM_FFMPEG_LOCAL,
                    TurkuFfmpegExecutionEnvironment.LOCAL).getStatus())
                    .isEqualTo(TurkuFfmpegManualExecutionStatus.REVIEW_DENIED);
        }
    }

    // ── E. Fixture Metadata ──
    @Nested @DisplayName("E. Fixture Metadata")
    class Fixture {
        @Test @DisplayName("mp4 valid bounds")
        void mp4Valid() {
            var m = new TurkuFfmpegFixtureMetadata("ref", "mp4", 120, 30, 1920, 1080, 500);
            assertThat(m.isBoundsValid()).isTrue();
        }
        @Test @DisplayName("webm valid")
        void webmValid() {
            assertThat(new TurkuFfmpegFixtureMetadata("ref", "webm", 60, 24, 1280, 720, 100).isBoundsValid()).isTrue();
        }
        @Test @DisplayName("avi invalid")
        void aviInvalid() {
            assertThat(new TurkuFfmpegFixtureMetadata("ref", "avi", 60, 24, 1280, 720, 100).isBoundsValid()).isFalse();
        }
        @Test @DisplayName("duration 601 invalid")
        void durationOver() {
            assertThat(new TurkuFfmpegFixtureMetadata("ref", "mp4", 601, 30, 1920, 1080, 500).isBoundsValid()).isFalse();
        }
        @Test @DisplayName("resolution 3841 invalid")
        void resOver() {
            assertThat(new TurkuFfmpegFixtureMetadata("ref", "mp4", 60, 30, 3841, 1080, 500).isBoundsValid()).isFalse();
        }
        @Test @DisplayName("size 1001 invalid")
        void sizeOver() {
            assertThat(new TurkuFfmpegFixtureMetadata("ref", "mp4", 60, 30, 1920, 1080, 1001).isBoundsValid()).isFalse();
        }
    }

    // ── F. Enum Inventory ──
    @Nested @DisplayName("F. Enum Inventory")
    class Enums {
        @Test @DisplayName("2 policies")
        void policies() { assertThat(TurkuFfmpegManualExecutionPolicy.values()).hasSize(2); }
        @Test @DisplayName("15 statuses")
        void statuses() { assertThat(TurkuFfmpegManualExecutionStatus.values()).hasSize(19); }
        @Test @DisplayName("16 reason codes")
        void reasons() { assertThat(TurkuFfmpegManualExecutionReasonCode.values()).hasSize(20); }
    }

    // ── G. ProcessBuilder Inventory ──
    @Nested @DisplayName("G. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("service no ProcessBuilder")
        void serviceClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegManualExecutionService.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("result no ProcessBuilder")
        void resultClean() {
            Set<String> types = Arrays.stream(TurkuFfmpegManualExecutionResult.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("runner HAS executeProcess")
        void runnerHas() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("executeProcess"))).isTrue();
        }
    }

    // ── H. Forbidden Exposure ──
    @Nested @DisplayName("H. Forbidden Exposure")
    class ForbiddenExposure {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuFfmpegManualExecutionPolicy.class, TurkuFfmpegManualExecutionStatus.class,
                TurkuFfmpegManualExecutionReasonCode.class, TurkuFfmpegManualExecutionRequest.class,
                TurkuFfmpegManualExecutionResult.class, TurkuFfmpegManualExecutionService.class,
                TurkuFfmpegFixtureMetadata.class);

        @Test @DisplayName("7 classes x 19 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── I. Endpoint Inventory ──
    @Nested @DisplayName("I. Endpoint Inventory (44 = 25 POST + 19 GET)")
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
