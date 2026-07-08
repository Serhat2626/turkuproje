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
 * FP-042 — Internal Render Review Package Foundation.
 */
@DisplayName("FP-042 — Render Review Package Foundation")
class TurkuRenderReviewPackageTest {

    private TurkuRenderReviewPackageService service;

    @BeforeEach
    void setUp() { service = new TurkuRenderReviewPackageService(); }

    private TurkuRenderReviewArtifactSummary primary(String ref) {
        return new TurkuRenderReviewArtifactSummary(ref,
                TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP,
                TurkuRenderArtifactVerificationStatus.PASSED,
                "mp4", 3, 24, 640, 360, 1024 * 1024);
    }

    private TurkuRenderReviewArtifactSummary secondary(String ref) {
        return new TurkuRenderReviewArtifactSummary(ref,
                TurkuRenderBundleArtifactRole.POSTER_FRAME,
                TurkuRenderArtifactVerificationStatus.PASSED,
                "mp4", 2, 24, 640, 360, 512 * 1024);
    }

    // ── A. Verified Stitched Artifact → READY_FOR_INTERNAL_REVIEW_ONLY ──
    @Nested @DisplayName("A. Verified Package")
    class Verified {
        @Test @DisplayName("1 primary → READY_FOR_INTERNAL_REVIEW_ONLY")
        void single() {
            var pkg = service.build("job-1", "bundle-1",
                    List.of(primary("art-1")), 3, 3);
            assertThat(pkg.getStatus()).isEqualTo(TurkuRenderReviewPackageStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
            assertThat(pkg.getIssueCodes()).isEmpty();
            assertThat(pkg.isInternalOnly()).isTrue();
            assertThat(pkg.getArtifactSummaries()).hasSize(1);
            assertThat(pkg.getTotalDurationSeconds()).isEqualTo(3);
        }
        @Test @DisplayName("primary + secondary → READY_FOR_INTERNAL_REVIEW_ONLY")
        void multi() {
            var pkg = service.build("job-2", "bundle-2",
                    List.of(primary("art-1"), secondary("art-2")), 3, 3);
            assertThat(pkg.getStatus()).isEqualTo(TurkuRenderReviewPackageStatus.READY_FOR_INTERNAL_REVIEW_ONLY);
            assertThat(pkg.getArtifactSummaries()).hasSize(2);
        }
    }

    // ── B. Missing Primary ──
    @Nested @DisplayName("B. Missing Primary Artifact")
    class NoPrimary {
        @Test @DisplayName("only secondary → NO_PRIMARY_ARTIFACT")
        void rejects() {
            var pkg = service.build("j", "b", List.of(secondary("s-1")), 1, 1);
            assertThat(pkg.getIssueCodes()).contains(TurkuRenderReviewPackageIssueCode.NO_PRIMARY_ARTIFACT);
        }
    }

    // ── C. Rejected Lifecycle ──
    @Nested @DisplayName("C. Rejected Lifecycle Artifact")
    class Rejected {
        @Test @DisplayName("REJECTED_DURATION → REJECTED_UNSAFE")
        void rejected() {
            var bad = new TurkuRenderReviewArtifactSummary("art-bad",
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP,
                    TurkuRenderArtifactVerificationStatus.REJECTED_DURATION,
                    "mp4", 3, 24, 640, 360, 1024*1024);
            var pkg = service.build("j", "b", List.of(bad), 1, 1);
            assertThat(pkg.getStatus()).isEqualTo(TurkuRenderReviewPackageStatus.REJECTED_UNSAFE);
            assertThat(pkg.getIssueCodes()).contains(TurkuRenderReviewPackageIssueCode.ARTIFACT_REJECTED);
        }
    }

    // ── D. Failed Lifecycle ──
    @Nested @DisplayName("D. Not Verified Artifact")
    class NotVerified {
        @Test @DisplayName("REJECTED_SIZE → ARTIFACT_NOT_VERIFIED")
        void notVerified() {
            var bad = new TurkuRenderReviewArtifactSummary("art-nv",
                    TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP,
                    TurkuRenderArtifactVerificationStatus.REJECTED_SIZE,
                    "mp4", 3, 24, 640, 360, 1024*1024);
            var pkg = service.build("j", "b", List.of(bad), 1, 1);
            assertThat(pkg.getStatus()).isEqualTo(TurkuRenderReviewPackageStatus.REJECTED_UNSAFE);
        }
    }

    // ── E. Too Many Artifacts ──
    @Nested @DisplayName("E. Too Many Artifacts")
    class TooMany {
        @Test @DisplayName("11 artifacts → TOO_MANY_ARTIFACTS")
        void rejects() {
            var list = new ArrayList<TurkuRenderReviewArtifactSummary>();
            list.add(primary("p-0"));
            for (int i = 1; i <= 10; i++) list.add(secondary("s-" + i));
            var pkg = service.build("j", "b", list, 1, 1);
            assertThat(pkg.getIssueCodes()).contains(TurkuRenderReviewPackageIssueCode.TOO_MANY_ARTIFACTS);
        }
    }

    // ── F. Null Role ──
    @Nested @DisplayName("F. Null Role")
    class NullRole {
        @Test @DisplayName("null role → ARTIFACT_ROLE_UNKNOWN")
        void nullRole() {
            var bad = new TurkuRenderReviewArtifactSummary("art-nr", null,
                    TurkuRenderArtifactVerificationStatus.PASSED,
                    "mp4", 3, 24, 640, 360, 1024*1024);
            var pkg = service.build("j", "b", List.of(bad), 1, 1);
            assertThat(pkg.getIssueCodes()).contains(TurkuRenderReviewPackageIssueCode.ARTIFACT_ROLE_UNKNOWN);
        }
    }

    // ── G. Duplicate Ref ──
    @Nested @DisplayName("G. Duplicate Artifact Ref")
    class DupRef {
        @Test @DisplayName("duplicate → DUPLICATE_ARTIFACT_REF")
        void dup() {
            var pkg = service.build("j", "b", List.of(primary("dup"), primary("dup")), 1, 1);
            assertThat(pkg.getIssueCodes()).contains(TurkuRenderReviewPackageIssueCode.DUPLICATE_ARTIFACT_REF);
        }
    }

    // ── H. Empty List ──
    @Nested @DisplayName("H. Empty List")
    class Empty {
        @Test @DisplayName("empty → MISSING_ARTIFACT_REF")
        void empty() {
            var pkg = service.build("j", "b", List.of(), 0, 0);
            assertThat(pkg.getIssueCodes()).contains(TurkuRenderReviewPackageIssueCode.MISSING_ARTIFACT_REF);
        }
    }

    // ── I. Aggregates ──
    @Nested @DisplayName("I. Aggregate Metadata")
    class Aggregates {
        @Test @DisplayName("aggregates correct")
        void ok() {
            var pkg = service.build("j", "b", List.of(primary("a1"), secondary("a2")), 3, 3);
            assertThat(pkg.getTotalDurationSeconds()).isEqualTo(5); // 3+2
            assertThat(pkg.getMaxWidth()).isEqualTo(640);
            assertThat(pkg.getMaxFps()).isEqualTo(24);
            assertThat(pkg.getTotalSizeBytes()).isEqualTo(1024*1024 + 512*1024);
            assertThat(pkg.getSceneCount()).isEqualTo(3);
            assertThat(pkg.getInputClipCount()).isEqualTo(3);
        }
    }

    // ── J. toString Safety ──
    @Nested @DisplayName("J. toString Safety")
    class ToStringSafety {
        @Test @DisplayName("safe toString")
        void safe() {
            var str = service.build("j", "b", List.of(primary("a")), 1, 1).toString();
            assertThat(str).doesNotContain("filePath", "absolutePath", "stdout", "stderr",
                    "commandLine", "storageKey", "publicUrl", "base64", "rawLog",
                    "token", "JWT", "session", "principal", "lyrics", "scriptBody");
        }
    }

    // ── K. Enum Inventory ──
    @Nested @DisplayName("K. Enum Inventory")
    class Enums {
        @Test @DisplayName("5 review package statuses")
        void statuses() { assertThat(TurkuRenderReviewPackageStatus.values()).hasSize(5); }
        @Test @DisplayName("12 review package issue codes")
        void issues() { assertThat(TurkuRenderReviewPackageIssueCode.values()).hasSize(12); }
    }

    // ── L. Forbidden Exposure ──
    @Nested @DisplayName("L. Forbidden Exposure")
    class Forbidden {
        private static final List<String> FORBIDDEN = List.of(
                "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
                "filePath", "absolutePath", "inputPath", "outputPath",
                "storageKey", "publicUrl", "signedUrl", "cdnUrl",
                "base64", "binaryData", "rawPayload", "downloadUrl", "publishUrl",
                "token", "jwt", "session", "principal", "authorizationHeader",
                "lyrics", "scriptBody", "promptText", "copyrightedContent");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuRenderReviewPackageStatus.class, TurkuRenderReviewPackageIssueCode.class,
                TurkuRenderReviewArtifactSummary.class, TurkuRenderReviewPackage.class,
                TurkuRenderReviewPackageService.class);

        @Test @DisplayName("5 classes x 29 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── M. ProcessBuilder Inventory ──
    @Nested @DisplayName("M. ProcessBuilder Inventory")
    class PBInventory {
        @Test @DisplayName("service no ProcessBuilder")
        void clean() {
            Set<String> types = Arrays.stream(TurkuRenderReviewPackageService.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
            assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
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
