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
 * FP-012 Task 04: Persists current internal review state for a reviewable item.
 *
 * <p>No provider URL, storage key, bucket/path, raw payload, base64, secret,
 * public/signed URL, or publish/export fields.</p>
 *
 * <p>APPROVED_FOR_INTERNAL_USE is NOT publish/export approval.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_records")
public class ReviewRecordEntity extends BaseEntity {

    /** Type of content under review. */
    @Enumerated(EnumType.STRING)
    @Column(name = "review_target_type", nullable = false, length = 50)
    private ReviewTargetType reviewTargetType;

    /** ID of the target item under review. */
    @Column(name = "review_target_id", nullable = false)
    private UUID reviewTargetId;

    /** Current review state. */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 50)
    private ReviewState currentState;

    /** Actor who created this review record. */
    @Column(name = "created_by_actor_reference", length = 100)
    private String createdByActorReference;

    /** Current reviewer reference, nullable until claimed. */
    @Column(name = "current_reviewer_actor_reference", length = 100)
    private String currentReviewerActorReference;

    /** Last action type performed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_action_type", length = 50)
    private ReviewActionType lastActionType;

    /** Timestamp of last action. */
    @Column(name = "last_action_at")
    private LocalDateTime lastActionAt;

    /** Actor who performed the last action. */
    @Column(name = "last_action_by_actor_reference", length = 100)
    private String lastActionByActorReference;

    /** Safe reason code for last action. */
    @Column(name = "safe_reason_code", length = 100)
    private String safeReasonCode;

    /** Safe message for last action. */
    @Column(name = "safe_message", length = 2000)
    private String safeMessage;

    /**
     * Safe toString — no secrets, provider URLs, storage keys, raw payloads, or base64.
     */
    @Override
    public String toString() {
        return "ReviewRecordEntity{" +
                "id=" + getId() +
                ", reviewTargetType=" + reviewTargetType +
                ", reviewTargetId=" + reviewTargetId +
                ", currentState=" + currentState +
                ", lastActionType=" + lastActionType +
                '}';
    }
}
