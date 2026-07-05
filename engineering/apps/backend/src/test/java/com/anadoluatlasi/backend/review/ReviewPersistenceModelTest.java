package com.anadoluatlasi.backend.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * FP-012 Task 04 — Review Persistence Model, Migration Planning & Repository Safety.
 */
@DisplayName("FP-012 Task 04 — Review Persistence Model & Migration Safety")
class ReviewPersistenceModelTest {

    private static final Set<String> FORBIDDEN_FIELD_NAMES = Set.of(
            "providerUrl", "storageKey", "bucketName", "bucketPath",
            "rawPayload", "rawProviderPayload", "rawStoragePayload",
            "base64", "secret", "apiKey", "config",
            "publicUrl", "signedUrl", "publishUrl", "exportUrl", "stackTrace"
    );

    // ── 1. Entity Model Safety ──────────────────────────────────────

    @Nested
    @DisplayName("1. Entity Model Safety")
    class EntityModelSafety {

        @Test
        @DisplayName("ReviewRecordEntity has expected safe fields")
        void recordEntitySafeFields() {
            Set<String> fields = getDeclaredFieldNames(ReviewRecordEntity.class);
            assertThat(fields).contains(
                    "reviewTargetType", "reviewTargetId", "currentState",
                    "createdByActorReference", "currentReviewerActorReference",
                    "lastActionType", "lastActionAt", "lastActionByActorReference",
                    "safeReasonCode", "safeMessage"
            );
        }

        @Test
        @DisplayName("ReviewAuditEventEntity has expected safe fields")
        void auditEntitySafeFields() {
            Set<String> fields = getDeclaredFieldNames(ReviewAuditEventEntity.class);
            assertThat(fields).contains(
                    "reviewRecordId", "eventType", "fromState", "toState",
                    "actionType", "actorRole", "actorReference",
                    "occurredAt", "safeReasonCode", "safeMessage", "sanitizedNote"
            );
        }

        @Test
        @DisplayName("ReviewRecordEntity does not contain forbidden fields")
        void recordEntityNoForbiddenFields() {
            Set<String> fields = getDeclaredFieldNames(ReviewRecordEntity.class);
            for (String f : FORBIDDEN_FIELD_NAMES) {
                assertThat(fields).as("Forbidden: " + f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("ReviewAuditEventEntity does not contain forbidden fields")
        void auditEntityNoForbiddenFields() {
            Set<String> fields = getDeclaredFieldNames(ReviewAuditEventEntity.class);
            for (String f : FORBIDDEN_FIELD_NAMES) {
                assertThat(fields).as("Forbidden: " + f).doesNotContain(f);
            }
        }

        @Test
        @DisplayName("ReviewRecordEntity is a JPA entity")
        void recordIsEntity() {
            assertThat(ReviewRecordEntity.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isTrue();
            assertThat(ReviewRecordEntity.class.isAnnotationPresent(
                    jakarta.persistence.Table.class)).isTrue();
        }

        @Test
        @DisplayName("ReviewAuditEventEntity is a JPA entity")
        void auditIsEntity() {
            assertThat(ReviewAuditEventEntity.class.isAnnotationPresent(
                    jakarta.persistence.Entity.class)).isTrue();
            assertThat(ReviewAuditEventEntity.class.isAnnotationPresent(
                    jakarta.persistence.Table.class)).isTrue();
        }
    }

    // ── 2. Enum/Status Safety ───────────────────────────────────────

    @Nested
    @DisplayName("2. Enum/Status Safety")
    class EnumStatusSafety {

        @Test
        @DisplayName("ReviewState still has only safe states")
        void safeStates() {
            assertThat(ReviewState.values()).hasSize(7);
            Set<String> names = Arrays.stream(ReviewState.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain(
                    "READY_FOR_PUBLISH", "COMPLETED", "PUBLISHED", "EXPORTED");
        }

        @Test
        @DisplayName("ReviewActionType still has only safe actions")
        void safeActions() {
            Set<String> names = Arrays.stream(ReviewActionType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain(
                    "PUBLISH", "EXPORT", "MARK_COMPLETED",
                    "AUTO_APPROVE", "GENERIC_STATUS_UPDATE");
        }

        @Test
        @DisplayName("ReviewAuditEventType still has only safe events")
        void safeEvents() {
            Set<String> names = Arrays.stream(ReviewAuditEventType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain(
                    "PUBLISHED", "EXPORTED", "COMPLETED", "READY_FOR_PUBLISH",
                    "PUBLIC_URL_CREATED", "SIGNED_URL_CREATED", "AUTO_APPROVED");
        }

        @Test
        @DisplayName("ReviewTargetType has only safe target types")
        void safeTargets() {
            assertThat(ReviewTargetType.values()).containsExactlyInAnyOrder(
                    ReviewTargetType.CONTENT_PROJECT,
                    ReviewTargetType.SCENARIO,
                    ReviewTargetType.STORYBOARD,
                    ReviewTargetType.PACKAGE,
                    ReviewTargetType.IMAGE_ASSET
            );
            Set<String> names = Arrays.stream(ReviewTargetType.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(names).doesNotContain(
                    "PUBLIC_MEDIA", "CDN_ASSET", "EXPORT_FILE", "PUBLISHED_CONTENT");
        }
    }

    // ── 3. Mapper Safety ────────────────────────────────────────────

    @Nested
    @DisplayName("3. Mapper Safety")
    class MapperSafety {

        @Test
        @DisplayName("applyDecisionToRecord creates safe record updates")
        void applyDecision() {
            ReviewRecordEntity entity = ReviewRecordEntity.builder()
                    .reviewTargetType(ReviewTargetType.CONTENT_PROJECT)
                    .reviewTargetId(UUID.randomUUID())
                    .currentState(ReviewState.IN_REVIEW)
                    .build();

            ReviewAuditRecord audit = ReviewAuditRecord.builder()
                    .eventType(ReviewAuditEventType.INTERNAL_APPROVAL_GRANTED)
                    .fromState(ReviewState.IN_REVIEW)
                    .toState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .actionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .actorReference("reviewer-42")
                    .occurredAt(Instant.now())
                    .build();

            ReviewDecisionResult result = ReviewDecisionResult.builder()
                    .previousState(ReviewState.IN_REVIEW)
                    .newState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .actionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .changed(true)
                    .auditRecord(audit)
                    .safeMessage("Approved for internal use")
                    .safeReasonCode("QUALITY_OK")
                    .build();

            ReviewPersistenceMapper.applyDecisionToRecord(result, entity);

            assertThat(entity.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
            assertThat(entity.getLastActionType()).isEqualTo(ReviewActionType.APPROVE_FOR_INTERNAL_USE);
            assertThat(entity.getLastActionByActorReference()).isEqualTo("reviewer-42");
            assertThat(entity.getSafeReasonCode()).isEqualTo("QUALITY_OK");
        }

        @Test
        @DisplayName("toAuditEventEntity creates safe audit event")
        void toAuditEvent() {
            UUID recordId = UUID.randomUUID();
            ReviewAuditRecord audit = ReviewAuditRecord.builder()
                    .eventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                    .fromState(ReviewState.DRAFT)
                    .toState(ReviewState.NEEDS_REVIEW)
                    .actionType(ReviewActionType.SUBMIT_FOR_REVIEW)
                    .actorRole(ReviewActorRole.CREATOR)
                    .actorReference("creator-01")
                    .occurredAt(Instant.now())
                    .safeReasonCode("INITIAL_SUBMIT")
                    .safeMessage("Content submitted for review")
                    .build();

            ReviewAuditEventEntity entity = ReviewPersistenceMapper.toAuditEventEntity(audit, recordId);

            assertThat(entity.getReviewRecordId()).isEqualTo(recordId);
            assertThat(entity.getEventType()).isEqualTo(ReviewAuditEventType.REVIEW_SUBMITTED);
            assertThat(entity.getFromState()).isEqualTo(ReviewState.DRAFT);
            assertThat(entity.getToState()).isEqualTo(ReviewState.NEEDS_REVIEW);
            assertThat(entity.getActorReference()).isEqualTo("creator-01");
            assertThat(entity.getOccurredAt()).isNotNull();
        }

        @Test
        @DisplayName("Messages/reasons are sanitized through mapper")
        void sanitization() {
            UUID recordId = UUID.randomUUID();
            ReviewAuditRecord audit = ReviewAuditRecord.builder()
                    .eventType(ReviewAuditEventType.REVIEW_REJECTED)
                    .fromState(ReviewState.IN_REVIEW)
                    .toState(ReviewState.REJECTED)
                    .actionType(ReviewActionType.REJECT)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .occurredAt(Instant.now())
                    .safeReasonCode("BAD!@#$")
                    .safeMessage("  rejected\u0000  ")
                    .build();

            ReviewAuditEventEntity entity = ReviewPersistenceMapper.toAuditEventEntity(audit, recordId);
            assertThat(entity.getSafeReasonCode()).isEqualTo("BAD");
            assertThat(entity.getSafeMessage()).isEqualTo("rejected");
        }

        @Test
        @DisplayName("APPROVED_FOR_INTERNAL_USE remains internal only")
        void approvedInternalOnly() {
            ReviewRecordEntity entity = ReviewRecordEntity.builder()
                    .reviewTargetType(ReviewTargetType.SCENARIO)
                    .reviewTargetId(UUID.randomUUID())
                    .currentState(ReviewState.IN_REVIEW)
                    .build();

            ReviewDecisionResult result = ReviewDecisionResult.builder()
                    .previousState(ReviewState.IN_REVIEW)
                    .newState(ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .actionType(ReviewActionType.APPROVE_FOR_INTERNAL_USE)
                    .actorRole(ReviewActorRole.REVIEWER)
                    .changed(true)
                    .safeMessage("Internal approval")
                    .build();

            ReviewPersistenceMapper.applyDecisionToRecord(result, entity);

            assertThat(entity.getCurrentState().name()).doesNotContain("PUBLISH");
            assertThat(entity.getCurrentState().name()).doesNotContain("EXPORT");
            assertThat(entity.getCurrentState()).isEqualTo(ReviewState.APPROVED_FOR_INTERNAL_USE);
        }

        @Test
        @DisplayName("createNewRecord creates safe initial record")
        void createNewRecord() {
            UUID targetId = UUID.randomUUID();
            ReviewRecordEntity entity = ReviewPersistenceMapper.createNewRecord(
                    ReviewTargetType.IMAGE_ASSET, targetId,
                    ReviewState.DRAFT, "creator-ref");

            assertThat(entity.getReviewTargetType()).isEqualTo(ReviewTargetType.IMAGE_ASSET);
            assertThat(entity.getReviewTargetId()).isEqualTo(targetId);
            assertThat(entity.getCurrentState()).isEqualTo(ReviewState.DRAFT);
            assertThat(entity.getCreatedByActorReference()).isEqualTo("creator-ref");
        }
    }

    // ── 4. Migration Safety ─────────────────────────────────────────

    @Nested
    @DisplayName("4. Migration Safety")
    class MigrationSafety {

        private static final Path MIGRATION_PATH = Path.of(
                "src/main/resources/db/migration/V10__fp012_review_workflow_tables.sql");

        @Test
        @DisplayName("Migration file exists")
        void migrationExists() {
            assertThat(MIGRATION_PATH.toFile()).exists();
        }

        @Test
        @DisplayName("Migration creates review_records table")
        void createsReviewRecords() throws IOException {
            String content = Files.readString(MIGRATION_PATH);
            assertThat(content).contains("CREATE TABLE IF NOT EXISTS review_records");
        }

        @Test
        @DisplayName("Migration creates review_audit_events table")
        void createsAuditEvents() throws IOException {
            String content = Files.readString(MIGRATION_PATH);
            assertThat(content).contains("CREATE TABLE IF NOT EXISTS review_audit_events");
        }

        @Test
        @DisplayName("Migration contains no forbidden column names in SQL statements")
        void noForbiddenColumns() throws IOException {
            // Filter out comment lines — comments document what is NOT present
            String content = Files.readString(MIGRATION_PATH).lines()
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"))
                    .toLowerCase();
            assertThat(content).doesNotContain("provider_url");
            assertThat(content).doesNotContain("storage_key");
            assertThat(content).doesNotContain("storage_object_key");
            assertThat(content).doesNotContain("raw_payload");
            assertThat(content).doesNotContain("raw_provider");
            assertThat(content).doesNotContain("raw_storage");
            assertThat(content).doesNotContain("base64");
            assertThat(content).doesNotContain("api_key");
            assertThat(content).doesNotContain("secret_key");
            assertThat(content).doesNotContain("access_key");
            assertThat(content).doesNotContain("public_url");
            assertThat(content).doesNotContain("signed_url");
            assertThat(content).doesNotContain("publish_url");
            assertThat(content).doesNotContain("export_url");
            assertThat(content).doesNotContain("cdn_url");
            assertThat(content).doesNotContain("stack_trace");
        }

        @Test
        @DisplayName("Migration contains no publish/export status values as defaults")
        void noPublishExportDefaults() throws IOException {
            String content = Files.readString(MIGRATION_PATH).toUpperCase();
            // These should not appear as DEFAULT or CHECK values
            assertThat(content).doesNotContain("'READY_FOR_PUBLISH'");
            assertThat(content).doesNotContain("'COMPLETED'");
            assertThat(content).doesNotContain("'PUBLISHED'");
            assertThat(content).doesNotContain("'EXPORTED'");
        }

        @Test
        @DisplayName("Migration includes useful indexes")
        void hasIndexes() throws IOException {
            String content = Files.readString(MIGRATION_PATH);
            assertThat(content).contains("idx_rr_target_type_id");
            assertThat(content).contains("idx_rr_current_state");
            assertThat(content).contains("idx_rr_reviewer");
            assertThat(content).contains("idx_rae_review_record");
            assertThat(content).contains("idx_rae_occurred_at");
            assertThat(content).contains("idx_rae_record_time");
        }

        @Test
        @DisplayName("Migration includes FK from audit to review_records")
        void hasForeignKey() throws IOException {
            String content = Files.readString(MIGRATION_PATH);
            assertThat(content).contains("fk_rae_review_record");
            assertThat(content).contains("REFERENCES review_records(id)");
        }
    }

    // ── 5. Repository Boundary ──────────────────────────────────────

    @Nested
    @DisplayName("5. Repository Boundary")
    class RepositoryBoundary {

        @Test
        @DisplayName("ReviewRecordRepository interface exists")
        void recordRepoExists() {
            assertThat(ReviewRecordRepository.class).isInterface();
            assertThat(ReviewRecordRepository.class.isAnnotationPresent(
                    org.springframework.stereotype.Repository.class)).isTrue();
        }

        @Test
        @DisplayName("ReviewAuditEventRepository interface exists")
        void auditRepoExists() {
            assertThat(ReviewAuditEventRepository.class).isInterface();
            assertThat(ReviewAuditEventRepository.class.isAnnotationPresent(
                    org.springframework.stereotype.Repository.class)).isTrue();
        }

        @Test
        @DisplayName("No controller annotation on repository classes")
        void noControllerOnRepo() {
            assertThat(ReviewRecordRepository.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
            assertThat(ReviewAuditEventRepository.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
        }
    }

    // ── 6. toString Safety ──────────────────────────────────────────

    @Nested
    @DisplayName("6. toString Safety")
    class ToStringSafety {

        @Test
        @DisplayName("ReviewRecordEntity toString is safe")
        void recordToStringSafe() {
            ReviewRecordEntity entity = ReviewRecordEntity.builder()
                    .reviewTargetType(ReviewTargetType.CONTENT_PROJECT)
                    .reviewTargetId(UUID.randomUUID())
                    .currentState(ReviewState.IN_REVIEW)
                    .lastActionType(ReviewActionType.START_REVIEW)
                    .safeMessage("Review in progress")
                    .build();
            String str = entity.toString();
            assertThat(str).contains("ReviewRecordEntity");
            assertThat(str).doesNotContain("Review in progress");
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secretKey");
            assertThat(str).doesNotContain("base64");
        }

        @Test
        @DisplayName("ReviewAuditEventEntity toString is safe")
        void auditToStringSafe() {
            ReviewAuditEventEntity entity = ReviewAuditEventEntity.builder()
                    .reviewRecordId(UUID.randomUUID())
                    .eventType(ReviewAuditEventType.REVIEW_SUBMITTED)
                    .fromState(ReviewState.DRAFT)
                    .toState(ReviewState.NEEDS_REVIEW)
                    .actionType(ReviewActionType.SUBMIT_FOR_REVIEW)
                    .actorRole(ReviewActorRole.CREATOR)
                    .occurredAt(LocalDateTime.now())
                    .safeMessage("Sensitive message here")
                    .sanitizedNote("Sensitive note here")
                    .build();
            String str = entity.toString();
            assertThat(str).contains("ReviewAuditEventEntity");
            assertThat(str).doesNotContain("Sensitive message here");
            assertThat(str).doesNotContain("Sensitive note here");
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("secretKey");
        }
    }

    // ── 7. Boundary Regression ──────────────────────────────────────

    @Nested
    @DisplayName("7. Boundary Regression")
    class BoundaryRegression {

        @Test
        @DisplayName("No controller annotation on persistence classes")
        void noController() {
            assertThat(ReviewPersistenceMapper.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)).isFalse();
            assertThat(ReviewPersistenceMapper.class.isAnnotationPresent(
                    org.springframework.stereotype.Controller.class)).isFalse();
        }

        @Test
        @DisplayName("No publish/export in any review enum name")
        void noPublishExportEnums() {
            Set<String> allNames = new java.util.HashSet<>();
            Arrays.stream(ReviewState.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActionType.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewActorRole.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewAuditEventType.values()).forEach(v -> allNames.add(v.name()));
            Arrays.stream(ReviewTargetType.values()).forEach(v -> allNames.add(v.name()));

            for (String name : allNames) {
                assertThat(name).doesNotContain("PUBLISH");
                assertThat(name).doesNotContain("EXPORT");
                assertThat(name).doesNotContain("CDN");
                assertThat(name).doesNotContain("SIGNED_URL");
                assertThat(name).doesNotContain("PUBLIC_URL");
                assertThat(name).doesNotContain("COMPLETED");
            }
        }

        @Test
        @DisplayName("No endpoint annotations in review package")
        void noEndpointAnnotations() {
            // Verify mapper is not exposed
            assertThat(ReviewPersistenceMapper.class.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RequestMapping.class)).isFalse();
        }

        @Test
        @DisplayName("ReviewDomainService still has no Spring annotations")
        void domainServiceNoSpring() {
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.stereotype.Service.class)).isFalse();
            assertThat(ReviewDomainService.class.isAnnotationPresent(
                    org.springframework.stereotype.Component.class)).isFalse();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private static Set<String> getDeclaredFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());
    }
}
