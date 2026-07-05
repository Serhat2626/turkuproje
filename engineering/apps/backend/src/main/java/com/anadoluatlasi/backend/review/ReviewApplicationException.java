package com.anadoluatlasi.backend.review;

/**
 * FP-012 Task 05: Safe application-level exception for review operations.
 *
 * <p>Messages are safe — no secrets, provider URLs, storage keys,
 * raw payloads, base64, or stack traces in user/admin-facing context.</p>
 */
public class ReviewApplicationException extends RuntimeException {

    private final ReviewTargetType targetType;
    private final java.util.UUID targetId;

    public ReviewApplicationException(String safeMessage) {
        super(safeMessage);
        this.targetType = null;
        this.targetId = null;
    }

    public ReviewApplicationException(String safeMessage, ReviewTargetType targetType, java.util.UUID targetId) {
        super(safeMessage);
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public ReviewTargetType getTargetType() { return targetType; }
    public java.util.UUID getTargetId() { return targetId; }
}
