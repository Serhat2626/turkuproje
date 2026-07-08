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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-036 — Render Manifest & Multi-Artifact Internal Bundle.
 */
@DisplayName("FP-036 — Render Bundle Manifest")
class TurkuRenderBundleManifestTest {

    private TurkuRenderBundleManifestService service;

    @BeforeEach
    void setUp() { service = new TurkuRenderBundleManifestService(); }

    private TurkuRenderBundleArtifactRef primaryClip() {
        return new TurkuRenderBundleArtifactRef("clip-ref",
                TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, TurkuRenderArtifactFormat.MP4,
                TurkuRenderArtifactVerificationStatus.PASSED, 3, 24, 640, 360, 1024 * 1024);
    }

    private TurkuRenderBundleArtifactRef poster() {
        return new TurkuRenderBundleArtifactRef("poster-ref",
                TurkuRenderBundleArtifactRole.POSTER_FRAME, TurkuRenderArtifactFormat.MP4,
                TurkuRenderArtifactVerificationStatus.PASSED, 0, 0, 640, 360, 50 * 1024);
    }

    // ── A. Single Primary → READY ──
    @Nested @DisplayName("A. Single Primary Clip → READY")
    class SinglePrimary {
        @Test @DisplayName("single verified clip → READY_FOR_INTERNAL_REVIEW_ONLY")
        void ready() {
            var m = service.build("bundle-1", List.of(primaryClip()));
            assertThat(m.getStatus()).isEqualTo(TurkuRenderBundleStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
            assertThat(m.getArtifactCount()).isEqualTo(1);
            assertThat(m.getVerifiedArtifactCount()).isEqualTo(1);
            assertThat(m.getRejectedArtifactCount()).isEqualTo(0);
            assertThat(m.isInternalOnly()).isTrue();
            assertThat(m.isPublicDeliveryAvailable()).isFalse();
            assertThat(m.isAutoApproved()).isFalse();
        }
    }

    // ── B. Multiple Safe Artifacts ──
    @Nested @DisplayName("B. Multiple Safe Artifacts")
    class MultiSafe {
        @Test @DisplayName("primary + poster → READY")
        void multiReady() {
            var m = service.build("bundle-2", List.of(primaryClip(), poster()));
            assertThat(m.getStatus()).isEqualTo(TurkuRenderBundleStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
            assertThat(m.getArtifactCount()).isEqualTo(2);
            assertThat(m.getVerifiedArtifactCount()).isEqualTo(2);
        }
    }

    // ── C. Missing Primary ──
    @Nested @DisplayName("C. Missing Primary Video")
    class MissingPrimary {
        @Test @DisplayName("no primary → REJECTED_UNSAFE")
        void noPrimary() {
            var m = service.build("bundle-3", List.of(poster()));
            assertThat(m.getStatus()).isEqualTo(TurkuRenderBundleStatus.REJECTED_UNSAFE);
            assertThat(m.getIssueCodes()).contains(TurkuRenderBundleIssueCode.MISSING_PRIMARY_VIDEO);
        }
    }

    // ── D. Duplicate Primary ──
    @Nested @DisplayName("D. Duplicate Primary Video")
    class DupPrimary {
        @Test @DisplayName("two primaries → REJECTED_UNSAFE")
        void dupPrimary() {
            var m = service.build("bundle-4", List.of(primaryClip(), primaryClip()));
            assertThat(m.getStatus()).isEqualTo(TurkuRenderBundleStatus.REJECTED_UNSAFE);
            assertThat(m.getIssueCodes()).contains(TurkuRenderBundleIssueCode.DUPLICATE_PRIMARY_VIDEO);
        }
    }

    // ── E. Too Many Artifacts ──
    @Nested @DisplayName("E. Too Many Artifacts")
    class TooMany {
        @Test @DisplayName(">10 artifacts → REJECTED_UNSAFE")
        void tooMany() {
            var list = new ArrayList<TurkuRenderBundleArtifactRef>();
            list.add(primaryClip());
            IntStream.range(0, 10).forEach(i -> list.add(poster()));
            var m = service.build("bundle-5", list);
            assertThat(m.getStatus()).isEqualTo(TurkuRenderBundleStatus.REJECTED_UNSAFE);
            assertThat(m.getIssueCodes()).contains(TurkuRenderBundleIssueCode.TOO_MANY_ARTIFACTS);
        }
    }

    // ── F. Total Size Over Limit ──
    @Nested @DisplayName("F. Total Size Over Limit")
    class OverSize {
        @Test @DisplayName(">100MB total → REJECTED_UNSAFE")
        void overSize() {
            var big = new TurkuRenderBundleArtifactRef("big-ref",
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, TurkuRenderArtifactFormat.MP4,
                    TurkuRenderArtifactVerificationStatus.PASSED, 3, 24, 640, 360, 101L * 1024 * 1024);
            var m = service.build("bundle-6", List.of(big));
            assertThat(m.getStatus()).isEqualTo(TurkuRenderBundleStatus.REJECTED_UNSAFE);
            assertThat(m.getIssueCodes()).contains(TurkuRenderBundleIssueCode.TOTAL_SIZE_EXCEEDS_LIMIT);
        }
    }

    // ── G. UNKNOWN Role ──
    @Nested @DisplayName("G. Unsupported/UNKNOWN Role")
    class UnknownRole {
        @Test @DisplayName("UNKNOWN role → REJECTED_UNSAFE")
        void unknownRole() {
            var unk = new TurkuRenderBundleArtifactRef("unk-ref",
                    TurkuRenderBundleArtifactRole.UNKNOWN, TurkuRenderArtifactFormat.MP4,
                    TurkuRenderArtifactVerificationStatus.PASSED, 3, 24, 640, 360, 1024);
            var m = service.build("bundle-7", List.of(primaryClip(), unk));
            assertThat(m.getStatus()).isEqualTo(TurkuRenderBundleStatus.REJECTED_UNSAFE);
            assertThat(m.getIssueCodes()).contains(TurkuRenderBundleIssueCode.UNSUPPORTED_ROLE);
        }
    }

    // ── H. Rejected Artifact ──
    @Nested @DisplayName("H. Rejected Artifact in Bundle")
    class RejectedArtifact {
        @Test @DisplayName("rejected artifact → REJECTED_UNSAFE")
        void rejected() {
            var bad = new TurkuRenderBundleArtifactRef("bad-ref",
                    TurkuRenderBundleArtifactRole.POSTER_FRAME, TurkuRenderArtifactFormat.MP4,
                    TurkuRenderArtifactVerificationStatus.REJECTED_FORMAT, 3, 24, 640, 360, 1024);
            var m = service.build("bundle-8", List.of(primaryClip(), bad));
            assertThat(m.getStatus()).isEqualTo(TurkuRenderBundleStatus.REJECTED_UNSAFE);
            assertThat(m.getIssueCodes()).contains(TurkuRenderBundleIssueCode.ARTIFACT_REJECTED);
        }
    }

    // ── I. Failed Artifact ──
    @Nested @DisplayName("I. Failed Artifact in Bundle")
    class FailedArtifact {
        @Test @DisplayName("failed artifact → FAILED_SAFE")
        void failed() {
            var fail = new TurkuRenderBundleArtifactRef("fail-ref",
                    TurkuRenderBundleArtifactRole.AUDIO_STUB, TurkuRenderArtifactFormat.MP4,
                    TurkuRenderArtifactVerificationStatus.FAILED_SAFE, 3, 24, 640, 360, 1024);
            var m = service.build("bundle-9", List.of(primaryClip(), fail));
            assertThat(m.getStatus()).isEqualTo(TurkuRenderBundleStatus.FAILED_SAFE);
            assertThat(m.getIssueCodes()).contains(TurkuRenderBundleIssueCode.ARTIFACT_FAILED);
        }
    }

    // ── J. FP-035 Lifecycle Integration ──
    @Nested @DisplayName("J. FP-035 Lifecycle Integration")
    class Integration {
        @Test @DisplayName("fromLifecycleReport builds valid ref")
        void fromReport() {
            var report = new TurkuRenderArtifactLifecycleReport("ref", TurkuRenderArtifactFormat.MP4,
                    3, 24, 640, 360, 1024 * 1024, TurkuRenderArtifactVerificationStatus.PASSED, List.of());
            var ref = TurkuRenderBundleArtifactRef.fromLifecycleReport(report,
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP);
            assertThat(ref.isVerified()).isTrue();
            assertThat(ref.getRole()).isEqualTo(TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP);
            var m = service.build("bundle-int", List.of(ref));
            assertThat(m.getStatus()).isEqualTo(TurkuRenderBundleStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
        }
        @Test @DisplayName("null report → NOT_VERIFIED")
        void nullReport() {
            var ref = TurkuRenderBundleArtifactRef.fromLifecycleReport(null,
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP);
            assertThat(ref.isVerified()).isFalse();
        }
    }

    // ── K. toString Safety ──
    @Nested @DisplayName("K. toString Safety")
    class ToStringSafety {
        @Test @DisplayName("toString safe")
        void safe() {
            var m = service.build("bundle-ts", List.of(primaryClip()));
            var str = m.toString();
            assertThat(str).doesNotContain("filePath", "absolutePath", "stdout", "stderr",
                    "commandLine", "storageKey", "publicUrl", "base64", "rawLog",
                    "token", "JWT", "session", "principal");
        }
    }

    // ── L. Enum Inventory ──
    @Nested @DisplayName("L. Enum Inventory")
    class Enums {
        @Test @DisplayName("6 bundle statuses")
        void statuses() { assertThat(TurkuRenderBundleStatus.values()).hasSize(6); }
        @Test @DisplayName("6 artifact roles")
        void roles() { assertThat(TurkuRenderBundleArtifactRole.values()).hasSize(6); }
        @Test @DisplayName("9 bundle issue codes")
        void issues() { assertThat(TurkuRenderBundleIssueCode.values()).hasSize(9); }
    }

    // ── M. ProcessBuilder Inventory ──
    @Nested @DisplayName("M. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("manifest service no ProcessBuilder")
        void serviceClean() {
            Set<String> types = Arrays.stream(TurkuRenderBundleManifestService.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("manifest no ProcessBuilder")
        void manifestClean() {
            Set<String> types = Arrays.stream(TurkuRenderBundleManifest.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
        }
        @Test @DisplayName("runner only allowed")
        void runnerOnly() {
            assertThat(Arrays.stream(TurkuFfmpegLocalDevProcessRunner.class.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals("generateClip"))).isTrue();
        }
    }

    // ── N. Forbidden Exposure ──
    @Nested @DisplayName("N. Forbidden Exposure")
    class Forbidden {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
                "token", "jwt", "session", "principal", "authorizationHeader");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuRenderBundleStatus.class, TurkuRenderBundleArtifactRole.class,
                TurkuRenderBundleIssueCode.class, TurkuRenderBundleArtifactRef.class,
                TurkuRenderBundleManifest.class, TurkuRenderBundleManifestService.class);

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

    // ── O. Endpoint Inventory ──
    @Nested @DisplayName("O. Endpoint Inventory (46 = 26 POST + 20 GET)")
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
