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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
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
 * FP-012 Task 11 — Action Availability, UX Hardening & Closure Candidate.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FP-012 Task 11 — Action Availability & Closure Candidate")
class AdminReviewAvailabilityTest {

    @Mock
    private ReviewRecordRepository reviewRecordRepository;
    @Mock
    private ReviewAuditEventRepository auditEventRepository;
    @Mock
    private ReviewApplicationService reviewApplicationServiceMock;
    @Mock
    private ReviewHandoffService reviewHandoffService;
    @Mock
    private AdminPreviewService adminPreviewService;
    @Mock
    private InternalApprovalReadinessService internalApprovalReadinessService;
    @Mock
    private InternalRevisionRequestService internalRevisionRequestService;

    private AdminReviewController controller;
    private ReviewApplicationService realService;

    private static final ReviewTargetType TARGET_TYPE = ReviewTargetType.CONTENT_PROJECT;
    private static final UUID TARGET_ID = UUID.randomUUID();

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "providerUrl", "storageKey", "bucketName", "bucketPath",
            "rawPayload", "base64", "secret", "apiKey", "config",
            "publicUrl", "signedUrl", "stackTrace"
    );

    @BeforeEach
    void setUp() {
        controller = new AdminReviewController(reviewApplicationServiceMock, reviewHandoffService, adminPreviewService, internalApprovalReadinessService, internalRevisionRequestService);
        realService = new ReviewApplicationService(reviewRecordRepository, auditEventRepository);
    }

    // ── 1. Availability Endpoint ────────────────────────────────────

    @Nested
    @DisplayName("1. Availability Endpoint")
    class AvailabilityEndpoint {

        @Test
        @DisplayName("getAvailableActions returns availability for REVIEWER")
        void availabilityHappy() {
            AdminReviewActionAvailability avail = AdminReviewActionAvailability.builder()
                    .targetType(TARGET_TYPE).targetId(TARGET_ID)
                    .currentState(ReviewState.IN_REVIEW)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .availableActions(List.of())
                    .build();
            when(reviewApplicationServiceMock.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.REVIEWER))
                    .thenReturn(Optional.of(avail));

            var response = controller.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.REVIEWER);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getActorRole()).isEqualTo(ReviewActorRole.REVIEWER);
        }

        @Test
        @DisplayName("getAvailableActions 404 when not found")
        void availabilityNotFound() {
            when(reviewApplicationServiceMock.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.REVIEWER))
                    .thenReturn(Optional.empty());

            var response = controller.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.REVIEWER);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ── 2. Availability by State & Actor Role (Service) ─────────────

    @Nested
    @DisplayName("2. Availability Logic")
    class AvailabilityLogic {

        private ReviewRecordEntity recordWithState(ReviewState state) {
            ReviewRecordEntity entity = ReviewRecordEntity.builder()
                    .reviewTargetType(TARGET_TYPE).reviewTargetId(TARGET_ID)
                    .currentState(state).build();
            entity.setId(UUID.randomUUID());
            return entity;
        }

        @Test
        @DisplayName("IN_REVIEW + REVIEWER: approve enabled")
        void inReviewReviewerApprove() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.IN_REVIEW)));

            var result = realService.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.REVIEWER);
            assertThat(result).isPresent();
            var actions = result.get().getAvailableActions();
            var approve = actions.stream()
                    .filter(a -> a.getActionType() == ReviewActionType.APPROVE_FOR_INTERNAL_USE).findFirst();
            assertThat(approve).isPresent();
            assertThat(approve.get().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("IN_REVIEW + CREATOR: approve disabled")
        void inReviewCreatorApproveDisabled() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.IN_REVIEW)));

            var result = realService.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.CREATOR);
            var approve = result.get().getAvailableActions().stream()
                    .filter(a -> a.getActionType() == ReviewActionType.APPROVE_FOR_INTERNAL_USE).findFirst();
            assertThat(approve.get().isEnabled()).isFalse();
            assertThat(approve.get().getDisabledReasonCode()).isEqualTo("ACTOR_NOT_PERMITTED");
        }

        @Test
        @DisplayName("IN_REVIEW + SYSTEM: approve disabled")
        void inReviewSystemApproveDisabled() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.IN_REVIEW)));

            var result = realService.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.SYSTEM);
            var approve = result.get().getAvailableActions().stream()
                    .filter(a -> a.getActionType() == ReviewActionType.APPROVE_FOR_INTERNAL_USE).findFirst();
            assertThat(approve.get().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("IN_REVIEW + ADMIN: approve enabled")
        void inReviewAdminApproveEnabled() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.IN_REVIEW)));

            var result = realService.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.ADMIN);
            var approve = result.get().getAvailableActions().stream()
                    .filter(a -> a.getActionType() == ReviewActionType.APPROVE_FOR_INTERNAL_USE).findFirst();
            assertThat(approve.get().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE: only note and REQUEST_INTERNAL_REVISION enabled")
        void approvedTerminalNoteAndRevision() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.APPROVED_FOR_INTERNAL_USE)));

            var result = realService.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.REVIEWER);
            var actions = result.get().getAvailableActions();
            // FP-015 Task 02: REQUEST_INTERNAL_REVISION is now also enabled
            long enabledStateChanging = actions.stream()
                    .filter(a -> a.getActionType() != ReviewActionType.ADD_REVIEW_NOTE)
                    .filter(a -> a.getActionType() != ReviewActionType.REQUEST_INTERNAL_REVISION)
                    .filter(AdminReviewAvailableAction::isEnabled).count();
            assertThat(enabledStateChanging).isZero();
            var note = actions.stream()
                    .filter(a -> a.getActionType() == ReviewActionType.ADD_REVIEW_NOTE).findFirst();
            assertThat(note.get().isEnabled()).isTrue();
            var revision = actions.stream()
                    .filter(a -> a.getActionType() == ReviewActionType.REQUEST_INTERNAL_REVISION).findFirst();
            assertThat(revision.get().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("REJECTED: only note enabled, no state-changing")
        void rejectedTerminalOnlyNote() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.REJECTED)));

            var result = realService.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.REVIEWER);
            var actions = result.get().getAvailableActions();
            long enabledStateChanging = actions.stream()
                    .filter(a -> a.getActionType() != ReviewActionType.ADD_REVIEW_NOTE)
                    .filter(AdminReviewAvailableAction::isEnabled).count();
            assertThat(enabledStateChanging).isZero();
        }

        @Test
        @DisplayName("DRAFT + CREATOR: submit enabled, approve disabled")
        void draftCreator() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.DRAFT)));

            var result = realService.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.CREATOR);
            var actions = result.get().getAvailableActions();
            var submit = actions.stream()
                    .filter(a -> a.getActionType() == ReviewActionType.SUBMIT_FOR_REVIEW).findFirst();
            assertThat(submit.get().isEnabled()).isTrue();
            var approve = actions.stream()
                    .filter(a -> a.getActionType() == ReviewActionType.APPROVE_FOR_INTERNAL_USE).findFirst();
            assertThat(approve.get().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("All 7 action types returned")
        void allActionTypes() {
            when(reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(TARGET_TYPE, TARGET_ID))
                    .thenReturn(Optional.of(recordWithState(ReviewState.IN_REVIEW)));

            var result = realService.getAvailableActions(TARGET_TYPE, TARGET_ID, ReviewActorRole.REVIEWER);
            assertThat(result.get().getAvailableActions()).hasSize(8);
        }
    }

    // ── 3. DTO Forbidden Fields ─────────────────────────────────────

    @Nested
    @DisplayName("3. DTO Forbidden Fields")
    class DtoForbiddenFields {

        @Test
        @DisplayName("AdminReviewAvailableAction has no forbidden fields")
        void actionSafe() {
            Set<String> fields = Arrays.stream(AdminReviewAvailableAction.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }

        @Test
        @DisplayName("AdminReviewActionAvailability has no forbidden fields")
        void availabilitySafe() {
            Set<String> fields = Arrays.stream(AdminReviewActionAvailability.class.getDeclaredFields())
                    .map(Field::getName).collect(Collectors.toSet());
            FORBIDDEN_FIELDS.forEach(f -> assertThat(fields).doesNotContain(f));
        }
    }

    // ── 4. Updated Endpoint Inventory ───────────────────────────────

    @Nested
    @DisplayName("4. Endpoint Inventory — 14 total")
    class EndpointInventory {

        @Test
        @DisplayName("Controller has 8 POST + 6 GET = 14 endpoints")
        void exactCount() {
            long post = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(PostMapping.class)).count();
            long get = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class)).count();
            assertThat(post).isEqualTo(9);
            assertThat(get).isEqualTo(8);
        }

        @Test
        @DisplayName("GET endpoints include actions")
        void actionsEndpoint() {
            Set<String> getPaths = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(GetMapping.class))
                    .flatMap(m -> Arrays.stream(m.getAnnotation(GetMapping.class).value()))
                    .collect(Collectors.toSet());
            assertThat(getPaths).anyMatch(p -> p.contains("actions"));
        }

        @Test
        @DisplayName("No forbidden endpoint paths")
        void noForbiddenPaths() {
            Set<String> allPaths = Arrays.stream(AdminReviewController.class.getDeclaredMethods())
                    .flatMap(m -> {
                        java.util.List<String> p = new java.util.ArrayList<>();
                        if (m.isAnnotationPresent(PostMapping.class))
                            p.addAll(Arrays.asList(m.getAnnotation(PostMapping.class).value()));
                        if (m.isAnnotationPresent(GetMapping.class))
                            p.addAll(Arrays.asList(m.getAnnotation(GetMapping.class).value()));
                        return p.stream();
                    }).collect(Collectors.toSet());
            Set<String> forbidden = Set.of("publish", "export", "completed",
                    "ready-for-publish", "public-url", "signed-url", "cdn",
                    "provider-url", "storage-key", "generic-status");
            for (String path : allPaths) {
                for (String f : forbidden) {
                    assertThat(path.toLowerCase()).doesNotContain(f);
                }
            }
        }
    }

    // ── 5. No Forbidden Action Values ───────────────────────────────

    @Nested
    @DisplayName("5. No Forbidden Action Values")
    class NoForbiddenActionValues {

        @Test
        @DisplayName("ReviewActionType has no publish/export/auto-approve")
        void noForbiddenActions() {
            Set<String> names = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISH");
            assertThat(names).doesNotContain("EXPORT");
            assertThat(names).doesNotContain("AUTO_APPROVE");
            assertThat(names).doesNotContain("MARK_COMPLETED");
            assertThat(names).doesNotContain("READY_FOR_PUBLISH");
        }
    }

    // ── 6. No Migration ─────────────────────────────────────────────

    @Nested
    @DisplayName("6. Boundary")
    class Boundary {

        @Test
        @DisplayName("No V11 migration file exists")
        void noNewMigration() {
            assertThat(java.nio.file.Path.of(
                    "src/main/resources/db/migration/V11__fp012_availability.sql").toFile())
                    .doesNotExist();
        }

        @Test
        @DisplayName("AdminReviewAvailableAction is not @Entity")
        void actionNotEntity() {
            assertThat(AdminReviewAvailableAction.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }

        @Test
        @DisplayName("AdminReviewActionAvailability is not @Entity")
        void availabilityNotEntity() {
            assertThat(AdminReviewActionAvailability.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }
    }
}
