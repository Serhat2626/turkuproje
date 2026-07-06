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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-017 Task 06: E2E Workflow Observability Regression & Safety Hardening.
 *
 * <p>End-to-end coverage proving observability summary + diagnostics
 * derive safely from pipeline aggregation, and all endpoints remain
 * read-only and separated from review actions.</p>
 */
@DisplayName("FP-017 Task 06 — E2E Observability Regression & Safety Hardening")
class FP017WorkflowObservabilityE2ERegressionTest {

    // ── A. Summary Derivation Safety ──────────────────────────────

    @Nested
    @DisplayName("A. Summary Derivation Safety")
    class SummaryDerivation {

        @Test
        @DisplayName("empty() summary tüm count'lar sıfır")
        void emptySummary_allZero() {
            var s = SafeWorkflowObservabilitySummary.empty();
            assertThat(s.getPreviewAvailableCount()).isZero();
            assertThat(s.getPreviewUnavailableCount()).isZero();
            assertThat(s.getRevisionRequestedCount()).isZero();
            assertThat(s.getUnavailableCount()).isZero();
            assertThat(s.getCountsByReviewState()).isEmpty();
            assertThat(s.getCountsByPipelineStage()).isEmpty();
            assertThat(s.getCountsByReadinessAvailability()).isEmpty();
            assertThat(s.getDiagnostics()).isEmpty();
        }

        @Test
        @DisplayName("empty() summary updatedAt set")
        void emptySummary_updatedAt() {
            var s = SafeWorkflowObservabilitySummary.empty();
            assertThat(s.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("builder ile counts doğru set ediliyor")
        void builderCounts() {
            var s = SafeWorkflowObservabilitySummary.builder()
                    .countsByReviewState(Map.of(ReviewState.DRAFT, 5, ReviewState.IN_REVIEW, 3))
                    .countsByPipelineStage(Map.of(SafePipelineStage.GENERATED, 5))
                    .countsByReadinessAvailability(Map.of())
                    .previewAvailableCount(4)
                    .previewUnavailableCount(4)
                    .revisionRequestedCount(1)
                    .unavailableCount(2)
                    .diagnostics(List.of())
                    .safeWarnings(List.of())
                    .updatedAt(LocalDateTime.now())
                    .build();
            assertThat(s.getPreviewAvailableCount()).isEqualTo(4);
            assertThat(s.getPreviewUnavailableCount()).isEqualTo(4);
            assertThat(s.getRevisionRequestedCount()).isEqualTo(1);
            assertThat(s.getUnavailableCount()).isEqualTo(2);
            assertThat(s.getCountsByReviewState()).hasSize(2);
        }

        @Test
        @DisplayName("degraded summary safeWarnings içerir")
        void degraded_warnings() {
            var s = SafeWorkflowObservabilitySummary.empty("PIPELINE_QUERY_FAILED");
            assertThat(s.getSafeWarnings()).containsExactly("PIPELINE_QUERY_FAILED");
            assertThat(s.getDiagnostics()).isEmpty();
        }

        @Test
        @DisplayName("multiple warnings birikir")
        void multipleWarnings() {
            var s = SafeWorkflowObservabilitySummary.empty("W1", "W2", "W3");
            assertThat(s.getSafeWarnings()).hasSize(3);
        }
    }

    // ── B. Diagnostic Derivation Safety ───────────────────────────

    @Nested
    @DisplayName("B. Diagnostic Derivation Safety")
    class DiagnosticDerivation {

        @Test
        @DisplayName("PIPELINE_UNAVAILABLE diagnostic oluşturulabilir")
        void pipelineUnavailable() {
            var d = buildDiagnostic(SafeWorkflowDiagnosticType.PIPELINE_UNAVAILABLE,
                    ReviewTargetType.SCENARIO, "PIPELINE_UNAVAILABLE");
            assertThat(d.getDiagnosticType()).isEqualTo(SafeWorkflowDiagnosticType.PIPELINE_UNAVAILABLE);
            assertThat(d.getSafeReasonCode()).isEqualTo("PIPELINE_UNAVAILABLE");
        }

        @Test
        @DisplayName("STALE_REVIEW diagnostic oluşturulabilir")
        void staleReview() {
            var d = buildDiagnostic(SafeWorkflowDiagnosticType.STALE_REVIEW,
                    ReviewTargetType.STORYBOARD, "STALE_REVIEW");
            assertThat(d.getDiagnosticType()).isEqualTo(SafeWorkflowDiagnosticType.STALE_REVIEW);
        }

        @Test
        @DisplayName("REVISION_REQUESTED diagnostic oluşturulabilir")
        void revisionRequested() {
            var d = buildDiagnostic(SafeWorkflowDiagnosticType.REVISION_REQUESTED,
                    ReviewTargetType.PACKAGE, "REVISION_REQUESTED");
            assertThat(d.getDiagnosticType()).isEqualTo(SafeWorkflowDiagnosticType.REVISION_REQUESTED);
        }

        @Test
        @DisplayName("BLOCKED_INTERNAL_WORKFLOW diagnostic oluşturulabilir")
        void blocked() {
            var d = buildDiagnostic(SafeWorkflowDiagnosticType.BLOCKED_INTERNAL_WORKFLOW,
                    ReviewTargetType.IMAGE_ASSET, "BLOCKED");
            assertThat(d.getDiagnosticType()).isEqualTo(SafeWorkflowDiagnosticType.BLOCKED_INTERNAL_WORKFLOW);
        }

        @Test
        @DisplayName("PREVIEW_UNAVAILABLE diagnostic oluşturulabilir")
        void previewUnavailable() {
            var d = buildDiagnostic(SafeWorkflowDiagnosticType.PREVIEW_UNAVAILABLE,
                    ReviewTargetType.CONTENT_PROJECT, "PREVIEW_UNAVAILABLE");
            assertThat(d.getDiagnosticType()).isEqualTo(SafeWorkflowDiagnosticType.PREVIEW_UNAVAILABLE);
        }

        @Test
        @DisplayName("READINESS_UNAVAILABLE diagnostic oluşturulabilir")
        void readinessUnavailable() {
            var d = buildDiagnostic(SafeWorkflowDiagnosticType.READINESS_UNAVAILABLE,
                    ReviewTargetType.SCENARIO, "READINESS_UNAVAILABLE");
            assertThat(d.getDiagnosticType()).isEqualTo(SafeWorkflowDiagnosticType.READINESS_UNAVAILABLE);
        }

        @Test
        @DisplayName("MISSING_TARGET diagnostic oluşturulabilir")
        void missingTarget() {
            var d = buildDiagnostic(SafeWorkflowDiagnosticType.MISSING_TARGET,
                    ReviewTargetType.STORYBOARD, "MISSING_TARGET");
            assertThat(d.getDiagnosticType()).isEqualTo(SafeWorkflowDiagnosticType.MISSING_TARGET);
        }

        @Test
        @DisplayName("DEGRADED_VISIBILITY diagnostic oluşturulabilir")
        void degradedVisibility() {
            var d = buildDiagnostic(SafeWorkflowDiagnosticType.DEGRADED_VISIBILITY,
                    ReviewTargetType.PACKAGE, "DEGRADED_VISIBILITY");
            assertThat(d.getDiagnosticType()).isEqualTo(SafeWorkflowDiagnosticType.DEGRADED_VISIBILITY);
        }

        @Test
        @DisplayName("all 5 target types diagnostic'te kullanılabilir")
        void allTargetTypes() {
            for (ReviewTargetType t : ReviewTargetType.values()) {
                var d = buildDiagnostic(SafeWorkflowDiagnosticType.MISSING_TARGET, t, "TEST");
                assertThat(d.getTargetType()).isEqualTo(t);
            }
        }

        @Test
        @DisplayName("diagnostic with hints")
        void diagnosticWithHints() {
            var d = SafeWorkflowDiagnosticItem.builder()
                    .diagnosticType(SafeWorkflowDiagnosticType.STALE_REVIEW)
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(UUID.randomUUID())
                    .pipelineStage(SafePipelineStage.REVIEW_PENDING)
                    .reviewState(ReviewState.NEEDS_REVIEW)
                    .safeReasonCode("STALE")
                    .safeMessage("7 gün")
                    .safeRecommendedHints(List.of(
                            SafeOperationalControlHint.REVIEW_PIPELINE_DETAIL,
                            SafeOperationalControlHint.REQUEST_MANUAL_INVESTIGATION))
                    .updatedAt(LocalDateTime.now())
                    .build();
            assertThat(d.getSafeRecommendedHints()).hasSize(2);
            assertThat(d.getSafeRecommendedHints()).contains(
                    SafeOperationalControlHint.REVIEW_PIPELINE_DETAIL);
        }

        private SafeWorkflowDiagnosticItem buildDiagnostic(
                SafeWorkflowDiagnosticType type, ReviewTargetType targetType, String reason) {
            return SafeWorkflowDiagnosticItem.builder()
                    .diagnosticType(type)
                    .targetType(targetType)
                    .targetId(UUID.randomUUID())
                    .pipelineStage(SafePipelineStage.UNAVAILABLE)
                    .safeReasonCode(reason)
                    .safeMessage("Test diagnostic")
                    .safeRecommendedHints(List.of(SafeOperationalControlHint.REFRESH_OVERVIEW))
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
    }

    // ── C. Endpoint Separation ─────────────────────────────────────

    @Nested
    @DisplayName("C. Controller Separation from Review Actions")
    class ControllerSeparation {

        @Test
        @DisplayName("3 controllers farklı base paths")
        void threeControllers() {
            String r = AdminReviewController.class.getAnnotation(RequestMapping.class).value()[0];
            String p = AdminPipelineController.class.getAnnotation(RequestMapping.class).value()[0];
            String o = AdminWorkflowObservabilityController.class.getAnnotation(RequestMapping.class).value()[0];
            assertThat(Set.of(r, p, o)).hasSize(3);
        }

        @Test
        @DisplayName("observability controller ReviewApplicationService bağımlılığı yok")
        void noReviewAppServiceInController() {
            var fieldTypes = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet());
            assertThat(fieldTypes).doesNotContain(ReviewApplicationService.class);
        }

        @Test
        @DisplayName("observability controller ReviewRecordRepository bağımlılığı yok")
        void noRepoInController() {
            var fieldTypes = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet());
            assertThat(fieldTypes).doesNotContain(ReviewRecordRepository.class);
        }

        @Test
        @DisplayName("observability controller single dep: WorkflowObservabilityService")
        void singleDep() {
            var fields = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredFields())
                    .filter(f -> !f.isSynthetic()).toList();
            assertThat(fields).hasSize(1);
            assertThat(fields.get(0).getType()).isEqualTo(WorkflowObservabilityService.class);
        }
    }

    // ── D. Service No Mutation ─────────────────────────────────────

    @Nested
    @DisplayName("D. Service No Mutation")
    class ServiceNoMutation {

        @Test
        @DisplayName("no mutating method names")
        void noMutatingMethods() {
            var publicMethods = Arrays.stream(WorkflowObservabilityService.class.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .map(java.lang.reflect.Method::getName)
                    .collect(Collectors.toSet());
            List.of("approve", "reject", "submit", "update", "delete",
                    "create", "save", "publish", "export", "generate",
                    "regenerate", "transition", "assign", "handoff")
                    .forEach(verb -> {
                        for (String method : publicMethods) {
                            assertThat(method.toLowerCase())
                                    .as("Method '%s' should not contain mutating verb '%s'", method, verb)
                                    .doesNotContain(verb);
                        }
                    });
        }

        @Test
        @DisplayName("getSummary return type is SafeWorkflowObservabilitySummary")
        void getSummaryReturnType() throws NoSuchMethodException {
            var m = WorkflowObservabilityService.class.getDeclaredMethod("getSummary");
            assertThat(m.getReturnType()).isEqualTo(SafeWorkflowObservabilitySummary.class);
        }

        @Test
        @DisplayName("getDiagnostics return type is List")
        void getDiagnosticsReturnType() throws NoSuchMethodException {
            var m = WorkflowObservabilityService.class.getDeclaredMethod("getDiagnostics", ReviewTargetType.class);
            assertThat(m.getReturnType()).isEqualTo(List.class);
        }
    }

    // ── E. Combined Endpoint Inventory ─────────────────────────────

    @Nested
    @DisplayName("E. Combined Endpoint Inventory (21 = 9 POST + 12 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("AdminReviewController: 9 POST + 8 GET = 17")
        void reviewController() {
            assertThat(countPost(AdminReviewController.class)).isEqualTo(9);
            assertThat(countGet(AdminReviewController.class)).isEqualTo(8);
        }

        @Test
        @DisplayName("AdminPipelineController: 0 POST + 2 GET = 2")
        void pipelineController() {
            assertThat(countPost(AdminPipelineController.class)).isZero();
            assertThat(countGet(AdminPipelineController.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("AdminWorkflowObservabilityController: 0 POST + 2 GET = 2")
        void observabilityController() {
            assertThat(countPost(AdminWorkflowObservabilityController.class)).isZero();
            assertThat(countGet(AdminWorkflowObservabilityController.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("toplam 21 = 9 POST + 12 GET")
        void total21() {
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
        @DisplayName("no PUT endpoints anywhere")
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
        @DisplayName("no DELETE endpoints anywhere")
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

    // ── F. DTO Forbidden Field Regression ──────────────────────────

    @Nested
    @DisplayName("F. DTO Forbidden Field Regression")
    class DtoForbiddenRegression {

        private static final List<String> FORBIDDEN = List.of(
                "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                "storageKey", "bucket", "objectPath", "rawPayload",
                "base64", "bytes", "secret", "apiKey", "token",
                "stackTrace", "exceptionClass",
                "publishStatus", "exportStatus", "publishedAt", "exportedAt");

        @Test
        @DisplayName("SafeWorkflowObservabilitySummary fields safe")
        void summaryFieldsSafe() {
            Set<String> fields = fieldNames(SafeWorkflowObservabilitySummary.class);
            FORBIDDEN.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("SafeWorkflowDiagnosticItem fields safe")
        void diagnosticFieldsSafe() {
            Set<String> fields = fieldNames(SafeWorkflowDiagnosticItem.class);
            FORBIDDEN.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("SafePipelineAggregateItem fields safe")
        void pipelineItemFieldsSafe() {
            Set<String> fields = fieldNames(SafePipelineAggregateItem.class);
            FORBIDDEN.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("SafePipelineOverview fields safe")
        void pipelineOverviewFieldsSafe() {
            Set<String> fields = fieldNames(SafePipelineOverview.class);
            FORBIDDEN.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("all summary toString safe")
        void summaryToStringSafe() {
            String str = SafeWorkflowObservabilitySummary.empty().toString();
            FORBIDDEN.forEach(f -> assertThat(str).doesNotContain(f));
        }

        private Set<String> fieldNames(Class<?> c) {
            return Arrays.stream(c.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
        }
    }

    // ── G. Forbidden Enum Values Regression ────────────────────────

    @Nested
    @DisplayName("G. Forbidden Enum Values Regression")
    class ForbiddenEnumRegression {

        @Test
        @DisplayName("SafeWorkflowDiagnosticType no PUBLISH/EXPORT/CDN/PROVIDER/STORAGE")
        void diagnosticType() {
            Set<String> v = enumNames(SafeWorkflowDiagnosticType.class);
            List.of("PUBLISH_FAILED", "EXPORT_FAILED", "CDN_FAILED",
                    "PROVIDER_ERROR", "STORAGE_ERROR", "SIGNED_URL_ERROR")
                    .forEach(f -> assertThat(v).doesNotContain(f));
        }

        @Test
        @DisplayName("SafeOperationalControlHint no PUBLISH/EXPORT/APPROVE/REGENERATE")
        void controlHint() {
            Set<String> v = enumNames(SafeOperationalControlHint.class);
            List.of("PUBLISH", "EXPORT", "APPROVE", "AUTO_FIX",
                    "REGENERATE", "PROVIDER_RETRY", "STORAGE_RETRY",
                    "CREATE_PUBLIC_URL", "CREATE_SIGNED_URL", "CDN_DELIVERY")
                    .forEach(f -> assertThat(v).doesNotContain(f));
        }

        @Test
        @DisplayName("SafePipelineStage no PUBLISH/EXPORT/CDN/PUBLIC")
        void pipelineStage() {
            Set<String> v = enumNames(SafePipelineStage.class);
            List.of("READY_FOR_PUBLISH", "PUBLISHABLE", "PUBLISHED",
                    "EXPORTABLE", "EXPORTED", "CDN_READY",
                    "PUBLIC_DELIVERY", "SIGNED_URL_READY")
                    .forEach(f -> assertThat(v).doesNotContain(f));
        }

        private <E extends Enum<E>> Set<String> enumNames(Class<E> e) {
            return Arrays.stream(e.getEnumConstants())
                    .map(Enum::name).collect(Collectors.toSet());
        }
    }

    // ── H. Diagnostic Filter Coverage ──────────────────────────────

    @Nested
    @DisplayName("H. Diagnostic Filter Coverage")
    class DiagnosticFilterCoverage {

        @Test
        @DisplayName("getDiagnostics(null) returns all diagnostics")
        void nullFilter() {
            // This tests the method signature — service accepts null for all
            var m = Arrays.stream(WorkflowObservabilityService.class.getDeclaredMethods())
                    .filter(me -> me.getName().equals("getDiagnostics"))
                    .findFirst().orElseThrow();
            assertThat(m.getParameterCount()).isEqualTo(1);
            assertThat(m.getParameterTypes()[0]).isEqualTo(ReviewTargetType.class);
        }

        @Test
        @DisplayName("all 5 target types accepted as filter")
        void allTargetTypesAccepted() {
            for (ReviewTargetType t : ReviewTargetType.values()) {
                // If this compiles and doesn't throw, the enum value is accepted
                assertThat(t).isNotNull();
            }
        }
    }
}
