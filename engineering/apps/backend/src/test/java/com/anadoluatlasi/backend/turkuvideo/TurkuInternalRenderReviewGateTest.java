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
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderReviewState.*;
import static com.anadoluatlasi.backend.turkuvideo.TurkuInternalRenderReviewAction.*;

/**
 * FP-026 Task 02: Human Review Gate Model + Transition Validator tests.
 */
@DisplayName("FP-026 Task 02 — Human Review Gate Model + Transition Validator")
class TurkuInternalRenderReviewGateTest {

    private TurkuInternalRenderReviewTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TurkuInternalRenderReviewTransitionValidator();
    }

    // ── A. Review Ref Safety ──

    @Nested
    @DisplayName("A. Review Ref Safety")
    class ReviewRefSafety {
        @Test
        @DisplayName("create generates valid opaque ref")
        void create() {
            var ref = TurkuInternalRenderReviewRef.create();
            assertThat(ref.toOpaqueRef()).startsWith("internal-render-review:");
            assertThat(ref.toString()).startsWith("internal-render-review:");
        }

        @Test
        @DisplayName("parse valid ref")
        void parseValid() {
            var ref = TurkuInternalRenderReviewRef.create();
            var parsed = TurkuInternalRenderReviewRef.parse(ref.toOpaqueRef());
            assertThat(parsed).isNotNull();
            assertThat(parsed.getReviewId()).isEqualTo(ref.getReviewId());
        }

        @Test
        @DisplayName("reject null ref")
        void rejectNull() {
            assertThat(TurkuInternalRenderReviewRef.parse(null)).isNull();
            assertThat(TurkuInternalRenderReviewRef.isValid(null)).isFalse();
        }

        @Test
        @DisplayName("reject blank ref")
        void rejectBlank() {
            assertThat(TurkuInternalRenderReviewRef.parse("")).isNull();
            assertThat(TurkuInternalRenderReviewRef.parse("   ")).isNull();
        }

        @Test
        @DisplayName("reject wrong prefix")
        void rejectWrongPrefix() {
            assertThat(TurkuInternalRenderReviewRef.parse("internal-render-job:abc")).isNull();
            assertThat(TurkuInternalRenderReviewRef.parse("/usr/local/file")).isNull();
        }

        @Test
        @DisplayName("reject malformed UUID")
        void rejectMalformed() {
            assertThat(TurkuInternalRenderReviewRef.parse("internal-render-review:not-uuid")).isNull();
        }

        @Test
        @DisplayName("toString does not leak unsafe internals")
        void safeToString() {
            var ref = TurkuInternalRenderReviewRef.create();
            String s = ref.toString();
            assertThat(s).doesNotContain("/");
            assertThat(s).doesNotContain("\\");
            assertThat(s).doesNotContain("file:");
            assertThat(s).doesNotContain("http");
        }
    }

    // ── B. State/Action Inventory ──

    @Nested
    @DisplayName("B. State/Action Inventory")
    class StateActionInventory {
        @Test
        @DisplayName("8 review states")
        void stateCount() {
            assertThat(TurkuInternalRenderReviewState.values()).hasSize(8);
        }

        @Test
        @DisplayName("7 review actions")
        void actionCount() {
            assertThat(TurkuInternalRenderReviewAction.values()).hasSize(7);
        }

        @Test
        @DisplayName("18 reason codes")
        void reasonCount() {
            assertThat(TurkuInternalRenderReviewReasonCode.values()).hasSize(18);
        }

        @Test
        @DisplayName("no AUTO_APPROVE action")
        void noAutoApprove() {
            Set<String> names = Arrays.stream(TurkuInternalRenderReviewAction.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("AUTO_APPROVE");
        }

        @Test
        @DisplayName("no generic UPDATE_STATUS action")
        void noGenericUpdate() {
            Set<String> names = Arrays.stream(TurkuInternalRenderReviewAction.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("UPDATE_STATUS", "SET_STATUS", "PATCH_STATUS");
        }

        @Test
        @DisplayName("no PUBLISH/EXPORT/DOWNLOAD action")
        void noPublish() {
            Set<String> names = Arrays.stream(TurkuInternalRenderReviewAction.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISH", "EXPORT", "DOWNLOAD",
                    "APPROVE_FOR_PUBLIC_RELEASE");
        }

        @Test
        @DisplayName("no PUBLISHED/EXPORTED state")
        void noPublishState() {
            Set<String> names = Arrays.stream(TurkuInternalRenderReviewState.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED", "EXPORTED", "DELIVERED", "DOWNLOADED");
        }
    }

    // ── C. Allowed Transition Matrix ──

    @Nested
    @DisplayName("C. Allowed Transition Matrix")
    class AllowedTransitions {
        @Test
        @DisplayName("NOT_REQUIRED + REQUIRE -> REVIEW_REQUIRED")
        void requireReview() {
            var r = validator.validate(REVIEW_NOT_REQUIRED, REQUIRE_HUMAN_REVIEW);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(REVIEW_REQUIRED);
        }

        @Test
        @DisplayName("REQUIRED + MARK_READY -> READY_FOR_REVIEW")
        void markReady() {
            var r = validator.validate(REVIEW_REQUIRED, MARK_READY_FOR_REVIEW);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(READY_FOR_REVIEW);
        }

        @Test
        @DisplayName("READY + START -> IN_REVIEW")
        void startReview() {
            var r = validator.validate(READY_FOR_REVIEW, START_HUMAN_REVIEW);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(IN_REVIEW);
        }

        @Test
        @DisplayName("IN_REVIEW + APPROVE -> APPROVED_FOR_INTERNAL_NEXT_STEP")
        void approve() {
            var r = validator.validate(IN_REVIEW, APPROVE_FOR_INTERNAL_NEXT_STEP);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(APPROVED_FOR_INTERNAL_NEXT_STEP);
        }

        @Test
        @DisplayName("IN_REVIEW + REJECT -> REJECTED_BY_HUMAN_REVIEW")
        void reject() {
            var r = validator.validate(IN_REVIEW, REJECT_HUMAN_REVIEW);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(REJECTED_BY_HUMAN_REVIEW);
        }

        @Test
        @DisplayName("IN_REVIEW + BLOCK -> BLOCKED_BY_SAFETY")
        void blockInReview() {
            var r = validator.validate(IN_REVIEW, BLOCK_FOR_SAFETY);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(BLOCKED_BY_SAFETY);
        }

        @Test
        @DisplayName("REQUIRED + BLOCK -> BLOCKED_BY_SAFETY")
        void blockRequired() {
            var r = validator.validate(REVIEW_REQUIRED, BLOCK_FOR_SAFETY);
            assertThat(r.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("READY + BLOCK -> BLOCKED_BY_SAFETY")
        void blockReady() {
            var r = validator.validate(READY_FOR_REVIEW, BLOCK_FOR_SAFETY);
            assertThat(r.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("REQUIRED + EXPIRE -> EXPIRED_REVIEW")
        void expireRequired() {
            var r = validator.validate(REVIEW_REQUIRED, EXPIRE_HUMAN_REVIEW);
            assertThat(r.isAllowed()).isTrue();
            assertThat(r.getToState()).isEqualTo(EXPIRED_REVIEW);
        }

        @Test
        @DisplayName("READY + EXPIRE -> EXPIRED_REVIEW")
        void expireReady() {
            var r = validator.validate(READY_FOR_REVIEW, EXPIRE_HUMAN_REVIEW);
            assertThat(r.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("IN_REVIEW + EXPIRE -> EXPIRED_REVIEW")
        void expireInReview() {
            var r = validator.validate(IN_REVIEW, EXPIRE_HUMAN_REVIEW);
            assertThat(r.isAllowed()).isTrue();
        }

        @Test
        @DisplayName("11 allowed transitions")
        void transitionCount() {
            assertThat(validator.getAllowedTransitionCount()).isEqualTo(11);
        }
    }

    // ── D. Forbidden Transitions ──

    @Nested
    @DisplayName("D. Forbidden Transitions")
    class ForbiddenTransitions {
        @Test
        @DisplayName("NOT_REQUIRED cannot skip to IN_REVIEW")
        void noSkipToInReview() {
            var r = validator.validate(REVIEW_NOT_REQUIRED, START_HUMAN_REVIEW);
            assertThat(r.isAllowed()).isFalse();
        }

        @Test
        @DisplayName("NOT_REQUIRED cannot skip to APPROVED")
        void noSkipToApproved() {
            var r = validator.validate(REVIEW_NOT_REQUIRED, APPROVE_FOR_INTERNAL_NEXT_STEP);
            assertThat(r.isAllowed()).isFalse();
        }

        @Test
        @DisplayName("REQUIRED cannot skip to APPROVED directly")
        void noRequiredToApproved() {
            var r = validator.validate(REVIEW_REQUIRED, APPROVE_FOR_INTERNAL_NEXT_STEP);
            assertThat(r.isAllowed()).isFalse();
        }

        @Test
        @DisplayName("READY cannot skip to APPROVED directly")
        void noReadyToApproved() {
            var r = validator.validate(READY_FOR_REVIEW, APPROVE_FOR_INTERNAL_NEXT_STEP);
            assertThat(r.isAllowed()).isFalse();
        }

        @Test
        @DisplayName("APPROVED is terminal — all actions denied")
        void approvedTerminal() {
            assertThat(APPROVED_FOR_INTERNAL_NEXT_STEP.isTerminal()).isTrue();
            for (var action : TurkuInternalRenderReviewAction.values()) {
                var r = validator.validate(APPROVED_FOR_INTERNAL_NEXT_STEP, action);
                assertThat(r.isAllowed()).as("APPROVED + %s", action).isFalse();
                assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderReviewReasonCode.TERMINAL_STATE);
            }
        }

        @Test
        @DisplayName("REJECTED is terminal")
        void rejectedTerminal() {
            assertThat(REJECTED_BY_HUMAN_REVIEW.isTerminal()).isTrue();
            for (var action : TurkuInternalRenderReviewAction.values()) {
                var r = validator.validate(REJECTED_BY_HUMAN_REVIEW, action);
                assertThat(r.isAllowed()).as("REJECTED + %s", action).isFalse();
            }
        }

        @Test
        @DisplayName("BLOCKED is terminal")
        void blockedTerminal() {
            assertThat(TurkuInternalRenderReviewState.BLOCKED_BY_SAFETY.isTerminal()).isTrue();
            for (var action : TurkuInternalRenderReviewAction.values()) {
                var r = validator.validate(TurkuInternalRenderReviewState.BLOCKED_BY_SAFETY, action);
                assertThat(r.isAllowed()).as("BLOCKED + %s", action).isFalse();
            }
        }

        @Test
        @DisplayName("EXPIRED is terminal")
        void expiredTerminal() {
            assertThat(EXPIRED_REVIEW.isTerminal()).isTrue();
            for (var action : TurkuInternalRenderReviewAction.values()) {
                var r = validator.validate(EXPIRED_REVIEW, action);
                assertThat(r.isAllowed()).as("EXPIRED + %s", action).isFalse();
            }
        }

        @Test
        @DisplayName("null state denied")
        void nullState() {
            var r = validator.validate(null, REQUIRE_HUMAN_REVIEW);
            assertThat(r.isAllowed()).isFalse();
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderReviewReasonCode.REVIEW_REF_INVALID);
        }

        @Test
        @DisplayName("null action denied")
        void nullAction() {
            var r = validator.validate(REVIEW_NOT_REQUIRED, null);
            assertThat(r.isAllowed()).isFalse();
        }
    }

    // ── E. Safe Result/Summary DTOs ──

    @Nested
    @DisplayName("E. Safe Result/Summary DTOs")
    class SafeDtos {
        @Test
        @DisplayName("summary has humanReviewRequired=true")
        void humanReview() {
            var s = new TurkuInternalRenderReviewSummary(
                    "internal-render-review:test", "internal-render-job:test",
                    REVIEW_REQUIRED, TurkuInternalRenderReviewReasonCode.REVIEW_REQUIRED);
            assertThat(s.isHumanReviewRequired()).isTrue();
        }

        @Test
        @DisplayName("summary has autoApproved=false")
        void autoApproved() {
            var s = new TurkuInternalRenderReviewSummary(
                    "ref", "job", REVIEW_REQUIRED, TurkuInternalRenderReviewReasonCode.REVIEW_REQUIRED);
            assertThat(s.isAutoApproved()).isFalse();
        }

        @Test
        @DisplayName("summary has publicDeliveryAvailable=false")
        void publicDelivery() {
            var s = new TurkuInternalRenderReviewSummary(
                    "ref", "job", REVIEW_REQUIRED, TurkuInternalRenderReviewReasonCode.REVIEW_REQUIRED);
            assertThat(s.isPublicDeliveryAvailable()).isFalse();
        }

        @Test
        @DisplayName("summary has internalOnly=true")
        void internalOnly() {
            var s = new TurkuInternalRenderReviewSummary(
                    "ref", "job", REVIEW_REQUIRED, TurkuInternalRenderReviewReasonCode.REVIEW_REQUIRED);
            assertThat(s.isInternalOnly()).isTrue();
        }

        @Test
        @DisplayName("summary has notPublishReady=true notExportReady=true")
        void notReady() {
            var s = new TurkuInternalRenderReviewSummary(
                    "ref", "job", REVIEW_REQUIRED, TurkuInternalRenderReviewReasonCode.REVIEW_REQUIRED);
            assertThat(s.isNotPublishReady()).isTrue();
            assertThat(s.isNotExportReady()).isTrue();
        }

        @Test
        @DisplayName("summary safety flags include NO_AUTO_APPROVE")
        void safetyFlags() {
            var s = new TurkuInternalRenderReviewSummary(
                    "ref", "job", IN_REVIEW, TurkuInternalRenderReviewReasonCode.REVIEW_STARTED);
            assertThat(s.getSafetyFlags()).contains("INTERNAL_ONLY", "NO_PUBLISH",
                    "NO_EXPORT", "NO_DOWNLOAD", "NO_AUTO_APPROVE");
        }
    }

    // ── F. Reason Codes ──

    @Nested
    @DisplayName("F. Reason Codes")
    class ReasonCodes {
        @Test
        @DisplayName("allowed transition has safe reason code")
        void allowedCode() {
            var r = validator.validate(IN_REVIEW, APPROVE_FOR_INTERNAL_NEXT_STEP);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderReviewReasonCode.APPROVED_INTERNAL_ONLY);
        }

        @Test
        @DisplayName("denied transition has TRANSITION_NOT_ALLOWED")
        void deniedCode() {
            var r = validator.validate(REVIEW_NOT_REQUIRED, APPROVE_FOR_INTERNAL_NEXT_STEP);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderReviewReasonCode.TRANSITION_NOT_ALLOWED);
        }

        @Test
        @DisplayName("terminal denial has TERMINAL_STATE")
        void terminalCode() {
            var r = validator.validate(APPROVED_FOR_INTERNAL_NEXT_STEP, START_HUMAN_REVIEW);
            assertThat(r.getReasonCode()).isEqualTo(TurkuInternalRenderReviewReasonCode.TERMINAL_STATE);
        }

        @Test
        @DisplayName("PUBLIC_DELIVERY_FORBIDDEN code exists")
        void publicForbidden() {
            assertThat(TurkuInternalRenderReviewReasonCode.PUBLIC_DELIVERY_FORBIDDEN).isNotNull();
        }

        @Test
        @DisplayName("AUTO_APPROVAL_FORBIDDEN code exists")
        void autoForbidden() {
            assertThat(TurkuInternalRenderReviewReasonCode.AUTO_APPROVAL_FORBIDDEN).isNotNull();
        }

        @Test
        @DisplayName("GENERIC_STATUS_UPDATE_FORBIDDEN code exists")
        void genericForbidden() {
            assertThat(TurkuInternalRenderReviewReasonCode.GENERIC_STATUS_UPDATE_FORBIDDEN).isNotNull();
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
                TurkuInternalRenderReviewState.class,
                TurkuInternalRenderReviewAction.class,
                TurkuInternalRenderReviewReasonCode.class,
                TurkuInternalRenderReviewRef.class,
                TurkuInternalRenderReviewTransitionResult.class,
                TurkuInternalRenderReviewTransitionValidator.class,
                TurkuInternalRenderReviewSummary.class);

        @Test
        @DisplayName("7 classes x 33 forbidden fields = 0 violations")
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
}
