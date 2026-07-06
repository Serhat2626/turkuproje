package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-016 Task 02: PipelineAggregationService comprehensive tests.
 *
 * <p>Covers: stage mapping, target type coverage, overview counts,
 * missing/unavailable safe fallback, preview/readiness integration,
 * no-mutation verification, forbidden field regression, DTO safety.</p>
 */
class PipelineAggregationServiceTest {

    private ReviewRecordRepository reviewRecordRepository;
    private ReviewAuditEventRepository auditEventRepository;
    private InternalApprovalReadinessService readinessService;
    private AdminPreviewService adminPreviewService;
    private PipelineAggregationService service;

    @BeforeEach
    void setUp() {
        reviewRecordRepository = mock(ReviewRecordRepository.class);
        auditEventRepository = mock(ReviewAuditEventRepository.class);
        readinessService = mock(InternalApprovalReadinessService.class);
        adminPreviewService = mock(AdminPreviewService.class);

        service = new PipelineAggregationService(
                reviewRecordRepository, auditEventRepository,
                readinessService, adminPreviewService);
    }

    // ── A. SafePipelineStage Enum ──────────────────────────────────

    @Nested
    @DisplayName("A. SafePipelineStage Enum")
    class SafePipelineStageTests {

        @Test
        @DisplayName("tam olarak 9 değer içerir")
        void hasExactly9Values() {
            assertThat(SafePipelineStage.values()).hasSize(9);
        }

        @Test
        @DisplayName("tüm dahili aşamalar mevcut")
        void hasAllInternalStages() {
            Set<String> stageNames = Arrays.stream(SafePipelineStage.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            assertThat(stageNames).containsExactlyInAnyOrder(
                    "GENERATED", "REVIEW_PENDING", "IN_REVIEW",
                    "CHANGES_REQUESTED", "INTERNALLY_APPROVED", "INTERNAL_READY",
                    "INTERNAL_REVISION_REQUESTED", "REJECTED", "UNAVAILABLE");
        }

        @Test
        @DisplayName("yasaklı publish/export aşamaları yok")
        void noForbiddenStages() {
            Set<String> stageNames = Arrays.stream(SafePipelineStage.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            List<String> forbidden = List.of(
                    "READY_FOR_PUBLISH", "PUBLISHABLE", "PUBLISHED",
                    "EXPORTABLE", "EXPORTED", "CDN_READY",
                    "PUBLIC_DELIVERY", "SIGNED_URL_READY");
            forbidden.forEach(f -> assertThat(stageNames).doesNotContain(f));
        }
    }

    // ── B. Stage Derivation ────────────────────────────────────────

    @Nested
    @DisplayName("B. Stage Derivation")
    class StageDerivationTests {

        @Test
        @DisplayName("null reviewState → GENERATED")
        void nullReviewState_isGenerated() {
            assertThat(service.derivePipelineStage(null, null, false))
                    .isEqualTo(SafePipelineStage.GENERATED);
        }

        @Test
        @DisplayName("DRAFT → GENERATED")
        void draft_isGenerated() {
            assertThat(service.derivePipelineStage(ReviewState.DRAFT, null, false))
                    .isEqualTo(SafePipelineStage.GENERATED);
        }

        @Test
        @DisplayName("AI_GENERATED → GENERATED")
        void aiGenerated_isGenerated() {
            assertThat(service.derivePipelineStage(ReviewState.AI_GENERATED, null, false))
                    .isEqualTo(SafePipelineStage.GENERATED);
        }

        @Test
        @DisplayName("NEEDS_REVIEW → REVIEW_PENDING")
        void needsReview_isReviewPending() {
            assertThat(service.derivePipelineStage(ReviewState.NEEDS_REVIEW, null, false))
                    .isEqualTo(SafePipelineStage.REVIEW_PENDING);
        }

        @Test
        @DisplayName("IN_REVIEW → IN_REVIEW")
        void inReview_isInReview() {
            assertThat(service.derivePipelineStage(ReviewState.IN_REVIEW, null, false))
                    .isEqualTo(SafePipelineStage.IN_REVIEW);
        }

        @Test
        @DisplayName("REJECTED → REJECTED")
        void rejected_isRejected() {
            assertThat(service.derivePipelineStage(ReviewState.REJECTED, null, false))
                    .isEqualTo(SafePipelineStage.REJECTED);
        }

        @Test
        @DisplayName("APPROVED + READY → INTERNAL_READY")
        void approvedAndReady_isInternalReady() {
            assertThat(service.derivePipelineStage(
                    ReviewState.APPROVED_FOR_INTERNAL_USE,
                    InternalApprovalReadinessAvailability.READY,
                    false))
                    .isEqualTo(SafePipelineStage.INTERNAL_READY);
        }

        @Test
        @DisplayName("APPROVED + NOT_APPROVED → INTERNALLY_APPROVED")
        void approvedNotReady_isInternallyApproved() {
            assertThat(service.derivePipelineStage(
                    ReviewState.APPROVED_FOR_INTERNAL_USE,
                    InternalApprovalReadinessAvailability.NOT_APPROVED,
                    false))
                    .isEqualTo(SafePipelineStage.INTERNALLY_APPROVED);
        }

        @Test
        @DisplayName("APPROVED + null readiness → INTERNALLY_APPROVED")
        void approvedNullReadiness_isInternallyApproved() {
            assertThat(service.derivePipelineStage(
                    ReviewState.APPROVED_FOR_INTERNAL_USE,
                    null,
                    false))
                    .isEqualTo(SafePipelineStage.INTERNALLY_APPROVED);
        }

        @Test
        @DisplayName("CHANGES_REQUESTED + revisionRequested → INTERNAL_REVISION_REQUESTED")
        void changesRequestedWithRevision_isRevisionRequested() {
            assertThat(service.derivePipelineStage(
                    ReviewState.CHANGES_REQUESTED, null, true))
                    .isEqualTo(SafePipelineStage.INTERNAL_REVISION_REQUESTED);
        }

        @Test
        @DisplayName("CHANGES_REQUESTED + no revision → CHANGES_REQUESTED")
        void changesRequestedNoRevision_isChangesRequested() {
            assertThat(service.derivePipelineStage(
                    ReviewState.CHANGES_REQUESTED, null, false))
                    .isEqualTo(SafePipelineStage.CHANGES_REQUESTED);
        }

        @Test
        @DisplayName("tüm ReviewState değerleri safe stage elde eder")
        void allReviewStates_produceValidStage() {
            for (ReviewState state : ReviewState.values()) {
                SafePipelineStage stage = service.derivePipelineStage(state, null, false);
                assertThat(stage).isNotNull();
            }
        }
    }

    // ── C. Revision Detection ──────────────────────────────────────

    @Nested
    @DisplayName("C. Revision Detection")
    class RevisionDetectionTests {

        @Test
        @DisplayName("CHANGES_REQUESTED + lastAction=REQUEST_INTERNAL_REVISION → true")
        void lastActionRevision_detected() {
            ReviewRecordEntity record = buildRecord(ReviewState.CHANGES_REQUESTED);
            record.setLastActionType(ReviewActionType.REQUEST_INTERNAL_REVISION);
            assertThat(service.detectRevisionRequested(record)).isTrue();
        }

        @Test
        @DisplayName("CHANGES_REQUESTED + lastAction=REQUEST_CHANGES → false (no audit)")
        void lastActionRequestChanges_notDetected() {
            ReviewRecordEntity record = buildRecord(ReviewState.CHANGES_REQUESTED);
            record.setLastActionType(ReviewActionType.REQUEST_CHANGES);
            when(auditEventRepository.findByReviewRecordIdOrderByOccurredAtAsc(any()))
                    .thenReturn(List.of());
            assertThat(service.detectRevisionRequested(record)).isFalse();
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE → false (wrong state)")
        void approvedState_notDetected() {
            ReviewRecordEntity record = buildRecord(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(service.detectRevisionRequested(record)).isFalse();
        }

        @Test
        @DisplayName("DRAFT → false")
        void draftState_notDetected() {
            ReviewRecordEntity record = buildRecord(ReviewState.DRAFT);
            assertThat(service.detectRevisionRequested(record)).isFalse();
        }

        @Test
        @DisplayName("audit trail ile revision tespit edilir")
        void auditTrail_detectsRevision() {
            ReviewRecordEntity record = buildRecord(ReviewState.CHANGES_REQUESTED);
            record.setLastActionType(ReviewActionType.ADD_REVIEW_NOTE);

            ReviewAuditEventEntity approveEvent = new ReviewAuditEventEntity();
            approveEvent.setActionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE);
            approveEvent.setOccurredAt(LocalDateTime.now().minusHours(2));

            ReviewAuditEventEntity revisionEvent = new ReviewAuditEventEntity();
            revisionEvent.setActionType(ReviewActionType.REQUEST_INTERNAL_REVISION);
            revisionEvent.setOccurredAt(LocalDateTime.now().minusHours(1));

            when(auditEventRepository.findByReviewRecordIdOrderByOccurredAtAsc(record.getId()))
                    .thenReturn(List.of(approveEvent, revisionEvent));

            assertThat(service.detectRevisionRequested(record)).isTrue();
        }

        @Test
        @DisplayName("audit trail exception → safe false")
        void auditTrailException_safeDefault() {
            ReviewRecordEntity record = buildRecord(ReviewState.CHANGES_REQUESTED);
            record.setLastActionType(ReviewActionType.ADD_REVIEW_NOTE);

            when(auditEventRepository.findByReviewRecordIdOrderByOccurredAtAsc(any()))
                    .thenThrow(new RuntimeException("DB error"));

            assertThat(service.detectRevisionRequested(record)).isFalse();
        }
    }

    // ── D. Single Item Detail ──────────────────────────────────────

    @Nested
    @DisplayName("D. Single Item Detail")
    class SingleItemDetailTests {

        @Test
        @DisplayName("null targetType → UNAVAILABLE")
        void nullTargetType_unavailable() {
            var item = service.getItemDetail(null, UUID.randomUUID());
            assertThat(item.getPipelineStage()).isEqualTo(SafePipelineStage.UNAVAILABLE);
        }

        @Test
        @DisplayName("null targetId → UNAVAILABLE")
        void nullTargetId_unavailable() {
            var item = service.getItemDetail(ReviewTargetType.SCENARIO, null);
            assertThat(item.getPipelineStage()).isEqualTo(SafePipelineStage.UNAVAILABLE);
        }

        @Test
        @DisplayName("no review record → UNAVAILABLE")
        void noRecord_unavailable() {
            UUID id = UUID.randomUUID();
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(
                    ReviewTargetType.SCENARIO, id))
                    .thenReturn(Optional.empty());

            var item = service.getItemDetail(ReviewTargetType.SCENARIO, id);
            assertThat(item.getPipelineStage()).isEqualTo(SafePipelineStage.UNAVAILABLE);
            assertThat(item.getSafeBlockingReasons()).contains("NO_REVIEW_RECORD");
        }

        @Test
        @DisplayName("existing record → correct aggregation")
        void existingRecord_aggregated() {
            UUID id = UUID.randomUUID();
            ReviewRecordEntity record = buildRecord(ReviewState.NEEDS_REVIEW);
            record.setReviewTargetType(ReviewTargetType.STORYBOARD);
            record.setReviewTargetId(id);

            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(
                    ReviewTargetType.STORYBOARD, id))
                    .thenReturn(Optional.of(record));
            when(adminPreviewService.getPreview(any(), any())).thenReturn(null);
            when(readinessService.getReadiness(any(), any())).thenReturn(null);

            var item = service.getItemDetail(ReviewTargetType.STORYBOARD, id);
            assertThat(item.getPipelineStage()).isEqualTo(SafePipelineStage.REVIEW_PENDING);
            assertThat(item.getTargetType()).isEqualTo(ReviewTargetType.STORYBOARD);
            assertThat(item.getTargetId()).isEqualTo(id);
        }
    }

    // ── E. Overview ────────────────────────────────────────────────

    @Nested
    @DisplayName("E. Overview")
    class OverviewTests {

        @Test
        @DisplayName("boş kayıt → boş overview")
        void emptyRecords_emptyOverview() {
            when(reviewRecordRepository.findAll()).thenReturn(List.of());
            var overview = service.getOverview(null, null, 50);
            assertThat(overview.getTotalCount()).isZero();
            assertThat(overview.getItems()).isEmpty();
        }

        @Test
        @DisplayName("overview counts doğru hesaplanır")
        void overviewCounts_correct() {
            ReviewRecordEntity r1 = buildRecord(ReviewState.NEEDS_REVIEW);
            r1.setReviewTargetType(ReviewTargetType.SCENARIO);
            r1.setReviewTargetId(UUID.randomUUID());

            ReviewRecordEntity r2 = buildRecord(ReviewState.APPROVED_FOR_INTERNAL_USE);
            r2.setReviewTargetType(ReviewTargetType.STORYBOARD);
            r2.setReviewTargetId(UUID.randomUUID());

            when(reviewRecordRepository.findAll()).thenReturn(List.of(r1, r2));
            when(adminPreviewService.getPreview(any(), any())).thenReturn(null);
            when(readinessService.getReadiness(any(), any())).thenReturn(null);

            var overview = service.getOverview(null, null, 50);
            assertThat(overview.getTotalCount()).isEqualTo(2);
            assertThat(overview.getCountsByStage()).containsKey(SafePipelineStage.REVIEW_PENDING);
            assertThat(overview.getCountsByTargetType()).containsKey(ReviewTargetType.SCENARIO);
        }

        @Test
        @DisplayName("targetType filtre çalışır")
        void targetTypeFilter_works() {
            when(reviewRecordRepository.findByReviewTargetType(ReviewTargetType.PACKAGE))
                    .thenReturn(List.of());

            var overview = service.getOverview(ReviewTargetType.PACKAGE, null, 50);
            assertThat(overview.getTotalCount()).isZero();
            verify(reviewRecordRepository).findByReviewTargetType(ReviewTargetType.PACKAGE);
            verify(reviewRecordRepository, never()).findAll();
        }

        @Test
        @DisplayName("stage filtre çalışır")
        void stageFilter_works() {
            ReviewRecordEntity r1 = buildRecord(ReviewState.NEEDS_REVIEW);
            r1.setReviewTargetType(ReviewTargetType.SCENARIO);
            r1.setReviewTargetId(UUID.randomUUID());

            ReviewRecordEntity r2 = buildRecord(ReviewState.REJECTED);
            r2.setReviewTargetType(ReviewTargetType.STORYBOARD);
            r2.setReviewTargetId(UUID.randomUUID());

            when(reviewRecordRepository.findAll()).thenReturn(List.of(r1, r2));
            when(adminPreviewService.getPreview(any(), any())).thenReturn(null);
            when(readinessService.getReadiness(any(), any())).thenReturn(null);

            var overview = service.getOverview(null, SafePipelineStage.REJECTED, 50);
            // Only REJECTED items pass the filter
            assertThat(overview.getItems()).allMatch(
                    i -> i.getPipelineStage() == SafePipelineStage.REJECTED);
        }

        @Test
        @DisplayName("limit uygulanır")
        void limit_applied() {
            List<ReviewRecordEntity> records = new java.util.ArrayList<>();
            for (int i = 0; i < 10; i++) {
                ReviewRecordEntity r = buildRecord(ReviewState.NEEDS_REVIEW);
                r.setReviewTargetType(ReviewTargetType.SCENARIO);
                r.setReviewTargetId(UUID.randomUUID());
                records.add(r);
            }
            when(reviewRecordRepository.findAll()).thenReturn(records);
            when(adminPreviewService.getPreview(any(), any())).thenReturn(null);
            when(readinessService.getReadiness(any(), any())).thenReturn(null);

            var overview = service.getOverview(null, null, 3);
            assertThat(overview.getItems()).hasSize(3);
            assertThat(overview.getTotalCount()).isEqualTo(10);
            assertThat(overview.getSafeWarnings()).contains("RESULT_TRUNCATED");
        }

        @Test
        @DisplayName("record query exception → safe empty overview")
        void recordQueryException_safeEmpty() {
            when(reviewRecordRepository.findAll())
                    .thenThrow(new RuntimeException("DB error"));

            var overview = service.getOverview(null, null, 50);
            assertThat(overview.getTotalCount()).isZero();
            assertThat(overview.getSafeWarnings()).contains("RECORD_QUERY_FAILED");
        }

        @Test
        @DisplayName("item aggregation exception → degraded item")
        void itemException_degraded() {
            ReviewRecordEntity r1 = buildRecord(ReviewState.NEEDS_REVIEW);
            r1.setReviewTargetType(ReviewTargetType.SCENARIO);
            r1.setReviewTargetId(UUID.randomUUID());

            when(reviewRecordRepository.findAll()).thenReturn(List.of(r1));
            when(adminPreviewService.getPreview(any(), any()))
                    .thenThrow(new RuntimeException("Preview error"));
            when(readinessService.getReadiness(any(), any()))
                    .thenThrow(new RuntimeException("Readiness error"));

            var overview = service.getOverview(null, null, 50);
            // Should still produce items (preview/readiness failures are handled gracefully)
            assertThat(overview.getTotalCount()).isEqualTo(1);
        }
    }

    // ── F. Target Type Coverage ────────────────────────────────────

    @Nested
    @DisplayName("F. Target Type Coverage")
    class TargetTypeCoverageTests {

        @Test
        @DisplayName("tüm target tipleri işlenebilir")
        void allTargetTypes_processable() {
            for (ReviewTargetType targetType : ReviewTargetType.values()) {
                UUID id = UUID.randomUUID();
                when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(targetType, id))
                        .thenReturn(Optional.empty());

                var item = service.getItemDetail(targetType, id);
                assertThat(item).isNotNull();
                assertThat(item.getPipelineStage()).isEqualTo(SafePipelineStage.UNAVAILABLE);
            }
        }

        @Test
        @DisplayName("5 target tipi desteklenir")
        void fiveTargetTypes_supported() {
            assertThat(ReviewTargetType.values()).hasSize(5);
            assertThat(ReviewTargetType.values()).containsExactlyInAnyOrder(
                    ReviewTargetType.CONTENT_PROJECT,
                    ReviewTargetType.SCENARIO,
                    ReviewTargetType.STORYBOARD,
                    ReviewTargetType.PACKAGE,
                    ReviewTargetType.IMAGE_ASSET);
        }
    }

    // ── G. No-Mutation Verification ────────────────────────────────

    @Nested
    @DisplayName("G. No-Mutation Verification")
    class NoMutationTests {

        @Test
        @DisplayName("getOverview hiçbir save/delete çağırmaz")
        void overview_noSaveOrDelete() {
            when(reviewRecordRepository.findAll()).thenReturn(List.of());
            service.getOverview(null, null, 50);
            verify(reviewRecordRepository, never()).save(any());
            verify(reviewRecordRepository, never()).delete(any());
            verify(reviewRecordRepository, never()).deleteById(any());
            verify(auditEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("getItemDetail hiçbir save/delete çağırmaz")
        void itemDetail_noSaveOrDelete() {
            UUID id = UUID.randomUUID();
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(any(), any()))
                    .thenReturn(Optional.empty());
            service.getItemDetail(ReviewTargetType.SCENARIO, id);
            verify(reviewRecordRepository, never()).save(any());
            verify(auditEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("service readOnly metotları kullanıyor (Transactional analizi)")
        void service_usesReadOnlyTransactions() {
            // Verify all public methods have @Transactional(readOnly = true)
            for (Method method : PipelineAggregationService.class.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    var txAnnotation = method.getAnnotation(
                            org.springframework.transaction.annotation.Transactional.class);
                    if (txAnnotation != null) {
                        assertThat(txAnnotation.readOnly())
                                .as("Method %s should be readOnly", method.getName())
                                .isTrue();
                    }
                }
            }
        }
    }

    // ── H. Forbidden Field Regression ──────────────────────────────

    @Nested
    @DisplayName("H. Forbidden Field Regression")
    class ForbiddenFieldTests {

        @Test
        @DisplayName("SafePipelineAggregateItem forbidden fields yok")
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
        @DisplayName("SafePipelineOverview forbidden fields yok")
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
        @DisplayName("SafePipelineStage forbidden values yok")
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

        @Test
        @DisplayName("SafePipelineAggregateItem toString forbidden string yok")
        void aggregateItemToString_noForbiddenStrings() {
            SafePipelineAggregateItem item = SafePipelineAggregateItem.unavailable(
                    ReviewTargetType.SCENARIO, UUID.randomUUID(), "TEST");
            String str = item.toString();

            List<String> forbidden = List.of(
                    "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                    "storageKey", "rawPayload", "base64", "secret", "apiKey",
                    "stackTrace", "publishStatus", "exportStatus");
            forbidden.forEach(f ->
                    assertThat(str).doesNotContain(f));
        }

        @Test
        @DisplayName("SafePipelineOverview toString forbidden string yok")
        void overviewToString_noForbiddenStrings() {
            SafePipelineOverview overview = SafePipelineOverview.empty();
            String str = overview.toString();

            List<String> forbidden = List.of(
                    "publicUrl", "signedUrl", "cdnUrl", "providerUrl",
                    "storageKey", "rawPayload", "base64", "secret", "apiKey",
                    "stackTrace", "publishStatus", "exportStatus");
            forbidden.forEach(f ->
                    assertThat(str).doesNotContain(f));
        }
    }

    // ── I. DTO Factory Methods ─────────────────────────────────────

    @Nested
    @DisplayName("I. DTO Factory Methods")
    class DtoFactoryTests {

        @Test
        @DisplayName("SafePipelineAggregateItem.unavailable doğru çalışır")
        void unavailable_correct() {
            var item = SafePipelineAggregateItem.unavailable(
                    ReviewTargetType.IMAGE_ASSET, UUID.randomUUID(), "TEST_REASON");
            assertThat(item.getPipelineStage()).isEqualTo(SafePipelineStage.UNAVAILABLE);
            assertThat(item.isPreviewAvailable()).isFalse();
            assertThat(item.isRevisionRequested()).isFalse();
            assertThat(item.getSafeBlockingReasons()).contains("TEST_REASON");
        }

        @Test
        @DisplayName("SafePipelineAggregateItem.unavailable null reason → default")
        void unavailable_nullReason_default() {
            var item = SafePipelineAggregateItem.unavailable(
                    ReviewTargetType.SCENARIO, UUID.randomUUID(), null);
            assertThat(item.getSafeBlockingReasons()).contains("TARGET_UNAVAILABLE");
        }

        @Test
        @DisplayName("SafePipelineOverview.empty doğru çalışır")
        void empty_correct() {
            var overview = SafePipelineOverview.empty();
            assertThat(overview.getTotalCount()).isZero();
            assertThat(overview.getItems()).isEmpty();
            assertThat(overview.getCountsByStage()).isEmpty();
            assertThat(overview.getCountsByTargetType()).isEmpty();
            assertThat(overview.getSafeWarnings()).isEmpty();
            assertThat(overview.getUpdatedAt()).isNotNull();
        }
    }

    // ── J. Endpoint Count Regression ───────────────────────────────

    @Nested
    @DisplayName("J. Endpoint Count Regression")
    class EndpointRegressionTests {

        @Test
        @DisplayName("PipelineAggregationService controller endpoint içermez")
        void service_noControllerAnnotation() {
            assertThat(PipelineAggregationService.class.getAnnotation(
                    org.springframework.web.bind.annotation.RestController.class)).isNull();
            assertThat(PipelineAggregationService.class.getAnnotation(
                    org.springframework.stereotype.Controller.class)).isNull();
        }

        @Test
        @DisplayName("PipelineAggregationService hiçbir @RequestMapping içermez")
        void service_noRequestMappings() {
            for (Method method : PipelineAggregationService.class.getDeclaredMethods()) {
                assertThat(method.getAnnotation(
                        org.springframework.web.bind.annotation.GetMapping.class)).isNull();
                assertThat(method.getAnnotation(
                        org.springframework.web.bind.annotation.PostMapping.class)).isNull();
                assertThat(method.getAnnotation(
                        org.springframework.web.bind.annotation.PutMapping.class)).isNull();
                assertThat(method.getAnnotation(
                        org.springframework.web.bind.annotation.DeleteMapping.class)).isNull();
            }
        }

        @Test
        @DisplayName("AdminReviewController POST sayısı 9")
        void adminReviewController_post9() {
            long postCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(
                            org.springframework.web.bind.annotation.PostMapping.class))
                    .count();
            assertThat(postCount).isEqualTo(9);
        }

        @Test
        @DisplayName("AdminReviewController GET sayısı 8")
        void adminReviewController_get8() {
            long getCount = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(
                            org.springframework.web.bind.annotation.GetMapping.class))
                    .count();
            assertThat(getCount).isEqualTo(8);
        }
    }

    // ── K. Preview/Readiness Integration ───────────────────────────

    @Nested
    @DisplayName("K. Preview/Readiness Integration")
    class IntegrationTests {

        @Test
        @DisplayName("preview exception → previewAvailable=false")
        void previewException_safeDefault() {
            UUID id = UUID.randomUUID();
            ReviewRecordEntity record = buildRecord(ReviewState.IN_REVIEW);
            record.setReviewTargetType(ReviewTargetType.SCENARIO);
            record.setReviewTargetId(id);

            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(any(), any()))
                    .thenReturn(Optional.of(record));
            when(adminPreviewService.getPreview(any(), any()))
                    .thenThrow(new RuntimeException("Preview service down"));
            when(readinessService.getReadiness(any(), any())).thenReturn(null);

            var item = service.getItemDetail(ReviewTargetType.SCENARIO, id);
            assertThat(item.isPreviewAvailable()).isFalse();
        }

        @Test
        @DisplayName("readiness exception → UNAVAILABLE readiness")
        void readinessException_safeDefault() {
            UUID id = UUID.randomUUID();
            ReviewRecordEntity record = buildRecord(ReviewState.APPROVED_FOR_INTERNAL_USE);
            record.setReviewTargetType(ReviewTargetType.PACKAGE);
            record.setReviewTargetId(id);

            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(any(), any()))
                    .thenReturn(Optional.of(record));
            when(adminPreviewService.getPreview(any(), any())).thenReturn(null);
            when(readinessService.getReadiness(any(), any()))
                    .thenThrow(new RuntimeException("Readiness service down"));

            var item = service.getItemDetail(ReviewTargetType.PACKAGE, id);
            assertThat(item.getReadinessAvailability())
                    .isEqualTo(InternalApprovalReadinessAvailability.UNAVAILABLE);
            // APPROVED + UNAVAILABLE readiness → INTERNALLY_APPROVED
            assertThat(item.getPipelineStage()).isEqualTo(SafePipelineStage.INTERNALLY_APPROVED);
        }

        @Test
        @DisplayName("preview available → item reflects it")
        void previewAvailable_reflected() {
            UUID id = UUID.randomUUID();
            ReviewRecordEntity record = buildRecord(ReviewState.IN_REVIEW);
            record.setReviewTargetType(ReviewTargetType.STORYBOARD);
            record.setReviewTargetId(id);

            SafeAdminPreview preview = SafeAdminPreview.builder()
                    .targetType(ReviewTargetType.STORYBOARD)
                    .targetId(id)
                    .previewAvailable(true)
                    .build();

            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(any(), any()))
                    .thenReturn(Optional.of(record));
            when(adminPreviewService.getPreview(any(), any())).thenReturn(preview);
            when(readinessService.getReadiness(any(), any())).thenReturn(null);

            var item = service.getItemDetail(ReviewTargetType.STORYBOARD, id);
            assertThat(item.isPreviewAvailable()).isTrue();
        }
    }

    // ── Helper ─────────────────────────────────────────────────────

    private ReviewRecordEntity buildRecord(ReviewState state) {
        ReviewRecordEntity record = new ReviewRecordEntity();
        record.setId(UUID.randomUUID());
        record.setReviewTargetType(ReviewTargetType.SCENARIO);
        record.setReviewTargetId(UUID.randomUUID());
        record.setCurrentState(state);
        record.setCreatedByActorReference("test-actor");
        record.setLastActionType(ReviewActionType.SUBMIT_FOR_REVIEW);
        record.setLastActionAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        return record;
    }
}
