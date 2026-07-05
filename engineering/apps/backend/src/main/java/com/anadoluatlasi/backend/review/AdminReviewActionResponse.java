package com.anadoluatlasi.backend.review;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-012 Task 06: Safe admin review action response DTO.
 *
 * <p>No public/signed URL, no publish/export semantics, no provider URL,
 * no storage key, no raw payload, no base64, no secret/config.
 * APPROVED_FOR_INTERNAL_USE remains internal-only.</p>
 */
public final class AdminReviewActionResponse {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final ReviewState previousState;
    private final ReviewState currentState;
    private final ReviewActionType actionType;
    private final ReviewActorRole actorRole;
    private final ReviewAuditEventType auditEventType;
    private final boolean changed;
    private final String safeMessage;
    private final String safeReasonCode;
    private final LocalDateTime occurredAt;

    private AdminReviewActionResponse(Builder builder) {
        this.targetType = builder.targetType;
        this.targetId = builder.targetId;
        this.previousState = builder.previousState;
        this.currentState = builder.currentState;
        this.actionType = builder.actionType;
        this.actorRole = builder.actorRole;
        this.auditEventType = builder.auditEventType;
        this.changed = builder.changed;
        this.safeMessage = builder.safeMessage;
        this.safeReasonCode = builder.safeReasonCode;
        this.occurredAt = builder.occurredAt;
    }

    public ReviewTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public ReviewState getPreviousState() { return previousState; }
    public ReviewState getCurrentState() { return currentState; }
    public ReviewActionType getActionType() { return actionType; }
    public ReviewActorRole getActorRole() { return actorRole; }
    public ReviewAuditEventType getAuditEventType() { return auditEventType; }
    public boolean isChanged() { return changed; }
    public String getSafeMessage() { return safeMessage; }
    public String getSafeReasonCode() { return safeReasonCode; }
    public LocalDateTime getOccurredAt() { return occurredAt; }

    @Override
    public String toString() {
        return "AdminReviewActionResponse{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", previousState=" + previousState +
                ", currentState=" + currentState +
                ", actionType=" + actionType +
                ", actorRole=" + actorRole +
                ", changed=" + changed +
                ", auditEventType=" + auditEventType +
                ", safeReasonCode='" + safeReasonCode + '\'' +
                '}';
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ReviewTargetType targetType;
        private UUID targetId;
        private ReviewState previousState;
        private ReviewState currentState;
        private ReviewActionType actionType;
        private ReviewActorRole actorRole;
        private ReviewAuditEventType auditEventType;
        private boolean changed;
        private String safeMessage;
        private String safeReasonCode;
        private LocalDateTime occurredAt;

        public Builder targetType(ReviewTargetType t) { this.targetType = t; return this; }
        public Builder targetId(UUID id) { this.targetId = id; return this; }
        public Builder previousState(ReviewState s) { this.previousState = s; return this; }
        public Builder currentState(ReviewState s) { this.currentState = s; return this; }
        public Builder actionType(ReviewActionType a) { this.actionType = a; return this; }
        public Builder actorRole(ReviewActorRole r) { this.actorRole = r; return this; }
        public Builder auditEventType(ReviewAuditEventType e) { this.auditEventType = e; return this; }
        public Builder changed(boolean c) { this.changed = c; return this; }
        public Builder safeMessage(String m) { this.safeMessage = m; return this; }
        public Builder safeReasonCode(String c) { this.safeReasonCode = c; return this; }
        public Builder occurredAt(LocalDateTime t) { this.occurredAt = t; return this; }

        public AdminReviewActionResponse build() { return new AdminReviewActionResponse(this); }
    }
}
