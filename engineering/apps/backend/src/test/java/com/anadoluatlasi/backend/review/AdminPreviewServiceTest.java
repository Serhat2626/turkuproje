package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FP-013 Task 03 — Safe Admin Preview DTO, Target Context Resolver & Preview Service.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FP-013 Task 03 — Admin Preview Service")
class AdminPreviewServiceTest {

    @Mock
    private ReviewRecordRepository reviewRecordRepository;

    private ReviewTargetContextResolver contextResolver;
    private AdminPreviewService previewService;

    private static final UUID TARGET_ID = UUID.randomUUID();

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "providerUrl", "storageKey", "bucketName", "bucketPath",
            "rawPayload", "rawProviderPayload", "rawStoragePayload",
            "base64", "secret", "apiKey", "config",
            "publicUrl", "signedUrl", "publishUrl", "exportUrl",
            "stackTrace", "rawImageBytes", "rawContent"
    );

    private static final Set<String> FORBIDDEN_MODES = Set.of(
            "PUBLIC_URL", "SIGNED_URL", "CDN", "RAW_PAYLOAD", "BASE64",
            "PROVIDER_URL", "STORAGE_KEY", "EXPORT", "PUBLISH"
    );

    @BeforeEach
    void setUp() {
        contextResolver = new ReviewTargetContextResolver(reviewRecordRepository);
        previewService = new AdminPreviewService(contextResolver);
    }

    private ReviewRecordEntity record(ReviewTargetType type, UUID id, ReviewState state) {
        ReviewRecordEntity entity = ReviewRecordEntity.builder()
                .reviewTargetType(type)
                .reviewTargetId(id)
                .currentState(state)
                .build();
        entity.setId(UUID.randomUUID());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. Preview Mode Safety
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Preview Mode Safety")
    class PreviewModeSafety {

        @Test
        @DisplayName("Safe preview modes exist")
        void safeModesExist() {
            Set<String> names = Arrays.stream(AdminPreviewMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).contains("TEXT_SUMMARY", "IMAGE_ASSET", "PACKAGE_SUMMARY",
                    "STORYBOARD_SUMMARY", "SCENARIO_SUMMARY", "UNAVAILABLE");
        }

        @Test
        @DisplayName("No forbidden preview modes")
        void noForbiddenModes() {
            Set<String> names = Arrays.stream(AdminPreviewMode.values())
                    .map(Enum::name).collect(Collectors.toSet());
            FORBIDDEN_MODES.forEach(m -> assertThat(names).doesNotContain(m));
        }

        @Test
        @DisplayName("Safe availability values exist")
        void availabilityValuesExist() {
            Set<String> names = Arrays.stream(AdminPreviewAvailability.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).contains("AVAILABLE", "UNAVAILABLE", "NOT_REVIEWABLE",
                    "MISSING_TARGET", "UNSAFE_TARGET");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. SafeAdminPreview Model Safety
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. SafeAdminPreview Model Safety")
    class ModelSafety {

        @Test
        @DisplayName("SafeAdminPreview has no forbidden fields")
        void noForbiddenFields() {
            Set<String> fields = Arrays.stream(SafeAdminPreview.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("SafeAdminPreview has expected safe fields")
        void hasExpectedFields() {
            Set<String> fields = Arrays.stream(SafeAdminPreview.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            assertThat(fields).contains("targetType", "targetId", "previewMode",
                    "availability", "previewAvailable", "safeTitle", "safeSummary",
                    "safeMessage", "safeReasonCode", "safePreviewLabel",
                    "safeThumbnailAvailable", "currentReviewState");
        }

        @Test
        @DisplayName("SafeAdminPreview.toString is safe")
        void toStringSafe() {
            SafeAdminPreview preview = SafeAdminPreview.builder()
                    .targetType(ReviewTargetType.CONTENT_PROJECT)
                    .targetId(TARGET_ID)
                    .previewMode(AdminPreviewMode.TEXT_SUMMARY)
                    .availability(AdminPreviewAvailability.AVAILABLE)
                    .previewAvailable(true)
                    .build();
            String s = preview.toString();
            assertThat(s).doesNotContain("providerUrl");
            assertThat(s).doesNotContain("storageKey");
            assertThat(s).doesNotContain("base64");
            assertThat(s).doesNotContain("secret");
            assertThat(s).contains("SafeAdminPreview");
        }

        @Test
        @DisplayName("SafeAdminPreview is not a JPA Entity")
        void notEntity() {
            assertThat(SafeAdminPreview.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("SafeAdminPreview.unavailable factory works")
        void unavailableFactory() {
            SafeAdminPreview p = SafeAdminPreview.unavailable(
                    ReviewTargetType.SCENARIO, TARGET_ID,
                    AdminPreviewAvailability.MISSING_TARGET,
                    "TEST_CODE", "Test message");
            assertThat(p.isPreviewAvailable()).isFalse();
            assertThat(p.getPreviewMode()).isEqualTo(AdminPreviewMode.UNAVAILABLE);
            assertThat(p.getAvailability()).isEqualTo(AdminPreviewAvailability.MISSING_TARGET);
            assertThat(p.getSafeReasonCode()).isEqualTo("TEST_CODE");
        }

        @Test
        @DisplayName("AdminPreviewException.toString is safe")
        void exceptionSafe() {
            AdminPreviewException ex = new AdminPreviewException("test", "ERR");
            assertThat(ex.toString()).doesNotContain("providerUrl");
            assertThat(ex.toString()).contains("ERR");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. Target Context Resolver
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Target Context Resolver")
    class ContextResolver {

        @Test
        @DisplayName("Resolves safe context from existing record")
        void resolvesFromRecord() {
            ReviewRecordEntity entity = record(ReviewTargetType.SCENARIO, TARGET_ID, ReviewState.NEEDS_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(
                    ReviewTargetType.SCENARIO, TARGET_ID))
                    .thenReturn(Optional.of(entity));

            var result = contextResolver.resolve(ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getTargetType()).isEqualTo(ReviewTargetType.SCENARIO);
            assertThat(result.get().getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
            assertThat(result.get().getSafeTitle()).contains("Senaryo");
            assertThat(result.get().getPreviewMode()).isEqualTo(AdminPreviewMode.SCENARIO_SUMMARY);
        }

        @Test
        @DisplayName("Missing target returns empty")
        void missingTarget() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(any(), any()))
                    .thenReturn(Optional.empty());

            var result = contextResolver.resolve(ReviewTargetType.CONTENT_PROJECT, TARGET_ID);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Null targetType returns empty")
        void nullTargetType() {
            var result = contextResolver.resolve(null, TARGET_ID);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Null targetId returns empty")
        void nullTargetId() {
            var result = contextResolver.resolve(ReviewTargetType.CONTENT_PROJECT, null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Resolved context toString is safe")
        void toStringSafe() {
            ReviewRecordEntity entity = record(ReviewTargetType.STORYBOARD, TARGET_ID, ReviewState.IN_REVIEW);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(
                    ReviewTargetType.STORYBOARD, TARGET_ID))
                    .thenReturn(Optional.of(entity));

            var ctx = contextResolver.resolve(ReviewTargetType.STORYBOARD, TARGET_ID).orElseThrow();
            String s = ctx.toString();
            assertThat(s).doesNotContain("providerUrl");
            assertThat(s).doesNotContain("storageKey");
            assertThat(s).contains("ResolvedTargetContext");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. Preview Service Happy Paths
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. Preview Service Happy Paths")
    class HappyPaths {

        @Test
        @DisplayName("CONTENT_PROJECT -> TEXT_SUMMARY")
        void contentProject() {
            mockRecord(ReviewTargetType.CONTENT_PROJECT, ReviewState.NEEDS_REVIEW);
            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.CONTENT_PROJECT, TARGET_ID);

            assertThat(p.isPreviewAvailable()).isTrue();
            assertThat(p.getPreviewMode()).isEqualTo(AdminPreviewMode.TEXT_SUMMARY);
            assertThat(p.getAvailability()).isEqualTo(AdminPreviewAvailability.AVAILABLE);
            assertThat(p.getSafeTitle()).contains("Proje");
        }

        @Test
        @DisplayName("SCENARIO -> SCENARIO_SUMMARY")
        void scenario() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.IN_REVIEW);
            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(p.isPreviewAvailable()).isTrue();
            assertThat(p.getPreviewMode()).isEqualTo(AdminPreviewMode.SCENARIO_SUMMARY);
            assertThat(p.getSafeTitle()).contains("Senaryo");
        }

        @Test
        @DisplayName("STORYBOARD -> STORYBOARD_SUMMARY")
        void storyboard() {
            mockRecord(ReviewTargetType.STORYBOARD, ReviewState.CHANGES_REQUESTED);
            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.STORYBOARD, TARGET_ID);

            assertThat(p.isPreviewAvailable()).isTrue();
            assertThat(p.getPreviewMode()).isEqualTo(AdminPreviewMode.STORYBOARD_SUMMARY);
            assertThat(p.getSafeTitle()).contains("Storyboard");
        }

        @Test
        @DisplayName("PACKAGE -> PACKAGE_SUMMARY")
        void packageTarget() {
            mockRecord(ReviewTargetType.PACKAGE, ReviewState.APPROVED_FOR_INTERNAL_USE);
            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.PACKAGE, TARGET_ID);

            assertThat(p.isPreviewAvailable()).isTrue();
            assertThat(p.getPreviewMode()).isEqualTo(AdminPreviewMode.PACKAGE_SUMMARY);
            assertThat(p.getSafePreviewLabel()).contains("Dahili Onaylı");
        }

        @Test
        @DisplayName("IMAGE_ASSET -> IMAGE_ASSET mode, no bytes/URL")
        void imageAsset() {
            mockRecord(ReviewTargetType.IMAGE_ASSET, ReviewState.NEEDS_REVIEW);
            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.IMAGE_ASSET, TARGET_ID);

            assertThat(p.isPreviewAvailable()).isTrue();
            assertThat(p.getPreviewMode()).isEqualTo(AdminPreviewMode.IMAGE_ASSET);
            assertThat(p.isSafeThumbnailAvailable()).isFalse();
            assertThat(p.getSafeTitle()).contains("Görsel");
        }

        private void mockRecord(ReviewTargetType type, ReviewState state) {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(type, TARGET_ID))
                    .thenReturn(Optional.of(record(type, TARGET_ID, state)));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. Unavailable / Failure Handling
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. Unavailable / Failure Handling")
    class UnavailableHandling {

        @Test
        @DisplayName("Missing target returns safe unavailable")
        void missingTarget() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(any(), any()))
                    .thenReturn(Optional.empty());

            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.CONTENT_PROJECT, TARGET_ID);
            assertThat(p.isPreviewAvailable()).isFalse();
            assertThat(p.getAvailability()).isEqualTo(AdminPreviewAvailability.MISSING_TARGET);
            assertThat(p.getSafeReasonCode()).isEqualTo("PREVIEW_TARGET_NOT_FOUND");
        }

        @Test
        @DisplayName("Null targetType returns safe unavailable")
        void nullTargetType() {
            SafeAdminPreview p = previewService.getPreview(null, TARGET_ID);
            assertThat(p.isPreviewAvailable()).isFalse();
            assertThat(p.getAvailability()).isEqualTo(AdminPreviewAvailability.NOT_REVIEWABLE);
        }

        @Test
        @DisplayName("Null targetId returns safe unavailable")
        void nullTargetId() {
            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.SCENARIO, null);
            assertThat(p.isPreviewAvailable()).isFalse();
            assertThat(p.getAvailability()).isEqualTo(AdminPreviewAvailability.MISSING_TARGET);
        }

        @Test
        @DisplayName("Failure message contains no sensitive data")
        void failureMessageSafe() {
            SafeAdminPreview p = previewService.getPreview(null, null);
            assertThat(p.getSafeMessage()).doesNotContain("providerUrl");
            assertThat(p.getSafeMessage()).doesNotContain("storageKey");
            assertThat(p.getSafeMessage()).doesNotContain("base64");
            assertThat(p.getSafeMessage()).doesNotContain("secret");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. Review Workflow Guardrails
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. Review Workflow Guardrails")
    class WorkflowGuardrails {

        @Test
        @DisplayName("AI_GENERATED preview does not imply approval")
        void aiGeneratedNoApproval() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(
                    ReviewTargetType.SCENARIO, TARGET_ID))
                    .thenReturn(Optional.of(record(ReviewTargetType.SCENARIO, TARGET_ID, ReviewState.AI_GENERATED)));

            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.SCENARIO, TARGET_ID);
            assertThat(p.getCurrentReviewState()).isEqualTo(ReviewState.AI_GENERATED);
            assertThat(p.isPreviewAvailable()).isTrue();
            // Preview does not change state
        }

        @Test
        @DisplayName("NEEDS_REVIEW preview is review preview, not publish preview")
        void needsReviewIsNotPublish() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(
                    ReviewTargetType.STORYBOARD, TARGET_ID))
                    .thenReturn(Optional.of(record(ReviewTargetType.STORYBOARD, TARGET_ID, ReviewState.NEEDS_REVIEW)));

            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.STORYBOARD, TARGET_ID);
            assertThat(p.getSafePreviewLabel()).doesNotContainIgnoringCase("yayın");
            assertThat(p.getSafePreviewLabel()).doesNotContainIgnoringCase("publish");
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE preview remains internal only")
        void approvedInternalOnly() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(
                    ReviewTargetType.PACKAGE, TARGET_ID))
                    .thenReturn(Optional.of(record(ReviewTargetType.PACKAGE, TARGET_ID,
                            ReviewState.APPROVED_FOR_INTERNAL_USE)));

            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.PACKAGE, TARGET_ID);
            assertThat(p.getSafePreviewLabel()).contains("Dahili");
            assertThat(p.getSafePreviewLabel()).contains("İç Kullanım");
            assertThat(p.getSafePreviewLabel()).doesNotContainIgnoringCase("publish");
            assertThat(p.getSafePreviewLabel()).doesNotContainIgnoringCase("export");
        }

        @Test
        @DisplayName("REJECTED target preview is still available for review context")
        void rejectedPreviewAvailable() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(
                    ReviewTargetType.IMAGE_ASSET, TARGET_ID))
                    .thenReturn(Optional.of(record(ReviewTargetType.IMAGE_ASSET, TARGET_ID, ReviewState.REJECTED)));

            SafeAdminPreview p = previewService.getPreview(ReviewTargetType.IMAGE_ASSET, TARGET_ID);
            assertThat(p.isPreviewAvailable()).isTrue();
            assertThat(p.getCurrentReviewState()).isEqualTo(ReviewState.REJECTED);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. Boundary Regression
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("7. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("AdminReviewController endpoint count unchanged (12)")
        void endpointCountUnchanged() {
            long post = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long get = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(post).isEqualTo(7);
            assertThat(get).isEqualTo(5);
        }

        @Test
        @DisplayName("AdminPreviewService is not a Controller")
        void notController() {
            assertThat(AdminPreviewService.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
        }

        @Test
        @DisplayName("ReviewTargetContextResolver is not a Controller")
        void resolverNotController() {
            assertThat(ReviewTargetContextResolver.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
        }

        @Test
        @DisplayName("SafeAdminPreview is not a JPA Entity")
        void previewNotEntity() {
            assertThat(SafeAdminPreview.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("No V11 migration file exists")
        void noNewMigration() {
            assertThat(java.nio.file.Path.of(
                    "src/main/resources/db/migration/V11__fp013_preview.sql").toFile())
                    .doesNotExist();
        }

        @Test
        @DisplayName("ReviewState has no forbidden values")
        void noForbiddenStates() {
            Set<String> names = Arrays.stream(ReviewState.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED", "EXPORTED", "COMPLETED", "READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("ReviewActionType has no forbidden values")
        void noForbiddenActions() {
            Set<String> names = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISH", "EXPORT", "AUTO_APPROVE", "READY_FOR_PUBLISH");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. Handoff Safety Preserved
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("8. Handoff Safety Preserved")
    class HandoffSafety {

        @Test
        @DisplayName("ReviewHandoffSource has no forbidden values")
        void handoffSourceSafe() {
            Set<String> names = Arrays.stream(ReviewHandoffSource.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISH", "EXPORT", "PUBLIC_DELIVERY", "CDN");
        }

        @Test
        @DisplayName("ReviewHandoffResult has no forbidden fields")
        void handoffResultSafe() {
            Set<String> fields = Arrays.stream(ReviewHandoffResult.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }
    }
}
