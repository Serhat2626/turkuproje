package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-012 Task 06 — Safe Admin Review DTOs, Query Models & API Contract Design.
 */
@DisplayName("FP-012 Task 06 — Admin Review DTOs & Mapper Safety")
class AdminReviewDtoSafetyTest {

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "providerUrl", "storageKey", "bucketName", "bucketPath",
            "rawPayload", "rawProviderPayload", "rawStoragePayload",
            "base64", "secret", "apiKey", "config",
            "publicUrl", "signedUrl", "publishUrl", "exportUrl", "stackTrace"
    );

    // ── 1. Request DTO Safety ───────────────────────────────────────

    @Nested
    @DisplayName("1. Request DTO Safety")
    class RequestDtoSafety {

        @Test
        @DisplayName("Expected safe fields exist")
        void expectedFields() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewActionRequest.class);
            assertThat(fields).contains("targetType", "targetId", "actionType",
                    "actorRole", "actorReference", "reasonCode", "note");
        }

        @Test
        @DisplayName("No forbidden fields")
        void noForbiddenFields() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewActionRequest.class);
            for (String f : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Forbidden: " + f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("Uses explicit ReviewActionType, not generic status string")
        void usesExplicitActionType() throws NoSuchFieldException {
            Field actionField = AdminReviewActionRequest.class.getDeclaredField("actionType");
            assertThat(actionField.getType()).isEqualTo(ReviewActionType.class);
        }

        @Test
        @DisplayName("No generic status field")
        void noGenericStatus() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewActionRequest.class);
            assertThat(fields).doesNotContain("status");
            assertThat(fields).doesNotContain("newStatus");
            assertThat(fields).doesNotContain("genericStatus");
        }

        @Test
        @DisplayName("Cannot represent READY_FOR_PUBLISH/COMPLETED/PUBLISHED/EXPORTED")
        void cannotRepresentForbiddenStates() {
            Set<String> actionNames = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(actionNames).doesNotContain("PUBLISH");
            assertThat(actionNames).doesNotContain("EXPORT");
            assertThat(actionNames).doesNotContain("MARK_COMPLETED");
            assertThat(actionNames).doesNotContain("GENERIC_STATUS_UPDATE");
        }

        @Test
        @DisplayName("toString does not leak note content")
        void toStringSafe() {
            AdminReviewActionRequest req = new AdminReviewActionRequest();
            req.setTargetType(ReviewTargetType.CONTENT_PROJECT);
            req.setTargetId(UUID.randomUUID());
            req.setActionType(ReviewActionType.REJECT);
            req.setActorRole(ReviewActorRole.REVIEWER);
            req.setNote("Sensitive review note");
            req.setReasonCode("QUALITY_ISSUE");

            String str = req.toString();
            assertThat(str).contains("hasNote=true");
            assertThat(str).doesNotContain("Sensitive review note");
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secretKey");
        }

        @Test
        @DisplayName("No controller/REST annotations")
        void noControllerAnnotations() {
            assertThat(AdminReviewActionRequest.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
            assertThat(AdminReviewActionRequest.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RequestMapping.class)).isFalse();
        }

        @Test
        @DisplayName("No JPA annotations")
        void noJpaAnnotations() {
            assertThat(AdminReviewActionRequest.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }
    }

    // ── 2. Response DTO Safety ──────────────────────────────────────

    @Nested
    @DisplayName("2. Response DTO Safety")
    class ResponseDtoSafety {

        @Test
        @DisplayName("No forbidden fields")
        void noForbiddenFields() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewActionResponse.class);
            for (String f : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Forbidden: " + f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("toString safe")
        void toStringSafe() {
            AdminReviewActionResponse resp = AdminReviewActionResponse.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(UUID.randomUUID())
                    .previousState(ReviewState.IN_REVIEW)
                    .currentState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .actionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .changed(true)
                    .safeMessage("Approved")
                    .build();
            String str = resp.toString();
            assertThat(str).contains("APPROVED_FOR_INTERNAL_USE");
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secretKey");
            assertThat(str).doesNotContain("base64");
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE remains internal only")
        void approvedInternalOnly() {
            AdminReviewActionResponse resp = AdminReviewActionResponse.builder()
                    .currentState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .build();
            assertThat(resp.getCurrentState().name()).doesNotContain("PUBLISH");
            assertThat(resp.getCurrentState().name()).doesNotContain("EXPORT");
        }

        @Test
        @DisplayName("No JPA annotations")
        void noJpa() {
            assertThat(AdminReviewActionResponse.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isFalse();
        }
    }

    // ── 3. Summary DTO Safety ───────────────────────────────────────

    @Nested
    @DisplayName("3. Summary DTO Safety")
    class SummaryDtoSafety {

        @Test
        @DisplayName("No forbidden fields")
        void noForbiddenFields() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewStateSummary.class);
            for (String f : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Forbidden: " + f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("No publish/export readiness field")
        void noPublishExportField() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewStateSummary.class);
            for (String f : fields) {
                assertThat(f.toLowerCase()).doesNotContain("publish");
                assertThat(f.toLowerCase()).doesNotContain("export");
                assertThat(f.toLowerCase()).doesNotContain("cdn");
            }
        }

        @Test
        @DisplayName("toString safe")
        void toStringSafe() {
            AdminReviewStateSummary summary = AdminReviewStateSummary.builder()
                    .targetType(ReviewTargetType.IMAGE_ASSET)
                    .targetId(UUID.randomUUID())
                    .currentState(ReviewState.IN_REVIEW)
                    .safeMessage("Some sensitive info should not leak")
                    .build();
            String str = summary.toString();
            assertThat(str).doesNotContain("sensitive info");
            assertThat(str).doesNotContain("https://");
        }

        @Test
        @DisplayName("Maps ReviewRecordEntity to safe summary")
        void mapperProducesSafeSummary() {
            ReviewRecordEntity entity = ReviewRecordEntity.builder()
                    .reviewTargetType(ReviewTargetType.STORYBOARD)
                    .reviewTargetId(UUID.randomUUID())
                    .currentState(ReviewState.CHANGES_REQUESTED)
                    .lastActionType(ReviewActionType.REQUEST_CHANGES)
                    .lastActionByActorReference("reviewer-01")
                    .safeMessage("Fix alignment")
                    .safeReasonCode("QUALITY")
                    .build();

            AdminReviewStateSummary summary = AdminReviewDtoMapper.toStateSummary(entity);
            assertThat(summary.getTargetType()).isEqualTo(ReviewTargetType.STORYBOARD);
            assertThat(summary.getCurrentState()).isEqualTo(ReviewState.CHANGES_REQUESTED);
            assertThat(summary.getLastActionType()).isEqualTo(ReviewActionType.REQUEST_CHANGES);
        }
    }

    // ── 4. Audit Timeline DTO Safety ────────────────────────────────

    @Nested
    @DisplayName("4. Audit Timeline DTO Safety")
    class TimelineDtoSafety {

        @Test
        @DisplayName("No forbidden fields")
        void noForbiddenFields() {
            Set<String> fields = getDeclaredFieldNames(AdminReviewAuditTimelineItem.class);
            for (String f : FORBIDDEN_FIELDS) {
                assertThat(fields).as("Forbidden: " + f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("toString safe — note content hidden")
        void toStringSafe() {
            AdminReviewAuditTimelineItem item = AdminReviewAuditTimelineItem.builder()
                    .eventType(ReviewAuditEventType.REVIEW_REJECTED)
                    .fromState(ReviewState.IN_REVIEW)
                    .toState(ReviewState.REJECTED)
                    .actionType(ReviewActionType.REJECT)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .sanitizedNote("Detailed rejection note with sensitive content")
                    .build();
            String str = item.toString();
            assertThat(str).contains("hasNote=true");
            assertThat(str).doesNotContain("Detailed rejection");
            assertThat(str).doesNotContain("https://");
        }

        @Test
        @DisplayName("Forbidden audit event types do not exist")
        void forbiddenAuditEvents() {
            Set<String> names = Arrays.stream(ReviewAuditEventType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain("PUBLISHED", "EXPORTED", "COMPLETED",
                    "READY_FOR_PUBLISH", "PUBLIC_URL_CREATED", "SIGNED_URL_CREATED", "AUTO_APPROVED");
        }

        @Test
        @DisplayName("Maps ReviewAuditEventEntity to safe timeline item")
        void mapperProducesSafeItem() {
            ReviewAuditEventEntity entity = ReviewAuditEventEntity.builder()
                    .reviewRecordId(UUID.randomUUID())
                    .eventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                    .fromState(ReviewState.DRAFT)
                    .toState(ReviewState.NEEDS_REVIEW)
                    .actionType(ReviewActionType.SUBMIT_FOR_REVIEW)
                    .actorRole(ReviewActorRole.CREATOR)
                    .actorReference("creator-01")
                    .occurredAt(LocalDateTime.now())
                    .safeMessage("Submitted")
                    .sanitizedNote("Initial submission note")
                    .build();

            AdminReviewAuditTimelineItem item = AdminReviewDtoMapper.toTimelineItem(entity);
            assertThat(item.getEventType()).isEqualTo(ReviewAuditEventType.REVIEW_SUBMITTED);
            assertThat(item.getFromState()).isEqualTo(ReviewState.DRAFT);
            assertThat(item.getToState()).isEqualTo(ReviewState.NEEDS_REVIEW);
            assertThat(item.getSanitizedNote()).isEqualTo("Initial submission note");
        }
    }

    // ── 5. Mapper Behavior ──────────────────────────────────────────

    @Nested
    @DisplayName("5. Mapper Behavior")
    class MapperBehavior {

        @Test
        @DisplayName("ReviewApplicationResult maps to AdminReviewActionResponse")
        void resultToResponse() {
            ReviewApplicationResult result = ReviewApplicationResult.builder()
                    .targetType(ReviewTargetType.CONTENT_PROJECT)
                    .targetId(UUID.randomUUID())
                    .previousState(ReviewState.IN_REVIEW)
                    .currentState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .actionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .auditEventType(ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED)
                    .changed(true)
                    .safeMessage("Approved internally")
                    .safeReasonCode("QUALITY_OK")
                    .occurredAt(LocalDateTime.now())
                    .build();

            AdminReviewActionResponse response = AdminReviewDtoMapper.toActionResponse(result);
            assertThat(response.getTargetType()).isEqualTo(ReviewTargetType.CONTENT_PROJECT);
            assertThat(response.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(response.getAuditEventType()).isEqualTo(ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED);
            assertThat(response.isChanged()).isTrue();
        }

        @Test
        @DisplayName("Notes/messages/reasons are sanitized through mapper")
        void sanitization() {
            ReviewAuditEventEntity entity = ReviewAuditEventEntity.builder()
                    .reviewRecordId(UUID.randomUUID())
                    .eventType(ReviewAuditEventType.REVIEW_REJECTED)
                    .fromState(ReviewState.IN_REVIEW)
                    .toState(ReviewState.REJECTED)
                    .actionType(ReviewActionType.REJECT)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .occurredAt(LocalDateTime.now())
                    .safeReasonCode("BAD!@#$")
                    .safeMessage("  rejected\u0000  ")
                    .sanitizedNote("  note\u0000 text  ")
                    .build();

            AdminReviewAuditTimelineItem item = AdminReviewDtoMapper.toTimelineItem(entity);
            assertThat(item.getSafeReasonCode()).isEqualTo("BAD");
            assertThat(item.getSafeMessage()).isEqualTo("rejected");
            assertThat(item.getSanitizedNote()).isEqualTo("note text");
        }

        @Test
        @DisplayName("No public/signed URLs generated by mapper")
        void noUrlGeneration() {
            ReviewRecordEntity entity = ReviewRecordEntity.builder()
                    .reviewTargetType(ReviewTargetType.IMAGE_ASSET)
                    .reviewTargetId(UUID.randomUUID())
                    .currentState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .safeMessage("Approved")
                    .build();

            AdminReviewStateSummary summary = AdminReviewDtoMapper.toStateSummary(entity);
            Set<String> fields = getDeclaredFieldNames(AdminReviewStateSummary.class);
            assertThat(fields).doesNotContain("publicUrl");
            assertThat(fields).doesNotContain("signedUrl");
            assertThat(fields).doesNotContain("cdnUrl");
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE preserved as internal only through mapper")
        void approvedInternalOnlyMapper() {
            ReviewApplicationResult result = ReviewApplicationResult.builder()
                    .targetType(ReviewTargetType.SCENARIO)
                    .targetId(UUID.randomUUID())
                    .previousState(ReviewState.IN_REVIEW)
                    .currentState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .actionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .auditEventType(ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED)
                    .changed(true)
                    .occurredAt(LocalDateTime.now())
                    .build();

            AdminReviewActionResponse resp = AdminReviewDtoMapper.toActionResponse(result);
            assertThat(resp.getCurrentState().name()).doesNotContain("PUBLISH");
            assertThat(resp.getCurrentState().name()).doesNotContain("EXPORT");
        }
    }

    // ── 6. Boundary Regression ──────────────────────────────────────

    @Nested
    @DisplayName("6. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("No controller annotations on DTOs or mapper")
        void noControllerAnnotations() {
            Class<?>[] classes = {
                    AdminReviewActionRequest.class,
                    AdminReviewActionResponse.class,
                    AdminReviewStateSummary.class,
                    AdminReviewAuditTimelineItem.class,
                    AdminReviewDtoMapper.class
            };
            for (Class<?> clazz : classes) {
                assertThat(clazz.isAnnotationPresent(
                        org.springframework.web.bind.annotation.RestController.class))
                        .as(clazz.getSimpleName() + " @RestController").isFalse();
                assertThat(clazz.isAnnotationPresent(
                        org.springframework.stereotype.Controller.class))
                        .as(clazz.getSimpleName() + " @Controller").isFalse();
                assertThat(clazz.isAnnotationPresent(
                        org.springframework.web.bind.annotation.RequestMapping.class))
                        .as(clazz.getSimpleName() + " @RequestMapping").isFalse();
            }
        }

        @Test
        @DisplayName("No JPA annotations on DTOs or mapper")
        void noJpaAnnotations() {
            Class<?>[] classes = {
                    AdminReviewActionRequest.class,
                    AdminReviewActionResponse.class,
                    AdminReviewStateSummary.class,
                    AdminReviewAuditTimelineItem.class,
                    AdminReviewDtoMapper.class
            };
            for (Class<?> clazz : classes) {
                assertThat(clazz.isAnnotationPresent(jakarta.persistence.Entity.class))
                        .as(clazz.getSimpleName() + " @Entity").isFalse();
            }
        }

        @Test
        @DisplayName("No publish/export in any review enum")
        void noPublishExportEnums() {
            Set<String> allNames = new java.util.HashSet<>();
            Arrays.stream(ReviewState.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActionType.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewAuditEventType.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActorRole.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewTargetType.values()).forEach(v -> allNames.add(v.name()));

            for (String name : allNames) {
                assertThat(name).doesNotContain("PUBLISH");
                assertThat(name).doesNotContain("EXPORT");
                assertThat(name).doesNotContain("CDN");
                assertThat(name).doesNotContain("COMPLETED");
            }
        }

        @Test
        @DisplayName("ReviewApplicationService still has no @RestController")
        void serviceNotController() {
            assertThat(ReviewApplicationService.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
        }

        @Test
        @DisplayName("ReviewDomainService still has no Spring annotations")
        void domainServiceNoSpring() {
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isFalse();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private static Set<String> getDeclaredFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());
    }
}
