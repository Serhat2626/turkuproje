package com.anadoluatlasi.backend.review;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-015 Task 03: Safe admin response DTO for internal revision request endpoint.
 *
 * <p>Does NOT contain:</p>
 * <ul>
 *   <li>publicUrl / signedUrl / cdnUrl</li>
 *   <li>providerUrl / storageKey / bucket / objectPath</li>
 *   <li>rawPayload / base64 / bytes</li>
 *   <li>secret / apiKey / accessKey / token</li>
 *   <li>publishStatus / exportStatus / publishedAt / exportedAt</li>
 *   <li>stackTrace / exceptionClass</li>
 *   <li>providerResponse / storageResponse / rawError / html</li>
 * </ul>
 */
public final class AdminInternalRevisionResponse {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final ReviewState previousState;
    private final ReviewState currentState;
    private final String actionApplied;
    private final String safeReasonCode;
    private final String safeMessage;
    private final boolean appendedNote;
    private final boolean auditRecorded;
    private final LocalDateTime updatedAt;

    private AdminInternalRevisionResponse(Builder builder) {
        this.targetType = builder.targetType;
        this.targetId = builder.targetId;
        this.previousState = builder.previousState;
        this.currentState = builder.currentState;
        this.actionApplied = builder.actionApplied;
        this.safeReasonCode = builder.safeReasonCode;
        this.safeMessage = builder.safeMessage;
        this.appendedNote = builder.appendedNote;
        this.auditRecorded = builder.auditRecorded;
        this.updatedAt = builder.updatedAt;
    }

    public ReviewTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public ReviewState getPreviousState() { return previousState; }
    public ReviewState getCurrentState() { return currentState; }
    public String getActionApplied() { return actionApplied; }
    public String getSafeReasonCode() { return safeReasonCode; }
    public String getSafeMessage() { return safeMessage; }
    public boolean isAppendedNote() { return appendedNote; }
    public boolean isAuditRecorded() { return auditRecorded; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * Safe toString — no secrets, no URLs, no storage keys.
     */
    @Override
    public String toString() {
        return "AdminInternalRevisionResponse{targetType=" + targetType
                + ", targetId=" + targetId
                + ", previousState=" + previousState
                + ", currentState=" + currentState
                + ", actionApplied=" + actionApplied
                + ", appendedNote=" + appendedNote
                + ", auditRecorded=" + auditRecorded
                + '}';
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ReviewTargetType targetType;
        private UUID targetId;
        private ReviewState previousState;
        private ReviewState currentState;
        private String actionApplied;
        private String safeReasonCode;
        private String safeMessage;
        private boolean appendedNote;
        private boolean auditRecorded;
        private LocalDateTime updatedAt;

        private Builder() {}

        public Builder targetType(ReviewTargetType val) { targetType = val; return this; }
        public Builder targetId(UUID val) { targetId = val; return this; }
        public Builder previousState(ReviewState val) { previousState = val; return this; }
        public Builder currentState(ReviewState val) { currentState = val; return this; }
        public Builder actionApplied(String val) { actionApplied = val; return this; }
        public Builder safeReasonCode(String val) { safeReasonCode = val; return this; }
        public Builder safeMessage(String val) { safeMessage = val; return this; }
        public Builder appendedNote(boolean val) { appendedNote = val; return this; }
        public Builder auditRecorded(boolean val) { auditRecorded = val; return this; }
        public Builder updatedAt(LocalDateTime val) { updatedAt = val; return this; }

        public AdminInternalRevisionResponse build() { return new AdminInternalRevisionResponse(this); }
    }
}
