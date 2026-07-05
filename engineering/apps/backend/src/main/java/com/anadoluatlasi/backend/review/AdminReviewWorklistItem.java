package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-012 Task 10: Safe admin review worklist item.
 *
 * <p>No provider URL, storage key, bucket/path, raw payload, base64,
 * secret, public/signed URL, publish/export fields, stack trace, raw notes.</p>
 */
@Data
@Builder
public class AdminReviewWorklistItem {

    private ReviewTargetType targetType;
    private UUID targetId;
    private ReviewState currentState;
    private String currentReviewerActorReference;
    private ReviewActionType lastActionType;
    private LocalDateTime lastActionAt;
    private String lastActionByActorReference;
    private String safeReasonCode;
    private String safeMessage;
    private String displayTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Safe toString — no secrets, provider URLs, storage keys, raw payloads.
     */
    @Override
    public String toString() {
        return "AdminReviewWorklistItem{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", currentState=" + currentState +
                ", lastActionType=" + lastActionType +
                '}';
    }
}
