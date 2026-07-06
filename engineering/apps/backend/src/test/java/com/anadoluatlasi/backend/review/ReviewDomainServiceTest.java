package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-012 Task 03 — Review Domain Service, Safe Command Model & Audit Event Factory.
 */
@DisplayName("FP-012 Task 03 — Review Domain Service")
class ReviewDomainServiceTest {

    // ── 1. Command Model Safety ─────────────────────────────────────

    @Nested
    @DisplayName("1. Command Model Safety")
    class CommandModelSafety {

        @Test
        @DisplayName("Required fields validated — null currentState")
        void nullCurrentState() {
            assertThatThrownBy(() -> ReviewCommand.builder()
                    .actionType(ReviewActionType.SUBMIT_FOR_REVIEW)
                    .actorRole(ReviewActorRole.CREATOR)
                    .build())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Required fields validated — null actionType")
        void nullActionType() {
            assertThatThrownBy(() -> ReviewCommand.builder()
                    .currentState(ReviewState.DRAFT)
                    .actorRole(ReviewActorRole.CREATOR)
                    .build())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Required fields validated — null actorRole")
        void nullActorRole() {
            assertThatThrownBy(() -> ReviewCommand.builder()
                    .currentState(ReviewState.DRAFT)
                    .actionType(ReviewActionType.SUBMIT_FOR_REVIEW)
                    .build())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("toString does not leak note content")
        void toStringNoNoteContent() {
            ReviewCommand cmd = ReviewCommand.builder()
                    .currentState(ReviewState.IN_REVIEW)
                    .actionType(ReviewActionType.REJECT)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .note("This is a sensitive review note")
                    .reasonCode("QUALITY_ISSUE")
                    .build();
            String str = cmd.toString();
            assertThat(str).contains("hasNote=true");
            assertThat(str).doesNotContain("sensitive review note");
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secretKey");
            assertThat(str).doesNotContain("accessKey");
        }

        @Test
        @DisplayName("Note is sanitized via ReviewTextSafety")
        void noteSanitized() {
            ReviewCommand cmd = ReviewCommand.builder()
                    .currentState(ReviewState.IN_REVIEW)
                    .actionType(ReviewActionType.REQUEST_CHANGES)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .note("  fix this\u0000  ")
                    .build();
            assertThat(cmd.getNote()).isEqualTo("fix this");
        }

        @Test
        @DisplayName("ReasonCode is sanitized via ReviewTextSafety")
        void reasonCodeSanitized() {
            ReviewCommand cmd = ReviewCommand.builder()
                    .currentState(ReviewState.IN_REVIEW)
                    .actionType(ReviewActionType.REJECT)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .reasonCode("BAD CODE!@#$")
                    .build();
            assertThat(cmd.getReasonCode()).isEqualTo("BADCODE");
        }

        @Test
        @DisplayName("No forbidden fields in ReviewCommand")
        void noForbiddenFields() {
            Set<String> forbidden = Set.of(
                    "providerUrl", "storageKey", "bucketName", "bucketPath",
                    "rawPayload", "base64", "apiKey", "secret",
                    "publicUrl", "signedUrl");
            Set<String> fields = Arrays.stream(ReviewCommand.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : forbidden) {
                assertThat(fields).doesNotContain(f);
            }
        }
    }

    // ── 2. Domain Service Allowed Decisions ─────────────────────────

    @Nested
    @DisplayName("2. Domain Service Allowed Decisions")
    class AllowedDecisions {

        @Test
        @DisplayName("DRAFT -> NEEDS_REVIEW via SUBMIT_FOR_REVIEW by CREATOR")
        void draftSubmit() {
            var result = processSimple(ReviewState.DRAFT, ReviewActionType.SUBMIT_FOR_REVIEW, ReviewActorRole.CREATOR);
            assertThat(result.isChanged()).isTrue();
            assertThat(result.getNewState()).isEqualTo(ReviewState.NEEDS_REVIEW);
            assertThat(result.getAuditRecord().getEventType()).isEqualTo(ReviewAuditEventType.REVIEW_SUBMITTED);
        }

        @Test
        @DisplayName("AI_GENERATED -> NEEDS_REVIEW via SUBMIT_FOR_REVIEW by CREATOR")
        void aiGeneratedSubmit() {
            var result = processSimple(ReviewState.AI_GENERATED, ReviewActionType.SUBMIT_FOR_REVIEW, ReviewActorRole.CREATOR);
            assertThat(result.isChanged()).isTrue();
            assertThat(result.getNewState()).isEqualTo(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("NEEDS_REVIEW -> IN_REVIEW via START_REVIEW by REVIEWER")
        void startReview() {
            var result = processSimple(ReviewState.NEEDS_REVIEW, ReviewActionType.START_REVIEW, ReviewActorRole.REVIEWER);
            assertThat(result.isChanged()).isTrue();
            assertThat(result.getNewState()).isEqualTo(ReviewState.IN_REVIEW);
            assertThat(result.getAuditRecord().getEventType()).isEqualTo(ReviewAuditEventType.REVIEW_STARTED);
        }

        @Test
        @DisplayName("IN_REVIEW -> CHANGES_REQUESTED via REQUEST_CHANGES by REVIEWER")
        void requestChanges() {
            var result = processSimple(ReviewState.IN_REVIEW, ReviewActionType.REQUEST_CHANGES, ReviewActorRole.REVIEWER);
            assertThat(result.isChanged()).isTrue();
            assertThat(result.getNewState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
            assertThat(result.getAuditRecord().getEventType()).isEqualTo(ReviewAuditEventType.CHANGES_REQUESTED);
        }

        @Test
        @DisplayName("CHANGES_REQUESTED -> NEEDS_REVIEW via SUBMIT_FOR_REVIEW by CREATOR")
        void changesResubmit() {
            var result = processSimple(ReviewState.CHANGES_REQUESTED, ReviewActionType.SUBMIT_FOR_REVIEW, ReviewActorRole.CREATOR);
            assertThat(result.isChanged()).isTrue();
            assertThat(result.getNewState()).isEqualTo(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("CHANGES_REQUESTED -> DRAFT via RETURN_TO_DRAFT by CREATOR")
        void returnToDraft() {
            var result = processSimple(ReviewState.CHANGES_REQUESTED, ReviewActionType.RETURN_TO_DRAFT, ReviewActorRole.CREATOR);
            assertThat(result.isChanged()).isTrue();
            assertThat(result.getNewState()).isEqualTo(ReviewState.DRAFT);
            assertThat(result.getAuditRecord().getEventType()).isEqualTo(ReviewAuditEventType.RETURNED_TO_DRAFT);
        }

        @Test
        @DisplayName("IN_REVIEW -> APPROVED_FOR_INTERNAL_USE via APPROVE by REVIEWER")
        void approveByReviewer() {
            var result = processSimple(ReviewState.IN_REVIEW, ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER);
            assertThat(result.isChanged()).isTrue();
            assertThat(result.getNewState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(result.getAuditRecord().getEventType()).isEqualTo(ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED);
        }

        @Test
        @DisplayName("IN_REVIEW -> APPROVED_FOR_INTERNAL_USE via APPROVE by ADMIN")
        void approveByAdmin() {
            var result = processSimple(ReviewState.IN_REVIEW, ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.ADMIN);
            assertThat(result.isChanged()).isTrue();
            assertThat(result.getNewState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("IN_REVIEW -> REJECTED via REJECT by REVIEWER with reason")
        void reject() {
            var cmd = ReviewCommand.builder()
                    .currentState(ReviewState.IN_REVIEW)
                    .actionType(ReviewActionType.REJECT)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .note("Quality does not meet standards")
                    .reasonCode("QUALITY_ISSUE")
                    .build();
            var result = ReviewDomainService.processCommand(cmd);
            assertThat(result.isChanged()).isTrue();
            assertThat(result.getNewState()).isEqualTo(ReviewState.REJECTED);
            assertThat(result.getAuditRecord().getEventType()).isEqualTo(ReviewAuditEventType.REVIEW_REJECTED);
        }

        @Test
        @DisplayName("ADD_REVIEW_NOTE keeps state and returns changed=false")
        void addNote() {
            var cmd = ReviewCommand.builder()
                    .currentState(ReviewState.IN_REVIEW)
                    .actionType(ReviewActionType.ADD_REVIEW_NOTE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .note("Looks good so far")
                    .build();
            var result = ReviewDomainService.processCommand(cmd);
            assertThat(result.isChanged()).isFalse();
            assertThat(result.getNewState()).isEqualTo(ReviewState.IN_REVIEW);
            assertThat(result.getAuditRecord().getEventType()).isEqualTo(ReviewAuditEventType.REVIEW_NOTE_ADDED);
        }
    }

    // ── 3. Forbidden Decisions ──────────────────────────────────────

    @Nested
    @DisplayName("3. Forbidden Decisions")
    class ForbiddenDecisions {

        @Test
        @DisplayName("CREATOR cannot approve")
        void creatorCannotApprove() {
            assertThatThrownBy(() -> processSimple(
                    ReviewState.IN_REVIEW, ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.CREATOR))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("SYSTEM cannot approve")
        void systemCannotApprove() {
            assertThatThrownBy(() -> processSimple(
                    ReviewState.IN_REVIEW, ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.SYSTEM))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("DRAFT cannot directly approve")
        void draftCannotApprove() {
            assertThatThrownBy(() -> processWithTarget(
                    ReviewState.DRAFT, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("AI_GENERATED cannot directly approve")
        void aiGeneratedCannotApprove() {
            assertThatThrownBy(() -> processWithTarget(
                    ReviewState.AI_GENERATED, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("NEEDS_REVIEW cannot directly approve")
        void needsReviewCannotApprove() {
            assertThatThrownBy(() -> processWithTarget(
                    ReviewState.NEEDS_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("CHANGES_REQUESTED cannot directly approve")
        void changesRequestedCannotApprove() {
            assertThatThrownBy(() -> processWithTarget(
                    ReviewState.CHANGES_REQUESTED, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("REJECTED cannot state-change further")
        void rejectedTerminal() {
            for (ReviewActionType a : ReviewActionType.values()) {
                if (a == ReviewActionType.ADD_REVIEW_NOTE) continue;
                assertThatThrownBy(() -> processSimple(ReviewState.REJECTED, a, ReviewActorRole.REVIEWER))
                        .as("REJECTED + " + a)
                        .isInstanceOf(ReviewTransitionViolationException.class);
            }
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE cannot state-change further except REQUEST_INTERNAL_REVISION")
        void approvedTerminal() {
            for (ReviewActionType a : ReviewActionType.values()) {
                if (a == ReviewActionType.ADD_REVIEW_NOTE) continue;
                // FP-015 Task 02: REQUEST_INTERNAL_REVISION is the only allowed state change
                if (a == ReviewActionType.REQUEST_INTERNAL_REVISION) continue;
                assertThatThrownBy(() -> processSimple(ReviewState.APPROVED_FOR_INTERNAL_USE, a, ReviewActorRole.REVIEWER))
                        .as("APPROVED + " + a)
                        .isInstanceOf(ReviewTransitionViolationException.class);
            }
        }

        @Test
        @DisplayName("Null command fails safely")
        void nullCommand() {
            assertThatThrownBy(() -> ReviewDomainService.processCommand(null))
                    .isInstanceOf(ReviewTransitionViolationException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("REJECT without reason fails")
        void rejectWithoutReason() {
            var cmd = ReviewCommand.builder()
                    .currentState(ReviewState.IN_REVIEW)
                    .actionType(ReviewActionType.REJECT)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .build();
            assertThatThrownBy(() -> ReviewDomainService.processCommand(cmd))
                    .isInstanceOf(ReviewTransitionViolationException.class)
                    .hasMessageContaining("must not be blank");
        }

        @Test
        @DisplayName("ADD_REVIEW_NOTE without note text fails")
        void addNoteWithoutText() {
            var cmd = ReviewCommand.builder()
                    .currentState(ReviewState.IN_REVIEW)
                    .actionType(ReviewActionType.ADD_REVIEW_NOTE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .build();
            assertThatThrownBy(() -> ReviewDomainService.processCommand(cmd))
                    .isInstanceOf(ReviewTransitionViolationException.class)
                    .hasMessageContaining("must not be blank");
        }
    }

    // ── 4. Audit Event Factory ──────────────────────────────────────

    @Nested
    @DisplayName("4. Audit Event Factory")
    class AuditEventFactoryTests {

        @Test
        @DisplayName("All action types map to correct audit events")
        void allMappings() {
            assertThat(ReviewAuditEventFactory.mapActionToEventType(ReviewActionType.SUBMIT_FOR_REVIEW))
                    .isEqualTo(ReviewAuditEventType.REVIEW_SUBMITTED);
            assertThat(ReviewAuditEventFactory.mapActionToEventType(ReviewActionType.START_REVIEW))
                    .isEqualTo(ReviewAuditEventType.REVIEW_STARTED);
            assertThat(ReviewAuditEventFactory.mapActionToEventType(ReviewActionType.REQUEST_CHANGES))
                    .isEqualTo(ReviewAuditEventType.CHANGES_REQUESTED);
            assertThat(ReviewAuditEventFactory.mapActionToEventType(ReviewActionType.APPROVE_FOR_INTERNAL_USE))
                    .isEqualTo(ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED);
            assertThat(ReviewAuditEventFactory.mapActionToEventType(ReviewActionType.REJECT))
                    .isEqualTo(ReviewAuditEventType.REVIEW_REJECTED);
            assertThat(ReviewAuditEventFactory.mapActionToEventType(ReviewActionType.RETURN_TO_DRAFT))
                    .isEqualTo(ReviewAuditEventType.RETURNED_TO_DRAFT);
            assertThat(ReviewAuditEventFactory.mapActionToEventType(ReviewActionType.ADD_REVIEW_NOTE))
                    .isEqualTo(ReviewAuditEventType.REVIEW_NOTE_ADDED);
        }

        @Test
        @DisplayName("Success record has correct fields")
        void successRecord() {
            var cmd = ReviewCommand.builder()
                    .currentState(ReviewState.IN_REVIEW)
                    .actionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .actorReference("reviewer-42")
                    .reasonCode("QUALITY_OK")
                    .build();
            var record = ReviewAuditEventFactory.createSuccessRecord(cmd, ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(record.getEventType()).isEqualTo(ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED);
            assertThat(record.getFromState()).isEqualTo(ReviewState.IN_REVIEW);
            assertThat(record.getToState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(record.getActorReference()).isEqualTo("reviewer-42");
            assertThat(record.getOccurredAt()).isNotNull();
        }

        @Test
        @DisplayName("Rejection record has correct event type")
        void rejectionRecord() {
            var cmd = ReviewCommand.builder()
                    .currentState(ReviewState.DRAFT)
                    .actionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE)
                    .actorRole(ReviewActorRole.CREATOR)
                    .build();
            var record = ReviewAuditEventFactory.createRejectionRecord(cmd, "Not allowed");
            assertThat(record.getEventType()).isEqualTo(ReviewAuditEventType.REVIEW_TRANSITION_REJECTED);
            assertThat(record.getSafeMessage()).isEqualTo("Not allowed");
        }

        @Test
        @DisplayName("Note record has correct event type and same state")
        void noteRecord() {
            var cmd = ReviewCommand.builder()
                    .currentState(ReviewState.IN_REVIEW)
                    .actionType(ReviewActionType.ADD_REVIEW_NOTE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .note("Good work")
                    .build();
            var record = ReviewAuditEventFactory.createNoteRecord(cmd);
            assertThat(record.getEventType()).isEqualTo(ReviewAuditEventType.REVIEW_NOTE_ADDED);
            assertThat(record.getFromState()).isEqualTo(ReviewState.IN_REVIEW);
            assertThat(record.getToState()).isEqualTo(ReviewState.IN_REVIEW);
        }
    }

    // ── 5. Result Model Safety ──────────────────────────────────────

    @Nested
    @DisplayName("5. Result Model Safety")
    class ResultModelSafety {

        @Test
        @DisplayName("Result toString is safe")
        void resultToStringSafe() {
            var result = processSimple(ReviewState.DRAFT, ReviewActionType.SUBMIT_FOR_REVIEW, ReviewActorRole.CREATOR);
            String str = result.toString();
            assertThat(str).contains("DRAFT");
            assertThat(str).contains("NEEDS_REVIEW");
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secretKey");
            assertThat(str).doesNotContain("accessKey");
            assertThat(str).doesNotContain("bucket");
            assertThat(str).doesNotContain("base64");
        }

        @Test
        @DisplayName("No forbidden fields in ReviewDecisionResult")
        void noForbiddenFields() {
            Set<String> forbidden = Set.of(
                    "providerUrl", "storageKey", "bucketName", "bucketPath",
                    "rawPayload", "base64", "apiKey", "secret",
                    "publicUrl", "signedUrl");
            Set<String> fields = Arrays.stream(ReviewDecisionResult.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : forbidden) {
                assertThat(fields).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("Audit record embedded in result is safe")
        void auditInResultSafe() {
            var result = processSimple(ReviewState.IN_REVIEW, ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER);
            var record = result.getAuditRecord();
            assertThat(record).isNotNull();
            assertThat(record.getEventType()).isEqualTo(ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED);
            String str = record.toString();
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secretKey");
        }

        @Test
        @DisplayName("No JPA annotations on result")
        void noJpaOnResult() {
            assertThat(ReviewDecisionResult.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("No publish/export semantics in result state names")
        void noPublishExportInResult() {
            var result = processSimple(ReviewState.IN_REVIEW, ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER);
            assertThat(result.getNewState().name()).doesNotContain("PUBLISH");
            assertThat(result.getNewState().name()).doesNotContain("EXPORT");
            assertThat(result.getNewState().name()).doesNotContain("CDN");
        }
    }

    // ── 6. Safe Failure Messages ────────────────────────────────────

    @Nested
    @DisplayName("6. Safe Failure Messages")
    class SafeFailureMessages {

        @Test
        @DisplayName("Forbidden transition message is safe")
        void forbiddenTransitionSafe() {
            assertThatThrownBy(() -> processSimple(
                    ReviewState.DRAFT, ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER))
                    .isInstanceOf(ReviewTransitionViolationException.class)
                    .satisfies(ex -> {
                        String msg = ex.getMessage();
                        assertThat(msg).doesNotContain("https://");
                        assertThat(msg).doesNotContain("secretKey");
                        assertThat(msg).doesNotContain("accessKey");
                        assertThat(msg).doesNotContain("bucket");
                        assertThat(msg).doesNotContain("base64");
                        assertThat(msg).doesNotContain("objectKey");
                        assertThat(msg).doesNotContain("rawPayload");
                    });
        }

        @ParameterizedTest
        @EnumSource(ReviewActorRole.class)
        @DisplayName("Actor role violation messages are safe")
        void actorViolationSafe(ReviewActorRole role) {
            if (role.canPerformReview()) return; // Would succeed
            try {
                processSimple(ReviewState.IN_REVIEW, ReviewActionType.APPROVE_FOR_INTERNAL_USE, role);
                fail("Should have thrown");
            } catch (ReviewTransitionViolationException e) {
                assertThat(e.getMessage()).doesNotContain("https://");
                assertThat(e.getMessage()).doesNotContain("secretKey");
            }
        }
    }

    // ── 7. Boundary Regression ──────────────────────────────────────

    @Nested
    @DisplayName("7. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("No controller annotation on domain service")
        void noController() {
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.stereotype.Controller.class)).isFalse();
        }

        @Test
        @DisplayName("No repository annotation on domain service")
        void noRepository() {
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.stereotype.Repository.class)).isFalse();
        }

        @Test
        @DisplayName("No JPA on command/result/factory")
        void noJpaOnModels() {
            assertThat(ReviewCommand.class.isAnnotationPresent(jakarta.persistence.Entity.class)).isFalse();
            assertThat(ReviewDecisionResult.class.isAnnotationPresent(jakarta.persistence.Entity.class)).isFalse();
            assertThat(ReviewAuditEventFactory.class.isAnnotationPresent(jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("No publish/export in any review package class")
        void noPublishExportInPackage() {
            Set<String> allNames = new java.util.HashSet<>();
            Arrays.stream(ReviewState.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActionType.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewAuditEventType.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActorRole.values()).forEach(v -> allNames.add(v.name()));

            for (String name : allNames) {
                assertThat(name).doesNotContain("PUBLISH");
                assertThat(name).doesNotContain("EXPORT");
                assertThat(name).doesNotContain("COMPLETED");
                assertThat(name).doesNotContain("AUTO_APPROV");
            }
        }

        @Test
        @DisplayName("Domain service has no Spring annotations")
        void noSpringAnnotations() {
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isFalse();
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.stereotype.Component.class)).isFalse();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private static ReviewDecisionResult processSimple(ReviewState from, ReviewActionType action, ReviewActorRole role) {
        return ReviewDomainService.processCommand(
                ReviewCommand.builder()
                        .currentState(from)
                        .actionType(action)
                        .actorRole(role)
                        .actorReference("test-actor")
                        .note(action == ReviewActionType.REJECT || action == ReviewActionType.ADD_REVIEW_NOTE
                                ? "Test note" : null)
                        .build());
    }

    private static ReviewDecisionResult processWithTarget(ReviewState from, ReviewState to,
                                                           ReviewActionType action, ReviewActorRole role) {
        return ReviewDomainService.processCommand(
                ReviewCommand.builder()
                        .currentState(from)
                        .requestedState(to)
                        .actionType(action)
                        .actorRole(role)
                        .actorReference("test-actor")
                        .build());
    }
}
