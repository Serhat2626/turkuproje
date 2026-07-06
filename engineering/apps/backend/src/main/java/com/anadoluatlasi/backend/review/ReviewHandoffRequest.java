package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * FP-013 Task 02: Immutable request to hand off a generated output into the review workflow.
 *
 * <p>No JPA annotations. No controller/DTO annotations.
 * No provider URL, storage key, bucket, raw payload, base64, secret, public/signed URL,
 * publish/export field.</p>
 *
 * <p>All text fields sanitized via {@link ReviewTextSafety} on build.</p>
 */
@Getter
@Builder
public class ReviewHandoffRequest {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final ReviewHandoffSource source;
    private final ReviewActorRole actorRole;
    private final String actorReference;
    private final String safeTitle;
    private final String safeSummary;
    private final String safeReasonCode;
    private final String safeMessage;

    /**
     * Factory method with text sanitization applied.
     */
    public static ReviewHandoffRequest createSafe(
            ReviewTargetType targetType,
            UUID targetId,
            ReviewHandoffSource source,
            ReviewActorRole actorRole,
            String actorReference,
            String title,
            String summary) {
        return ReviewHandoffRequest.builder()
                .targetType(targetType)
                .targetId(targetId)
                .source(source)
                .actorRole(actorRole)
                .actorReference(actorReference)
                .safeTitle(ReviewTextSafety.sanitize(title))
                .safeSummary(ReviewTextSafety.sanitize(summary))
                .build();
    }

    @Override
    public String toString() {
        return "ReviewHandoffRequest{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", source=" + source +
                ", actorRole=" + actorRole +
                '}';
    }
}
