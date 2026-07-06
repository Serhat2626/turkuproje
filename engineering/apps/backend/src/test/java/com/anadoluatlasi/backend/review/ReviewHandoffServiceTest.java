package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FP-013 Task 02 — Review Handoff Service, Idempotency & Safe Audit Integration.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FP-013 Task 02 — Review Handoff Service")
class ReviewHandoffServiceTest {

    @Mock
    private ReviewRecordRepository reviewRecordRepository;
    @Mock
    private ReviewAuditEventRepository auditEventRepository;

    private ReviewHandoffService handoffService;

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
        handoffService = new ReviewHandoffService(reviewRecordRepository, auditEventRepository);
    }

    private ReviewHandoffRequest defaultRequest() {
        return ReviewHandoffRequest.builder()
                .targetType(TARGET_TYPE)
                .targetId(TARGET_ID)
                .source(ReviewHandoffSource.CONTENT_PROJECT_GENERATION)
                .actorRole(ReviewActorRole.SYSTEM)
                .actorReference("system-gen-001")
                .safeTitle("Test Title")
                .safeSummary("Test Summary")
                .build();
    }

    private ReviewRecordEntity recordWithState(ReviewState state) {
        ReviewRecordEntity entity = ReviewRecordEntity.builder()
                .reviewTargetType(TARGET_TYPE)
                .reviewTargetId(TARGET_ID)
                .currentState(state)
                .build();
        entity.setId(UUID.randomUUID());
        return entity;
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. Request/Result Model Safety
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Request/Result Model Safety")
    class ModelSafety {

        @Test
        @DisplayName("ReviewHandoffRequest has no forbidden fields")
        void requestNoForbiddenFields() {
            Set<String> fields = Arrays.stream(ReviewHandoffRequest.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("ReviewHandoffResult has no forbidden fields")
        void resultNoForbiddenFields() {
            Set<String> fields = Arrays.stream(ReviewHandoffResult.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("ReviewHandoffRequest.toString is safe")
        void requestToStringSafe() {
            String s = defaultRequest().toString();
            assertThat(s).doesNotContain("providerUrl");
            assertThat(s).doesNotContain("storageKey");
            assertThat(s).doesNotContain("base64");
            assertThat(s).doesNotContain("secret");
            assertThat(s).contains("ReviewHandoffRequest");
        }

        @Test
        @DisplayName("ReviewHandoffResult.toString is safe")
        void resultToStringSafe() {
            ReviewHandoffResult result = ReviewHandoffResult.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .currentState(ReviewState.NEEDS_REVIEW)
                    .created(true).build();
            String s = result.toString();
            assertThat(s).doesNotContain("providerUrl");
            assertThat(s).doesNotContain("storageKey");
            assertThat(s).contains("ReviewHandoffResult");
        }

        @Test
        @DisplayName("ReviewHandoffRequest.createSafe sanitizes text via ReviewTextSafety")
        void createSafeSanitizes() {
            String rawTitle = "Title with \u0000 control chars";
            String rawSummary = "Summary with \u0007 bell";
            ReviewHandoffRequest req = ReviewHandoffRequest.createSafe(
                    TARGET_TYPE, TARGET_ID,
                    ReviewHandoffSource.SCENARIO_GENERATION,
                    ReviewActorRole.SYSTEM, "ref",
                    rawTitle, rawSummary);
            // ReviewTextSafety.sanitize removes control characters
            assertThat(req.getSafeTitle()).doesNotContain("\u0000");
            assertThat(req.getSafeSummary()).doesNotContain("\u0007");
            assertThat(req.getSafeTitle()).isNotNull();
            assertThat(req.getSafeSummary()).isNotNull();
        }

        @Test
        @DisplayName("ReviewHandoffException.toString is safe")
        void exceptionToStringSafe() {
            ReviewHandoffException ex = new ReviewHandoffException("test", "ERR_CODE");
            assertThat(ex.toString()).doesNotContain("providerUrl");
            assertThat(ex.toString()).contains("ERR_CODE");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. New Handoff Creation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. New Handoff Creation")
    class NewCreation {

        @Test
        @DisplayName("No existing record -> creates review record in NEEDS_REVIEW")
        void createsNew() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.isCreated()).isTrue();
            assertThat(result.isPreserved()).isFalse();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("Audit event persisted on creation")
        void auditOnCreate() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            verify(auditEventRepository).save(any(ReviewAuditEventEntity.class));
            assertThat(result.getAuditEventType()).isEqualTo(ReviewAuditEventType.REVIEW_SUBMITTED);
        }

        @Test
        @DisplayName("Result says created=true, updated=false, preserved=false")
        void resultFlags() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.isCreated()).isTrue();
            assertThat(result.isUpdated()).isFalse();
            assertThat(result.isPreserved()).isFalse();
        }

        @Test
        @DisplayName("No automatic approval on creation")
        void noAutoApprove() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.getCurrentState()).isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. Idempotency / Duplicate Protection
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Idempotency")
    class Idempotency {

        @Test
        @DisplayName("Existing NEEDS_REVIEW is preserved")
        void needsReviewPreserved() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.NEEDS_REVIEW)));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.isPreserved()).isTrue();
            assertThat(result.isCreated()).isFalse();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("Existing IN_REVIEW is preserved")
        void inReviewPreserved() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.IN_REVIEW)));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.isPreserved()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.IN_REVIEW);
        }

        @Test
        @DisplayName("Existing CHANGES_REQUESTED is preserved")
        void changesRequestedPreserved() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.CHANGES_REQUESTED)));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.isPreserved()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
        }

        @Test
        @DisplayName("Existing APPROVED_FOR_INTERNAL_USE is never overwritten")
        void approvedNeverOverwritten() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.APPROVED_FOR_INTERNAL_USE)));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.isPreserved()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            verify(reviewRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Existing REJECTED is preserved")
        void rejectedPreserved() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.REJECTED)));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.isPreserved()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.REJECTED);
        }

        @Test
        @DisplayName("Preserved result has safeReasonCode ALREADY_IN_PIPELINE")
        void preservedReasonCode() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.IN_REVIEW)));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.getSafeReasonCode()).isEqualTo("ALREADY_IN_PIPELINE");
        }

        @Test
        @DisplayName("No duplicate record created when existing")
        void noDuplicate() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.NEEDS_REVIEW)));

            handoffService.handoffForReview(defaultRequest());

            verify(reviewRecordRepository, never()).save(any());
            verify(auditEventRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. DRAFT / AI_GENERATED Handling
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. DRAFT / AI_GENERATED Handling")
    class AdvanceableStates {

        @Test
        @DisplayName("Existing DRAFT advances to NEEDS_REVIEW")
        void draftAdvances() {
            ReviewRecordEntity record = recordWithState(ReviewState.DRAFT);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(record));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.isUpdated()).isTrue();
            assertThat(result.isCreated()).isFalse();
            assertThat(result.isPreserved()).isFalse();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
            assertThat(result.getPreviousState()).isEqualTo(ReviewState.DRAFT);
        }

        @Test
        @DisplayName("Existing AI_GENERATED advances to NEEDS_REVIEW")
        void aiGeneratedAdvances() {
            ReviewRecordEntity record = recordWithState(ReviewState.AI_GENERATED);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(record));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.isUpdated()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("Advance persists audit event")
        void advanceAudits() {
            ReviewRecordEntity record = recordWithState(ReviewState.DRAFT);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(record));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            handoffService.handoffForReview(defaultRequest());

            verify(auditEventRepository).save(any(ReviewAuditEventEntity.class));
        }

        @Test
        @DisplayName("Advance does not auto-approve")
        void advanceNoApproval() {
            ReviewRecordEntity record = recordWithState(ReviewState.AI_GENERATED);
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(record));
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());

            assertThat(result.getCurrentState()).isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. Actor Guardrails
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. Actor Guardrails")
    class ActorGuardrails {

        @Test
        @DisplayName("SYSTEM can initiate handoff")
        void systemCanHandoff() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewHandoffRequest req = ReviewHandoffRequest.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .source(ReviewHandoffSource.SCENARIO_GENERATION)
                    .actorRole(ReviewActorRole.SYSTEM)
                    .actorReference("system").build();

            ReviewHandoffResult result = handoffService.handoffForReview(req);
            assertThat(result.isCreated()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("SYSTEM handoff never results in APPROVED_FOR_INTERNAL_USE")
        void systemNeverApproves() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewHandoffResult result = handoffService.handoffForReview(defaultRequest());
            assertThat(result.getCurrentState()).isNotEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("CREATOR can initiate handoff")
        void creatorCanHandoff() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.empty());
            when(reviewRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReviewHandoffRequest req = ReviewHandoffRequest.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .source(ReviewHandoffSource.CONTENT_PROJECT_GENERATION)
                    .actorRole(ReviewActorRole.CREATOR)
                    .actorReference("creator").build();

            ReviewHandoffResult result = handoffService.handoffForReview(req);
            assertThat(result.isCreated()).isTrue();
        }

        @Test
        @DisplayName("actorRole does not bypass terminal state preservation")
        void actorRoleDoesNotBypassTerminal() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.APPROVED_FOR_INTERNAL_USE)));

            ReviewHandoffRequest req = ReviewHandoffRequest.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .source(ReviewHandoffSource.IMAGE_GENERATION)
                    .actorRole(ReviewActorRole.ADMIN)
                    .actorReference("admin").build();

            ReviewHandoffResult result = handoffService.handoffForReview(req);
            assertThat(result.isPreserved()).isTrue();
            assertThat(result.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. Safe Failure Behavior
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. Safe Failure Behavior")
    class FailureBehavior {

        @Test
        @DisplayName("Null request throws ReviewHandoffException")
        void nullRequest() {
            assertThatThrownBy(() -> handoffService.handoffForReview(null))
                    .isInstanceOf(ReviewHandoffException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("Null targetType throws safely")
        void nullTargetType() {
            ReviewHandoffRequest req = ReviewHandoffRequest.builder()
                    .targetId(TARGET_ID)
                    .source(ReviewHandoffSource.SCENARIO_GENERATION)
                    .actorRole(ReviewActorRole.SYSTEM).build();
            assertThatThrownBy(() -> handoffService.handoffForReview(req))
                    .isInstanceOf(ReviewHandoffException.class);
        }

        @Test
        @DisplayName("Null targetId throws safely")
        void nullTargetId() {
            ReviewHandoffRequest req = ReviewHandoffRequest.builder()
                    .targetType(TARGET_TYPE)
                    .source(ReviewHandoffSource.SCENARIO_GENERATION)
                    .actorRole(ReviewActorRole.SYSTEM).build();
            assertThatThrownBy(() -> handoffService.handoffForReview(req))
                    .isInstanceOf(ReviewHandoffException.class);
        }

        @Test
        @DisplayName("Null source throws safely")
        void nullSource() {
            ReviewHandoffRequest req = ReviewHandoffRequest.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .actorRole(ReviewActorRole.SYSTEM).build();
            assertThatThrownBy(() -> handoffService.handoffForReview(req))
                    .isInstanceOf(ReviewHandoffException.class);
        }

        @Test
        @DisplayName("Null actorRole throws safely")
        void nullActorRole() {
            ReviewHandoffRequest req = ReviewHandoffRequest.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .source(ReviewHandoffSource.SCENARIO_GENERATION).build();
            assertThatThrownBy(() -> handoffService.handoffForReview(req))
                    .isInstanceOf(ReviewHandoffException.class);
        }

        @Test
        @DisplayName("Exception message does not expose forbidden details")
        void exceptionSafe() {
            try {
                handoffService.handoffForReview(null);
                fail("Expected exception");
            } catch (ReviewHandoffException e) {
                assertThat(e.getMessage()).doesNotContain("providerUrl");
                assertThat(e.getMessage()).doesNotContain("storageKey");
                assertThat(e.getMessage()).doesNotContain("base64");
                assertThat(e.getMessage()).doesNotContain("secret");
                assertThat(e.getSafeErrorCode()).isNotNull();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. Audit Safety
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("7. Audit Safety")
    class AuditSafety {

        @Test
        @DisplayName("Audit event enum has no forbidden event types")
        void noForbiddenEventTypes() {
            Set<String> names = Arrays.stream(ReviewAuditEventType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED");
            assertThat(names).doesNotContain("EXPORTED");
            assertThat(names).doesNotContain("COMPLETED");
            assertThat(names).doesNotContain("READY_FOR_PUBLISH");
            assertThat(names).doesNotContain("PUBLIC_URL_CREATED");
            assertThat(names).doesNotContain("SIGNED_URL_CREATED");
            assertThat(names).doesNotContain("AUTO_APPROVED");
        }

        @Test
        @DisplayName("ReviewHandoffSource has no forbidden values")
        void noForbiddenSources() {
            Set<String> names = Arrays.stream(ReviewHandoffSource.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISH");
            assertThat(names).doesNotContain("EXPORT");
            assertThat(names).doesNotContain("PUBLIC_DELIVERY");
            assertThat(names).doesNotContain("CDN");
            assertThat(names).doesNotContain("SIGNED_URL");
            assertThat(names).doesNotContain("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("ReviewAuditEventEntity has no forbidden fields")
        void auditEntitySafe() {
            Set<String> fields = Arrays.stream(ReviewAuditEventEntity.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. Boundary Regression
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("8. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("AdminReviewController endpoint count unchanged (14)")
        void endpointCountUnchanged() {
            long post = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long get = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(post).isEqualTo(8);
            assertThat(get).isEqualTo(6);
        }

        @Test
        @DisplayName("ReviewHandoffService is not a Controller")
        void notController() {
            assertThat(ReviewHandoffService.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
        }

        @Test
        @DisplayName("ReviewHandoffRequest is not a JPA Entity")
        void requestNotEntity() {
            assertThat(ReviewHandoffRequest.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("ReviewHandoffResult is not a JPA Entity")
        void resultNotEntity() {
            assertThat(ReviewHandoffResult.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("No V11 migration file exists")
        void noNewMigration() {
            assertThat(java.nio.file.Path.of(
                    "src/main/resources/db/migration/V11__fp013_handoff.sql").toFile())
                    .doesNotExist();
        }

        @Test
        @DisplayName("ReviewActionType has no forbidden values")
        void noForbiddenActions() {
            Set<String> names = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISH");
            assertThat(names).doesNotContain("EXPORT");
            assertThat(names).doesNotContain("AUTO_APPROVE");
            assertThat(names).doesNotContain("MARK_COMPLETED");
            assertThat(names).doesNotContain("READY_FOR_PUBLISH");
        }

        @Test
        @DisplayName("ReviewState has no forbidden values")
        void noForbiddenStates() {
            Set<String> names = Arrays.stream(ReviewState.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED");
            assertThat(names).doesNotContain("EXPORTED");
            assertThat(names).doesNotContain("COMPLETED");
            assertThat(names).doesNotContain("READY_FOR_PUBLISH");
        }
    }
}
