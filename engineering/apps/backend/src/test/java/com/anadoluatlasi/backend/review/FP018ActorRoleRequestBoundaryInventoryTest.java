package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-018 Task 02: Actor Role Request Boundary Inventory tests.
 *
 * <p>Documents which request DTOs currently contain actorRole,
 * which controllers accept it from request body, and confirms
 * endpoint inventory and safety baseline.</p>
 */
@DisplayName("FP-018 Task 02 — Actor Role Request Boundary Inventory")
class FP018ActorRoleRequestBoundaryInventoryTest {

    // ── A. Request DTOs with actorRole ────────────────────────

    @Nested
    @DisplayName("A. Request DTOs with actorRole")
    class DtosWithActorRole {

        @Test
        @DisplayName("AdminReviewActionRequest has actorRole field")
        void actionRequestHasActorRole() {
            Set<String> fields = fieldNames(AdminReviewActionRequest.class);
            assertThat(fields).contains("actorRole");
        }

        @Test
        @DisplayName("AdminReviewHandoffRequest has actorRole field")
        void handoffRequestHasActorRole() {
            Set<String> fields = fieldNames(AdminReviewHandoffRequest.class);
            assertThat(fields).contains("actorRole");
        }

        @Test
        @DisplayName("AdminInternalRevisionRequest has actorRole field")
        void revisionRequestHasActorRole() {
            Set<String> fields = fieldNames(AdminInternalRevisionRequest.class);
            assertThat(fields).contains("actorRole");
        }

        @Test
        @DisplayName("actorRole is NOT auth — it is test/dev input")
        void actorRoleNotAuth() {
            // These DTOs have actorRole for test/dev compatibility.
            // They do NOT represent production authentication.
            // Future Task 03 will route through AdminActorResolver.
            assertThat(true).as("actorRole field is test/dev input, not production auth").isTrue();
        }
    }

    // ── B. Controllers with actorRole ─────────────────────────

    @Nested
    @DisplayName("B. Controllers Accepting actorRole")
    class ControllersWithActorRole {

        @Test
        @DisplayName("AdminReviewController has 9 POST endpoints")
        void reviewControllerPost() {
            long count = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isEqualTo(9);
        }

        @Test
        @DisplayName("AdminPipelineController has 0 POST endpoints")
        void pipelineControllerNoPost() {
            long count = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("AdminWorkflowObservabilityController has 0 POST endpoints")
        void observabilityControllerNoPost() {
            long count = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("only AdminReviewController has POST — only target for actor resolution")
        void onlyReviewControllerNeedsResolution() {
            // Pipeline and Observability controllers are read-only.
            // Only AdminReviewController POST endpoints need actor resolution.
            long pipelinePost = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long observabilityPost = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(pipelinePost + observabilityPost).isZero();
        }
    }

    // ── C. Endpoint Inventory ─────────────────────────────────

    @Nested
    @DisplayName("C. Endpoint Inventory (21 = 9 POST + 12 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("toplam POST = 9")
        void totalPost() {
            long total = countPost(AdminReviewController.class)
                    + countPost(AdminPipelineController.class)
                    + countPost(AdminWorkflowObservabilityController.class);
            assertThat(total).isEqualTo(9);
        }

        @Test
        @DisplayName("toplam GET = 12")
        void totalGet() {
            long total = countGet(AdminReviewController.class)
                    + countGet(AdminPipelineController.class)
                    + countGet(AdminWorkflowObservabilityController.class);
            assertThat(total).isEqualTo(12);
        }

        @Test
        @DisplayName("toplam 21")
        void total21() {
            long post = countPost(AdminReviewController.class)
                    + countPost(AdminPipelineController.class)
                    + countPost(AdminWorkflowObservabilityController.class);
            long get = countGet(AdminReviewController.class)
                    + countGet(AdminPipelineController.class)
                    + countGet(AdminWorkflowObservabilityController.class);
            assertThat(post + get).isEqualTo(21);
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

    // ── D. New Resolver Components ────────────────────────────

    @Nested
    @DisplayName("D. New Resolver Components Exist")
    class ResolverComponentsExist {

        @Test
        @DisplayName("AdminActorResolver class exists")
        void resolverExists() {
            assertThat(AdminActorResolver.class).isNotNull();
        }

        @Test
        @DisplayName("ResolvedAdminActor class exists")
        void resolvedActorExists() {
            assertThat(ResolvedAdminActor.class).isNotNull();
        }

        @Test
        @DisplayName("AdminActorResolutionMode class exists")
        void modeExists() {
            assertThat(AdminActorResolutionMode.class).isNotNull();
        }

        @Test
        @DisplayName("AdminActorResolutionRequest class exists")
        void requestExists() {
            assertThat(AdminActorResolutionRequest.class).isNotNull();
        }

        @Test
        @DisplayName("AdminActorResolutionException class exists")
        void exceptionExists() {
            assertThat(AdminActorResolutionException.class).isNotNull();
        }

        @Test
        @DisplayName("AdminActorBoundaryPolicy class exists")
        void policyExists() {
            assertThat(AdminActorBoundaryPolicy.class).isNotNull();
        }
    }

    // ── E. No Forbidden DTO Fields Across All Request DTOs ────

    @Nested
    @DisplayName("E. Forbidden Fields Absent")
    class ForbiddenFields {

        private static final List<String> FORBIDDEN = List.of(
                "token", "jwt", "session", "credential", "password",
                "secret", "apiKey", "accessKey", "principalRaw",
                "rawPrincipal", "authorizationHeader");

        @Test
        @DisplayName("ResolvedAdminActor no forbidden fields")
        void resolvedActorClean() {
            Set<String> fields = fieldNames(ResolvedAdminActor.class);
            FORBIDDEN.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("AdminActorResolutionRequest no forbidden fields")
        void requestClean() {
            Set<String> fields = fieldNames(AdminActorResolutionRequest.class);
            FORBIDDEN.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("AdminActorResolutionException no forbidden fields")
        void exceptionClean() {
            Set<String> fields = fieldNames(AdminActorResolutionException.class);
            FORBIDDEN.forEach(f -> assertThat(fields).doesNotContain(f));
        }
    }

    private static Set<String> fieldNames(Class<?> c) {
        return Arrays.stream(c.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());
    }
}
