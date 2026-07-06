package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * FP-014 Task 02: Comprehensive tests for InternalApprovalReadinessService,
 * SafeInternalApprovalReadiness DTO, SafeInternalAction enum, and
 * InternalApprovalReadinessAvailability enum.
 *
 * <p>Covers: approved readiness, non-approved states, rejected state, missing target,
 * target type coverage, preview safety, DTO safety, enum safety, endpoint/migration
 * regression, and no write side effects.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FP-014 Task 02 — Internal Approval Readiness Service & DTO Foundation")
class InternalApprovalReadinessServiceTest {

    private static final UUID TARGET_ID = UUID.fromString("e1a2b3c4-d5e6-f7a8-b9c0-d1e2f3a4b5c6");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 6, 12, 0, 0);

    @Mock
    private ReviewRecordRepository reviewRecordRepository;

    @Mock
    private AdminPreviewService adminPreviewService;

    private InternalApprovalReadinessService service;

    @BeforeEach
    void setUp() {
        service = new InternalApprovalReadinessService(reviewRecordRepository, adminPreviewService);
    }

    // ════════════════════════════════════════════════════════════════
    // 1. APPROVED READINESS
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Approved Readiness — APPROVED_FOR_INTERNAL_USE")
    class ApprovedReadiness {

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE returns READY availability")
        void approvedReturnsReady() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.getAvailability()).isEqualTo(InternalApprovalReadinessAvailability.READY);
            assertThat(result.getReviewState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("Approved readiness includes all 5 internal-only actions")
        void approvedIncludesInternalActions() {
            mockRecord(ReviewTargetType.CONTENT_PROJECT, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.CONTENT_PROJECT, TARGET_ID);

            assertThat(result.getSafeNextInternalActions()).containsExactlyInAnyOrder(
                    SafeInternalAction.VIEW_SAFE_PREVIEW,
                    SafeInternalAction.REVIEW_APPROVAL_DETAILS,
                    SafeInternalAction.REQUEST_INTERNAL_REVISION,
                    SafeInternalAction.PREPARE_FOR_FUTURE_EXPORT,
                    SafeInternalAction.MARK_AS_INTERNAL_READY_CANDIDATE);
        }

        @Test
        @DisplayName("Approved readiness includes no publish/export/CDN actions")
        void approvedNoPublishExportActions() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            List<String> actionNames = result.getSafeNextInternalActions().stream()
                    .map(Enum::name)
                    .toList();

            // PREPARE_FOR_FUTURE_EXPORT is explicitly allowed as informational/non-executing
            List<String> forbidden = List.of("PUBLISH", "PUBLIC_DELIVERY",
                    "CREATE_SIGNED_URL", "CREATE_PUBLIC_URL", "CDN_DELIVERY",
                    "USER_MEDIA_LIBRARY", "EXPORTED", "PUBLISHED");

            for (String f : forbidden) {
                assertThat(actionNames).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("Approved readiness safe message states internal-only")
        void approvedMessageStatesInternalOnly() {
            mockRecord(ReviewTargetType.STORYBOARD, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.STORYBOARD, TARGET_ID);

            assertThat(result.getSafeMessage()).contains("dahili");
            assertThat(result.getSafeMessage()).doesNotContainIgnoringCase("publish");
            assertThat(result.getSafeMessage()).doesNotContainIgnoringCase("export");
        }

        @Test
        @DisplayName("Approved readiness populates safePreviewAvailable correctly")
        void approvedPopulatesPreviewAvailable() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.isSafePreviewAvailable()).isTrue();
        }

        @Test
        @DisplayName("Approved readiness populates approvedAt from last action")
        void approvedPopulatesApprovedAt() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.getApprovedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Approved readiness includes internal-only warning")
        void approvedIncludesInternalWarning() {
            mockRecord(ReviewTargetType.PACKAGE, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.PACKAGE, TARGET_ID);

            assertThat(result.getSafeWarnings()).anyMatch(w ->
                    w.contains("yayın") || w.contains("dışa aktarma"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. NON-APPROVED STATES
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. Non-Approved States — NOT_APPROVED availability")
    class NonApprovedStates {

        @Test
        @DisplayName("DRAFT returns NOT_APPROVED")
        void draftReturnsNotApproved() {
            assertNonApproved(ReviewState.DRAFT);
        }

        @Test
        @DisplayName("AI_GENERATED returns NOT_APPROVED")
        void aiGeneratedReturnsNotApproved() {
            assertNonApproved(ReviewState.AI_GENERATED);
        }

        @Test
        @DisplayName("NEEDS_REVIEW returns NOT_APPROVED")
        void needsReviewReturnsNotApproved() {
            assertNonApproved(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("IN_REVIEW returns NOT_APPROVED")
        void inReviewReturnsNotApproved() {
            assertNonApproved(ReviewState.IN_REVIEW);
        }

        @Test
        @DisplayName("CHANGES_REQUESTED returns NOT_APPROVED")
        void changesRequestedReturnsNotApproved() {
            assertNonApproved(ReviewState.CHANGES_REQUESTED);
        }

        @Test
        @DisplayName("Non-approved states have no ready candidate or export actions")
        void nonApprovedNoReadyCandidateActions() {
            for (ReviewState state : List.of(ReviewState.DRAFT, ReviewState.AI_GENERATED,
                    ReviewState.NEEDS_REVIEW, ReviewState.IN_REVIEW, ReviewState.CHANGES_REQUESTED)) {

                mockRecord(ReviewTargetType.SCENARIO, state);
                mockPreviewAvailable();

                SafeInternalApprovalReadiness result = service.getReadiness(
                        ReviewTargetType.SCENARIO, TARGET_ID);

                assertThat(result.getSafeNextInternalActions())
                        .as("State %s should not have MARK_AS_INTERNAL_READY_CANDIDATE", state)
                        .doesNotContain(SafeInternalAction.MARK_AS_INTERNAL_READY_CANDIDATE);

                assertThat(result.getSafeNextInternalActions())
                        .as("State %s should not have PREPARE_FOR_FUTURE_EXPORT", state)
                        .doesNotContain(SafeInternalAction.PREPARE_FOR_FUTURE_EXPORT);

                assertThat(result.getSafeNextInternalActions())
                        .as("State %s should not have REQUEST_INTERNAL_REVISION", state)
                        .doesNotContain(SafeInternalAction.REQUEST_INTERNAL_REVISION);
            }
        }

        @Test
        @DisplayName("Non-approved states safe message mentions manual approval required")
        void nonApprovedMessageMentionsManualApproval() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.NEEDS_REVIEW);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.getSafeMessage()).containsIgnoringCase("onay");
        }

        @Test
        @DisplayName("Non-approved states do not mutate review state")
        void nonApprovedDoNotMutateState() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.IN_REVIEW);
            mockPreviewAvailable();

            service.getReadiness(ReviewTargetType.SCENARIO, TARGET_ID);

            // Verify no save/persist operations
            verify(reviewRecordRepository, never()).save(any());
            verify(reviewRecordRepository, never()).saveAndFlush(any());
        }

        private void assertNonApproved(ReviewState state) {
            mockRecord(ReviewTargetType.SCENARIO, state);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.getAvailability())
                    .isEqualTo(InternalApprovalReadinessAvailability.NOT_APPROVED);
            assertThat(result.getApprovedAt()).isNull();
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 3. REJECTED STATE
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Rejected State — REJECTED availability")
    class RejectedState {

        @Test
        @DisplayName("REJECTED returns REJECTED availability")
        void rejectedReturnsRejected() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.REJECTED);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.getAvailability()).isEqualTo(InternalApprovalReadinessAvailability.REJECTED);
        }

        @Test
        @DisplayName("Rejected has no export/ready candidate actions")
        void rejectedNoExportActions() {
            mockRecord(ReviewTargetType.STORYBOARD, ReviewState.REJECTED);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.STORYBOARD, TARGET_ID);

            assertThat(result.getSafeNextInternalActions())
                    .doesNotContain(SafeInternalAction.PREPARE_FOR_FUTURE_EXPORT);
            assertThat(result.getSafeNextInternalActions())
                    .doesNotContain(SafeInternalAction.MARK_AS_INTERNAL_READY_CANDIDATE);
            assertThat(result.getSafeNextInternalActions())
                    .doesNotContain(SafeInternalAction.REQUEST_INTERNAL_REVISION);
        }

        @Test
        @DisplayName("Rejected safe message is clear")
        void rejectedMessageClear() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.REJECTED);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.getSafeMessage()).containsIgnoringCase("reddedil");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 4. MISSING / UNAVAILABLE TARGET
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. Missing / Unavailable Target")
    class MissingTarget {

        @Test
        @DisplayName("Missing review record returns MISSING_TARGET")
        void missingRecordReturnsMissingTarget() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(any(), any()))
                    .thenReturn(Optional.empty());

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.getAvailability()).isEqualTo(InternalApprovalReadinessAvailability.MISSING_TARGET);
            assertThat(result.getSafeReasonCode()).isNotNull();
            assertThat(result.getSafeMessage()).isNotNull();
        }

        @Test
        @DisplayName("Null targetType returns UNAVAILABLE")
        void nullTargetTypeReturnsUnavailable() {
            SafeInternalApprovalReadiness result = service.getReadiness(null, TARGET_ID);

            assertThat(result.getAvailability()).isEqualTo(InternalApprovalReadinessAvailability.UNAVAILABLE);
        }

        @Test
        @DisplayName("Null targetId returns MISSING_TARGET")
        void nullTargetIdReturnsMissingTarget() {
            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, null);

            assertThat(result.getAvailability()).isEqualTo(InternalApprovalReadinessAvailability.MISSING_TARGET);
        }

        @Test
        @DisplayName("Missing target does not leak stack trace")
        void missingTargetNoStackTrace() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(any(), any()))
                    .thenReturn(Optional.empty());

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.toString()).doesNotContain("Exception");
            assertThat(result.getSafeMessage()).doesNotContain("Exception");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 5. TARGET TYPE COVERAGE
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. Target Type Coverage — All 5 Types")
    class TargetTypeCoverage {

        @Test
        @DisplayName("CONTENT_PROJECT approved readiness is correct")
        void contentProjectReadiness() {
            assertTargetTypeApproved(ReviewTargetType.CONTENT_PROJECT, "Proje");
        }

        @Test
        @DisplayName("SCENARIO approved readiness is correct")
        void scenarioReadiness() {
            assertTargetTypeApproved(ReviewTargetType.SCENARIO, "Senaryo");
        }

        @Test
        @DisplayName("STORYBOARD approved readiness is correct")
        void storyboardReadiness() {
            assertTargetTypeApproved(ReviewTargetType.STORYBOARD, "Storyboard");
        }

        @Test
        @DisplayName("PACKAGE approved readiness is correct")
        void packageReadiness() {
            assertTargetTypeApproved(ReviewTargetType.PACKAGE, "Paket");
        }

        @Test
        @DisplayName("IMAGE_ASSET approved readiness is correct with metadata-only warning")
        void imageAssetReadiness() {
            mockRecord(ReviewTargetType.IMAGE_ASSET, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.IMAGE_ASSET, TARGET_ID);

            assertThat(result.getAvailability()).isEqualTo(InternalApprovalReadinessAvailability.READY);
            assertThat(result.getTargetType()).isEqualTo(ReviewTargetType.IMAGE_ASSET);
            assertThat(result.getSafeWarnings()).anyMatch(w -> w.contains("meta veri"));
        }

        private void assertTargetTypeApproved(ReviewTargetType type, String titlePrefix) {
            mockRecord(type, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(type, TARGET_ID);

            assertThat(result.getAvailability()).isEqualTo(InternalApprovalReadinessAvailability.READY);
            assertThat(result.getTargetType()).isEqualTo(type);
            assertThat(result.getSafeTitle()).startsWith(titlePrefix);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 6. PREVIEW SAFETY INTEGRATION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. Preview Safety Integration")
    class PreviewSafety {

        @Test
        @DisplayName("Preview unavailable does not fail readiness")
        void previewUnavailableDoesNotFail() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.APPROVED_FOR_INTERNAL_USE);
            when(adminPreviewService.getPreview(any(), any()))
                    .thenReturn(SafeAdminPreview.unavailable(ReviewTargetType.SCENARIO, TARGET_ID,
                            AdminPreviewAvailability.MISSING_TARGET, "MISSING", "missing"));

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.getAvailability()).isEqualTo(InternalApprovalReadinessAvailability.READY);
            assertThat(result.isSafePreviewAvailable()).isFalse();
        }

        @Test
        @DisplayName("Preview exception does not fail readiness")
        void previewExceptionDoesNotFail() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.APPROVED_FOR_INTERNAL_USE);
            when(adminPreviewService.getPreview(any(), any()))
                    .thenThrow(new RuntimeException("Preview failure"));

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.SCENARIO, TARGET_ID);

            assertThat(result.getAvailability()).isEqualTo(InternalApprovalReadinessAvailability.READY);
            assertThat(result.isSafePreviewAvailable()).isFalse();
        }

        @Test
        @DisplayName("IMAGE_ASSET readiness does not expose URL/base64/bytes/storage key")
        void imageAssetNoUrlBytes() {
            mockRecord(ReviewTargetType.IMAGE_ASSET, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            SafeInternalApprovalReadiness result = service.getReadiness(
                    ReviewTargetType.IMAGE_ASSET, TARGET_ID);

            String str = result.toString();
            assertThat(str).doesNotContain("providerUrl", "storageKey", "base64",
                    "signedUrl", "publicUrl", "bytes");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 7. SAFE DTO SERIALIZATION / TOSTRING
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("7. Safe DTO Serialization & toString")
    class SafeDtoSerialization {

        private static final Set<String> FORBIDDEN_FIELDS = Set.of(
                "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                "storageKey", "bucket", "objectPath", "rawPayload",
                "base64", "bytes", "secret", "apiKey", "accessKey",
                "token", "stackTrace", "exceptionClass",
                "publishStatus", "exportStatus", "publishedAt", "exportedAt",
                "imageBytes", "imageUrl", "imageData", "rawContent");

        @Test
        @DisplayName("SafeInternalApprovalReadiness has no forbidden fields")
        void readinessDtoNoForbiddenFields() {
            Set<String> fields = Arrays.stream(SafeInternalApprovalReadiness.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields)
                        .as("DTO must not have field '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("SafeInternalApprovalReadiness toString is safe")
        void readinessToStringSafe() {
            SafeInternalApprovalReadiness readiness = SafeInternalApprovalReadiness.unavailable(
                    ReviewTargetType.SCENARIO, TARGET_ID,
                    InternalApprovalReadinessAvailability.MISSING_TARGET,
                    "TEST", "Test mesaj");

            String str = readiness.toString();
            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(str.toLowerCase())
                        .as("toString must not contain '%s'", forbidden)
                        .doesNotContain(forbidden.toLowerCase());
            }
        }

        @Test
        @DisplayName("InternalApprovalReadinessException toString is safe")
        void exceptionToStringSafe() {
            InternalApprovalReadinessException ex =
                    new InternalApprovalReadinessException("Test error", "TEST_ERROR");

            String str = ex.toString();
            assertThat(str).doesNotContain("providerUrl", "storageKey", "base64",
                    "signedUrl", "secret", "apiKey");
            assertThat(str).contains("TEST_ERROR");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 8. ENUM SAFETY
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("8. Enum Safety — SafeInternalAction & Availability")
    class EnumSafety {

        @Test
        @DisplayName("SafeInternalAction has exactly 5 values")
        void safeInternalActionHasFiveValues() {
            assertThat(SafeInternalAction.values()).hasSize(5);
        }

        @Test
        @DisplayName("SafeInternalAction does not contain forbidden values")
        void safeInternalActionNoForbiddenValues() {
            Set<String> values = Arrays.stream(SafeInternalAction.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            assertThat(values).doesNotContain(
                    "PUBLISH", "EXPORT", "PUBLIC_DELIVERY",
                    "CREATE_PUBLIC_URL", "CREATE_SIGNED_URL",
                    "CDN_DELIVERY", "USER_MEDIA_LIBRARY",
                    "READY_FOR_PUBLISH", "PUBLISHED", "EXPORTED", "COMPLETED");
        }

        @Test
        @DisplayName("InternalApprovalReadinessAvailability has exactly 7 values")
        void availabilityHasSevenValues() {
            assertThat(InternalApprovalReadinessAvailability.values()).hasSize(7);
        }

        @Test
        @DisplayName("InternalApprovalReadinessAvailability does not contain forbidden values")
        void availabilityNoForbiddenValues() {
            Set<String> values = Arrays.stream(InternalApprovalReadinessAvailability.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            assertThat(values).doesNotContain(
                    "PUBLISHABLE", "EXPORTABLE", "PUBLICLY_AVAILABLE",
                    "CDN_READY", "SIGNED_URL_READY");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 9. ENDPOINT / MIGRATION REGRESSION
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("9. Endpoint & Migration Regression")
    class EndpointMigrationRegression {

        @Test
        @DisplayName("AdminReviewController has exactly 16 endpoints")
        void adminReviewControllerHas16Endpoints() {
            long endpointCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m ->
                            m.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class) ||
                            m.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class) ||
                            m.isAnnotationPresent(org.springframework.web.bind.annotation.PutMapping.class) ||
                            m.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class))
                    .count();

            assertThat(endpointCount)
                    .as("Admin review endpoints must be 16 (FP-014 baseline)")
                    .isEqualTo(16);
        }

        @Test
        @DisplayName("InternalApprovalReadinessService is NOT a controller")
        void readinessServiceIsNotController() {
            assertThat(InternalApprovalReadinessService.class.getAnnotationsByType(
                    org.springframework.web.bind.annotation.RestController.class)).isEmpty();
            assertThat(InternalApprovalReadinessService.class.getAnnotationsByType(
                    org.springframework.stereotype.Controller.class)).isEmpty();
            assertThat(InternalApprovalReadinessService.class.getAnnotationsByType(
                    org.springframework.web.bind.annotation.RequestMapping.class)).isEmpty();
        }

        @Test
        @DisplayName("No forbidden endpoint methods in AdminReviewController")
        void noForbiddenEndpoints() {
            Set<String> methodNames = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .map(Method::getName)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            List<String> forbidden = List.of("publish", "export", "public", "signed",
                    "cdn", "provider", "storage", "raw", "base64", "media-library",
                    "video", "audio");

            for (String f : forbidden) {
                assertThat(methodNames.stream().anyMatch(m -> m.contains(f)))
                        .as("No endpoint method should contain '%s'", f)
                        .isFalse();
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 10. NO WRITE SIDE EFFECTS
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("10. No Write Side Effects")
    class NoWriteSideEffects {

        @Test
        @DisplayName("getReadiness does not save/persist any entity")
        void getReadinessDoesNotSave() {
            mockRecord(ReviewTargetType.SCENARIO, ReviewState.APPROVED_FOR_INTERNAL_USE);
            mockPreviewAvailable();

            service.getReadiness(ReviewTargetType.SCENARIO, TARGET_ID);

            verify(reviewRecordRepository, never()).save(any());
            verify(reviewRecordRepository, never()).saveAndFlush(any());
            verify(reviewRecordRepository, never()).saveAll(any());
            verify(reviewRecordRepository, never()).delete(any());
            verify(reviewRecordRepository, never()).deleteAll(any());
        }

        @Test
        @DisplayName("getApprovedReadinessList does not save/persist any entity")
        void getApprovedListDoesNotSave() {
            when(reviewRecordRepository.findByCurrentStateAndReviewTargetType(
                    eq(ReviewState.APPROVED_FOR_INTERNAL_USE), any()))
                    .thenReturn(List.of());

            service.getApprovedReadinessList(ReviewTargetType.SCENARIO);

            verify(reviewRecordRepository, never()).save(any());
            verify(reviewRecordRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Service is annotated with @Transactional(readOnly = true)")
        void serviceMethodsAreReadOnly() {
            var methods = InternalApprovalReadinessService.class.getDeclaredMethods();
            for (var method : methods) {
                var txn = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
                if (txn != null) {
                    assertThat(txn.readOnly())
                            .as("Method %s must be readOnly", method.getName())
                            .isTrue();
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 11. APPROVED LIST
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("11. Approved Readiness List")
    class ApprovedList {

        @Test
        @DisplayName("getApprovedReadinessList with targetType filters correctly")
        void approvedListFiltersByType() {
            ReviewRecordEntity record = buildRecord(ReviewTargetType.SCENARIO,
                    ReviewState.APPROVED_FOR_INTERNAL_USE);

            when(reviewRecordRepository.findByCurrentStateAndReviewTargetType(
                    ReviewState.APPROVED_FOR_INTERNAL_USE, ReviewTargetType.SCENARIO))
                    .thenReturn(List.of(record));
            mockPreviewAvailable();

            List<SafeInternalApprovalReadiness> results =
                    service.getApprovedReadinessList(ReviewTargetType.SCENARIO);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAvailability())
                    .isEqualTo(InternalApprovalReadinessAvailability.READY);
        }

        @Test
        @DisplayName("getApprovedReadinessList with null type returns all approved")
        void approvedListNullTypeReturnsAll() {
            when(reviewRecordRepository.findByCurrentState(ReviewState.APPROVED_FOR_INTERNAL_USE))
                    .thenReturn(List.of());

            List<SafeInternalApprovalReadiness> results =
                    service.getApprovedReadinessList(null);

            assertThat(results).isEmpty();
            verify(reviewRecordRepository).findByCurrentState(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════

    private void mockRecord(ReviewTargetType type, ReviewState state) {
        ReviewRecordEntity record = buildRecord(type, state);
        when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(eq(type), eq(TARGET_ID)))
                .thenReturn(Optional.of(record));
    }

    private ReviewRecordEntity buildRecord(ReviewTargetType type, ReviewState state) {
        ReviewRecordEntity record = ReviewRecordEntity.builder()
                .reviewTargetType(type)
                .reviewTargetId(TARGET_ID)
                .currentState(state)
                .lastActionAt(NOW)
                .lastActionByActorReference("admin:test-reviewer")
                .safeReasonCode("TEST_REASON")
                .safeMessage("Test mesaj")
                .build();
        record.setId(UUID.randomUUID());
        record.setUpdatedAt(NOW);
        return record;
    }

    private void mockPreviewAvailable() {
        SafeAdminPreview preview = SafeAdminPreview.builder()
                .targetType(ReviewTargetType.SCENARIO)
                .targetId(TARGET_ID)
                .previewMode(AdminPreviewMode.SCENARIO_SUMMARY)
                .availability(AdminPreviewAvailability.AVAILABLE)
                .previewAvailable(true)
                .safeTitle("Test")
                .safeSummary("Test summary")
                .safePreviewLabel("Test label")
                .safeThumbnailAvailable(false)
                .currentReviewState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                .build();
        lenient().when(adminPreviewService.getPreview(any(), any())).thenReturn(preview);
    }
}
