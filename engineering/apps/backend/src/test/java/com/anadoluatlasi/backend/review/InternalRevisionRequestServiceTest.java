package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * FP-015 Task 02: Comprehensive tests for InternalRevisionRequestService,
 * InternalRevisionRequest, InternalRevisionRequestResult, and
 * InternalRevisionRequestException.
 *
 * <p>Covers: happy path, idempotent note append, forbidden states, actor validation,
 * safety regression, no write bypass, and target type coverage.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FP-015 Task 02 — Internal Revision Request Service Foundation")
class InternalRevisionRequestServiceTest {

    private static final UUID TARGET_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String ACTOR_REF = "admin-user-1";
    private static final String SAFE_REASON = "CONTENT_OUTDATED";
    private static final String SAFE_MSG = "İçerik güncellenmeli";

    @Mock
    private ReviewApplicationService reviewApplicationService;

    private InternalRevisionRequestService service;

    @BeforeEach
    void setUp() {
        service = new InternalRevisionRequestService(reviewApplicationService);
    }

    // ════════════════════════════════════════════════════════════════
    // 1. HAPPY PATH — APPROVED_FOR_INTERNAL_USE → CHANGES_REQUESTED
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Happy Path — APPROVED_FOR_INTERNAL_USE → CHANGES_REQUESTED")
    class HappyPath {

        @Test
        @DisplayName("Approved item transitions to CHANGES_REQUESTED")
        void approvedTransitionsToChangesRequested() {
            mockCurrentState(ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockRevisionResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED);

            InternalRevisionRequestResult result = service.requestRevision(
                    buildRequest(ReviewTargetType.SCENARIO, ReviewActorRole.REVIEWER));

            assertThat(result.getPreviousState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
            assertThat(result.getActionApplied()).isEqualTo("REQUEST_INTERNAL_REVISION");
            assertThat(result.isAuditRecorded()).isTrue();
            assertThat(result.isAppendedNote()).isFalse();
        }

        @Test
        @DisplayName("Audit is recorded via ReviewApplicationService")
        void auditRecordedViaApplicationService() {
            mockCurrentState(ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockRevisionResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED);

            service.requestRevision(buildRequest(ReviewTargetType.STORYBOARD, ReviewActorRole.ADMIN));

            verify(reviewApplicationService).requestInternalRevision(
                    eq(ReviewTargetType.STORYBOARD), eq(TARGET_ID),
                    eq(ACTOR_REF), eq(ReviewActorRole.ADMIN),
                    any(), any());
        }

        @Test
        @DisplayName("Safe reason code is included in result")
        void safeReasonCodeInResult() {
            mockCurrentState(ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockRevisionResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED);

            InternalRevisionRequestResult result = service.requestRevision(
                    buildRequest(ReviewTargetType.CONTENT_PROJECT, ReviewActorRole.REVIEWER));

            assertThat(result.getSafeReasonCode()).isEqualTo(SAFE_REASON);
        }

        @Test
        @DisplayName("UpdatedAt is set in result")
        void updatedAtIsSet() {
            mockCurrentState(ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockRevisionResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED);

            InternalRevisionRequestResult result = service.requestRevision(
                    buildRequest(ReviewTargetType.PACKAGE, ReviewActorRole.ADMIN));

            assertThat(result.getUpdatedAt()).isNotNull();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. IDEMPOTENT — CHANGES_REQUESTED → Note Append
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. Idempotent — CHANGES_REQUESTED → Note Append")
    class IdempotentNoteAppend {

        @Test
        @DisplayName("CHANGES_REQUESTED appends safe note without state change")
        void changesRequestedAppendsNote() {
            mockCurrentState(ReviewState.CHANGES_REQUESTED);
            mockNoteResult(ReviewState.CHANGES_REQUESTED);

            InternalRevisionRequestResult result = service.requestRevision(
                    buildRequest(ReviewTargetType.SCENARIO, ReviewActorRole.REVIEWER));

            assertThat(result.getPreviousState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
            assertThat(result.getActionApplied()).isEqualTo("NOTE_APPENDED");
            assertThat(result.isAppendedNote()).isTrue();
            assertThat(result.isAuditRecorded()).isTrue();
        }

        @Test
        @DisplayName("Duplicate revision does not create duplicate transition")
        void noDuplicateTransition() {
            mockCurrentState(ReviewState.CHANGES_REQUESTED);
            mockNoteResult(ReviewState.CHANGES_REQUESTED);

            service.requestRevision(buildRequest(ReviewTargetType.SCENARIO, ReviewActorRole.REVIEWER));

            verify(reviewApplicationService, never()).requestInternalRevision(any(), any(), any(), any(), any(), any());
            verify(reviewApplicationService).addReviewNote(
                    eq(ReviewTargetType.SCENARIO), eq(TARGET_ID),
                    eq(ACTOR_REF), eq(ReviewActorRole.REVIEWER),
                    any());
        }

        @Test
        @DisplayName("ADMIN can also append note on CHANGES_REQUESTED")
        void adminAppendsNote() {
            mockCurrentState(ReviewState.CHANGES_REQUESTED);
            mockNoteResult(ReviewState.CHANGES_REQUESTED);

            InternalRevisionRequestResult result = service.requestRevision(
                    buildRequest(ReviewTargetType.IMAGE_ASSET, ReviewActorRole.ADMIN));

            assertThat(result.isAppendedNote()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 3. FORBIDDEN STATES
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Forbidden States — No Mutation")
    class ForbiddenStates {

        @Test
        @DisplayName("DRAFT throws InternalRevisionRequestException")
        void draftForbidden() {
            mockCurrentState(ReviewState.DRAFT);

            assertThatThrownBy(() -> service.requestRevision(
                    buildRequest(ReviewTargetType.SCENARIO, ReviewActorRole.REVIEWER)))
                    .isInstanceOf(InternalRevisionRequestException.class)
                    .satisfies(ex -> assertThat(
                            ((InternalRevisionRequestException) ex).getSafeErrorCode())
                            .isEqualTo("STATE_NOT_ALLOWED"));

            verifyNoRevisionOrNote();
        }

        @Test
        @DisplayName("AI_GENERATED throws InternalRevisionRequestException")
        void aiGeneratedForbidden() {
            mockCurrentState(ReviewState.AI_GENERATED);

            assertThatThrownBy(() -> service.requestRevision(
                    buildRequest(ReviewTargetType.STORYBOARD, ReviewActorRole.ADMIN)))
                    .isInstanceOf(InternalRevisionRequestException.class);

            verifyNoRevisionOrNote();
        }

        @Test
        @DisplayName("NEEDS_REVIEW throws InternalRevisionRequestException")
        void needsReviewForbidden() {
            mockCurrentState(ReviewState.NEEDS_REVIEW);

            assertThatThrownBy(() -> service.requestRevision(
                    buildRequest(ReviewTargetType.CONTENT_PROJECT, ReviewActorRole.REVIEWER)))
                    .isInstanceOf(InternalRevisionRequestException.class);

            verifyNoRevisionOrNote();
        }

        @Test
        @DisplayName("IN_REVIEW throws InternalRevisionRequestException")
        void inReviewForbidden() {
            mockCurrentState(ReviewState.IN_REVIEW);

            assertThatThrownBy(() -> service.requestRevision(
                    buildRequest(ReviewTargetType.PACKAGE, ReviewActorRole.ADMIN)))
                    .isInstanceOf(InternalRevisionRequestException.class);

            verifyNoRevisionOrNote();
        }

        @Test
        @DisplayName("REJECTED throws InternalRevisionRequestException")
        void rejectedForbidden() {
            mockCurrentState(ReviewState.REJECTED);

            assertThatThrownBy(() -> service.requestRevision(
                    buildRequest(ReviewTargetType.IMAGE_ASSET, ReviewActorRole.REVIEWER)))
                    .isInstanceOf(InternalRevisionRequestException.class);

            verifyNoRevisionOrNote();
        }

        @Test
        @DisplayName("Missing review record throws with REVIEW_NOT_FOUND")
        void missingRecordThrows() {
            when(reviewApplicationService.getReviewState(any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.requestRevision(
                    buildRequest(ReviewTargetType.SCENARIO, ReviewActorRole.REVIEWER)))
                    .isInstanceOf(InternalRevisionRequestException.class)
                    .satisfies(ex -> assertThat(
                            ((InternalRevisionRequestException) ex).getSafeErrorCode())
                            .isEqualTo("REVIEW_NOT_FOUND"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 4. ACTOR VALIDATION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. Actor Role Validation")
    class ActorValidation {

        @Test
        @DisplayName("REVIEWER is allowed")
        void reviewerAllowed() {
            mockCurrentState(ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockRevisionResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED);

            InternalRevisionRequestResult result = service.requestRevision(
                    buildRequest(ReviewTargetType.SCENARIO, ReviewActorRole.REVIEWER));

            assertThat(result.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
        }

        @Test
        @DisplayName("ADMIN is allowed")
        void adminAllowed() {
            mockCurrentState(ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockRevisionResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED);

            InternalRevisionRequestResult result = service.requestRevision(
                    buildRequest(ReviewTargetType.STORYBOARD, ReviewActorRole.ADMIN));

            assertThat(result.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
        }

        @Test
        @DisplayName("SYSTEM is rejected with SYSTEM_NOT_ALLOWED")
        void systemRejected() {
            assertThatThrownBy(() -> service.requestRevision(
                    buildRequest(ReviewTargetType.SCENARIO, ReviewActorRole.SYSTEM)))
                    .isInstanceOf(InternalRevisionRequestException.class)
                    .satisfies(ex -> assertThat(
                            ((InternalRevisionRequestException) ex).getSafeErrorCode())
                            .isEqualTo("SYSTEM_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("CREATOR is rejected with CREATOR_NOT_ALLOWED")
        void creatorRejected() {
            assertThatThrownBy(() -> service.requestRevision(
                    buildRequest(ReviewTargetType.PACKAGE, ReviewActorRole.CREATOR)))
                    .isInstanceOf(InternalRevisionRequestException.class)
                    .satisfies(ex -> assertThat(
                            ((InternalRevisionRequestException) ex).getSafeErrorCode())
                            .isEqualTo("CREATOR_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("Null request throws with NULL_REQUEST")
        void nullRequestThrows() {
            assertThatThrownBy(() -> service.requestRevision(null))
                    .isInstanceOf(InternalRevisionRequestException.class)
                    .satisfies(ex -> assertThat(
                            ((InternalRevisionRequestException) ex).getSafeErrorCode())
                            .isEqualTo("NULL_REQUEST"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 5. SAFETY REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. Safety Regression — Forbidden Fields")
    class SafetyRegression {

        private static final Set<String> FORBIDDEN_FIELDS = Set.of(
                "publicUrl", "signedUrl", "cdnUrl",
                "providerUrl", "storageKey", "bucket", "objectPath",
                "rawPayload", "base64", "bytes",
                "secret", "apiKey", "accessKey", "token",
                "stackTrace", "exceptionClass",
                "publishStatus", "exportStatus", "publishedAt", "exportedAt"
        );

        @Test
        @DisplayName("InternalRevisionRequest has no forbidden fields")
        void requestHasNoForbiddenFields() {
            Set<String> fields = Arrays.stream(InternalRevisionRequest.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Field '" + forbidden + "' must not exist").doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("InternalRevisionRequestResult has no forbidden fields")
        void resultHasNoForbiddenFields() {
            Set<String> fields = Arrays.stream(InternalRevisionRequestResult.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Field '" + forbidden + "' must not exist").doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("InternalRevisionRequestException has no forbidden fields")
        void exceptionHasNoForbiddenFields() {
            Set<String> fields = Arrays.stream(InternalRevisionRequestException.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Field '" + forbidden + "' must not exist").doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("InternalRevisionRequestService has no forbidden methods")
        void serviceHasNoForbiddenMethods() {
            Set<String> forbidden = Set.of(
                    "publish", "export", "getPublicUrl", "getSignedUrl",
                    "createPublicUrl", "createSignedUrl", "approve",
                    "returnToDraft", "generate", "regenerate",
                    "invokeProvider", "invokeStorage",
                    "inspectProvider", "inspectStorage"
            );
            Set<String> methods = Arrays.stream(InternalRevisionRequestService.class.getDeclaredMethods())
                    .map(Method::getName)
                    .collect(Collectors.toSet());
            for (String f : forbidden) {
                assertThat(methods).as("Method '" + f + "' must not exist").doesNotContain(f);
            }
        }

        @Test
        @DisplayName("InternalRevisionRequest.toString does not contain provider/storage/secret")
        void requestToStringIsSafe() {
            InternalRevisionRequest request = buildRequest(ReviewTargetType.SCENARIO, ReviewActorRole.REVIEWER);
            String str = request.toString();
            assertThat(str).doesNotContain("provider");
            assertThat(str).doesNotContain("storage");
            assertThat(str).doesNotContain("secret");
            assertThat(str).doesNotContain("base64");
            assertThat(str).doesNotContain("password");
        }

        @Test
        @DisplayName("InternalRevisionRequestResult.toString does not contain provider/storage/secret")
        void resultToStringIsSafe() {
            InternalRevisionRequestResult result = InternalRevisionRequestResult.builder()
                    .previousState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .currentState(ReviewState.CHANGES_REQUESTED)
                    .actionApplied("REQUEST_INTERNAL_REVISION")
                    .auditRecorded(true)
                    .build();
            String str = result.toString();
            assertThat(str).doesNotContain("provider");
            assertThat(str).doesNotContain("storage");
            assertThat(str).doesNotContain("secret");
            assertThat(str).doesNotContain("base64");
        }

        @Test
        @DisplayName("InternalRevisionRequestException.toString does not contain stack trace")
        void exceptionToStringIsSafe() {
            InternalRevisionRequestException ex = new InternalRevisionRequestException(
                    "test error", "TEST_CODE");
            String str = ex.toString();
            assertThat(str).doesNotContain("stackTrace");
            assertThat(str).doesNotContain("provider");
            assertThat(str).doesNotContain("storage");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 6. NO WRITE BYPASS
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. No Write Bypass — Only ReviewApplicationService Used")
    class NoWriteBypass {

        @Test
        @DisplayName("Service does not inject ReviewRecordRepository directly")
        void noDirectRepositoryInjection() {
            Field[] fields = InternalRevisionRequestService.class.getDeclaredFields();
            Set<String> fieldTypes = Arrays.stream(fields)
                    .map(f -> f.getType().getSimpleName())
                    .collect(Collectors.toSet());
            assertThat(fieldTypes).doesNotContain("ReviewRecordRepository");
            assertThat(fieldTypes).doesNotContain("ReviewAuditEventRepository");
        }

        @Test
        @DisplayName("Service does not inject ReviewDomainService directly")
        void noDirectDomainServiceUse() {
            Field[] fields = InternalRevisionRequestService.class.getDeclaredFields();
            Set<String> fieldTypes = Arrays.stream(fields)
                    .map(f -> f.getType().getSimpleName())
                    .collect(Collectors.toSet());
            assertThat(fieldTypes).doesNotContain("ReviewDomainService");
        }

        @Test
        @DisplayName("Forbidden state does not call any write method")
        void forbiddenStateNoWrite() {
            mockCurrentState(ReviewState.DRAFT);

            assertThatThrownBy(() -> service.requestRevision(
                    buildRequest(ReviewTargetType.SCENARIO, ReviewActorRole.REVIEWER)))
                    .isInstanceOf(InternalRevisionRequestException.class);

            verify(reviewApplicationService, never()).requestInternalRevision(any(), any(), any(), any(), any(), any());
            verify(reviewApplicationService, never()).addReviewNote(any(), any(), any(), any(), any());
            verify(reviewApplicationService, never()).requestChanges(any(), any(), any(), any(), any(), any());
            verify(reviewApplicationService, never()).approveForInternalUse(any(), any(), any(), any(), any(), any());
            verify(reviewApplicationService, never()).reject(any(), any(), any(), any(), any(), any());
            verify(reviewApplicationService, never()).returnToDraft(any(), any(), any(), any(), any(), any());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 7. TARGET TYPE COVERAGE
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("7. Target Type Coverage — All 5 Types")
    class TargetTypeCoverage {

        @Test
        @DisplayName("CONTENT_PROJECT revision works")
        void contentProjectWorks() {
            verifyTargetType(ReviewTargetType.CONTENT_PROJECT);
        }

        @Test
        @DisplayName("SCENARIO revision works")
        void scenarioWorks() {
            verifyTargetType(ReviewTargetType.SCENARIO);
        }

        @Test
        @DisplayName("STORYBOARD revision works")
        void storyboardWorks() {
            verifyTargetType(ReviewTargetType.STORYBOARD);
        }

        @Test
        @DisplayName("PACKAGE revision works")
        void packageWorks() {
            verifyTargetType(ReviewTargetType.PACKAGE);
        }

        @Test
        @DisplayName("IMAGE_ASSET revision works")
        void imageAssetWorks() {
            verifyTargetType(ReviewTargetType.IMAGE_ASSET);
        }

        private void verifyTargetType(ReviewTargetType targetType) {
            when(reviewApplicationService.getReviewState(eq(targetType), eq(TARGET_ID)))
                    .thenReturn(Optional.of(ReviewState.APPROVED_FOR_INTERNAL_USE));
            mockRevisionResultForType(targetType);

            InternalRevisionRequestResult result = service.requestRevision(
                    buildRequest(targetType, ReviewActorRole.REVIEWER));

            assertThat(result.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
            assertThat(result.getActionApplied()).isEqualTo("REQUEST_INTERNAL_REVISION");
        }

        private void mockRevisionResultForType(ReviewTargetType targetType) {
            when(reviewApplicationService.requestInternalRevision(
                    eq(targetType), eq(TARGET_ID), any(), any(), any(), any()))
                    .thenReturn(ReviewApplicationResult.builder()
                            .targetType(targetType)
                            .targetId(TARGET_ID)
                            .previousState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                            .currentState(ReviewState.CHANGES_REQUESTED)
                            .actionType(ReviewActionType.REQUEST_INTERNAL_REVISION)
                            .actorRole(ReviewActorRole.REVIEWER)
                            .changed(true)
                            .auditEventType(ReviewAuditEventType.INTERNAL_REVISION_REQUESTED)
                            .safeMessage("Revision requested")
                            .build());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 8. DOMAIN MODEL INTEGRATION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("8. Domain Model Integration")
    class DomainModelIntegration {

        @Test
        @DisplayName("REQUEST_INTERNAL_REVISION exists in ReviewActionType")
        void actionTypeExists() {
            assertThat(ReviewActionType.valueOf("REQUEST_INTERNAL_REVISION")).isNotNull();
        }

        @Test
        @DisplayName("INTERNAL_REVISION_REQUESTED exists in ReviewAuditEventType")
        void auditEventTypeExists() {
            assertThat(ReviewAuditEventType.valueOf("INTERNAL_REVISION_REQUESTED")).isNotNull();
        }

        @Test
        @DisplayName("ReviewTransitionPolicy allows APPROVED → CHANGES_REQUESTED via REQUEST_INTERNAL_REVISION")
        void transitionPolicyAllowsApprovedRevision() {
            ReviewState target = ReviewTransitionPolicy.targetFor(
                    ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.REQUEST_INTERNAL_REVISION);
            assertThat(target).isEqualTo(ReviewState.CHANGES_REQUESTED);
        }

        @Test
        @DisplayName("ReviewTransitionPolicy does NOT allow DRAFT via REQUEST_INTERNAL_REVISION")
        void transitionPolicyRejectsDraft() {
            ReviewState target = ReviewTransitionPolicy.targetFor(
                    ReviewState.DRAFT,
                    ReviewActionType.REQUEST_INTERNAL_REVISION);
            assertThat(target).isNull();
        }

        @Test
        @DisplayName("ReviewTransitionPolicy does NOT allow REJECTED via REQUEST_INTERNAL_REVISION")
        void transitionPolicyRejectsRejected() {
            ReviewState target = ReviewTransitionPolicy.targetFor(
                    ReviewState.REJECTED,
                    ReviewActionType.REQUEST_INTERNAL_REVISION);
            assertThat(target).isNull();
        }

        @Test
        @DisplayName("ReviewAuditEventFactory maps REQUEST_INTERNAL_REVISION correctly")
        void auditFactoryMapsCorrectly() {
            ReviewAuditEventType eventType = ReviewAuditEventFactory.mapActionToEventType(
                    ReviewActionType.REQUEST_INTERNAL_REVISION);
            assertThat(eventType).isEqualTo(ReviewAuditEventType.INTERNAL_REVISION_REQUESTED);
        }

        @Test
        @DisplayName("REVIEWER can perform REQUEST_INTERNAL_REVISION via transition policy")
        void reviewerCanTransition() {
            boolean allowed = ReviewTransitionPolicy.canTransition(
                    ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewState.CHANGES_REQUESTED,
                    ReviewActionType.REQUEST_INTERNAL_REVISION,
                    ReviewActorRole.REVIEWER);
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("ADMIN can perform REQUEST_INTERNAL_REVISION via transition policy")
        void adminCanTransition() {
            boolean allowed = ReviewTransitionPolicy.canTransition(
                    ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewState.CHANGES_REQUESTED,
                    ReviewActionType.REQUEST_INTERNAL_REVISION,
                    ReviewActorRole.ADMIN);
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("CREATOR cannot perform REQUEST_INTERNAL_REVISION via transition policy")
        void creatorCannotTransition() {
            boolean allowed = ReviewTransitionPolicy.canTransition(
                    ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewState.CHANGES_REQUESTED,
                    ReviewActionType.REQUEST_INTERNAL_REVISION,
                    ReviewActorRole.CREATOR);
            assertThat(allowed).isFalse();
        }

        @Test
        @DisplayName("SYSTEM cannot perform REQUEST_INTERNAL_REVISION via transition policy")
        void systemCannotTransition() {
            boolean allowed = ReviewTransitionPolicy.canTransition(
                    ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewState.CHANGES_REQUESTED,
                    ReviewActionType.REQUEST_INTERNAL_REVISION,
                    ReviewActorRole.SYSTEM);
            assertThat(allowed).isFalse();
        }

        @Test
        @DisplayName("ReviewActionType still has no forbidden values")
        void noForbiddenActionTypes() {
            Set<String> names = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISH", "EXPORT", "AUTO_APPROVE",
                    "GENERIC_STATUS_UPDATE", "MARK_COMPLETED", "READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("ReviewAuditEventType still has no forbidden values")
        void noForbiddenAuditEventTypes() {
            Set<String> names = Arrays.stream(ReviewAuditEventType.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED", "EXPORTED", "COMPLETED",
                    "READY_FOR_PUBLISH", "PUBLIC_URL_CREATED", "SIGNED_URL_CREATED", "AUTO_APPROVED");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 9. REQUEST VALIDATION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("9. Request Validation")
    class RequestValidation {

        @Test
        @DisplayName("Null targetType is rejected")
        void nullTargetType() {
            assertThatThrownBy(() -> InternalRevisionRequest.builder()
                    .targetId(TARGET_ID)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .actorReference(ACTOR_REF)
                    .safeMessage(SAFE_MSG)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Null targetId is rejected")
        void nullTargetId() {
            assertThatThrownBy(() -> InternalRevisionRequest.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .actorReference(ACTOR_REF)
                    .safeMessage(SAFE_MSG)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Null actorRole is rejected")
        void nullActorRole() {
            assertThatThrownBy(() -> InternalRevisionRequest.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .actorReference(ACTOR_REF)
                    .safeMessage(SAFE_MSG)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Blank actorReference is rejected")
        void blankActorReference() {
            assertThatThrownBy(() -> InternalRevisionRequest.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .actorReference("")
                    .safeMessage(SAFE_MSG)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Blank safeMessage is rejected")
        void blankMessage() {
            assertThatThrownBy(() -> InternalRevisionRequest.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .actorReference(ACTOR_REF)
                    .safeMessage("")
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("safeMessage is sanitized via ReviewTextSafety — control chars removed")
        void messageSanitized() {
            InternalRevisionRequest request = InternalRevisionRequest.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .actorReference(ACTOR_REF)
                    .safeMessage("test\u0000message\u0007with control chars")
                    .build();
            assertThat(request.getSafeMessage()).doesNotContain("\u0000");
            assertThat(request.getSafeMessage()).doesNotContain("\u0007");
            assertThat(request.getSafeMessage()).contains("test");
            assertThat(request.getSafeMessage()).contains("message");
        }

        @Test
        @DisplayName("safeReasonCode is sanitized via ReviewTextSafety")
        void reasonCodeSanitized() {
            InternalRevisionRequest request = InternalRevisionRequest.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .actorReference(ACTOR_REF)
                    .safeReasonCode("VALID_CODE-123")
                    .safeMessage(SAFE_MSG)
                    .build();
            assertThat(request.getSafeReasonCode()).isEqualTo("VALID_CODE-123");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════

    private InternalRevisionRequest buildRequest(ReviewTargetType targetType, ReviewActorRole actorRole) {
        return InternalRevisionRequest.builder()
                .targetType(targetType)
                .targetId(TARGET_ID)
                .actorRole(actorRole)
                .actorReference(ACTOR_REF)
                .safeReasonCode(SAFE_REASON)
                .safeMessage(SAFE_MSG)
                .build();
    }

    private void mockCurrentState(ReviewState state) {
        when(reviewApplicationService.getReviewState(any(), eq(TARGET_ID)))
                .thenReturn(Optional.of(state));
    }

    private void mockRevisionResult(ReviewState from, ReviewState to) {
        when(reviewApplicationService.requestInternalRevision(any(), eq(TARGET_ID), any(), any(), any(), any()))
                .thenReturn(ReviewApplicationResult.builder()
                        .previousState(from)
                        .currentState(to)
                        .actionType(ReviewActionType.REQUEST_INTERNAL_REVISION)
                        .actorRole(ReviewActorRole.REVIEWER)
                        .changed(true)
                        .auditEventType(ReviewAuditEventType.INTERNAL_REVISION_REQUESTED)
                        .safeMessage("Revision requested")
                        .build());
    }

    private void mockNoteResult(ReviewState state) {
        when(reviewApplicationService.addReviewNote(any(), eq(TARGET_ID), any(), any(), any()))
                .thenReturn(ReviewApplicationResult.builder()
                        .previousState(state)
                        .currentState(state)
                        .actionType(ReviewActionType.ADD_REVIEW_NOTE)
                        .actorRole(ReviewActorRole.REVIEWER)
                        .changed(false)
                        .auditEventType(ReviewAuditEventType.REVIEW_NOTE_ADDED)
                        .safeMessage("Note appended")
                        .build());
    }

    private void verifyNoRevisionOrNote() {
        verify(reviewApplicationService, never()).requestInternalRevision(any(), any(), any(), any(), any(), any());
        verify(reviewApplicationService, never()).addReviewNote(any(), any(), any(), any(), any());
    }
}
