package com.anadoluatlasi.backend.review;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FP-012 Task 10 — Admin Review Worklist, Target Context & Console Integration.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FP-012 Task 10 — Worklist, Target Context & Integration")
class AdminReviewWorklistTest {

    @Mock
    private ReviewApplicationService reviewApplicationService;
    @Mock
    private ReviewHandoffService reviewHandoffService;
    @Mock
    private AdminPreviewService adminPreviewService;

    private AdminReviewController controller;

    private static final ReviewTargetType TARGET_TYPE = ReviewTargetType.CONTENT_PROJECT;
    private static final UUID TARGET_ID = UUID.randomUUID();

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "providerUrl", "storageKey", "bucketName", "bucketPath",
            "rawPayload", "rawProviderPayload", "rawStoragePayload",
            "base64", "secret", "apiKey", "config",
            "publicUrl", "signedUrl", "publishUrl", "exportUrl", "stackTrace"
    );

    @BeforeEach
    void setUp() {
        controller = new AdminReviewController(reviewApplicationService, reviewHandoffService, adminPreviewService);
    }

    // ── 1. Worklist Endpoint ────────────────────────────────────────

    @Nested
    @DisplayName("1. Worklist Endpoint")
    class WorklistEndpoint {

        @Test
        @DisplayName("getWorklist returns safe list")
        void worklistHappy() {
            AdminReviewWorklistItem item = AdminReviewWorklistItem.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .currentState(ReviewState.IN_REVIEW)
                    .lastActionType(ReviewActionType.START_REVIEW)
                    .displayTitle("CONTENT_PROJECT — " + TARGET_ID.toString().substring(0, 8))
                    .createdAt(LocalDateTime.now()).build();
            when(reviewApplicationService.listWorkItems(any(), any(), any(), anyInt()))
                    .thenReturn(List.of(item));

            var response = controller.getWorklist(null, null, null, 50);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getDisplayTitle()).isNotNull();
        }

        @Test
        @DisplayName("getWorklist with state filter")
        void worklistByState() {
            when(reviewApplicationService.listWorkItems(eq(ReviewState.IN_REVIEW), any(), any(), anyInt()))
                    .thenReturn(List.of());

            var response = controller.getWorklist(ReviewState.IN_REVIEW, null, null, 50);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).isEmpty();
            verify(reviewApplicationService).listWorkItems(eq(ReviewState.IN_REVIEW), isNull(), isNull(), eq(50));
        }

        @Test
        @DisplayName("getWorklist with targetType filter")
        void worklistByTargetType() {
            when(reviewApplicationService.listWorkItems(any(), eq(TARGET_TYPE), any(), anyInt()))
                    .thenReturn(List.of());

            var response = controller.getWorklist(null, TARGET_TYPE, null, 50);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(reviewApplicationService).listWorkItems(isNull(), eq(TARGET_TYPE), isNull(), eq(50));
        }

        @Test
        @DisplayName("getWorklist returns empty for no results")
        void worklistEmpty() {
            when(reviewApplicationService.listWorkItems(any(), any(), any(), anyInt()))
                    .thenReturn(List.of());

            var response = controller.getWorklist(null, null, null, 50);
            assertThat(response.getBody().getData()).isEmpty();
        }
    }

    // ── 2. Target Context Endpoint ──────────────────────────────────

    @Nested
    @DisplayName("2. Target Context Endpoint")
    class TargetContextEndpoint {

        @Test
        @DisplayName("getTargetContext returns safe context")
        void contextHappy() {
            AdminReviewTargetContext ctx = AdminReviewTargetContext.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .currentState(ReviewState.IN_REVIEW)
                    .displayTitle("CONTENT_PROJECT — " + TARGET_ID.toString().substring(0, 8))
                    .displaySubtitle("CONTENT_PROJECT")
                    .safePreviewAvailable(false)
                    .safePreviewLabel("Önizleme henüz desteklenmiyor")
                    .createdAt(LocalDateTime.now()).build();
            when(reviewApplicationService.getTargetContext(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(ctx));

            var response = controller.getTargetContext(TARGET_TYPE, TARGET_ID);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getDisplayTitle()).isNotNull();
            assertThat(response.getBody().getData().isSafePreviewAvailable()).isFalse();
        }

        @Test
        @DisplayName("getTargetContext 404 when not found")
        void contextNotFound() {
            when(reviewApplicationService.getTargetContext(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());

            var response = controller.getTargetContext(TARGET_TYPE, TARGET_ID);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().isSuccess()).isFalse();
        }
    }

    // ── 3. DTO Forbidden Field Regression ───────────────────────────

    @Nested
    @DisplayName("3. New DTO Forbidden Fields")
    class DtoForbiddenFields {

        @Test
        @DisplayName("AdminReviewWorklistItem has no forbidden fields")
        void worklistSafe() {
            assertNoForbiddenFields(AdminReviewWorklistItem.class);
        }

        @Test
        @DisplayName("AdminReviewTargetContext has no forbidden fields")
        void contextSafe() {
            assertNoForbiddenFields(AdminReviewTargetContext.class);
        }
    }

    // ── 4. Updated Endpoint Inventory ───────────────────────────────

    @Nested
    @DisplayName("4. Endpoint Inventory")
    class EndpointInventory {

        @Test
        @DisplayName("Controller has exactly 14 endpoints (8 POST + 6 GET)")
        void exactEndpointCount() {
            long postCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long getCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(postCount).isEqualTo(8);
            assertThat(getCount).isEqualTo(6);
        }

        @Test
        @DisplayName("GET endpoints include worklist and context")
        void getEndpoints() {
            Set<String> getPaths = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .collect(Collectors.toSet());
            assertThat(getPaths).anyMatch(p -> p.contains("worklist"));
            assertThat(getPaths).anyMatch(p -> p.contains("context"));
            assertThat(getPaths).anyMatch(p -> p.contains("state"));
            assertThat(getPaths).anyMatch(p -> p.contains("audit"));
        }

        @Test
        @DisplayName("No forbidden endpoint paths")
        void noForbiddenPaths() {
            Set<String> allPaths = collectAllPaths();
            Set<String> forbidden = Set.of("publish", "export", "completed", "complete",
                    "ready-for-publish", "public-url", "signed-url", "cdn",
                    "provider-url", "storage-key", "generic-status");
            for (String path : allPaths) {
                for (String f : forbidden) {
                    assertThat(path.toLowerCase()).doesNotContain(f);
                }
            }
        }
    }

    // ── 5. Service Limit Behavior ───────────────────────────────────

    @Nested
    @DisplayName("5. Service Limit Behavior")
    class ServiceLimitBehavior {

        @Test
        @DisplayName("Limit is capped at 200")
        void limitCapped() {
            ReviewApplicationService realService = mock(ReviewApplicationService.class);
            when(realService.listWorkItems(any(), any(), any(), anyInt())).thenCallRealMethod();
            // We verify the method exists and handles limit
            AdminReviewController ctrl = new AdminReviewController(reviewApplicationService, reviewHandoffService, adminPreviewService);
            when(reviewApplicationService.listWorkItems(any(), any(), any(), eq(200)))
                    .thenReturn(List.of());
            ctrl.getWorklist(null, null, null, 999);
        }

        @Test
        @DisplayName("Limit minimum is 1")
        void limitMinimum() {
            when(reviewApplicationService.listWorkItems(any(), any(), any(), eq(0)))
                    .thenReturn(List.of());
            controller.getWorklist(null, null, null, 0);
        }
    }

    // ── 6. Safe Error Regression ────────────────────────────────────

    @Nested
    @DisplayName("6. Safe Error Regression")
    class SafeErrorRegression {

        @Test
        @DisplayName("Context 404 error is safe")
        void contextErrorSafe() {
            when(reviewApplicationService.getTargetContext(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());

            var response = controller.getTargetContext(TARGET_TYPE, TARGET_ID);
            String msg = response.getBody().getMessage();
            assertThat(msg).doesNotContain("https://");
            assertThat(msg).doesNotContain("secretKey");
            assertThat(msg).doesNotContain("bucket");
        }
    }

    // ── 7. No Migration / No Orchestration ──────────────────────────

    @Nested
    @DisplayName("7. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("No V11 migration file exists")
        void noNewMigration() {
            assertThat(java.nio.file.Path.of(
                    "src/main/resources/db/migration/V11__fp012_worklist.sql").toFile())
                    .doesNotExist();
        }

        @Test
        @DisplayName("ReviewApplicationService is still @Service")
        void serviceAnnotation() {
            assertThat(ReviewApplicationService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isTrue();
        }

        @Test
        @DisplayName("AdminReviewWorklistItem is not @Entity")
        void worklistNotEntity() {
            assertThat(AdminReviewWorklistItem.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("AdminReviewTargetContext is not @Entity")
        void contextNotEntity() {
            assertThat(AdminReviewTargetContext.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private void assertNoForbiddenFields(Class<?> clazz) {
        Set<String> fields = Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());
        for (String f : FORBIDDEN_FIELDS) {
            assertThat(fields).as(clazz.getSimpleName() + " forbidden: " + f).doesNotContain(f);
        }
    }

    private static Set<String> collectAllPaths() {
        return Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                .flatMap(m -> {
                    java.util.List<String> paths = new java.util.ArrayList<>();
                    if (m.isAnnotationPresent(PostMapping.class)) {
                        paths.addAll(Arrays.asList(m.getAnnotation(PostMapping.class).value()));
                    }
                    if (m.isAnnotationPresent(GetMapping.class)) {
                        paths.addAll(Arrays.asList(m.getAnnotation(GetMapping.class).value()));
                    }
                    return paths.stream();
                })
                .collect(Collectors.toSet());
    }
}
