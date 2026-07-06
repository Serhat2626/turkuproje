package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.BeforeEach;
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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-016 Task 03: AdminPipelineController comprehensive tests.
 *
 * <p>Covers: endpoint inventory, overview/detail API behavior, filter handling,
 * target type coverage, forbidden fields, forbidden paths, no-mutation,
 * combined controller inventory regression.</p>
 */
class PipelineAggregationAdminApiTest {

    private PipelineAggregationService pipelineAggregationService;
    private AdminPipelineController controller;

    @BeforeEach
    void setUp() {
        pipelineAggregationService = mock(PipelineAggregationService.class);
        controller = new AdminPipelineController(pipelineAggregationService);
    }

    // ── A. Controller Annotations ──────────────────────────────────

    @Nested
    @DisplayName("A. Controller Annotations")
    class ControllerAnnotationTests {

        @Test
        @DisplayName("is a @RestController")
        void isRestController() {
            assertThat(AdminPipelineController.class.getAnnotation(RestController.class))
                    .isNotNull();
        }

        @Test
        @DisplayName("base path is /api/admin/pipeline")
        void basePath() {
            RequestMapping mapping = AdminPipelineController.class.getAnnotation(RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).contains("/api/admin/pipeline");
        }
    }

    // ── B. Endpoint Inventory ──────────────────────────────────────

    @Nested
    @DisplayName("B. Endpoint Inventory")
    class EndpointInventoryTests {

        @Test
        @DisplayName("AdminPipelineController has exactly 2 GET endpoints")
        void pipelineController_2Get() {
            long getCount = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .count();
            assertThat(getCount).isEqualTo(2);
        }

        @Test
        @DisplayName("AdminPipelineController has 0 POST endpoints")
        void pipelineController_0Post() {
            long postCount = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .count();
            assertThat(postCount).isZero();
        }

        @Test
        @DisplayName("AdminPipelineController has 0 PUT endpoints")
        void pipelineController_0Put() {
            long putCount = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PutMapping.class))
                    .count();
            assertThat(putCount).isZero();
        }

        @Test
        @DisplayName("AdminPipelineController has 0 DELETE endpoints")
        void pipelineController_0Delete() {
            long deleteCount = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(DeleteMapping.class))
                    .count();
            assertThat(deleteCount).isZero();
        }

        @Test
        @DisplayName("GET paths are /overview and /{targetType}/{targetId}")
        void getPaths_correct() {
            Set<String> getPaths = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .collect(Collectors.toSet());
            assertThat(getPaths).containsExactlyInAnyOrder(
                    "/overview", "/{targetType}/{targetId}");
        }

        @Test
        @DisplayName("combined total: AdminReviewController(17) + AdminPipelineController(2) = 19")
        void combinedTotal_19() {
            long reviewPost = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long reviewGet = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            long pipelineGet = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            long pipelinePost = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();

            assertThat(reviewPost).isEqualTo(9);
            assertThat(reviewGet).isEqualTo(8);
            assertThat(pipelineGet).isEqualTo(2);
            assertThat(pipelinePost).isZero();
            assertThat(reviewPost + reviewGet + pipelineGet + pipelinePost).isEqualTo(19);
        }

        @Test
        @DisplayName("total POST across all controllers: 9")
        void totalPost_9() {
            long totalPost = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count()
                    + Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            assertThat(totalPost).isEqualTo(9);
        }

        @Test
        @DisplayName("total GET across all controllers: 10")
        void totalGet_10() {
            long totalGet = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count()
                    + Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(totalGet).isEqualTo(10);
        }
    }

    // ── C. Overview Endpoint ───────────────────────────────────────

    @Nested
    @DisplayName("C. Overview Endpoint")
    class OverviewEndpointTests {

        @Test
        @DisplayName("returns safe overview with items")
        void overview_returnsSafeOverview() {
            SafePipelineAggregateItem item = SafePipelineAggregateItem.unavailable(
                    ReviewTargetType.SCENARIO, UUID.randomUUID(), "TEST");
            SafePipelineOverview overview = SafePipelineOverview.builder()
                    .items(List.of(item))
                    .totalCount(1)
                    .countsByStage(java.util.Map.of(SafePipelineStage.UNAVAILABLE, 1))
                    .countsByTargetType(java.util.Map.of(ReviewTargetType.SCENARIO, 1))
                    .safeWarnings(List.of())
                    .updatedAt(java.time.LocalDateTime.now())
                    .build();

            when(pipelineAggregationService.getOverview(any(), any(), anyInt()))
                    .thenReturn(overview);

            var response = controller.getOverview(null, null, 50);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData().getTotalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("passes targetType filter to service")
        void overview_passesTargetTypeFilter() {
            when(pipelineAggregationService.getOverview(any(), any(), anyInt()))
                    .thenReturn(SafePipelineOverview.empty());

            controller.getOverview(ReviewTargetType.IMAGE_ASSET, null, 50);
            verify(pipelineAggregationService).getOverview(
                    ReviewTargetType.IMAGE_ASSET, null, 50);
        }

        @Test
        @DisplayName("passes stage filter to service")
        void overview_passesStageFilter() {
            when(pipelineAggregationService.getOverview(any(), any(), anyInt()))
                    .thenReturn(SafePipelineOverview.empty());

            controller.getOverview(null, SafePipelineStage.INTERNAL_READY, 50);
            verify(pipelineAggregationService).getOverview(
                    null, SafePipelineStage.INTERNAL_READY, 50);
        }

        @Test
        @DisplayName("passes limit to service")
        void overview_passesLimit() {
            when(pipelineAggregationService.getOverview(any(), any(), anyInt()))
                    .thenReturn(SafePipelineOverview.empty());

            controller.getOverview(null, null, 25);
            verify(pipelineAggregationService).getOverview(null, null, 25);
        }

        @Test
        @DisplayName("empty overview returns 200 with zero items")
        void overview_empty_returns200() {
            when(pipelineAggregationService.getOverview(any(), any(), anyInt()))
                    .thenReturn(SafePipelineOverview.empty());

            var response = controller.getOverview(null, null, 50);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getData().getTotalCount()).isZero();
        }
    }

    // ── D. Detail Endpoint ─────────────────────────────────────────

    @Nested
    @DisplayName("D. Detail Endpoint")
    class DetailEndpointTests {

        @Test
        @DisplayName("returns safe item for existing target")
        void detail_returnsSafeItem() {
            UUID id = UUID.randomUUID();
            SafePipelineAggregateItem item = SafePipelineAggregateItem.builder()
                    .targetType(ReviewTargetType.STORYBOARD)
                    .targetId(id)
                    .pipelineStage(SafePipelineStage.IN_REVIEW)
                    .reviewState(ReviewState.IN_REVIEW)
                    .previewAvailable(true)
                    .readinessAvailability(InternalApprovalReadinessAvailability.NOT_APPROVED)
                    .revisionRequested(false)
                    .safeBlockingReasons(List.of("REVIEW_IN_PROGRESS"))
                    .safeNextInternalHints(List.of("AWAIT_REVIEW_DECISION"))
                    .build();

            when(pipelineAggregationService.getItemDetail(ReviewTargetType.STORYBOARD, id))
                    .thenReturn(item);

            var response = controller.getItemDetail(ReviewTargetType.STORYBOARD, id);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getData().getPipelineStage())
                    .isEqualTo(SafePipelineStage.IN_REVIEW);
        }

        @Test
        @DisplayName("returns UNAVAILABLE for missing target")
        void detail_missingTarget_unavailable() {
            UUID id = UUID.randomUUID();
            when(pipelineAggregationService.getItemDetail(ReviewTargetType.PACKAGE, id))
                    .thenReturn(SafePipelineAggregateItem.unavailable(
                            ReviewTargetType.PACKAGE, id, "NO_REVIEW_RECORD"));

            var response = controller.getItemDetail(ReviewTargetType.PACKAGE, id);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getData().getPipelineStage())
                    .isEqualTo(SafePipelineStage.UNAVAILABLE);
        }
    }

    // ── E. Target Type Coverage ────────────────────────────────────

    @Nested
    @DisplayName("E. Target Type Coverage")
    class TargetTypeCoverageTests {

        @Test
        @DisplayName("detail endpoint handles all 5 target types")
        void allTargetTypes_handled() {
            for (ReviewTargetType targetType : ReviewTargetType.values()) {
                UUID id = UUID.randomUUID();
                when(pipelineAggregationService.getItemDetail(targetType, id))
                        .thenReturn(SafePipelineAggregateItem.unavailable(targetType, id, "TEST"));

                var response = controller.getItemDetail(targetType, id);
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getBody().getData().getTargetType()).isEqualTo(targetType);
            }
        }
    }

    // ── F. All Stage Values Serialize ──────────────────────────────

    @Nested
    @DisplayName("F. Stage Serialization")
    class StageSerializationTests {

        @Test
        @DisplayName("all SafePipelineStage values can appear in response")
        void allStages_serializable() {
            for (SafePipelineStage stage : SafePipelineStage.values()) {
                UUID id = UUID.randomUUID();
                SafePipelineAggregateItem item = SafePipelineAggregateItem.builder()
                        .targetType(ReviewTargetType.SCENARIO)
                        .targetId(id)
                        .pipelineStage(stage)
                        .previewAvailable(false)
                        .revisionRequested(false)
                        .safeBlockingReasons(List.of())
                        .safeNextInternalHints(List.of())
                        .build();

                when(pipelineAggregationService.getItemDetail(ReviewTargetType.SCENARIO, id))
                        .thenReturn(item);

                var response = controller.getItemDetail(ReviewTargetType.SCENARIO, id);
                assertThat(response.getBody().getData().getPipelineStage()).isEqualTo(stage);
            }
        }
    }

    // ── G. No-Mutation Verification ────────────────────────────────

    @Nested
    @DisplayName("G. No-Mutation Verification")
    class NoMutationTests {

        @Test
        @DisplayName("overview does not call any mutation service")
        void overview_noMutation() {
            when(pipelineAggregationService.getOverview(any(), any(), anyInt()))
                    .thenReturn(SafePipelineOverview.empty());
            controller.getOverview(null, null, 50);
            // Only getOverview should be called — no mutation
            verify(pipelineAggregationService).getOverview(any(), any(), anyInt());
            verify(pipelineAggregationService, never()).getItemDetail(any(), any());
        }

        @Test
        @DisplayName("detail does not call any mutation service")
        void detail_noMutation() {
            UUID id = UUID.randomUUID();
            when(pipelineAggregationService.getItemDetail(any(), any()))
                    .thenReturn(SafePipelineAggregateItem.unavailable(
                            ReviewTargetType.SCENARIO, id, "TEST"));
            controller.getItemDetail(ReviewTargetType.SCENARIO, id);
            verify(pipelineAggregationService).getItemDetail(any(), any());
            verify(pipelineAggregationService, never()).getOverview(any(), any(), anyInt());
        }

        @Test
        @DisplayName("controller has no @PostMapping methods")
        void noPostMethods() {
            for (Method method : AdminPipelineController.class.getDeclaredMethods()) {
                assertThat(method.getAnnotation(PostMapping.class))
                        .as("Method %s should NOT have @PostMapping", method.getName())
                        .isNull();
            }
        }
    }

    // ── H. Forbidden Path Fragments ────────────────────────────────

    @Nested
    @DisplayName("H. Forbidden Path Fragments")
    class ForbiddenPathTests {

        @Test
        @DisplayName("no endpoint paths contain forbidden fragments")
        void noPaths_containForbiddenFragments() {
            Set<String> allPaths = Arrays.stream(AdminPipelineController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .collect(Collectors.toSet());

            // Add base path
            RequestMapping mapping = AdminPipelineController.class.getAnnotation(RequestMapping.class);
            if (mapping != null) {
                allPaths.addAll(Arrays.asList(mapping.value()));
            }

            List<String> forbidden = List.of(
                    "publish", "export", "public", "signed", "cdn",
                    "provider", "storage-inspection", "raw", "base64",
                    "media-library", "video", "audio");

            for (String path : allPaths) {
                String lowerPath = path.toLowerCase();
                for (String fragment : forbidden) {
                    assertThat(lowerPath)
                            .as("Path '%s' should not contain '%s'", path, fragment)
                            .doesNotContain(fragment);
                }
            }
        }
    }

    // ── I. Forbidden Response Fields ───────────────────────────────

    @Nested
    @DisplayName("I. Forbidden Response Fields")
    class ForbiddenResponseFieldTests {

        @Test
        @DisplayName("SafePipelineAggregateItem has no forbidden fields")
        void aggregateItem_noForbiddenFields() {
            Set<String> fieldNames = Arrays.stream(SafePipelineAggregateItem.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            List<String> forbidden = List.of(
                    "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                    "storageKey", "bucket", "objectPath", "rawPayload",
                    "base64", "bytes", "secret", "apiKey", "token",
                    "stackTrace", "exceptionClass",
                    "publishStatus", "exportStatus", "publishedAt", "exportedAt");

            forbidden.forEach(f ->
                    assertThat(fieldNames).doesNotContain(f));
        }

        @Test
        @DisplayName("SafePipelineOverview has no forbidden fields")
        void overview_noForbiddenFields() {
            Set<String> fieldNames = Arrays.stream(SafePipelineOverview.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            List<String> forbidden = List.of(
                    "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                    "storageKey", "bucket", "objectPath", "rawPayload",
                    "base64", "bytes", "secret", "apiKey", "token",
                    "stackTrace", "exceptionClass",
                    "publishStatus", "exportStatus", "publishedAt", "exportedAt");

            forbidden.forEach(f ->
                    assertThat(fieldNames).doesNotContain(f));
        }

        @Test
        @DisplayName("SafePipelineStage has no forbidden values")
        void pipelineStage_noForbiddenValues() {
            Set<String> values = Arrays.stream(SafePipelineStage.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            List<String> forbidden = List.of(
                    "READY_FOR_PUBLISH", "PUBLISHABLE", "PUBLISHED",
                    "EXPORTABLE", "EXPORTED", "CDN_READY",
                    "PUBLIC_DELIVERY", "SIGNED_URL_READY");

            forbidden.forEach(f ->
                    assertThat(values).doesNotContain(f));
        }
    }

    // ── J. Controller Constructor ──────────────────────────────────

    @Nested
    @DisplayName("J. Controller Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("has exactly 1 constructor parameter (PipelineAggregationService)")
        void singleConstructorParam() {
            var constructors = AdminPipelineController.class.getDeclaredConstructors();
            assertThat(constructors).hasSize(1);
            assertThat(constructors[0].getParameterCount()).isEqualTo(1);
            assertThat(constructors[0].getParameterTypes()[0])
                    .isEqualTo(PipelineAggregationService.class);
        }

        @Test
        @DisplayName("does NOT depend on ReviewApplicationService")
        void noReviewApplicationServiceDependency() {
            var paramTypes = Arrays.stream(AdminPipelineController.class.getDeclaredConstructors())
                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
                    .collect(Collectors.toSet());

            assertThat(paramTypes).doesNotContain(ReviewApplicationService.class);
        }

        @Test
        @DisplayName("does NOT depend on ReviewRecordRepository")
        void noRepositoryDependency() {
            var paramTypes = Arrays.stream(AdminPipelineController.class.getDeclaredConstructors())
                    .flatMap(c -> Arrays.stream(c.getParameterTypes()))
                    .collect(Collectors.toSet());

            assertThat(paramTypes).doesNotContain(ReviewRecordRepository.class);
        }
    }

    // ── K. AdminReviewController Unchanged ─────────────────────────

    @Nested
    @DisplayName("K. AdminReviewController Unchanged")
    class ReviewControllerUnchangedTests {

        @Test
        @DisplayName("AdminReviewController still has 9 POST")
        void reviewController_still9Post() {
            long postCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .count();
            assertThat(postCount).isEqualTo(9);
        }

        @Test
        @DisplayName("AdminReviewController still has 8 GET")
        void reviewController_still8Get() {
            long getCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .count();
            assertThat(getCount).isEqualTo(8);
        }

        @Test
        @DisplayName("AdminReviewController base path unchanged (/api/admin/reviews)")
        void reviewController_basePath_unchanged() {
            RequestMapping mapping = AdminReviewController.class.getAnnotation(RequestMapping.class);
            assertThat(mapping).isNotNull();
            assertThat(mapping.value()).contains("/api/admin/reviews");
        }
    }
}
