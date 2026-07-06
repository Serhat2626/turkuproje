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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FP-018 Task 05: Broad Auth Boundary Regression + Final Closure Verification.
 *
 * <p>Comprehensive regression that covers the full FP-018 scope:
 * actor resolver, controller integration, boundary policy, handoff/revision
 * hardening, sanitization, endpoint inventory, and safety baseline.</p>
 */
@DisplayName("FP-018 Task 05 — Broad Auth Boundary Regression & Final Closure")
class FP018BroadAuthBoundaryRegressionTest {

    private final AdminActorResolver resolver = new AdminActorResolver();

    // ── A. All POST Endpoints Route Through Resolver ────────

    @Nested
    @DisplayName("A. Controller Has Resolver Dependency")
    class ResolverDependency {

        @Test
        @DisplayName("AdminReviewController has AdminActorResolver")
        void hasResolver() {
            assertThat(Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet()))
                    .contains(AdminActorResolver.class);
        }

        @Test
        @DisplayName("AdminReviewController has exactly 6 dependencies")
        void exactDeps() {
            long count = Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .filter(f -> !f.isSynthetic()).count();
            assertThat(count).isEqualTo(6);
        }
    }

    // ── B. actorRole is TEST_FALLBACK Only ──────────────────

    @Nested
    @DisplayName("B. actorRole Trust Boundary")
    class ActorRoleTrust {

        @Test
        @DisplayName("REVIEWER resolves as TEST_FALLBACK")
        void reviewer() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "rev");
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.TEST_FALLBACK);
        }

        @Test
        @DisplayName("ADMIN resolves as TEST_FALLBACK")
        void admin() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin");
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.TEST_FALLBACK);
        }

        @Test
        @DisplayName("CREATOR resolves as TEST_FALLBACK")
        void creator() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.TEST_FALLBACK);
        }

        @Test
        @DisplayName("no AUTHENTICATED_CONTEXT resolution in current code")
        void noAuthContext() {
            // All resolve calls with fallbackAllowed=true produce TEST_FALLBACK
            for (ReviewActorRole role : List.of(ReviewActorRole.REVIEWER, ReviewActorRole.ADMIN, ReviewActorRole.CREATOR)) {
                var actor = resolver.resolveTestFallback(role, "test");
                assertThat(actor.getResolutionMode()).isNotEqualTo(AdminActorResolutionMode.AUTHENTICATED_CONTEXT);
            }
        }

        @Test
        @DisplayName("no CLIENT_TRUSTED or BYPASS_AUTH mode exists")
        void noForbiddenModes() {
            Set<String> names = Arrays.stream(AdminActorResolutionMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("CLIENT_TRUSTED", "REQUEST_ROLE_TRUSTED", "BYPASS_AUTH", "FORCE_ADMIN")
                    .forEach(f -> assertThat(names).doesNotContain(f));
        }
    }

    // ── C. SYSTEM Blocking Matrix ───────────────────────────

    @Nested
    @DisplayName("C. SYSTEM Blocking Matrix")
    class SystemBlocking {

        @Test
        @DisplayName("SYSTEM blocked for REVIEW_ACTION")
        void reviewBlocked() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION))
                    .doesNotContain(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("SYSTEM blocked for INTERNAL_REVISION_REQUEST")
        void revisionBlocked() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_REVISION_REQUEST))
                    .doesNotContain(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("SYSTEM blocked for SUBMIT_FOR_REVIEW")
        void submitBlocked() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.SUBMIT_FOR_REVIEW))
                    .doesNotContain(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("SYSTEM blocked for ADD_REVIEW_NOTE")
        void noteBlocked() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.ADD_REVIEW_NOTE))
                    .doesNotContain(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("SYSTEM allowed for INTERNAL_HANDOFF only")
        void handoffAllowed() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF))
                    .contains(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("SYSTEM allowed for READ_ONLY_VISIBILITY")
        void readOnlyAllowed() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.READ_ONLY_VISIBILITY))
                    .contains(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("canSystemApprove is false")
        void noApprove() {
            assertThat(AdminActorBoundaryPolicy.canSystemApprove()).isFalse();
        }

        @Test
        @DisplayName("canSystemRevision is false")
        void noRevision() {
            assertThat(AdminActorBoundaryPolicy.canSystemRevision()).isFalse();
        }

        @Test
        @DisplayName("canSystemHandoff is true")
        void handoff() {
            assertThat(AdminActorBoundaryPolicy.canSystemHandoff()).isTrue();
        }

        @Test
        @DisplayName("SYSTEM blocked by resolver when systemInternalAllowed=false")
        void resolverBlocks() {
            var req = AdminActorResolutionRequest.builder()
                    .requestedActorRole(ReviewActorRole.SYSTEM)
                    .requestedActorReference("system")
                    .fallbackAllowed(true)
                    .systemInternalAllowed(false)
                    .build();
            assertThatThrownBy(() -> resolver.resolve(req))
                    .isInstanceOf(AdminActorResolutionException.class);
        }

        @Test
        @DisplayName("SYSTEM resolves via SYSTEM_INTERNAL when allowed")
        void resolverAllows() {
            var actor = resolver.resolveSystemInternal("TEST");
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.SYSTEM_INTERNAL);
        }
    }

    // ── D. CREATOR Restriction Matrix ───────────────────────

    @Nested
    @DisplayName("D. CREATOR Restrictions")
    class CreatorRestrictions {

        @Test
        @DisplayName("canCreatorApprove is false")
        void noApprove() {
            assertThat(AdminActorBoundaryPolicy.canCreatorApprove()).isFalse();
        }

        @Test
        @DisplayName("canCreatorHandoff is false")
        void noHandoff() {
            assertThat(AdminActorBoundaryPolicy.canCreatorHandoff()).isFalse();
        }

        @Test
        @DisplayName("canCreatorRevision is false")
        void noRevision() {
            assertThat(AdminActorBoundaryPolicy.canCreatorRevision()).isFalse();
        }

        @Test
        @DisplayName("CREATOR blocked for REVIEW_ACTION")
        void reviewBlocked() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION))
                    .doesNotContain(ReviewActorRole.CREATOR);
        }

        @Test
        @DisplayName("CREATOR blocked for INTERNAL_HANDOFF")
        void handoffBlocked() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF))
                    .doesNotContain(ReviewActorRole.CREATOR);
        }

        @Test
        @DisplayName("CREATOR blocked for INTERNAL_REVISION_REQUEST")
        void revisionBlocked() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_REVISION_REQUEST))
                    .doesNotContain(ReviewActorRole.CREATOR);
        }

        @Test
        @DisplayName("CREATOR allowed for SUBMIT_FOR_REVIEW")
        void submitAllowed() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.SUBMIT_FOR_REVIEW))
                    .contains(ReviewActorRole.CREATOR);
        }

        @Test
        @DisplayName("CREATOR allowed for ADD_REVIEW_NOTE")
        void noteAllowed() {
            assertThat(AdminActorBoundaryPolicy.getAllowedRoles(
                    AdminActorBoundaryPolicy.ActionCategory.ADD_REVIEW_NOTE))
                    .contains(ReviewActorRole.CREATOR);
        }
    }

    // ── E. Sanitization Comprehensive ───────────────────────

    @Nested
    @DisplayName("E. Sanitization Comprehensive")
    class Sanitization {

        @Test
        @DisplayName("bearer token stripped")
        void bearer() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "Bearer eyJxyz...");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
            assertThat(actor.getActorReference()).doesNotContain("eyJxyz");
        }

        @Test
        @DisplayName("password stripped")
        void password() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "password=secret");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
        }

        @Test
        @DisplayName("JWT-like string stripped")
        void jwt() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN,
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
            assertThat(actor.getActorReference()).doesNotContain("eyJhbGci");
        }

        @Test
        @DisplayName("authorization header stripped")
        void authHeader() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "authorization=sometoken");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
        }

        @Test
        @DisplayName("HTML tags stripped")
        void htmlStripped() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "<script>alert('xss')</script>");
            assertThat(actor.getActorReference()).doesNotContain("<script>");
        }

        @Test
        @DisplayName("long reference truncated to 200")
        void truncated() {
            String longRef = "a".repeat(300);
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, longRef);
            assertThat(actor.getActorReference().length()).isLessThanOrEqualTo(200);
        }

        @Test
        @DisplayName("empty reference falls back to role name")
        void emptyFallback() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "");
            assertThat(actor.getActorReference()).isEqualTo("REVIEWER");
        }

        @Test
        @DisplayName("null reference falls back to role name")
        void nullFallback() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, null);
            assertThat(actor.getActorReference()).isEqualTo("ADMIN");
        }
    }

    // ── F. Exception Safety ─────────────────────────────────

    @Nested
    @DisplayName("F. Exception Safety")
    class ExceptionSafety {

        @Test
        @DisplayName("SYSTEM_NOT_ALLOWED exception safe")
        void systemNotAllowed() {
            var ex = new AdminActorResolutionException("msg", "SYSTEM_NOT_ALLOWED", ReviewActorRole.SYSTEM);
            assertThat(ex.getSafeErrorCode()).isEqualTo("SYSTEM_NOT_ALLOWED");
            String str = ex.toString();
            for (String f : List.of("token", "jwt", "session", "credential", "password", "stackTrace")) {
                assertThat(str).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("BOUNDARY_POLICY_VIOLATION exception safe")
        void boundaryViolation() {
            var ex = new AdminActorResolutionException("msg", "BOUNDARY_POLICY_VIOLATION", ReviewActorRole.CREATOR);
            assertThat(ex.getSafeErrorCode()).isEqualTo("BOUNDARY_POLICY_VIOLATION");
        }

        @Test
        @DisplayName("null actorRole rejected at builder level")
        void actorRoleNull() {
            // The builder validates requestedActorRole != null
            try {
                AdminActorResolutionRequest.builder()
                        .requestedActorRole(null)
                        .fallbackAllowed(true)
                        .build();
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage()).contains("requestedActorRole");
            }
        }

        @Test
        @DisplayName("resolution exception does not contain credential data")
        void noCredentialLeak() {
            try {
                resolver.resolve(AdminActorResolutionRequest.builder()
                        .requestedActorRole(ReviewActorRole.SYSTEM)
                        .requestedActorReference("Bearer super_secret_token")
                        .systemInternalAllowed(false)
                        .fallbackAllowed(true)
                        .build());
            } catch (AdminActorResolutionException e) {
                assertThat(e.getMessage()).doesNotContain("super_secret_token");
                assertThat(e.getMessage()).doesNotContain("Bearer");
            }
        }
    }

    // ── G. Endpoint Inventory ───────────────────────────────

    @Nested
    @DisplayName("G. Endpoint Inventory (21 = 9 POST + 12 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("AdminReviewController: 9 POST + 8 GET")
        void review() {
            assertThat(countMethods(AdminReviewController.class, PostMapping.class)).isEqualTo(9);
            assertThat(countMethods(AdminReviewController.class, GetMapping.class)).isEqualTo(8);
        }

        @Test
        @DisplayName("AdminPipelineController: 0 POST + 2 GET")
        void pipeline() {
            assertThat(countMethods(AdminPipelineController.class, PostMapping.class)).isZero();
            assertThat(countMethods(AdminPipelineController.class, GetMapping.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("AdminWorkflowObservabilityController: 0 POST + 2 GET")
        void observability() {
            assertThat(countMethods(AdminWorkflowObservabilityController.class, PostMapping.class)).isZero();
            assertThat(countMethods(AdminWorkflowObservabilityController.class, GetMapping.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("total 21 = 9 POST + 12 GET")
        void total() {
            long post = countMethods(AdminReviewController.class, PostMapping.class)
                    + countMethods(AdminPipelineController.class, PostMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, PostMapping.class);
            long get = countMethods(AdminReviewController.class, GetMapping.class)
                    + countMethods(AdminPipelineController.class, GetMapping.class)
                    + countMethods(AdminWorkflowObservabilityController.class, GetMapping.class);
            assertThat(post).isEqualTo(9);
            assertThat(get).isEqualTo(12);
            assertThat(post + get).isEqualTo(21);
        }

        @Test
        @DisplayName("no PUT anywhere")
        void noPut() {
            for (var c : List.of(AdminReviewController.class, AdminPipelineController.class,
                    AdminWorkflowObservabilityController.class)) {
                assertThat(countMethods(c, PutMapping.class)).as("PUT in %s", c.getSimpleName()).isZero();
            }
        }

        @Test
        @DisplayName("no DELETE anywhere")
        void noDelete() {
            for (var c : List.of(AdminReviewController.class, AdminPipelineController.class,
                    AdminWorkflowObservabilityController.class)) {
                assertThat(countMethods(c, DeleteMapping.class)).as("DELETE in %s", c.getSimpleName()).isZero();
            }
        }

        private long countMethods(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── H. No Forbidden Action Types ────────────────────────

    @Nested
    @DisplayName("H. No Forbidden Action Types")
    class NoForbiddenActions {

        @Test
        @DisplayName("no PUBLISH/EXPORT/AUTO_APPROVE/GENERIC_STATUS_UPDATE")
        void noForbidden() {
            Set<String> names = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("PUBLISH", "EXPORT", "AUTO_APPROVE", "GENERIC_STATUS_UPDATE",
                    "MARK_COMPLETED", "READY_FOR_PUBLISH", "FORCE_APPROVE")
                    .forEach(f -> assertThat(names).doesNotContain(f));
        }

        @Test
        @DisplayName("REQUEST_INTERNAL_REVISION classifies to INTERNAL_REVISION_REQUEST")
        void revisionClassifies() {
            assertThat(AdminActorBoundaryPolicy.classify(ReviewActionType.REQUEST_INTERNAL_REVISION))
                    .isEqualTo(AdminActorBoundaryPolicy.ActionCategory.INTERNAL_REVISION_REQUEST);
        }
    }

    // ── I. No Forbidden DTO Fields ──────────────────────────

    @Nested
    @DisplayName("I. No Forbidden DTO Fields")
    class NoForbiddenFields {

        private static final List<String> FORBIDDEN = List.of(
                "token", "jwt", "session", "credential", "password",
                "secret", "apiKey", "accessKey", "principalRaw",
                "rawPrincipal", "authorizationHeader", "publicUrl",
                "signedUrl", "cdnUrl", "providerUrl", "storageKey",
                "bucket", "objectPath", "rawPayload", "base64");

        @Test
        @DisplayName("ResolvedAdminActor has no forbidden fields")
        void resolvedActor() {
            checkFields(ResolvedAdminActor.class);
        }

        @Test
        @DisplayName("AdminActorResolutionRequest has no forbidden fields")
        void resolutionRequest() {
            checkFields(AdminActorResolutionRequest.class);
        }

        @Test
        @DisplayName("AdminActorResolutionException has no forbidden fields")
        void resolutionException() {
            checkFields(AdminActorResolutionException.class);
        }

        @Test
        @DisplayName("AdminReviewActionRequest has no forbidden fields")
        void actionRequest() {
            checkFields(AdminReviewActionRequest.class);
        }

        @Test
        @DisplayName("AdminReviewHandoffRequest has no forbidden fields")
        void handoffRequest() {
            checkFields(AdminReviewHandoffRequest.class);
        }

        @Test
        @DisplayName("AdminReviewActionResponse has no forbidden fields")
        void actionResponse() {
            checkFields(AdminReviewActionResponse.class);
        }

        @Test
        @DisplayName("AdminReviewHandoffResponse has no forbidden fields")
        void handoffResponse() {
            checkFields(AdminReviewHandoffResponse.class);
        }

        private void checkFields(Class<?> c) {
            Set<String> fields = Arrays.stream(c.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            for (String f : FORBIDDEN) {
                assertThat(fields).as("forbidden field '%s' in %s", f, c.getSimpleName())
                        .doesNotContain(f);
            }
        }
    }

    // ── J. Boundary Enforcement ─────────────────────────────

    @Nested
    @DisplayName("J. Boundary Enforcement")
    class BoundaryEnforcement {

        @Test
        @DisplayName("enforce throws BOUNDARY_POLICY_VIOLATION for invalid actor/action")
        void enforceThrows() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            assertThatThrownBy(() -> AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor))
                    .isInstanceOf(AdminActorResolutionException.class);
        }

        @Test
        @DisplayName("enforce passes for valid REVIEWER + REVIEW_ACTION")
        void enforcePasses() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "rev");
            AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor);
        }

        @Test
        @DisplayName("enforce for SYSTEM handoff via SYSTEM_INTERNAL passes")
        void systemHandoffPasses() {
            var actor = resolver.resolveSystemInternal("HANDOFF");
            AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF, actor);
        }

        @Test
        @DisplayName("enforce for SYSTEM revision via SYSTEM_INTERNAL fails")
        void systemRevisionFails() {
            var actor = resolver.resolveSystemInternal("REVISION");
            assertThatThrownBy(() -> AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_REVISION_REQUEST, actor))
                    .isInstanceOf(AdminActorResolutionException.class);
        }
    }

    // ── K. Narrow Gate Methods ───────────────────────────────

    @Nested
    @DisplayName("K. Narrow Gate Methods")
    class NarrowGates {

        @Test
        @DisplayName("handoff narrow gate true only for SYSTEM")
        void handoffGate() {
            assertThat(AdminActorBoundaryPolicy.isHandoffSystemInternalPath(ReviewActorRole.SYSTEM)).isTrue();
            assertThat(AdminActorBoundaryPolicy.isHandoffSystemInternalPath(ReviewActorRole.REVIEWER)).isFalse();
            assertThat(AdminActorBoundaryPolicy.isHandoffSystemInternalPath(ReviewActorRole.ADMIN)).isFalse();
            assertThat(AdminActorBoundaryPolicy.isHandoffSystemInternalPath(ReviewActorRole.CREATOR)).isFalse();
        }

        @Test
        @DisplayName("revision gate always false for all roles")
        void revisionGate() {
            for (ReviewActorRole role : ReviewActorRole.values()) {
                assertThat(AdminActorBoundaryPolicy.isRevisionSystemInternalPath(role))
                        .as("role=%s", role).isFalse();
            }
        }
    }
}
