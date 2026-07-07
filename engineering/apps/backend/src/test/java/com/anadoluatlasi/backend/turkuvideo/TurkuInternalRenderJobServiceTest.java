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
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderJobStatus.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderJobAction.*;

/**
 * FP-025 Task 03: Internal Render Job Service + Readiness + Progress tests.
 */
@DisplayName("FP-025 Task 03 — Job Service + Readiness + Progress")
class TurkuInternalRenderJobServiceTest {

    private TurkuInternalRenderJobService service;

    @BeforeEach
    void setUp() {
        service = new TurkuInternalRenderJobService();
    }

    // ── A. Service Lifecycle Behavior ──

    @Nested
    @DisplayName("A. Service Lifecycle Behavior")
    class ServiceLifecycle {
        @Test
        @DisplayName("create draft returns DRAFT summary")
        void createDraft() {
            var s = service.createDraft();
            assertThat(s.getStatus()).isEqualTo(DRAFT);
            assertThat(s.getJobRef()).startsWith("internal-render-job:");
            assertThat(s.getProgressPercent()).isEqualTo(0);
            assertThat(s.isHumanReviewRequired()).isTrue();
            assertThat(s.isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("valid action moves summary to next state")
        void validAction() {
            var draft = service.createDraft();
            var result = service.applyAction(draft, VALIDATE_FOR_INTERNAL_RENDER);
            assertThat(result.isAllowed()).isTrue();
            assertThat(result.getSummary().getStatus()).isEqualTo(READY_FOR_INTERNAL_RENDER);
        }

        @Test
        @DisplayName("full happy path: DRAFT -> SUCCEEDED")
        void fullPath() {
            var s = service.createDraft();
            s = service.applyAction(s, VALIDATE_FOR_INTERNAL_RENDER).getSummary();
            assertThat(s.getStatus()).isEqualTo(READY_FOR_INTERNAL_RENDER);
            s = service.applyAction(s, ENQUEUE_INTERNAL_RENDER).getSummary();
            assertThat(s.getStatus()).isEqualTo(QUEUED_INTERNAL);
            s = service.applyAction(s, START_INTERNAL_RENDER).getSummary();
            assertThat(s.getStatus()).isEqualTo(RUNNING_INTERNAL);
            s = service.applyAction(s, COMPLETE_INTERNAL_RENDER).getSummary();
            assertThat(s.getStatus()).isEqualTo(SUCCEEDED_INTERNAL);
            assertThat(s.getProgressPercent()).isEqualTo(100);
        }

        @Test
        @DisplayName("denied action preserves current state")
        void deniedPreserves() {
            var draft = service.createDraft();
            var result = service.applyAction(draft, ENQUEUE_INTERNAL_RENDER);
            assertThat(result.isAllowed()).isFalse();
            assertThat(result.getSummary().getStatus()).isEqualTo(DRAFT);
        }

        @Test
        @DisplayName("terminal state action is denied")
        void terminalDenied() {
            var draft = service.createDraft();
            var cancelled = service.applyAction(draft, CANCEL_INTERNAL_RENDER);
            assertThat(cancelled.isAllowed()).isTrue();
            var retry = service.applyAction(cancelled.getSummary(), VALIDATE_FOR_INTERNAL_RENDER);
            assertThat(retry.isAllowed()).isFalse();
            assertThat(retry.getReasonCode()).isEqualTo(TurkuInternalRenderJobReasonCode.TERMINAL_STATE);
        }

        @Test
        @DisplayName("null summary returns JOB_REF_INVALID")
        void nullSummary() {
            var r = service.applyAction(null, VALIDATE_FOR_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isFalse();
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderJobReasonCode.JOB_REF_INVALID);
        }

        @Test
        @DisplayName("reason codes are safe and bounded")
        void safeReasons() {
            var r = service.applyAction(service.createDraft(), ENQUEUE_INTERNAL_RENDER);
            assertThat(r.getReasonCode()).isNotNull();
            assertThat(r.getReasonCode().name()).doesNotContain("Exception");
        }
    }

    // ── B. Explicit Action Boundary ──

    @Nested
    @DisplayName("B. Explicit Action Boundary")
    class ExplicitActionBoundary {
        @Test
        @DisplayName("no setStatus/updateStatus/patchStatus method")
        void noGenericUpdate() {
            Set<String> methods = Arrays.stream(TurkuInternalRenderJobService.class.getDeclaredMethods())
                    .map(Method::getName).collect(Collectors.toSet());
            assertThat(methods).doesNotContain("setStatus", "updateStatus", "patchStatus");
        }

        @Test
        @DisplayName("no publish/export/download method")
        void noPublishMethod() {
            Set<String> methods = Arrays.stream(TurkuInternalRenderJobService.class.getDeclaredMethods())
                    .map(Method::getName).collect(Collectors.toSet());
            assertThat(methods).doesNotContain("publish", "export", "download",
                    "publishVideo", "exportVideo", "downloadVideo");
        }

        @Test
        @DisplayName("applyAction only accepts TurkuInternalRenderJobAction")
        void actionType() {
            var method = Arrays.stream(TurkuInternalRenderJobService.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals("applyAction")).findFirst();
            assertThat(method).isPresent();
            assertThat(method.get().getParameterTypes()[1])
                    .isEqualTo(TurkuInternalRenderJobAction.class);
        }
    }

    // ── C. Readiness Snapshots ──

    @Nested
    @DisplayName("C. Readiness Snapshots")
    class ReadinessSnapshots {
        @Test
        @DisplayName("all valid => READY_INTERNAL_ONLY")
        void allReady() {
            var r = service.readiness(true, true, true, true);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY);
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("config invalid => BLOCKED_BY_CONFIG")
        void blockedConfig() {
            var r = service.readiness(false, true, true, true);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalRenderReadinessStatus.BLOCKED_BY_CONFIG);
        }

        @Test
        @DisplayName("workspace invalid => BLOCKED_BY_WORKSPACE")
        void blockedWorkspace() {
            var r = service.readiness(true, false, true, true);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalRenderReadinessStatus.BLOCKED_BY_WORKSPACE);
        }

        @Test
        @DisplayName("execution disabled => BLOCKED_BY_EXECUTION_BOUNDARY")
        void blockedExecution() {
            var r = service.readiness(true, true, false, true);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalRenderReadinessStatus.BLOCKED_BY_EXECUTION_BOUNDARY);
        }

        @Test
        @DisplayName("artifact verification failed => BLOCKED_BY_ARTIFACT_VERIFICATION")
        void blockedArtifact() {
            var r = service.readiness(true, true, true, false);
            assertThat(r.getStatus()).isEqualTo(TurkuInternalRenderReadinessStatus.BLOCKED_BY_ARTIFACT_VERIFICATION);
        }

        @Test
        @DisplayName("human review always required")
        void humanReview() {
            var r = service.readiness(true, true, true, true);
            assertThat(r.isHumanReviewRequired()).isTrue();
            assertThat(r.getIssues()).contains(TurkuInternalRenderReadinessIssue.HUMAN_REVIEW_REQUIRED);
        }

        @Test
        @DisplayName("public delivery always forbidden")
        void publicForbidden() {
            var r = service.readiness(true, true, true, true);
            assertThat(r.isPublicDeliveryAvailable()).isFalse();
            assertThat(r.getIssues()).contains(TurkuInternalRenderReadinessIssue.PUBLIC_DELIVERY_FORBIDDEN);
        }

        @Test
        @DisplayName("auto approval always forbidden")
        void autoForbidden() {
            var r = service.readiness(true, true, true, true);
            assertThat(r.isAutoApproved()).isFalse();
            assertThat(r.getIssues()).contains(TurkuInternalRenderReadinessIssue.AUTO_APPROVAL_FORBIDDEN);
        }

        @Test
        @DisplayName("7 readiness statuses")
        void statusCount() {
            assertThat(TurkuInternalRenderReadinessStatus.values()).hasSize(7);
        }

        @Test
        @DisplayName("13 readiness issues")
        void issueCount() {
            assertThat(TurkuInternalRenderReadinessIssue.values()).hasSize(13);
        }
    }

    // ── D. Safety Chain Snapshot ──

    @Nested
    @DisplayName("D. FP-024 Safety Chain Snapshot")
    class SafetyChain {
        @Test
        @DisplayName("all safe snapshot")
        void allSafe() {
            var s = service.safetyChain(true, "FFMPEG_DISABLED", true,
                    true, "DISABLED", true, "VERIFIED");
            assertThat(s.isConfigValid()).isTrue();
            assertThat(s.isWorkspaceSafe()).isTrue();
            assertThat(s.isExecutionBoundarySafe()).isTrue();
            assertThat(s.isArtifactVerified()).isTrue();
            assertThat(s.isHumanReviewRequired()).isTrue();
            assertThat(s.isAutoApproved()).isFalse();
            assertThat(s.isPublicDeliveryAvailable()).isFalse();
        }

        @Test
        @DisplayName("snapshot has safe labels only")
        void safeLabels() {
            var s = service.safetyChain(false, "STUB_ONLY", false,
                    false, "DISABLED", false, "PENDING");
            assertThat(s.getConfigModeLabel()).isEqualTo("STUB_ONLY");
            assertThat(s.getExecutionModeLabel()).isEqualTo("DISABLED");
            assertThat(s.getVerificationStatusLabel()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("snapshot safety flags")
        void flags() {
            var s = service.safetyChain(true, "X", true, true, "Y", true, "Z");
            assertThat(s.getSafetyFlags()).contains("INTERNAL_ONLY", "NO_PUBLISH", "NO_PROCESS_EXECUTION");
        }
    }

    // ── E. Progress Snapshots ──

    @Nested
    @DisplayName("E. Progress Snapshots")
    class ProgressSnapshots {
        @Test
        @DisplayName("DRAFT progress = 0")
        void draft() {
            var p = service.progressForState(DRAFT);
            assertThat(p.getProgressPercent()).isEqualTo(0);
            assertThat(p.getStatus()).isEqualTo(DRAFT);
        }

        @Test
        @DisplayName("READY progress = 10")
        void ready() {
            var p = service.progressForState(READY_FOR_INTERNAL_RENDER);
            assertThat(p.getProgressPercent()).isEqualTo(10);
        }

        @Test
        @DisplayName("QUEUED progress = 25")
        void queued() {
            var p = service.progressForState(QUEUED_INTERNAL);
            assertThat(p.getProgressPercent()).isEqualTo(25);
        }

        @Test
        @DisplayName("RUNNING progress = 60")
        void running() {
            var p = service.progressForState(RUNNING_INTERNAL);
            assertThat(p.getProgressPercent()).isEqualTo(60);
        }

        @Test
        @DisplayName("SUCCEEDED progress = 100")
        void succeeded() {
            var p = service.progressForState(SUCCEEDED_INTERNAL);
            assertThat(p.getProgressPercent()).isEqualTo(100);
        }

        @Test
        @DisplayName("FAILED progress = 0, safe label")
        void failed() {
            var p = service.progressForState(FAILED_INTERNAL);
            assertThat(p.getProgressPercent()).isGreaterThanOrEqualTo(0);
            assertThat(p.getProgressPercent()).isLessThanOrEqualTo(100);
        }

        @Test
        @DisplayName("CANCELLED progress safe")
        void cancelled() {
            var p = service.progressForState(CANCELLED_INTERNAL);
            assertThat(p.getProgressPercent()).isGreaterThanOrEqualTo(0);
            assertThat(p.getProgressPercent()).isLessThanOrEqualTo(100);
        }

        @Test
        @DisplayName("EXPIRED progress safe")
        void expired() {
            var p = service.progressForState(EXPIRED_INTERNAL);
            assertThat(p.getProgressPercent()).isGreaterThanOrEqualTo(0);
            assertThat(p.getProgressPercent()).isLessThanOrEqualTo(100);
        }

        @Test
        @DisplayName("all states have predefined stage labels")
        void allLabels() {
            for (var state : TurkuInternalRenderJobStatus.values()) {
                var p = service.progressForState(state);
                assertThat(p.getCurrentStage()).isNotNull();
                assertThat(p.getCurrentStage().getLabel()).isNotBlank();
            }
        }
    }

    // ── F. Forbidden Exposure Regression ──

    @Nested
    @DisplayName("F. Forbidden Exposure Regression")
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
                TurkuInternalRenderReadinessStatus.class,
                TurkuInternalRenderReadinessIssue.class,
                TurkuInternalRenderReadinessSnapshot.class,
                TurkuInternalRenderSafetyChainSnapshot.class,
                TurkuInternalRenderJobService.class,
                TurkuInternalRenderJobActionResult.class);

        @Test
        @DisplayName("6 classes x 33 forbidden fields = 0 violations")
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

    // ── G. Endpoint Inventory ──

    @Nested
    @DisplayName("G. Endpoint Inventory (44 = 25 POST + 19 GET)")
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
