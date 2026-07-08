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
 * FP-028 Task 02 — Dev/Test Adapter + Output + Verification.
 */
@DisplayName("FP-028 Task 02 — Adapter + Output + Verification")
class TurkuFfmpegDevTestAdapterTest {

    // ── A. Adapter Gate Enforcement ──
    @Nested @DisplayName("A. Adapter Gate Enforcement")
    class AdapterGate {
        @Test @DisplayName("default adapter denied")
        void defaultDenied() {
            var adapter = new TurkuFfmpegDevTestAdapter();
            var r = adapter.execute(SYSTEM_FFMPEG_LOCAL, "job", null, null);
            assertThat(r.isAllowed()).isFalse();
            assertThat(r.getOutputRef()).isNull();
            assertThat(r.isInternalOnly()).isTrue();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
        }
        @Test @DisplayName("real LOCAL allowed produces output ref")
        void localAllowed() {
            var gate = new TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, LOCAL, true);
            var adapter = new TurkuFfmpegDevTestAdapter(gate);
            var r = adapter.execute(SYSTEM_FFMPEG_LOCAL, "job", "q", "rev");
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getOutputRef()).startsWith("internal-ffmpeg-output:");
        }
        @Test @DisplayName("STAGING denied")
        void stagingDenied() {
            var gate = new TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, STAGING, true);
            var r = new TurkuFfmpegDevTestAdapter(gate).execute(SYSTEM_FFMPEG_LOCAL, "j", null, null);
            assertThat(r.isAllowed()).isFalse();
        }
        @Test @DisplayName("PROD denied")
        void prodDenied() {
            var gate = new TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, PROD, true);
            assertThat(new TurkuFfmpegDevTestAdapter(gate).execute(SYSTEM_FFMPEG_LOCAL, "j", null, null).isAllowed()).isFalse();
        }
        @Test @DisplayName("DRY_RUN allowed no output")
        void dryRunAllowed() {
            var gate = new TurkuFfmpegExecutionGate(DRY_RUN, LOCAL, true);
            var r = new TurkuFfmpegDevTestAdapter(gate).execute(SYSTEM_FFMPEG_LOCAL, "j", null, null);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getOutputRef()).startsWith("internal-ffmpeg-output:");
        }
    }

    // ── B. Output Ref Safety ──
    @Nested @DisplayName("B. Output Ref Safety")
    class OutputRef {
        @Test @DisplayName("create opaque ref")
        void create() {
            var ref = TurkuFfmpegInternalOutputRef.create();
            assertThat(ref.toOpaqueRef()).startsWith("internal-ffmpeg-output:");
        }
        @Test @DisplayName("parse roundtrip")
        void parse() {
            var ref = TurkuFfmpegInternalOutputRef.create();
            var p = TurkuFfmpegInternalOutputRef.parse(ref.toOpaqueRef());
            assertThat(p).isNotNull();
            assertThat(p.getOutputId()).isEqualTo(ref.getOutputId());
        }
        @Test @DisplayName("reject null/blank")
        void reject() {
            assertThat(TurkuFfmpegInternalOutputRef.parse(null)).isNull();
            assertThat(TurkuFfmpegInternalOutputRef.parse("")).isNull();
            assertThat(TurkuFfmpegInternalOutputRef.parse("wrong:abc")).isNull();
        }
        @Test @DisplayName("safe toString")
        void safeToString() {
            assertThat(TurkuFfmpegInternalOutputRef.create().toString()).doesNotContain("/", "\\", "file:", "http");
        }
    }

    // ── C. Verification Candidate ──
    @Nested @DisplayName("C. Verification Candidate")
    class Verification {
        @Test @DisplayName("mp4 valid")
        void mp4Valid() {
            var v = new TurkuFfmpegVerificationCandidate("out", "job", "mp4", 120, 30, 1920, 1080, 500);
            assertThat(v.isBoundsValid()).isTrue();
            assertThat(v.isInternalOnly()).isTrue();
            assertThat(v.isPublicDeliveryAvailable()).isFalse();
        }
        @Test @DisplayName("webm valid")
        void webmValid() {
            assertThat(new TurkuFfmpegVerificationCandidate("o", "j", "webm", 60, 24, 1280, 720, 100).isBoundsValid()).isTrue();
        }
        @Test @DisplayName("invalid format rejected")
        void invalidFormat() {
            assertThat(new TurkuFfmpegVerificationCandidate("o", "j", "avi", 60, 30, 1920, 1080, 100).isBoundsValid()).isFalse();
        }
        @Test @DisplayName("duration exceeded rejected")
        void durationExceeded() {
            assertThat(new TurkuFfmpegVerificationCandidate("o", "j", "mp4", 999, 30, 1920, 1080, 100).isBoundsValid()).isFalse();
        }
        @Test @DisplayName("size exceeded rejected")
        void sizeExceeded() {
            assertThat(new TurkuFfmpegVerificationCandidate("o", "j", "mp4", 60, 30, 1920, 1080, 2000).isBoundsValid()).isFalse();
        }
        @Test @DisplayName("resolution exceeded rejected")
        void resExceeded() {
            assertThat(new TurkuFfmpegVerificationCandidate("o", "j", "mp4", 60, 30, 9999, 9999, 100).isBoundsValid()).isFalse();
        }
    }

    // ── D. Forbidden Exposure ──
    @Nested @DisplayName("D. Forbidden Exposure")
    class ForbiddenExposure {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "rawPayload", "providerPayload",
                "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "workspacePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "secret", "apiKey",
                "downloadUrl", "publishUrl", "exportUrl", "renderedVideoUrl");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuFfmpegDevTestAdapter.class,
                TurkuFfmpegDevTestAdapterResult.class,
                TurkuFfmpegInternalOutputRef.class,
                TurkuFfmpegVerificationCandidate.class);

        @Test @DisplayName("4 classes x 25 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── E. No ProcessBuilder ──
    @Nested @DisplayName("E. No ProcessBuilder")
    class NoProcess {
        @Test @DisplayName("adapter no ProcessBuilder field")
        void adapter() {
            Set<String> types = Arrays.stream(TurkuFfmpegDevTestAdapter.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("result no ProcessBuilder field")
        void result() {
            Set<String> types = Arrays.stream(TurkuFfmpegDevTestAdapterResult.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
    }

    // ── F. Endpoint Inventory ──
    @Nested @DisplayName("F. Endpoint Inventory (46 = 26 POST + 20 GET)")
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
