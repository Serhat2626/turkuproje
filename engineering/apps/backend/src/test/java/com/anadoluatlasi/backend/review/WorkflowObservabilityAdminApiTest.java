package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-017 Task 03: Workflow Observability Admin API Tests.
 *
 * <p>Covers controller annotations, endpoint inventory, paths,
 * forbidden fields, no-mutation, and cross-controller isolation.</p>
 */
@DisplayName("FP-017 Task 03 — Workflow Observability Admin API")
class WorkflowObservabilityAdminApiTest {

    // ── A. Controller Annotations ─────────────────────────────────

    @Nested
    @DisplayName("A. Controller Annotations")
    class ControllerAnnotations {

        @Test
        @DisplayName("@RestController annotation mevcut")
        void hasRestController() {
            assertThat(AdminWorkflowObservabilityController.class
                    .getAnnotation(RestController.class)).isNotNull();
        }

        @Test
        @DisplayName("@RequestMapping /api/admin/workflow-observability")
        void hasCorrectBasePath() {
            var mapping = AdminWorkflowObservabilityController.class
                    .getAnnotation(RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).contains("/api/admin/workflow-observability");
        }
    }

    // ── B. Endpoint Inventory (21 = 9 POST + 12 GET) ──────────────

    @Nested
    @DisplayName("B. Endpoint Inventory (21 = 9 POST + 12 GET)")
    class EndpointInventory {

        @Test
        @DisplayName("AdminWorkflowObservabilityController: 2 GET")
        void observabilityController_2Get() {
            long count = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("AdminWorkflowObservabilityController: 0 POST")
        void observabilityController_0Post() {
            long count = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("AdminWorkflowObservabilityController: 0 PUT")
        void observabilityController_0Put() {
            long count = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PutMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("AdminWorkflowObservabilityController: 0 DELETE")
        void observabilityController_0Delete() {
            long count = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(DeleteMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("AdminReviewController: 9 POST (unchanged)")
        void reviewController_9Post() {
            long count = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isEqualTo(9);
        }

        @Test
        @DisplayName("AdminReviewController: 8 GET (unchanged)")
        void reviewController_8Get() {
            long count = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(count).isEqualTo(8);
        }

        @Test
        @DisplayName("AdminPipelineController: 2 GET (unchanged)")
        void pipelineController_2Get() {
            long count = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("toplam 21 endpoint = 9 POST + 12 GET")
        void totalEndpoints_21() {
            long totalPost = countAnnotated(AdminReviewController.class, PostMapping.class)
                    + countAnnotated(AdminPipelineController.class, PostMapping.class)
                    + countAnnotated(AdminWorkflowObservabilityController.class, PostMapping.class);
            long totalGet = countAnnotated(AdminReviewController.class, GetMapping.class)
                    + countAnnotated(AdminPipelineController.class, GetMapping.class)
                    + countAnnotated(AdminWorkflowObservabilityController.class, GetMapping.class);
            assertThat(totalPost).isEqualTo(9);
            assertThat(totalGet).isEqualTo(12);
            assertThat(totalPost + totalGet).isEqualTo(21);
        }

        private long countAnnotated(Class<?> clazz, Class<? extends java.lang.annotation.Annotation> ann) {
            return Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(ann)).count();
        }
    }

    // ── C. Endpoint Paths ─────────────────────────────────────────

    @Nested
    @DisplayName("C. Endpoint Paths")
    class EndpointPaths {

        @Test
        @DisplayName("GET paths doğru: /summary ve /diagnostics")
        void correctPaths() {
            Set<String> paths = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .collect(Collectors.toSet());
            assertThat(paths).containsExactlyInAnyOrder("/summary", "/diagnostics");
        }

        @Test
        @DisplayName("base path /api/admin/workflow-observability")
        void basePath() {
            var mapping = AdminWorkflowObservabilityController.class
                    .getAnnotation(RequestMapping.class);
            assertThat(mapping.value()[0]).isEqualTo("/api/admin/workflow-observability");
        }
    }

    // ── D. Forbidden Endpoint Path Fragments ──────────────────────

    @Nested
    @DisplayName("D. Forbidden Endpoint Path Fragments")
    class ForbiddenPaths {

        private static final List<String> FORBIDDEN = List.of(
                "publish", "export", "public", "signed", "cdn",
                "provider", "storage-inspection", "raw", "base64",
                "media-library", "video", "audio", "regenerate",
                "auto-approve", "generic-status");

        @Test
        @DisplayName("observability paths yasaklı fragment içermez")
        void noPaths() {
            Set<String> paths = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .collect(Collectors.toSet());
            // Also include base path
            var basePath = AdminWorkflowObservabilityController.class
                    .getAnnotation(RequestMapping.class).value()[0];
            paths.add(basePath);

            for (String path : paths) {
                String lp = path.toLowerCase();
                FORBIDDEN.forEach(f ->
                        assertThat(lp).as("Path '%s' should not contain '%s'", path, f)
                                .doesNotContain(f));
            }
        }
    }

    // ── E. Controller Dependency ──────────────────────────────────

    @Nested
    @DisplayName("E. Controller Dependency")
    class ControllerDependency {

        @Test
        @DisplayName("single dependency: WorkflowObservabilityService")
        void singleDependency() {
            var fields = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredFields())
                    .filter(f -> !f.isSynthetic())
                    .toList();
            assertThat(fields).hasSize(1);
            assertThat(fields.get(0).getType()).isEqualTo(WorkflowObservabilityService.class);
        }

        @Test
        @DisplayName("ReviewApplicationService bağımlılığı yok")
        void noReviewApplicationServiceDep() {
            var fieldTypes = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet());
            assertThat(fieldTypes).doesNotContain(ReviewApplicationService.class);
        }

        @Test
        @DisplayName("PipelineAggregationService doğrudan bağımlılığı yok")
        void noPipelineServiceDirectDep() {
            var fieldTypes = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredFields())
                    .map(Field::getType).collect(Collectors.toSet());
            assertThat(fieldTypes).doesNotContain(PipelineAggregationService.class);
        }
    }

    // ── F. No Mutation ────────────────────────────────────────────

    @Nested
    @DisplayName("F. No Mutation")
    class NoMutation {

        @Test
        @DisplayName("POST endpoint yok")
        void noPost() {
            long count = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("PUT endpoint yok")
        void noPut() {
            long count = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PutMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("DELETE endpoint yok")
        void noDelete() {
            long count = Arrays.stream(AdminWorkflowObservabilityController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(DeleteMapping.class)).count();
            assertThat(count).isZero();
        }
    }

    // ── G. Response DTO Forbidden Fields ───────────────────────────

    @Nested
    @DisplayName("G. Response DTO Forbidden Fields")
    class ForbiddenResponseFields {

        private static final List<String> FORBIDDEN_FIELDS = List.of(
                "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                "storageKey", "bucket", "objectPath", "rawPayload",
                "base64", "bytes", "secret", "apiKey", "token",
                "stackTrace", "exceptionClass",
                "publishStatus", "exportStatus", "publishedAt", "exportedAt");

        @Test
        @DisplayName("SafeWorkflowObservabilitySummary forbidden fields yok")
        void summary_noForbidden() {
            Set<String> fields = Arrays.stream(SafeWorkflowObservabilitySummary.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("SafeWorkflowDiagnosticItem forbidden fields yok")
        void diagnosticItem_noForbidden() {
            Set<String> fields = Arrays.stream(SafeWorkflowDiagnosticItem.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("toString forbidden string içermez (summary)")
        void summary_toString_safe() {
            String str = SafeWorkflowObservabilitySummary.empty().toString();
            FORBIDDEN_FIELDS.forEach(f -> assertThat(str).doesNotContain(f));
        }
    }

    // ── H. Forbidden Enum Values ──────────────────────────────────

    @Nested
    @DisplayName("H. Forbidden Enum Values in Response")
    class ForbiddenEnumValues {

        @Test
        @DisplayName("DiagnosticType yasaklı değerler yok")
        void diagnosticType_noForbidden() {
            Set<String> values = Arrays.stream(SafeWorkflowDiagnosticType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("PUBLISH_FAILED", "EXPORT_FAILED", "CDN_FAILED",
                    "PROVIDER_ERROR", "STORAGE_ERROR", "SIGNED_URL_ERROR")
                    .forEach(f -> assertThat(values).doesNotContain(f));
        }

        @Test
        @DisplayName("ControlHint yasaklı değerler yok")
        void controlHint_noForbidden() {
            Set<String> values = Arrays.stream(SafeOperationalControlHint.values())
                    .map(Enum::name).collect(Collectors.toSet());
            List.of("PUBLISH", "EXPORT", "APPROVE", "AUTO_FIX",
                    "REGENERATE", "PROVIDER_RETRY", "STORAGE_RETRY",
                    "CREATE_PUBLIC_URL", "CREATE_SIGNED_URL", "CDN_DELIVERY")
                    .forEach(f -> assertThat(values).doesNotContain(f));
        }
    }

    // ── I. Cross-Controller Isolation ─────────────────────────────

    @Nested
    @DisplayName("I. Cross-Controller Isolation")
    class CrossControllerIsolation {

        @Test
        @DisplayName("3 controllers farklı base paths")
        void threeDifferentBasePaths() {
            String reviewPath = AdminReviewController.class.getAnnotation(RequestMapping.class).value()[0];
            String pipelinePath = AdminPipelineController.class.getAnnotation(RequestMapping.class).value()[0];
            String observabilityPath = AdminWorkflowObservabilityController.class.getAnnotation(RequestMapping.class).value()[0];

            assertThat(Set.of(reviewPath, pipelinePath, observabilityPath)).hasSize(3);
        }

        @Test
        @DisplayName("observability path review/pipeline'dan farklı")
        void observabilityPathUnique() {
            String observabilityPath = AdminWorkflowObservabilityController.class
                    .getAnnotation(RequestMapping.class).value()[0];
            assertThat(observabilityPath).doesNotContain("/reviews");
            assertThat(observabilityPath).doesNotContain("/pipeline");
        }
    }

    // ── J. AdminReviewController Unchanged ─────────────────────────

    @Nested
    @DisplayName("J. AdminReviewController Unchanged")
    class ReviewControllerUnchanged {

        @Test
        @DisplayName("AdminReviewController 9 POST")
        void post9() {
            long count = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isEqualTo(9);
        }

        @Test
        @DisplayName("AdminReviewController 8 GET")
        void get8() {
            long count = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(count).isEqualTo(8);
        }

        @Test
        @DisplayName("AdminReviewController base path /api/admin/reviews")
        void basePath() {
            var mapping = AdminReviewController.class.getAnnotation(RequestMapping.class);
            assertThat(mapping.value()).contains("/api/admin/reviews");
        }
    }

    // ── K. AdminPipelineController Unchanged ───────────────────────

    @Nested
    @DisplayName("K. AdminPipelineController Unchanged")
    class PipelineControllerUnchanged {

        @Test
        @DisplayName("AdminPipelineController 0 POST")
        void post0() {
            long count = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("AdminPipelineController 2 GET")
        void get2() {
            long count = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(count).isEqualTo(2);
        }
    }
}
