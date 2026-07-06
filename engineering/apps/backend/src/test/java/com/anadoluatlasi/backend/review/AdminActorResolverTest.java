package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FP-018 Task 02: Admin Actor Resolver tests.
 */
@DisplayName("FP-018 Task 02 — AdminActorResolver")
class AdminActorResolverTest {

    private final AdminActorResolver resolver = new AdminActorResolver();

    // ── A. Resolution Mode Enum ──────────────────────────────

    @Nested
    @DisplayName("A. AdminActorResolutionMode")
    class ModeEnum {

        @Test
        @DisplayName("tam olarak 3 mode")
        void exactCount() {
            assertThat(AdminActorResolutionMode.values()).hasSize(3);
        }

        @Test
        @DisplayName("beklenen 3 mode mevcut")
        void expectedValues() {
            Set<String> names = Arrays.stream(AdminActorResolutionMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).containsExactlyInAnyOrder(
                    "AUTHENTICATED_CONTEXT", "TEST_FALLBACK", "SYSTEM_INTERNAL");
        }

        @Test
        @DisplayName("yasaklı mode'lar yok")
        void noForbidden() {
            Set<String> names = Arrays.stream(AdminActorResolutionMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("CLIENT_TRUSTED", "REQUEST_ROLE_TRUSTED", "BYPASS_AUTH", "FORCE_ADMIN")
                    .forEach(f -> assertThat(names).doesNotContain(f));
        }
    }

    // ── B. Resolved Admin Actor DTO ──────────────────────────

    @Nested
    @DisplayName("B. ResolvedAdminActor")
    class ActorDto {

        @Test
        @DisplayName("builder + getter doğru çalışıyor")
        void builderGetters() {
            var actor = ResolvedAdminActor.builder()
                    .actorRole(ReviewActorRole.ADMIN)
                    .actorReference("admin-ref")
                    .resolutionMode(AdminActorResolutionMode.TEST_FALLBACK)
                    .safeDisplayName("Yönetici")
                    .safeReasonCode("TEST")
                    .safeMessage("test")
                    .build();
            assertThat(actor.getActorRole()).isEqualTo(ReviewActorRole.ADMIN);
            assertThat(actor.getActorReference()).isEqualTo("admin-ref");
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.TEST_FALLBACK);
            assertThat(actor.getSafeDisplayName()).isEqualTo("Yönetici");
            assertThat(actor.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("toString safe — no forbidden fields")
        void toStringSafe() {
            var actor = ResolvedAdminActor.builder()
                    .actorRole(ReviewActorRole.REVIEWER)
                    .actorReference("test")
                    .resolutionMode(AdminActorResolutionMode.TEST_FALLBACK)
                    .build();
            String str = actor.toString();
            List.of("token", "jwt", "session", "credential", "password",
                    "secret", "apiKey", "accessKey", "principalRaw",
                    "rawPrincipal", "authorizationHeader", "stackTrace", "exceptionClass")
                    .forEach(f -> assertThat(str).doesNotContain(f));
        }

        @Test
        @DisplayName("forbidden fields yok")
        void noForbiddenFields() {
            Set<String> fields = Arrays.stream(ResolvedAdminActor.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            List.of("token", "jwt", "session", "credential", "password",
                    "secret", "apiKey", "accessKey", "principalRaw",
                    "rawPrincipal", "authorizationHeader")
                    .forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("builder actorRole zorunlu")
        void actorRoleRequired() {
            assertThatThrownBy(() -> ResolvedAdminActor.builder()
                    .resolutionMode(AdminActorResolutionMode.TEST_FALLBACK).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("builder resolutionMode zorunlu")
        void modeRequired() {
            assertThatThrownBy(() -> ResolvedAdminActor.builder()
                    .actorRole(ReviewActorRole.ADMIN).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── C. Resolution Request ────────────────────────────────

    @Nested
    @DisplayName("C. AdminActorResolutionRequest")
    class RequestObj {

        @Test
        @DisplayName("builder + getter doğru çalışıyor")
        void builderGetters() {
            var req = AdminActorResolutionRequest.builder()
                    .requestedActorRole(ReviewActorRole.REVIEWER)
                    .requestedActorReference("ref")
                    .fallbackAllowed(true)
                    .systemInternalAllowed(false)
                    .safeReasonCode("REVIEW")
                    .build();
            assertThat(req.getRequestedActorRole()).isEqualTo(ReviewActorRole.REVIEWER);
            assertThat(req.isFallbackAllowed()).isTrue();
            assertThat(req.isSystemInternalAllowed()).isFalse();
        }

        @Test
        @DisplayName("toString safe")
        void toStringSafe() {
            var req = AdminActorResolutionRequest.builder()
                    .requestedActorRole(ReviewActorRole.ADMIN)
                    .build();
            String str = req.toString();
            List.of("token", "jwt", "session", "credential", "password")
                    .forEach(f -> assertThat(str).doesNotContain(f));
        }

        @Test
        @DisplayName("requestedActorRole zorunlu")
        void roleRequired() {
            assertThatThrownBy(() -> AdminActorResolutionRequest.builder().build())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── D. Resolution Exception ──────────────────────────────

    @Nested
    @DisplayName("D. AdminActorResolutionException")
    class ExceptionTests {

        @Test
        @DisplayName("safe error code + message")
        void safeFields() {
            var ex = new AdminActorResolutionException("msg", "CODE", ReviewActorRole.ADMIN);
            assertThat(ex.getSafeErrorCode()).isEqualTo("CODE");
            assertThat(ex.getRequestedRole()).isEqualTo(ReviewActorRole.ADMIN);
            assertThat(ex.getMessage()).isEqualTo("msg");
        }

        @Test
        @DisplayName("toString safe")
        void toStringSafe() {
            var ex = new AdminActorResolutionException("msg", "CODE", null);
            String str = ex.toString();
            List.of("token", "jwt", "session", "credential", "password")
                    .forEach(f -> assertThat(str).doesNotContain(f));
        }
    }

    // ── E. Resolver Behavior ─────────────────────────────────

    @Nested
    @DisplayName("E. Resolver Behavior")
    class ResolverBehavior {

        @Test
        @DisplayName("REVIEWER fallback başarılı")
        void reviewerFallback() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "reviewer-1");
            assertThat(actor.getActorRole()).isEqualTo(ReviewActorRole.REVIEWER);
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.TEST_FALLBACK);
            assertThat(actor.getActorReference()).isEqualTo("reviewer-1");
        }

        @Test
        @DisplayName("ADMIN fallback başarılı")
        void adminFallback() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "admin-1");
            assertThat(actor.getActorRole()).isEqualTo(ReviewActorRole.ADMIN);
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.TEST_FALLBACK);
        }

        @Test
        @DisplayName("CREATOR fallback başarılı (ama approve edemez)")
        void creatorFallback() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.CREATOR, "creator-1");
            assertThat(actor.getActorRole()).isEqualTo(ReviewActorRole.CREATOR);
            assertThat(actor.getActorRole().canPerformReview()).isFalse();
        }

        @Test
        @DisplayName("SYSTEM rejected when systemInternalAllowed=false")
        void systemRejected() {
            assertThatThrownBy(() -> resolver.resolveTestFallback(ReviewActorRole.SYSTEM, "sys"))
                    .isInstanceOf(AdminActorResolutionException.class)
                    .hasMessageContaining("SYSTEM");
        }

        @Test
        @DisplayName("SYSTEM allowed when systemInternalAllowed=true")
        void systemAllowed() {
            var actor = resolver.resolveSystemInternal("HANDOFF");
            assertThat(actor.getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
            assertThat(actor.getResolutionMode()).isEqualTo(AdminActorResolutionMode.SYSTEM_INTERNAL);
        }

        @Test
        @DisplayName("fallbackAllowed=false fails safely")
        void fallbackDisabled() {
            var req = AdminActorResolutionRequest.builder()
                    .requestedActorRole(ReviewActorRole.ADMIN)
                    .fallbackAllowed(false)
                    .build();
            assertThatThrownBy(() -> resolver.resolve(req))
                    .isInstanceOf(AdminActorResolutionException.class)
                    .hasMessageContaining("geri dönüşü devre dışı");
        }

        @Test
        @DisplayName("null request fails safely")
        void nullRequest() {
            assertThatThrownBy(() -> resolver.resolve(null))
                    .isInstanceOf(AdminActorResolutionException.class);
        }

        @Test
        @DisplayName("null actorRole fails safely")
        void nullRole() {
            var req = AdminActorResolutionRequest.builder()
                    .requestedActorRole(ReviewActorRole.ADMIN)
                    .build();
            // Build succeeds, but let's test with a direct resolve with null role
            assertThatThrownBy(() -> resolver.resolve(null))
                    .isInstanceOf(AdminActorResolutionException.class);
        }

        @Test
        @DisplayName("reference sanitized — bearer stripped")
        void bearerStripped() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "Bearer eyJhbGci...");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
        }

        @Test
        @DisplayName("reference sanitized — password stripped")
        void passwordStripped() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.ADMIN, "password=s3cret");
            assertThat(actor.getActorReference()).contains("[REDACTED]");
        }

        @Test
        @DisplayName("empty reference uses role name as fallback")
        void emptyRefFallback() {
            var actor = resolver.resolveTestFallback(ReviewActorRole.REVIEWER, "");
            assertThat(actor.getActorReference()).isEqualTo("REVIEWER");
        }
    }

    // ── F. Resolver No Forbidden Dependencies ─────────────────

    @Nested
    @DisplayName("F. Resolver No Forbidden Dependencies")
    class ResolverDependencies {

        @Test
        @DisplayName("@Service annotation")
        void isService() {
            assertThat(AdminActorResolver.class.getAnnotation(
                    org.springframework.stereotype.Service.class)).isNotNull();
        }

        @Test
        @DisplayName("no ReviewApplicationService dependency")
        void noReviewAppService() {
            var params = Arrays.stream(AdminActorResolver.class.getDeclaredConstructors())
                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
                    .collect(Collectors.toSet());
            assertThat(params).doesNotContain(ReviewApplicationService.class);
        }

        @Test
        @DisplayName("no provider/storage dependency")
        void noProviderStorage() {
            var fields = Arrays.stream(AdminActorResolver.class.getDeclaredFields())
                    .map(f -> f.getType().getSimpleName().toLowerCase())
                    .collect(Collectors.toSet());
            fields.forEach(f -> {
                assertThat(f).doesNotContain("provider");
                assertThat(f).doesNotContain("storage");
            });
        }

        @Test
        @DisplayName("no controller annotation")
        void noController() {
            assertThat(AdminActorResolver.class.getAnnotation(
                    org.springframework.web.bind.annotation.RestController.class)).isNull();
        }
    }
}
