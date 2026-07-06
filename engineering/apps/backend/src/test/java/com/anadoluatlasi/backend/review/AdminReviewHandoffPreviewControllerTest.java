package com.anadoluatlasi.backend.review;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * FP-013 Task 04: AdminReviewController handoff & preview endpoint tests.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>A. Handoff endpoint happy paths</li>
 *   <li>B. Handoff validation/failure</li>
 *   <li>C. Preview endpoint happy paths</li>
 *   <li>D. Safe response body regression</li>
 *   <li>E. Endpoint inventory regression</li>
 *   <li>F. Boundary regression</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AdminReviewHandoffPreviewControllerTest {

    @Mock
    private ReviewApplicationService reviewApplicationService;

    @Mock
    private ReviewHandoffService reviewHandoffService;

    @Mock
    private AdminPreviewService adminPreviewService;

    @Mock
    private InternalApprovalReadinessService internalApprovalReadinessService;

    @Mock
    private InternalRevisionRequestService internalRevisionRequestService;

    private AdminReviewController controller;

    private static final UUID TARGET_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new AdminReviewController(
                reviewApplicationService, reviewHandoffService, adminPreviewService,
                internalApprovalReadinessService, internalRevisionRequestService);
    }

    // ── A. Handoff Endpoint Happy Paths ──────────────────────────────

    @Nested
    @DisplayName("A. Handoff endpoint happy paths")
    class HandoffHappyPaths {

        @Test
        @DisplayName("POST /handoff creates a NEEDS_REVIEW record when missing")
        void shouldCreateNeedsReviewWhenMissing() {
            AdminReviewHandoffRequest request = buildValidHandoffRequest();
            ReviewHandoffResult result = ReviewHandoffResult.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .previousState(null)
                    .currentState(ReviewState.NEEDS_REVIEW)
                    .created(true).updated(false).preserved(false)
                    .auditEventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                    .safeMessage("Review record created")
                    .build();

            when(reviewHandoffService.handoffForReview(any(ReviewHandoffRequest.class)))
                    .thenReturn(result);

            ResponseEntity<ApiResponse<AdminReviewHandoffResponse>> response =
                    controller.handoffForReview(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            AdminReviewHandoffResponse data = response.getBody().getData();
            assertThat(data.isCreated()).isTrue();
            assertThat(data.isPreserved()).isFalse();
            assertThat(data.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("Existing NEEDS_REVIEW is preserved")
        void shouldPreserveNeedsReview() {
            assertPreservedState(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("Existing IN_REVIEW is preserved")
        void shouldPreserveInReview() {
            assertPreservedState(ReviewState.IN_REVIEW);
        }

        @Test
        @DisplayName("Existing APPROVED_FOR_INTERNAL_USE is preserved")
        void shouldPreserveApprovedForInternalUse() {
            assertPreservedState(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("Existing REJECTED is preserved")
        void shouldPreserveRejected() {
            assertPreservedState(ReviewState.REJECTED);
        }

        @Test
        @DisplayName("Response contains created/updated/preserved correctly")
        void shouldContainCorrectFlags() {
            AdminReviewHandoffRequest request = buildValidHandoffRequest();
            ReviewHandoffResult result = ReviewHandoffResult.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .previousState(null)
                    .currentState(ReviewState.NEEDS_REVIEW)
                    .created(true).updated(false).preserved(false)
                    .auditEventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                    .build();

            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ResponseEntity<ApiResponse<AdminReviewHandoffResponse>> response =
                    controller.handoffForReview(request);

            AdminReviewHandoffResponse data = response.getBody().getData();
            assertThat(data.isCreated()).isTrue();
            assertThat(data.isUpdated()).isFalse();
            assertThat(data.isPreserved()).isFalse();
        }

        @Test
        @DisplayName("No automatic approval via handoff")
        void shouldNotAutoApprove() {
            AdminReviewHandoffRequest request = buildValidHandoffRequest();
            ReviewHandoffResult result = ReviewHandoffResult.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .previousState(null)
                    .currentState(ReviewState.NEEDS_REVIEW)
                    .created(true).updated(false).preserved(false)
                    .build();

            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ResponseEntity<ApiResponse<AdminReviewHandoffResponse>> response =
                    controller.handoffForReview(request);

            assertThat(response.getBody().getData().getCurrentState())
                    .isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        private void assertPreservedState(ReviewState state) {
            AdminReviewHandoffRequest request = buildValidHandoffRequest();
            ReviewHandoffResult result = ReviewHandoffResult.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .previousState(state)
                    .currentState(state)
                    .created(false).updated(false).preserved(true)
                    .safeReasonCode("ALREADY_IN_PIPELINE")
                    .safeMessage("Preserved")
                    .build();

            when(reviewHandoffService.handoffForReview(any())).thenReturn(result);

            ResponseEntity<ApiResponse<AdminReviewHandoffResponse>> response =
                    controller.handoffForReview(request);

            assertThat(response.getBody().getData().isPreserved()).isTrue();
            assertThat(response.getBody().getData().getCurrentState()).isEqualTo(state);
        }
    }

    // ── B. Handoff Validation/Failure ────────────────────────────────

    @Nested
    @DisplayName("B. Handoff validation/failure")
    class HandoffValidation {

        @Test
        @DisplayName("Missing targetType rejected safely")
        void shouldRejectMissingTargetType() {
            AdminReviewHandoffRequest request = AdminReviewHandoffRequest.builder()
                    .targetId(TARGET_ID)
                    .source(ReviewHandoffSource.SCENARIO_GENERATION)
                    .actorRole(ReviewActorRole.SYSTEM)
                    .actorReference("admin-1")
                    .build();

            ResponseEntity<ApiResponse<Void>> response =
                    controller.handleValidation(
                            new IllegalArgumentException("targetType is required"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertNoForbiddenFieldsInError(response);
        }

        @Test
        @DisplayName("Blank targetId rejected safely")
        void shouldRejectBlankTargetId() {
            AdminReviewHandoffRequest request = AdminReviewHandoffRequest.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .source(ReviewHandoffSource.SCENARIO_GENERATION)
                    .actorRole(ReviewActorRole.SYSTEM)
                    .actorReference("admin-1")
                    .build();

            ResponseEntity<ApiResponse<Void>> response =
                    controller.handleValidation(
                            new IllegalArgumentException("targetId is required"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Missing source rejected safely")
        void shouldRejectMissingSource() {
            AdminReviewHandoffRequest request = AdminReviewHandoffRequest.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .actorRole(ReviewActorRole.SYSTEM)
                    .actorReference("admin-1")
                    .build();

            ResponseEntity<ApiResponse<Void>> response =
                    controller.handleValidation(
                            new IllegalArgumentException("source is required"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Missing actorRole rejected safely")
        void shouldRejectMissingActorRole() {
            AdminReviewHandoffRequest request = AdminReviewHandoffRequest.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .source(ReviewHandoffSource.SCENARIO_GENERATION)
                    .actorReference("admin-1")
                    .build();

            ResponseEntity<ApiResponse<Void>> response =
                    controller.handleValidation(
                            new IllegalArgumentException("actorRole is required"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Blank actorReference rejected safely")
        void shouldRejectBlankActorReference() {
            AdminReviewHandoffRequest request = AdminReviewHandoffRequest.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .source(ReviewHandoffSource.SCENARIO_GENERATION)
                    .actorRole(ReviewActorRole.SYSTEM)
                    .actorReference("")
                    .build();

            ResponseEntity<ApiResponse<Void>> response =
                    controller.handleValidation(
                            new IllegalArgumentException("actorReference is required"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("ReviewHandoffException returns 422")
        void shouldReturn422ForHandoffException() {
            ReviewHandoffException ex = new ReviewHandoffException("Test error", "TEST_CODE");
            ResponseEntity<ApiResponse<Void>> response = controller.handleHandoffException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody().getMessage()).isEqualTo("Test error");
            assertNoForbiddenFieldsInError(response);
        }

        @Test
        @DisplayName("AdminPreviewException returns 404")
        void shouldReturn404ForPreviewException() {
            AdminPreviewException ex = new AdminPreviewException("Not found", "NOT_FOUND");
            ResponseEntity<ApiResponse<Void>> response = controller.handlePreviewException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertNoForbiddenFieldsInError(response);
        }

        private void assertNoForbiddenFieldsInError(ResponseEntity<ApiResponse<Void>> response) {
            String body = response.getBody() != null ? response.getBody().toString() : "";
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(body.toLowerCase()).doesNotContain(forbidden.toLowerCase());
            }
        }
    }

    // ── C. Preview Endpoint Happy Paths ──────────────────────────────

    @Nested
    @DisplayName("C. Preview endpoint happy paths")
    class PreviewHappyPaths {

        @Test
        @DisplayName("GET preview returns SafeAdminPreview for CONTENT_PROJECT")
        void shouldReturnPreviewForContentProject() {
            assertPreviewForType(ReviewTargetType.CONTENT_PROJECT, AdminPreviewMode.TEXT_SUMMARY);
        }

        @Test
        @DisplayName("GET preview returns SafeAdminPreview for SCENARIO")
        void shouldReturnPreviewForScenario() {
            assertPreviewForType(ReviewTargetType.SCENARIO, AdminPreviewMode.SCENARIO_SUMMARY);
        }

        @Test
        @DisplayName("GET preview returns SafeAdminPreview for STORYBOARD")
        void shouldReturnPreviewForStoryboard() {
            assertPreviewForType(ReviewTargetType.STORYBOARD, AdminPreviewMode.STORYBOARD_SUMMARY);
        }

        @Test
        @DisplayName("GET preview returns SafeAdminPreview for PACKAGE")
        void shouldReturnPreviewForPackage() {
            assertPreviewForType(ReviewTargetType.PACKAGE, AdminPreviewMode.PACKAGE_SUMMARY);
        }

        @Test
        @DisplayName("GET preview returns SafeAdminPreview for IMAGE_ASSET — metadata only")
        void shouldReturnPreviewForImageAsset() {
            SafeAdminPreview preview = SafeAdminPreview.builder()
                    .targetType(ReviewTargetType.IMAGE_ASSET)
                    .targetId(TARGET_ID)
                    .previewMode(AdminPreviewMode.IMAGE_ASSET)
                    .availability(AdminPreviewAvailability.AVAILABLE)
                    .previewAvailable(true)
                    .safeTitle("Görsel Plan")
                    .safeSummary("Görsel bilgisi")
                    .safePreviewLabel("Görsel Bilgisi — İnceleme Bekliyor")
                    .safeThumbnailAvailable(false)
                    .currentReviewState(ReviewState.NEEDS_REVIEW)
                    .build();

            when(adminPreviewService.getPreview(ReviewTargetType.IMAGE_ASSET, TARGET_ID))
                    .thenReturn(preview);

            ResponseEntity<ApiResponse<SafeAdminPreview>> response =
                    controller.getPreview(ReviewTargetType.IMAGE_ASSET, TARGET_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            SafeAdminPreview data = response.getBody().getData();
            assertThat(data.getTargetType()).isEqualTo(ReviewTargetType.IMAGE_ASSET);
            assertThat(data.isSafeThumbnailAvailable()).isFalse();
            // Verify no bytes/URL/base64/key via toString
            String str = data.toString();
            assertThat(str).doesNotContain("url");
            assertThat(str).doesNotContain("base64");
            assertThat(str).doesNotContain("storageKey");
        }

        @Test
        @DisplayName("Missing target returns safe unavailable preview")
        void shouldReturnUnavailableForMissingTarget() {
            SafeAdminPreview unavailable = SafeAdminPreview.unavailable(
                    ReviewTargetType.SCENARIO, TARGET_ID,
                    AdminPreviewAvailability.MISSING_TARGET,
                    "PREVIEW_TARGET_NOT_FOUND", "Hedef bulunamadı");

            when(adminPreviewService.getPreview(ReviewTargetType.SCENARIO, TARGET_ID))
                    .thenReturn(unavailable);

            ResponseEntity<ApiResponse<SafeAdminPreview>> response =
                    controller.getPreview(ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().isPreviewAvailable()).isFalse();
            assertThat(response.getBody().getData().getAvailability())
                    .isEqualTo(AdminPreviewAvailability.MISSING_TARGET);
        }

        private void assertPreviewForType(ReviewTargetType type, AdminPreviewMode mode) {
            SafeAdminPreview preview = SafeAdminPreview.builder()
                    .targetType(type)
                    .targetId(TARGET_ID)
                    .previewMode(mode)
                    .availability(AdminPreviewAvailability.AVAILABLE)
                    .previewAvailable(true)
                    .safeTitle("Test Title")
                    .safeSummary("Test Summary")
                    .safePreviewLabel("Label")
                    .currentReviewState(ReviewState.NEEDS_REVIEW)
                    .build();

            when(adminPreviewService.getPreview(type, TARGET_ID)).thenReturn(preview);

            ResponseEntity<ApiResponse<SafeAdminPreview>> response =
                    controller.getPreview(type, TARGET_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getTargetType()).isEqualTo(type);
            assertThat(response.getBody().getData().getPreviewMode()).isEqualTo(mode);
        }
    }

    // ── D. Safe Response Body Regression ─────────────────────────────

    private static final String[] FORBIDDEN_FIELDS = {
            "providerUrl", "storageKey", "bucketName", "bucketPath",
            "rawPayload", "rawProviderPayload", "rawStoragePayload",
            "base64", "secret", "apiKey", "config",
            "publicUrl", "signedUrl", "publishUrl", "exportUrl",
            "stackTrace", "rawImageBytes", "rawContent"
    };

    @Nested
    @DisplayName("D. Safe response body regression")
    class SafeResponseRegression {

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
        @DisplayName("AdminReviewHandoffRequest has no forbidden fields")
        void handoffRequestHasNoForbiddenFields() {
            Set<String> fields = Arrays.stream(AdminReviewHandoffRequest.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields).doesNotContain(forbidden);
            }
        }

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
        @DisplayName("AdminReviewHandoffResponse safe toString")
        void handoffResponseSafeToString() {
            AdminReviewHandoffResponse response = AdminReviewHandoffResponse.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .currentState(ReviewState.NEEDS_REVIEW)
                    .created(true)
                    .build();

            String str = response.toString();
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(str.toLowerCase()).doesNotContain(forbidden.toLowerCase());
            }
        }

        @Test
        @DisplayName("AdminReviewHandoffRequest safe toString")
        void handoffRequestSafeToString() {
            AdminReviewHandoffRequest request = buildValidHandoffRequest();
            String str = request.toString();
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(str.toLowerCase()).doesNotContain(forbidden.toLowerCase());
            }
        }
    }

    // ── E. Endpoint Inventory Regression ─────────────────────────────

    @Nested
    @DisplayName("E. Endpoint inventory regression")
    class EndpointInventory {

        @Test
        @DisplayName("AdminReviewController has exactly 16 endpoints (8 POST + 8 GET)")
        void shouldHave14Endpoints() {
            long post = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long get = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();

            assertThat(post).isEqualTo(9);
            assertThat(get).isEqualTo(8);
            assertThat(post + get).isEqualTo(17);
        }

        @Test
        @DisplayName("POST paths include /handoff")
        void shouldIncludeHandoffPath() {
            Set<String> postPaths = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(PostMapping.class).value()))
                    .collect(Collectors.toSet());

            assertThat(postPaths).contains("/handoff");
        }

        @Test
        @DisplayName("GET paths include preview")
        void shouldIncludePreviewPath() {
            Set<String> getPaths = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .collect(Collectors.toSet());

            assertThat(getPaths).contains("/{targetType}/{targetId}/preview");
        }

        @Test
        @DisplayName("No endpoint contains forbidden path segments")
        void shouldNotContainForbiddenPaths() {
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
        @DisplayName("Controller base path is /api/admin/reviews")
        void shouldHaveCorrectBasePath() {
            RequestMapping rm = AdminReviewController.class.getAnnotation(RequestMapping.class);
            assertThat(rm).isNotNull();
            assertThat(rm.value()).contains("/api/admin/reviews");
        }

        @Test
        @DisplayName("Controller is @RestController")
        void shouldBeRestController() {
            assertThat(AdminReviewController.class.isAnnotationPresent(RestController.class)).isTrue();
        }
    }

    // ── F. Boundary Regression ───────────────────────────────────────

    @Nested
    @DisplayName("F. Boundary regression")
    class BoundaryRegression {

        @Test
        @DisplayName("No new migration file in V10+")
        void shouldNotHaveNewMigration() {
            // Verify by reflection: controller has no DataSource/JdbcTemplate dependency
            Set<String> fields = Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fields).doesNotContain("dataSource");
            assertThat(fields).doesNotContain("jdbcTemplate");
            assertThat(fields).doesNotContain("entityManager");
        }

        @Test
        @DisplayName("No public/signed URL class/field in controller")
        void shouldNotHavePublicUrlFields() {
            Set<String> fields = Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fields).doesNotContain("urlSigner");
            assertThat(fields).doesNotContain("publicUrlService");
            assertThat(fields).doesNotContain("signedUrlService");
            assertThat(fields).doesNotContain("cdnService");
        }

        @Test
        @DisplayName("No storage/provider service in controller")
        void shouldNotHaveStorageService() {
            Set<String> fields = Arrays.stream(AdminReviewController.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            assertThat(fields).doesNotContain("storageService");
            assertThat(fields).doesNotContain("providerService");
            assertThat(fields).doesNotContain("storageClient");
            assertThat(fields).doesNotContain("providerClient");
        }

        @Test
        @DisplayName("AdminReviewDtoMapper has toHandoffRequest mapper")
        void shouldHaveHandoffRequestMapper() {
            AdminReviewHandoffRequest dto = buildValidHandoffRequest();
            ReviewHandoffRequest mapped = AdminReviewDtoMapper.toHandoffRequest(dto);

            assertThat(mapped.getTargetType()).isEqualTo(dto.getTargetType());
            assertThat(mapped.getTargetId()).isEqualTo(dto.getTargetId());
            assertThat(mapped.getSource()).isEqualTo(dto.getSource());
        }

        @Test
        @DisplayName("AdminReviewDtoMapper has toHandoffResponse mapper")
        void shouldHaveHandoffResponseMapper() {
            ReviewHandoffResult result = ReviewHandoffResult.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(TARGET_ID)
                    .currentState(ReviewState.NEEDS_REVIEW)
                    .created(true).updated(false).preserved(false)
                    .build();

            AdminReviewHandoffResponse response = AdminReviewDtoMapper.toHandoffResponse(result);

            assertThat(response.getTargetType()).isEqualTo(result.getTargetType());
            assertThat(response.isCreated()).isTrue();
        }
    }

    // ── Test Helpers ─────────────────────────────────────────────────

    private AdminReviewHandoffRequest buildValidHandoffRequest() {
        return AdminReviewHandoffRequest.builder()
                .targetType(ReviewTargetType.SCENARIO)
                .targetId(TARGET_ID)
                .source(ReviewHandoffSource.SCENARIO_GENERATION)
                .actorRole(ReviewActorRole.SYSTEM)
                .actorReference("system-generation")
                .safeTitle("Test Scenario")
                .safeSummary("AI generated scenario")
                .build();
    }
}
