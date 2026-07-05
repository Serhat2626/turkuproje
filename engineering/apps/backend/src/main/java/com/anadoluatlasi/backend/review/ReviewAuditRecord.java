package com.anadoluatlasi.backend.review;

import java.time.Instant;
import java.util.Objects;

/**
 * FP-012 Task 02: Immutable audit record for review workflow actions.
 *
 * <p>No DB annotation, no entity mapping, no migration.
 * toString is safe — no secrets, provider URLs, storage keys, raw payloads, or base64.</p>
 */
public final class ReviewAuditRecord {

    private final ReviewAuditEventType eventType;
    private final ReviewState fromState;
    private final ReviewState toState;
    private final ReviewActionType actionType;
    private final ReviewActorRole actorRole;
    private final String actorReference;
    private final Instant occurredAt;
    private final String safeReasonCode;
    private final String safeMessage;

    private ReviewAuditRecord(Builder builder) {
        this.eventType = Objects.requireNonNull(builder.eventType, "eventType must not be null");
        this.fromState = builder.fromState;
        this.toState = builder.toState;
        this.actionType = builder.actionType;
        this.actorRole = builder.actorRole;
        this.actorReference = builder.actorReference;
        this.occurredAt = builder.occurredAt != null ? builder.occurredAt : Instant.now();
        this.safeReasonCode = builder.safeReasonCode;
        this.safeMessage = builder.safeMessage;
    }

    public ReviewAuditEventType getEventType() { return eventType; }
    public ReviewState getFromState() { return fromState; }
    public ReviewState getToState() { return toState; }
    public ReviewActionType getActionType() { return actionType; }
    public ReviewActorRole getActorRole() { return actorRole; }
    public String getActorReference() { return actorReference; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getSafeReasonCode() { return safeReasonCode; }
    public String getSafeMessage() { return safeMessage; }

    /**
     * Safe toString — contains only enum names, safe codes/messages, timestamps.
     * Never contains secrets, provider URLs, storage keys, raw payloads, or base64.
     */
    @Override
    public String toString() {
        return "ReviewAuditRecord{" +
                "eventType=" + eventType +
                ", fromState=" + fromState +
                ", toState=" + toState +
                ", actionType=" + actionType +
                ", actorRole=" + actorRole +
                ", actorReference='" + actorReference + '\'' +
                ", occurredAt=" + occurredAt +
                ", safeReasonCode='" + safeReasonCode + '\'' +
                ", safeMessage='" + safeMessage + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ReviewAuditEventType eventType;
        private ReviewState fromState;
        private ReviewState toState;
        private ReviewActionType actionType;
        private ReviewActorRole actorRole;
        private String actorReference;
        private Instant occurredAt;
        private String safeReasonCode;
        private String safeMessage;

        public Builder eventType(ReviewAuditEventType eventType) { this.eventType = eventType; return this; }
        public Builder fromState(ReviewState fromState) { this.fromState = fromState; return this; }
        public Builder toState(ReviewState toState) { this.toState = toState; return this; }
        public Builder actionType(ReviewActionType actionType) { this.actionType = actionType; return this; }
        public Builder actorRole(ReviewActorRole actorRole) { this.actorRole = actorRole; return this; }
        public Builder actorReference(String actorReference) { this.actorReference = actorReference; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder safeReasonCode(String safeReasonCode) { this.safeReasonCode = safeReasonCode; return this; }
        public Builder safeMessage(String safeMessage) { this.safeMessage = safeMessage; return this; }

        public ReviewAuditRecord build() {
            return new ReviewAuditRecord(this);
        }
    }
}
