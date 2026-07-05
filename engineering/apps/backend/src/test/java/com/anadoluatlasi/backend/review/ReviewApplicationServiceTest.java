package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-012 Task 05 — Review Application Service, Transaction Boundary & Persistence Integration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FP-012 Task 05 — Review Application Service")
class ReviewApplicationServiceTest {

    @Mock
    private ReviewRecordRepository reviewRecordRepository;

    @Mock
    private ReviewAuditEventRepository auditEventRepository;

    @InjectMocks
    private ReviewApplicationService service;

    private static final ReviewTargetType TARGET_TYPE = ReviewTargetType.CONTENT_PROJECT;
    private static final UUID TARGET_ID = UUID.randomUUID();
    private static final String ACTOR_REF = "test-actor-01";

    // ── 1. Happy Paths ──────────────────────────────────────────────

    @Nested
    @DisplayName("1. Application Service Happy Paths")
    class HappyPaths {

        @Test
        @DisplayName("submitForReview creates new record and persists audit event")
        void submitForReview() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.submitForReview(TARGET_TYPE, TARGET_ID, ACTOR_REF, ReviewActorRole.CREATOR, null);

            assertThat(result.isChanged()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
            assertThat(result.getAuditEventType()).isEqualTo(ReviewAuditEventType.REVIEW_SUBMITTED);
            verify(reviewRecordRepository).save(any(ReviewRecordEntity.class));
            verify(auditEventRepository).save(any(ReviewAuditEventEntity.class));
        }

        @Test
        @DisplayName("submitForReview on existing DRAFT record transitions to NEEDS_REVIEW")
        void submitExistingDraft() {
            ReviewRecordEntity existing = buildRecord(ReviewState.DRAFT);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.submitForReview(TARGET_TYPE, TARGET_ID, ACTOR_REF, ReviewActorRole.CREATOR, null);

            assertThat(result.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
            assertThat(result.getPreviousState()).isEqualTo(ReviewState.DRAFT);
        }

        @Test
        @DisplayName("startReview updates state to IN_REVIEW")
        void startReview() {
            ReviewRecordEntity existing = buildRecord(ReviewState.NEEDS_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.startReview(TARGET_TYPE, TARGET_ID, ACTOR_REF, ReviewActorRole.REVIEWER, null);

            assertThat(result.isChanged()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.IN_REVIEW);
            assertThat(result.getAuditEventType()).isEqualTo(ReviewAuditEventType.REVIEW_STARTED);
        }

        @Test
        @DisplayName("requestChanges updates state and persists audit event")
        void requestChanges() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.requestChanges(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "QUALITY", "Needs improvement");

            assertThat(result.isChanged()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
            assertThat(result.getAuditEventType()).isEqualTo(ReviewAuditEventType.CHANGES_REQUESTED);
            verify(auditEventRepository).save(any(ReviewAuditEventEntity.class));
        }

        @Test
        @DisplayName("returnToDraft updates state")
        void returnToDraft() {
            ReviewRecordEntity existing = buildRecord(ReviewState.CHANGES_REQUESTED);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.returnToDraft(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.CREATOR, null, null);

            assertThat(result.isChanged()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.DRAFT);
            assertThat(result.getAuditEventType()).isEqualTo(ReviewAuditEventType.RETURNED_TO_DRAFT);
        }

        @Test
        @DisplayName("approveForInternalUse updates state by REVIEWER")
        void approveByReviewer() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "QUALITY_OK", null);

            assertThat(result.isChanged()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(result.getAuditEventType()).isEqualTo(ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED);
        }

        @Test
        @DisplayName("reject updates state with reason")
        void reject() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.reject(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "QUALITY_ISSUE", "Does not meet standards");

            assertThat(result.isChanged()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.REJECTED);
            assertThat(result.getAuditEventType()).isEqualTo(ReviewAuditEventType.REVIEW_REJECTED);
        }

        @Test
        @DisplayName("addReviewNote does not change state but persists audit")
        void addReviewNote() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.addReviewNote(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "Looking good so far");

            assertThat(result.isChanged()).isFalse();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.IN_REVIEW);
            assertThat(result.getAuditEventType()).isEqualTo(ReviewAuditEventType.REVIEW_NOTE_ADDED);
            verify(auditEventRepository).save(any(ReviewAuditEventEntity.class));
        }
    }

    // ── 2. Record Creation / Missing Record ─────────────────────────

    @Nested
    @DisplayName("2. Record Creation / Missing Record Behavior")
    class RecordCreation {

        @Test
        @DisplayName("submitForReview creates new record when none exists")
        void createsNewRecord() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<ReviewRecordEntity> captor = ArgumentCaptor.forClass(ReviewRecordEntity.class);

            service.submitForReview(TARGET_TYPE, TARGET_ID, ACTOR_REF, ReviewActorRole.CREATOR, null);

            verify(reviewRecordRepository).save(captor.capture());
            ReviewRecordEntity saved = captor.getValue();
            assertThat(saved.getReviewTargetType()).isEqualTo(TARGET_TYPE);
            assertThat(saved.getReviewTargetId()).isEqualTo(TARGET_ID);
            assertThat(saved.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("startReview fails when no record exists")
        void startReviewMissing() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.startReview(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, null))
                    .isInstanceOf(ReviewApplicationException.class)
                    .hasMessageContaining("not found");
            verify(reviewRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("approveForInternalUse fails when no record exists")
        void approveMissing() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "OK", null))
                    .isInstanceOf(ReviewApplicationException.class);
            verify(reviewRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("reject fails when no record exists")
        void rejectMissing() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reject(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "BAD", "Reject"))
                    .isInstanceOf(ReviewApplicationException.class);
        }

        @Test
        @DisplayName("Missing record error message does not leak internals")
        void missingRecordSafeMessage() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.startReview(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, null))
                    .satisfies(ex -> {
                        String msg = ex.getMessage();
                        assertThat(msg).doesNotContain("https://");
                        assertThat(msg).doesNotContain("secretKey");
                        assertThat(msg).doesNotContain("accessKey");
                        assertThat(msg).doesNotContain("bucket");
                        assertThat(msg).doesNotContain("base64");
                    });
        }
    }

    // ── 3. Transaction/Persistence Behavior ─────────────────────────

    @Nested
    @DisplayName("3. Transaction/Persistence Behavior")
    class TransactionPersistence {

        @Test
        @DisplayName("Both record and audit event are saved for successful action")
        void bothSaved() {
            ReviewRecordEntity existing = buildRecord(ReviewState.NEEDS_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.startReview(TARGET_TYPE, TARGET_ID, ACTOR_REF, ReviewActorRole.REVIEWER, null);

            verify(reviewRecordRepository, times(1)).save(any(ReviewRecordEntity.class));
            verify(auditEventRepository, times(1)).save(any(ReviewAuditEventEntity.class));
        }

        @Test
        @DisplayName("Invalid transition does not persist state change")
        void invalidTransitionNoPersistence() {
            ReviewRecordEntity existing = buildRecord(ReviewState.DRAFT);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "OK", null))
                    .isInstanceOf(ReviewTransitionViolationException.class);

            verify(reviewRecordRepository, never()).save(any());
            verify(auditEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Audit event entity captures sanitized note")
        void auditCapturesNote() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<ReviewAuditEventEntity> captor = ArgumentCaptor.forClass(ReviewAuditEventEntity.class);
            when(auditEventRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.requestChanges(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "QUALITY", "Fix spacing issues");

            ReviewAuditEventEntity audit = captor.getValue();
            assertThat(audit.getSanitizedNote()).isEqualTo("Fix spacing issues");
            assertThat(audit.getEventType()).isEqualTo(ReviewAuditEventType.CHANGES_REQUESTED);
        }

        @Test
        @DisplayName("Record state is updated after successful action")
        void recordStateUpdated() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));

            ArgumentCaptor<ReviewRecordEntity> captor = ArgumentCaptor.forClass(ReviewRecordEntity.class);
            when(reviewRecordRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.ADMIN, "OK", null);

            ReviewRecordEntity saved = captor.getValue();
            assertThat(saved.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(saved.getLastActionType()).isEqualTo(ReviewActionType.APPROVE_FOR_INTERNAL_USE);
        }
    }

    // ── 4. Actor Guardrails ─────────────────────────────────────────

    @Nested
    @DisplayName("4. Actor Guardrails")
    class ActorGuardrails {

        @Test
        @DisplayName("CREATOR cannot approve")
        void creatorCannotApprove() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.CREATOR, "OK", null))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("SYSTEM cannot approve")
        void systemCannotApprove() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.SYSTEM, "OK", null))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("REVIEWER can approve from IN_REVIEW")
        void reviewerCanApprove() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "OK", null);

            assertThat(result.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("ADMIN can approve from IN_REVIEW")
        void adminCanApprove() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.ADMIN, "OK", null);

            assertThat(result.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE is internal only — name check")
        void approvedInternalOnly() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "OK", null);

            assertThat(result.getCurrentState().name()).doesNotContain("PUBLISH");
            assertThat(result.getCurrentState().name()).doesNotContain("EXPORT");
        }
    }

    // ── 5. Result Safety ────────────────────────────────────────────

    @Nested
    @DisplayName("5. Result Safety")
    class ResultSafety {

        @Test
        @DisplayName("ReviewApplicationResult has no forbidden fields")
        void noForbiddenFields() {
            Set<String> forbidden = Set.of(
                    "providerUrl", "storageKey", "bucketName", "bucketPath",
                    "rawPayload", "base64", "apiKey", "secret",
                    "publicUrl", "signedUrl");
            Set<String> fields = Arrays.stream(ReviewApplicationResult.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : forbidden) {
                assertThat(fields).as("Forbidden: " + f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("Result toString is safe")
        void toStringSafe() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "OK", null);

            String str = result.toString();
            assertThat(str).contains("APPROVED_FOR_INTERNAL_USE");
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secretKey");
            assertThat(str).doesNotContain("accessKey");
            assertThat(str).doesNotContain("base64");
        }

        @Test
        @DisplayName("No publish/export semantics in result")
        void noPublishExport() {
            ReviewRecordEntity existing = buildRecord(ReviewState.NEEDS_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.startReview(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, null);

            assertThat(result.getCurrentState().name()).doesNotContain("PUBLISH");
            assertThat(result.getCurrentState().name()).doesNotContain("EXPORT");
            assertThat(result.getAuditEventType().name()).doesNotContain("PUBLISH");
        }
    }

    // ── 6. Audit Persistence Safety ─────────────────────────────────

    @Nested
    @DisplayName("6. Audit Persistence Safety")
    class AuditPersistenceSafety {

        @Test
        @DisplayName("Audit event entity from service is safe")
        void auditEntitySafe() {
            ReviewRecordEntity existing = buildRecord(ReviewState.NEEDS_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<ReviewAuditEventEntity> captor = ArgumentCaptor.forClass(ReviewAuditEventEntity.class);
            when(auditEventRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.startReview(TARGET_TYPE, TARGET_ID, ACTOR_REF, ReviewActorRole.REVIEWER, null);

            ReviewAuditEventEntity audit = captor.getValue();
            assertThat(audit.getEventType()).isEqualTo(ReviewAuditEventType.REVIEW_STARTED);
            assertThat(audit.getFromState()).isEqualTo(ReviewState.NEEDS_REVIEW);
            assertThat(audit.getToState()).isEqualTo(ReviewState.IN_REVIEW);
            String str = audit.toString();
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secretKey");
        }

        @Test
        @DisplayName("Forbidden audit event types do not exist")
        void forbiddenAuditEvents() {
            Set<String> names = Arrays.stream(ReviewAuditEventType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED");
            assertThat(names).doesNotContain("EXPORTED");
            assertThat(names).doesNotContain("COMPLETED");
            assertThat(names).doesNotContain("READY_FOR_PUBLISH");
            assertThat(names).doesNotContain("PUBLIC_URL_CREATED");
            assertThat(names).doesNotContain("SIGNED_URL_CREATED");
            assertThat(names).doesNotContain("AUTO_APPROVED");
        }

        @Test
        @DisplayName("getAuditTimeline returns empty for missing record")
        void auditTimelineMissing() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());

            List<ReviewAuditEventEntity> timeline = service.getAuditTimeline(TARGET_TYPE, TARGET_ID);
            assertThat(timeline).isEmpty();
        }

        @Test
        @DisplayName("getReviewState returns empty for missing record")
        void getStateMissing() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());

            Optional<ReviewState> state = service.getReviewState(TARGET_TYPE, TARGET_ID);
            assertThat(state).isEmpty();
        }

        @Test
        @DisplayName("getReviewState returns current state")
        void getStatePresent() {
            ReviewRecordEntity existing = buildRecord(ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));

            Optional<ReviewState> state = service.getReviewState(TARGET_TYPE, TARGET_ID);
            assertThat(state).contains(ReviewState.IN_REVIEW);
        }
    }

    // ── 7. Safe Failure Messages ────────────────────────────────────

    @Nested
    @DisplayName("7. Safe Failure Messages")
    class SafeFailureMessages {

        @Test
        @DisplayName("Transition violation message is safe")
        void transitionViolationSafe() {
            ReviewRecordEntity existing = buildRecord(ReviewState.DRAFT);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.approveForInternalUse(TARGET_TYPE, TARGET_ID, ACTOR_REF,
                    ReviewActorRole.REVIEWER, "OK", null))
                    .satisfies(ex -> {
                        String msg = ex.getMessage();
                        assertThat(msg).doesNotContain("https://");
                        assertThat(msg).doesNotContain("secretKey");
                        assertThat(msg).doesNotContain("accessKey");
                        assertThat(msg).doesNotContain("bucket");
                        assertThat(msg).doesNotContain("base64");
                        assertThat(msg).doesNotContain("rawPayload");
                    });
        }

        @Test
        @DisplayName("Application exception message is safe")
        void applicationExceptionSafe() {
            ReviewApplicationException ex = new ReviewApplicationException(
                    "Record not found", TARGET_TYPE, TARGET_ID);
            assertThat(ex.getMessage()).doesNotContain("https://");
            assertThat(ex.getMessage()).doesNotContain("secretKey");
            assertThat(ex.getTargetType()).isEqualTo(TARGET_TYPE);
        }
    }

    // ── 8. Boundary Regression ──────────────────────────────────────

    @Nested
    @DisplayName("8. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("Service is annotated @Service but NOT @RestController")
        void serviceNotController() {
            assertThat(ReviewApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isTrue();
            assertThat(ReviewApplicationService.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
            assertThat(ReviewApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Controller.class)).isFalse();
        }

        @Test
        @DisplayName("Service has no @RequestMapping")
        void noRequestMapping() {
            assertThat(ReviewApplicationService.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RequestMapping.class)).isFalse();
        }

        @Test
        @DisplayName("No publish/export in any review enum")
        void noPublishExportEnums() {
            Set<String> allNames = new java.util.HashSet<>();
            Arrays.stream(ReviewState.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActionType.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewAuditEventType.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActorRole.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewTargetType.values()).forEach(v -> allNames.add(v.name()));

            for (String name : allNames) {
                assertThat(name).doesNotContain("PUBLISH");
                assertThat(name).doesNotContain("EXPORT");
                assertThat(name).doesNotContain("CDN");
                assertThat(name).doesNotContain("COMPLETED");
            }
        }

        @Test
        @DisplayName("No forbidden fields on ReviewApplicationException")
        void exceptionNoForbiddenFields() {
            Set<String> forbidden = Set.of(
                    "providerUrl", "storageKey", "bucketName", "bucketPath",
                    "rawPayload", "base64", "apiKey", "secret",
                    "publicUrl", "signedUrl");
            Set<String> fields = Arrays.stream(ReviewApplicationException.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : forbidden) {
                assertThat(fields).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("ReviewApplicationResult has no JPA annotation")
        void resultNoJpa() {
            assertThat(ReviewApplicationResult.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("ReviewApplicationException has no JPA annotation")
        void exceptionNoJpa() {
            assertThat(ReviewApplicationException.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private ReviewRecordEntity buildRecord(ReviewState state) {
        return ReviewRecordEntity.builder()
                .id(UUID.randomUUID())
                .reviewTargetType(TARGET_TYPE)
                .reviewTargetId(TARGET_ID)
                .currentState(state)
                .createdByActorReference(ACTOR_REF)
                .build();
    }
}
