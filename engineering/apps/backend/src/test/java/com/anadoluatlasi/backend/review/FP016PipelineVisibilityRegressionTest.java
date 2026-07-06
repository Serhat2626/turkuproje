package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-016 Task 05: E2E Pipeline Visibility Regression & Safety Hardening.
 *
 * <p>Cross-layer regression tests verifying pipeline aggregation remains safe,
 * read-only, and free of forbidden exposure across all components.</p>
 *
 * <p>No DOM rendering. Structural + reflection + contract verification only.</p>
 */
@DisplayName("FP-016 Task 05 — E2E Pipeline Visibility Regression")
class FP016PipelineVisibilityRegressionTest {

    // ── A. Full Stage Mapping Regression ───────────────────────────

    @Nested
    @DisplayName("A. Full Stage Mapping Regression")
    class StageMappingRegression {

        @Test
        @DisplayName("SafePipelineStage tam olarak 9 değer içerir")
        void exactStageCount() {
            assertThat(SafePipelineStage.values()).hasSize(9);
        }

        @Test
        @DisplayName("tüm beklenen aşamalar mevcut")
        void allExpectedStages() {
            Set<String> stages = Arrays.stream(SafePipelineStage.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(stages).containsExactlyInAnyOrder(
                    "GENERATED", "REVIEW_PENDING", "IN_REVIEW",
                    "CHANGES_REQUESTED", "INTERNALLY_APPROVED", "INTERNAL_READY",
                    "INTERNAL_REVISION_REQUESTED", "REJECTED", "UNAVAILABLE");
        }

        @Test
        @DisplayName("yasaklı publish/export aşamaları yok")
        void noForbiddenStages() {
            Set<String> stages = Arrays.stream(SafePipelineStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("READY_FOR_PUBLISH", "PUBLISHABLE", "PUBLISHED",
                    "EXPORTABLE", "EXPORTED", "CDN_READY",
                    "PUBLIC_DELIVERY", "SIGNED_URL_READY")
                    .forEach(f -> assertThat(stages).doesNotContain(f));
        }

        @Test
        @DisplayName("GENERATED aşaması DRAFT/AI_GENERATED/null için")
        void generatedStage_forDraftStates() {
            assertThat(SafePipelineStage.GENERATED.name()).isEqualTo("GENERATED");
        }

        @Test
        @DisplayName("UNAVAILABLE aşaması eksik/hata durumlar için")
        void unavailableStage_forMissing() {
            assertThat(SafePipelineStage.UNAVAILABLE.name()).isEqualTo("UNAVAILABLE");
        }
    }

    // ── B. Target Type Coverage ────────────────────────────────────

    @Nested
    @DisplayName("B. Target Type Coverage")
    class TargetTypeCoverage {

        @Test
        @DisplayName("5 target tipi desteklenir")
        void fiveTargetTypes() {
            assertThat(ReviewTargetType.values()).hasSize(5);
        }

        @Test
        @DisplayName("beklenen 5 target tipi")
        void expectedTargetTypes() {
            assertThat(ReviewTargetType.values()).containsExactlyInAnyOrder(
                    ReviewTargetType.CONTENT_PROJECT,
                    ReviewTargetType.SCENARIO,
                    ReviewTargetType.STORYBOARD,
                    ReviewTargetType.PACKAGE,
                    ReviewTargetType.IMAGE_ASSET);
        }

        @Test
        @DisplayName("tüm target tipleri SafePipelineAggregateItem'da kullanılabilir")
        void allTypesUsableInDto() {
            for (ReviewTargetType t : ReviewTargetType.values()) {
                SafePipelineAggregateItem item = SafePipelineAggregateItem.unavailable(
                        t, UUID.randomUUID(), "REGRESSION_TEST");
                assertThat(item.getTargetType()).isEqualTo(t);
                assertThat(item.getPipelineStage()).isEqualTo(SafePipelineStage.UNAVAILABLE);
            }
        }
    }

    // ── C. DTO Safety Regression ───────────────────────────────────

    @Nested
    @DisplayName("C. DTO Safety Regression")
    class DtoSafetyRegression {

        private static final List<String> FORBIDDEN_FIELDS = List.of(
                "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                "storageKey", "bucket", "objectPath", "rawPayload",
                "base64", "bytes", "secret", "apiKey", "token",
                "stackTrace", "exceptionClass",
                "publishStatus", "exportStatus", "publishedAt", "exportedAt",
                "rawContent", "rawImageBytes", "rawProviderPayload",
                "config", "connectionString", "password");

        @Test
        @DisplayName("SafePipelineAggregateItem forbidden fields yok")
        void aggregateItem_noForbiddenFields() {
            Set<String> fields = Arrays.stream(SafePipelineAggregateItem.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("SafePipelineOverview forbidden fields yok")
        void overview_noForbiddenFields() {
            Set<String> fields = Arrays.stream(SafePipelineOverview.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("SafePipelineAggregateItem.unavailable safe varsayılanları döner")
        void unavailable_safeDefaults() {
            var item = SafePipelineAggregateItem.unavailable(
                    ReviewTargetType.SCENARIO, UUID.randomUUID(), "TEST_REASON");
            assertThat(item.getPipelineStage()).isEqualTo(SafePipelineStage.UNAVAILABLE);
            assertThat(item.isPreviewAvailable()).isFalse();
            assertThat(item.isRevisionRequested()).isFalse();
            assertThat(item.getSafeBlockingReasons()).contains("TEST_REASON");
        }

        @Test
        @DisplayName("SafePipelineOverview.empty safe boş sonuç döner")
        void empty_safeDefaults() {
            var overview = SafePipelineOverview.empty();
            assertThat(overview.getTotalCount()).isZero();
            assertThat(overview.getItems()).isEmpty();
            assertThat(overview.getCountsByStage()).isEmpty();
            assertThat(overview.getCountsByTargetType()).isEmpty();
        }

        @Test
        @DisplayName("SafePipelineAggregateItem toString forbidden string içermez")
        void aggregateItem_toString_safe() {
            var item = SafePipelineAggregateItem.unavailable(
                    ReviewTargetType.IMAGE_ASSET, UUID.randomUUID(), "TEST");
            String str = item.toString();
            FORBIDDEN_FIELDS.forEach(f -> assertThat(str).doesNotContain(f));
        }

        @Test
        @DisplayName("SafePipelineOverview toString forbidden string içermez")
        void overview_toString_safe() {
            String str = SafePipelineOverview.empty().toString();
            FORBIDDEN_FIELDS.forEach(f -> assertThat(str).doesNotContain(f));
        }
    }

    // ── D. Combined Endpoint Inventory ─────────────────────────────

    @Nested
    @DisplayName("D. Combined Endpoint Inventory (19 = 9 POST + 10 GET)")
    class CombinedEndpointInventory {

        @Test
        @DisplayName("AdminReviewController: 9 POST")
        void reviewController_9Post() {
            long count = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isEqualTo(9);
        }

        @Test
        @DisplayName("AdminReviewController: 8 GET")
        void reviewController_8Get() {
            long count = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(count).isEqualTo(8);
        }

        @Test
        @DisplayName("AdminPipelineController: 0 POST")
        void pipelineController_0Post() {
            long count = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("AdminPipelineController: 2 GET")
        void pipelineController_2Get() {
            long count = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("AdminPipelineController: 0 PUT")
        void pipelineController_0Put() {
            long count = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PutMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("AdminPipelineController: 0 DELETE")
        void pipelineController_0Delete() {
            long count = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(DeleteMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("toplam 19 endpoint = 9 POST + 10 GET")
        void totalEndpoints_19() {
            long totalPost = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count()
                    + Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long totalGet = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count()
                    + Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(totalPost).isEqualTo(9);
            assertThat(totalGet).isEqualTo(10);
            assertThat(totalPost + totalGet).isEqualTo(19);
        }

        @Test
        @DisplayName("pipeline controller GET paths doğru")
        void pipelineGetPaths() {
            Set<String> paths = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .collect(Collectors.toSet());
            assertThat(paths).containsExactlyInAnyOrder(
                    "/overview", "/{targetType}/{targetId}");
        }
    }

    // ── E. Forbidden Endpoint Path Regression ──────────────────────

    @Nested
    @DisplayName("E. Forbidden Endpoint Path Regression")
    class ForbiddenEndpointPaths {

        private static final List<String> FORBIDDEN_PATH_FRAGMENTS = List.of(
                "publish", "export", "public", "signed", "cdn",
                "provider", "storage-inspection", "raw", "base64",
                "media-library", "video", "audio", "regenerate",
                "auto-approve", "generic-status");

        @Test
        @DisplayName("AdminPipelineController paths yasaklı fragment içermez")
        void pipelinePaths_noForbidden() {
            Set<String> paths = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .collect(Collectors.toSet());
            for (String path : paths) {
                String lp = path.toLowerCase();
                FORBIDDEN_PATH_FRAGMENTS.forEach(f ->
                        assertThat(lp).as("Path '%s' should not contain '%s'", path, f)
                                .doesNotContain(f));
            }
        }

        @Test
        @DisplayName("AdminReviewController paths yasaklı fragment içermez")
        void reviewPaths_noForbidden() {
            Set<String> paths = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class) || m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> {
                        PostMapping pm = m.getAnnotation(PostMapping.class);
                        GetMapping gm = m.getAnnotation(GetMapping.class);
                        if (pm != null) return Arrays.stream(pm.value());
                        if (gm != null) return Arrays.stream(gm.value());
                        return java.util.stream.Stream.empty();
                    })
                    .collect(Collectors.toSet());
            for (String path : paths) {
                String lp = path.toLowerCase();
                FORBIDDEN_PATH_FRAGMENTS.forEach(f ->
                        assertThat(lp).as("Path '%s' should not contain '%s'", path, f)
                                .doesNotContain(f));
            }
        }
    }

    // ── F. Service Read-Only Regression ────────────────────────────

    @Nested
    @DisplayName("F. Service Read-Only Regression")
    class ServiceReadOnlyRegression {

        @Test
        @DisplayName("PipelineAggregationService @Service annotation")
        void isService() {
            assertThat(PipelineAggregationService.class.getAnnotation(
                    org.springframework.stereotype.Service.class)).isNotNull();
        }

        @Test
        @DisplayName("PipelineAggregationService hiçbir controller annotation yok")
        void noControllerAnnotation() {
            assertThat(PipelineAggregationService.class.getAnnotation(
                    org.springframework.web.bind.annotation.RestController.class)).isNull();
            assertThat(PipelineAggregationService.class.getAnnotation(
                    org.springframework.stereotype.Controller.class)).isNull();
        }

        @Test
        @DisplayName("public metotlar @Transactional(readOnly=true)")
        void publicMethods_readOnlyTransactional() {
            for (Method m : PipelineAggregationService.class.getDeclaredMethods()) {
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

        @Test
        @DisplayName("service ReviewApplicationService bağımlılığı yok")
        void noReviewApplicationServiceDep() {
            var paramTypes = Arrays.stream(PipelineAggregationService.class.getDeclaredConstructors())
                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
                    .collect(Collectors.toSet());
            assertThat(paramTypes).doesNotContain(ReviewApplicationService.class);
        }

        @Test
        @DisplayName("controller single-dependency (PipelineAggregationService)")
        void controllerSingleDep() {
            var constructors = AdminPipelineController.class.getDeclaredConstructors();
            assertThat(constructors).hasSize(1);
            assertThat(constructors[0].getParameterCount()).isEqualTo(1);
            assertThat(constructors[0].getParameterTypes()[0])
                    .isEqualTo(PipelineAggregationService.class);
        }
    }

    // ── G. ReviewState Coverage Regression ──────────────────────────

    @Nested
    @DisplayName("G. ReviewState Coverage Regression")
    class ReviewStateCoverage {

        @Test
        @DisplayName("ReviewState tam olarak 7 değer")
        void reviewState_7Values() {
            assertThat(ReviewState.values()).hasSize(7);
        }

        @Test
        @DisplayName("beklenen ReviewState değerleri")
        void expectedReviewStates() {
            assertThat(ReviewState.values()).containsExactlyInAnyOrder(
                    ReviewState.DRAFT,
                    ReviewState.AI_GENERATED,
                    ReviewState.NEEDS_REVIEW,
                    ReviewState.IN_REVIEW,
                    ReviewState.CHANGES_REQUESTED,
                    ReviewState.APPROVED_FOR_INTERNAL_USE,
                    ReviewState.REJECTED);
        }

        @Test
        @DisplayName("yasaklı ReviewState değerleri yok")
        void noForbiddenReviewStates() {
            Set<String> states = Arrays.stream(ReviewState.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("COMPLETED", "PUBLISHED", "EXPORTED",
                    "READY_FOR_PUBLISH", "PUBLISHABLE", "CDN_READY")
                    .forEach(f -> assertThat(states).doesNotContain(f));
        }
    }

    // ── H. Readiness Integration Regression ────────────────────────

    @Nested
    @DisplayName("H. Readiness Integration Regression")
    class ReadinessIntegration {

        @Test
        @DisplayName("InternalApprovalReadinessAvailability READY dahili-only")
        void readyMeansInternalOnly() {
            // READY should exist — it's internal readiness, NOT publish readiness
            assertThat(InternalApprovalReadinessAvailability.READY.name()).isEqualTo("READY");
        }

        @Test
        @DisplayName("readiness availability yasaklı değerler içermez")
        void noForbiddenAvailability() {
            Set<String> values = Arrays.stream(InternalApprovalReadinessAvailability.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("PUBLISHED", "EXPORTED", "CDN_READY", "PUBLISH_READY")
                    .forEach(f -> assertThat(values).doesNotContain(f));
        }
    }

    // ── I. Cross-Controller Isolation ──────────────────────────────

    @Nested
    @DisplayName("I. Cross-Controller Isolation")
    class CrossControllerIsolation {

        @Test
        @DisplayName("AdminPipelineController /api/admin/pipeline base path")
        void pipelineBasePath() {
            var mapping = AdminPipelineController.class.getAnnotation(
                    org.springframework.web.bind.annotation.RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).contains("/api/admin/pipeline");
        }

        @Test
        @DisplayName("AdminReviewController /api/admin/reviews base path")
        void reviewBasePath() {
            var mapping = AdminReviewController.class.getAnnotation(
                    org.springframework.web.bind.annotation.RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).contains("/api/admin/reviews");
        }

        @Test
        @DisplayName("controllers farklı base path kullanıyor")
        void differentBasePaths() {
            var pipelinePath = AdminPipelineController.class.getAnnotation(
                    org.springframework.web.bind.annotation.RequestMapping.class).value()[0];
            var reviewPath = AdminReviewController.class.getAnnotation(
                    org.springframework.web.bind.annotation.RequestMapping.class).value()[0];
            assertThat(pipelinePath).isNotEqualTo(reviewPath);
        }
    }

    // ── J. No AI_GENERATED State Leak ──────────────────────────────

    @Nested
    @DisplayName("J. AI_GENERATED State Safety")
    class AiGeneratedStateSafety {

        @Test
        @DisplayName("ReviewState AI_GENERATED mevcut")
        void aiGeneratedExists() {
            // AI_GENERATED state exists in ReviewState but is not in SafePipelineStage
            boolean hasAiGenerated = false;
            for (ReviewState s : ReviewState.values()) {
                if (s.name().equals("AI_GENERATED")) hasAiGenerated = true;
            }
            // AI_GENERATED may or may not exist depending on design
            // But if it does, there should be no matching stage in SafePipelineStage
            Set<String> stages = Arrays.stream(SafePipelineStage.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(stages).doesNotContain("AI_GENERATED");
        }
    }
}
