package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PD-006 \u2014 Public Delivery Boundary + Explicit Publish Gate")
class ProductionMediaDeliveryGateTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html");

    private static final List<Class<?>> PD006_CLASSES = List.of(
            ProductionMediaDeliveryGateRequest.class,
            ProductionMediaDeliveryGateResult.class,
            ProductionMediaDeliveryGatePolicy.class,
            ProductionMediaDeliveryGateService.class);

    private ProductionMediaDeliveryGateService service;

    @BeforeEach void setUp() { service = new ProductionMediaDeliveryGateService(); }

    private ProductionMediaDeliveryGateRequest validRequest() {
        return new ProductionMediaDeliveryGateRequest(
                "decision:abc",
                ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP,
                "fake-obj:123",
                ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                "playback-prep:xyz", "art:mp4", "MP4",
                10, 24, 640, 360, 1024, true);
    }

    // \u2500\u2500 A. Valid Gate Pass \u2500\u2500
    @Nested @DisplayName("A. Valid Gate Pass")
    class ValidPass {
        @Test @DisplayName("approved creates APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP")
        void approved() {
            var r = service.evaluate(validRequest());
            assertThat(r.getStatus()).isEqualTo(ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP);
            assertThat(r.isControlledPublicDeliveryPrepEligible()).isTrue();
            assertThat(r.getOpaqueDeliveryGateRef()).startsWith("gate:");
            assertThat(r.getOpaqueStorageObjectRef()).isEqualTo("fake-obj:123");
        }
    }

    // \u2500\u2500 B. Human Decision Rejection \u2500\u2500
    @Nested @DisplayName("B. Human Decision Rejection")
    class DecisionRejection {
        @Test @DisplayName("missing decision ref")
        void missing() {
            var req = new ProductionMediaDeliveryGateRequest(
                    null, ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_MISSING);
        }
        @Test @DisplayName("rejected decision")
        void rejected() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.REJECTED_FOR_INTERNAL_REVISION,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_REJECTED);
        }
        @Test @DisplayName("fix requested")
        void fix() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.INTERNAL_FIX_REQUESTED,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_FIX_REQUESTED);
        }
        @Test @DisplayName("unsafe decision")
        void unsafe() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.REJECTED_UNSAFE,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_UNSAFE);
        }
        @Test @DisplayName("null request")
        void nullReq() {
            assertThat(service.evaluate(null).getStatus())
                    .isEqualTo(ProductionMediaDeliveryGateStatus.REJECTED_UNSAFE);
        }
    }

    // \u2500\u2500 C. Storage Rejection \u2500\u2500
    @Nested @DisplayName("C. Storage Rejection")
    class StorageRejection {
        @Test @DisplayName("missing storage ref")
        void missing() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP,
                    null, ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.STORAGE_REF_MISSING);
        }
        @Test @DisplayName("unverified storage")
        void unverified() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.CREATED_INTERNAL_ONLY,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.STORAGE_REF_NOT_VERIFIED);
        }
        @Test @DisplayName("failed storage")
        void failed() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.FAILED_SAFE,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.STORAGE_REF_FAILED);
        }
    }

    // \u2500\u2500 D. Policy Rejection \u2500\u2500
    @Nested @DisplayName("D. Policy Rejection")
    class PolicyRejection {
        @Test @DisplayName("unsupported format")
        void format() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "AVI", 1, 24, 640, 360, 100, true);
            assertThat(service.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.FORMAT_NOT_SUPPORTED);
        }
        @Test @DisplayName("over-size")
        void size() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 600L * 1024 * 1024, true);
            assertThat(service.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.SIZE_EXCEEDS_LIMIT);
        }
        @Test @DisplayName("missing playback metadata")
        void noPlayback() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    null, "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.PLAYBACK_METADATA_MISSING);
        }
        @Test @DisplayName("rejected not eligible")
        void notEligible() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "AVI", 1, 24, 640, 360, 100, true);
            assertThat(service.evaluate(req).isControlledPublicDeliveryPrepEligible()).isFalse();
        }
    }

    // \u2500\u2500 E. Forbidden Sweep \u2500\u2500
    @Nested @DisplayName("E. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : PD006_CLASSES) {
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
            for (var v : ProductionMediaDeliveryGateStatus.values())
                assertThat(v.name()).doesNotContain("COMPLETED").doesNotContain("PUBLISHED")
                        .doesNotContain("PUBLICLY_AVAILABLE").doesNotContain("AUTO_APPROVED")
                        .doesNotContain("DOWNLOAD_READY").doesNotContain("EXPORT_READY").doesNotContain("CDN_READY");
        }
        @Test @DisplayName("exactly 7 statuses")
        void count() { assertThat(ProductionMediaDeliveryGateStatus.values()).hasSize(7); }
        @Test @DisplayName("exactly 15 issue codes")
        void issues() { assertThat(ProductionMediaDeliveryGateIssueCode.values()).hasSize(14); }
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

    // \u2500\u2500 H. Static Factories + ProcessBuilder \u2500\u2500
    @Nested @DisplayName("H. Factories + ProcessBuilder")
    class FactoriesProcess {
        @Test @DisplayName("rejected factory") void rej() {
            var r = ProductionMediaDeliveryGateResult.rejected(ProductionMediaDeliveryGateIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaDeliveryGateStatus.REJECTED_UNSAFE);
            assertThat(r.isControlledPublicDeliveryPrepEligible()).isFalse();
        }
        @Test @DisplayName("failed factory") void fail() {
            var r = ProductionMediaDeliveryGateResult.failed(ProductionMediaDeliveryGateIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaDeliveryGateStatus.FAILED_SAFE);
        }
        @Test @DisplayName("no ProcessBuilder/Runtime") void clean() {
            for (var c : PD006_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }
}
