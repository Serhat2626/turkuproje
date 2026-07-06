package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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
 * FP-017 Task 02: Workflow Observability Service Foundation Tests.
 *
 * <p>Covers DTO safety, enum values, service structure, forbidden fields,
 * and endpoint inventory regression.</p>
 */
@DisplayName("FP-017 Task 02 — Workflow Observability Service Foundation")
class WorkflowObservabilityServiceTest {

    // ── A. SafeWorkflowDiagnosticType Enum ────────────────────────

    @Nested
    @DisplayName("A. SafeWorkflowDiagnosticType Enum")
    class DiagnosticTypeEnum {

        @Test
        @DisplayName("tam olarak 8 değer")
        void exactCount() {
            assertThat(SafeWorkflowDiagnosticType.values()).hasSize(8);
        }

        @Test
        @DisplayName("beklenen değerler mevcut")
        void expectedValues() {
            Set<String> values = Arrays.stream(SafeWorkflowDiagnosticType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(values).containsExactlyInAnyOrder(
                    "STALE_REVIEW", "PREVIEW_UNAVAILABLE",
                    "READINESS_UNAVAILABLE", "PIPELINE_UNAVAILABLE",
                    "REVISION_REQUESTED", "MISSING_TARGET",
                    "BLOCKED_INTERNAL_WORKFLOW", "DEGRADED_VISIBILITY");
        }

        @Test
        @DisplayName("yasaklı değerler yok")
        void noForbiddenValues() {
            Set<String> values = Arrays.stream(SafeWorkflowDiagnosticType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("PUBLISH_FAILED", "EXPORT_FAILED", "CDN_FAILED",
                    "PROVIDER_ERROR", "STORAGE_ERROR", "SIGNED_URL_ERROR")
                    .forEach(f -> assertThat(values).doesNotContain(f));
        }
    }

    // ── B. SafeOperationalControlHint Enum ─────────────────────────

    @Nested
    @DisplayName("B. SafeOperationalControlHint Enum")
    class ControlHintEnum {

        @Test
        @DisplayName("tam olarak 5 değer")
        void exactCount() {
            assertThat(SafeOperationalControlHint.values()).hasSize(5);
        }

        @Test
        @DisplayName("beklenen değerler mevcut")
        void expectedValues() {
            Set<String> values = Arrays.stream(SafeOperationalControlHint.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(values).containsExactlyInAnyOrder(
                    "REFRESH_OVERVIEW", "RECHECK_READINESS",
                    "VIEW_SAFE_PREVIEW", "REVIEW_PIPELINE_DETAIL",
                    "REQUEST_MANUAL_INVESTIGATION");
        }

        @Test
        @DisplayName("yasaklı değerler yok")
        void noForbiddenValues() {
            Set<String> values = Arrays.stream(SafeOperationalControlHint.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("PUBLISH", "EXPORT", "APPROVE", "AUTO_FIX",
                    "REGENERATE", "PROVIDER_RETRY", "STORAGE_RETRY",
                    "CREATE_PUBLIC_URL", "CREATE_SIGNED_URL", "CDN_DELIVERY")
                    .forEach(f -> assertThat(values).doesNotContain(f));
        }

        @Test
        @DisplayName("tüm hint'ler non-mutating isimli")
        void allNonMutating() {
            for (SafeOperationalControlHint hint : SafeOperationalControlHint.values()) {
                String name = hint.name();
                assertThat(name).as("Hint '%s' should not imply mutation", name)
                        .doesNotContain("DELETE")
                        .doesNotContain("UPDATE")
                        .doesNotContain("CREATE")
                        .doesNotContain("FORCE");
            }
        }
    }

    // ── C. SafeWorkflowDiagnosticItem DTO ──────────────────────────

    @Nested
    @DisplayName("C. SafeWorkflowDiagnosticItem DTO")
    class DiagnosticItemDto {

        private static final List<String> FORBIDDEN_FIELDS = List.of(
                "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                "storageKey", "bucket", "objectPath", "rawPayload",
                "base64", "bytes", "secret", "apiKey", "token",
                "stackTrace", "exceptionClass",
                "publishStatus", "exportStatus", "publishedAt", "exportedAt");

        @Test
        @DisplayName("forbidden fields yok")
        void noForbiddenFields() {
            Set<String> fields = Arrays.stream(SafeWorkflowDiagnosticItem.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("builder ile oluşturma safe varsayılanlar")
        void builderDefaults() {
            var item = SafeWorkflowDiagnosticItem.builder()
                    .diagnosticType(SafeWorkflowDiagnosticType.STALE_REVIEW)
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(UUID.randomUUID())
                    .pipelineStage(SafePipelineStage.REVIEW_PENDING)
                    .reviewState(ReviewState.NEEDS_REVIEW)
                    .safeReasonCode("TEST")
                    .safeMessage("Test message")
                    .safeRecommendedHints(List.of(SafeOperationalControlHint.REFRESH_OVERVIEW))
                    .updatedAt(LocalDateTime.now())
                    .build();
            assertThat(item.getDiagnosticType()).isEqualTo(SafeWorkflowDiagnosticType.STALE_REVIEW);
            assertThat(item.getSafeReasonCode()).isEqualTo("TEST");
            assertThat(item.getSafeRecommendedHints()).hasSize(1);
        }

        @Test
        @DisplayName("toString forbidden string içermez")
        void toString_noForbidden() {
            var item = SafeWorkflowDiagnosticItem.builder()
                    .diagnosticType(SafeWorkflowDiagnosticType.PREVIEW_UNAVAILABLE)
                    .targetType(ReviewTargetType.IMAGE_ASSET)
                    .targetId(UUID.randomUUID())
                    .pipelineStage(SafePipelineStage.IN_REVIEW)
                    .safeReasonCode("SAFE")
                    .safeMessage("Safe message")
                    .safeRecommendedHints(List.of())
                    .updatedAt(LocalDateTime.now())
                    .build();
            String str = item.toString();
            FORBIDDEN_FIELDS.forEach(f -> assertThat(str).doesNotContain(f));
        }

        @Test
        @DisplayName("tüm diagnostic type'lar DTO'da kullanılabilir")
        void allDiagnosticTypesUsable() {
            for (SafeWorkflowDiagnosticType type : SafeWorkflowDiagnosticType.values()) {
                var item = SafeWorkflowDiagnosticItem.builder()
                        .diagnosticType(type)
                        .targetType(ReviewTargetType.CONTENT_PROJECT)
                        .targetId(UUID.randomUUID())
                        .safeReasonCode("TEST")
                        .safeMessage("test")
                        .safeRecommendedHints(List.of())
                        .build();
                assertThat(item.getDiagnosticType()).isEqualTo(type);
            }
        }
    }

    // ── D. SafeWorkflowObservabilitySummary DTO ────────────────────

    @Nested
    @DisplayName("D. SafeWorkflowObservabilitySummary DTO")
    class SummaryDto {

        private static final List<String> FORBIDDEN_FIELDS = List.of(
                "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                "storageKey", "bucket", "objectPath", "rawPayload",
                "base64", "bytes", "secret", "apiKey", "token",
                "stackTrace", "exceptionClass",
                "publishStatus", "exportStatus", "publishedAt", "exportedAt");

        @Test
        @DisplayName("forbidden fields yok")
        void noForbiddenFields() {
            Set<String> fields = Arrays.stream(SafeWorkflowObservabilitySummary.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("empty() factory safe sonuç döner")
        void emptyFactory() {
            var summary = SafeWorkflowObservabilitySummary.empty();
            assertThat(summary.getCountsByReviewState()).isEmpty();
            assertThat(summary.getCountsByPipelineStage()).isEmpty();
            assertThat(summary.getCountsByReadinessAvailability()).isEmpty();
            assertThat(summary.getPreviewAvailableCount()).isZero();
            assertThat(summary.getPreviewUnavailableCount()).isZero();
            assertThat(summary.getRevisionRequestedCount()).isZero();
            assertThat(summary.getUnavailableCount()).isZero();
            assertThat(summary.getDiagnostics()).isEmpty();
            assertThat(summary.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("empty() with warnings")
        void emptyWithWarnings() {
            var summary = SafeWorkflowObservabilitySummary.empty("WARN1", "WARN2");
            assertThat(summary.getSafeWarnings()).containsExactly("WARN1", "WARN2");
        }

        @Test
        @DisplayName("builder ile oluşturma safe")
        void builderSafe() {
            var summary = SafeWorkflowObservabilitySummary.builder()
                    .countsByReviewState(Map.of(ReviewState.DRAFT, 3, ReviewState.IN_REVIEW, 2))
                    .countsByPipelineStage(Map.of(SafePipelineStage.GENERATED, 3))
                    .countsByReadinessAvailability(Map.of())
                    .previewAvailableCount(1)
                    .previewUnavailableCount(4)
                    .revisionRequestedCount(0)
                    .unavailableCount(1)
                    .diagnostics(List.of())
                    .safeWarnings(List.of())
                    .updatedAt(LocalDateTime.now())
                    .build();
            assertThat(summary.getPreviewAvailableCount()).isEqualTo(1);
            assertThat(summary.getPreviewUnavailableCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("toString forbidden string içermez")
        void toString_noForbidden() {
            String str = SafeWorkflowObservabilitySummary.empty().toString();
            FORBIDDEN_FIELDS.forEach(f -> assertThat(str).doesNotContain(f));
        }
    }

    // ── E. Service Structure ──────────────────────────────────────

    @Nested
    @DisplayName("E. Service Structure")
    class ServiceStructure {

        @Test
        @DisplayName("@Service annotation mevcut")
        void hasServiceAnnotation() {
            assertThat(WorkflowObservabilityService.class.getAnnotation(
                    org.springframework.stereotype.Service.class)).isNotNull();
        }

        @Test
        @DisplayName("controller annotation yok")
        void noControllerAnnotation() {
            assertThat(WorkflowObservabilityService.class.getAnnotation(
                    org.springframework.web.bind.annotation.RestController.class)).isNull();
            assertThat(WorkflowObservabilityService.class.getAnnotation(
                    org.springframework.stereotype.Controller.class)).isNull();
        }

        @Test
        @DisplayName("ReviewApplicationService bağımlılığı yok")
        void noReviewApplicationServiceDep() {
            var paramTypes = Arrays.stream(WorkflowObservabilityService.class.getDeclaredConstructors())
                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
                    .collect(Collectors.toSet());
            assertThat(paramTypes).doesNotContain(ReviewApplicationService.class);
        }

        @Test
        @DisplayName("PipelineAggregationService bağımlılığı var")
        void hasPipelineServiceDep() {
            var paramTypes = Arrays.stream(WorkflowObservabilityService.class.getDeclaredConstructors())
                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
                    .collect(Collectors.toSet());
            assertThat(paramTypes).contains(PipelineAggregationService.class);
        }

        @Test
        @DisplayName("constructor 2 parametre alır")
        void constructorParams() {
            var constructors = WorkflowObservabilityService.class.getDeclaredConstructors();
            assertThat(constructors).hasSize(1);
            assertThat(constructors[0].getParameterCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("public metotlar @Transactional(readOnly=true)")
        void publicMethods_readOnlyTransactional() {
            for (var m : WorkflowObservabilityService.class.getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers())) {
                    var tx = m.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
                    if (tx != null) {
                        assertThat(tx.readOnly())
                                .as("Method %s should be readOnly", m.getName())
                                .isTrue();
                    }
                }
            }
        }
    }

    // ── F. Target Type Coverage ────────────────────────────────────

    @Nested
    @DisplayName("F. Target Type Coverage")
    class TargetTypeCoverage {

        @Test
        @DisplayName("tüm target tipleri diagnostic'te kullanılabilir")
        void allTypesInDiagnostic() {
            for (ReviewTargetType t : ReviewTargetType.values()) {
                var item = SafeWorkflowDiagnosticItem.builder()
                        .diagnosticType(SafeWorkflowDiagnosticType.MISSING_TARGET)
                        .targetType(t)
                        .targetId(UUID.randomUUID())
                        .safeReasonCode("TEST")
                        .safeMessage("test")
                        .safeRecommendedHints(List.of())
                        .build();
                assertThat(item.getTargetType()).isEqualTo(t);
            }
        }

        @Test
        @DisplayName("tüm pipeline stage'ler diagnostic'te kullanılabilir")
        void allStagesInDiagnostic() {
            for (SafePipelineStage s : SafePipelineStage.values()) {
                var item = SafeWorkflowDiagnosticItem.builder()
                        .diagnosticType(SafeWorkflowDiagnosticType.DEGRADED_VISIBILITY)
                        .targetType(ReviewTargetType.STORYBOARD)
                        .targetId(UUID.randomUUID())
                        .pipelineStage(s)
                        .safeReasonCode("TEST")
                        .safeMessage("test")
                        .safeRecommendedHints(List.of())
                        .build();
                assertThat(item.getPipelineStage()).isEqualTo(s);
            }
        }
    }

    // ── G. Endpoint Inventory Regression ───────────────────────────

    @Nested
    @DisplayName("G. Endpoint Inventory Regression")
    class EndpointInventory {

        @Test
        @DisplayName("AdminReviewController 9 POST")
        void reviewController_9Post() {
            long count = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isEqualTo(9);
        }

        @Test
        @DisplayName("AdminReviewController 8 GET")
        void reviewController_8Get() {
            long count = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(count).isEqualTo(8);
        }

        @Test
        @DisplayName("AdminPipelineController 2 GET")
        void pipelineController_2Get() {
            long count = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("toplam 19 endpoint (9 POST + 10 GET)")
        void totalEndpoints_19() {
            long totalPost = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count()
                    + Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long totalGet = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count()
                    + Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(totalPost + totalGet).isEqualTo(19);
        }
    }

    // ── H. Forbidden Strings in Enums ─────────────────────────────

    @Nested
    @DisplayName("H. Forbidden Strings in Enum Names")
    class ForbiddenEnumStrings {

        @Test
        @DisplayName("DiagnosticType publish/export/provider/storage yok")
        void diagnosticType_noForbidden() {
            for (SafeWorkflowDiagnosticType t : SafeWorkflowDiagnosticType.values()) {
                String name = t.name();
                assertThat(name).doesNotContain("PUBLISH").doesNotContain("EXPORT")
                        .doesNotContain("CDN").doesNotContain("SIGNED_URL");
            }
        }

        @Test
        @DisplayName("ControlHint publish/export/approve/auto yok")
        void controlHint_noForbidden() {
            for (SafeOperationalControlHint h : SafeOperationalControlHint.values()) {
                String name = h.name();
                assertThat(name).doesNotContain("PUBLISH").doesNotContain("EXPORT")
                        .doesNotContain("APPROVE").doesNotContain("AUTO_FIX")
                        .doesNotContain("REGENERATE").doesNotContain("CDN");
            }
        }
    }

    // ── I. Summary Count Types ─────────────────────────────────────

    @Nested
    @DisplayName("I. Summary Count Types")
    class SummaryCountTypes {

        @Test
        @DisplayName("countsByReviewState Map tipinde")
        void countsByReviewState_isMap() throws NoSuchFieldException {
            var field = SafeWorkflowObservabilitySummary.class.getDeclaredField("countsByReviewState");
            assertThat(field.getType()).isEqualTo(Map.class);
        }

        @Test
        @DisplayName("countsByPipelineStage Map tipinde")
        void countsByPipelineStage_isMap() throws NoSuchFieldException {
            var field = SafeWorkflowObservabilitySummary.class.getDeclaredField("countsByPipelineStage");
            assertThat(field.getType()).isEqualTo(Map.class);
        }

        @Test
        @DisplayName("countsByReadinessAvailability Map tipinde")
        void countsByReadiness_isMap() throws NoSuchFieldException {
            var field = SafeWorkflowObservabilitySummary.class.getDeclaredField("countsByReadinessAvailability");
            assertThat(field.getType()).isEqualTo(Map.class);
        }

        @Test
        @DisplayName("diagnostics List tipinde")
        void diagnostics_isList() throws NoSuchFieldException {
            var field = SafeWorkflowObservabilitySummary.class.getDeclaredField("diagnostics");
            assertThat(field.getType()).isEqualTo(List.class);
        }
    }

    // ── J. Degraded Behavior ──────────────────────────────────────

    @Nested
    @DisplayName("J. Degraded/Partial Behavior")
    class DegradedBehavior {

        @Test
        @DisplayName("empty summary safe sonuç döner")
        void emptySummary_safe() {
            var summary = SafeWorkflowObservabilitySummary.empty();
            assertThat(summary.getCountsByReviewState()).isNotNull();
            assertThat(summary.getDiagnostics()).isNotNull();
            assertThat(summary.getSafeWarnings()).isNotNull();
        }

        @Test
        @DisplayName("empty summary with warning safe")
        void emptySummary_withWarning() {
            var summary = SafeWorkflowObservabilitySummary.empty("SOURCE_DEGRADED");
            assertThat(summary.getSafeWarnings()).contains("SOURCE_DEGRADED");
            assertThat(summary.getUnavailableCount()).isZero();
        }

        @Test
        @DisplayName("diagnostic item null reviewState safe")
        void diagnosticItem_nullReviewState() {
            var item = SafeWorkflowDiagnosticItem.builder()
                    .diagnosticType(SafeWorkflowDiagnosticType.MISSING_TARGET)
                    .targetType(ReviewTargetType.PACKAGE)
                    .targetId(UUID.randomUUID())
                    .reviewState(null)
                    .safeReasonCode("MISSING")
                    .safeMessage("Target bulunamadı")
                    .safeRecommendedHints(List.of())
                    .build();
            assertThat(item.getReviewState()).isNull();
            assertThat(item.getSafeReasonCode()).isEqualTo("MISSING");
        }
    }
}
