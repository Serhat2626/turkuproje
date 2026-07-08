package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PD-003 — Internal Secure Playback Preparation Boundary")
class ProductionMediaPlaybackPreparationTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey",
            "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html");

    private static final List<Class<?>> PD003_CLASSES = List.of(
            ProductionMediaPlaybackPreparationRequest.class,
            ProductionMediaPlaybackPreparationResult.class,
            ProductionMediaPlaybackPreparationPolicy.class,
            ProductionMediaPlaybackPreparationService.class);

    private ProductionMediaPlaybackPreparationService service;

    @BeforeEach void setUp() { service = new ProductionMediaPlaybackPreparationService(); }

    private ProductionMediaPlaybackPreparationRequest validMp4() {
        return new ProductionMediaPlaybackPreparationRequest(
                "fake-obj:abc", "art:mp4",
                ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                "MP4", 10, 24, 640, 360, 1024);
    }

    private ProductionMediaPlaybackPreparationRequest validWebm() {
        return new ProductionMediaPlaybackPreparationRequest(
                "fake-obj:xyz", "art:webm",
                ProductionMediaStorageObjectStatus.METADATA_VERIFIED,
                "WEBM", 5, 30, 1920, 1080, 2048);
    }

    // ── A. Valid Preparation ──
    @Nested @DisplayName("A. Valid Preparation")
    class Valid {
        @Test @DisplayName("MP4 → READY_FOR_INTERNAL_PREVIEW_ONLY")
        void mp4() {
            var r = service.prepare(validMp4());
            assertThat(r.getStatus()).isEqualTo(ProductionMediaPlaybackPreparationStatus.READY_FOR_INTERNAL_PREVIEW_ONLY);
            assertThat(r.isInternalPreviewEligible()).isTrue();
            assertThat(r.getOpaquePlaybackPreparationRef()).startsWith("playback-prep:");
            assertThat(r.getOpaqueStorageObjectRef()).isEqualTo("fake-obj:abc");
            assertThat(r.getMediaFormat()).isEqualTo("MP4");
        }
        @Test @DisplayName("WEBM → READY_FOR_INTERNAL_PREVIEW_ONLY")
        void webm() {
            var r = service.prepare(validWebm());
            assertThat(r.getStatus()).isEqualTo(ProductionMediaPlaybackPreparationStatus.READY_FOR_INTERNAL_PREVIEW_ONLY);
            assertThat(r.isInternalPreviewEligible()).isTrue();
        }
    }

    // ── B. Storage Ref Rejection ──
    @Nested @DisplayName("B. Storage Ref Rejection")
    class StorageRejection {
        @Test @DisplayName("null request")
        void nullReq() {
            assertThat(service.prepare(null).getStatus())
                    .isEqualTo(ProductionMediaPlaybackPreparationStatus.REJECTED_UNSAFE);
        }
        @Test @DisplayName("missing ref")
        void missingRef() {
            var req = new ProductionMediaPlaybackPreparationRequest(
                    null, "art:x", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "MP4", 1, 24, 640, 360, 100);
            assertThat(service.prepare(req).getIssueCodes())
                    .contains(ProductionMediaPlaybackPreparationIssueCode.STORAGE_REF_MISSING);
        }
        @Test @DisplayName("rejected storage ref")
        void rejected() {
            var req = new ProductionMediaPlaybackPreparationRequest(
                    "ref:x", "art:x", ProductionMediaStorageObjectStatus.REJECTED_UNSAFE,
                    "MP4", 1, 24, 640, 360, 100);
            assertThat(service.prepare(req).getIssueCodes())
                    .contains(ProductionMediaPlaybackPreparationIssueCode.STORAGE_REF_REJECTED);
        }
        @Test @DisplayName("failed storage ref")
        void failed() {
            var req = new ProductionMediaPlaybackPreparationRequest(
                    "ref:x", "art:x", ProductionMediaStorageObjectStatus.FAILED_SAFE,
                    "MP4", 1, 24, 640, 360, 100);
            assertThat(service.prepare(req).getIssueCodes())
                    .contains(ProductionMediaPlaybackPreparationIssueCode.STORAGE_REF_FAILED);
        }
        @Test @DisplayName("unverified storage ref")
        void unverified() {
            var req = new ProductionMediaPlaybackPreparationRequest(
                    "ref:x", "art:x", ProductionMediaStorageObjectStatus.CREATED_INTERNAL_ONLY,
                    "MP4", 1, 24, 640, 360, 100);
            assertThat(service.prepare(req).getIssueCodes())
                    .contains(ProductionMediaPlaybackPreparationIssueCode.STORAGE_REF_NOT_VERIFIED);
        }
    }

    // ── C. Policy Rejection ──
    @Nested @DisplayName("C. Policy Rejection")
    class PolicyRejection {
        @Test @DisplayName("unsupported format")
        void format() {
            var req = new ProductionMediaPlaybackPreparationRequest(
                    "ref:x", "art:x", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "AVI", 1, 24, 640, 360, 100);
            assertThat(service.prepare(req).getIssueCodes())
                    .contains(ProductionMediaPlaybackPreparationIssueCode.FORMAT_NOT_SUPPORTED);
        }
        @Test @DisplayName("over-size")
        void size() {
            var req = new ProductionMediaPlaybackPreparationRequest(
                    "ref:x", "art:x", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "MP4", 1, 24, 640, 360, 600L * 1024 * 1024);
            assertThat(service.prepare(req).getIssueCodes())
                    .contains(ProductionMediaPlaybackPreparationIssueCode.SIZE_EXCEEDS_PLAYBACK_LIMIT);
        }
        @Test @DisplayName("over-duration")
        void duration() {
            var req = new ProductionMediaPlaybackPreparationRequest(
                    "ref:x", "art:x", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "MP4", 7200, 24, 640, 360, 100);
            assertThat(service.prepare(req).getIssueCodes())
                    .contains(ProductionMediaPlaybackPreparationIssueCode.DURATION_EXCEEDS_LIMIT);
        }
        @Test @DisplayName("rejected result not eligible")
        void notEligible() {
            var req = new ProductionMediaPlaybackPreparationRequest(
                    "ref:x", "art:x", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "AVI", 1, 24, 640, 360, 100);
            assertThat(service.prepare(req).isInternalPreviewEligible()).isFalse();
        }
    }

    // ── D. Forbidden Field Sweep ──
    @Nested @DisplayName("D. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("all PD-003 classes — 0 forbidden fields")
        void sweep() {
            for (var c : PD003_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── E. No Publish/Export Statuses ──
    @Nested @DisplayName("E. No READY_FOR_PUBLISH / COMPLETED / PUBLIC")
    class NoPublish {
        @Test @DisplayName("status enum safe")
        void status() {
            for (var v : ProductionMediaPlaybackPreparationStatus.values())
                assertThat(v.name()).doesNotContain("READY_FOR_PUBLISH").doesNotContain("COMPLETED")
                        .doesNotContain("PUBLIC").doesNotContain("CDN_READY")
                        .doesNotContain("AUTO_APPROVED").doesNotContain("PUBLISHED");
        }
        @Test @DisplayName("exactly 6 statuses")
        void count() { assertThat(ProductionMediaPlaybackPreparationStatus.values()).hasSize(6); }
        @Test @DisplayName("exactly 9 issue codes")
        void issueCount() { assertThat(ProductionMediaPlaybackPreparationIssueCode.values()).hasSize(9); }
    }

    // ── F. Endpoint Inventory Unchanged ──
    @Nested @DisplayName("F. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test @DisplayName("total: 46 = 26 POST + 20 GET")
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

    // ── G. ProcessBuilder Boundary ──
    @Nested @DisplayName("G. ProcessBuilder Boundary")
    class ProcessBoundary {
        @Test @DisplayName("PD-003 classes no ProcessBuilder/Runtime")
        void clean() {
            for (var c : PD003_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // ── H. Static Factories ──
    @Nested @DisplayName("H. Static Factories")
    class Factories {
        @Test @DisplayName("rejected factory")
        void rej() {
            var r = ProductionMediaPlaybackPreparationResult.rejected(
                    ProductionMediaPlaybackPreparationIssueCode.STORAGE_REF_MISSING);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaPlaybackPreparationStatus.REJECTED_UNSAFE);
            assertThat(r.isInternalPreviewEligible()).isFalse();
        }
        @Test @DisplayName("failed factory")
        void fail() {
            var r = ProductionMediaPlaybackPreparationResult.failed(
                    ProductionMediaPlaybackPreparationIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaPlaybackPreparationStatus.FAILED_SAFE);
        }
    }
}
