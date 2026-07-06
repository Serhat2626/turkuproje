package com.anadoluatlasi.backend.review;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FP-015 Task 03: Internal Revision Request Admin API + Approved Inventory API Hardening.
 *
 * <p>Covers: endpoint happy path, idempotent path, actor rejection, forbidden states,
 * request validation, safe payload, readiness regression, endpoint inventory,
 * no side effects, and migration/schema regression.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FP-015 Task 03 — Internal Revision Request Admin API")
class InternalRevisionRequestAdminApiTest {

    private static final ReviewTargetType TARGET_TYPE = ReviewTargetType.SCENARIO;
    private static final UUID TARGET_ID = UUID.fromString("b1c2d3e4-f5a6-7890-bcde-f12345678901");
    private static final String ACTOR_REF = "reviewer-01";
    private static final String SAFE_MSG = "İçerik güncellenmeli";
    private static final String SAFE_REASON = "CONTENT_OUTDATED";

    @Mock
    private ReviewApplicationService reviewApplicationService;
    @Mock
    private ReviewHandoffService reviewHandoffService;
    @Mock
    private AdminPreviewService adminPreviewService;
    @Mock
    private InternalApprovalReadinessService internalApprovalReadinessService;
    @Mock
    private InternalRevisionRequestService internalRevisionRequestService;

    private AdminReviewController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminReviewController(
                reviewApplicationService, reviewHandoffService,
                adminPreviewService, internalApprovalReadinessService,
                internalRevisionRequestService);
    }

    // ════════════════════════════════════════════════════════════════
    // A. ENDPOINT HAPPY PATH
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("A. Endpoint Happy Path")
    class EndpointHappyPath {

        @Test
        @DisplayName("POST request-internal-revision for APPROVED with REVIEWER succeeds")
        void approvedWithReviewerSucceeds() {
            mockServiceResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED,
                    "REQUEST_INTERNAL_REVISION", false);

            var response = callEndpoint(ReviewActorRole.REVIEWER);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            var data = response.getBody().getData();
            assertThat(data.getPreviousState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(data.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
            assertThat(data.getActionApplied()).isEqualTo("REQUEST_INTERNAL_REVISION");
        }

        @Test
        @DisplayName("POST request-internal-revision for APPROVED with ADMIN succeeds")
        void approvedWithAdminSucceeds() {
            mockServiceResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED,
                    "REQUEST_INTERNAL_REVISION", false);

            var response = callEndpoint(ReviewActorRole.ADMIN);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Response includes targetType and targetId from path")
        void responseIncludesTarget() {
            mockServiceResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED,
                    "REQUEST_INTERNAL_REVISION", false);

            var data = callEndpoint(ReviewActorRole.REVIEWER).getBody().getData();

            assertThat(data.getTargetType()).isEqualTo(TARGET_TYPE);
            assertThat(data.getTargetId()).isEqualTo(TARGET_ID);
        }

        @Test
        @DisplayName("Audit flags are safe and correct")
        void auditFlagsCorrect() {
            mockServiceResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED,
                    "REQUEST_INTERNAL_REVISION", false);

            var data = callEndpoint(ReviewActorRole.REVIEWER).getBody().getData();

            assertThat(data.isAuditRecorded()).isTrue();
            assertThat(data.isAppendedNote()).isFalse();
        }

        @Test
        @DisplayName("Safe reason code is passed through")
        void safeReasonCodePassedThrough() {
            mockServiceResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED,
                    "REQUEST_INTERNAL_REVISION", false);

            var data = callEndpoint(ReviewActorRole.REVIEWER).getBody().getData();

            assertThat(data.getSafeReasonCode()).isEqualTo(SAFE_REASON);
        }

        @Test
        @DisplayName("UpdatedAt is set in response")
        void updatedAtSet() {
            mockServiceResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED,
                    "REQUEST_INTERNAL_REVISION", false);

            var data = callEndpoint(ReviewActorRole.REVIEWER).getBody().getData();

            assertThat(data.getUpdatedAt()).isNotNull();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // B. IDEMPOTENT CHANGES_REQUESTED PATH
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("B. Idempotent CHANGES_REQUESTED Path")
    class IdempotentPath {

        @Test
        @DisplayName("POST when target is CHANGES_REQUESTED returns note append result")
        void changesRequestedReturnsNoteAppend() {
            mockServiceResult(ReviewState.CHANGES_REQUESTED, ReviewState.CHANGES_REQUESTED,
                    "NOTE_APPENDED", true);

            var data = callEndpoint(ReviewActorRole.REVIEWER).getBody().getData();

            assertThat(data.getPreviousState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
            assertThat(data.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
            assertThat(data.getActionApplied()).isEqualTo("NOTE_APPENDED");
            assertThat(data.isAppendedNote()).isTrue();
        }

        @Test
        @DisplayName("Idempotent path does not auto-approve")
        void noAutoApprove() {
            mockServiceResult(ReviewState.CHANGES_REQUESTED, ReviewState.CHANGES_REQUESTED,
                    "NOTE_APPENDED", true);

            var data = callEndpoint(ReviewActorRole.ADMIN).getBody().getData();

            assertThat(data.getCurrentState()).isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("Idempotent path does not publish or export")
        void noPublishExport() {
            mockServiceResult(ReviewState.CHANGES_REQUESTED, ReviewState.CHANGES_REQUESTED,
                    "NOTE_APPENDED", true);

            var data = callEndpoint(ReviewActorRole.REVIEWER).getBody().getData();

            assertThat(data.getActionApplied()).doesNotContain("PUBLISH");
            assertThat(data.getActionApplied()).doesNotContain("EXPORT");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // C. ACTOR REJECTION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("C. Actor Rejection")
    class ActorRejection {

        @Test
        @DisplayName("SYSTEM rejected safely with 403")
        void systemRejected() {
            when(internalRevisionRequestService.requestRevision(any()))
                    .thenThrow(new InternalRevisionRequestException(
                            "SYSTEM cannot request internal revision", "SYSTEM_NOT_ALLOWED"));

            var ex = catchThrowableOfType(() -> callEndpoint(ReviewActorRole.SYSTEM),
                    InternalRevisionRequestException.class);
            var response = controller.handleInternalRevisionException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().isSuccess()).isFalse();
        }

        @Test
        @DisplayName("CREATOR rejected safely with 403")
        void creatorRejected() {
            when(internalRevisionRequestService.requestRevision(any()))
                    .thenThrow(new InternalRevisionRequestException(
                            "CREATOR cannot request internal revision", "CREATOR_NOT_ALLOWED"));

            var ex = catchThrowableOfType(() -> callEndpoint(ReviewActorRole.CREATOR),
                    InternalRevisionRequestException.class);
            var response = controller.handleInternalRevisionException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Rejection error message is safe")
        void rejectionMessageSafe() {
            when(internalRevisionRequestService.requestRevision(any()))
                    .thenThrow(new InternalRevisionRequestException(
                            "SYSTEM cannot request internal revision", "SYSTEM_NOT_ALLOWED"));

            var ex = catchThrowableOfType(() -> callEndpoint(ReviewActorRole.SYSTEM),
                    InternalRevisionRequestException.class);
            var response = controller.handleInternalRevisionException(ex);
            String message = response.getBody().getMessage();
            assertThat(message).doesNotContain("stackTrace");
            assertThat(message).doesNotContain("provider");
            assertThat(message).doesNotContain("storage");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // D. FORBIDDEN STATE REJECTION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("D. Forbidden State Rejection")
    class ForbiddenStateRejection {

        @Test
        @DisplayName("DRAFT rejected safely with 409")
        void draftRejected() {
            assertForbiddenState("DRAFT");
        }

        @Test
        @DisplayName("AI_GENERATED rejected safely with 409")
        void aiGeneratedRejected() {
            assertForbiddenState("AI_GENERATED");
        }

        @Test
        @DisplayName("NEEDS_REVIEW rejected safely with 409")
        void needsReviewRejected() {
            assertForbiddenState("NEEDS_REVIEW");
        }

        @Test
        @DisplayName("IN_REVIEW rejected safely with 409")
        void inReviewRejected() {
            assertForbiddenState("IN_REVIEW");
        }

        @Test
        @DisplayName("REJECTED rejected safely with 409")
        void rejectedRejected() {
            assertForbiddenState("REJECTED");
        }

        @Test
        @DisplayName("Missing review record rejected with 404")
        void missingRecordRejected() {
            when(internalRevisionRequestService.requestRevision(any()))
                    .thenThrow(new InternalRevisionRequestException(
                            "Review record not found", "REVIEW_NOT_FOUND"));

            var ex = catchThrowableOfType(() -> callEndpoint(ReviewActorRole.REVIEWER),
                    InternalRevisionRequestException.class);
            var response = controller.handleInternalRevisionException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().isSuccess()).isFalse();
        }

        private void assertForbiddenState(String stateName) {
            when(internalRevisionRequestService.requestRevision(any()))
                    .thenThrow(new InternalRevisionRequestException(
                            "Internal revision request not allowed from state " + stateName,
                            "STATE_NOT_ALLOWED"));

            var ex = catchThrowableOfType(() -> callEndpoint(ReviewActorRole.REVIEWER),
                    InternalRevisionRequestException.class);
            var response = controller.handleInternalRevisionException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().isSuccess()).isFalse();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // E. REQUEST VALIDATION AND SANITIZATION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("E. Request Validation and Sanitization")
    class RequestValidation {

        @Test
        @DisplayName("Missing actorRole returns 400")
        void missingActorRole() {
            var request = new AdminInternalRevisionRequest();
            request.setActorReference(ACTOR_REF);
            request.setSafeMessage(SAFE_MSG);

            assertThatThrownBy(() -> controller.requestInternalRevision(TARGET_TYPE, TARGET_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("actorRole");
        }

        @Test
        @DisplayName("Missing actorReference returns 400")
        void missingActorReference() {
            var request = new AdminInternalRevisionRequest();
            request.setActorRole(ReviewActorRole.REVIEWER);
            request.setSafeMessage(SAFE_MSG);

            assertThatThrownBy(() -> controller.requestInternalRevision(TARGET_TYPE, TARGET_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("actorReference");
        }

        @Test
        @DisplayName("Missing safeMessage returns 400")
        void missingSafeMessage() {
            var request = new AdminInternalRevisionRequest();
            request.setActorRole(ReviewActorRole.REVIEWER);
            request.setActorReference(ACTOR_REF);

            assertThatThrownBy(() -> controller.requestInternalRevision(TARGET_TYPE, TARGET_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("safeMessage");
        }

        @Test
        @DisplayName("Blank safeMessage returns 400")
        void blankSafeMessage() {
            var request = new AdminInternalRevisionRequest();
            request.setActorRole(ReviewActorRole.REVIEWER);
            request.setActorReference(ACTOR_REF);
            request.setSafeMessage("   ");

            assertThatThrownBy(() -> controller.requestInternalRevision(TARGET_TYPE, TARGET_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("safeMessage");
        }

        @Test
        @DisplayName("Blank actorReference returns 400")
        void blankActorReference() {
            var request = new AdminInternalRevisionRequest();
            request.setActorRole(ReviewActorRole.REVIEWER);
            request.setActorReference("");
            request.setSafeMessage(SAFE_MSG);

            assertThatThrownBy(() -> controller.requestInternalRevision(TARGET_TYPE, TARGET_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("actorReference");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // F. SAFE RESPONSE PAYLOAD
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("F. Safe Response Payload")
    class SafeResponsePayload {

        private static final Set<String> FORBIDDEN_FIELDS = Set.of(
                "publicUrl", "signedUrl", "cdnUrl",
                "providerUrl", "storageKey", "bucket", "objectPath",
                "rawPayload", "base64", "bytes",
                "secret", "apiKey", "accessKey", "token",
                "stackTrace", "exceptionClass",
                "publishStatus", "exportStatus", "publishedAt", "exportedAt",
                "providerResponse", "storageResponse", "rawError", "html"
        );

        @Test
        @DisplayName("AdminInternalRevisionRequest has no forbidden fields")
        void requestNoForbiddenFields() {
            Set<String> fields = getDeclaredFieldNames(AdminInternalRevisionRequest.class);
            for (String f : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Field '" + f + "'").doesNotContain(f);
            }
        }

        @Test
        @DisplayName("AdminInternalRevisionResponse has no forbidden fields")
        void responseNoForbiddenFields() {
            Set<String> fields = getDeclaredFieldNames(AdminInternalRevisionResponse.class);
            for (String f : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Field '" + f + "'").doesNotContain(f);
            }
        }

        @Test
        @DisplayName("AdminInternalRevisionRequest.toString is safe")
        void requestToStringSafe() {
            var request = new AdminInternalRevisionRequest();
            request.setActorRole(ReviewActorRole.REVIEWER);
            request.setActorReference(ACTOR_REF);
            request.setSafeMessage(SAFE_MSG);
            String str = request.toString();
            assertSafeString(str);
        }

        @Test
        @DisplayName("AdminInternalRevisionResponse.toString is safe")
        void responseToStringSafe() {
            var response = AdminInternalRevisionResponse.builder()
                    .targetType(TARGET_TYPE)
                    .targetId(TARGET_ID)
                    .previousState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .currentState(ReviewState.CHANGES_REQUESTED)
                    .actionApplied("REQUEST_INTERNAL_REVISION")
                    .auditRecorded(true)
                    .build();
            String str = response.toString();
            assertSafeString(str);
        }

        @Test
        @DisplayName("Serialized response JSON does not contain forbidden keywords")
        void serializedResponseSafe() {
            mockServiceResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED,
                    "REQUEST_INTERNAL_REVISION", false);

            var data = callEndpoint(ReviewActorRole.REVIEWER).getBody().getData();
            String str = data.toString();
            assertSafeString(str);
        }

        private void assertSafeString(String str) {
            assertThat(str).doesNotContain("provider");
            assertThat(str).doesNotContain("storage");
            assertThat(str).doesNotContain("secret");
            assertThat(str).doesNotContain("base64");
            assertThat(str).doesNotContain("password");
            assertThat(str).doesNotContain("stackTrace");
            assertThat(str).doesNotContain("publish");
            assertThat(str).doesNotContain("export");
            assertThat(str).doesNotContain("cdnUrl");
            assertThat(str).doesNotContain("signedUrl");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // G. APPROVED INVENTORY/READINESS REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("G. Approved Inventory/Readiness Regression")
    class ReadinessRegression {

        @Test
        @DisplayName("GET approved/internal-readiness still returns approved candidates")
        void approvedReadinessStillWorks() {
            SafeInternalApprovalReadiness readiness = buildReadiness(
                    ReviewState.APPROVED_FOR_INTERNAL_USE, InternalApprovalReadinessAvailability.READY);
            when(internalApprovalReadinessService.getApprovedReadinessList(any()))
                    .thenReturn(List.of(readiness));

            var response = controller.getApprovedReadinessList(null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getReviewState())
                    .isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("Single target internal-readiness for CHANGES_REQUESTED returns NOT_APPROVED")
        void changesRequestedNotReady() {
            SafeInternalApprovalReadiness readiness = buildReadiness(
                    ReviewState.CHANGES_REQUESTED, InternalApprovalReadinessAvailability.NOT_APPROVED);
            when(internalApprovalReadinessService.getReadiness(any(), any()))
                    .thenReturn(readiness);

            var response = controller.getInternalReadiness(TARGET_TYPE, TARGET_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getAvailability())
                    .isEqualTo(InternalApprovalReadinessAvailability.NOT_APPROVED);
        }

        @Test
        @DisplayName("Readiness response has no forbidden fields")
        void readinessResponseSafe() {
            Set<String> fields = getDeclaredFieldNames(SafeInternalApprovalReadiness.class);
            for (String f : Set.of("publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                    "storageKey", "base64", "bytes", "publishStatus", "exportStatus")) {
                assertThat(fields).as("Field '" + f + "'").doesNotContain(f);
            }
        }

        @Test
        @DisplayName("GET internal-readiness endpoints are read-only (no POST)")
        void readinessEndpointsReadOnly() {
            long readinessGetCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .filter(m -> {
                        String[] paths = m.getAnnotation(GetMapping.class).value();
                        return Arrays.stream(paths).anyMatch(p -> p.contains("internal-readiness"));
                    })
                    .count();
            assertThat(readinessGetCount).isGreaterThanOrEqualTo(2);

            long readinessPostCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .filter(m -> {
                        String[] paths = m.getAnnotation(PostMapping.class).value();
                        return Arrays.stream(paths).anyMatch(p -> p.contains("internal-readiness"));
                    })
                    .count();
            assertThat(readinessPostCount).isZero();
        }

        @Test
        @DisplayName("Readiness APIs do not include executable publish/export actions")
        void readinessNoPublishExportActions() {
            Set<String> fields = getDeclaredFieldNames(SafeInternalApprovalReadiness.class);
            assertThat(fields).doesNotContain("publishAction", "exportAction",
                    "publishEndpoint", "exportEndpoint");
        }

        private SafeInternalApprovalReadiness buildReadiness(ReviewState state,
                                                              InternalApprovalReadinessAvailability availability) {
            return SafeInternalApprovalReadiness.builder()
                    .targetType(TARGET_TYPE)
                    .targetId(TARGET_ID)
                    .reviewState(state)
                    .availability(availability)
                    .safeNextInternalActions(List.of())
                    .safePreviewAvailable(true)
                    .safeTitle("Test title")
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // H. ENDPOINT INVENTORY REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("H. Endpoint Inventory Regression")
    class EndpointInventory {

        @Test
        @DisplayName("Total endpoints = 17")
        void totalEndpoints17() {
            long total = countEndpoints();
            assertThat(total).isEqualTo(17);
        }

        @Test
        @DisplayName("POST endpoints = 9")
        void postEndpoints9() {
            long postCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .count();
            assertThat(postCount).isEqualTo(9);
        }

        @Test
        @DisplayName("GET endpoints = 8")
        void getEndpoints8() {
            long getCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .count();
            assertThat(getCount).isEqualTo(8);
        }

        @Test
        @DisplayName("New endpoint is exactly POST /{targetType}/{targetId}/request-internal-revision")
        void newEndpointPath() {
            Set<String> postPaths = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(PostMapping.class).value()))
                    .collect(Collectors.toSet());
            assertThat(postPaths).contains("/{targetType}/{targetId}/request-internal-revision");
        }

        @Test
        @DisplayName("No forbidden endpoint paths")
        void noForbiddenPaths() {
            Set<String> allPaths = collectAllPaths();
            Set<String> forbidden = Set.of(
                    "publish", "export", "public", "signed", "cdn",
                    "provider", "storage-inspection", "raw", "base64",
                    "media-library", "video", "audio"
            );
            for (String path : allPaths) {
                // request-internal-revision is allowed
                if (path.contains("request-internal-revision")) continue;
                String lower = path.toLowerCase();
                for (String f : forbidden) {
                    assertThat(lower).as("Path '" + path + "' contains '" + f + "'")
                            .doesNotContain(f);
                }
            }
        }

        @Test
        @DisplayName("Controller is annotated with /api/admin/reviews base path")
        void controllerBasePath() {
            var mapping = AdminReviewController.class.getAnnotation(
                    org.springframework.web.bind.annotation.RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).contains("/api/admin/reviews");
        }

        private long countEndpoints() {
            return Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)
                            || m.isAnnotationPresent(GetMapping.class))
                    .count();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // I. NO SIDE EFFECTS REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("I. No Side Effects Regression")
    class NoSideEffects {

        @Test
        @DisplayName("Controller does not inject ReviewRecordRepository")
        void noDirectRepository() {
            Set<String> fieldTypes = Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName())
                    .collect(Collectors.toSet());
            assertThat(fieldTypes).doesNotContain("ReviewRecordRepository",
                    "ReviewAuditEventRepository");
        }

        @Test
        @DisplayName("Controller has no forbidden methods")
        void noForbiddenMethods() {
            Set<String> methods = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .map(Method::getName).collect(Collectors.toSet());
            Set<String> forbidden = Set.of(
                    "publish", "export", "getPublicUrl", "getSignedUrl",
                    "createPublicUrl", "createSignedUrl",
                    "generate", "regenerate", "invokeProvider", "invokeStorage",
                    "inspectProvider", "inspectStorage",
                    "autoApprove", "markCompleted", "markReadyForPublish"
            );
            for (String f : forbidden) {
                assertThat(methods).as("Method '" + f + "'").doesNotContain(f);
            }
        }

        @Test
        @DisplayName("Successful call delegates to InternalRevisionRequestService only")
        void delegatesToServiceOnly() {
            mockServiceResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED,
                    "REQUEST_INTERNAL_REVISION", false);

            callEndpoint(ReviewActorRole.REVIEWER);

            verify(internalRevisionRequestService).requestRevision(any());
            verifyNoInteractions(reviewHandoffService);
            verifyNoInteractions(adminPreviewService);
        }

        @Test
        @DisplayName("Endpoint does not call reviewApplicationService directly")
        void noDirectAppServiceCall() {
            mockServiceResult(ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewState.CHANGES_REQUESTED,
                    "REQUEST_INTERNAL_REVISION", false);

            callEndpoint(ReviewActorRole.REVIEWER);

            verify(reviewApplicationService, never()).approveForInternalUse(any(), any(), any(), any(), any(), any());
            verify(reviewApplicationService, never()).reject(any(), any(), any(), any(), any(), any());
            verify(reviewApplicationService, never()).returnToDraft(any(), any(), any(), any(), any(), any());
            verify(reviewApplicationService, never()).submitForReview(any(), any(), any(), any(), any());
            verify(reviewApplicationService, never()).startReview(any(), any(), any(), any(), any());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // J. MIGRATION/SCHEMA REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("J. Migration/Schema Regression")
    class MigrationRegression {

        @Test
        @DisplayName("No new entity annotations on controller")
        void noEntityAnnotations() {
            assertThat(AdminReviewController.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
            assertThat(AdminReviewController.class.isAnnotationPresent(
                    jakarta.persistence.Table.class)).isFalse();
        }

        @Test
        @DisplayName("InternalRevisionRequestService is not an entity")
        void serviceNotEntity() {
            assertThat(InternalRevisionRequestService.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("AdminInternalRevisionRequest is not an entity")
        void requestNotEntity() {
            assertThat(AdminInternalRevisionRequest.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("AdminInternalRevisionResponse is not an entity")
        void responseNotEntity() {
            assertThat(AdminInternalRevisionResponse.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private ResponseEntity<ApiResponse<AdminInternalRevisionResponse>> callEndpoint(ReviewActorRole actorRole) {
        var request = new AdminInternalRevisionRequest();
        request.setActorRole(actorRole);
        request.setActorReference(ACTOR_REF);
        request.setSafeReasonCode(SAFE_REASON);
        request.setSafeMessage(SAFE_MSG);
        return controller.requestInternalRevision(TARGET_TYPE, TARGET_ID, request);
    }

    private void mockServiceResult(ReviewState from, ReviewState to, String action, boolean appendedNote) {
        InternalRevisionRequestResult result = InternalRevisionRequestResult.builder()
                .previousState(from)
                .currentState(to)
                .actionApplied(action)
                .safeReasonCode(SAFE_REASON)
                .safeMessage(SAFE_MSG)
                .appendedNote(appendedNote)
                .auditRecorded(true)
                .updatedAt(LocalDateTime.now())
                .build();
        when(internalRevisionRequestService.requestRevision(any())).thenReturn(result);
    }

    private static Set<String> collectAllPaths() {
        return Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                .flatMap(m -> {
                    java.util.List<String> paths = new java.util.ArrayList<>();
                    if (m.isAnnotationPresent(PostMapping.class)) {
                        paths.addAll(Arrays.asList(m.getAnnotation(PostMapping.class).value()));
                    }
                    if (m.isAnnotationPresent(GetMapping.class)) {
                        paths.addAll(Arrays.asList(m.getAnnotation(GetMapping.class).value()));
                    }
                    return paths.stream();
                })
                .collect(Collectors.toSet());
    }

    private static Set<String> getDeclaredFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());
    }
}
