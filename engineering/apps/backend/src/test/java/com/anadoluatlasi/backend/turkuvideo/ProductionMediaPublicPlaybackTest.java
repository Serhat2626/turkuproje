package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PD-007 \u2014 Controlled Public Playback / Delivery MVP")
class ProductionMediaPublicPlaybackTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html");

    private static final List<Class<?>> PD007_CLASSES = List.of(
            ProductionMediaPublicPlaybackRequest.class,
            ProductionMediaPublicPlaybackResult.class,
            ProductionMediaPublicPlaybackPolicy.class,
            ProductionMediaPublicPlaybackService.class);

    private ProductionMediaPublicPlaybackService service;

    @BeforeEach void setUp() { service = new ProductionMediaPublicPlaybackService(); }

    private ProductionMediaPublicPlaybackRequest validRequest() {
        return new ProductionMediaPublicPlaybackRequest(
                "gate:abc",
                ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                "fake-obj:123",
                ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                "playback-prep:xyz", "art:mp4", "MP4",
                10, 24, 640, 360, 1024, true);
    }

    // \u2500\u2500 A. Valid Playback Handle \u2500\u2500
    @Nested @DisplayName("A. Valid Playback Handle")
    class ValidHandle {
        @Test @DisplayName("approved gate creates READY_FOR_CONTROLLED_PUBLIC_PLAYBACK")
        void approved() {
            var r = service.createPlaybackHandle(validRequest());
            assertThat(r.getStatus()).isEqualTo(ProductionMediaPublicPlaybackStatus.READY_FOR_CONTROLLED_PUBLIC_PLAYBACK);
            assertThat(r.isControlledPlaybackEligible()).isTrue();
            assertThat(r.getOpaquePublicPlaybackRef()).startsWith("pub-play:");
            assertThat(r.getOpaqueDeliveryGateRef()).isEqualTo("gate:abc");
            assertThat(r.getMediaFormat()).isEqualTo("MP4");
        }
    }

    // \u2500\u2500 B. Gate Rejection \u2500\u2500
    @Nested @DisplayName("B. Gate Rejection")
    class GateRejection {
        @Test @DisplayName("null request") void nullReq() {
            assertThat(service.createPlaybackHandle(null).getStatus())
                    .isEqualTo(ProductionMediaPublicPlaybackStatus.REJECTED_UNSAFE);
        }
        @Test @DisplayName("missing gate ref") void missingGate() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    null, ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.createPlaybackHandle(req).getIssueCodes())
                    .contains(ProductionMediaPublicPlaybackIssueCode.DELIVERY_GATE_MISSING);
        }
        @Test @DisplayName("not eligible") void notEligible() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, false);
            assertThat(service.createPlaybackHandle(req).getIssueCodes())
                    .contains(ProductionMediaPublicPlaybackIssueCode.DELIVERY_GATE_NOT_APPROVED);
        }
        @Test @DisplayName("rejected gate") void rejectedGate() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.REJECTED_UNSAFE,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.createPlaybackHandle(req).getIssueCodes())
                    .contains(ProductionMediaPublicPlaybackIssueCode.DELIVERY_GATE_REJECTED);
        }
        @Test @DisplayName("failed gate") void failedGate() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.FAILED_SAFE,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.createPlaybackHandle(req).getStatus())
                    .isEqualTo(ProductionMediaPublicPlaybackStatus.FAILED_SAFE);
        }
    }

    // \u2500\u2500 C. Storage/Prep Rejection \u2500\u2500
    @Nested @DisplayName("C. Storage/Prep Rejection")
    class StoragePrepRejection {
        @Test @DisplayName("missing storage") void noStorage() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                    null, ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.createPlaybackHandle(req).getIssueCodes())
                    .contains(ProductionMediaPublicPlaybackIssueCode.STORAGE_REF_MISSING);
        }
        @Test @DisplayName("unverified storage") void unverified() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.CREATED_INTERNAL_ONLY,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.createPlaybackHandle(req).getIssueCodes())
                    .contains(ProductionMediaPublicPlaybackIssueCode.STORAGE_REF_NOT_VERIFIED);
        }
        @Test @DisplayName("missing playback prep") void noPrep() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    null, "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.createPlaybackHandle(req).getIssueCodes())
                    .contains(ProductionMediaPublicPlaybackIssueCode.PLAYBACK_PREP_MISSING);
        }
    }

    // \u2500\u2500 D. Policy Rejection \u2500\u2500
    @Nested @DisplayName("D. Policy Rejection")
    class PolicyRejection {
        @Test @DisplayName("unsupported format") void format() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "AVI", 1, 24, 640, 360, 100, true);
            assertThat(service.createPlaybackHandle(req).getIssueCodes())
                    .contains(ProductionMediaPublicPlaybackIssueCode.FORMAT_NOT_SUPPORTED);
        }
        @Test @DisplayName("over-size") void size() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 600L * 1024 * 1024, true);
            assertThat(service.createPlaybackHandle(req).getIssueCodes())
                    .contains(ProductionMediaPublicPlaybackIssueCode.SIZE_EXCEEDS_LIMIT);
        }
        @Test @DisplayName("rejected not eligible") void notEligible() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "AVI", 1, 24, 640, 360, 100, true);
            assertThat(service.createPlaybackHandle(req).isControlledPlaybackEligible()).isFalse();
        }
    }

    // \u2500\u2500 E. Forbidden Sweep \u2500\u2500
    @Nested @DisplayName("E. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields") void sweep() {
            for (var c : PD007_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // \u2500\u2500 F. No Publish Statuses \u2500\u2500
    @Nested @DisplayName("F. No COMPLETED / PUBLISHED / PUBLICLY_AVAILABLE / AUTO_APPROVED")
    class NoPublish {
        @Test @DisplayName("status enum safe") void status() {
            for (var v : ProductionMediaPublicPlaybackStatus.values())
                assertThat(v.name()).doesNotContain("COMPLETED").doesNotContain("PUBLISHED")
                        .doesNotContain("PUBLICLY_AVAILABLE").doesNotContain("AUTO_APPROVED")
                        .doesNotContain("DOWNLOAD_READY").doesNotContain("EXPORT_READY").doesNotContain("CDN_READY");
        }
        @Test @DisplayName("exactly 6 statuses")
        void count() { assertThat(ProductionMediaPublicPlaybackStatus.values()).hasSize(6); }
        @Test @DisplayName("exactly 10 issue codes")
        void issues() { assertThat(ProductionMediaPublicPlaybackIssueCode.values()).hasSize(10); }
    }

    // \u2500\u2500 G. Endpoint Unchanged \u2500\u2500
    @Nested @DisplayName("G. Endpoint Inventory (46 = 26 POST + 20 GET)")
    class Endpoints {
        @Test @DisplayName("total unchanged") void total() {
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

    // \u2500\u2500 H. Factories + ProcessBuilder \u2500\u2500
    @Nested @DisplayName("H. Factories + ProcessBuilder")
    class FactoriesProcess {
        @Test @DisplayName("rejected factory") void rej() {
            var r = ProductionMediaPublicPlaybackResult.rejected(ProductionMediaPublicPlaybackIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaPublicPlaybackStatus.REJECTED_UNSAFE);
            assertThat(r.isControlledPlaybackEligible()).isFalse();
        }
        @Test @DisplayName("failed factory") void fail() {
            var r = ProductionMediaPublicPlaybackResult.failed(ProductionMediaPublicPlaybackIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaPublicPlaybackStatus.FAILED_SAFE);
        }
        @Test @DisplayName("no ProcessBuilder/Runtime") void clean() {
            for (var c : PD007_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
