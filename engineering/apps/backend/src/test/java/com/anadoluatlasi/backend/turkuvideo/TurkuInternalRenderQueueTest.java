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
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderQueueState.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderQueueAction.*;

/**
 * FP-027 Task 02 — Queue/Retry/Expiration + Service + Integration tests.
 */
@DisplayName("FP-027 Task 02 — Queue/Retry/Expiration Model + Service")
class TurkuInternalRenderQueueTest {

    private TurkuInternalRenderQueueTransitionValidator validator;
    private TurkuInternalRenderQueueService service;

    @BeforeEach
    void setUp() {
        validator = new TurkuInternalRenderQueueTransitionValidator();
        service = new TurkuInternalRenderQueueService();
    }

    // ── A. Queue Ref Safety ──
    @Nested @DisplayName("A. Queue Ref Safety")
    class QueueRef {
        @Test @DisplayName("create opaque ref")
        void create() {
            var ref = TurkuInternalRenderQueueItemRef.create();
            assertThat(ref.toOpaqueRef()).startsWith("internal-render-queue-item:");
        }
        @Test @DisplayName("parse valid")
        void parse() {
            var ref = TurkuInternalRenderQueueItemRef.create();
            var p = TurkuInternalRenderQueueItemRef.parse(ref.toOpaqueRef());
            assertThat(p).isNotNull();
            assertThat(p.getItemId()).isEqualTo(ref.getItemId());
        }
        @Test @DisplayName("reject null/blank")
        void rejectNull() {
            assertThat(TurkuInternalRenderQueueItemRef.parse(null)).isNull();
            assertThat(TurkuInternalRenderQueueItemRef.parse("")).isNull();
            assertThat(TurkuInternalRenderQueueItemRef.parse("   ")).isNull();
        }
        @Test @DisplayName("reject wrong prefix")
        void rejectPrefix() {
            assertThat(TurkuInternalRenderQueueItemRef.parse("internal-render-job:abc")).isNull();
        }
        @Test @DisplayName("reject malformed UUID")
        void rejectMalformed() {
            assertThat(TurkuInternalRenderQueueItemRef.parse("internal-render-queue-item:not-uuid")).isNull();
        }
        @Test @DisplayName("safe toString")
        void safeToString() {
            var ref = TurkuInternalRenderQueueItemRef.create();
            assertThat(ref.toString()).doesNotContain("/", "\\", "file:", "http");
        }
    }

    // ── B. Enum Inventory ──
    @Nested @DisplayName("B. Enum Inventory")
    class Enums {
        @Test @DisplayName("10 queue states")
        void states() { assertThat(TurkuInternalRenderQueueState.values()).hasSize(10); }
        @Test @DisplayName("9 queue actions")
        void actions() { assertThat(TurkuInternalRenderQueueAction.values()).hasSize(9); }
        @Test @DisplayName("20 queue reason codes")
        void reasons() { assertThat(TurkuInternalRenderQueueReasonCode.values()).hasSize(20); }
        @Test @DisplayName("7 retry reason codes")
        void retryReasons() { assertThat(TurkuInternalRenderRetryReasonCode.values()).hasSize(7); }
        @Test @DisplayName("5 expiration reason codes")
        void expReasons() { assertThat(TurkuInternalRenderExpirationReasonCode.values()).hasSize(5); }
        @Test @DisplayName("no forbidden actions")
        void noForbidden() {
            Set<String> names = Arrays.stream(TurkuInternalRenderQueueAction.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("UPDATE_STATUS", "SET_STATUS", "PATCH_STATUS",
                    "PUBLISH", "EXPORT", "DOWNLOAD", "AUTO_APPROVE", "PUBLIC_DELIVERY");
        }
    }

    // ── C. Transition Matrix ──
    @Nested @DisplayName("C. Transition Matrix (17 allowed)")
    class Transitions {
        @Test @DisplayName("17 allowed transitions")
        void count() { assertThat(validator.getAllowedTransitionCount()).isEqualTo(17); }

        @Test @DisplayName("NOT_QUEUED->QUEUE_READY")
        void t1() { assertAllowed(NOT_QUEUED, MARK_QUEUE_READY, QUEUE_READY); }
        @Test @DisplayName("QUEUE_READY->QUEUED")
        void t2() { assertAllowed(QUEUE_READY, ENQUEUE_INTERNAL, QUEUED_INTERNAL); }
        @Test @DisplayName("QUEUED->DISPATCHING")
        void t3() { assertAllowed(QUEUED_INTERNAL, DISPATCH_INTERNAL, DISPATCHING_INTERNAL); }
        @Test @DisplayName("DISPATCHING->IN_PROGRESS")
        void t4() { assertAllowed(DISPATCHING_INTERNAL, MARK_IN_PROGRESS, IN_PROGRESS_INTERNAL); }
        @Test @DisplayName("IN_PROGRESS->SUCCEEDED")
        void t5() { assertAllowed(IN_PROGRESS_INTERNAL, MARK_SUCCEEDED, SUCCEEDED_INTERNAL); }
        @Test @DisplayName("IN_PROGRESS->FAILED")
        void t6() { assertAllowed(IN_PROGRESS_INTERNAL, MARK_FAILED, FAILED_INTERNAL); }
        @Test @DisplayName("IN_PROGRESS->RETRY_WAIT")
        void t7() { assertAllowed(IN_PROGRESS_INTERNAL, MARK_RETRY_WAIT, RETRY_WAIT_INTERNAL); }
        @Test @DisplayName("RETRY_WAIT->QUEUED (re-enqueue)")
        void t8() { assertAllowed(RETRY_WAIT_INTERNAL, ENQUEUE_INTERNAL, QUEUED_INTERNAL); }

        @Test @DisplayName("cancel from 6 states")
        void cancel() {
            for (var s : List.of(NOT_QUEUED, QUEUE_READY, QUEUED_INTERNAL, DISPATCHING_INTERNAL,
                    IN_PROGRESS_INTERNAL, RETRY_WAIT_INTERNAL)) {
                assertAllowed(s, CANCEL_INTERNAL, CANCELLED_INTERNAL);
            }
        }
        @Test @DisplayName("expire from 3 states")
        void expire() {
            for (var s : List.of(QUEUE_READY, QUEUED_INTERNAL, RETRY_WAIT_INTERNAL)) {
                assertAllowed(s, EXPIRE_INTERNAL, EXPIRED_INTERNAL);
            }
        }

        private void assertAllowed(TurkuInternalRenderQueueState from,
                TurkuInternalRenderQueueAction action, TurkuInternalRenderQueueState to) {
            var r = validator.validate(from, action);
            assertThat(r.isAllowed()).as("%s + %s", from, action).isTrue();
            assertThat(r.getToState()).isEqualTo(to);
        }
    }

    // ── D. Forbidden Transitions ──
    @Nested @DisplayName("D. Forbidden Transitions")
    class Forbidden {
        @Test @DisplayName("NOT_QUEUED cannot skip to IN_PROGRESS")
        void noSkip() {
            assertThat(validator.validate(NOT_QUEUED, MARK_IN_PROGRESS).isAllowed()).isFalse();
        }
        @Test @DisplayName("SUCCEEDED terminal")
        void succeeded() {
            assertThat(SUCCEEDED_INTERNAL.isTerminal()).isTrue();
            for (var a : TurkuInternalRenderQueueAction.values())
                assertThat(validator.validate(SUCCEEDED_INTERNAL, a).isAllowed()).isFalse();
        }
        @Test @DisplayName("FAILED terminal")
        void failed() {
            assertThat(FAILED_INTERNAL.isTerminal()).isTrue();
            for (var a : TurkuInternalRenderQueueAction.values())
                assertThat(validator.validate(FAILED_INTERNAL, a).isAllowed()).isFalse();
        }
        @Test @DisplayName("CANCELLED terminal")
        void cancelled() {
            for (var a : TurkuInternalRenderQueueAction.values())
                assertThat(validator.validate(CANCELLED_INTERNAL, a).isAllowed()).isFalse();
        }
        @Test @DisplayName("EXPIRED terminal")
        void expired() {
            for (var a : TurkuInternalRenderQueueAction.values())
                assertThat(validator.validate(EXPIRED_INTERNAL, a).isAllowed()).isFalse();
        }
        @Test @DisplayName("null state denied")
        void nullState() { assertThat(validator.validate(null, MARK_QUEUE_READY).isAllowed()).isFalse(); }
        @Test @DisplayName("null action denied")
        void nullAction() { assertThat(validator.validate(NOT_QUEUED, null).isAllowed()).isFalse(); }
    }

    // ── E. Retry Bounds ──
    @Nested @DisplayName("E. Retry Bounds")
    class RetryBounds {
        @Test @DisplayName("max attempts clamped 0-5")
        void maxClamped() {
            assertThat(new TurkuInternalRenderRetryPolicy(10, 60).getMaxAttempts()).isEqualTo(5);
            assertThat(new TurkuInternalRenderRetryPolicy(-1, 60).getMaxAttempts()).isEqualTo(0);
        }
        @Test @DisplayName("delay clamped 30-600")
        void delayClamped() {
            assertThat(new TurkuInternalRenderRetryPolicy(3, 5).getRetryDelaySeconds()).isEqualTo(30);
            assertThat(new TurkuInternalRenderRetryPolicy(3, 9999).getRetryDelaySeconds()).isEqualTo(600);
        }
        @Test @DisplayName("defaults")
        void defaults() {
            var p = TurkuInternalRenderRetryPolicy.defaults();
            assertThat(p.getMaxAttempts()).isEqualTo(3);
            assertThat(p.getRetryDelaySeconds()).isEqualTo(60);
        }
        @Test @DisplayName("snapshot retriable when under max")
        void retriable() {
            var p = TurkuInternalRenderRetryPolicy.defaults();
            var s = new TurkuInternalRenderRetrySnapshot(1, p,
                    TurkuInternalRenderRetryReasonCode.EXECUTION_FAILED_RETRIABLE);
            assertThat(s.isRetriable()).isTrue();
        }
        @Test @DisplayName("snapshot not retriable at max")
        void notRetriable() {
            var p = TurkuInternalRenderRetryPolicy.defaults();
            var s = new TurkuInternalRenderRetrySnapshot(3, p,
                    TurkuInternalRenderRetryReasonCode.MAX_RETRIES_EXHAUSTED);
            assertThat(s.isRetriable()).isFalse();
        }
    }

    // ── F. Expiration Bounds ──
    @Nested @DisplayName("F. Expiration Bounds")
    class ExpirationBounds {
        @Test @DisplayName("queue duration clamped 60-7200")
        void queueClamped() {
            assertThat(new TurkuInternalRenderExpirationPolicy(10, 60).getMaxQueueDurationSeconds()).isEqualTo(60);
            assertThat(new TurkuInternalRenderExpirationPolicy(99999, 60).getMaxQueueDurationSeconds()).isEqualTo(7200);
        }
        @Test @DisplayName("retry wait clamped 60-3600")
        void retryClamped() {
            assertThat(new TurkuInternalRenderExpirationPolicy(3600, 10).getMaxRetryWaitDurationSeconds()).isEqualTo(60);
            assertThat(new TurkuInternalRenderExpirationPolicy(3600, 99999).getMaxRetryWaitDurationSeconds()).isEqualTo(3600);
        }
        @Test @DisplayName("defaults")
        void defaults() {
            var p = TurkuInternalRenderExpirationPolicy.defaults();
            assertThat(p.getMaxQueueDurationSeconds()).isEqualTo(3600);
            assertThat(p.getMaxRetryWaitDurationSeconds()).isEqualTo(1800);
        }
        @Test @DisplayName("snapshot expired")
        void expired() {
            var p = TurkuInternalRenderExpirationPolicy.defaults();
            var s = new TurkuInternalRenderExpirationSnapshot(true,
                    TurkuInternalRenderExpirationReasonCode.QUEUE_DURATION_EXCEEDED, p);
            assertThat(s.isExpired()).isTrue();
        }
        @Test @DisplayName("snapshot not expired")
        void notExpired() {
            var p = TurkuInternalRenderExpirationPolicy.defaults();
            var s = new TurkuInternalRenderExpirationSnapshot(false,
                    TurkuInternalRenderExpirationReasonCode.NOT_EXPIRED, p);
            assertThat(s.isExpired()).isFalse();
        }
    }

    // ── G. Service Lifecycle ──
    @Nested @DisplayName("G. Service Lifecycle")
    class ServiceLifecycle {
        @Test @DisplayName("create returns NOT_QUEUED")
        void create() {
            var s = service.createQueueItem("internal-render-job:test", null);
            assertThat(s.getQueueState()).isEqualTo(NOT_QUEUED);
            assertThat(s.getQueueItemRef()).startsWith("internal-render-queue-item:");
            assertThat(s.isAutoApproved()).isFalse();
            assertThat(s.isPublicDeliveryAvailable()).isFalse();
            assertThat(s.isInternalOnly()).isTrue();
        }
        @Test @DisplayName("allowed action updates")
        void allowed() {
            var s = service.createQueueItem("job", null);
            var r = service.applyAction(s, MARK_QUEUE_READY);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getSummary().getQueueState()).isEqualTo(QUEUE_READY);
        }
        @Test @DisplayName("denied preserves previous")
        void denied() {
            var s = service.createQueueItem("job", null);
            var r = service.applyAction(s, DISPATCH_INTERNAL);
            assertThat(r.isAllowed()).isFalse();
            assertThat(r.getSummary().getQueueState()).isEqualTo(NOT_QUEUED);
        }
        @Test @DisplayName("full happy path")
        void happyPath() {
            var s = service.createQueueItem("job", "review");
            s = service.applyAction(s, MARK_QUEUE_READY).getSummary();
            s = service.applyAction(s, ENQUEUE_INTERNAL).getSummary();
            s = service.applyAction(s, DISPATCH_INTERNAL).getSummary();
            s = service.applyAction(s, MARK_IN_PROGRESS).getSummary();
            s = service.applyAction(s, MARK_SUCCEEDED).getSummary();
            assertThat(s.getQueueState()).isEqualTo(SUCCEEDED_INTERNAL);
            assertThat(s.isPublicDeliveryAvailable()).isFalse();
        }
        @Test @DisplayName("retry cycle increments attempt")
        void retryCycle() {
            var s = service.createQueueItem("job", null);
            s = service.applyAction(s, MARK_QUEUE_READY).getSummary();
            s = service.applyAction(s, ENQUEUE_INTERNAL).getSummary();
            s = service.applyAction(s, DISPATCH_INTERNAL).getSummary();
            s = service.applyAction(s, MARK_IN_PROGRESS).getSummary();
            s = service.applyAction(s, MARK_RETRY_WAIT).getSummary();
            assertThat(s.getAttemptNumber()).isEqualTo(1);
            assertThat(s.getQueueState()).isEqualTo(RETRY_WAIT_INTERNAL);
        }
        @Test @DisplayName("null summary denied")
        void nullSummary() {
            var r = service.applyAction(null, MARK_QUEUE_READY);
            assertThat(r.isAllowed()).isFalse();
        }
    }

    // ── H. FP-025/026 Dispatch Integration ──
    @Nested @DisplayName("H. FP-025/026 Dispatch Integration")
    class DispatchIntegration {
        @Test @DisplayName("dispatch ready when all prerequisites met")
        void ready() {
            var readiness = new TurkuInternalRenderReadinessSnapshot(
                    TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
            var chain = new TurkuInternalRenderSafetyChainSnapshot(
                    true, true, true, true, "STUB", "DISABLED", "VERIFIED");
            assertThat(service.isDispatchReady(readiness, chain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP)).isTrue();
        }
        @Test @DisplayName("blocked without readiness")
        void noReadiness() {
            var readiness = new TurkuInternalRenderReadinessSnapshot(
                    TurkuInternalRenderReadinessStatus.BLOCKED_BY_CONFIG, List.of());
            var chain = new TurkuInternalRenderSafetyChainSnapshot(
                    true, true, true, true, "X", "Y", "Z");
            assertThat(service.isDispatchReady(readiness, chain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP)).isFalse();
        }
        @Test @DisplayName("blocked without review approval")
        void noReview() {
            var readiness = new TurkuInternalRenderReadinessSnapshot(
                    TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
            var chain = new TurkuInternalRenderSafetyChainSnapshot(
                    true, true, true, true, "X", "Y", "Z");
            assertThat(service.isDispatchReady(readiness, chain,
                    TurkuInternalRenderReviewState.IN_REVIEW)).isFalse();
        }
        @Test @DisplayName("blocked without safe chain")
        void unsafeChain() {
            var readiness = new TurkuInternalRenderReadinessSnapshot(
                    TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY, List.of());
            var chain = new TurkuInternalRenderSafetyChainSnapshot(
                    false, true, true, false, "X", "Y", "Z");
            assertThat(service.isDispatchReady(readiness, chain,
                    TurkuInternalRenderReviewState.APPROVED_FOR_INTERNAL_NEXT_STEP)).isFalse();
        }
    }

    // ── I. Safety Guarantees ──
    @Nested @DisplayName("I. Safety Guarantees")
    class Safety {
        @Test @DisplayName("summary always publicDeliveryAvailable=false")
        void noPublic() {
            var s = service.createQueueItem("job", null);
            assertThat(s.isPublicDeliveryAvailable()).isFalse();
            s = service.applyAction(s, MARK_QUEUE_READY).getSummary();
            assertThat(s.isPublicDeliveryAvailable()).isFalse();
        }
        @Test @DisplayName("summary always autoApproved=false")
        void noAuto() {
            assertThat(service.createQueueItem("job", null).isAutoApproved()).isFalse();
        }
        @Test @DisplayName("no generic status methods")
        void noGeneric() {
            Set<String> methods = Arrays.stream(TurkuInternalRenderQueueService.class.getDeclaredMethods())
                    .map(Method::getName).collect(Collectors.toSet());
            assertThat(methods).doesNotContain("setStatus", "updateStatus", "patchStatus",
                    "publish", "export", "download", "autoApprove");
        }
    }

    // ── J. Forbidden Exposure ──
    @Nested @DisplayName("J. Forbidden Exposure")
    class ForbiddenExposure {
        private static final List<String> FORBIDDEN = List.of(
                "rawPayload", "token", "jwt", "session", "credential", "password",
                "secret", "apiKey", "authorizationHeader",
                "publicUrl", "signedUrl", "cdnUrl", "exportUrl", "publishUrl",
                "downloadUrl", "storageKey", "base64", "binaryData",
                "renderedVideo", "videoFile", "downloadFile",
                "filePath", "absolutePath", "renderedVideoUrl",
                "commandLine", "ffmpegCommand", "shellCommand", "processBuilder",
                "workspacePath", "inputPath", "outputPath", "executablePath");
        private static final List<Class<?>> CLASSES = List.of(
                TurkuInternalRenderQueueState.class,
                TurkuInternalRenderQueueAction.class,
                TurkuInternalRenderQueueReasonCode.class,
                TurkuInternalRenderQueueItemRef.class,
                TurkuInternalRenderQueueTransitionResult.class,
                TurkuInternalRenderQueueTransitionValidator.class,
                TurkuInternalRenderQueueSummary.class,
                TurkuInternalRenderQueueActionResult.class,
                TurkuInternalRenderQueueService.class,
                TurkuInternalRenderRetryPolicy.class,
                TurkuInternalRenderRetrySnapshot.class,
                TurkuInternalRenderRetryReasonCode.class,
                TurkuInternalRenderExpirationPolicy.class,
                TurkuInternalRenderExpirationSnapshot.class,
                TurkuInternalRenderExpirationReasonCode.class);

        @Test @DisplayName("15 classes x 33 forbidden = 0 violations")
        void allSafe() {
            for (Class<?> c : CLASSES) {
                Set<String> fields = Arrays.stream(c.getDeclaredFields())
                        .map(Field::getName).collect(Collectors.toSet());
                for (String f : FORBIDDEN)
                    assertThat(fields).as("field '%s' in %s", f, c.getSimpleName()).doesNotContain(f);
            }
        }
    }

    // ── K. Endpoint Inventory ──
    @Nested @DisplayName("K. Endpoint Inventory (46 = 26 POST + 20 GET)")
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
