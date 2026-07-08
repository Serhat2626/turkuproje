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
import static com.anadoluatlasi.backend.turkuvideo.TurkuFfmpegRealExecutionMode.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuFfmpegExecutionEnvironment.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuFfmpegBinaryPolicy.*;

/**
 * FP-028 Task 01 — Real FFmpeg Internal Dev/Test Enablement.
 */
@DisplayName("FP-028 Task 01 — Real FFmpeg Dev/Test Enablement")
class TurkuFfmpegRealExecutionGateTest {

    // ── A. Default Disabled ──
    @Nested @DisplayName("A. Default Disabled")
    class Defaults {
        @Test @DisplayName("default gate is disabled")
        void defaultGate() {
            var gate = new TurkuFfmpegExecutionGate();
            assertThat(gate.getMode()).isEqualTo(DISABLED);
            assertThat(gate.getEnvironment()).isEqualTo(UNKNOWN);
            assertThat(gate.isInternalDevTestEnabled()).isFalse();
        }
        @Test @DisplayName("disabled gate denies all")
        void disabledDenies() {
            var gate = new TurkuFfmpegExecutionGate();
            var r = gate.evaluate(SYSTEM_FFMPEG_LOCAL, "job", "q", "rev");
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegBoundaryResultStatus.DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuFfmpegBoundaryReasonCode.MODE_DISABLED);
        }
        @Test @DisplayName("disabled result is internal-only")
        void disabledSafety() {
            var gate = new TurkuFfmpegExecutionGate();
            var r = gate.evaluate(SYSTEM_FFMPEG_LOCAL, null, null, null);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
        }
    }

    // ── B. Environment Gate ──
    @Nested @DisplayName("B. Environment Gate")
    class EnvGate {
        @Test @DisplayName("LOCAL allows real")
        void local() { assertThat(LOCAL.isRealExecutionAllowed()).isTrue(); }
        @Test @DisplayName("DEV allows real")
        void dev() { assertThat(DEV.isRealExecutionAllowed()).isTrue(); }
        @Test @DisplayName("TEST allows real")
        void test() { assertThat(TEST.isRealExecutionAllowed()).isTrue(); }
        @Test @DisplayName("STAGING denies real")
        void staging() { assertThat(STAGING.isRealExecutionAllowed()).isFalse(); }
        @Test @DisplayName("PROD denies real")
        void prod() { assertThat(PROD.isRealExecutionAllowed()).isFalse(); }
        @Test @DisplayName("UNKNOWN denies real")
        void unknown() { assertThat(UNKNOWN.isRealExecutionAllowed()).isFalse(); }
        @Test @DisplayName("6 environments")
        void count() { assertThat(TurkuFfmpegExecutionEnvironment.values()).hasSize(6); }
    }

    // ── C. Mode Gate ──
    @Nested @DisplayName("C. Mode Gate")
    class ModeGate {
        @Test @DisplayName("4 modes")
        void count() { assertThat(TurkuFfmpegRealExecutionMode.values()).hasSize(4); }
        @Test @DisplayName("DRY_RUN returns safe result")
        void dryRun() {
            var gate = new TurkuFfmpegExecutionGate(DRY_RUN, LOCAL, true);
            var r = gate.evaluate(SYSTEM_FFMPEG_LOCAL, "job", null, null);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegBoundaryResultStatus.DRY_RUN_COMPLETE);
        }
        @Test @DisplayName("SIMULATED returns safe result")
        void simulated() {
            var gate = new TurkuFfmpegExecutionGate(SIMULATED, LOCAL, true);
            var r = gate.evaluate(SYSTEM_FFMPEG_LOCAL, "job", null, null);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegBoundaryResultStatus.SIMULATED_COMPLETE);
        }
    }

    // ── D. Real Execution Gate ──
    @Nested @DisplayName("D. Real Execution Gate")
    class RealGate {
        @Test @DisplayName("real allowed LOCAL+flag")
        void localAllowed() {
            var gate = new TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, LOCAL, true);
            var r = gate.evaluate(SYSTEM_FFMPEG_LOCAL, "job", "q", "rev");
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegBoundaryResultStatus.EXECUTED_INTERNAL);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
        }
        @Test @DisplayName("real denied STAGING")
        void stagingDenied() {
            var gate = new TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, STAGING, true);
            var r = gate.evaluate(SYSTEM_FFMPEG_LOCAL, "job", null, null);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegBoundaryResultStatus.DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuFfmpegBoundaryReasonCode.ENVIRONMENT_DENIED);
        }
        @Test @DisplayName("real denied PROD")
        void prodDenied() {
            var gate = new TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, PROD, true);
            var r = gate.evaluate(SYSTEM_FFMPEG_LOCAL, null, null, null);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegBoundaryResultStatus.DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuFfmpegBoundaryReasonCode.ENVIRONMENT_DENIED);
        }
        @Test @DisplayName("real denied UNKNOWN")
        void unknownDenied() {
            var gate = new TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, UNKNOWN, true);
            assertThat(gate.evaluate(SYSTEM_FFMPEG_LOCAL, null, null, null).getStatus())
                    .isEqualTo(TurkuFfmpegBoundaryResultStatus.DENIED);
        }
        @Test @DisplayName("real denied without flag")
        void noFlag() {
            var gate = new TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, LOCAL, false);
            var r = gate.evaluate(SYSTEM_FFMPEG_LOCAL, "job", null, null);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegBoundaryResultStatus.DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuFfmpegBoundaryReasonCode.INTERNAL_FLAG_NOT_SET);
        }
        @Test @DisplayName("real DEV allowed")
        void devAllowed() {
            var gate = new TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, DEV, true);
            assertThat(gate.evaluate(SYSTEM_FFMPEG_LOCAL, "j", null, null).getStatus())
                    .isEqualTo(TurkuFfmpegBoundaryResultStatus.EXECUTED_INTERNAL);
        }
        @Test @DisplayName("real TEST allowed")
        void testAllowed() {
            var gate = new TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, TEST, true);
            assertThat(gate.evaluate(TEST_FIXTURE_FFMPEG, "j", null, null).getStatus())
                    .isEqualTo(TurkuFfmpegBoundaryResultStatus.EXECUTED_INTERNAL);
        }
    }

    // ── E. Binary Policy ──
    @Nested @DisplayName("E. Binary Policy")
    class BinaryPol {
        @Test @DisplayName("3 policies")
        void count() { assertThat(TurkuFfmpegBinaryPolicy.values()).hasSize(3); }
        @Test @DisplayName("SYSTEM available")
        void system() { assertThat(SYSTEM_FFMPEG_LOCAL.isAvailable()).isTrue(); }
        @Test @DisplayName("TEST available")
        void test() { assertThat(TEST_FIXTURE_FFMPEG.isAvailable()).isTrue(); }
        @Test @DisplayName("DISABLED not available")
        void disabled() { assertThat(DISABLED_BINARY.isAvailable()).isFalse(); }
        @Test @DisplayName("disabled binary denied")
        void disabledDenied() {
            var gate = new TurkuFfmpegExecutionGate(DRY_RUN, LOCAL, true);
            var r = gate.evaluate(DISABLED_BINARY, "j", null, null);
            assertThat(r.getStatus()).isEqualTo(TurkuFfmpegBoundaryResultStatus.DENIED);
            assertThat(r.getReasonCode()).isEqualTo(TurkuFfmpegBoundaryReasonCode.BINARY_UNAVAILABLE);
        }
        @Test @DisplayName("null binary denied")
        void nullBinary() {
            var gate = new TurkuFfmpegExecutionGate(DRY_RUN, LOCAL, true);
            assertThat(gate.evaluate(null, null, null, null).getStatus())
                    .isEqualTo(TurkuFfmpegBoundaryResultStatus.DENIED);
        }
    }

    // ── F. Execution Limits ──
    @Nested @DisplayName("F. Execution Limits")
    class Limits {
        @Test @DisplayName("defaults valid")
        void defaults() {
            var lim = new TurkuFfmpegExecutionLimits();
            assertThat(lim.getTimeoutSeconds()).isEqualTo(300);
            assertThat(lim.getMaxDurationSeconds()).isEqualTo(600);
            assertThat(lim.getMaxFps()).isEqualTo(30);
            assertThat(lim.getMaxWidth()).isEqualTo(1920);
            assertThat(lim.getMaxHeight()).isEqualTo(1080);
        }
        @Test @DisplayName("clamped timeout")
        void timeoutClamped() {
            assertThat(new TurkuFfmpegExecutionLimits(0, 60, 1, 100, 100, 1).getTimeoutSeconds()).isEqualTo(1);
            assertThat(new TurkuFfmpegExecutionLimits(9999, 60, 1, 100, 100, 1).getTimeoutSeconds()).isEqualTo(600);
        }
        @Test @DisplayName("clamped fps")
        void fpsClamped() {
            assertThat(new TurkuFfmpegExecutionLimits(60, 60, 1, 100, 100, 999).getMaxFps()).isEqualTo(60);
        }
        @Test @DisplayName("clamped resolution")
        void resClamped() {
            assertThat(new TurkuFfmpegExecutionLimits(60, 60, 1, 9999, 9999, 30).getMaxWidth()).isEqualTo(3840);
            assertThat(new TurkuFfmpegExecutionLimits(60, 60, 1, 9999, 9999, 30).getMaxHeight()).isEqualTo(2160);
        }
    }

    // ── G. Result Safety ──
    @Nested @DisplayName("G. Result Safety")
    class ResultSafety {
        @Test @DisplayName("6 result statuses")
        void statuses() { assertThat(TurkuFfmpegBoundaryResultStatus.values()).hasSize(6); }
        @Test @DisplayName("14 reason codes")
        void reasons() { assertThat(TurkuFfmpegBoundaryReasonCode.values()).hasSize(14); }
        @Test @DisplayName("result always internal-only")
        void internalOnly() {
            var r = TurkuFfmpegExecutionBoundaryResult.denied(
                    TurkuFfmpegBoundaryReasonCode.DISABLED_BY_DEFAULT, DISABLED, UNKNOWN);
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
            assertThat(r.getSafetyFlags()).contains("NO_PUBLISH", "NO_DOWNLOAD", "NO_AUTO_APPROVE");
        }
    }

    // ── H. Forbidden Exposure ──
    @Nested @DisplayName("H. Forbidden Exposure")
    class ForbiddenExposure {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "rawPayload", "providerPayload",
                "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "workspacePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "secret", "apiKey", "authorizationHeader",
                "jwt", "token", "session", "credential", "password",
                "downloadUrl", "publishUrl", "exportUrl", "renderedVideoUrl");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuFfmpegRealExecutionMode.class,
                TurkuFfmpegExecutionEnvironment.class,
                TurkuFfmpegBinaryPolicy.class,
                TurkuFfmpegBoundaryResultStatus.class,
                TurkuFfmpegBoundaryReasonCode.class,
                TurkuFfmpegExecutionBoundaryResult.class,
                TurkuFfmpegExecutionGate.class);

        @Test @DisplayName("7 classes x 31 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("field '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── I. No ProcessBuilder/Runtime.exec ──
    @Nested @DisplayName("I. No ProcessBuilder/Runtime.exec")
    class NoProcess {
        @Test @DisplayName("gate has no ProcessBuilder field")
        void noProcessBuilder() {
            Set<String> types = Arrays.stream(TurkuFfmpegExecutionGate.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("result has no ProcessBuilder field")
        void noProcessResult() {
            Set<String> types = Arrays.stream(TurkuFfmpegExecutionBoundaryResult.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
    }

    // ── J. Endpoint Inventory ──
    @Nested @DisplayName("J. Endpoint Inventory (46 = 26 POST + 20 GET)")
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
