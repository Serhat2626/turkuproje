package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-017 Task 05: Operational Controls Guardrail Hardening.
 *
 * <p>Proves all control hints are non-mutating labels.
 * No hint executes workflow actions. No new POST endpoints.
 * Observability layer remains read-only.</p>
 */
@DisplayName("FP-017 Task 05 — Operational Controls Guardrail Hardening")
class FP017OperationalControlsGuardrailTest {

    // ── A. SafeOperationalControlHint Exact Values ─────────────────

    @Nested
    @DisplayName("A. SafeOperationalControlHint Exact Values")
    class HintExactValues {

        @Test
        @DisplayName("tam olarak 5 hint değeri")
        void exactCount() {
            assertThat(SafeOperationalControlHint.values()).hasSize(5);
        }

        @Test
        @DisplayName("beklenen 5 hint mevcut")
        void expectedValues() {
            Set<String> values = Arrays.stream(SafeOperationalControlHint.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(values).containsExactlyInAnyOrder(
                    "REFRESH_OVERVIEW",
                    "RECHECK_READINESS",
                    "VIEW_SAFE_PREVIEW",
                    "REVIEW_PIPELINE_DETAIL",
                    "REQUEST_MANUAL_INVESTIGATION");
        }
    }

    // ── B. Forbidden Hint/Action Values ────────────────────────────

    @Nested
    @DisplayName("B. Forbidden Hint/Action Values")
    class ForbiddenHints {

        private static final List<String> FORBIDDEN = List.of(
                "PUBLISH", "EXPORT", "APPROVE", "AUTO_FIX",
                "REGENERATE", "PROVIDER_RETRY", "STORAGE_RETRY",
                "CREATE_PUBLIC_URL", "CREATE_SIGNED_URL", "CDN_DELIVERY",
                "FORCE_APPROVE", "FORCE_PUBLISH", "AUTO_APPROVE",
                "GENERIC_STATUS_UPDATE", "DELETE", "FORCE_TRANSITION");

        @Test
        @DisplayName("yasaklı hint değerleri yok")
        void noForbiddenValues() {
            Set<String> values = Arrays.stream(SafeOperationalControlHint.values())
                    .map(Enum::name).collect(Collectors.toSet());
            FORBIDDEN.forEach(f -> assertThat(values)
                    .as("Hint '%s' should not exist", f)
                    .doesNotContain(f));
        }

        @Test
        @DisplayName("hint isimleri mutation/create/delete/force içermez")
        void noMutationKeywords() {
            for (SafeOperationalControlHint h : SafeOperationalControlHint.values()) {
                String name = h.name();
                assertThat(name).doesNotContain("DELETE")
                        .doesNotContain("UPDATE")
                        .doesNotContain("CREATE")
                        .doesNotContain("FORCE")
                        .doesNotContain("WRITE")
                        .doesNotContain("MUTATE")
                        .doesNotContain("INSERT");
            }
        }
    }

    // ── C. Service Read-Only ──────────────────────────────────────

    @Nested
    @DisplayName("C. WorkflowObservabilityService Read-Only")
    class ServiceReadOnly {

        @Test
        @DisplayName("@Service annotation")
        void isService() {
            assertThat(WorkflowObservabilityService.class.getAnnotation(
                    org.springframework.stereotype.Service.class)).isNotNull();
        }

        @Test
        @DisplayName("no @RestController/@Controller")
        void noController() {
            assertThat(WorkflowObservabilityService.class.getAnnotation(
                    org.springframework.web.bind.annotation.RestController.class)).isNull();
            assertThat(WorkflowObservabilityService.class.getAnnotation(
                    org.springframework.stereotype.Controller.class)).isNull();
        }

        @Test
        @DisplayName("public metotlar @Transactional(readOnly=true)")
        void readOnlyTransactional() {
            for (var m : WorkflowObservabilityService.class.getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers())) {
                    var tx = m.getAnnotation(
                            org.springframework.transaction.annotation.Transactional.class);
                    if (tx != null) {
                        assertThat(tx.readOnly())
                                .as("Method %s should be readOnly", m.getName())
                                .isTrue();
                    }
                }
            }
        }

        @Test
        @DisplayName("ReviewApplicationService bağımlılığı yok")
        void noReviewAppServiceDep() {
            var params = Arrays.stream(WorkflowObservabilityService.class.getDeclaredConstructors())
                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
                    .collect(Collectors.toSet());
            assertThat(params).doesNotContain(ReviewApplicationService.class);
        }

        @Test
        @DisplayName("provider/storage service bağımlılığı yok")
        void noProviderStorageDep() {
            var paramNames = Arrays.stream(WorkflowObservabilityService.class.getDeclaredConstructors())
                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
                    .map(Class::getSimpleName)
                    .collect(Collectors.toSet());
            for (String name : paramNames) {
                String lower = name.toLowerCase();
                assertThat(lower).doesNotContain("provider")
                        .doesNotContain("storage")
                        .doesNotContain("cdn")
                        .doesNotContain("generation");
            }
        }
    }

    // ── D. Controller GET-Only ─────────────────────────────────────

    @Nested
    @DisplayName("D. AdminWorkflowObservabilityController GET-Only")
    class ControllerGetOnly {

        @Test
        @DisplayName("0 POST")
        void noPost() {
            assertThat(Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count()).isZero();
        }

        @Test
        @DisplayName("0 PUT")
        void noPut() {
            assertThat(Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PutMapping.class)).count()).isZero();
        }

        @Test
        @DisplayName("0 DELETE")
        void noDelete() {
            assertThat(Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(DeleteMapping.class)).count()).isZero();
        }

        @Test
        @DisplayName("2 GET (unchanged)")
        void twoGet() {
            assertThat(Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count()).isEqualTo(2);
        }

        @Test
        @DisplayName("single dependency WorkflowObservabilityService")
        void singleDep() {
            var fields = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredFields())
                    .filter(f -> !f.isSynthetic()).toList();
            assertThat(fields).hasSize(1);
            assertThat(fields.get(0).getType()).isEqualTo(WorkflowObservabilityService.class);
        }
    }

    // ── E. Combined Endpoint Inventory ─────────────────────────────

    @Nested
    @DisplayName("E. Combined Endpoint Inventory (21 = 9 POST + 12 GET)")
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
        void total() {
            long post = countPost(AdminReviewController.class)
                    + countPost(AdminPipelineController.class)
                    + countPost(AdminWorkflowObservabilityController.class);
            long get = countGet(AdminReviewController.class)
                    + countGet(AdminPipelineController.class)
                    + countGet(AdminWorkflowObservabilityController.class);
            assertThat(post + get).isEqualTo(21);
        }

        @Test
        @DisplayName("no operational-control POST endpoint")
        void noOperationalControlPost() {
            // Check that no controller has a path containing 'operational' or 'control' with POST
            for (Class<?> ctrl : List.of(AdminReviewController.class,
                    AdminPipelineController.class,
                    AdminWorkflowObservabilityController.class)) {
                Arrays.stream(ctrl.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(PostMapping.class))
                        .forEach(m -> {
                            String[] paths = m.getAnnotation(PostMapping.class).value();
                            for (String p : paths) {
                                assertThat(p.toLowerCase())
                                        .doesNotContain("operational-control")
                                        .doesNotContain("execute-hint")
                                        .doesNotContain("run-control");
                            }
                        });
            }
        }

        private long countPost(Class<?> clazz) {
            return Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
        }

        private long countGet(Class<?> clazz) {
            return Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
        }
    }

    // ── F. DTO Forbidden Fields ────────────────────────────────────

    @Nested
    @DisplayName("F. DTO Forbidden Fields (all observability DTOs)")
    class DtoForbiddenFields {

        private static final List<String> FORBIDDEN = List.of(
                "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                "storageKey", "bucket", "objectPath", "rawPayload",
                "base64", "bytes", "secret", "apiKey", "token",
                "stackTrace", "exceptionClass",
                "publishStatus", "exportStatus", "publishedAt", "exportedAt");

        @Test
        @DisplayName("SafeWorkflowObservabilitySummary")
        void summaryNoForbidden() {
            Set<String> fields = fieldNames(SafeWorkflowObservabilitySummary.class);
            FORBIDDEN.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("SafeWorkflowDiagnosticItem")
        void diagnosticNoForbidden() {
            Set<String> fields = fieldNames(SafeWorkflowDiagnosticItem.class);
            FORBIDDEN.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("SafeWorkflowObservabilitySummary toString safe")
        void summaryToStringSafe() {
            String str = SafeWorkflowObservabilitySummary.empty().toString();
            FORBIDDEN.forEach(f -> assertThat(str).doesNotContain(f));
        }

        @Test
        @DisplayName("SafeWorkflowDiagnosticItem toString safe")
        void diagnosticToStringSafe() {
            var item = SafeWorkflowDiagnosticItem.builder()
                    .diagnosticType(SafeWorkflowDiagnosticType.STALE_REVIEW)
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(java.util.UUID.randomUUID())
                    .safeReasonCode("TEST")
                    .safeMessage("test")
                    .safeRecommendedHints(List.of())
                    .build();
            String str = item.toString();
            FORBIDDEN.forEach(f -> assertThat(str).doesNotContain(f));
        }

        private Set<String> fieldNames(Class<?> clazz) {
            return Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
        }
    }

    // ── G. Cross-Controller Isolation ─────────────────────────────

    @Nested
    @DisplayName("G. Cross-Controller Base Path Isolation")
    class ControllerIsolation {

        @Test
        @DisplayName("3 controllers, 3 unique base paths")
        void threeUniquePaths() {
            String r = AdminReviewController.class.getAnnotation(RequestMapping.class).value()[0];
            String p = AdminPipelineController.class.getAnnotation(RequestMapping.class).value()[0];
            String o = AdminWorkflowObservabilityController.class.getAnnotation(RequestMapping.class).value()[0];
            assertThat(Set.of(r, p, o)).hasSize(3);
        }

        @Test
        @DisplayName("observability path contains 'workflow-observability'")
        void observabilityPathCorrect() {
            String path = AdminWorkflowObservabilityController.class
                    .getAnnotation(RequestMapping.class).value()[0];
            assertThat(path).contains("workflow-observability");
        }
    }

    // ── H. DiagnosticType Forbidden Values ─────────────────────────

    @Nested
    @DisplayName("H. DiagnosticType Forbidden Values")
    class DiagnosticTypeForbidden {

        @Test
        @DisplayName("PUBLISH/EXPORT/CDN/PROVIDER/STORAGE yok")
        void noForbidden() {
            Set<String> values = Arrays.stream(SafeWorkflowDiagnosticType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("PUBLISH_FAILED", "EXPORT_FAILED", "CDN_FAILED",
                    "PROVIDER_ERROR", "STORAGE_ERROR", "SIGNED_URL_ERROR")
                    .forEach(f -> assertThat(values).doesNotContain(f));
        }
    }
}
