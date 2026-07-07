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
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderJobStatus.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderJobAction.*;

/**
 * FP-025 Task 02: Backend Render Job Lifecycle Model + Transition Validator tests.
 */
@DisplayName("FP-025 Task 02 — Render Job Lifecycle + Transition Validator")
class TurkuInternalRenderJobLifecycleTest {

    private TurkuInternalRenderJobTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TurkuInternalRenderJobTransitionValidator();
    }

    // ── A. Job Ref Safety ──

    @Nested
    @DisplayName("A. Job Ref Safety")
    class JobRefSafety {
        @Test
        @DisplayName("create generates valid opaque ref")
        void create() {
            var ref = TurkuInternalRenderJobRef.create();
            assertThat(ref.toOpaqueRef()).startsWith("internal-render-job:");
            assertThat(ref.toString()).startsWith("internal-render-job:");
        }

        @Test
        @DisplayName("parse valid ref")
        void parseValid() {
            var ref = TurkuInternalRenderJobRef.create();
            var parsed = TurkuInternalRenderJobRef.parse(ref.toOpaqueRef());
            assertThat(parsed).isNotNull();
            assertThat(parsed.getJobId()).isEqualTo(ref.getJobId());
        }

        @Test
        @DisplayName("reject null ref")
        void rejectNull() {
            assertThat(TurkuInternalRenderJobRef.parse(null)).isNull();
            assertThat(TurkuInternalRenderJobRef.isValid(null)).isFalse();
        }

        @Test
        @DisplayName("reject blank ref")
        void rejectBlank() {
            assertThat(TurkuInternalRenderJobRef.parse("")).isNull();
            assertThat(TurkuInternalRenderJobRef.parse("   ")).isNull();
        }

        @Test
        @DisplayName("reject wrong prefix")
        void rejectWrongPrefix() {
            assertThat(TurkuInternalRenderJobRef.parse("wrong-prefix:abc")).isNull();
            assertThat(TurkuInternalRenderJobRef.parse("/usr/local/file")).isNull();
        }

        @Test
        @DisplayName("reject malformed UUID")
        void rejectMalformed() {
            assertThat(TurkuInternalRenderJobRef.parse("internal-render-job:not-a-uuid")).isNull();
        }

        @Test
        @DisplayName("toString does not leak unsafe internals")
        void safeToString() {
            var ref = TurkuInternalRenderJobRef.create();
            String s = ref.toString();
            assertThat(s).doesNotContain("/");
            assertThat(s).doesNotContain("\\");
            assertThat(s).doesNotContain("file:");
            assertThat(s).doesNotContain("http");
        }
    }

    // ── B. Lifecycle State/Action Inventory ──

    @Nested
    @DisplayName("B. State/Action Inventory")
    class StateActionInventory {
        @Test
        @DisplayName("8 lifecycle states")
        void stateCount() {
            assertThat(TurkuInternalRenderJobStatus.values()).hasSize(8);
        }

        @Test
        @DisplayName("7 lifecycle actions")
        void actionCount() {
            assertThat(TurkuInternalRenderJobAction.values()).hasSize(7);
        }

        @Test
        @DisplayName("14 reason codes")
        void reasonCodeCount() {
            assertThat(TurkuInternalRenderJobReasonCode.values()).hasSize(14);
        }

        @Test
        @DisplayName("no generic UPDATE_STATUS action")
        void noGenericUpdate() {
            Set<String> actions = Arrays.stream(TurkuInternalRenderJobAction.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(actions).doesNotContain("UPDATE_STATUS", "SET_STATUS", "PATCH_STATUS");
        }

        @Test
        @DisplayName("no publish/export/download action")
        void noPublishAction() {
            Set<String> actions = Arrays.stream(TurkuInternalRenderJobAction.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(actions).doesNotContain("PUBLISH", "EXPORT", "DOWNLOAD");
        }

        @Test
        @DisplayName("no publish/export/download state")
        void noPublishState() {
            Set<String> states = Arrays.stream(TurkuInternalRenderJobStatus.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(states).doesNotContain("PUBLISHED", "EXPORTED", "DELIVERED", "DOWNLOADED");
        }
    }

    // ── C. Allowed Transition Matrix ──

    @Nested
    @DisplayName("C. Allowed Transition Matrix")
    class AllowedTransitions {
        @Test
        @DisplayName("DRAFT + VALIDATE -> READY_FOR_INTERNAL_RENDER")
        void draftToReady() {
            var r = validator.validate(DRAFT, VALIDATE_FOR_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(READY_FOR_INTERNAL_RENDER);
        }

        @Test
        @DisplayName("READY + ENQUEUE -> QUEUED_INTERNAL")
        void readyToQueued() {
            var r = validator.validate(READY_FOR_INTERNAL_RENDER, ENQUEUE_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(QUEUED_INTERNAL);
        }

        @Test
        @DisplayName("QUEUED + START -> RUNNING_INTERNAL")
        void queuedToRunning() {
            var r = validator.validate(QUEUED_INTERNAL, START_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(RUNNING_INTERNAL);
        }

        @Test
        @DisplayName("RUNNING + COMPLETE -> SUCCEEDED_INTERNAL")
        void runningToSucceeded() {
            var r = validator.validate(RUNNING_INTERNAL, COMPLETE_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(SUCCEEDED_INTERNAL);
        }

        @Test
        @DisplayName("RUNNING + FAIL -> FAILED_INTERNAL")
        void runningToFailed() {
            var r = validator.validate(RUNNING_INTERNAL, FAIL_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(FAILED_INTERNAL);
        }

        @Test
        @DisplayName("DRAFT + CANCEL -> CANCELLED_INTERNAL")
        void draftCancel() {
            var r = validator.validate(DRAFT, CANCEL_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(CANCELLED_INTERNAL);
        }

        @Test
        @DisplayName("QUEUED + CANCEL -> CANCELLED_INTERNAL")
        void queuedCancel() {
            var r = validator.validate(QUEUED_INTERNAL, CANCEL_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("RUNNING + CANCEL -> CANCELLED_INTERNAL")
        void runningCancel() {
            var r = validator.validate(RUNNING_INTERNAL, CANCEL_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("SUCCEEDED + EXPIRE -> EXPIRED_INTERNAL")
        void succeededExpire() {
            var r = validator.validate(SUCCEEDED_INTERNAL, EXPIRE_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(EXPIRED_INTERNAL);
        }

        @Test
        @DisplayName("transition count >= 13")
        void transitionCount() {
            assertThat(validator.getAllowedTransitionCount()).isGreaterThanOrEqualTo(13);
        }
    }

    // ── D. Forbidden Transitions ──

    @Nested
    @DisplayName("D. Forbidden Transitions")
    class ForbiddenTransitions {
        @Test
        @DisplayName("DRAFT cannot skip to QUEUED")
        void draftToQueued() {
            var r = validator.validate(DRAFT, ENQUEUE_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isFalse();
        }

        @Test
        @DisplayName("DRAFT cannot skip to RUNNING")
        void draftToRunning() {
            var r = validator.validate(DRAFT, START_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isFalse();
        }

        @Test
        @DisplayName("DRAFT cannot skip to SUCCEEDED")
        void draftToSucceeded() {
            var r = validator.validate(DRAFT, COMPLETE_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isFalse();
        }

        @Test
        @DisplayName("READY cannot skip to RUNNING")
        void readyToRunning() {
            var r = validator.validate(READY_FOR_INTERNAL_RENDER, START_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isFalse();
        }

        @Test
        @DisplayName("QUEUED cannot skip to SUCCEEDED")
        void queuedToSucceeded() {
            var r = validator.validate(QUEUED_INTERNAL, COMPLETE_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isFalse();
        }

        @Test
        @DisplayName("FAILED is terminal")
        void failedTerminal() {
            assertThat(FAILED_INTERNAL.isTerminal()).isTrue();
            for (var action : TurkuInternalRenderJobAction.values()) {
                var r = validator.validate(FAILED_INTERNAL, action);
                assertThat(r.isAllowed()).as("FAILED + %s", action).isFalse();
                assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderJobReasonCode.TERMINAL_STATE);
            }
        }

        @Test
        @DisplayName("CANCELLED is terminal")
        void cancelledTerminal() {
            assertThat(CANCELLED_INTERNAL.isTerminal()).isTrue();
            for (var action : TurkuInternalRenderJobAction.values()) {
                var r = validator.validate(CANCELLED_INTERNAL, action);
                assertThat(r.isAllowed()).as("CANCELLED + %s", action).isFalse();
            }
        }

        @Test
        @DisplayName("EXPIRED is terminal")
        void expiredTerminal() {
            assertThat(EXPIRED_INTERNAL.isTerminal()).isTrue();
            for (var action : TurkuInternalRenderJobAction.values()) {
                var r = validator.validate(EXPIRED_INTERNAL, action);
                assertThat(r.isAllowed()).as("EXPIRED + %s", action).isFalse();
            }
        }

        @Test
        @DisplayName("null state denied")
        void nullState() {
            var r = validator.validate(null, VALIDATE_FOR_INTERNAL_RENDER);
            assertThat(r.isAllowed()).isFalse();
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderJobReasonCode.UNKNOWN_CURRENT_STATE);
        }

        @Test
        @DisplayName("null action denied")
        void nullAction() {
            var r = validator.validate(DRAFT, null);
            assertThat(r.isAllowed()).isFalse();
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderJobReasonCode.UNKNOWN_ACTION);
        }
    }

    // ── E. Safe Reason Codes ──

    @Nested
    @DisplayName("E. Safe Reason Codes")
    class SafeReasonCodes {
        @Test
        @DisplayName("allowed transition has TRANSITION_ALLOWED code")
        void allowedCode() {
            var r = validator.validate(DRAFT, VALIDATE_FOR_INTERNAL_RENDER);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderJobReasonCode.TRANSITION_ALLOWED);
        }

        @Test
        @DisplayName("denied transition has safe reason code")
        void deniedCode() {
            var r = validator.validate(DRAFT, ENQUEUE_INTERNAL_RENDER);
            assertThat(r.getReasonCode()).isNotNull();
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderJobReasonCode.TRANSITION_NOT_ALLOWED);
        }

        @Test
        @DisplayName("terminal denial has TERMINAL_STATE code")
        void terminalCode() {
            var r = validator.validate(FAILED_INTERNAL, START_INTERNAL_RENDER);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderJobReasonCode.TERMINAL_STATE);
        }
    }

    // ── F. Progress Bounds ──

    @Nested
    @DisplayName("F. Progress Bounds")
    class ProgressBounds {
        @Test
        @DisplayName("0 is valid")
        void zero() {
            assertThat(TurkuInternalRenderJobProgress.isValidProgress(0)).isTrue();
            var p = new TurkuInternalRenderJobProgress("ref", DRAFT, 0,
                    TurkuInternalRenderJobProgress.StageLabel.HAZIRLANIYOR);
            assertThat(p.getProgressPercent()).isEqualTo(0);
        }

        @Test
        @DisplayName("100 is valid")
        void hundred() {
            assertThat(TurkuInternalRenderJobProgress.isValidProgress(100)).isTrue();
            var p = new TurkuInternalRenderJobProgress("ref", SUCCEEDED_INTERNAL, 100,
                    TurkuInternalRenderJobProgress.StageLabel.TAMAMLANDI);
            assertThat(p.getProgressPercent()).isEqualTo(100);
        }

        @Test
        @DisplayName("negative is invalid, clamped to 0")
        void negative() {
            assertThat(TurkuInternalRenderJobProgress.isValidProgress(-1)).isFalse();
            assertThat(TurkuInternalRenderJobProgress.clampProgress(-50)).isEqualTo(0);
        }

        @Test
        @DisplayName(">100 is invalid, clamped to 100")
        void overHundred() {
            assertThat(TurkuInternalRenderJobProgress.isValidProgress(101)).isFalse();
            assertThat(TurkuInternalRenderJobProgress.clampProgress(200)).isEqualTo(100);
        }

        @Test
        @DisplayName("7 stage labels")
        void stageLabels() {
            assertThat(TurkuInternalRenderJobProgress.StageLabel.values()).hasSize(7);
        }

        @Test
        @DisplayName("progress has safety flags")
        void safetyFlags() {
            var p = new TurkuInternalRenderJobProgress("ref", RUNNING_INTERNAL, 50,
                    TurkuInternalRenderJobProgress.StageLabel.DAHILI_RENDER_CALISIYOR);
            assertThat(p.isHumanReviewRequired()).isTrue();
            assertThat(p.isAutoApproved()).isFalse();
            assertThat(p.getSafetyFlags()).contains("INTERNAL_ONLY", "NO_PUBLISH");
        }
    }

    // ── G. Forbidden Exposure Regression ──

    @Nested
    @DisplayName("G. Forbidden Exposure Regression")
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
                TurkuInternalRenderJobStatus.class,
                TurkuInternalRenderJobAction.class,
                TurkuInternalRenderJobReasonCode.class,
                TurkuInternalRenderJobRef.class,
                TurkuInternalRenderJobTransitionResult.class,
                TurkuInternalRenderJobTransitionValidator.class,
                TurkuInternalRenderJobProgress.class,
                TurkuInternalRenderJobSummary.class);

        @Test
        @DisplayName("8 classes x 33 forbidden fields = 0 violations")
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

    // ── H. Endpoint Inventory ──

    @Nested
    @DisplayName("H. Endpoint Inventory (44 = 25 POST + 19 GET)")
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

    // ── I. Summary DTO Safety ──

    @Nested
    @DisplayName("I. Summary DTO Safety")
    class SummaryDtoSafety {
        @Test
        @DisplayName("summary has safety flags")
        void summaryFlags() {
            var summary = new TurkuInternalRenderJobSummary(
                    "internal-render-job:test", DRAFT, 0, "Hazırlanıyor", "trace:1");
            assertThat(summary.isHumanReviewRequired()).isTrue();
            assertThat(summary.isCopyrightReviewRequired()).isTrue();
            assertThat(summary.isAutoApproved()).isFalse();
            assertThat(summary.isNotPublishReady()).isTrue();
            assertThat(summary.isNotExportReady()).isTrue();
            assertThat(summary.getSafetyFlags()).contains("INTERNAL_ONLY", "NO_PUBLISH");
        }

        @Test
        @DisplayName("summary clamps negative progress")
        void clampNeg() {
            var summary = new TurkuInternalRenderJobSummary(
                    "ref", DRAFT, -10, "label", "trace");
            assertThat(summary.getProgressPercent()).isEqualTo(0);
        }

        @Test
        @DisplayName("summary clamps >100 progress")
        void clampOver() {
            var summary = new TurkuInternalRenderJobSummary(
                    "ref", SUCCEEDED_INTERNAL, 200, "label", "trace");
            assertThat(summary.getProgressPercent()).isEqualTo(100);
        }
    }
}
