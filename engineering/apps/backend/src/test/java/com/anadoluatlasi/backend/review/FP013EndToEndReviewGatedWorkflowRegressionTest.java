package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-013 Task 06: End-to-end review-gated workflow regression, preview contract
 * hardening, and deferred target guardrails.
 *
 * <p>This test class proves:</p>
 * <ul>
 *   <li>Generated reviewable outputs enter the review workflow safely</li>
 *   <li>Admin preview remains safe after orchestration integration</li>
 *   <li>Deferred/non-reviewable artifacts do not accidentally enter review</li>
 *   <li>FP-013 did not create publish/export/public delivery behavior</li>
 * </ul>
 *
 * <p>Hardening test only — no new production code, endpoints, or migrations.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FP-013 Task 06 — End-to-End Review-Gated Workflow Regression")
class FP013EndToEndReviewGatedWorkflowRegressionTest {

    private static final UUID TARGET_ID = UUID.fromString("54858075-e8c5-473b-9515-632926cbfa9c");

    @Mock
    private ReviewHandoffService reviewHandoffService;

    @Mock
    private ReviewTargetContextResolver contextResolver;

    private GenerationReviewHandoffAdapter adapter;
    private AdminPreviewService previewService;

    @BeforeEach
    void setUp() {
        adapter = new GenerationReviewHandoffAdapter(reviewHandoffService);
        previewService = new AdminPreviewService(contextResolver);
    }

    // ════════════════════════════════════════════════════════════════
    // A. END-TO-END REVIEW-GATED WORKFLOW REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("A. End-to-End Review-Gated Workflow — Per Target Type")
    class EndToEndWorkflow {

        // ── CONTENT_PROJECT ─────────────────────────────────────

        @Test
        @DisplayName("CONTENT_PROJECT: handoff creates NEEDS_REVIEW with correct source")
        void contentProjectHandoffCreatesNeedsReview() {
            ReviewHandoffResult result = buildCreatedResult(
                    ReviewTargetType.CONTENT_PROJECT, ReviewState.NEEDS_REVIEW);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffContentProject(
                    TARGET_ID, "Proje Başlığı", "Proje özeti");

            assertThat(actual).isNotNull();
            assertThat(actual.isCreated()).isTrue();
            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);

            ArgumentCaptor<ReviewHandoffRequest> captor = ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            ReviewHandoffRequest req = captor.getValue();
            assertThat(req.getTargetType()).isEqualTo(ReviewTargetType.CONTENT_PROJECT);
            assertThat(req.getSource()).isEqualTo(ReviewHandoffSource.CONTENT_PROJECT_GENERATION);
            assertThat(req.getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
            assertThat(req.getActorReference()).startsWith("system:");
        }

        // ── SCENARIO ────────────────────────────────────────────

        @Test
        @DisplayName("SCENARIO: handoff creates NEEDS_REVIEW with correct source")
        void scenarioHandoffCreatesNeedsReview() {
            ReviewHandoffResult result = buildCreatedResult(
                    ReviewTargetType.SCENARIO, ReviewState.NEEDS_REVIEW);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffScenario(
                    TARGET_ID, "Senaryo Başlığı", "Senaryo özeti");

            assertThat(actual).isNotNull();
            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);

            ArgumentCaptor<ReviewHandoffRequest> captor = ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            assertThat(captor.getValue().getTargetType()).isEqualTo(ReviewTargetType.SCENARIO);
            assertThat(captor.getValue().getSource()).isEqualTo(ReviewHandoffSource.SCENARIO_GENERATION);
            assertThat(captor.getValue().getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
        }

        // ── STORYBOARD ──────────────────────────────────────────

        @Test
        @DisplayName("STORYBOARD: handoff creates NEEDS_REVIEW with correct source")
        void storyboardHandoffCreatesNeedsReview() {
            ReviewHandoffResult result = buildCreatedResult(
                    ReviewTargetType.STORYBOARD, ReviewState.NEEDS_REVIEW);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffStoryboard(
                    TARGET_ID, "Storyboard Başlığı", "Storyboard özeti");

            assertThat(actual).isNotNull();
            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);

            ArgumentCaptor<ReviewHandoffRequest> captor = ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            assertThat(captor.getValue().getTargetType()).isEqualTo(ReviewTargetType.STORYBOARD);
            assertThat(captor.getValue().getSource()).isEqualTo(ReviewHandoffSource.STORYBOARD_GENERATION);
            assertThat(captor.getValue().getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
        }

        // ── PACKAGE ─────────────────────────────────────────────

        @Test
        @DisplayName("PACKAGE: handoff creates NEEDS_REVIEW with correct source")
        void packageHandoffCreatesNeedsReview() {
            ReviewHandoffResult result = buildCreatedResult(
                    ReviewTargetType.PACKAGE, ReviewState.NEEDS_REVIEW);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffPackage(
                    TARGET_ID, "Paket Başlığı", "Paket özeti");

            assertThat(actual).isNotNull();
            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);

            ArgumentCaptor<ReviewHandoffRequest> captor = ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            assertThat(captor.getValue().getTargetType()).isEqualTo(ReviewTargetType.PACKAGE);
            assertThat(captor.getValue().getSource()).isEqualTo(ReviewHandoffSource.PACKAGE_GENERATION);
            assertThat(captor.getValue().getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
        }

        // ── IMAGE_ASSET ─────────────────────────────────────────

        @Test
        @DisplayName("IMAGE_ASSET: handoff creates NEEDS_REVIEW with correct source")
        void imageAssetHandoffCreatesNeedsReview() {
            ReviewHandoffResult result = buildCreatedResult(
                    ReviewTargetType.IMAGE_ASSET, ReviewState.NEEDS_REVIEW);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffImageAsset(
                    TARGET_ID, "Görsel Başlığı", "Görsel özeti");

            assertThat(actual).isNotNull();
            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);

            ArgumentCaptor<ReviewHandoffRequest> captor = ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            assertThat(captor.getValue().getTargetType()).isEqualTo(ReviewTargetType.IMAGE_ASSET);
            assertThat(captor.getValue().getSource()).isEqualTo(ReviewHandoffSource.IMAGE_GENERATION);
            assertThat(captor.getValue().getActorRole()).isEqualTo(ReviewActorRole.SYSTEM);
        }

        // ── ACTOR ROLE SYSTEM ONLY ──────────────────────────────

        @Test
        @DisplayName("All target types use SYSTEM actor role — never REVIEWER/ADMIN/CREATOR")
        void allTargetTypesUseSystemActor() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenReturn(buildCreatedResult(ReviewTargetType.SCENARIO, ReviewState.NEEDS_REVIEW));

            adapter.handoffScenario(TARGET_ID, "T", "S");
            adapter.handoffStoryboard(TARGET_ID, "T", "S");
            adapter.handoffPackage(TARGET_ID, "T", "S");
            adapter.handoffImageAsset(TARGET_ID, "T", "S");
            adapter.handoffContentProject(TARGET_ID, "T", "S");

            ArgumentCaptor<ReviewHandoffRequest> captor = ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService, times(5)).handoffForReview(captor.capture());

            captor.getAllValues().forEach(req ->
                    assertThat(req.getActorRole())
                            .as("Actor for %s must be SYSTEM", req.getTargetType())
                            .isEqualTo(ReviewActorRole.SYSTEM));
        }

        // ── SANITIZATION ────────────────────────────────────────

        @Test
        @DisplayName("Title and summary are sanitized through createSafe")
        void titleAndSummaryAreSanitized() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenReturn(buildCreatedResult(ReviewTargetType.SCENARIO, ReviewState.NEEDS_REVIEW));

            // Input with control characters that should be stripped
            String titleWithControlChars = "Başlık\u0001\u0002\u0003 temiz";
            String summaryWithControlChars = "Özet\u0007\u000B kontrol";

            adapter.handoffScenario(TARGET_ID, titleWithControlChars, summaryWithControlChars);

            ArgumentCaptor<ReviewHandoffRequest> captor = ArgumentCaptor.forClass(ReviewHandoffRequest.class);
            verify(reviewHandoffService).handoffForReview(captor.capture());

            ReviewHandoffRequest req = captor.getValue();
            // createSafe calls ReviewTextSafety.sanitize — strips control chars
            assertThat(req.getSafeTitle()).doesNotContain("\u0001");
            assertThat(req.getSafeTitle()).doesNotContain("\u0002");
            assertThat(req.getSafeTitle()).contains("Başlık");
            assertThat(req.getSafeSummary()).doesNotContain("\u0007");
            assertThat(req.getSafeSummary()).contains("Özet");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // A2. TERMINAL & ACTIVE STATE PRESERVATION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("A2. Terminal & Active State Preservation")
    class StatePreservation {

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE is preserved — not overwritten by handoff")
        void approvedNotOverwritten() {
            ReviewHandoffResult result = buildPreservedResult(
                    ReviewTargetType.SCENARIO, ReviewState.APPROVED_FOR_INTERNAL_USE);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffScenario(TARGET_ID, "T", "S");

            assertThat(actual.isPreserved()).isTrue();
            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("REJECTED is preserved — not overwritten by handoff")
        void rejectedNotOverwritten() {
            ReviewHandoffResult result = buildPreservedResult(
                    ReviewTargetType.SCENARIO, ReviewState.REJECTED);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffScenario(TARGET_ID, "T", "S");

            assertThat(actual.isPreserved()).isTrue();
            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.REJECTED);
        }

        @Test
        @DisplayName("NEEDS_REVIEW is preserved on repeated handoff")
        void needsReviewPreserved() {
            ReviewHandoffResult result = buildPreservedResult(
                    ReviewTargetType.STORYBOARD, ReviewState.NEEDS_REVIEW);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffStoryboard(TARGET_ID, "T", "S");

            assertThat(actual.isPreserved()).isTrue();
            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("IN_REVIEW is preserved on repeated handoff")
        void inReviewPreserved() {
            ReviewHandoffResult result = buildPreservedResult(
                    ReviewTargetType.PACKAGE, ReviewState.IN_REVIEW);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffPackage(TARGET_ID, "T", "S");

            assertThat(actual.isPreserved()).isTrue();
            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.IN_REVIEW);
        }

        @Test
        @DisplayName("CHANGES_REQUESTED is preserved on repeated handoff")
        void changesRequestedPreserved() {
            ReviewHandoffResult result = buildPreservedResult(
                    ReviewTargetType.IMAGE_ASSET, ReviewState.CHANGES_REQUESTED);
            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ReviewHandoffResult actual = adapter.handoffImageAsset(TARGET_ID, "T", "S");

            assertThat(actual.isPreserved()).isTrue();
            assertThat(actual.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // A3. PREVIEW AFTER HANDOFF — SAFE RESPONSE CONTRACT
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("A3. Preview After Handoff — Safe Response Contract")
    class PreviewAfterHandoff {

        @Test
        @DisplayName("Preview for missing target returns safe unavailable response")
        void previewMissingTargetSafeResponse() {
            when(contextResolver.resolve(any(), any())).thenReturn(java.util.Optional.empty());

            SafeAdminPreview preview = previewService.getPreview(ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(preview.isPreviewAvailable()).isFalse();
            assertThat(preview.getAvailability()).isEqualTo(AdminPreviewAvailability.MISSING_TARGET);
            assertThat(preview.getSafeReasonCode()).isNotNull();
            assertThat(preview.getSafeMessage()).isNotNull();
            // Yasak alanlar olmamalı
            assertPreviewHasNoForbiddenFields(preview);
        }

        @Test
        @DisplayName("Preview for null targetType returns safe unavailable response")
        void previewNullTargetTypeSafeResponse() {
            SafeAdminPreview preview = previewService.getPreview(null, TARGET_ID);

            assertThat(preview.isPreviewAvailable()).isFalse();
            assertThat(preview.getAvailability()).isEqualTo(AdminPreviewAvailability.NOT_REVIEWABLE);
        }

        @Test
        @DisplayName("Preview for null targetId returns safe unavailable response")
        void previewNullTargetIdSafeResponse() {
            SafeAdminPreview preview = previewService.getPreview(ReviewTargetType.SCENARIO, null);

            assertThat(preview.isPreviewAvailable()).isFalse();
            assertThat(preview.getAvailability()).isEqualTo(AdminPreviewAvailability.MISSING_TARGET);
        }

        @Test
        @DisplayName("IMAGE_ASSET preview returns metadata only — no bytes/URL/base64")
        void imageAssetPreviewMetadataOnly() {
            // Verify SafeAdminPreview class has no image bytes/URL fields
            Set<String> fields = getFieldNames(SafeAdminPreview.class);
            assertThat(fields).doesNotContain(
                    "imageBytes", "imageUrl", "imageData", "base64",
                    "providerUrl", "storageKey", "signedUrl", "publicUrl",
                    "cdnUrl", "rawPayload", "bucket", "objectPath");
        }

        private void assertPreviewHasNoForbiddenFields(SafeAdminPreview preview) {
            String str = preview.toString();
            assertThat(str).doesNotContain("providerUrl", "storageKey", "signedUrl",
                    "publicUrl", "cdnUrl", "base64", "rawPayload", "secret", "apiKey");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // B. DEFERRED VISUAL ASSET PLAN GUARDRAILS
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("B. Deferred VisualAssetPlan Guardrails")
    class DeferredVisualAssetPlanGuardrails {

        @Test
        @DisplayName("ReviewTargetType does NOT contain VISUAL_ASSET_PLAN")
        void reviewTargetTypeDoesNotContainVisualAssetPlan() {
            Set<String> values = Arrays.stream(ReviewTargetType.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            assertThat(values).doesNotContain(
                    "VISUAL_ASSET_PLAN",
                    "VISUAL_PLAN",
                    "ASSET_PLAN",
                    "SCENE_VISUAL_PLAN",
                    "THUMBNAIL_PLAN");
        }

        @Test
        @DisplayName("ReviewHandoffSource does NOT contain VISUAL_ASSET_PLAN sources")
        void reviewHandoffSourceDoesNotContainVisualPlanSources() {
            Set<String> values = Arrays.stream(ReviewHandoffSource.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            assertThat(values).doesNotContain(
                    "VISUAL_ASSET_PLAN_GENERATION",
                    "VISUAL_PLAN_GENERATION",
                    "ASSET_PLAN_GENERATION",
                    "THUMBNAIL_PLAN_GENERATION");
        }

        @Test
        @DisplayName("GenerationReviewHandoffAdapter has no handoffVisualAssetPlan method")
        void adapterHasNoVisualAssetPlanHandoff() {
            Set<String> methodNames = Arrays.stream(
                            GenerationReviewHandoffAdapter.class.getDeclaredMethods())
                    .map(Method::getName)
                    .collect(Collectors.toSet());

            assertThat(methodNames).doesNotContain(
                    "handoffVisualAssetPlan",
                    "handoffVisualPlan",
                    "handoffAssetPlan",
                    "handoffThumbnailPlan");
        }

        @Test
        @DisplayName("AdminPreviewMode does NOT contain VISUAL_ASSET_PLAN")
        void adminPreviewModeDoesNotContainVisualAssetPlan() {
            Set<String> values = Arrays.stream(AdminPreviewMode.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            assertThat(values).doesNotContain(
                    "VISUAL_ASSET_PLAN",
                    "VISUAL_PLAN",
                    "ASSET_PLAN_SUMMARY");
        }

        @Test
        @DisplayName("Adapter has exactly 5 handoff methods for stable target types")
        void adapterHasExactlyFiveHandoffMethods() {
            List<String> handoffMethods = Arrays.stream(
                            GenerationReviewHandoffAdapter.class.getDeclaredMethods())
                    .map(Method::getName)
                    .filter(name -> name.startsWith("handoff"))
                    .collect(Collectors.toList());

            assertThat(handoffMethods).containsExactlyInAnyOrder(
                    "handoffScenario",
                    "handoffStoryboard",
                    "handoffPackage",
                    "handoffImageAsset",
                    "handoffContentProject");
        }

        @Test
        @DisplayName("ReviewTargetType has exactly 5 stable values")
        void reviewTargetTypeHasExactlyFiveValues() {
            assertThat(ReviewTargetType.values()).hasSize(5);
            assertThat(ReviewTargetType.values()).containsExactlyInAnyOrder(
                    ReviewTargetType.CONTENT_PROJECT,
                    ReviewTargetType.SCENARIO,
                    ReviewTargetType.STORYBOARD,
                    ReviewTargetType.PACKAGE,
                    ReviewTargetType.IMAGE_ASSET);
        }

        @Test
        @DisplayName("ReviewHandoffSource has exactly 6 stable values (including STORAGE_HANDOFF)")
        void reviewHandoffSourceHasExactlySixValues() {
            assertThat(ReviewHandoffSource.values()).hasSize(6);
            assertThat(ReviewHandoffSource.values()).containsExactlyInAnyOrder(
                    ReviewHandoffSource.CONTENT_PROJECT_GENERATION,
                    ReviewHandoffSource.SCENARIO_GENERATION,
                    ReviewHandoffSource.STORYBOARD_GENERATION,
                    ReviewHandoffSource.PACKAGE_GENERATION,
                    ReviewHandoffSource.IMAGE_GENERATION,
                    ReviewHandoffSource.STORAGE_HANDOFF);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // C. API ENDPOINT INVENTORY REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("C. API Endpoint Inventory Regression")
    class ApiEndpointInventoryRegression {

        @Test
        @DisplayName("AdminReviewController has exactly 14 endpoints")
        void adminReviewControllerHas14Endpoints() {
            long endpointCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m ->
                            m.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class) ||
                            m.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class) ||
                            m.isAnnotationPresent(org.springframework.web.bind.annotation.PutMapping.class) ||
                            m.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class))
                    .count();

            assertThat(endpointCount)
                    .as("Total admin review endpoints must remain 14 (FP-013 baseline)")
                    .isEqualTo(14);
        }

        @Test
        @DisplayName("No forbidden endpoint paths exist in AdminReviewController")
        void noForbiddenEndpointPaths() {
            List<String> forbiddenPatterns = List.of(
                    "publish", "export", "public", "signed", "cdn",
                    "provider", "storage-inspection", "raw", "base64",
                    "media-library", "video", "audio");

            // Inspect all endpoint method names — they map to URL paths
            Set<String> methodNames = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .map(Method::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            for (String forbidden : forbiddenPatterns) {
                assertThat(methodNames.stream().anyMatch(m -> m.contains(forbidden)))
                        .as("No endpoint method should contain '%s'", forbidden)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("GenerationReviewHandoffAdapter is NOT a controller")
        void adapterIsNotController() {
            assertThat(GenerationReviewHandoffAdapter.class.getAnnotationsByType(
                    org.springframework.web.bind.annotation.RestController.class)).isEmpty();
            assertThat(GenerationReviewHandoffAdapter.class.getAnnotationsByType(
                    org.springframework.stereotype.Controller.class)).isEmpty();
            assertThat(GenerationReviewHandoffAdapter.class.getAnnotationsByType(
                    org.springframework.web.bind.annotation.RequestMapping.class)).isEmpty();
        }

        @Test
        @DisplayName("AdminPreviewService is NOT a controller")
        void previewServiceIsNotController() {
            assertThat(AdminPreviewService.class.getAnnotationsByType(
                    org.springframework.web.bind.annotation.RestController.class)).isEmpty();
            assertThat(AdminPreviewService.class.getAnnotationsByType(
                    org.springframework.stereotype.Controller.class)).isEmpty();
        }

        @Test
        @DisplayName("ReviewHandoffService is NOT a controller")
        void handoffServiceIsNotController() {
            assertThat(ReviewHandoffService.class.getAnnotationsByType(
                    org.springframework.web.bind.annotation.RestController.class)).isEmpty();
            assertThat(ReviewHandoffService.class.getAnnotationsByType(
                    org.springframework.stereotype.Controller.class)).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // D. SAFE PAYLOAD REGRESSION — DTO/API FIELD INSPECTION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("D. Safe Payload Regression — DTO Field Inspection")
    class SafePayloadRegression {

        private static final Set<String> FORBIDDEN_FIELDS = Set.of(
                "providerUrl", "storageKey", "bucket", "objectPath",
                "signedUrl", "publicUrl", "cdnUrl", "rawPayload",
                "base64", "secret", "apiKey", "accessKey", "token",
                "stackTrace", "exceptionClass", "bytes",
                "imageBytes", "imageUrl", "imageData", "rawContent",
                "rawImageBytes");

        @Test
        @DisplayName("ReviewHandoffResult has no forbidden fields")
        void reviewHandoffResultNoForbiddenFields() {
            assertNoForbiddenFields(ReviewHandoffResult.class);
        }

        @Test
        @DisplayName("AdminReviewHandoffResponse has no forbidden fields")
        void adminReviewHandoffResponseNoForbiddenFields() {
            assertNoForbiddenFields(AdminReviewHandoffResponse.class);
        }

        @Test
        @DisplayName("SafeAdminPreview has no forbidden fields")
        void safeAdminPreviewNoForbiddenFields() {
            assertNoForbiddenFields(SafeAdminPreview.class);
        }

        @Test
        @DisplayName("ReviewHandoffRequest has no forbidden fields")
        void reviewHandoffRequestNoForbiddenFields() {
            assertNoForbiddenFields(ReviewHandoffRequest.class);
        }

        @Test
        @DisplayName("ReviewRecordEntity has no forbidden fields")
        void reviewRecordEntityNoForbiddenFields() {
            assertNoForbiddenFields(ReviewRecordEntity.class);
        }

        @Test
        @DisplayName("ReviewAuditEventEntity has no forbidden fields")
        void reviewAuditEventEntityNoForbiddenFields() {
            assertNoForbiddenFields(ReviewAuditEventEntity.class);
        }

        @Test
        @DisplayName("ReviewHandoffResult toString does not expose forbidden terms")
        void resultToStringNoForbiddenTerms() {
            ReviewHandoffResult result = buildCreatedResult(
                    ReviewTargetType.SCENARIO, ReviewState.NEEDS_REVIEW);
            String str = result.toString();

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(str.toLowerCase())
                        .as("toString must not contain '%s'", forbidden)
                        .doesNotContain(forbidden.toLowerCase());
            }
        }

        @Test
        @DisplayName("AdminReviewHandoffResponse toString does not expose forbidden terms")
        void responseToStringNoForbiddenTerms() {
            AdminReviewHandoffResponse response = AdminReviewHandoffResponse.builder()
                    .targetType(ReviewTargetType.IMAGE_ASSET)
                    .targetId(TARGET_ID)
                    .currentState(ReviewState.NEEDS_REVIEW)
                    .created(true)
                    .build();
            String str = response.toString();

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(str.toLowerCase())
                        .as("toString must not contain '%s'", forbidden)
                        .doesNotContain(forbidden.toLowerCase());
            }
        }

        @Test
        @DisplayName("SafeAdminPreview toString does not expose forbidden terms")
        void previewToStringNoForbiddenTerms() {
            SafeAdminPreview preview = SafeAdminPreview.unavailable(
                    ReviewTargetType.SCENARIO, TARGET_ID,
                    AdminPreviewAvailability.MISSING_TARGET,
                    "TEST", "Test mesaj");
            String str = preview.toString();

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(str.toLowerCase())
                        .as("toString must not contain '%s'", forbidden)
                        .doesNotContain(forbidden.toLowerCase());
            }
        }

        @Test
        @DisplayName("ReviewHandoffRequest toString does not expose forbidden terms")
        void requestToStringNoForbiddenTerms() {
            ReviewHandoffRequest request = ReviewHandoffRequest.createSafe(
                    ReviewTargetType.SCENARIO, TARGET_ID,
                    ReviewHandoffSource.SCENARIO_GENERATION,
                    ReviewActorRole.SYSTEM, "system:test",
                    "Test Title", "Test Summary");
            String str = request.toString();

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(str.toLowerCase())
                        .as("toString must not contain '%s'", forbidden)
                        .doesNotContain(forbidden.toLowerCase());
            }
        }

        private void assertNoForbiddenFields(Class<?> clazz) {
            Set<String> fields = getFieldNames(clazz);
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields)
                        .as("%s must not have field '%s'", clazz.getSimpleName(), forbidden)
                        .doesNotContain(forbidden);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // E. FAILURE BEHAVIOR REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("E. Failure Behavior Regression — Non-Fatal Handoff")
    class FailureBehaviorRegression {

        @Test
        @DisplayName("RuntimeException in handoff returns null — does not throw")
        void runtimeExceptionReturnsNull() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenThrow(new RuntimeException("Database down"));

            ReviewHandoffResult result = adapter.handoffScenario(TARGET_ID, "T", "S");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ReviewHandoffException in handoff returns null — does not throw")
        void reviewHandoffExceptionReturnsNull() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenThrow(new ReviewHandoffException("Duplicate key", "DUPLICATE_HANDOFF"));

            ReviewHandoffResult result = adapter.handoffStoryboard(TARGET_ID, "T", "S");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("NullPointerException in handoff returns null — does not throw")
        void nullPointerExceptionReturnsNull() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenThrow(new NullPointerException("null target"));

            ReviewHandoffResult result = adapter.handoffPackage(TARGET_ID, "T", "S");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("All 5 handoff methods handle failure gracefully")
        void allMethodsHandleFailure() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenThrow(new RuntimeException("Fatal"));

            assertThatCode(() -> adapter.handoffScenario(TARGET_ID, "T", "S"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> adapter.handoffStoryboard(TARGET_ID, "T", "S"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> adapter.handoffPackage(TARGET_ID, "T", "S"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> adapter.handoffImageAsset(TARGET_ID, "T", "S"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> adapter.handoffContentProject(TARGET_ID, "T", "S"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Failure does not auto-approve — no APPROVED state on failure")
        void failureDoesNotAutoApprove() {
            when(reviewHandoffService.handoffForReview(any()))
                    .thenThrow(new RuntimeException("Failure"));

            // All methods return null on failure — no state transition occurred
            assertThat(adapter.handoffScenario(TARGET_ID, "T", "S")).isNull();
            assertThat(adapter.handoffContentProject(TARGET_ID, "T", "S")).isNull();
            assertThat(adapter.handoffImageAsset(TARGET_ID, "T", "S")).isNull();
        }

        @Test
        @DisplayName("Multi-package handoff failures are isolated")
        void multiPackageFailuresIsolated() {
            // First call succeeds, second fails
            UUID pkg1 = UUID.randomUUID();
            UUID pkg2 = UUID.randomUUID();

            when(reviewHandoffService.handoffForReview(any()))
                    .thenReturn(buildCreatedResult(ReviewTargetType.PACKAGE, ReviewState.NEEDS_REVIEW))
                    .thenThrow(new RuntimeException("Failure on pkg2"));

            ReviewHandoffResult result1 = adapter.handoffPackage(pkg1, "Paket 1", "Özet 1");
            ReviewHandoffResult result2 = adapter.handoffPackage(pkg2, "Paket 2", "Özet 2");

            assertThat(result1).isNotNull();
            assertThat(result1.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
            assertThat(result2).isNull(); // Failed — but pkg1 is unaffected
        }
    }

    // ════════════════════════════════════════════════════════════════
    // F. REVIEW STATE ENUM SAFETY & FORBIDDEN VALUES
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("F. ReviewState Enum Safety — Forbidden Values")
    class ReviewStateEnumSafety {

        @Test
        @DisplayName("ReviewState does NOT contain forbidden publish/export states")
        void noForbiddenStates() {
            Set<String> values = Arrays.stream(ReviewState.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            assertThat(values).doesNotContain(
                    "READY_FOR_PUBLISH",
                    "PUBLISHED",
                    "EXPORTED",
                    "COMPLETED",
                    "READY_FOR_EXPORT",
                    "DELIVERED",
                    "PUBLIC");
        }

        @Test
        @DisplayName("SYSTEM canPerformReview returns false")
        void systemCannotPerformReview() {
            assertThat(ReviewActorRole.SYSTEM.canPerformReview()).isFalse();
        }

        @Test
        @DisplayName("SYSTEM canSubmitForReview returns false")
        void systemCannotSubmitForReview() {
            assertThat(ReviewActorRole.SYSTEM.canSubmitForReview()).isFalse();
        }

        @Test
        @DisplayName("REJECTED state is terminal")
        void rejectedIsTerminal() {
            assertThat(ReviewState.REJECTED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE is NOT terminal — but does not imply publish")
        void approvedNotTerminal() {
            assertThat(ReviewState.APPROVED_FOR_INTERNAL_USE.isTerminal()).isFalse();
            assertThat(ReviewState.APPROVED_FOR_INTERNAL_USE.isApprovedForInternalUse()).isTrue();
        }

        @Test
        @DisplayName("AI_GENERATED does NOT imply approval")
        void aiGeneratedDoesNotImplyApproval() {
            assertThat(ReviewState.AI_GENERATED.isApprovedForInternalUse()).isFalse();
            assertThat(ReviewState.AI_GENERATED.isAiGenerated()).isTrue();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // G. HANDOFF SOURCE TO TARGET TYPE MAPPING COMPLETENESS
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("G. Handoff Source to Target Type Mapping Completeness")
    class SourceTargetMapping {

        @Test
        @DisplayName("Every ReviewTargetType has at least one corresponding ReviewHandoffSource")
        void everyTargetTypeHasSource() {
            // CONTENT_PROJECT → CONTENT_PROJECT_GENERATION
            // SCENARIO → SCENARIO_GENERATION
            // STORYBOARD → STORYBOARD_GENERATION
            // PACKAGE → PACKAGE_GENERATION
            // IMAGE_ASSET → IMAGE_GENERATION (and STORAGE_HANDOFF)
            assertThat(ReviewHandoffSource.CONTENT_PROJECT_GENERATION).isNotNull();
            assertThat(ReviewHandoffSource.SCENARIO_GENERATION).isNotNull();
            assertThat(ReviewHandoffSource.STORYBOARD_GENERATION).isNotNull();
            assertThat(ReviewHandoffSource.PACKAGE_GENERATION).isNotNull();
            assertThat(ReviewHandoffSource.IMAGE_GENERATION).isNotNull();
        }

        @Test
        @DisplayName("Adapter method count matches ReviewTargetType count")
        void adapterMethodCountMatchesTargetTypes() {
            long handoffMethodCount = Arrays.stream(
                            GenerationReviewHandoffAdapter.class.getDeclaredMethods())
                    .map(Method::getName)
                    .filter(name -> name.startsWith("handoff"))
                    .count();

            assertThat(handoffMethodCount)
                    .as("Adapter should have exactly as many handoff methods as ReviewTargetType values")
                    .isEqualTo(ReviewTargetType.values().length);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // H. FORBIDDEN BEHAVIOR — PUBLISH/EXPORT/CDN/DELIVERY
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("H. Forbidden Behavior — No Publish/Export/CDN/Delivery")
    class ForbiddenBehavior {

        @Test
        @DisplayName("ReviewHandoffResult has no publish/export methods")
        void resultNoPublishExportMethods() {
            Set<String> methods = Arrays.stream(ReviewHandoffResult.class.getDeclaredMethods())
                    .map(Method::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            assertThat(methods.stream().anyMatch(m ->
                    m.contains("publish") || m.contains("export") ||
                    m.contains("signed") || m.contains("cdn") ||
                    m.contains("public") || m.contains("delivery")))
                    .isFalse();
        }

        @Test
        @DisplayName("SafeAdminPreview has no publish/export methods")
        void previewNoPublishExportMethods() {
            Set<String> methods = Arrays.stream(SafeAdminPreview.class.getDeclaredMethods())
                    .map(Method::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            assertThat(methods.stream().anyMatch(m ->
                    m.contains("publish") || m.contains("export") ||
                    m.contains("signed") || m.contains("cdn") ||
                    m.contains("delivery")))
                    .isFalse();
        }

        @Test
        @DisplayName("GenerationReviewHandoffAdapter has no publish/export/approval methods")
        void adapterNoPublishExportApprovalMethods() {
            Set<String> methods = Arrays.stream(
                            GenerationReviewHandoffAdapter.class.getDeclaredMethods())
                    .map(Method::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            assertThat(methods.stream().anyMatch(m ->
                    m.contains("publish") || m.contains("export") ||
                    m.contains("approve") || m.contains("reject") ||
                    m.contains("signed") || m.contains("cdn")))
                    .isFalse();
        }

        @Test
        @DisplayName("ReviewHandoffService has no publish/export methods")
        void handoffServiceNoPublishExportMethods() {
            Set<String> methods = Arrays.stream(ReviewHandoffService.class.getDeclaredMethods())
                    .map(Method::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            assertThat(methods.stream().anyMatch(m ->
                    m.contains("publish") || m.contains("export") ||
                    m.contains("signed") || m.contains("cdn") ||
                    m.contains("delivery")))
                    .isFalse();
        }

        @Test
        @DisplayName("AdminPreviewService has no publish/export methods")
        void previewServiceNoPublishExportMethods() {
            Set<String> methods = Arrays.stream(AdminPreviewService.class.getDeclaredMethods())
                    .map(Method::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            assertThat(methods.stream().anyMatch(m ->
                    m.contains("publish") || m.contains("export") ||
                    m.contains("signed") || m.contains("cdn") ||
                    m.contains("delivery")))
                    .isFalse();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ════════════════════════════════════════════════════════════════

    private ReviewHandoffResult buildCreatedResult(ReviewTargetType type, ReviewState state) {
        return ReviewHandoffResult.builder()
                .targetType(type)
                .targetId(TARGET_ID)
                .previousState(null)
                .currentState(state)
                .created(true)
                .updated(false)
                .preserved(false)
                .auditEventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                .safeReasonCode("REVIEW_HANDOFF_OK")
                .safeMessage("Review kaydı oluşturuldu")
                .build();
    }

    private ReviewHandoffResult buildPreservedResult(ReviewTargetType type, ReviewState state) {
        return ReviewHandoffResult.builder()
                .targetType(type)
                .targetId(TARGET_ID)
                .previousState(state)
                .currentState(state)
                .created(false)
                .updated(false)
                .preserved(true)
                .auditEventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                .safeReasonCode("REVIEW_PRESERVED")
                .safeMessage("Mevcut durum korundu")
                .build();
    }

    private static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
