package com.anadoluatlasi.backend.review;

import java.util.UUID;

/**
 * FP-015 Task 02: Safe immutable request for internal revision.
 *
 * <p>Narrow request — NOT a generic status update.</p>
 *
 * <p>Does NOT contain:</p>
 * <ul>
 *   <li>publicUrl / signedUrl / cdnUrl</li>
 *   <li>providerUrl / storageKey / bucket / objectPath</li>
 *   <li>rawPayload / base64 / bytes</li>
 *   <li>secret / apiKey / accessKey / token</li>
 *   <li>publishStatus / exportStatus</li>
 *   <li>stackTrace / exceptionClass</li>
 * </ul>
 */
public final class InternalRevisionRequest {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final ReviewActorRole actorRole;
    private final String actorReference;
    private final String safeReasonCode;
    private final String safeMessage;

    private InternalRevisionRequest(Builder builder) {
        if (builder.targetType == null) {
            throw new IllegalArgumentException("targetType must not be null");
        }
        if (builder.targetId == null) {
            throw new IllegalArgumentException("targetId must not be null");
        }
        if (builder.actorRole == null) {
            throw new IllegalArgumentException("actorRole must not be null");
        }
        if (builder.actorReference == null || builder.actorReference.isBlank()) {
            throw new IllegalArgumentException("actorReference must not be blank");
        }
        if (builder.safeMessage == null || builder.safeMessage.isBlank()) {
            throw new IllegalArgumentException("safeMessage must not be blank");
        }
        this.targetType = builder.targetType;
        this.targetId = builder.targetId;
        this.actorRole = builder.actorRole;
        this.actorReference = builder.actorReference;
        this.safeReasonCode = ReviewTextSafety.sanitizeReasonCode(builder.safeReasonCode);
        this.safeMessage = ReviewTextSafety.sanitize(builder.safeMessage);
    }

    public ReviewTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public ReviewActorRole getActorRole() { return actorRole; }
    public String getActorReference() { return actorReference; }
    public String getSafeReasonCode() { return safeReasonCode; }
    public String getSafeMessage() { return safeMessage; }

    /**
     * Safe toString — no secrets, no URLs, no storage keys.
     */
    @Override
    public String toString() {
        return "InternalRevisionRequest{targetType=" + targetType
                + ", targetId=" + targetId
                + ", actorRole=" + actorRole
                + ", safeReasonCode=" + safeReasonCode
                + '}';
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ReviewTargetType targetType;
        private UUID targetId;
        private ReviewActorRole actorRole;
        private String actorReference;
        private String safeReasonCode;
        private String safeMessage;

        private Builder() {}

        public Builder targetType(ReviewTargetType val) { targetType = val; return this; }
        public Builder targetId(UUID val) { targetId = val; return this; }
        public Builder actorRole(ReviewActorRole val) { actorRole = val; return this; }
        public Builder actorReference(String val) { actorReference = val; return this; }
        public Builder safeReasonCode(String val) { safeReasonCode = val; return this; }
        public Builder safeMessage(String val) { safeMessage = val; return this; }

        public InternalRevisionRequest build() { return new InternalRevisionRequest(this); }
    }
}
