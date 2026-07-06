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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
 * FP-012 Task 07 — Admin Review Controller / Internal Admin API.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FP-012 Task 07 — Admin Review Controller")
class AdminReviewControllerTest {

    @Mock
    private ReviewApplicationService reviewApplicationService;
    @Mock
    private ReviewHandoffService reviewHandoffService;
    @Mock
    private AdminPreviewService adminPreviewService;
    @Mock
    private InternalApprovalReadinessService internalApprovalReadinessService;

    private AdminReviewController controller;

    private static final ReviewTargetType TARGET_TYPE = ReviewTargetType.CONTENT_PROJECT;
    private static final UUID TARGET_ID = UUID.randomUUID();
    private static final String ACTOR_REF = "admin-01";

    @BeforeEach
    void setUp() {
        controller = new AdminReviewController(reviewApplicationService, reviewHandoffService, adminPreviewService, internalApprovalReadinessService);
    }

    // ── 1. Action Endpoints Happy Path ──────────────────────────────

    @Nested
    @DisplayName("1. Action Endpoints Happy Path")
    class ActionHappyPath {

        @Test
        @DisplayName("submitForReview returns safe response")
        void submitForReview() {
            mockServiceResult(ReviewState.DRAFT, ReviewState.NEEDS_REVIEW,
                    ReviewActionType.SUBMIT_FOR_REVIEW, ReviewAuditEventType.REVIEW_SUBMITTED);

            var request = buildRequest(ReviewActionType.SUBMIT_FOR_REVIEW, ReviewActorRole.CREATOR);
            ResponseEntity<ApiResponse<AdminReviewActionResponse>> response = controller.submitForReview(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("startReview returns safe response")
        void startReview() {
            mockServiceResult(ReviewState.NEEDS_REVIEW, ReviewState.IN_REVIEW,
                    ReviewActionType.START_REVIEW, ReviewAuditEventType.REVIEW_STARTED);

            var request = buildRequest(ReviewActionType.START_REVIEW, ReviewActorRole.REVIEWER);
            var response = controller.startReview(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getCurrentState()).isEqualTo(ReviewState.IN_REVIEW);
        }

        @Test
        @DisplayName("requestChanges returns safe response")
        void requestChanges() {
            mockServiceResult(ReviewState.IN_REVIEW, ReviewState.CHANGES_REQUESTED,
                    ReviewActionType.REQUEST_CHANGES, ReviewAuditEventType.CHANGES_REQUESTED);

            var request = buildRequest(ReviewActionType.REQUEST_CHANGES, ReviewActorRole.REVIEWER);
            request.setReasonCode("QUALITY");
            request.setNote("Needs fixes");
            var response = controller.requestChanges(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
        }

        @Test
        @DisplayName("approveForInternalUse returns safe response — NOT publish")
        void approveInternal() {
            mockServiceResult(ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED);

            var request = buildRequest(ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER);
            var response = controller.approveForInternalUse(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            AdminReviewActionResponse data = response.getBody().getData();
            assertThat(data.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(data.getCurrentState().name()).doesNotContain("PUBLISH");
        }

        @Test
        @DisplayName("reject returns safe response")
        void reject() {
            mockServiceResult(ReviewState.IN_REVIEW, ReviewState.REJECTED,
                    ReviewActionType.REJECT, ReviewAuditEventType.REVIEW_REJECTED);

            var request = buildRequest(ReviewActionType.REJECT, ReviewActorRole.REVIEWER);
            request.setReasonCode("QUALITY_ISSUE");
            var response = controller.reject(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getCurrentState()).isEqualTo(ReviewState.REJECTED);
        }

        @Test
        @DisplayName("returnToDraft returns safe response")
        void returnToDraft() {
            mockServiceResult(ReviewState.CHANGES_REQUESTED, ReviewState.DRAFT,
                    ReviewActionType.RETURN_TO_DRAFT, ReviewAuditEventType.RETURNED_TO_DRAFT);

            var request = buildRequest(ReviewActionType.RETURN_TO_DRAFT, ReviewActorRole.CREATOR);
            var response = controller.returnToDraft(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getCurrentState()).isEqualTo(ReviewState.DRAFT);
        }

        @Test
        @DisplayName("addReviewNote returns safe response without state change")
        void addNote() {
            ReviewApplicationResult result = ReviewApplicationResult.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .previousState(ReviewState.IN_REVIEW).currentState(ReviewState.IN_REVIEW)
                    .actionType(ReviewActionType.ADD_REVIEW_NOTE).actorRole(ReviewActorRole.REVIEWER)
                    .changed(false).auditEventType(ReviewAuditEventType.REVIEW_NOTE_ADDED)
                    .occurredAt(LocalDateTime.now()).build();
            when(reviewApplicationService.addReviewNote(any(), any(), any(), any(), any())).thenReturn(result);

            var request = buildRequest(ReviewActionType.ADD_REVIEW_NOTE, ReviewActorRole.REVIEWER);
            request.setNote("Looking good");
            var response = controller.addReviewNote(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().isChanged()).isFalse();
        }
    }

    // ── 2. Query Endpoints Happy Path ───────────────────────────────

    @Nested
    @DisplayName("2. Query Endpoints Happy Path")
    class QueryHappyPath {

        @Test
        @DisplayName("getReviewState returns safe summary")
        void getState() {
            AdminReviewStateSummary summary = AdminReviewStateSummary.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .currentState(ReviewState.IN_REVIEW).build();
            when(reviewApplicationService.getReviewStateSummary(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(summary));

            var response = controller.getReviewState(TARGET_TYPE, TARGET_ID);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getCurrentState()).isEqualTo(ReviewState.IN_REVIEW);
        }

        @Test
        @DisplayName("getReviewState returns 404 when not found")
        void getStateMissing() {
            when(reviewApplicationService.getReviewStateSummary(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());

            var response = controller.getReviewState(TARGET_TYPE, TARGET_ID);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().isSuccess()).isFalse();
        }

        @Test
        @DisplayName("getAuditTimeline returns safe timeline items")
        void getAudit() {
            ReviewAuditEventEntity event = ReviewAuditEventEntity.builder()
                    .reviewRecordId(UUID.randomUUID())
                    .eventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                    .fromState(ReviewState.DRAFT).toState(ReviewState.NEEDS_REVIEW)
                    .actionType(ReviewActionType.SUBMIT_FOR_REVIEW)
                    .actorRole(ReviewActorRole.CREATOR).actorReference("creator-01")
                    .occurredAt(LocalDateTime.now()).build();
            when(reviewApplicationService.getAuditTimeline(TARGET_TYPE, TARGET_ID))
                    .thenReturn(List.of(event));

            var response = controller.getAuditTimeline(TARGET_TYPE, TARGET_ID);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getEventType())
                    .isEqualTo(ReviewAuditEventType.REVIEW_SUBMITTED);
        }

        @Test
        @DisplayName("getAuditTimeline returns empty for no events")
        void getAuditEmpty() {
            when(reviewApplicationService.getAuditTimeline(TARGET_TYPE, TARGET_ID))
                    .thenReturn(List.of());

            var response = controller.getAuditTimeline(TARGET_TYPE, TARGET_ID);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).isEmpty();
        }
    }

    // ── 3. Request Validation ───────────────────────────────────────

    @Nested
    @DisplayName("3. Request Validation")
    class RequestValidation {

        @Test
        @DisplayName("Missing targetType rejected safely")
        void missingTargetType() {
            AdminReviewActionRequest req = new AdminReviewActionRequest();
            req.setTargetId(TARGET_ID);
            req.setActorRole(ReviewActorRole.CREATOR);
            req.setActorReference(ACTOR_REF);

            assertThatThrownBy(() -> controller.submitForReview(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("targetType");
        }

        @Test
        @DisplayName("Null targetId rejected safely")
        void missingTargetId() {
            AdminReviewActionRequest req = new AdminReviewActionRequest();
            req.setTargetType(TARGET_TYPE);
            req.setActorRole(ReviewActorRole.CREATOR);
            req.setActorReference(ACTOR_REF);

            assertThatThrownBy(() -> controller.submitForReview(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("targetId");
        }

        @Test
        @DisplayName("Missing actorRole rejected safely")
        void missingActorRole() {
            AdminReviewActionRequest req = new AdminReviewActionRequest();
            req.setTargetType(TARGET_TYPE);
            req.setTargetId(TARGET_ID);
            req.setActorReference(ACTOR_REF);

            assertThatThrownBy(() -> controller.submitForReview(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("actorRole");
        }

        @Test
        @DisplayName("Blank actorReference rejected safely")
        void blankActorReference() {
            AdminReviewActionRequest req = new AdminReviewActionRequest();
            req.setTargetType(TARGET_TYPE);
            req.setTargetId(TARGET_ID);
            req.setActorRole(ReviewActorRole.CREATOR);
            req.setActorReference("   ");

            assertThatThrownBy(() -> controller.submitForReview(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("actorReference");
        }

        @Test
        @DisplayName("Note required for add-note action")
        void noteRequiredForAddNote() {
            AdminReviewActionRequest req = buildRequest(ReviewActionType.ADD_REVIEW_NOTE, ReviewActorRole.REVIEWER);
            req.setNote(null);

            var response = controller.addReviewNote(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Blank note for add-note returns bad request")
        void blankNoteForAddNote() {
            AdminReviewActionRequest req = buildRequest(ReviewActionType.ADD_REVIEW_NOTE, ReviewActorRole.REVIEWER);
            req.setNote("   ");

            var response = controller.addReviewNote(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── 4. Forbidden Endpoint/Wording Regression ────────────────────

    @Nested
    @DisplayName("4. Forbidden Endpoint/Wording Regression")
    class ForbiddenEndpoints {

        @Test
        @DisplayName("No endpoint path contains forbidden wording")
        void noForbiddenPaths() {
            Set<String> paths = collectEndpointPaths();
            for (String path : paths) {
                String lower = path.toLowerCase();
                assertThat(lower).doesNotContain("publish");
                assertThat(lower).doesNotContain("export");
                assertThat(lower).doesNotContain("completed");
                assertThat(lower).doesNotContain("ready-for-publish");
                assertThat(lower).doesNotContain("public-url");
                assertThat(lower).doesNotContain("signed-url");
                assertThat(lower).doesNotContain("cdn");
                assertThat(lower).doesNotContain("provider-url");
                assertThat(lower).doesNotContain("storage-key");
                assertThat(lower).doesNotContain("generic-status");
            }
        }

        @Test
        @DisplayName("Controller base path is /api/admin/reviews")
        void basePath() {
            RequestMapping rm = AdminReviewController.class.getAnnotation(RequestMapping.class);
            assertThat(rm).isNotNull();
            assertThat(rm.value()).contains("/api/admin/reviews");
        }

        @Test
        @DisplayName("Endpoint list matches expected safe actions")
        void expectedEndpoints() {
            Set<String> paths = collectEndpointPaths();
            assertThat(paths).contains("/submit", "/start", "/request-changes",
                    "/approve-internal", "/reject", "/return-to-draft", "/note");
        }

        @Test
        @DisplayName("Has state query endpoint")
        void hasStateQuery() {
            Set<String> getPaths = collectGetPaths();
            assertThat(getPaths).anyMatch(p -> p.contains("state"));
        }

        @Test
        @DisplayName("Has audit query endpoint")
        void hasAuditQuery() {
            Set<String> getPaths = collectGetPaths();
            assertThat(getPaths).anyMatch(p -> p.contains("audit"));
        }
    }

    // ── 5. Forbidden Status/Action Regression ───────────────────────

    @Nested
    @DisplayName("5. Forbidden Status/Action Regression")
    class ForbiddenStatusAction {

        @Test
        @DisplayName("ReviewActionType cannot represent forbidden actions")
        void forbiddenActions() {
            Set<String> names = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISH", "EXPORT", "MARK_COMPLETED",
                    "AUTO_APPROVE", "GENERIC_STATUS_UPDATE");
        }

        @Test
        @DisplayName("ReviewState cannot represent forbidden states")
        void forbiddenStates() {
            Set<String> names = Arrays.stream(ReviewState.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("READY_FOR_PUBLISH", "COMPLETED", "PUBLISHED", "EXPORTED");
        }
    }

    // ── 6. Safe Response Body ───────────────────────────────────────

    @Nested
    @DisplayName("6. Safe Response Body")
    class SafeResponseBody {

        private static final Set<String> FORBIDDEN_FIELDS = Set.of(
                "providerUrl", "storageKey", "bucketName", "bucketPath",
                "rawPayload", "rawProviderPayload", "rawStoragePayload",
                "base64", "secret", "apiKey", "config",
                "publicUrl", "signedUrl", "publishUrl", "exportUrl", "stackTrace");

        @Test
        @DisplayName("AdminReviewActionResponse has no forbidden fields")
        void responseSafe() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewActionResponse.class);
            for (String f : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Forbidden: " + f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("AdminReviewStateSummary has no forbidden fields")
        void summarySafe() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewStateSummary.class);
            for (String f : FORBIDDEN_FIELDS) {
                assertThat(fields).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("AdminReviewAuditTimelineItem has no forbidden fields")
        void timelineSafe() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewAuditTimelineItem.class);
            for (String f : FORBIDDEN_FIELDS) {
                assertThat(fields).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("approve-internal response contains no public/signed URL")
        void approveResponseNoUrl() {
            mockServiceResult(ReviewState.IN_REVIEW, ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED);
            var request = buildRequest(ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.REVIEWER);
            var response = controller.approveForInternalUse(request);

            AdminReviewActionResponse data = response.getBody().getData();
            String str = data.toString();
            assertThat(str).doesNotContain("publicUrl");
            assertThat(str).doesNotContain("signedUrl");
            assertThat(str).doesNotContain("https://");
        }
    }

    // ── 7. Safe Error Body ──────────────────────────────────────────

    @Nested
    @DisplayName("7. Safe Error Body")
    class SafeErrorBody {

        @Test
        @DisplayName("ReviewApplicationException error is safe")
        void applicationError() {
            ReviewApplicationException ex = new ReviewApplicationException(
                    "Review record not found", TARGET_TYPE, TARGET_ID);
            ResponseEntity<ApiResponse<Void>> response = controller.handleReviewApplicationException(ex);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getMessage()).doesNotContain("https://");
            assertThat(response.getBody().getMessage()).doesNotContain("secretKey");
            assertThat(response.getBody().getMessage()).doesNotContain("base64");
        }

        @Test
        @DisplayName("TransitionViolation error is safe")
        void transitionError() {
            ReviewTransitionViolationException ex = new ReviewTransitionViolationException(
                    "Cannot approve from DRAFT");
            ResponseEntity<ApiResponse<Void>> response = controller.handleTransitionViolation(ex);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getMessage()).doesNotContain("https://");
            assertThat(response.getBody().getMessage()).doesNotContain("bucket");
        }

        @Test
        @DisplayName("Validation error is safe")
        void validationError() {
            IllegalArgumentException ex = new IllegalArgumentException("targetType is required");
            ResponseEntity<ApiResponse<Void>> response = controller.handleValidation(ex);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage()).doesNotContain("https://");
        }

        @Test
        @DisplayName("Error response has no stack trace field")
        void noStackTrace() {
            Set<String> fields = getDeclaredFieldNames(ApiResponse.class);
            assertThat(fields).doesNotContain("stackTrace");
        }
    }

    // ── 8. Human Approval Guardrails through API ────────────────────

    @Nested
    @DisplayName("8. Human Approval Guardrails")
    class HumanApprovalGuardrails {

        @Test
        @DisplayName("CREATOR cannot approve via API — domain rejects")
        void creatorCannotApprove() {
            when(reviewApplicationService.approveForInternalUse(any(), any(), any(),
                    eq(ReviewActorRole.CREATOR), any(), any()))
                    .thenThrow(new ReviewTransitionViolationException("CREATOR cannot approve"));

            var request = buildRequest(ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.CREATOR);
            assertThatThrownBy(() -> controller.approveForInternalUse(request))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("SYSTEM cannot approve via API — domain rejects")
        void systemCannotApprove() {
            when(reviewApplicationService.approveForInternalUse(any(), any(), any(),
                    eq(ReviewActorRole.SYSTEM), any(), any()))
                    .thenThrow(new ReviewTransitionViolationException("SYSTEM cannot approve"));

            var request = buildRequest(ReviewActionType.APPROVE_FOR_INTERNAL_USE, ReviewActorRole.SYSTEM);
            assertThatThrownBy(() -> controller.approveForInternalUse(request))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE is internal only")
        void approvedInternalOnly() {
            assertThat(ReviewState.APPROVED_FOR_INTERNAL_USE.name()).doesNotContain("PUBLISH");
            assertThat(ReviewState.APPROVED_FOR_INTERNAL_USE.name()).doesNotContain("EXPORT");
        }

        @Test
        @DisplayName("approve-internal endpoint returns no public/signed URL")
        void approveNoUrl() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewActionResponse.class);
            assertThat(fields).doesNotContain("publicUrl");
            assertThat(fields).doesNotContain("signedUrl");
            assertThat(fields).doesNotContain("cdnUrl");
        }
    }

    // ── 9. Boundary Regression ──────────────────────────────────────

    @Nested
    @DisplayName("9. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("Controller is @RestController")
        void isRestController() {
            assertThat(AdminReviewController.class.isAnnotationPresent(RestController.class)).isTrue();
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
        @DisplayName("ReviewDomainService still has no Spring annotations")
        void domainServiceNoSpring() {
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isFalse();
        }

        @Test
        @DisplayName("ReviewApplicationService is @Service not @Controller")
        void appServiceIsService() {
            assertThat(ReviewApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isTrue();
            assertThat(ReviewApplicationService.class.isAnnotationPresent(
                    RestController.class)).isFalse();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private AdminReviewActionRequest buildRequest(ReviewActionType actionType, ReviewActorRole actorRole) {
        AdminReviewActionRequest req = new AdminReviewActionRequest();
        req.setTargetType(TARGET_TYPE);
        req.setTargetId(TARGET_ID);
        req.setActionType(actionType);
        req.setActorRole(actorRole);
        req.setActorReference(ACTOR_REF);
        return req;
    }

    private void mockServiceResult(ReviewState from, ReviewState to,
                                    ReviewActionType action, ReviewAuditEventType auditEvent) {
        ReviewApplicationResult result = ReviewApplicationResult.builder()
                .targetType(TARGET_TYPE).targetId(TARGET_ID)
                .previousState(from).currentState(to)
                .actionType(action).actorRole(ReviewActorRole.REVIEWER)
                .changed(true).auditEventType(auditEvent)
                .safeMessage("OK").occurredAt(LocalDateTime.now()).build();

        lenient().when(reviewApplicationService.submitForReview(any(), any(), any(), any(), any())).thenReturn(result);
        lenient().when(reviewApplicationService.startReview(any(), any(), any(), any(), any())).thenReturn(result);
        lenient().when(reviewApplicationService.requestChanges(any(), any(), any(), any(), any(), any())).thenReturn(result);
        lenient().when(reviewApplicationService.approveForInternalUse(any(), any(), any(), any(), any(), any())).thenReturn(result);
        lenient().when(reviewApplicationService.reject(any(), any(), any(), any(), any(), any())).thenReturn(result);
        lenient().when(reviewApplicationService.returnToDraft(any(), any(), any(), any(), any(), any())).thenReturn(result);
    }

    private static Set<String> collectEndpointPaths() {
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

    private static Set<String> collectGetPaths() {
        return Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(GetMapping.class))
                .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                .collect(Collectors.toSet());
    }

    private static Set<String> getDeclaredFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());
    }
}
