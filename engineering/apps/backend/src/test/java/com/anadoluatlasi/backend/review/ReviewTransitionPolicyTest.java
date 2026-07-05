package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-012 Task 02 — Review Workflow Core Model, Transition Policy & Audit Foundation.
 */
@DisplayName("FP-012 Task 02 — Review Transition Policy")
class ReviewTransitionPolicyTest {

    // ── 1. State Model Completeness ─────────────────────────────────

    @Nested
    @DisplayName("1. Review State Model Completeness")
    class StateModelCompleteness {

        @Test
        @DisplayName("Expected safe states exist")
        void expectedStatesExist() {
            assertThat(ReviewState.values()).containsExactlyInAnyOrder(
                    ReviewState.DRAFT,
                    ReviewState.AI_GENERATED,
                    ReviewState.NEEDS_REVIEW,
                    ReviewState.IN_REVIEW,
                    ReviewState.CHANGES_REQUESTED,
                    ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewState.REJECTED
            );
        }

        @Test
        @DisplayName("Has exactly 7 states")
        void exactlySevenStates() {
            assertThat(ReviewState.values()).hasSize(7);
        }

        @Test
        @DisplayName("Forbidden states do not exist")
        void forbiddenStatesAbsent() {
            Set<String> names = Arrays.stream(ReviewState.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain(
                    "READY_FOR_PUBLISH", "COMPLETED", "PUBLISHED", "EXPORTED"
            );
        }

        @Test
        @DisplayName("REJECTED is terminal")
        void rejectedIsTerminal() {
            assertThat(ReviewState.REJECTED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("Non-terminal states are not terminal")
        void nonTerminalStates() {
            for (ReviewState s : ReviewState.values()) {
                if (s != ReviewState.REJECTED) {
                    assertThat(s.isTerminal()).as(s.name()).isFalse();
                }
            }
        }

        @Test
        @DisplayName("Helper methods return correct values")
        void helperMethods() {
            assertThat(ReviewState.NEEDS_REVIEW.requiresHumanAction()).isTrue();
            assertThat(ReviewState.IN_REVIEW.requiresHumanAction()).isTrue();
            assertThat(ReviewState.CHANGES_REQUESTED.requiresHumanAction()).isTrue();
            assertThat(ReviewState.DRAFT.requiresHumanAction()).isFalse();
            assertThat(ReviewState.AI_GENERATED.requiresHumanAction()).isFalse();

            assertThat(ReviewState.APPROVED_FOR_INTERNAL_USE.isApprovedForInternalUse()).isTrue();
            assertThat(ReviewState.REJECTED.isRejected()).isTrue();
            assertThat(ReviewState.IN_REVIEW.isReviewActive()).isTrue();
            assertThat(ReviewState.AI_GENERATED.isAiGenerated()).isTrue();
            assertThat(ReviewState.DRAFT.isAiGenerated()).isFalse();
        }
    }

    // ── 2. Action Model Completeness ────────────────────────────────

    @Nested
    @DisplayName("2. Review Action Model Completeness")
    class ActionModelCompleteness {

        @Test
        @DisplayName("Expected safe actions exist")
        void expectedActionsExist() {
            assertThat(ReviewActionType.values()).containsExactlyInAnyOrder(
                    ReviewActionType.SUBMIT_FOR_REVIEW,
                    ReviewActionType.START_REVIEW,
                    ReviewActionType.REQUEST_CHANGES,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE,
                    ReviewActionType.REJECT,
                    ReviewActionType.RETURN_TO_DRAFT,
                    ReviewActionType.ADD_REVIEW_NOTE
            );
        }

        @Test
        @DisplayName("Has exactly 7 actions")
        void exactlySevenActions() {
            assertThat(ReviewActionType.values()).hasSize(7);
        }

        @Test
        @DisplayName("Forbidden actions do not exist")
        void forbiddenActionsAbsent() {
            Set<String> names = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain(
                    "PUBLISH", "EXPORT", "MARK_COMPLETED",
                    "READY_FOR_PUBLISH", "AUTO_APPROVE", "GENERIC_STATUS_UPDATE"
            );
        }

        @Test
        @DisplayName("ADD_REVIEW_NOTE is non-state-changing")
        void addNoteNonStateChanging() {
            assertThat(ReviewTransitionPolicy.isStateChangingAction(ReviewActionType.ADD_REVIEW_NOTE)).isFalse();
        }

        @Test
        @DisplayName("All other actions are state-changing")
        void otherActionsStateChanging() {
            for (ReviewActionType a : ReviewActionType.values()) {
                if (a != ReviewActionType.ADD_REVIEW_NOTE) {
                    assertThat(ReviewTransitionPolicy.isStateChangingAction(a))
                            .as(a.name()).isTrue();
                }
            }
        }
    }

    // ── 3. Actor Role Model ─────────────────────────────────────────

    @Nested
    @DisplayName("3. Review Actor Role Model")
    class ActorRoleModel {

        @Test
        @DisplayName("Expected roles exist")
        void expectedRolesExist() {
            assertThat(ReviewActorRole.values()).containsExactlyInAnyOrder(
                    ReviewActorRole.CREATOR,
                    ReviewActorRole.REVIEWER,
                    ReviewActorRole.ADMIN,
                    ReviewActorRole.SYSTEM
            );
        }

        @Test
        @DisplayName("CREATOR cannot perform review")
        void creatorCannotReview() {
            assertThat(ReviewActorRole.CREATOR.canPerformReview()).isFalse();
        }

        @Test
        @DisplayName("REVIEWER can perform review")
        void reviewerCanReview() {
            assertThat(ReviewActorRole.REVIEWER.canPerformReview()).isTrue();
        }

        @Test
        @DisplayName("ADMIN can perform review")
        void adminCanReview() {
            assertThat(ReviewActorRole.ADMIN.canPerformReview()).isTrue();
        }

        @Test
        @DisplayName("SYSTEM cannot perform review")
        void systemCannotReview() {
            assertThat(ReviewActorRole.SYSTEM.canPerformReview()).isFalse();
        }

        @Test
        @DisplayName("SYSTEM cannot add review notes")
        void systemCannotAddNotes() {
            assertThat(ReviewActorRole.SYSTEM.canAddReviewNote()).isFalse();
        }

        @Test
        @DisplayName("Non-SYSTEM can add notes")
        void nonSystemCanAddNotes() {
            assertThat(ReviewActorRole.CREATOR.canAddReviewNote()).isTrue();
            assertThat(ReviewActorRole.REVIEWER.canAddReviewNote()).isTrue();
            assertThat(ReviewActorRole.ADMIN.canAddReviewNote()).isTrue();
        }
    }

    // ── 4. Allowed Transitions ──────────────────────────────────────

    @Nested
    @DisplayName("4. Allowed Transitions")
    class AllowedTransitions {

        @Test
        @DisplayName("DRAFT -> NEEDS_REVIEW via SUBMIT_FOR_REVIEW by CREATOR")
        void draftToNeedsReview() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.DRAFT, ReviewState.NEEDS_REVIEW,
                    ReviewActionType.SUBMIT_FOR_REVIEW, ReviewActorRole.CREATOR)).isTrue();
        }

        @Test
        @DisplayName("AI_GENERATED -> NEEDS_REVIEW via SUBMIT_FOR_REVIEW by CREATOR")
        void aiGeneratedToNeedsReview() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.AI_GENERATED, ReviewState.NEEDS_REVIEW,
                    ReviewActionType.SUBMIT_FOR_REVIEW, ReviewActorRole.CREATOR)).isTrue();
        }

        @Test
        @DisplayName("NEEDS_REVIEW -> IN_REVIEW via START_REVIEW by REVIEWER")
        void needsReviewToInReview() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.NEEDS_REVIEW, ReviewState.IN_REVIEW,
                    ReviewActionType.START_REVIEW, ReviewActorRole.REVIEWER)).isTrue();
        }

        @Test
        @DisplayName("IN_REVIEW -> CHANGES_REQUESTED via REQUEST_CHANGES by REVIEWER")
        void inReviewToChangesRequested() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.CHANGES_REQUESTED,
                    ReviewActionType.REQUEST_CHANGES, ReviewActorRole.REVIEWER)).isTrue();
        }

        @Test
        @DisplayName("IN_REVIEW -> APPROVED_FOR_INTERNAL_USE via APPROVE by REVIEWER")
        void inReviewToApproved() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER)).isTrue();
        }

        @Test
        @DisplayName("IN_REVIEW -> APPROVED_FOR_INTERNAL_USE via APPROVE by ADMIN")
        void inReviewToApprovedByAdmin() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.ADMIN)).isTrue();
        }

        @Test
        @DisplayName("IN_REVIEW -> REJECTED via REJECT by REVIEWER")
        void inReviewToRejected() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.REJECTED,
                    ReviewActionType.REJECT, ReviewActorRole.REVIEWER)).isTrue();
        }

        @Test
        @DisplayName("CHANGES_REQUESTED -> NEEDS_REVIEW via SUBMIT_FOR_REVIEW by CREATOR")
        void changesRequestedToNeedsReview() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.CHANGES_REQUESTED, ReviewState.NEEDS_REVIEW,
                    ReviewActionType.SUBMIT_FOR_REVIEW, ReviewActorRole.CREATOR)).isTrue();
        }

        @Test
        @DisplayName("CHANGES_REQUESTED -> DRAFT via RETURN_TO_DRAFT by CREATOR")
        void changesRequestedToDraft() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.CHANGES_REQUESTED, ReviewState.DRAFT,
                    ReviewActionType.RETURN_TO_DRAFT, ReviewActorRole.CREATOR)).isTrue();
        }

        @Test
        @DisplayName("ADD_REVIEW_NOTE keeps state in allowed states by REVIEWER")
        void addNoteKeepsState() {
            for (ReviewState s : ReviewState.values()) {
                if (s != ReviewState.AI_GENERATED) {
                    assertThat(ReviewTransitionPolicy.canTransition(
                            s, s, ReviewActionType.ADD_REVIEW_NOTE, ReviewActorRole.REVIEWER))
                            .as("ADD_REVIEW_NOTE in " + s).isTrue();
                }
            }
        }
    }

    // ── 5. Forbidden Transitions ────────────────────────────────────

    @Nested
    @DisplayName("5. Forbidden Transitions")
    class ForbiddenTransitions {

        @Test
        @DisplayName("DRAFT cannot directly approve")
        void draftCannotApprove() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.DRAFT, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER)).isFalse();
        }

        @Test
        @DisplayName("AI_GENERATED cannot directly approve")
        void aiGeneratedCannotApprove() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.AI_GENERATED, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER)).isFalse();
        }

        @Test
        @DisplayName("NEEDS_REVIEW cannot directly approve")
        void needsReviewCannotApprove() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.NEEDS_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER)).isFalse();
        }

        @Test
        @DisplayName("CHANGES_REQUESTED cannot directly approve")
        void changesRequestedCannotApprove() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.CHANGES_REQUESTED, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER)).isFalse();
        }

        @Test
        @DisplayName("CREATOR cannot approve from IN_REVIEW")
        void creatorCannotApprove() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.CREATOR)).isFalse();
        }

        @Test
        @DisplayName("SYSTEM cannot approve from IN_REVIEW")
        void systemCannotApprove() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.SYSTEM)).isFalse();
        }

        @Test
        @DisplayName("SYSTEM cannot reject")
        void systemCannotReject() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.REJECTED,
                    ReviewActionType.REJECT, ReviewActorRole.SYSTEM)).isFalse();
        }

        @Test
        @DisplayName("SYSTEM cannot start review")
        void systemCannotStartReview() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.NEEDS_REVIEW, ReviewState.IN_REVIEW,
                    ReviewActionType.START_REVIEW, ReviewActorRole.SYSTEM)).isFalse();
        }

        @Test
        @DisplayName("SYSTEM cannot add review note")
        void systemCannotAddNote() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.IN_REVIEW,
                    ReviewActionType.ADD_REVIEW_NOTE, ReviewActorRole.SYSTEM)).isFalse();
        }

        @Test
        @DisplayName("REJECTED has no state-changing outgoing transitions")
        void rejectedTerminal() {
            Set<ReviewState> targets = ReviewTransitionPolicy.allowedTargets(
                    ReviewState.REJECTED, ReviewActorRole.REVIEWER);
            assertThat(targets).isEmpty();
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE has no state-changing outgoing transitions")
        void approvedTerminal() {
            Set<ReviewState> targets = ReviewTransitionPolicy.allowedTargets(
                    ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER);
            assertThat(targets).isEmpty();
        }

        @Test
        @DisplayName("ADD_REVIEW_NOTE cannot change state")
        void addNoteCannotChangeState() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.ADD_REVIEW_NOTE, ReviewActorRole.REVIEWER)).isFalse();
        }

        @Test
        @DisplayName("Same-state transition is not allowed except ADD_REVIEW_NOTE")
        void sameStateNotAllowed() {
            for (ReviewState s : ReviewState.values()) {
                for (ReviewActionType a : ReviewActionType.values()) {
                    if (a == ReviewActionType.ADD_REVIEW_NOTE) continue;
                    assertThat(ReviewTransitionPolicy.canTransition(
                            s, s, a, ReviewActorRole.REVIEWER))
                            .as(s + " -> " + s + " via " + a).isFalse();
                }
            }
        }
    }

    // ── 6. Human Approval Guardrails ────────────────────────────────

    @Nested
    @DisplayName("6. Human Approval Guardrails")
    class HumanApprovalGuardrails {

        @Test
        @DisplayName("APPROVE_FOR_INTERNAL_USE only from IN_REVIEW")
        void approveOnlyFromInReview() {
            for (ReviewState s : ReviewState.values()) {
                boolean result = ReviewTransitionPolicy.canTransition(
                        s, ReviewState.APPROVED_FOR_INTERNAL_USE,
                        ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER);
                if (s == ReviewState.IN_REVIEW) {
                    assertThat(result).as("Approve from " + s).isTrue();
                } else {
                    assertThat(result).as("Approve from " + s).isFalse();
                }
            }
        }

        @Test
        @DisplayName("APPROVE requires REVIEWER or ADMIN")
        void approveRequiresReviewerOrAdmin() {
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER)).isTrue();
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.ADMIN)).isTrue();
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.CREATOR)).isFalse();
            assertThat(ReviewTransitionPolicy.canTransition(
                    ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.SYSTEM)).isFalse();
        }

        @Test
        @DisplayName("AI_GENERATED does not imply approval")
        void aiGeneratedNotApproval() {
            assertThat(ReviewState.AI_GENERATED.isApprovedForInternalUse()).isFalse();
            assertThat(ReviewState.AI_GENERATED.isAiGenerated()).isTrue();
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE is internal only — name does not imply publish")
        void approvedInternalOnly() {
            String name = ReviewState.APPROVED_FOR_INTERNAL_USE.name();
            assertThat(name).doesNotContain("PUBLISH");
            assertThat(name).doesNotContain("EXPORT");
            assertThat(name).doesNotContain("PUBLIC");
            assertThat(name).doesNotContain("CDN");
            assertThat(name).doesNotContain("SIGNED");
        }
    }

    // ── 7. Audit Model Safety ───────────────────────────────────────

    @Nested
    @DisplayName("7. Audit Model Safety")
    class AuditModelSafety {

        @Test
        @DisplayName("Expected audit events exist")
        void expectedEventsExist() {
            assertThat(ReviewAuditEventType.values()).containsExactlyInAnyOrder(
                    ReviewAuditEventType.REVIEW_SUBMITTED,
                    ReviewAuditEventType.REVIEW_STARTED,
                    ReviewAuditEventType.CHANGES_REQUESTED,
                    ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED,
                    ReviewAuditEventType.REVIEW_REJECTED,
                    ReviewAuditEventType.RETURNED_TO_DRAFT,
                    ReviewAuditEventType.REVIEW_NOTE_ADDED,
                    ReviewAuditEventType.REVIEW_TRANSITION_REJECTED
            );
        }

        @Test
        @DisplayName("Forbidden audit events do not exist")
        void forbiddenEventsAbsent() {
            Set<String> names = Arrays.stream(ReviewAuditEventType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain(
                    "PUBLISHED", "EXPORTED", "COMPLETED", "READY_FOR_PUBLISH",
                    "PUBLIC_URL_CREATED", "SIGNED_URL_CREATED", "AUTO_APPROVED"
            );
        }

        @Test
        @DisplayName("Audit record toString is safe")
        void auditRecordToStringSafe() {
            ReviewAuditRecord record = ReviewAuditRecord.builder()
                    .eventType(ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED)
                    .fromState(ReviewState.IN_REVIEW)
                    .toState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .actionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .actorReference("reviewer-001")
                    .occurredAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .safeReasonCode("CONTENT_QUALITY_OK")
                    .safeMessage("Content meets quality standards")
                    .build();
            String str = record.toString();
            assertThat(str).contains("INTERNAL_APPROVAL_GRANTED");
            assertThat(str).contains("IN_REVIEW");
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secretKey");
            assertThat(str).doesNotContain("accessKey");
            assertThat(str).doesNotContain("bucket");
            assertThat(str).doesNotContain("objectKey");
            assertThat(str).doesNotContain("base64");
        }

        @Test
        @DisplayName("Audit record has no JPA annotations")
        void auditRecordNoJpa() {
            assertThat(ReviewAuditRecord.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
            assertThat(ReviewAuditRecord.class.isAnnotationPresent(
                    jakarta.persistence.Table.class)).isFalse();
        }

        @Test
        @DisplayName("Audit record requires eventType")
        void auditRecordRequiresEventType() {
            assertThatThrownBy(() -> ReviewAuditRecord.builder().build())
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ── 8. Safe Failure Messages ────────────────────────────────────

    @Nested
    @DisplayName("8. Safe Failure Messages")
    class SafeFailureMessages {

        @Test
        @DisplayName("Invalid transition throws with safe message")
        void invalidTransitionSafeMessage() {
            assertThatThrownBy(() -> ReviewTransitionPolicy.validateTransition(
                    ReviewState.DRAFT, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER))
                    .isInstanceOf(ReviewTransitionViolationException.class)
                    .hasMessageContaining("not allowed")
                    .hasMessageNotContaining("https://")
                    .hasMessageNotContaining("secretKey")
                    .hasMessageNotContaining("accessKey")
                    .hasMessageNotContaining("bucket")
                    .hasMessageNotContaining("base64");
        }

        @Test
        @DisplayName("Null parameters throw with safe message")
        void nullParametersSafeMessage() {
            assertThatThrownBy(() -> ReviewTransitionPolicy.validateTransition(
                    null, ReviewState.IN_REVIEW,
                    ReviewActionType.START_REVIEW, ReviewActorRole.REVIEWER))
                    .isInstanceOf(ReviewTransitionViolationException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("Exception fields are accessible")
        void exceptionFields() {
            var ex = new ReviewTransitionViolationException(
                    ReviewState.DRAFT, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE);
            assertThat(ex.getFromState()).isEqualTo(ReviewState.DRAFT);
            assertThat(ex.getToState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(ex.getAction()).isEqualTo(ReviewActionType.APPROVE_FOR_INTERNAL_USE);
        }

        @ParameterizedTest
        @EnumSource(ReviewActionType.class)
        @DisplayName("All actions produce safe violation messages when invalid")
        void allActionsSafeViolation(ReviewActionType action) {
            // Try from REJECTED — all should fail for state-changing
            if (action == ReviewActionType.ADD_REVIEW_NOTE) return;
            try {
                ReviewTransitionPolicy.validateTransition(
                        ReviewState.REJECTED, ReviewState.DRAFT, action, ReviewActorRole.REVIEWER);
                fail("Should have thrown");
            } catch (ReviewTransitionViolationException e) {
                assertThat(e.getMessage()).doesNotContain("https://");
                assertThat(e.getMessage()).doesNotContain("secretKey");
                assertThat(e.getMessage()).doesNotContain("base64");
            }
        }
    }

    // ── 9. Review Text Safety ───────────────────────────────────────

    @Nested
    @DisplayName("9. Review Text Safety")
    class ReviewTextSafetyTests {

        @Test
        @DisplayName("Null input returns null")
        void nullInput() {
            assertThat(ReviewTextSafety.sanitize(null)).isNull();
            assertThat(ReviewTextSafety.sanitizeReasonCode(null)).isNull();
        }

        @Test
        @DisplayName("Whitespace trimmed")
        void whitespace() {
            assertThat(ReviewTextSafety.sanitize("  hello  ")).isEqualTo("hello");
        }

        @Test
        @DisplayName("Control characters removed")
        void controlChars() {
            assertThat(ReviewTextSafety.sanitize("hello\u0000world\u0007")).isEqualTo("helloworld");
        }

        @Test
        @DisplayName("Newline and tab preserved")
        void newlineTab() {
            assertThat(ReviewTextSafety.sanitize("hello\nworld\ttab")).isEqualTo("hello\nworld\ttab");
        }

        @Test
        @DisplayName("Max length enforced")
        void maxLength() {
            String longText = "x".repeat(3000);
            assertThat(ReviewTextSafety.sanitize(longText)).hasSize(ReviewTextSafety.MAX_NOTE_LENGTH);
        }

        @Test
        @DisplayName("Reason code sanitized to alphanumeric")
        void reasonCode() {
            assertThat(ReviewTextSafety.sanitizeReasonCode("QUALITY_OK")).isEqualTo("QUALITY_OK");
            assertThat(ReviewTextSafety.sanitizeReasonCode("bad code!@#")).isEqualTo("badcode");
        }

        @Test
        @DisplayName("Reason code max length enforced")
        void reasonCodeMaxLength() {
            String longCode = "A".repeat(200);
            assertThat(ReviewTextSafety.sanitizeReasonCode(longCode))
                    .hasSize(ReviewTextSafety.MAX_REASON_CODE_LENGTH);
        }

        @Test
        @DisplayName("requireNonBlank throws for blank")
        void requireNonBlankThrows() {
            assertThatThrownBy(() -> ReviewTextSafety.requireNonBlank("", "reason"))
                    .isInstanceOf(ReviewTransitionViolationException.class)
                    .hasMessageContaining("must not be blank");
        }

        @Test
        @DisplayName("requireNonBlank throws for null")
        void requireNonBlankNull() {
            assertThatThrownBy(() -> ReviewTextSafety.requireNonBlank(null, "reason"))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("requireNonBlank passes for valid text")
        void requireNonBlankPasses() {
            assertThatNoException().isThrownBy(
                    () -> ReviewTextSafety.requireNonBlank("valid", "reason"));
        }
    }

    // ── 10. Boundary Regression ─────────────────────────────────────

    @Nested
    @DisplayName("10. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("No controller annotation on policy")
        void noControllerOnPolicy() {
            assertThat(ReviewTransitionPolicy.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
            assertThat(ReviewTransitionPolicy.class.isAnnotationPresent(
                    org.springframework.stereotype.Controller.class)).isFalse();
        }

        @Test
        @DisplayName("No entity/table annotation on audit record")
        void noEntityOnAuditRecord() {
            assertThat(ReviewAuditRecord.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("No publish/export in any review enum name")
        void noPublishExportInEnums() {
            Set<String> allNames = new java.util.HashSet<>();
            Arrays.stream(ReviewState.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActionType.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActorRole.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewAuditEventType.values()).forEach(v -> allNames.add(v.name()));

            for (String name : allNames) {
                assertThat(name).doesNotContain("PUBLISH");
                assertThat(name).doesNotContain("EXPORT");
                assertThat(name).doesNotContain("CDN");
                assertThat(name).doesNotContain("SIGNED_URL");
                assertThat(name).doesNotContain("PUBLIC_URL");
            }
        }

        @Test
        @DisplayName("No video/audio in any review enum name")
        void noVideoAudioInEnums() {
            Set<String> allNames = new java.util.HashSet<>();
            Arrays.stream(ReviewState.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActionType.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewAuditEventType.values()).forEach(v -> allNames.add(v.name()));

            for (String name : allNames) {
                assertThat(name).doesNotContain("VIDEO");
                assertThat(name).doesNotContain("AUDIO");
            }
        }

        @Test
        @DisplayName("targetFor returns null for ADD_REVIEW_NOTE")
        void targetForAddNote() {
            assertThat(ReviewTransitionPolicy.targetFor(
                    ReviewState.IN_REVIEW, ReviewActionType.ADD_REVIEW_NOTE)).isNull();
        }

        @Test
        @DisplayName("allowedTargets returns empty for terminal REJECTED")
        void allowedTargetsRejected() {
            assertThat(ReviewTransitionPolicy.allowedTargets(
                    ReviewState.REJECTED, ReviewActorRole.ADMIN)).isEmpty();
        }

        @Test
        @DisplayName("allowedTargets throws for null state")
        void allowedTargetsNullState() {
            assertThatThrownBy(() -> ReviewTransitionPolicy.allowedTargets(null, ReviewActorRole.ADMIN))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("allowedTargets throws for null role")
        void allowedTargetsNullRole() {
            assertThatThrownBy(() -> ReviewTransitionPolicy.allowedTargets(ReviewState.DRAFT, null))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }
    }
}
