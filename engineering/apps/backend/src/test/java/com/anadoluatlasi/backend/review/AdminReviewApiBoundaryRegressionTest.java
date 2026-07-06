package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FP-012 Task 08 — Admin Review API Authorization Boundary, Observability & Regression Hardening.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FP-012 Task 08 — API Boundary, Observability & Regression Hardening")
class AdminReviewApiBoundaryRegressionTest {

    @Mock
    private ReviewApplicationService reviewApplicationService;
    @Mock
    private ReviewHandoffService reviewHandoffService;
    @Mock
    private AdminPreviewService adminPreviewService;

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "providerUrl", "storageKey", "bucketName", "bucketPath",
            "rawPayload", "rawProviderPayload", "rawStoragePayload",
            "base64", "secret", "apiKey", "config",
            "publicUrl", "signedUrl", "publishUrl", "exportUrl", "stackTrace"
    );

    private static final Set<String> FORBIDDEN_PATH_FRAGMENTS = Set.of(
            "publish", "export", "completed", "complete", "ready-for-publish",
            "public-url", "signed-url", "cdn", "provider-url", "storage-key",
            "generic-status", "config"
    );

    // ── 1. Endpoint Inventory Regression ────────────────────────────

    @Nested
    @DisplayName("1. Endpoint Inventory Regression")
    class EndpointInventory {

        @Test
        @DisplayName("Controller has exactly 14 mapped endpoints")
        void exactEndpointCount() {
            long postCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long getCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(postCount).isEqualTo(8);
            assertThat(getCount).isEqualTo(6);
        }

        @Test
        @DisplayName("POST endpoints are exactly the expected safe actions")
        void postEndpoints() {
            Set<String> postPaths = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(PostMapping.class).value()))
                    .collect(Collectors.toSet());
            assertThat(postPaths).containsExactlyInAnyOrder(
                    "/submit", "/start", "/request-changes",
                    "/approve-internal", "/reject", "/return-to-draft", "/note", "/handoff");
        }

        @Test
        @DisplayName("GET endpoints contain state, audit, worklist, context, actions, and preview")
        void getEndpoints() {
            Set<String> getPaths = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .collect(Collectors.toSet());
            assertThat(getPaths).anyMatch(p -> p.contains("state"));
            assertThat(getPaths).anyMatch(p -> p.contains("audit"));
            assertThat(getPaths).anyMatch(p -> p.contains("worklist"));
            assertThat(getPaths).anyMatch(p -> p.contains("context"));
            assertThat(getPaths).anyMatch(p -> p.contains("actions"));
            assertThat(getPaths).anyMatch(p -> p.contains("preview"));
        }

        @Test
        @DisplayName("No endpoint path contains forbidden fragments")
        void noForbiddenPaths() {
            Set<String> allPaths = collectAllEndpointPaths();
            for (String path : allPaths) {
                String lower = path.toLowerCase();
                for (String forbidden : FORBIDDEN_PATH_FRAGMENTS) {
                    assertThat(lower).as("Path '%s' contains forbidden '%s'", path, forbidden)
                            .doesNotContain(forbidden);
                }
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
        @DisplayName("Controller is @RestController")
        void isRestController() {
            assertThat(AdminReviewController.class.isAnnotationPresent(RestController.class)).isTrue();
        }
    }

    // ── 2. Request/Response Forbidden Field Regression ──────────────

    @Nested
    @DisplayName("2. DTO Forbidden Field Regression")
    class DtoForbiddenFields {

        @Test
        @DisplayName("AdminReviewActionRequest has no forbidden fields")
        void requestSafe() {
            assertNoForbiddenFields(AdminReviewActionRequest.class);
        }

        @Test
        @DisplayName("AdminReviewActionResponse has no forbidden fields")
        void responseSafe() {
            assertNoForbiddenFields(AdminReviewActionResponse.class);
        }

        @Test
        @DisplayName("AdminReviewStateSummary has no forbidden fields")
        void summarySafe() {
            assertNoForbiddenFields(AdminReviewStateSummary.class);
        }

        @Test
        @DisplayName("AdminReviewAuditTimelineItem has no forbidden fields")
        void timelineSafe() {
            assertNoForbiddenFields(AdminReviewAuditTimelineItem.class);
        }

        @Test
        @DisplayName("ReviewApplicationResult has no forbidden fields")
        void appResultSafe() {
            assertNoForbiddenFields(ReviewApplicationResult.class);
        }

        @Test
        @DisplayName("ReviewApplicationException has no forbidden fields")
        void appExceptionSafe() {
            assertNoForbiddenFields(ReviewApplicationException.class);
        }

        @Test
        @DisplayName("ReviewRecordEntity has no forbidden fields")
        void recordEntitySafe() {
            assertNoForbiddenFields(ReviewRecordEntity.class);
        }

        @Test
        @DisplayName("ReviewAuditEventEntity has no forbidden fields")
        void auditEntitySafe() {
            assertNoForbiddenFields(ReviewAuditEventEntity.class);
        }
    }

    // ── 3. Actor/Authorization Boundary Regression ──────────────────

    @Nested
    @DisplayName("3. Actor/Authorization Boundary Regression")
    class ActorAuthBoundary {

        @Test
        @DisplayName("CREATOR cannot approve — domain rejects")
        void creatorCannotApprove() {
            when(reviewApplicationService.approveForInternalUse(any(), any(), any(),
                    eq(ReviewActorRole.CREATOR), any(), any()))
                    .thenThrow(new ReviewTransitionViolationException("CREATOR cannot approve"));

            AdminReviewController ctrl = new AdminReviewController(reviewApplicationService, reviewHandoffService, adminPreviewService);
            AdminReviewActionRequest req = buildRequest(ReviewActorRole.CREATOR);
            assertThatThrownBy(() -> ctrl.approveForInternalUse(req))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("SYSTEM cannot approve — domain rejects")
        void systemCannotApprove() {
            when(reviewApplicationService.approveForInternalUse(any(), any(), any(),
                    eq(ReviewActorRole.SYSTEM), any(), any()))
                    .thenThrow(new ReviewTransitionViolationException("SYSTEM cannot approve"));

            AdminReviewController ctrl = new AdminReviewController(reviewApplicationService, reviewHandoffService, adminPreviewService);
            AdminReviewActionRequest req = buildRequest(ReviewActorRole.SYSTEM);
            assertThatThrownBy(() -> ctrl.approveForInternalUse(req))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("actorRole cannot create publish/export behavior")
        void noPublishExportBehavior() {
            Set<String> actionNames = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(actionNames).doesNotContain("PUBLISH", "EXPORT", "MARK_COMPLETED", "AUTO_APPROVE");
        }

        @Test
        @DisplayName("actorRole cannot bypass transition policy — tested via domain")
        void cannotBypassPolicy() {
            // Verify that ReviewTransitionPolicy is used by domain service
            // by attempting an invalid transition
            assertThatThrownBy(() ->
                    ReviewDomainService.processCommand(ReviewCommand.builder()
                            .currentState(ReviewState.DRAFT)
                            .actionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE)
                            .actorRole(ReviewActorRole.REVIEWER)
                            .actorReference("test")
                            .build()))
                    .isInstanceOf(ReviewTransitionViolationException.class);
        }

        @Test
        @DisplayName("AdminReviewActionRequest.actorRole is typed enum, not free string")
        void actorRoleIsEnum() throws NoSuchFieldException {
            Field actorRoleField = AdminReviewActionRequest.class.getDeclaredField("actorRole");
            assertThat(actorRoleField.getType()).isEqualTo(ReviewActorRole.class);
        }
    }

    // ── 4. Safe Error Regression ────────────────────────────────────

    @Nested
    @DisplayName("4. Safe Error Regression")
    class SafeErrorRegression {

        @Test
        @DisplayName("ReviewApplicationException message is safe")
        void appExceptionSafe() {
            ReviewApplicationException ex = new ReviewApplicationException(
                    "Review record not found for CONTENT_PROJECT " + UUID.randomUUID(),
                    ReviewTargetType.CONTENT_PROJECT, UUID.randomUUID());
            assertSafeMessage(ex.getMessage());
        }

        @Test
        @DisplayName("ReviewTransitionViolationException message is safe")
        void transitionExceptionSafe() {
            ReviewTransitionViolationException ex = new ReviewTransitionViolationException(
                    "Cannot transition from DRAFT to APPROVED_FOR_INTERNAL_USE via APPROVE_FOR_INTERNAL_USE");
            assertSafeMessage(ex.getMessage());
        }

        @Test
        @DisplayName("Controller error handler for ApplicationException returns safe body")
        void controllerAppErrorSafe() {
            AdminReviewController ctrl = new AdminReviewController(reviewApplicationService, reviewHandoffService, adminPreviewService);
            var resp = ctrl.handleReviewApplicationException(
                    new ReviewApplicationException("Not found"));
            assertThat(resp.getBody().isSuccess()).isFalse();
            assertSafeMessage(resp.getBody().getMessage());
        }

        @Test
        @DisplayName("Controller error handler for TransitionViolation returns safe body")
        void controllerTransitionErrorSafe() {
            AdminReviewController ctrl = new AdminReviewController(reviewApplicationService, reviewHandoffService, adminPreviewService);
            var resp = ctrl.handleTransitionViolation(
                    new ReviewTransitionViolationException("Invalid transition"));
            assertThat(resp.getBody().isSuccess()).isFalse();
            assertSafeMessage(resp.getBody().getMessage());
        }

        @Test
        @DisplayName("Controller error handler for validation returns safe body")
        void controllerValidationErrorSafe() {
            AdminReviewController ctrl = new AdminReviewController(reviewApplicationService, reviewHandoffService, adminPreviewService);
            var resp = ctrl.handleValidation(new IllegalArgumentException("targetType is required"));
            assertThat(resp.getBody().isSuccess()).isFalse();
            assertSafeMessage(resp.getBody().getMessage());
        }
    }

    // ── 5. Observability Documentation Regression ───────────────────

    @Nested
    @DisplayName("5. Observability Documentation Regression")
    class ObservabilityDocRegression {

        private static final Path DOC_PATH = Path.of(
                "../../../docs/05-engineering/79-fp012-admin-review-api-authorization-observability-hardening.md");

        @Test
        @DisplayName("Observability hardening doc exists")
        void docExists() {
            assertThat(DOC_PATH.toFile()).exists();
        }

        @Test
        @DisplayName("Doc contains safe metric names")
        void safeMetrics() throws IOException {
            String content = Files.readString(DOC_PATH);
            assertThat(content).contains("admin_review_action_total");
            assertThat(content).contains("admin_review_transition_violation_total");
        }

        @Test
        @DisplayName("Doc contains allowed labels")
        void allowedLabels() throws IOException {
            String content = Files.readString(DOC_PATH);
            assertThat(content).contains("action_type");
            assertThat(content).contains("target_type");
            assertThat(content).contains("actor_role");
        }

        @Test
        @DisplayName("Doc contains forbidden labels section")
        void forbiddenLabels() throws IOException {
            String content = Files.readString(DOC_PATH);
            assertThat(content).contains("Forbidden Label");
            assertThat(content).contains("actor_reference");
            assertThat(content).contains("target_id");
        }

        @Test
        @DisplayName("Doc states actorRole is not authentication")
        void actorRoleNotAuth() throws IOException {
            String content = Files.readString(DOC_PATH);
            assertThat(content).containsIgnoringCase("not a replacement for real authentication");
        }

        @Test
        @DisplayName("Doc states production route protection required")
        void routeProtection() throws IOException {
            String content = Files.readString(DOC_PATH);
            assertThat(content).containsIgnoringCase("protect admin routes");
        }

        @Test
        @DisplayName("Doc contains no publish/export as enabled behavior")
        void noPublishExport() throws IOException {
            String content = Files.readString(DOC_PATH);
            // These should not appear as "enabled" endpoints
            assertThat(content).doesNotContain("POST /api/admin/reviews/publish");
            assertThat(content).doesNotContain("POST /api/admin/reviews/export");
        }
    }

    // ── 6. Forbidden Status/Action Regression ───────────────────────

    @Nested
    @DisplayName("6. Forbidden Status/Action Regression")
    class ForbiddenStatusAction {

        @Test
        @DisplayName("ReviewState has no forbidden states")
        void noForbiddenStates() {
            Set<String> names = enumNames(ReviewState.class);
            assertThat(names).doesNotContain("READY_FOR_PUBLISH", "COMPLETED", "PUBLISHED", "EXPORTED");
        }

        @Test
        @DisplayName("ReviewActionType has no forbidden actions")
        void noForbiddenActions() {
            Set<String> names = enumNames(ReviewActionType.class);
            assertThat(names).doesNotContain("PUBLISH", "EXPORT", "MARK_COMPLETED",
                    "AUTO_APPROVE", "GENERIC_STATUS_UPDATE");
        }

        @Test
        @DisplayName("ReviewAuditEventType has no forbidden events")
        void noForbiddenEvents() {
            Set<String> names = enumNames(ReviewAuditEventType.class);
            assertThat(names).doesNotContain("PUBLISHED", "EXPORTED", "COMPLETED",
                    "READY_FOR_PUBLISH", "PUBLIC_URL_CREATED", "SIGNED_URL_CREATED", "AUTO_APPROVED");
        }

        @Test
        @DisplayName("ReviewTargetType has no forbidden targets")
        void noForbiddenTargets() {
            Set<String> names = enumNames(ReviewTargetType.class);
            assertThat(names).doesNotContain("PUBLIC_MEDIA", "CDN_ASSET", "EXPORT_FILE", "PUBLISHED_CONTENT");
        }

        @Test
        @DisplayName("No enum value across all review enums contains PUBLISH/EXPORT/CDN/COMPLETED")
        void crossEnumRegression() {
            Set<String> allNames = new java.util.HashSet<>();
            allNames.addAll(enumNames(ReviewState.class));
            allNames.addAll(enumNames(ReviewActionType.class));
            allNames.addAll(enumNames(ReviewAuditEventType.class));
            allNames.addAll(enumNames(ReviewActorRole.class));
            allNames.addAll(enumNames(ReviewTargetType.class));
            for (String name : allNames) {
                assertThat(name).doesNotContain("PUBLISH");
                assertThat(name).doesNotContain("EXPORT");
                assertThat(name).doesNotContain("CDN");
                assertThat(name).doesNotContain("COMPLETED");
            }
        }
    }

    // ── 7. Boundary Regression ──────────────────────────────────────

    @Nested
    @DisplayName("7. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("ReviewDomainService has no Spring annotations")
        void domainServiceNoSpring() {
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isFalse();
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.stereotype.Component.class)).isFalse();
        }

        @Test
        @DisplayName("ReviewApplicationService is @Service not @Controller")
        void appServiceAnnotation() {
            assertThat(ReviewApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isTrue();
            assertThat(ReviewApplicationService.class.isAnnotationPresent(
                    RestController.class)).isFalse();
        }

        @Test
        @DisplayName("Only one @RestController in review package")
        void singleController() {
            Class<?>[] reviewClasses = {
                    ReviewState.class, ReviewActionType.class, ReviewActorRole.class,
                    ReviewTransitionPolicy.class, ReviewTransitionViolationException.class,
                    ReviewAuditEventType.class, ReviewAuditRecord.class, ReviewTextSafety.class,
                    ReviewCommand.class, ReviewDecisionResult.class,
                    ReviewAuditEventFactory.class, ReviewDomainService.class,
                    ReviewTargetType.class, ReviewRecordEntity.class, ReviewAuditEventEntity.class,
                    ReviewRecordRepository.class, ReviewAuditEventRepository.class,
                    ReviewPersistenceMapper.class,
                    ReviewApplicationService.class, ReviewApplicationResult.class,
                    ReviewApplicationException.class,
                    AdminReviewActionRequest.class, AdminReviewActionResponse.class,
                    AdminReviewStateSummary.class, AdminReviewAuditTimelineItem.class,
                    AdminReviewDtoMapper.class,
                    AdminReviewController.class
            };
            long controllerCount = Arrays.stream(reviewClasses)
                    .filter(c -> c.isAnnotationPresent(RestController.class))
                    .count();
            assertThat(controllerCount).isEqualTo(1);
        }

        @Test
        @DisplayName("No DTO has @RestController")
        void noDtoIsController() {
            Class<?>[] dtoClasses = {
                    AdminReviewActionRequest.class, AdminReviewActionResponse.class,
                    AdminReviewStateSummary.class, AdminReviewAuditTimelineItem.class,
                    ReviewApplicationResult.class, ReviewApplicationException.class
            };
            for (Class<?> clazz : dtoClasses) {
                assertThat(clazz.isAnnotationPresent(RestController.class))
                        .as(clazz.getSimpleName()).isFalse();
            }
        }

        @Test
        @DisplayName("No mapper/service/repo has @RestController")
        void noMapperIsController() {
            Class<?>[] classes = {
                    AdminReviewDtoMapper.class, ReviewPersistenceMapper.class,
                    ReviewDomainService.class, ReviewApplicationService.class
            };
            for (Class<?> clazz : classes) {
                assertThat(clazz.isAnnotationPresent(RestController.class))
                        .as(clazz.getSimpleName()).isFalse();
            }
        }

        @Test
        @DisplayName("V10 migration exists and no V11 migration added")
        void migrationRegression() {
            assertThat(Path.of("src/main/resources/db/migration/V10__fp012_review_workflow_tables.sql").toFile())
                    .exists();
            assertThat(Path.of("src/main/resources/db/migration/V11__fp012_admin_review_hardening.sql").toFile())
                    .doesNotExist();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private AdminReviewActionRequest buildRequest(ReviewActorRole role) {
        AdminReviewActionRequest req = new AdminReviewActionRequest();
        req.setTargetType(ReviewTargetType.CONTENT_PROJECT);
        req.setTargetId(UUID.randomUUID());
        req.setActionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE);
        req.setActorRole(role);
        req.setActorReference("test-actor");
        return req;
    }

    private void assertNoForbiddenFields(Class<?> clazz) {
        Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());
        for (String f : FORBIDDEN_FIELDS) {
            assertThat(fields).as(clazz.getSimpleName() + " forbidden: " + f).doesNotContain(f);
        }
    }

    private void assertSafeMessage(String message) {
        assertThat(message).doesNotContain("https://");
        assertThat(message).doesNotContain("secretKey");
        assertThat(message).doesNotContain("accessKey");
        assertThat(message).doesNotContain("bucket");
        assertThat(message).doesNotContain("base64");
        assertThat(message).doesNotContain("rawPayload");
        assertThat(message).doesNotContain("apiKey");
        assertThat(message).doesNotContain("stackTrace");
    }

    private static <E extends Enum<E>> Set<String> enumNames(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name).collect(Collectors.toSet());
    }

    private static Set<String> collectAllEndpointPaths() {
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
}
