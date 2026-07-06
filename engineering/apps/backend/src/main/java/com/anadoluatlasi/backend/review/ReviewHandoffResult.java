package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * FP-013 Task 02: Safe immutable result of a review handoff operation.
 *
 * <p>No public/signed URL, provider/storage/config/secret, publish/export fields.</p>
 */
@Getter
@Builder
public class ReviewHandoffResult {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final ReviewState previousState;
    private final ReviewState currentState;
    private final boolean created;
    private final boolean updated;
    private final boolean preserved;
    private final ReviewAuditEventType auditEventType;
    private final String safeReasonCode;
    private final String safeMessage;

    @Override
    public String toString() {
        return "ReviewHandoffResult{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", previousState=" + previousState +
                ", currentState=" + currentState +
                ", created=" + created +
                ", updated=" + updated +
                ", preserved=" + preserved +
                '}';
    }
}
