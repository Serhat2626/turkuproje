package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PD-005 \u2014 Human Review Decision Workflow Boundary")
class ProductionMediaReviewDecisionTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html");

    private static final List<Class<?>> PD005_CLASSES = List.of(
            ProductionMediaReviewDecisionRequest.class,
            ProductionMediaReviewDecisionResult.class,
            ProductionMediaReviewDecisionService.class);

    private ProductionMediaReviewDecisionService service;

    @BeforeEach void setUp() { service = new ProductionMediaReviewDecisionService(); }

    private ProductionMediaReviewDecisionRequest validApprove() {
        return new ProductionMediaReviewDecisionRequest(
                "pkg:1", "playback-prep:abc", "fake-obj:123", "art:mp4",
                ProductionMediaReviewDecisionType.APPROVE_FOR_INTERNAL_DELIVERY_PREP,
                "MP4", 10, 24, 640, 360, 1024, true);
    }

    // \u2500\u2500 A. Approve for Internal Delivery Prep \u2500\u2500
    @Nested @DisplayName("A. Approve")
    class Approve {
        @Test @DisplayName("approve records APPROVED_FOR_INTERNAL_DELIVERY_PREP")
        void approve() {
            var r = service.recordDecision(validApprove());
            assertThat(r.getStatus()).isEqualTo(ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP);
            assertThat(r.getDecisionType()).isEqualTo(ProductionMediaReviewDecisionType.APPROVE_FOR_INTERNAL_DELIVERY_PREP);
            assertThat(r.getOpaqueDecisionRef()).startsWith("decision:");
            assertThat(r.isInternalOnly()).isTrue();
        }
    }

    // \u2500\u2500 B. Other Decisions \u2500\u2500
    @Nested @DisplayName("B. Reject/Fix/Unsafe")
    class OtherDecisions {
        @Test @DisplayName("reject for revision")
        void reject() {
            var req = new ProductionMediaReviewDecisionRequest(
                    "pkg:1", "pp:1", "obj:1", "art:1",
                    ProductionMediaReviewDecisionType.REJECT_FOR_INTERNAL_REVISION,
                    "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.recordDecision(req).getStatus())
                    .isEqualTo(ProductionMediaReviewDecisionStatus.REJECTED_FOR_INTERNAL_REVISION);
        }
        @Test @DisplayName("request fix")
        void fix() {
            var req = new ProductionMediaReviewDecisionRequest(
                    "pkg:1", "pp:1", "obj:1", "art:1",
                    ProductionMediaReviewDecisionType.REQUEST_INTERNAL_FIX,
                    "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.recordDecision(req).getStatus())
                    .isEqualTo(ProductionMediaReviewDecisionStatus.INTERNAL_FIX_REQUESTED);
        }
        @Test @DisplayName("mark unsafe")
        void unsafe() {
            var req = new ProductionMediaReviewDecisionRequest(
                    "pkg:1", "pp:1", "obj:1", "art:1",
                    ProductionMediaReviewDecisionType.MARK_UNSAFE_INTERNAL_ONLY,
                    "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.recordDecision(req).getStatus())
                    .isEqualTo(ProductionMediaReviewDecisionStatus.REJECTED_UNSAFE);
        }
    }

    // \u2500\u2500 C. Validation \u2500\u2500
    @Nested @DisplayName("C. Validation")
    class Validation {
        @Test @DisplayName("null request") void nullReq() {
            assertThat(service.recordDecision(null).getStatus())
                    .isEqualTo(ProductionMediaReviewDecisionStatus.REJECTED_UNSAFE);
        }
        @Test @DisplayName("missing storage ref") void noStorage() {
            var req = new ProductionMediaReviewDecisionRequest(
                    "pkg:1", "pp:1", null, "art:1",
                    ProductionMediaReviewDecisionType.APPROVE_FOR_INTERNAL_DELIVERY_PREP,
                    "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.recordDecision(req).getIssueCodes())
                    .contains(ProductionMediaReviewDecisionIssueCode.STORAGE_REF_MISSING);
        }
        @Test @DisplayName("not eligible") void notEligible() {
            var req = new ProductionMediaReviewDecisionRequest(
                    "pkg:1", "pp:1", "obj:1", "art:1",
                    ProductionMediaReviewDecisionType.APPROVE_FOR_INTERNAL_DELIVERY_PREP,
                    "MP4", 1, 24, 640, 360, 100, false);
            assertThat(service.recordDecision(req).getIssueCodes())
                    .contains(ProductionMediaReviewDecisionIssueCode.PREVIEW_NOT_ELIGIBLE);
        }
        @Test @DisplayName("null decision type") void nullType() {
            var req = new ProductionMediaReviewDecisionRequest(
                    "pkg:1", "pp:1", "obj:1", "art:1", null,
                    "MP4", 1, 24, 640, 360, 100, true);
            assertThat(service.recordDecision(req).getIssueCodes())
                    .contains(ProductionMediaReviewDecisionIssueCode.DECISION_TYPE_INVALID);
        }
    }

    // \u2500\u2500 D. Forbidden Sweep \u2500\u2500
    @Nested @DisplayName("D. Forbidden Field Sweep")
    class ForbiddenSweep {
        @Test @DisplayName("0 forbidden fields")
        void sweep() {
            for (var c : PD005_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("'%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // \u2500\u2500 E. No Publish Statuses \u2500\u2500
    @Nested @DisplayName("E. No READY_FOR_PUBLISH / COMPLETED / AUTO_APPROVED")
    class NoPublish {
        @Test @DisplayName("status enum safe") void status() {
            for (var v : ProductionMediaReviewDecisionStatus.values())
                assertThat(v.name()).doesNotContain("READY_FOR_PUBLISH").doesNotContain("COMPLETED")
                        .doesNotContain("PUBLIC").doesNotContain("AUTO_APPROVED").doesNotContain("PUBLISHED");
        }
        @Test @DisplayName("exactly 8 statuses")
        void count() { assertThat(ProductionMediaReviewDecisionStatus.values()).hasSize(8); }
        @Test @DisplayName("exactly 4 decision types")
        void types() { assertThat(ProductionMediaReviewDecisionType.values()).hasSize(4); }
        @Test @DisplayName("exactly 7 issue codes")
        void issues() { assertThat(ProductionMediaReviewDecisionIssueCode.values()).hasSize(7); }
    }

    // \u2500\u2500 F. Endpoint Unchanged \u2500\u2500
    @Nested @DisplayName("F. Endpoint Inventory (46 = 26 POST + 20 GET)")
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

    // \u2500\u2500 G. Static Factories \u2500\u2500
    @Nested @DisplayName("G. Static Factories")
    class Factories {
        @Test @DisplayName("rejected") void rej() {
            var r = ProductionMediaReviewDecisionResult.rejected(ProductionMediaReviewDecisionIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaReviewDecisionStatus.REJECTED_UNSAFE);
            assertThat(r.isInternalOnly()).isTrue();
        }
        @Test @DisplayName("failed") void fail() {
            var r = ProductionMediaReviewDecisionResult.failed(ProductionMediaReviewDecisionIssueCode.INTERNAL_ERROR);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaReviewDecisionStatus.FAILED_SAFE);
        }
    }
}
