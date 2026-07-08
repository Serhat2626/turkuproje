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
 * FP-035 — Render Verification & Artifact Lifecycle Hardening.
 */
@DisplayName("FP-035 — Render Verification & Artifact Lifecycle")
class TurkuRenderArtifactLifecycleTest {

    private TurkuRenderArtifactLifecycleService service;

    @BeforeEach
    void setUp() { service = new TurkuRenderArtifactLifecycleService(); }

    // ── A. Valid Artifact ──
    @Nested @DisplayName("A. Valid Artifact")
    class Valid {
        @Test @DisplayName("mp4 3s 24fps 640x360 → PASSED")
        void validMp4() {
            var r = service.verify("opaque-ref", "mp4", 3, 24, 640, 360, 1024 * 1024);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.PASSED);
            assertThat(r.getIssueCodes()).isEmpty();
            assertThat(r.getOpaqueArtifactRef()).isEqualTo("opaque-ref");
            assertThat(r.getFormat()).isEqualTo(TurkuRenderArtifactFormat.MP4);
        }
        @Test @DisplayName("webm 5s 30fps 1280x720 → PASSED")
        void validWebm() {
            var r = service.verify("ref", "webm", 5, 30, 1280, 720, 50L * 1024 * 1024);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.PASSED);
        }
    }

    // ── B. Over-Duration ──
    @Nested @DisplayName("B. Over-Duration")
    class Duration {
        @Test @DisplayName("6s → REJECTED_DURATION")
        void overDuration() {
            var r = service.verify("ref", "mp4", 6, 24, 640, 360, 1024);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.REJECTED_DURATION);
            assertThat(r.getIssueCodes()).containsExactly(TurkuRenderArtifactIssueCode.DURATION_EXCEEDS_LIMIT);
        }
    }

    // ── C. Over-FPS ──
    @Nested @DisplayName("C. Over-FPS")
    class Fps {
        @Test @DisplayName("31fps → REJECTED_FPS")
        void overFps() {
            var r = service.verify("ref", "mp4", 3, 31, 640, 360, 1024);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.REJECTED_FPS);
            assertThat(r.getIssueCodes()).containsExactly(TurkuRenderArtifactIssueCode.FPS_EXCEEDS_LIMIT);
        }
    }

    // ── D. Over-Resolution ──
    @Nested @DisplayName("D. Over-Resolution")
    class Resolution {
        @Test @DisplayName("1281 width → REJECTED_RESOLUTION")
        void overWidth() {
            var r = service.verify("ref", "mp4", 3, 24, 1281, 360, 1024);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.REJECTED_RESOLUTION);
            assertThat(r.getIssueCodes()).containsExactly(TurkuRenderArtifactIssueCode.WIDTH_EXCEEDS_LIMIT);
        }
        @Test @DisplayName("721 height → REJECTED_RESOLUTION")
        void overHeight() {
            var r = service.verify("ref", "mp4", 3, 24, 640, 721, 1024);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.REJECTED_RESOLUTION);
            assertThat(r.getIssueCodes()).containsExactly(TurkuRenderArtifactIssueCode.HEIGHT_EXCEEDS_LIMIT);
        }
    }

    // ── E. Over-Size ──
    @Nested @DisplayName("E. Over-Size")
    class Size {
        @Test @DisplayName("51MB → REJECTED_SIZE")
        void overSize() {
            var r = service.verify("ref", "mp4", 3, 24, 640, 360, 51L * 1024 * 1024);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.REJECTED_SIZE);
            assertThat(r.getIssueCodes()).containsExactly(TurkuRenderArtifactIssueCode.SIZE_EXCEEDS_LIMIT);
        }
    }

    // ── F. Unsupported Format ──
    @Nested @DisplayName("F. Unsupported Format")
    class Format {
        @Test @DisplayName("avi → REJECTED_FORMAT")
        void aviRejected() {
            var r = service.verify("ref", "avi", 3, 24, 640, 360, 1024);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.REJECTED_FORMAT);
            assertThat(r.getIssueCodes()).containsExactly(TurkuRenderArtifactIssueCode.FORMAT_NOT_ALLOWED);
        }
        @Test @DisplayName("null → UNKNOWN format")
        void nullFormat() {
            assertThat(TurkuRenderArtifactFormat.fromString(null)).isEqualTo(TurkuRenderArtifactFormat.UNKNOWN);
        }
    }

    // ── G. Multiple Issues ──
    @Nested @DisplayName("G. Multiple Issues")
    class MultipleIssues {
        @Test @DisplayName("duration+fps+format → REJECTED_MULTIPLE")
        void multipleRejects() {
            var r = service.verify("ref", "avi", 6, 31, 640, 360, 1024);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.REJECTED_MULTIPLE);
            assertThat(r.getIssueCodes()).hasSize(3);
        }
    }

    // ── H. Fixture Integration ──
    @Nested @DisplayName("H. FP-034 Fixture Integration")
    class FixtureIntegration {
        @Test @DisplayName("valid fixture → PASSED")
        void validFixture() {
            var metadata = new TurkuFfmpegFixtureMetadata("ref", "mp4", 3, 24, 640, 360, 1);
            var r = service.verifyFromFixture(metadata);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.PASSED);
        }
        @Test @DisplayName("null fixture → FAILED_SAFE")
        void nullFixture() {
            var r = service.verifyFromFixture(null);
            assertThat(r.getVerificationStatus()).isEqualTo(TurkuRenderArtifactVerificationStatus.FAILED_SAFE);
            assertThat(r.getIssueCodes()).containsExactly(TurkuRenderArtifactIssueCode.METADATA_MISSING);
        }
    }

    // ── I. Cleanup ──
    @Nested @DisplayName("I. Cleanup Lifecycle")
    class Cleanup {
        @Test @DisplayName("register → SCHEDULED")
        void registerCleanup() {
            assertThat(service.registerCleanup("ref")).isEqualTo(
                    TurkuRenderArtifactLifecycleStatus.CLEANUP_SCHEDULED_OR_REGISTERED);
        }
        @Test @DisplayName("confirm → CONFIRMED")
        void confirmCleanup() {
            assertThat(service.confirmCleanup("ref")).isEqualTo(
                    TurkuRenderArtifactLifecycleStatus.CLEANUP_CONFIRMED_OR_BEST_EFFORT);
        }
        @Test @DisplayName("null ref → FAILED_SAFE")
        void nullCleanup() {
            assertThat(service.registerCleanup(null)).isEqualTo(
                    TurkuRenderArtifactLifecycleStatus.FAILED_SAFE);
        }
    }

    // ── J. toString Safety ──
    @Nested @DisplayName("J. toString Safety")
    class ToStringSafety {
        @Test @DisplayName("toString does not expose forbidden fields")
        void toStringSafe() {
            var r = service.verify("ref", "mp4", 3, 24, 640, 360, 1024);
            var str = r.toString();
            assertThat(str).doesNotContain("filePath", "absolutePath", "stdout", "stderr",
                    "commandLine", "storageKey", "publicUrl", "base64", "rawLog");
        }
    }

    // ── K. Enum Inventory ──
    @Nested @DisplayName("K. Enum Inventory")
    class Enums {
        @Test @DisplayName("7 lifecycle statuses")
        void lifecycle() { assertThat(TurkuRenderArtifactLifecycleStatus.values()).hasSize(7); }
        @Test @DisplayName("15 verification statuses")
        void verification() { assertThat(TurkuRenderArtifactVerificationStatus.values()).hasSize(15); }
        @Test @DisplayName("9 issue codes")
        void issues() { assertThat(TurkuRenderArtifactIssueCode.values()).hasSize(9); }
        @Test @DisplayName("3 formats")
        void formats() { assertThat(TurkuRenderArtifactFormat.values()).hasSize(3); }
    }

    // ── L. ProcessBuilder Inventory ──
    @Nested @DisplayName("L. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("lifecycle service no ProcessBuilder")
        void serviceClean() {
            Set<String> types = Arrays.stream(TurkuRenderArtifactLifecycleService.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("report no ProcessBuilder")
        void reportClean() {
            Set<String> types = Arrays.stream(TurkuRenderArtifactLifecycleReport.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("runner only allowed")
        void runnerOnly() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("generateClip"))).isTrue();
        }
    }

    // ── M. Forbidden Exposure ──
    @Nested @DisplayName("M. Forbidden Exposure")
    class Forbidden {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuRenderArtifactLifecycleStatus.class, TurkuRenderArtifactVerificationStatus.class,
                TurkuRenderArtifactIssueCode.class, TurkuRenderArtifactFormat.class,
                TurkuRenderArtifactLifecycleReport.class, TurkuRenderArtifactLifecycleService.class);

        @Test @DisplayName("6 classes x 19 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── N. Endpoint Inventory ──
    @Nested @DisplayName("N. Endpoint Inventory (46 = 26 POST + 20 GET)")
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
