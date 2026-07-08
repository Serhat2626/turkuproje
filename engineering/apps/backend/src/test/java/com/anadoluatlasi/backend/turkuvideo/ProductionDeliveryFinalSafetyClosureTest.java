package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.*;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * PD-008 — Production Delivery Final Safety Closure.
 * Locks PD-001→PD-007 chain. No new capability.
 */
@DisplayName("PD-008 — Production Delivery Final Safety Closure")
class ProductionDeliveryFinalSafetyClosureTest {

    private static final List<String> FORBIDDEN = List.of(
            "stdout", "stderr", "rawLog", "commandLine", "shellCommand", "executablePath",
            "filePath", "absolutePath", "inputPath", "outputPath", "workspacePath",
            "publicUrl", "signedUrl", "cdnUrl", "url", "downloadUrl", "publishUrl", "streamUrl",
            "bucketName", "objectKey", "base64", "binaryData", "rawPayload",
            "token", "jwt", "session", "principal", "authorizationHeader",
            "accessKey", "secretKey", "secret",
            "lyrics", "scriptBody", "promptText", "copyrightedContent", "userMedia",
            "unsafeHtml", "html");

    private static final List<Class<?>> ALL_PD_CLASSES = List.of(
            // PD-001
            ProductionMediaStorageProviderType.class, ProductionMediaStorageMode.class,
            ProductionMediaStorageObjectStatus.class, ProductionMediaStorageIssueCode.class,
            ProductionMediaStorageObjectRef.class, ProductionMediaStorageResult.class,
            ProductionMediaStoragePolicy.class, FakeProductionMediaStorageGateway.class,
            // PD-002
            ProductionMediaStorageUploadStatus.class, ProductionMediaStorageUploadIssueCode.class,
            ProductionMediaStorageUploadRequest.class, ProductionMediaStorageUploadResult.class,
            ProductionMediaStorageUploadService.class,
            // PD-003
            ProductionMediaPlaybackPreparationStatus.class, ProductionMediaPlaybackPreparationIssueCode.class,
            ProductionMediaPlaybackPreparationRequest.class, ProductionMediaPlaybackPreparationResult.class,
            ProductionMediaPlaybackPreparationPolicy.class, ProductionMediaPlaybackPreparationService.class,
            // PD-005
            ProductionMediaReviewDecisionType.class, ProductionMediaReviewDecisionStatus.class,
            ProductionMediaReviewDecisionIssueCode.class, ProductionMediaReviewDecisionRequest.class,
            ProductionMediaReviewDecisionResult.class, ProductionMediaReviewDecisionService.class,
            // PD-006
            ProductionMediaDeliveryGateStatus.class, ProductionMediaDeliveryGateIssueCode.class,
            ProductionMediaDeliveryGateRequest.class, ProductionMediaDeliveryGateResult.class,
            ProductionMediaDeliveryGatePolicy.class, ProductionMediaDeliveryGateService.class,
            // PD-007
            ProductionMediaPublicPlaybackStatus.class, ProductionMediaPublicPlaybackIssueCode.class,
            ProductionMediaPublicPlaybackRequest.class, ProductionMediaPublicPlaybackResult.class,
            ProductionMediaPublicPlaybackPolicy.class, ProductionMediaPublicPlaybackService.class);

    // ── A. Full PD Forbidden Surface Sweep ──
    @Nested @DisplayName("A. Full PD Forbidden Surface Sweep (36 classes)")
    class FullForbiddenSweep {
        @Test @DisplayName("0 forbidden fields across all PD classes")
        void sweep() {
            for (var c : ALL_PD_CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("forbidden '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── B. No Publish/Auto-Approval Statuses ──
    @Nested @DisplayName("B. No COMPLETED/PUBLISHED/PUBLICLY_AVAILABLE/AUTO_APPROVED")
    class NoPublishStatuses {
        @Test @DisplayName("PD-001 object status") void pd001() {
            for (var v : ProductionMediaStorageObjectStatus.values())
                assertSafe(v.name());
        }
        @Test @DisplayName("PD-002 upload status") void pd002() {
            for (var v : ProductionMediaStorageUploadStatus.values())
                assertSafe(v.name());
        }
        @Test @DisplayName("PD-003 playback prep status") void pd003() {
            for (var v : ProductionMediaPlaybackPreparationStatus.values())
                assertSafe(v.name());
        }
        @Test @DisplayName("PD-005 decision status") void pd005() {
            for (var v : ProductionMediaReviewDecisionStatus.values())
                assertSafe(v.name());
        }
        @Test @DisplayName("PD-006 gate status") void pd006() {
            for (var v : ProductionMediaDeliveryGateStatus.values())
                assertSafe(v.name());
        }
        @Test @DisplayName("PD-007 playback status") void pd007() {
            for (var v : ProductionMediaPublicPlaybackStatus.values())
                assertSafe(v.name());
        }
        private void assertSafe(String name) {
            assertThat(name).doesNotContain("COMPLETED").doesNotContain("PUBLISHED")
                    .doesNotContain("PUBLICLY_AVAILABLE").doesNotContain("AUTO_APPROVED")
                    .doesNotContain("DOWNLOAD_READY").doesNotContain("EXPORT_READY").doesNotContain("CDN_READY");
        }
    }

    // ── C. Enum Size Inventory ──
    @Nested @DisplayName("C. Enum Size Inventory")
    class EnumSizes {
        @Test @DisplayName("PD-001: 4 providers") void p1() { assertThat(ProductionMediaStorageProviderType.values()).hasSize(4); }
        @Test @DisplayName("PD-001: 2 modes") void m1() { assertThat(ProductionMediaStorageMode.values()).hasSize(2); }
        @Test @DisplayName("PD-001: 6 obj statuses") void s1() { assertThat(ProductionMediaStorageObjectStatus.values()).hasSize(6); }
        @Test @DisplayName("PD-001: 10 issue codes") void i1() { assertThat(ProductionMediaStorageIssueCode.values()).hasSize(10); }
        @Test @DisplayName("PD-002: 7 upload statuses") void s2() { assertThat(ProductionMediaStorageUploadStatus.values()).hasSize(7); }
        @Test @DisplayName("PD-002: 11 upload issues") void i2() { assertThat(ProductionMediaStorageUploadIssueCode.values()).hasSize(11); }
        @Test @DisplayName("PD-003: 6 prep statuses") void s3() { assertThat(ProductionMediaPlaybackPreparationStatus.values()).hasSize(6); }
        @Test @DisplayName("PD-003: 9 prep issues") void i3() { assertThat(ProductionMediaPlaybackPreparationIssueCode.values()).hasSize(9); }
        @Test @DisplayName("PD-005: 4 decision types") void t5() { assertThat(ProductionMediaReviewDecisionType.values()).hasSize(4); }
        @Test @DisplayName("PD-005: 8 decision statuses") void s5() { assertThat(ProductionMediaReviewDecisionStatus.values()).hasSize(8); }
        @Test @DisplayName("PD-005: 7 decision issues") void i5() { assertThat(ProductionMediaReviewDecisionIssueCode.values()).hasSize(7); }
        @Test @DisplayName("PD-006: 7 gate statuses") void s6() { assertThat(ProductionMediaDeliveryGateStatus.values()).hasSize(7); }
        @Test @DisplayName("PD-006: 14 gate issues") void i6() { assertThat(ProductionMediaDeliveryGateIssueCode.values()).hasSize(14); }
        @Test @DisplayName("PD-007: 6 playback statuses") void s7() { assertThat(ProductionMediaPublicPlaybackStatus.values()).hasSize(6); }
        @Test @DisplayName("PD-007: 10 playback issues") void i7() { assertThat(ProductionMediaPublicPlaybackIssueCode.values()).hasSize(10); }
    }

    // ── D. Controlled Playback Gate Dependency ──
    @Nested @DisplayName("D. Controlled Playback Gate Dependency")
    class GateDependency {
        private final ProductionMediaPublicPlaybackService playbackService = new ProductionMediaPublicPlaybackService();

        @Test @DisplayName("missing gate rejects")
        void missingGate() {
            var r = playbackService.createPlaybackHandle(null);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaPublicPlaybackStatus.REJECTED_UNSAFE);
        }
        @Test @DisplayName("rejected gate cannot create handle")
        void rejectedGate() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.REJECTED_UNSAFE,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(playbackService.createPlaybackHandle(req).isControlledPlaybackEligible()).isFalse();
        }
        @Test @DisplayName("failed gate cannot create handle")
        void failedGate() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.FAILED_SAFE,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(playbackService.createPlaybackHandle(req).getStatus())
                    .isEqualTo(ProductionMediaPublicPlaybackStatus.FAILED_SAFE);
        }
        @Test @DisplayName("approved gate creates handle")
        void approvedGate() {
            var req = new ProductionMediaPublicPlaybackRequest(
                    "g:1", ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 10, 24, 640, 360, 1024, true);
            var r = playbackService.createPlaybackHandle(req);
            assertThat(r.getStatus()).isEqualTo(ProductionMediaPublicPlaybackStatus.READY_FOR_CONTROLLED_PUBLIC_PLAYBACK);
            assertThat(r.getOpaquePublicPlaybackRef()).startsWith("pub-play:");
        }
    }

    // ── E. Human Review Chain ──
    @Nested @DisplayName("E. Human Review Decision Chain")
    class ReviewChain {
        private final ProductionMediaReviewDecisionService decisionService = new ProductionMediaReviewDecisionService();
        private final ProductionMediaDeliveryGateService gateService = new ProductionMediaDeliveryGateService();

        @Test @DisplayName("unapproved review blocks gate")
        void unapprovedBlocks() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.REJECTED_FOR_INTERNAL_REVISION,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 1, 24, 640, 360, 100, true);
            assertThat(gateService.evaluate(req).getIssueCodes())
                    .contains(ProductionMediaDeliveryGateIssueCode.HUMAN_DECISION_REJECTED);
        }
        @Test @DisplayName("approved review passes gate")
        void approvedPasses() {
            var req = new ProductionMediaDeliveryGateRequest(
                    "d:1", ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP,
                    "obj:1", ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                    "pp:1", "art:1", "MP4", 10, 24, 640, 360, 1024, true);
            assertThat(gateService.evaluate(req).isControlledPublicDeliveryPrepEligible()).isTrue();
        }
    }

    // ── F. Fake Provider CI Boundary ──
    @Nested @DisplayName("F. Fake Provider CI Boundary")
    class FakeProviderBoundary {
        @Test @DisplayName("fake gateway returns FAKE_TEST_DOUBLE")
        void fakeType() {
            assertThat(new FakeProductionMediaStorageGateway().getProviderType())
                    .isEqualTo(ProductionMediaStorageProviderType.FAKE_TEST_DOUBLE);
        }
        @Test @DisplayName("fake gateway private-only result")
        void fakePrivate() {
            var r = new FakeProductionMediaStorageGateway()
                    .storePrivate("a", "MP4", 1, 24, 640, 360, 100, true, "c");
            assertThat(r.isPrivateOnly()).isTrue();
            assertThat(r.getObjectRef().getStorageMode()).isEqualTo(ProductionMediaStorageMode.PRIVATE_ONLY);
        }
    }

    // ── G. ProcessBuilder / Runtime Boundary ──
    @Nested @DisplayName("G. ProcessBuilder / Runtime Boundary")
    class ProcessBoundary {
        @Test @DisplayName("all PD classes no ProcessBuilder/Runtime")
        void clean() {
            for (var c : ALL_PD_CLASSES) {
                Set<String> types = Arrays.stream(c.getDeclaredFields())
                        .map(f -> f.getType().getSimpleName()).collect(Collectors.toSet());
                assertThat(types).as(c.getSimpleName()).doesNotContain("ProcessBuilder", "Process", "Runtime");
            }
        }
    }

    // ── H. Endpoint Inventory Final ──
    @Nested @DisplayName("H. Final Endpoint Inventory (46 = 26 POST + 20 GET)")
    class EndpointsFinal {
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
}
