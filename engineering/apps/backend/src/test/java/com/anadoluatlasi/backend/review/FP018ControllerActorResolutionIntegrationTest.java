package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-018 Task 03: Review Action Controller Actor Resolution Integration tests.
 *
 * <p>Proves review actions route through AdminActorResolver,
 * actorRole is TEST_FALLBACK only, SYSTEM blocked for review actions,
 * endpoint inventory unchanged, and safety baseline preserved.</p>
 */
@DisplayName("FP-018 Task 03 — Controller Actor Resolution Integration")
class FP018ControllerActorResolutionIntegrationTest {

    // ── A. Controller has AdminActorResolver dependency ──────

    @Nested
    @DisplayName("A. Controller Dependencies")
    class ControllerDependencies {

        @Test
        @DisplayName("AdminReviewController has AdminActorResolver field")
        void hasResolver() {
            Set<Class<?>> fieldTypes = Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet());
            assertThat(fieldTypes).contains(AdminActorResolver.class);
        }

        @Test
        @DisplayName("AdminReviewController has ReviewApplicationService field")
        void hasReviewApp() {
            Set<Class<?>> fieldTypes = Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet());
            assertThat(fieldTypes).contains(ReviewApplicationService.class);
        }

        @Test
        @DisplayName("AdminReviewController has ReviewHandoffService field")
        void hasHandoff() {
            Set<Class<?>> fieldTypes = Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet());
            assertThat(fieldTypes).contains(ReviewHandoffService.class);
        }

        @Test
        @DisplayName("AdminReviewController has InternalRevisionRequestService field")
        void hasRevision() {
            Set<Class<?>> fieldTypes = Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet());
            assertThat(fieldTypes).contains(InternalRevisionRequestService.class);
        }

        @Test
        @DisplayName("total 6 service dependencies")
        void sixDeps() {
            long nonSyntheticFields = Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .filter(f -> !f.isSynthetic()).count();
            assertThat(nonSyntheticFields).isEqualTo(6);
        }
    }

    // ── B. Resolver Behavior ──────────────────────────────────

    @Nested
    @DisplayName("B. Resolver Behavior for Review Actions")
    class ResolverBehavior {

        private final AdminActorResolver resolver = new AdminActorResolver();

        @Test
        @DisplayName("REVIEWER resolves as TEST_FALLBACK")
        void reviewerResolves() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "reviewer-1");
            assertThat(actor.getActorRole()).isEqualTo(ReviewActorRole.REVIEWER);
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.TEST_FALLBACK);
        }

        @Test
        @DisplayName("ADMIN resolves as TEST_FALLBACK")
        void adminResolves() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin-1");
            assertThat(actor.getActorRole()).isEqualTo(ReviewActorRole.ADMIN);
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.TEST_FALLBACK);
        }

        @Test
        @DisplayName("CREATOR resolves but cannot perform review")
        void creatorResolves() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator-1");
            assertThat(actor.getActorRole()).isEqualTo(ReviewActorRole.CREATOR);
            assertThat(actor.getActorRole().canPerformReview()).isFalse();
        }

        @Test
        @DisplayName("SYSTEM blocked when systemInternalAllowed=false (review path)")
        void systemBlockedForReview() {
            var req = AdminActorResolutionRequest.builder()
                    .requestedActorRole(ReviewActorRole.SYSTEM)
                    .requestedActorReference("system")
                    .fallbackAllowed(true)
                    .systemInternalAllowed(false)
                    .build();
            try {
                resolver.resolve(req);
                assertThat(true).as("Should have thrown").isFalse();
            } catch (AdminActorResolutionException e) {
                assertThat(e.getSafeErrorCode()).isEqualTo("SYSTEM_NOT_ALLOWED");
            }
        }

        @Test
        @DisplayName("SYSTEM allowed for handoff when systemInternalAllowed=true")
        void systemAllowedForHandoff() {
            var req = AdminActorResolutionRequest.builder()
                    .requestedActorRole(ReviewActorRole.SYSTEM)
                    .requestedActorReference("system-handoff")
                    .fallbackAllowed(true)
                    .systemInternalAllowed(true)
                    .build();
            var actor = resolver.resolve(req);
            assertThat(actor.getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.SYSTEM_INTERNAL);
        }

        @Test
        @DisplayName("resolved actor reference is sanitized")
        void referenceSanitized() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "Bearer eyJhbGci...");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
            assertThat(actor.getActorReference()).doesNotContain("eyJhbGci");
        }

        @Test
        @DisplayName("resolved actor reference: password stripped")
        void passwordStripped() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "password=s3cret");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
        }

        @Test
        @DisplayName("empty reference uses role as fallback")
        void emptyRefFallback() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "");
            assertThat(actor.getActorReference()).isEqualTo("REVIEWER");
        }
    }

    // ── C. Exception Safety ──────────────────────────────────

    @Nested
    @DisplayName("C. Exception Safety")
    class ExceptionSafety {

        @Test
        @DisplayName("AdminActorResolutionException handler exists in controller")
        void handlerExists() {
            boolean found = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .anyMatch(m -> Arrays.stream(m.getParameterTypes())
                            .anyMatch(p -> p == AdminActorResolutionException.class));
            assertThat(found).isTrue();
        }

        @Test
        @DisplayName("exception toString is safe")
        void exceptionToStringSafe() {
            var ex = new AdminActorResolutionException("msg", "CODE", ReviewActorRole.ADMIN);
            String str = ex.toString();
            List.of("token", "jwt", "session", "credential", "password",
                    "stackTrace", "exceptionClass")
                    .forEach(f -> assertThat(str).doesNotContain(f));
        }

        @Test
        @DisplayName("exception message does not leak auth details")
        void exceptionMessageSafe() {
            var resolver = new AdminActorResolver();
            try {
                resolver.resolve(AdminActorResolutionRequest.builder()
                        .requestedActorRole(ReviewActorRole.SYSTEM)
                        .requestedActorReference("Bearer secret_token_here")
                        .fallbackAllowed(true)
                        .systemInternalAllowed(false)
                        .build());
            } catch (AdminActorResolutionException e) {
                assertThat(e.getMessage()).doesNotContain("secret_token_here");
                assertThat(e.getMessage()).doesNotContain("Bearer");
            }
        }
    }

    // ── D. Endpoint Inventory ────────────────────────────────

    @Nested
    @DisplayName("D. Endpoint Inventory (unchanged: 21 = 9 POST + 12 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("AdminReviewController: 9 POST")
        void reviewPost() {
            assertThat(countPost(AdminReviewController.class)).isEqualTo(9);
        }

        @Test
        @DisplayName("AdminReviewController: 8 GET")
        void reviewGet() {
            assertThat(countGet(AdminReviewController.class)).isEqualTo(8);
        }

        @Test
        @DisplayName("AdminPipelineController: 0 POST + 2 GET")
        void pipeline() {
            assertThat(countPost(AdminPipelineController.class)).isZero();
            assertThat(countGet(AdminPipelineController.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("AdminWorkflowObservabilityController: 0 POST + 2 GET")
        void observability() {
            assertThat(countPost(AdminWorkflowObservabilityController.class)).isZero();
            assertThat(countGet(AdminWorkflowObservabilityController.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("total 21 = 9 POST + 12 GET")
        void total() {
            long post = countPost(AdminReviewController.class)
                    + countPost(AdminPipelineController.class)
                    + countPost(AdminWorkflowObservabilityController.class);
            long get = countGet(AdminReviewController.class)
                    + countGet(AdminPipelineController.class)
                    + countGet(AdminWorkflowObservabilityController.class);
            assertThat(post).isEqualTo(9);
            assertThat(get).isEqualTo(12);
            assertThat(post + get).isEqualTo(21);
        }

        @Test
        @DisplayName("no PUT endpoints")
        void noPut() {
            for (Class<?> c : List.of(AdminReviewController.class,
                    AdminPipelineController.class,
                    AdminWorkflowObservabilityController.class)) {
                assertThat(Arrays.stream(c.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(PutMapping.class)).count())
                        .as("PUT in %s", c.getSimpleName()).isZero();
            }
        }

        @Test
        @DisplayName("no DELETE endpoints")
        void noDelete() {
            for (Class<?> c : List.of(AdminReviewController.class,
                    AdminPipelineController.class,
                    AdminWorkflowObservabilityController.class)) {
                assertThat(Arrays.stream(c.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(DeleteMapping.class)).count())
                        .as("DELETE in %s", c.getSimpleName()).isZero();
            }
        }

        private long countPost(Class<?> c) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
        }

        private long countGet(Class<?> c) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
        }
    }

    // ── E. Mapper Overload ──────────────────────────────────

    @Nested
    @DisplayName("E. Mapper Resolved Actor Overload")
    class MapperOverload {

        @Test
        @DisplayName("toHandoffRequest overload with resolved actor exists")
        void overloadExists() {
            boolean found = Arrays.stream(AdminReviewDtoMapper.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals("toHandoffRequest"))
                    .anyMatch(m -> m.getParameterCount() == 3);
            assertThat(found).isTrue();
        }

        @Test
        @DisplayName("original toHandoffRequest still exists for backward compat")
        void originalExists() {
            boolean found = Arrays.stream(AdminReviewDtoMapper.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals("toHandoffRequest"))
                    .anyMatch(m -> m.getParameterCount() == 1);
            assertThat(found).isTrue();
        }
    }

    // ── F. No Forbidden Field/String Exposure ────────────────

    @Nested
    @DisplayName("F. No Forbidden Exposure")
    class NoForbidden {

        private static final List<String> FORBIDDEN_FIELDS = List.of(
                "token", "jwt", "session", "credential", "password",
                "secret", "apiKey", "accessKey", "principalRaw",
                "rawPrincipal", "authorizationHeader");

        @Test
        @DisplayName("ResolvedAdminActor no forbidden fields")
        void resolvedActorClean() {
            Set<String> fields = fieldNames(ResolvedAdminActor.class);
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("AdminActorResolutionRequest no forbidden fields")
        void requestClean() {
            Set<String> fields = fieldNames(AdminActorResolutionRequest.class);
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("AdminActorResolutionException no forbidden fields")
        void exceptionClean() {
            Set<String> fields = fieldNames(AdminActorResolutionException.class);
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("AdminReviewActionRequest no forbidden fields")
        void actionRequestClean() {
            Set<String> fields = fieldNames(AdminReviewActionRequest.class);
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("AdminReviewHandoffRequest no forbidden fields")
        void handoffRequestClean() {
            Set<String> fields = fieldNames(AdminReviewHandoffRequest.class);
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("resolved actor toString safe")
        void toStringSafe() {
            var resolver = new AdminActorResolver();
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin-1");
            String str = actor.toString();
            FORBIDDEN_FIELDS.forEach(f -> assertThat(str).doesNotContain(f));
        }

        private Set<String> fieldNames(Class<?> c) {
            return Arrays.stream(c.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
        }
    }

    // ── G. Boundary Policy Integration ──────────────────────

    @Nested
    @DisplayName("G. Boundary Policy Integration")
    class BoundaryIntegration {

        private final AdminActorResolver resolver = new AdminActorResolver();

        @Test
        @DisplayName("resolved REVIEWER allowed for REVIEW_ACTION")
        void reviewerReviewAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "rev");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor)).isTrue();
        }

        @Test
        @DisplayName("resolved ADMIN allowed for REVIEW_ACTION")
        void adminReviewAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor)).isTrue();
        }

        @Test
        @DisplayName("resolved CREATOR not allowed for REVIEW_ACTION")
        void creatorReviewNotAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor)).isFalse();
        }

        @Test
        @DisplayName("resolved SYSTEM allowed for INTERNAL_HANDOFF")
        void systemHandoffAllowed() {
            var actor = resolver.resolveSystemInternal("HANDOFF");
            assertThat(AdminActorBoundaryPolicy.isAllowed(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF, actor)).isTrue();
        }

        @Test
        @DisplayName("SYSTEM cannot approve")
        void systemCannotApprove() {
            assertThat(AdminActorBoundaryPolicy.canSystemApprove()).isFalse();
        }

        @Test
        @DisplayName("CREATOR cannot approve")
        void creatorCannotApprove() {
            assertThat(AdminActorBoundaryPolicy.canCreatorApprove()).isFalse();
        }
    }

    // ── H. No Forbidden Enum Values ─────────────────────────

    @Nested
    @DisplayName("H. No Forbidden Enum Values")
    class ForbiddenEnums {

        @Test
        @DisplayName("resolution mode no CLIENT_TRUSTED/BYPASS_AUTH/FORCE_ADMIN")
        void modeClean() {
            Set<String> names = Arrays.stream(AdminActorResolutionMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("CLIENT_TRUSTED", "REQUEST_ROLE_TRUSTED", "BYPASS_AUTH", "FORCE_ADMIN")
                    .forEach(f -> assertThat(names).doesNotContain(f));
        }

        @Test
        @DisplayName("review action type no PUBLISH/EXPORT/AUTO_APPROVE/GENERIC_STATUS_UPDATE")
        void actionTypeClean() {
            Set<String> names = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("PUBLISH", "EXPORT", "AUTO_APPROVE", "GENERIC_STATUS_UPDATE",
                    "MARK_COMPLETED", "READY_FOR_PUBLISH")
                    .forEach(f -> assertThat(names).doesNotContain(f));
        }
    }
}
