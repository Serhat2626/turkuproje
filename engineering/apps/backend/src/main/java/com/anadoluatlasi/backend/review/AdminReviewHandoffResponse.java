package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * FP-013 Task 04: Safe immutable handoff response DTO.
 *
 * <p>No provider URL, storage key, bucket/path, raw payload, base64, secret,
 * public/signed URL, publish/export field.</p>
 */
@Getter
@Builder
public class AdminReviewHandoffResponse {

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
        return "AdminReviewHandoffResponse{" +
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
