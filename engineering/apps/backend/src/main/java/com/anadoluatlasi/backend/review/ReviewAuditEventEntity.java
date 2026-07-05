package com.anadoluatlasi.backend.review;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-012 Task 04: Persists safe audit events for review actions.
 *
 * <p>No provider URL, storage key, bucket/path, raw payload, base64, secret,
 * public/signed URL, or publish/export events.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_audit_events")
public class ReviewAuditEventEntity extends BaseEntity {

    /** Reference to the parent review record. */
    @Column(name = "review_record_id", nullable = false)
    private UUID reviewRecordId;

    /** Audit event type. */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ReviewAuditEventType eventType;

    /** State before this action. */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 50)
    private ReviewState fromState;

    /** State after this action. */
    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", length = 50)
    private ReviewState toState;

    /** Action type performed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50)
    private ReviewActionType actionType;

    /** Actor role. */
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", length = 30)
    private ReviewActorRole actorRole;

    /** Safe opaque actor reference. */
    @Column(name = "actor_reference", length = 100)
    private String actorReference;

    /** When the action occurred. */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** Safe reason code. */
    @Column(name = "safe_reason_code", length = 100)
    private String safeReasonCode;

    /** Safe message. */
    @Column(name = "safe_message", length = 2000)
    private String safeMessage;

    /** Sanitized, length-limited note. */
    @Column(name = "sanitized_note", length = 2000)
    private String sanitizedNote;

    /**
     * Safe toString — no secrets, provider URLs, storage keys, raw payloads, or base64.
     */
    @Override
    public String toString() {
        return "ReviewAuditEventEntity{" +
                "id=" + getId() +
                ", reviewRecordId=" + reviewRecordId +
                ", eventType=" + eventType +
                ", fromState=" + fromState +
                ", toState=" + toState +
                ", actionType=" + actionType +
                ", actorRole=" + actorRole +
                '}';
    }
}
