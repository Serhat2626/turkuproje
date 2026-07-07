package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.review.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderReviewState.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderReviewAction.*;

/**
 * FP-026 Task 03: Review Gate Service + Readiness Integration tests.
 */
@DisplayName("FP-026 Task 03 — Review Gate Service + Readiness Integration")
class TurkuInternalRenderReviewGateServiceTest {

    private TurkuInternalRenderReviewGateService service;

    @BeforeEach
    void setUp() {
        service = new TurkuInternalRenderReviewGateService();
    }

    // ── A. Service Lifecycle ──

    @Nested
    @DisplayName("A. Service Lifecycle")
    class ServiceLifecycle {
        @Test
        @DisplayName("create review returns REVIEW_NOT_REQUIRED")
        void createReview() {
            var s = service.createReview("internal-render-job:test");
            assertThat(s.getReviewState()).isEqualTo(REVIEW_NOT_REQUIRED);
            assertThat(s.getReviewRef()).startsWith("internal-render-review:");
            assertThat(s.isHumanReviewRequired()).isTrue();
            assertThat(s.isAutoApproved()).isFalse();
            assertThat(s.isInternalOnly()).isTrue();
        }

        @Test
        @DisplayName("allowed action updates summary safely")
        void allowedAction() {
            var s = service.createReview("job-ref");
            var r = service.applyAction(s, REQUIRE_HUMAN_REVIEW);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getSummary().getReviewState()).isEqualTo(REVIEW_REQUIRED);
        }

        @Test
        @DisplayName("full happy path to APPROVED_FOR_INTERNAL_NEXT_STEP")
        void fullPath() {
            var s = service.createReview("job-ref");
            s = service.applyAction(s, REQUIRE_HUMAN_REVIEW).getSummary();
            s = service.applyAction(s, MARK_READY_FOR_REVIEW).getSummary();
            s = service.applyAction(s, START_HUMAN_REVIEW).getSummary();
            var r = service.applyAction(s, APPROVE_FOR_INTERNAL_NEXT_STEP);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getSummary().getReviewState()).isEqualTo(APPROVED_FOR_INTERNAL_NEXT_STEP);
            // internal only — not publish
            assertThat(r.getSummary().isNotPublishReady()).isTrue();
            assertThat(r.getSummary().isNotExportReady()).isTrue();
            assertThat(r.getSummary().isPublicDeliveryAvailable()).isFalse();
        }

        @Test
        @DisplayName("denied action preserves previous summary")
        void deniedPreserves() {
            var s = service.createReview("job-ref");
            var r = service.applyAction(s, APPROVE_FOR_INTERNAL_NEXT_STEP);
            assertThat(r.isAllowed()).isFalse();
            assertThat(r.getSummary().getReviewState()).isEqualTo(REVIEW_NOT_REQUIRED);
        }

        @Test
        @DisplayName("terminal state denies all actions")
        void terminalDenied() {
            var s = service.createReview("job-ref");
            s = service.applyAction(s, REQUIRE_HUMAN_REVIEW).getSummary();
            var blocked = service.applyAction(s, BLOCK_FOR_SAFETY);
            assertThat(blocked.isAllowed()).isTrue();
            for (var action : TurkuInternalRenderReviewAction.values()) {
                var r = service.applyAction(blocked.getSummary(), action);
                assertThat(r.isAllowed()).as("BLOCKED + %s", action).isFalse();
            }
        }

        @Test
        @DisplayName("null summary returns REVIEW_REF_INVALID")
        void nullSummary() {
            var r = service.applyAction(null, REQUIRE_HUMAN_REVIEW);
            assertThat(r.isAllowed()).isFalse();
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderReviewReasonCode.REVIEW_REF_INVALID);
        }
    }

    // ── B. Explicit Action Boundary ──

    @Nested
    @DisplayName("B. Explicit Action Boundary")
    class ExplicitActions {
        @Test
        @DisplayName("no setStatus/updateStatus/patchStatus method")
        void noGenericUpdate() {
            Set<String> methods = Arrays.stream(TurkuInternalRenderReviewGateService.class.getDeclaredMethods())
                    .map(Method::getName).collect(Collectors.toSet());
            assertThat(methods).doesNotContain("setStatus", "updateStatus", "patchStatus");
        }

        @Test
        @DisplayName("no publish/export/download method")
        void noPublish() {
            Set<String> methods = Arrays.stream(TurkuInternalRenderReviewGateService.class.getDeclaredMethods())
                    .map(Method::getName).collect(Collectors.toSet());
            assertThat(methods).doesNotContain("publish", "export", "download",
                    "publishVideo", "exportVideo", "downloadVideo", "autoApprove");
        }
    }

    // ── C. FP-025 Readiness Integration ──

    @Nested
    @DisplayName("C. FP-025 Readiness Integration")
    class ReadinessIntegration {
        @Test
        @DisplayName("ready readiness + safe chain => READY_FOR_HUMAN_REVIEW")
        void allReady() {
            var readiness = new TurkuInternalRenderReadinessSnapshot(
                    TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
            var chain = new TurkuInternalRenderSafetyChainSnapshot(
                    true, true, true, true, "STUB", "DISABLED", "VERIFIED");
            var r = service.reviewReadiness(readiness, chain);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalRenderReviewReadinessStatus.READY_FOR_HUMAN_REVIEW);
            assertThat(r.isAutoApproved()).isFalse();
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
        }

        @Test
        @DisplayName("blocked readiness => NOT_READY_FOR_REVIEW")
        void blockedReadiness() {
            var readiness = new TurkuInternalRenderReadinessSnapshot(
                    TurkuInternalRenderReadinessStatus.BLOCKED_BY_CONFIG, List.of());
            var chain = new TurkuInternalRenderSafetyChainSnapshot(
                    true, true, true, true, "STUB", "DISABLED", "VERIFIED");
            var r = service.reviewReadiness(readiness, chain);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalRenderReviewReadinessStatus.NOT_READY_FOR_REVIEW);
            assertThat(r.getIssues()).contains(TurkuInternalRenderReviewReadinessIssue.JOB_NOT_READY);
        }

        @Test
        @DisplayName("unsafe safety chain => NOT_READY_FOR_REVIEW")
        void unsafeChain() {
            var readiness = new TurkuInternalRenderReadinessSnapshot(
                    TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
            var chain = new TurkuInternalRenderSafetyChainSnapshot(
                    false, true, true, false, "STUB", "DISABLED", "PENDING");
            var r = service.reviewReadiness(readiness, chain);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalRenderReviewReadinessStatus.NOT_READY_FOR_REVIEW);
            assertThat(r.getIssues()).contains(TurkuInternalRenderReviewReadinessIssue.ARTIFACT_VERIFICATION_FAILED);
        }

        @Test
        @DisplayName("always includes HUMAN_REVIEW_REQUIRED + PUBLIC_DELIVERY_FORBIDDEN + AUTO_APPROVAL_FORBIDDEN")
        void alwaysIncludes() {
            var readiness = new TurkuInternalRenderReadinessSnapshot(
                    TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
            var chain = new TurkuInternalRenderSafetyChainSnapshot(
                    true, true, true, true, "X", "Y", "Z");
            var r = service.reviewReadiness(readiness, chain);
            assertThat(r.getIssues()).contains(
                    TurkuInternalRenderReviewReadinessIssue.HUMAN_REVIEW_REQUIRED,
                    TurkuInternalRenderReviewReadinessIssue.PUBLIC_DELIVERY_FORBIDDEN,
                    TurkuInternalRenderReviewReadinessIssue.AUTO_APPROVAL_FORBIDDEN);
        }

        @Test
        @DisplayName("7 review readiness statuses")
        void statusCount() {
            assertThat(TurkuInternalRenderReviewReadinessStatus.values()).hasSize(7);
        }

        @Test
        @DisplayName("7 review readiness issues")
        void issueCount() {
            assertThat(TurkuInternalRenderReviewReadinessIssue.values()).hasSize(7);
        }
    }

    // ── D. Approved Does Not Enable Public Delivery ──

    @Nested
    @DisplayName("D. Approved Internal-Only Guarantees")
    class ApprovedGuarantees {
        @Test
        @DisplayName("approved summary still has publicDeliveryAvailable=false")
        void approvedNoPublic() {
            var s = service.createReview("job");
            s = service.applyAction(s, REQUIRE_HUMAN_REVIEW).getSummary();
            s = service.applyAction(s, MARK_READY_FOR_REVIEW).getSummary();
            s = service.applyAction(s, START_HUMAN_REVIEW).getSummary();
            s = service.applyAction(s, APPROVE_FOR_INTERNAL_NEXT_STEP).getSummary();
            assertThat(s.isPublicDeliveryAvailable()).isFalse();
            assertThat(s.isAutoApproved()).isFalse();
            assertThat(s.isInternalOnly()).isTrue();
            assertThat(s.isNotPublishReady()).isTrue();
            assertThat(s.getSafetyFlags()).contains("NO_AUTO_APPROVE");
        }
    }

    // ── E. Forbidden Exposure ──

    @Nested
    @DisplayName("E. Forbidden Exposure Regression")
    class ForbiddenExposure {
        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "token", "jwt", "session", "credential",
                "password", "secret", "apiKey", "authorizationHeader",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "binaryData",
                "renderedVideo", "videoFile", "downloadFile",
                "filePath", "absolutePath", "renderedVideoUrl",
                "commandLine", "ffmpegCommand", "shellCommand", "processBuilder",
                "workspacePath", "inputPath", "outputPath", "executablePath");

        private static final List<Class<?>> CLASSES = List.of(
                TurkuInternalRenderReviewReadinessStatus.class,
                TurkuInternalRenderReviewReadinessIssue.class,
                TurkuInternalRenderReviewReadinessSnapshot.class,
                TurkuInternalRenderReviewActionResult.class,
                TurkuInternalRenderReviewGateService.class);

        @Test
        @DisplayName("5 classes x 33 forbidden fields = 0 violations")
        void allSafe() {
            for (Class<?> clazz : CLASSES) {
                Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN) {
                    assertThat(fields)
                            .as("field '%s' in %s", f, clazz.getSimpleName())
                            .doesNotContain(f);
                }
            }
        }
    }

    // ── F. Endpoint Inventory ──

    @Nested
    @DisplayName("F. Endpoint Inventory (44 = 25 POST + 19 GET)")
    class Endpoints {
        @Test
        @DisplayName("44 = 25 POST + 19 GET")
        void total() {
            long post = count(AdminReviewController.class, PostMapping.class)
                    + count(AdminPipelineController.class, PostMapping.class)
                    + count(AdminWorkflowObservabilityController.class, PostMapping.class)
                    + count(TurkuVideoProjectController.class, PostMapping.class);
            long get = count(AdminReviewController.class, GetMapping.class)
                    + count(AdminPipelineController.class, GetMapping.class)
                    + count(AdminWorkflowObservabilityController.class, GetMapping.class)
                    + count(TurkuVideoProjectController.class, GetMapping.class);
            assertThat(post + get).isEqualTo(44);
            assertThat(post).isEqualTo(25);
            assertThat(get).isEqualTo(19);
        }

        private long count(Class<?> c, Class<? extends java.lang.annotation.Annotation> a) {
            return Arrays.stream(c.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(a)).count();
        }
    }
}
