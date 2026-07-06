package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * FP-013 Task 04: Safe admin-facing handoff request DTO.
 *
 * <p>No provider URL, storage key, bucket/path, raw payload, base64, secret,
 * public/signed URL, publish/export field.</p>
 *
 * <p>All text fields sanitized through {@link ReviewTextSafety} at mapping time.</p>
 */
@Getter
@Builder
public class AdminReviewHandoffRequest {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final ReviewHandoffSource source;
    private final ReviewActorRole actorRole;
    private final String actorReference;
    private final String safeTitle;
    private final String safeSummary;
    private final String safeReasonCode;
    private final String safeMessage;

    @Override
    public String toString() {
        return "AdminReviewHandoffRequest{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", source=" + source +
                ", actorRole=" + actorRole +
                '}';
    }
}
