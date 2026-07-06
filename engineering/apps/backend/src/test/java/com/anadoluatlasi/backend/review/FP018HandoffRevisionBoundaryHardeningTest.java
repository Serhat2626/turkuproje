package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FP-018 Task 04: Handoff & Internal Revision Actor Boundary Hardening tests.
 *
 * <p>Proves handoff SYSTEM path is narrow, revision SYSTEM is always blocked,
 * CREATOR restrictions enforced, and safety baseline preserved.</p>
 */
@DisplayName("FP-018 Task 04 — Handoff & Revision Actor Boundary Hardening")
class FP018HandoffRevisionBoundaryHardeningTest {

    private final AdminActorResolver resolver = new AdminActorResolver();

    // ── A. Handoff Boundary Policy ──────────────────────────

    @Nested
    @DisplayName("A. Handoff Boundary Policy")
    class HandoffPolicy {

        @Test
        @DisplayName("canSystemHandoff returns true (via SYSTEM_INTERNAL only)")
        void canSystemHandoff() {
            assertThat(AdminActorBoundaryPolicy.canSystemHandoff()).isTrue();
        }

        @Test
        @DisplayName("canCreatorHandoff returns false")
        void canCreatorHandoff() {
            assertThat(AdminActorBoundaryPolicy.canCreatorHandoff()).isFalse();
        }

        @Test
        @DisplayName("isHandoffSystemInternalPath true for SYSTEM")
        void systemPathTrue() {
            assertThat(AdminActorBoundaryPolicy.isHandoffSystemInternalPath(ReviewActorRole.SYSTEM)).isTrue();
        }

        @Test
        @DisplayName("isHandoffSystemInternalPath false for REVIEWER")
        void reviewerPathFalse() {
            assertThat(AdminActorBoundaryPolicy.isHandoffSystemInternalPath(ReviewActorRole.REVIEWER)).isFalse();
        }

        @Test
        @DisplayName("isHandoffSystemInternalPath false for ADMIN")
        void adminPathFalse() {
            assertThat(AdminActorBoundaryPolicy.isHandoffSystemInternalPath(ReviewActorRole.ADMIN)).isFalse();
        }

        @Test
        @DisplayName("isHandoffSystemInternalPath false for CREATOR")
        void creatorPathFalse() {
            assertThat(AdminActorBoundaryPolicy.isHandoffSystemInternalPath(ReviewActorRole.CREATOR)).isFalse();
        }

        @Test
        @DisplayName("REVIEWER allowed for handoff via TEST_FALLBACK")
        void reviewerHandoffAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "rev");
            assertThat(AdminActorBoundaryPolicy.isHandoffAllowed(actor)).isTrue();
        }

        @Test
        @DisplayName("ADMIN allowed for handoff via TEST_FALLBACK")
        void adminHandoffAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin");
            assertThat(AdminActorBoundaryPolicy.isHandoffAllowed(actor)).isTrue();
        }

        @Test
        @DisplayName("SYSTEM allowed for handoff via SYSTEM_INTERNAL")
        void systemHandoffAllowed() {
            var actor = resolver.resolveSystemInternal("HANDOFF");
            assertThat(AdminActorBoundaryPolicy.isHandoffAllowed(actor)).isTrue();
        }

        @Test
        @DisplayName("CREATOR not allowed for handoff")
        void creatorHandoffNotAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            assertThat(AdminActorBoundaryPolicy.isHandoffAllowed(actor)).isFalse();
        }

        @Test
        @DisplayName("enforce throws for CREATOR handoff")
        void enforceCreatorHandoffThrows() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            assertThatThrownBy(() -> AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF, actor))
                    .isInstanceOf(AdminActorResolutionException.class)
                    .hasMessageContaining("CREATOR");
        }
    }

    // ── B. Revision Boundary Policy ─────────────────────────

    @Nested
    @DisplayName("B. Revision Boundary Policy")
    class RevisionPolicy {

        @Test
        @DisplayName("canSystemRevision returns false")
        void canSystemRevision() {
            assertThat(AdminActorBoundaryPolicy.canSystemRevision()).isFalse();
        }

        @Test
        @DisplayName("canCreatorRevision returns false")
        void canCreatorRevision() {
            assertThat(AdminActorBoundaryPolicy.canCreatorRevision()).isFalse();
        }

        @Test
        @DisplayName("isRevisionSystemInternalPath always false for SYSTEM")
        void systemPathAlwaysFalse() {
            assertThat(AdminActorBoundaryPolicy.isRevisionSystemInternalPath(ReviewActorRole.SYSTEM)).isFalse();
        }

        @Test
        @DisplayName("isRevisionSystemInternalPath always false for REVIEWER")
        void reviewerPathFalse() {
            assertThat(AdminActorBoundaryPolicy.isRevisionSystemInternalPath(ReviewActorRole.REVIEWER)).isFalse();
        }

        @Test
        @DisplayName("isRevisionSystemInternalPath always false for all roles")
        void allPathsFalse() {
            for (ReviewActorRole role : ReviewActorRole.values()) {
                assertThat(AdminActorBoundaryPolicy.isRevisionSystemInternalPath(role))
                        .as("role=%s", role).isFalse();
            }
        }

        @Test
        @DisplayName("REVIEWER allowed for revision via TEST_FALLBACK")
        void reviewerRevisionAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "rev");
            assertThat(AdminActorBoundaryPolicy.isRevisionAllowed(actor)).isTrue();
        }

        @Test
        @DisplayName("ADMIN allowed for revision via TEST_FALLBACK")
        void adminRevisionAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin");
            assertThat(AdminActorBoundaryPolicy.isRevisionAllowed(actor)).isTrue();
        }

        @Test
        @DisplayName("CREATOR not allowed for revision")
        void creatorRevisionNotAllowed() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            assertThat(AdminActorBoundaryPolicy.isRevisionAllowed(actor)).isFalse();
        }

        @Test
        @DisplayName("enforce throws for CREATOR revision")
        void enforceCreatorRevisionThrows() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            assertThatThrownBy(() -> AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_REVISION_REQUEST, actor))
                    .isInstanceOf(AdminActorResolutionException.class)
                    .hasMessageContaining("CREATOR");
        }
    }

    // ── C. SYSTEM Blocking in Revision via Resolver ─────────

    @Nested
    @DisplayName("C. SYSTEM Blocking in Revision")
    class SystemRevisionBlocking {

        @Test
        @DisplayName("SYSTEM blocked by resolver when systemInternalAllowed=false")
        void systemBlockedByResolver() {
            var req = AdminActorResolutionRequest.builder()
                    .requestedActorRole(ReviewActorRole.SYSTEM)
                    .requestedActorReference("system")
                    .fallbackAllowed(true)
                    .systemInternalAllowed(false)
                    .safeReasonCode("REVISION")
                    .build();
            assertThatThrownBy(() -> resolver.resolve(req))
                    .isInstanceOf(AdminActorResolutionException.class)
                    .hasMessageContaining("SYSTEM");
        }

        @Test
        @DisplayName("even with systemInternalAllowed=true, SYSTEM rejected by revision boundary policy")
        void systemRejectedByBoundaryEvenIfResolverAllows() {
            // Simulate: resolver allows SYSTEM (would happen if someone bypasses the narrow gate)
            var actor = resolver.resolveSystemInternal("REVISION");
            // But boundary policy rejects: INTERNAL_REVISION_REQUEST does not allow SYSTEM_INTERNAL mode
            assertThat(AdminActorBoundaryPolicy.isRevisionAllowed(actor)).isFalse();
            assertThatThrownBy(() -> AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_REVISION_REQUEST, actor))
                    .isInstanceOf(AdminActorResolutionException.class);
        }

        @Test
        @DisplayName("isRevisionSystemInternalPath prevents SYSTEM from even reaching resolver")
        void gatePreventsSytem() {
            boolean isSystemPath = AdminActorBoundaryPolicy.isRevisionSystemInternalPath(ReviewActorRole.SYSTEM);
            assertThat(isSystemPath).isFalse();
            // This means systemInternalAllowed will be false when passed to resolver
        }
    }

    // ── D. Handoff SYSTEM via Narrow Gate ────────────────────

    @Nested
    @DisplayName("D. Handoff SYSTEM Only via Narrow Gate")
    class HandoffSystemNarrowGate {

        @Test
        @DisplayName("SYSTEM handoff succeeds through narrow gate")
        void systemHandoffViaGate() {
            boolean isSystemPath = AdminActorBoundaryPolicy.isHandoffSystemInternalPath(ReviewActorRole.SYSTEM);
            assertThat(isSystemPath).isTrue();

            var actor = resolver.resolve(AdminActorResolutionRequest.builder()
                    .requestedActorRole(ReviewActorRole.SYSTEM)
                    .requestedActorReference("system-handoff")
                    .fallbackAllowed(true)
                    .systemInternalAllowed(isSystemPath)
                    .safeReasonCode("HANDOFF")
                    .build());

            assertThat(actor.getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.SYSTEM_INTERNAL);
            assertThat(AdminActorBoundaryPolicy.isHandoffAllowed(actor)).isTrue();
        }

        @Test
        @DisplayName("non-SYSTEM handoff does not use SYSTEM_INTERNAL mode")
        void nonSystemHandoffDoesNotUseSystemInternal() {
            boolean isSystemPath = AdminActorBoundaryPolicy.isHandoffSystemInternalPath(ReviewActorRole.ADMIN);
            assertThat(isSystemPath).isFalse();

            var actor = resolver.resolve(AdminActorResolutionRequest.builder()
                    .requestedActorRole(ReviewActorRole.ADMIN)
                    .requestedActorReference("admin")
                    .fallbackAllowed(true)
                    .systemInternalAllowed(isSystemPath)
                    .safeReasonCode("HANDOFF")
                    .build());

            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.TEST_FALLBACK);
            assertThat(AdminActorBoundaryPolicy.isHandoffAllowed(actor)).isTrue();
        }
    }

    // ── E. Boundary Enforcement ──────────────────────────────

    @Nested
    @DisplayName("E. Boundary Enforcement")
    class BoundaryEnforcement {

        @Test
        @DisplayName("enforce passes for valid REVIEWER + REVIEW_ACTION")
        void validReviewerReviewAction() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "rev");
            AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor);
            // No exception = pass
        }

        @Test
        @DisplayName("enforce passes for valid ADMIN + INTERNAL_HANDOFF")
        void validAdminHandoff() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin");
            AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF, actor);
        }

        @Test
        @DisplayName("enforce passes for SYSTEM + INTERNAL_HANDOFF via SYSTEM_INTERNAL")
        void validSystemHandoff() {
            var actor = resolver.resolveSystemInternal("HANDOFF");
            AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF, actor);
        }

        @Test
        @DisplayName("enforce throws for CREATOR + REVIEW_ACTION")
        void invalidCreatorReview() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            assertThatThrownBy(() -> AdminActorBoundaryPolicy.enforce(
                    AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor))
                    .isInstanceOf(AdminActorResolutionException.class)
                    .hasMessageContaining("CREATOR");
        }

        @Test
        @DisplayName("enforce exception has safe error code")
        void enforceExceptionSafe() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator");
            try {
                AdminActorBoundaryPolicy.enforce(
                        AdminActorBoundaryPolicy.ActionCategory.REVIEW_ACTION, actor);
            } catch (AdminActorResolutionException e) {
                assertThat(e.getSafeErrorCode()).isEqualTo("BOUNDARY_POLICY_VIOLATION");
                assertThat(e.toString()).doesNotContain("token");
                assertThat(e.toString()).doesNotContain("jwt");
            }
        }
    }

    // ── F. Sanitization Regression ──────────────────────────

    @Nested
    @DisplayName("F. Sanitization Regression")
    class SanitizationRegression {

        @Test
        @DisplayName("bearer token stripped in handoff actor reference")
        void bearerStrippedHandoff() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "Bearer eyJhbGciOi...");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
            assertThat(actor.getActorReference()).doesNotContain("eyJhbGci");
        }

        @Test
        @DisplayName("password stripped in handoff actor reference")
        void passwordStrippedHandoff() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "password=secret123");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
        }

        @Test
        @DisplayName("JWT-looking string stripped in actor reference")
        void jwtStripped() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN,
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.sig");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
        }

        @Test
        @DisplayName("safe toString on resolved actor — no forbidden fields")
        void toStringSafe() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin");
            String str = actor.toString();
            List.of("token", "jwt", "session", "credential", "password",
                    "secret", "apiKey", "accessKey", "principalRaw",
                    "rawPrincipal", "authorizationHeader", "stackTrace", "exceptionClass")
                    .forEach(f -> assertThat(str).doesNotContain(f));
        }

        @Test
        @DisplayName("safe toString on resolution exception")
        void exceptionToStringSafe() {
            var ex = new AdminActorResolutionException("msg", "CODE", ReviewActorRole.ADMIN);
            String str = ex.toString();
            List.of("token", "jwt", "session", "credential", "password")
                    .forEach(f -> assertThat(str).doesNotContain(f));
        }
    }

    // ── G. Endpoint Inventory ───────────────────────────────

    @Nested
    @DisplayName("G. Endpoint Inventory (21 = 9 POST + 12 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("total 21 endpoints unchanged")
        void total21() {
            long post = countAnnotated(AdminReviewController.class, PostMapping.class)
                    + countAnnotated(AdminPipelineController.class, PostMapping.class)
                    + countAnnotated(AdminWorkflowObservabilityController.class, PostMapping.class);
            long get = countAnnotated(AdminReviewController.class, GetMapping.class)
                    + countAnnotated(AdminPipelineController.class, GetMapping.class)
                    + countAnnotated(AdminWorkflowObservabilityController.class, GetMapping.class);
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
                assertThat(countAnnotated(c, PutMapping.class)).as("PUT in %s", c.getSimpleName()).isZero();
            }
        }

        @Test
        @DisplayName("no DELETE endpoints")
        void noDelete() {
            for (Class<?> c : List.of(AdminReviewController.class,
                    AdminPipelineController.class,
                    AdminWorkflowObservabilityController.class)) {
                assertThat(countAnnotated(c, DeleteMapping.class)).as("DELETE in %s", c.getSimpleName()).isZero();
            }
        }

        private long countAnnotated(Class<?> c, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(c.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── H. No Forbidden Enum/Action ─────────────────────────

    @Nested
    @DisplayName("H. No Forbidden Actions")
    class NoForbiddenActions {

        @Test
        @DisplayName("no PUBLISH/EXPORT/AUTO_APPROVE/GENERIC_STATUS_UPDATE in ReviewActionType")
        void noForbiddenActionTypes() {
            Set<String> names = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("PUBLISH", "EXPORT", "AUTO_APPROVE", "GENERIC_STATUS_UPDATE",
                    "MARK_COMPLETED", "READY_FOR_PUBLISH")
                    .forEach(f -> assertThat(names).doesNotContain(f));
        }

        @Test
        @DisplayName("no CLIENT_TRUSTED/BYPASS_AUTH/FORCE_ADMIN in resolution modes")
        void noForbiddenModes() {
            Set<String> names = Arrays.stream(AdminActorResolutionMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("CLIENT_TRUSTED", "REQUEST_ROLE_TRUSTED", "BYPASS_AUTH", "FORCE_ADMIN")
                    .forEach(f -> assertThat(names).doesNotContain(f));
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

        @Test
        @DisplayName("SYSTEM cannot request revision")
        void systemCannotRevision() {
            assertThat(AdminActorBoundaryPolicy.canSystemRevision()).isFalse();
        }

        @Test
        @DisplayName("CREATOR cannot request revision")
        void creatorCannotRevision() {
            assertThat(AdminActorBoundaryPolicy.canCreatorRevision()).isFalse();
        }

        @Test
        @DisplayName("CREATOR cannot handoff")
        void creatorCannotHandoff() {
            assertThat(AdminActorBoundaryPolicy.canCreatorHandoff()).isFalse();
        }
    }

    // ── I. Cross-Category Boundary Matrix ────────────────────

    @Nested
    @DisplayName("I. Cross-Category Boundary Matrix")
    class BoundaryMatrix {

        @Test
        @DisplayName("SYSTEM only allowed in INTERNAL_HANDOFF and READ_ONLY_VISIBILITY")
        void systemOnlyInHandoffAndReadOnly() {
            for (var cat : AdminActorBoundaryPolicy.allCategories()) {
                Set<ReviewActorRole> roles = AdminActorBoundaryPolicy.getAllowedRoles(cat);
                if (cat == AdminActorBoundaryPolicy.ActionCategory.INTERNAL_HANDOFF
                        || cat == AdminActorBoundaryPolicy.ActionCategory.READ_ONLY_VISIBILITY) {
                    assertThat(roles).as("SYSTEM in %s", cat).contains(ReviewActorRole.SYSTEM);
                } else {
                    assertThat(roles).as("SYSTEM not in %s", cat).doesNotContain(ReviewActorRole.SYSTEM);
                }
            }
        }

        @Test
        @DisplayName("CREATOR only allowed in READ_ONLY, SUBMIT, and ADD_NOTE")
        void creatorOnlyInReadSubmitNote() {
            for (var cat : AdminActorBoundaryPolicy.allCategories()) {
                Set<ReviewActorRole> roles = AdminActorBoundaryPolicy.getAllowedRoles(cat);
                if (cat == AdminActorBoundaryPolicy.ActionCategory.READ_ONLY_VISIBILITY
                        || cat == AdminActorBoundaryPolicy.ActionCategory.SUBMIT_FOR_REVIEW
                        || cat == AdminActorBoundaryPolicy.ActionCategory.ADD_REVIEW_NOTE) {
                    assertThat(roles).as("CREATOR in %s", cat).contains(ReviewActorRole.CREATOR);
                } else {
                    assertThat(roles).as("CREATOR not in %s", cat).doesNotContain(ReviewActorRole.CREATOR);
                }
            }
        }

        @Test
        @DisplayName("REVIEWER and ADMIN allowed in all categories")
        void reviewerAdminEverywhere() {
            for (var cat : AdminActorBoundaryPolicy.allCategories()) {
                Set<ReviewActorRole> roles = AdminActorBoundaryPolicy.getAllowedRoles(cat);
                assertThat(roles).as("REVIEWER in %s", cat).contains(ReviewActorRole.REVIEWER);
                assertThat(roles).as("ADMIN in %s", cat).contains(ReviewActorRole.ADMIN);
            }
        }
    }
}
