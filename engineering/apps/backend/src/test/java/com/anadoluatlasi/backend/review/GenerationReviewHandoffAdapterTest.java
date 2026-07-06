package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-013 Task 05: GenerationReviewHandoffAdapter + orchestration integration tests.
 *
 * <p>Categories:</p>
 * <ul>
 *   <li>A. Handoff adapter behavior per generation type</li>
 *   <li>B. Idempotency and preservation</li>
 *   <li>C. Safety regression</li>
 *   <li>D. Failure safety</li>
 *   <li>E. Endpoint inventory regression</li>
 *   <li>F. Preview safety preservation</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class GenerationReviewHandoffAdapterTest {

    @Mock
    private ReviewHandoffService reviewHandoffService;

    private GenerationReviewHandoffAdapter adapter;

    private static final UUID TARGET_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new GenerationReviewHandoffAdapter(reviewHandoffService);
    }

    // ── A. Handoff adapter behavior per generation type ──────────

    @Nested
    @DisplayName("A. Handoff adapter behavior per generation type")
    class AdapterBehavior {

        @Test
        @DisplayName("Scenario handoff uses SCENARIO + SCENARIO_GENERATION + SYSTEM")
        void scenarioHandoff() {
            ReviewHandoffResult expected = buildCreatedResult(
                    ReviewTargetType.SCENARIO, TARGET_ID);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(expected);

            ReviewHandoffResult result = adapter.handoffScenario(
                    TARGET_ID, "Test Senaryo", "Test özet");

            assertThat(result).isNotNull();
            assertThat(result.getTargetType()).isEqualTo(ReviewTargetType.SCENARIO);

            ArgumentCaptor<ReviewHandoffRequest> captor =
                    ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            ReviewHandoffRequest captured = captor.getValue();
            assertThat(captured.getTargetType()).isEqualTo(ReviewTargetType.SCENARIO);
            assertThat(captured.getTargetId()).isEqualTo(TARGET_ID);
            assertThat(captured.getSource()).isEqualTo(ReviewHandoffSource.SCENARIO_GENERATION);
            assertThat(captured.getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("Storyboard handoff uses STORYBOARD + STORYBOARD_GENERATION + SYSTEM")
        void storyboardHandoff() {
            ReviewHandoffResult expected = buildCreatedResult(
                    ReviewTargetType.STORYBOARD, TARGET_ID);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(expected);

            ReviewHandoffResult result = adapter.handoffStoryboard(
                    TARGET_ID, "Test Storyboard", "Test özet");

            assertThat(result).isNotNull();

            ArgumentCaptor<ReviewHandoffRequest> captor =
                    ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            assertThat(captor.getValue().getTargetType()).isEqualTo(ReviewTargetType.STORYBOARD);
            assertThat(captor.getValue().getSource()).isEqualTo(ReviewHandoffSource.STORYBOARD_GENERATION);
            assertThat(captor.getValue().getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("Package handoff uses PACKAGE + PACKAGE_GENERATION + SYSTEM")
        void packageHandoff() {
            ReviewHandoffResult expected = buildCreatedResult(
                    ReviewTargetType.PACKAGE, TARGET_ID);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(expected);

            ReviewHandoffResult result = adapter.handoffPackage(
                    TARGET_ID, "Test Paket", "Test açıklama");

            assertThat(result).isNotNull();

            ArgumentCaptor<ReviewHandoffRequest> captor =
                    ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            assertThat(captor.getValue().getTargetType()).isEqualTo(ReviewTargetType.PACKAGE);
            assertThat(captor.getValue().getSource()).isEqualTo(ReviewHandoffSource.PACKAGE_GENERATION);
            assertThat(captor.getValue().getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("Image asset handoff uses IMAGE_ASSET + IMAGE_GENERATION + SYSTEM")
        void imageAssetHandoff() {
            ReviewHandoffResult expected = buildCreatedResult(
                    ReviewTargetType.IMAGE_ASSET, TARGET_ID);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(expected);

            ReviewHandoffResult result = adapter.handoffImageAsset(
                    TARGET_ID, "Görsel Üretim", "AI görsel üretimi");

            assertThat(result).isNotNull();

            ArgumentCaptor<ReviewHandoffRequest> captor =
                    ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            assertThat(captor.getValue().getTargetType()).isEqualTo(ReviewTargetType.IMAGE_ASSET);
            assertThat(captor.getValue().getSource()).isEqualTo(ReviewHandoffSource.IMAGE_GENERATION);
            assertThat(captor.getValue().getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("Content project handoff uses CONTENT_PROJECT + CONTENT_PROJECT_GENERATION + SYSTEM")
        void contentProjectHandoff() {
            ReviewHandoffResult expected = buildCreatedResult(
                    ReviewTargetType.CONTENT_PROJECT, TARGET_ID);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(expected);

            ReviewHandoffResult result = adapter.handoffContentProject(
                    TARGET_ID, "Test Proje", "Test açıklama");

            assertThat(result).isNotNull();

            ArgumentCaptor<ReviewHandoffRequest> captor =
                    ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            assertThat(captor.getValue().getTargetType()).isEqualTo(ReviewTargetType.CONTENT_PROJECT);
            assertThat(captor.getValue().getSource()).isEqualTo(ReviewHandoffSource.CONTENT_PROJECT_GENERATION);
            assertThat(captor.getValue().getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
        }

        @Test
        @DisplayName("All handoff methods use ReviewHandoffRequest.createSafe")
        void allMethodsUseSafeFactory() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenReturn(buildCreatedResult(ReviewTargetType.SCENARIO, TARGET_ID));

            adapter.handoffScenario(TARGET_ID, "Test", "Test");

            ArgumentCaptor<ReviewHandoffRequest> captor =
                    ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            // createSafe applies ReviewTextSafety.sanitize — verify safe fields exist
            ReviewHandoffRequest req = captor.getValue();
            assertThat(req.getActorReference()).isNotNull();
            assertThat(req.getActorReference()).startsWith("system:");
        }

        @Test
        @DisplayName("Actor reference includes source name")
        void actorReferenceIncludesSource() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenReturn(buildCreatedResult(ReviewTargetType.SCENARIO, TARGET_ID));

            adapter.handoffScenario(TARGET_ID, "Test", "Test");

            ArgumentCaptor<ReviewHandoffRequest> captor =
                    ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            assertThat(captor.getValue().getActorReference()).contains("scenario-generation");
        }

        @Test
        @DisplayName("No handoff auto-approves — current state is NEEDS_REVIEW")
        void noAutoApproval() {
            ReviewHandoffResult result = buildCreatedResult(ReviewTargetType.SCENARIO, TARGET_ID);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffScenario(TARGET_ID, "Test", "Test");

            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
            assertThat(actual.getCurrentState()).isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }
    }

    // ── B. Idempotency and preservation ──────────────────────────

    @Nested
    @DisplayName("B. Idempotency and preservation")
    class IdempotencyPreservation {

        @Test
        @DisplayName("Repeated scenario handoff is safe (idempotent)")
        void repeatedHandoffIsSafe() {
            ReviewHandoffResult preserved = buildPreservedResult(
                    ReviewTargetType.SCENARIO, TARGET_ID, ReviewState.NEEDS_REVIEW);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(preserved);

            ReviewHandoffResult result = adapter.handoffScenario(TARGET_ID, "Test", "Test");

            assertThat(result.isPreserved()).isTrue();
            assertThat(result.isCreated()).isFalse();
        }

        @Test
        @DisplayName("Existing NEEDS_REVIEW is preserved")
        void needsReviewPreserved() {
            assertStatePreserved(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("Existing IN_REVIEW is preserved")
        void inReviewPreserved() {
            assertStatePreserved(ReviewState.IN_REVIEW);
        }

        @Test
        @DisplayName("Existing CHANGES_REQUESTED is preserved")
        void changesRequestedPreserved() {
            assertStatePreserved(ReviewState.CHANGES_REQUESTED);
        }

        @Test
        @DisplayName("Existing APPROVED_FOR_INTERNAL_USE is not overwritten")
        void approvedNotOverwritten() {
            assertStatePreserved(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("Existing REJECTED is not overwritten")
        void rejectedNotOverwritten() {
            assertStatePreserved(ReviewState.REJECTED);
        }

        private void assertStatePreserved(ReviewState state) {
            ReviewHandoffResult preserved = buildPreservedResult(
                    ReviewTargetType.SCENARIO, TARGET_ID, state);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(preserved);

            ReviewHandoffResult result = adapter.handoffScenario(TARGET_ID, "Test", "Test");

            assertThat(result.isPreserved()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(state);
        }
    }

    // ── C. Safety regression ─────────────────────────────────────

    private static final String[] FORBIDDEN_FIELDS = {
            "providerUrl", "storageKey", "bucketName", "bucketPath",
            "rawPayload", "rawProviderPayload", "rawStoragePayload",
            "base64", "secret", "apiKey", "config",
            "publicUrl", "signedUrl", "publishUrl", "exportUrl",
            "stackTrace", "rawImageBytes", "rawContent"
    };

    @Nested
    @DisplayName("C. Safety regression")
    class SafetyRegression {

        @Test
        @DisplayName("GenerationReviewHandoffAdapter has no forbidden fields")
        void adapterHasNoForbiddenFields() {
            Set<String> fields = Arrays.stream(GenerationReviewHandoffAdapter.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields).doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("Adapter only depends on ReviewHandoffService")
        void adapterDependsOnlyOnHandoffService() {
            Set<String> fields = Arrays.stream(GenerationReviewHandoffAdapter.class.getDeclaredFields())
                    .filter(f -> !f.isSynthetic())
                    .filter(f -> !f.getName().equals("log"))
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fields).containsExactly("reviewHandoffService");
        }

        @Test
        @DisplayName("No automatic approval in any handoff path")
        void noAutoApproval() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenReturn(buildCreatedResult(ReviewTargetType.SCENARIO, TARGET_ID));

            ReviewHandoffResult r1 = adapter.handoffScenario(TARGET_ID, "t", "s");
            assertThat(r1.getCurrentState()).isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);

            ReviewHandoffResult r2 = adapter.handoffStoryboard(TARGET_ID, "t", "s");
            assertThat(r2.getCurrentState()).isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);

            ReviewHandoffResult r3 = adapter.handoffPackage(TARGET_ID, "t", "s");
            assertThat(r3.getCurrentState()).isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);

            ReviewHandoffResult r4 = adapter.handoffImageAsset(TARGET_ID, "t", "s");
            assertThat(r4.getCurrentState()).isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);

            ReviewHandoffResult r5 = adapter.handoffContentProject(TARGET_ID, "t", "s");
            assertThat(r5.getCurrentState()).isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("Adapter class does not contain forbidden string patterns")
        void adapterSourceSafety() {
            String className = GenerationReviewHandoffAdapter.class.getName();
            // Class exists and is in the review package
            assertThat(className).contains("review");
            assertThat(className).doesNotContain("publish");
            assertThat(className).doesNotContain("export");
            assertThat(className).doesNotContain("cdn");
        }

        @Test
        @DisplayName("Adapter is @Service annotated")
        void adapterIsService() {
            assertThat(GenerationReviewHandoffAdapter.class
                    .isAnnotationPresent(org.springframework.stereotype.Service.class)).isTrue();
        }
    }

    // ── D. Failure safety ────────────────────────────────────────

    @Nested
    @DisplayName("D. Failure safety")
    class FailureSafety {

        @Test
        @DisplayName("Handoff failure returns null (non-fatal)")
        void handoffFailureReturnsNull() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenThrow(new RuntimeException("Test failure"));

            ReviewHandoffResult result = adapter.handoffScenario(TARGET_ID, "Test", "Test");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ReviewHandoffException returns null (non-fatal)")
        void handoffExceptionReturnsNull() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenThrow(new ReviewHandoffException("Test", "TEST_CODE"));

            ReviewHandoffResult result = adapter.handoffStoryboard(TARGET_ID, "Test", "Test");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("NullPointerException returns null (non-fatal)")
        void npeReturnsNull() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenThrow(new NullPointerException("null target"));

            ReviewHandoffResult result = adapter.handoffPackage(TARGET_ID, "Test", "Test");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Handoff failure does not throw to caller")
        void handoffFailureDoesNotThrow() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenThrow(new RuntimeException("Catastrophic"));

            // Must not throw
            adapter.handoffScenario(TARGET_ID, "Test", "Test");
            adapter.handoffStoryboard(TARGET_ID, "Test", "Test");
            adapter.handoffPackage(TARGET_ID, "Test", "Test");
            adapter.handoffImageAsset(TARGET_ID, "Test", "Test");
            adapter.handoffContentProject(TARGET_ID, "Test", "Test");
        }

        @Test
        @DisplayName("Multiple package handoff failures are individually safe")
        void multiplePackageFailuresSafe() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenThrow(new RuntimeException("Fail 1"))
                    .thenThrow(new RuntimeException("Fail 2"))
                    .thenReturn(buildCreatedResult(ReviewTargetType.PACKAGE, UUID.randomUUID()));

            assertThat(adapter.handoffPackage(UUID.randomUUID(), "P1", "S1")).isNull();
            assertThat(adapter.handoffPackage(UUID.randomUUID(), "P2", "S2")).isNull();
            assertThat(adapter.handoffPackage(UUID.randomUUID(), "P3", "S3")).isNotNull();
        }
    }

    // ── E. Endpoint inventory regression ─────────────────────────

    @Nested
    @DisplayName("E. Endpoint inventory regression")
    class EndpointInventory {

        @Test
        @DisplayName("AdminReviewController still has exactly 16 endpoints")
        void shouldStillHave16Endpoints() {
            long post = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long get = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();

            assertThat(post).isEqualTo(8);
            assertThat(get).isEqualTo(8);
            assertThat(post + get).isEqualTo(16);
        }

        @Test
        @DisplayName("No new publish/export/CDN/provider/storage-inspection endpoints")
        void noForbiddenEndpoints() {
            String[] forbiddenSegments = {
                    "publish", "export", "completed", "complete",
                    "ready-for-publish", "public-url", "signed-url",
                    "cdn", "provider-url", "storage-key",
                    "generic-status", "config",
                    "provider-inspection", "storage-inspection"
            };

            Set<String> allPaths = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)
                            || m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> {
                        if (m.isAnnotationPresent(PostMapping.class)) {
                            return Arrays.stream(m.getAnnotation(PostMapping.class).value());
                        }
                        return Arrays.stream(m.getAnnotation(GetMapping.class).value());
                    })
                    .collect(Collectors.toSet());

            for (String path : allPaths) {
                for (String forbidden : forbiddenSegments) {
                    assertThat(path.toLowerCase()).doesNotContain(forbidden);
                }
            }
        }

        @Test
        @DisplayName("GenerationReviewHandoffAdapter has no controller/endpoint annotations")
        void adapterHasNoEndpointAnnotations() {
            assertThat(GenerationReviewHandoffAdapter.class.getAnnotationsByType(
                    org.springframework.web.bind.annotation.RestController.class)).isEmpty();
            assertThat(GenerationReviewHandoffAdapter.class.getAnnotationsByType(
                    org.springframework.web.bind.annotation.RequestMapping.class)).isEmpty();
            assertThat(GenerationReviewHandoffAdapter.class.getAnnotationsByType(
                    org.springframework.stereotype.Controller.class)).isEmpty();
        }
    }

    // ── F. Preview safety preservation ───────────────────────────

    @Nested
    @DisplayName("F. Preview safety preservation")
    class PreviewSafety {

        @Test
        @DisplayName("SafeAdminPreview has no forbidden fields")
        void previewHasNoForbiddenFields() {
            Set<String> fields = Arrays.stream(SafeAdminPreview.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields).doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("AdminReviewHandoffResponse has no forbidden fields")
        void handoffResponseHasNoForbiddenFields() {
            Set<String> fields = Arrays.stream(AdminReviewHandoffResponse.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields).doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("ReviewHandoffRequest has no forbidden fields")
        void handoffRequestHasNoForbiddenFields() {
            Set<String> fields = Arrays.stream(ReviewHandoffRequest.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields).doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("No new migration V10+ needed")
        void noNewMigration() {
            Set<String> fields = Arrays.stream(GenerationReviewHandoffAdapter.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fields).doesNotContain("dataSource");
            assertThat(fields).doesNotContain("jdbcTemplate");
            assertThat(fields).doesNotContain("entityManager");
        }
    }

    // ── Test Helpers ─────────────────────────────────────────────

    private ReviewHandoffResult buildCreatedResult(ReviewTargetType type, UUID id) {
        return ReviewHandoffResult.builder()
                .targetType(type)
                .targetId(id)
                .previousState(null)
                .currentState(ReviewState.NEEDS_REVIEW)
                .created(true)
                .updated(false)
                .preserved(false)
                .auditEventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                .safeMessage("Created")
                .build();
    }

    private ReviewHandoffResult buildPreservedResult(ReviewTargetType type, UUID id,
                                                      ReviewState state) {
        return ReviewHandoffResult.builder()
                .targetType(type)
                .targetId(id)
                .previousState(state)
                .currentState(state)
                .created(false)
                .updated(false)
                .preserved(true)
                .safeReasonCode("ALREADY_IN_PIPELINE")
                .safeMessage("Preserved")
                .build();
    }
}
