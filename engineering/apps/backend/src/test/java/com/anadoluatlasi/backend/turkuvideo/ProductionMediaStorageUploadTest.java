package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * PD-002 — Private Storage Upload Pipeline + Safe Artifact Handoff.
 */
@DisplayName("PD-002 — Upload Pipeline + Artifact Handoff")
class ProductionMediaStorageUploadTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl",
            "bucketName", "objectKey",
            "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html");

    private static final List<Class<?>> PD002_CLASSES = List.of(
            ProductionMediaStorageUploadRequest.class,
            ProductionMediaStorageUploadResult.class,
            ProductionMediaStorageUploadService.class);

    private ProductionMediaStorageUploadService service;

    @BeforeEach void setUp() {
        service = new ProductionMediaStorageUploadService(new FakeProductionMediaStorageGateway());
    }

    private ProductionMediaStorageUploadRequest validMp4() {
        return new ProductionMediaStorageUploadRequest(
                "art:mp4", "MP4", TurkuRenderArtifactLifecycleStatus.VERIFIED_METADATA_ONLY,
                10, 24, 640, 360, 1024, true, "chk:abc");
    }

    private ProductionMediaStorageUploadRequest validWebm() {
        return new ProductionMediaStorageUploadRequest(
                "art:webm", "WEBM", TurkuRenderArtifactLifecycleStatus.VERIFIED_METADATA_ONLY,
                5, 30, 1920, 1080, 2048, true, "chk:xyz");
    }

    // ── A. Valid Upload ──
    @Nested @DisplayName("A. Valid Upload")
    class ValidUpload {
        @Test @DisplayName("verified MP4 creates PRIVATE_STORAGE_REF_CREATED")
        void mp4() {
            var r = service.upload(validMp4());
            assertThat(r.getStatus()).isEqualTo(ProductionMediaStorageUploadStatus.PRIVATE_STORAGE_REF_CREATED);
            assertThat(r.isPrivateOnly()).isTrue();
            assertThat(r.getPrivateObjectRef()).isNotNull();
            assertThat(r.getPrivateObjectRef().getOpaqueStorageObjectRef()).startsWith("fake-obj:");
            assertThat(r.getPrivateObjectRef().getStorageMode()).isEqualTo(ProductionMediaStorageMode.PRIVATE_ONLY);
        }
        @Test @DisplayName("verified WEBM creates PRIVATE_STORAGE_REF_CREATED")
        void webm() {
            var r = service.upload(validWebm());
            assertThat(r.getStatus()).isEqualTo(ProductionMediaStorageUploadStatus.PRIVATE_STORAGE_REF_CREATED);
        }
    }

    // ── B. Lifecycle Rejection ──
    @Nested @DisplayName("B. Lifecycle Rejection")
    class LifecycleRejection {
        @Test @DisplayName("unverified artifact rejected")
        void unverified() {
            var req = new ProductionMediaStorageUploadRequest(
                    "art:x", "MP4", TurkuRenderArtifactLifecycleStatus.CREATED_INTERNAL_ONLY,
                    1, 24, 640, 360, 100, true, "c");
            var r = service.upload(req);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaStorageUploadStatus.REJECTED_UNSAFE);
            assertThat(r.getIssueCodes()).contains(ProductionMediaStorageUploadIssueCode.ARTIFACT_NOT_VERIFIED);
        }
        @Test @DisplayName("rejected lifecycle rejected")
        void rejected() {
            var req = new ProductionMediaStorageUploadRequest(
                    "art:x", "MP4", TurkuRenderArtifactLifecycleStatus.REJECTED_UNSAFE,
                    1, 24, 640, 360, 100, true, "c");
            var r = service.upload(req);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaStorageUploadStatus.REJECTED_UNSAFE);
            assertThat(r.getIssueCodes()).contains(ProductionMediaStorageUploadIssueCode.ARTIFACT_LIFECYCLE_REJECTED);
        }
        @Test @DisplayName("failed lifecycle rejected")
        void failed() {
            var req = new ProductionMediaStorageUploadRequest(
                    "art:x", "MP4", TurkuRenderArtifactLifecycleStatus.FAILED_SAFE,
                    1, 24, 640, 360, 100, true, "c");
            var r = service.upload(req);
            assertThat(r.getIssueCodes()).contains(ProductionMediaStorageUploadIssueCode.ARTIFACT_LIFECYCLE_FAILED);
        }
        @Test @DisplayName("null request rejected")
        void nullReq() {
            var r = service.upload(null);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaStorageUploadStatus.REJECTED_UNSAFE);
        }
    }

    // ── C. Policy Rejection ──
    @Nested @DisplayName("C. Policy Rejection")
    class PolicyRejection {
        @Test @DisplayName("unsupported format")
        void format() {
            var req = new ProductionMediaStorageUploadRequest(
                    "art:x", "AVI", TurkuRenderArtifactLifecycleStatus.VERIFIED_METADATA_ONLY,
                    1, 24, 640, 360, 100, true, "c");
            var r = service.upload(req);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaStorageUploadStatus.REJECTED_UNSAFE);
            assertThat(r.getIssueCodes()).contains(ProductionMediaStorageUploadIssueCode.FORMAT_NOT_ALLOWED);
        }
        @Test @DisplayName("over-size")
        void overSize() {
            var req = new ProductionMediaStorageUploadRequest(
                    "art:x", "MP4", TurkuRenderArtifactLifecycleStatus.VERIFIED_METADATA_ONLY,
                    1, 24, 640, 360, 600L * 1024 * 1024, true, "c");
            var r = service.upload(req);
            assertThat(r.getIssueCodes()).contains(ProductionMediaStorageUploadIssueCode.SIZE_EXCEEDS_LIMIT);
        }
        @Test @DisplayName("missing checksum")
        void noChecksum() {
            var req = new ProductionMediaStorageUploadRequest(
                    "art:x", "MP4", TurkuRenderArtifactLifecycleStatus.VERIFIED_METADATA_ONLY,
                    1, 24, 640, 360, 100, false, null);
            var r = service.upload(req);
            assertThat(r.getIssueCodes()).contains(ProductionMediaStorageUploadIssueCode.CHECKSUM_MISSING);
        }
        @Test @DisplayName("rejected result has no objectRef")
        void noRef() {
            var req = new ProductionMediaStorageUploadRequest(
                    "art:x", "AVI", TurkuRenderArtifactLifecycleStatus.VERIFIED_METADATA_ONLY,
                    1, 24, 640, 360, 100, true, "c");
            assertThat(service.upload(req).getPrivateObjectRef()).isNull();
        }
    }

    // ── D. Static Factories ──
    @Nested @DisplayName("D. Static Factories")
    class Factories {
        @Test @DisplayName("rejected factory") void rej() {
            var r = ProductionMediaStorageUploadResult.rejected(ProductionMediaStorageUploadIssueCode.GATEWAY_REJECTED);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaStorageUploadStatus.REJECTED_UNSAFE);
        }
        @Test @DisplayName("failed factory") void fail() {
            var r = ProductionMediaStorageUploadResult.failed(ProductionMediaStorageUploadIssueCode.GATEWAY_FAILED);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaStorageUploadStatus.FAILED_SAFE);
        }
    }

    // ── E. Forbidden Field Sweep ──
    @Nested @DisplayName("E. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("all PD-002 classes — 0 forbidden fields")
        void sweep() {
            for (var c : PD002_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── F. No Publish/Export Statuses ──
    @Nested @DisplayName("F. No READY_FOR_PUBLISH / COMPLETED / PUBLIC")
    class NoPublish {
        @Test @DisplayName("upload status enum safe")
        void status() {
            for (var v : ProductionMediaStorageUploadStatus.values()) {
                assertThat(v.name()).doesNotContain("READY_FOR_PUBLISH").doesNotContain("COMPLETED")
                        .doesNotContain("PUBLIC").doesNotContain("CDN_READY")
                        .doesNotContain("AUTO_APPROVED").doesNotContain("PUBLISHED")
                        .doesNotContain("DOWNLOAD_READY").doesNotContain("EXPORT_READY");
            }
        }
        @Test @DisplayName("exactly 7 upload statuses")
        void count() { assertThat(ProductionMediaStorageUploadStatus.values()).hasSize(7); }
        @Test @DisplayName("exactly 11 upload issue codes")
        void issueCount() { assertThat(ProductionMediaStorageUploadIssueCode.values()).hasSize(11); }
    }

    // ── G. Endpoint Inventory Unchanged ──
    @Nested @DisplayName("G. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // ── H. ProcessBuilder Boundary ──
    @Nested @DisplayName("H. ProcessBuilder Boundary")
    class ProcessBoundary {
        @Test @DisplayName("PD-002 classes no ProcessBuilder/Runtime")
        void clean() {
            for (var c : PD002_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
