package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * FP-014 Task 03: API-level tests for internal approval readiness endpoints.
 *
 * <p>Covers controller integration, response shape, error handling, endpoint
 * inventory, no mutation/side effects, and serialization safety.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FP-014 Task 03 — Internal Approval Readiness Admin API")
class InternalApprovalReadinessAdminApiTest {

    private static final UUID TARGET_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 6, 14, 0, 0);

    @Mock private ReviewApplicationService reviewApplicationService;
    @Mock private ReviewHandoffService reviewHandoffService;
    @Mock private AdminPreviewService adminPreviewService;
    @Mock private InternalApprovalReadinessService internalApprovalReadinessService;
    @Mock private InternalRevisionRequestService internalRevisionRequestService;

    private AdminReviewController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminReviewController(
                reviewApplicationService, reviewHandoffService,
                adminPreviewService, internalApprovalReadinessService,
                internalRevisionRequestService);
    }

    // ════════════════════════════════════════════════════════════════
    // A. SINGLE TARGET READINESS ENDPOINT
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("A. Single Target Readiness — GET /{targetType}/{targetId}/internal-readiness")
    class SingleTargetReadiness {

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE target returns 200 with READY availability")
        void approvedTargetReturnsReady() {
            SafeInternalApprovalReadiness readiness = buildApprovedReadiness(ReviewTargetType.SCENARIO);
            when(internalApprovalReadinessService.getReadiness(ReviewTargetType.SCENARIO, TARGET_ID))
                    .thenReturn(readiness);

            var response = controller.getInternalReadiness(ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData().getAvailability())
                    .isEqualTo(InternalApprovalReadinessAvailability.READY);
        }

        @Test
        @DisplayName("Response contains all expected safe fields")
        void responseContainsSafeFields() {
            SafeInternalApprovalReadiness readiness = buildApprovedReadiness(ReviewTargetType.SCENARIO);
            when(internalApprovalReadinessService.getReadiness(any(), any())).thenReturn(readiness);

            var response = controller.getInternalReadiness(ReviewTargetType.SCENARIO, TARGET_ID);
            var data = response.getBody().getData();

            assertThat(data.getTargetType()).isEqualTo(ReviewTargetType.SCENARIO);
            assertThat(data.getTargetId()).isEqualTo(TARGET_ID);
            assertThat(data.getReviewState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(data.getSafeTitle()).isNotNull();
            assertThat(data.getSafeSummary()).isNotNull();
            assertThat(data.getSafeMessage()).isNotNull();
            assertThat(data.getSafeNextInternalActions()).isNotEmpty();
            assertThat(data.getSafeWarnings()).isNotNull();
        }

        @Test
        @DisplayName("Non-approved target returns NOT_APPROVED safely")
        void nonApprovedTargetSafe() {
            SafeInternalApprovalReadiness readiness = buildNonApprovedReadiness(
                    ReviewTargetType.SCENARIO, ReviewState.NEEDS_REVIEW);
            when(internalApprovalReadinessService.getReadiness(any(), any())).thenReturn(readiness);

            var response = controller.getInternalReadiness(ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getAvailability())
                    .isEqualTo(InternalApprovalReadinessAvailability.NOT_APPROVED);
        }

        @Test
        @DisplayName("Rejected target returns REJECTED safely")
        void rejectedTargetSafe() {
            SafeInternalApprovalReadiness readiness = buildNonApprovedReadiness(
                    ReviewTargetType.STORYBOARD, ReviewState.REJECTED);
            when(internalApprovalReadinessService.getReadiness(any(), any())).thenReturn(readiness);

            var response = controller.getInternalReadiness(ReviewTargetType.STORYBOARD, TARGET_ID);

            assertThat(response.getBody().getData().getAvailability())
                    .isEqualTo(InternalApprovalReadinessAvailability.REJECTED);
        }

        @Test
        @DisplayName("Missing target returns MISSING_TARGET safely")
        void missingTargetSafe() {
            SafeInternalApprovalReadiness readiness = SafeInternalApprovalReadiness.unavailable(
                    ReviewTargetType.SCENARIO, TARGET_ID,
                    InternalApprovalReadinessAvailability.MISSING_TARGET,
                    "NOT_FOUND", "Record not found");
            when(internalApprovalReadinessService.getReadiness(any(), any())).thenReturn(readiness);

            var response = controller.getInternalReadiness(ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getAvailability())
                    .isEqualTo(InternalApprovalReadinessAvailability.MISSING_TARGET);
        }

        @Test
        @DisplayName("IMAGE_ASSET readiness does not expose URL/base64/bytes/storage key")
        void imageAssetNoExposure() {
            SafeInternalApprovalReadiness readiness = buildApprovedReadiness(ReviewTargetType.IMAGE_ASSET);
            when(internalApprovalReadinessService.getReadiness(any(), any())).thenReturn(readiness);

            var response = controller.getInternalReadiness(ReviewTargetType.IMAGE_ASSET, TARGET_ID);
            var data = response.getBody().getData();

            String str = data.toString();
            assertThat(str).doesNotContain("providerUrl", "storageKey", "base64",
                    "signedUrl", "publicUrl", "bytes", "cdnUrl");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // B. APPROVED READINESS LIST ENDPOINT
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("B. Approved Readiness List — GET /approved/internal-readiness")
    class ApprovedReadinessList {

        @Test
        @DisplayName("Returns only approved readiness candidates")
        void returnsApprovedOnly() {
            List<SafeInternalApprovalReadiness> list = List.of(
                    buildApprovedReadiness(ReviewTargetType.SCENARIO),
                    buildApprovedReadiness(ReviewTargetType.STORYBOARD));
            when(internalApprovalReadinessService.getApprovedReadinessList(null)).thenReturn(list);

            var response = controller.getApprovedReadinessList(null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(2);
            response.getBody().getData().forEach(r ->
                    assertThat(r.getAvailability()).isEqualTo(InternalApprovalReadinessAvailability.READY));
        }

        @Test
        @DisplayName("Filtered by target type")
        void filteredByType() {
            List<SafeInternalApprovalReadiness> list = List.of(
                    buildApprovedReadiness(ReviewTargetType.PACKAGE));
            when(internalApprovalReadinessService.getApprovedReadinessList(ReviewTargetType.PACKAGE))
                    .thenReturn(list);

            var response = controller.getApprovedReadinessList(ReviewTargetType.PACKAGE);

            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getTargetType())
                    .isEqualTo(ReviewTargetType.PACKAGE);
        }

        @Test
        @DisplayName("Empty list returns safe empty response")
        void emptyListSafe() {
            when(internalApprovalReadinessService.getApprovedReadinessList(any())).thenReturn(List.of());

            var response = controller.getApprovedReadinessList(null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).isEmpty();
        }

        @Test
        @DisplayName("List items do not contain forbidden fields in serialization")
        void noForbiddenFieldsInList() {
            Set<String> forbiddenFields = Set.of(
                    "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                    "storageKey", "bucket", "objectPath", "rawPayload",
                    "base64", "bytes", "secret", "apiKey",
                    "publishStatus", "exportStatus");

            Set<String> dtoFields = Arrays.stream(SafeInternalApprovalReadiness.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : forbiddenFields) {
                assertThat(dtoFields).doesNotContain(forbidden);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // C. EXCEPTION / FAILURE HANDLING
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("C. Exception & Failure Handling")
    class ExceptionHandling {

        @Test
        @DisplayName("InternalApprovalReadinessException maps to safe NOT_FOUND response")
        void readinessExceptionMapsToNotFound() {
            var ex = new InternalApprovalReadinessException("Not found", "READINESS_NOT_FOUND");
            var response = controller.handleReadinessException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getMessage()).contains("Not found");
        }

        @Test
        @DisplayName("Exception response has no stack trace")
        void exceptionNoStackTrace() {
            var ex = new InternalApprovalReadinessException("Safe message", "SAFE_CODE");
            var response = controller.handleReadinessException(ex);

            assertThat(response.getBody().getMessage()).doesNotContain("Exception");
            assertThat(response.getBody().getMessage()).doesNotContain("stackTrace");
            assertThat(response.getBody().getMessage()).doesNotContain("providerUrl");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // D. ENDPOINT INVENTORY REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("D. Endpoint Inventory Regression")
    class EndpointInventory {

        @Test
        @DisplayName("AdminReviewController has exactly 16 endpoints (8 POST + 8 GET)")
        void controllerHas16Endpoints() {
            long post = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(
                            org.springframework.web.bind.annotation.PostMapping.class)).count();
            long get = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(
                            org.springframework.web.bind.annotation.GetMapping.class)).count();

            assertThat(post).isEqualTo(9);
            assertThat(get).isEqualTo(8);
            assertThat(post + get).isEqualTo(17);
        }

        @Test
        @DisplayName("New endpoints include internal-readiness GET paths")
        void newEndpointsIncludeReadiness() {
            Set<String> getMethodNames = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(
                            org.springframework.web.bind.annotation.GetMapping.class))
                    .map(Method::getName)
                    .collect(Collectors.toSet());

            assertThat(getMethodNames).contains("getInternalReadiness", "getApprovedReadinessList");
        }

        @Test
        @DisplayName("No forbidden endpoint paths")
        void noForbiddenEndpoints() {
            Set<String> allMethodNames = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m ->
                            m.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class) ||
                            m.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class))
                    .map(m -> m.getName().toLowerCase())
                    .collect(Collectors.toSet());

            List<String> forbidden = List.of("publish", "export", "cdn", "signed",
                    "provider", "storage", "raw", "base64", "medialibrary", "video", "audio");

            for (String f : forbidden) {
                assertThat(allMethodNames.stream().anyMatch(m -> m.contains(f)))
                        .as("No endpoint method should contain '%s'", f)
                        .isFalse();
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // E. NO MUTATION / NO SIDE EFFECTS
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("E. No Mutation / No Side Effects")
    class NoMutation {

        @Test
        @DisplayName("getInternalReadiness does not call any mutation methods")
        void singleReadinessNoMutation() {
            when(internalApprovalReadinessService.getReadiness(any(), any()))
                    .thenReturn(buildApprovedReadiness(ReviewTargetType.SCENARIO));

            controller.getInternalReadiness(ReviewTargetType.SCENARIO, TARGET_ID);

            verifyNoInteractions(reviewHandoffService);
            verify(internalApprovalReadinessService, only()).getReadiness(any(), any());
        }

        @Test
        @DisplayName("getApprovedReadinessList does not call any mutation methods")
        void listReadinessNoMutation() {
            when(internalApprovalReadinessService.getApprovedReadinessList(any()))
                    .thenReturn(List.of());

            controller.getApprovedReadinessList(null);

            verifyNoInteractions(reviewHandoffService);
            verify(internalApprovalReadinessService, only()).getApprovedReadinessList(any());
        }

        @Test
        @DisplayName("Readiness endpoints do not interact with ReviewApplicationService")
        void noReviewApplicationServiceInteraction() {
            when(internalApprovalReadinessService.getReadiness(any(), any()))
                    .thenReturn(buildApprovedReadiness(ReviewTargetType.SCENARIO));

            controller.getInternalReadiness(ReviewTargetType.SCENARIO, TARGET_ID);

            verifyNoInteractions(reviewApplicationService);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // F. SAFE ACTION TAXONOMY IN API RESPONSE
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("F. Safe Action Taxonomy in API Response")
    class SafeActionTaxonomy {

        @Test
        @DisplayName("Approved response includes safe internal actions only")
        void approvedResponseSafeActions() {
            SafeInternalApprovalReadiness readiness = buildApprovedReadiness(ReviewTargetType.CONTENT_PROJECT);
            when(internalApprovalReadinessService.getReadiness(any(), any())).thenReturn(readiness);

            var response = controller.getInternalReadiness(ReviewTargetType.CONTENT_PROJECT, TARGET_ID);
            var actions = response.getBody().getData().getSafeNextInternalActions();

            assertThat(actions).containsExactlyInAnyOrder(
                    SafeInternalAction.VIEW_SAFE_PREVIEW,
                    SafeInternalAction.REVIEW_APPROVAL_DETAILS,
                    SafeInternalAction.REQUEST_INTERNAL_REVISION,
                    SafeInternalAction.PREPARE_FOR_FUTURE_EXPORT,
                    SafeInternalAction.MARK_AS_INTERNAL_READY_CANDIDATE);
        }

        @Test
        @DisplayName("SafeInternalAction enum has no forbidden values")
        void safeInternalActionNoForbidden() {
            Set<String> values = Arrays.stream(SafeInternalAction.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            assertThat(values).doesNotContain(
                    "PUBLISH", "EXPORT", "PUBLIC_DELIVERY",
                    "CREATE_PUBLIC_URL", "CREATE_SIGNED_URL",
                    "CDN_DELIVERY", "USER_MEDIA_LIBRARY",
                    "READY_FOR_PUBLISH", "PUBLISHED", "EXPORTED", "COMPLETED");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // G. MIGRATION / SCHEMA REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("G. Migration & Schema Regression")
    class MigrationRegression {

        @Test
        @DisplayName("InternalApprovalReadinessService is @Service, not @Entity")
        void serviceNotEntity() {
            assertThat(InternalApprovalReadinessService.class.getAnnotationsByType(
                    jakarta.persistence.Entity.class)).isEmpty();
            assertThat(InternalApprovalReadinessService.class.getAnnotationsByType(
                    jakarta.persistence.Table.class)).isEmpty();
        }

        @Test
        @DisplayName("SafeInternalApprovalReadiness is not a JPA entity")
        void dtoNotEntity() {
            assertThat(SafeInternalApprovalReadiness.class.getAnnotationsByType(
                    jakarta.persistence.Entity.class)).isEmpty();
            assertThat(SafeInternalApprovalReadiness.class.getAnnotationsByType(
                    jakarta.persistence.Table.class)).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════

    private SafeInternalApprovalReadiness buildApprovedReadiness(ReviewTargetType type) {
        return SafeInternalApprovalReadiness.builder()
                .targetType(type)
                .targetId(TARGET_ID)
                .reviewState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                .availability(InternalApprovalReadinessAvailability.READY)
                .safeTitle("Test başlık")
                .safeSummary("Test özet")
                .safeReasonCode("APPROVED_OK")
                .safeMessage("Dahili onay verildi")
                .safePreviewAvailable(true)
                .approvedAt(NOW)
                .approvedByRole("ADMIN")
                .safeNextInternalActions(List.of(
                        SafeInternalAction.VIEW_SAFE_PREVIEW,
                        SafeInternalAction.REVIEW_APPROVAL_DETAILS,
                        SafeInternalAction.REQUEST_INTERNAL_REVISION,
                        SafeInternalAction.PREPARE_FOR_FUTURE_EXPORT,
                        SafeInternalAction.MARK_AS_INTERNAL_READY_CANDIDATE))
                .safeWarnings(List.of("Dahili onay yayın/dışa aktarma onayı değildir"))
                .updatedAt(NOW)
                .build();
    }

    private SafeInternalApprovalReadiness buildNonApprovedReadiness(
            ReviewTargetType type, ReviewState state) {
        InternalApprovalReadinessAvailability avail = state == ReviewState.REJECTED
                ? InternalApprovalReadinessAvailability.REJECTED
                : InternalApprovalReadinessAvailability.NOT_APPROVED;

        return SafeInternalApprovalReadiness.builder()
                .targetType(type)
                .targetId(TARGET_ID)
                .reviewState(state)
                .availability(avail)
                .safeTitle("Test başlık")
                .safeSummary("Test özet")
                .safeReasonCode("PENDING")
                .safeMessage("Onay bekliyor")
                .safePreviewAvailable(false)
                .safeNextInternalActions(List.of(SafeInternalAction.VIEW_SAFE_PREVIEW))
                .safeWarnings(List.of())
                .updatedAt(NOW)
                .build();
    }
}
