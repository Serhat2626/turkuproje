package com.anadoluatlasi.backend.review;

import java.time.LocalDateTime;

/**
 * FP-012 Task 06: Safe admin review audit timeline item DTO.
 *
 * <p>No raw payload, provider URL, storage key, base64, secret/config,
 * public/signed URL, or publish/export events. Sanitized note only.</p>
 */
public final class AdminReviewAuditTimelineItem {

    private final ReviewAuditEventType eventType;
    private final ReviewState fromState;
    private final ReviewState toState;
    private final ReviewActionType actionType;
    private final ReviewActorRole actorRole;
    private final String actorReference;
    private final LocalDateTime occurredAt;
    private final String safeReasonCode;
    private final String safeMessage;
    private final String sanitizedNote;

    private AdminReviewAuditTimelineItem(Builder builder) {
        this.eventType = builder.eventType;
        this.fromState = builder.fromState;
        this.toState = builder.toState;
        this.actionType = builder.actionType;
        this.actorRole = builder.actorRole;
        this.actorReference = builder.actorReference;
        this.occurredAt = builder.occurredAt;
        this.safeReasonCode = builder.safeReasonCode;
        this.safeMessage = builder.safeMessage;
        this.sanitizedNote = builder.sanitizedNote;
    }

    public ReviewAuditEventType getEventType() { return eventType; }
    public ReviewState getFromState() { return fromState; }
    public ReviewState getToState() { return toState; }
    public ReviewActionType getActionType() { return actionType; }
    public ReviewActorRole getActorRole() { return actorRole; }
    public String getActorReference() { return actorReference; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getSafeReasonCode() { return safeReasonCode; }
    public String getSafeMessage() { return safeMessage; }
    public String getSanitizedNote() { return sanitizedNote; }

    @Override
    public String toString() {
        return "AdminReviewAuditTimelineItem{" +
                "eventType=" + eventType +
                ", fromState=" + fromState +
                ", toState=" + toState +
                ", actionType=" + actionType +
                ", actorRole=" + actorRole +
                ", hasNote=" + (sanitizedNote != null && !sanitizedNote.isBlank()) +
                '}';
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ReviewAuditEventType eventType;
        private ReviewState fromState;
        private ReviewState toState;
        private ReviewActionType actionType;
        private ReviewActorRole actorRole;
        private String actorReference;
        private LocalDateTime occurredAt;
        private String safeReasonCode;
        private String safeMessage;
        private String sanitizedNote;

        public Builder eventType(ReviewAuditEventType e) { this.eventType = e; return this; }
        public Builder fromState(ReviewState s) { this.fromState = s; return this; }
        public Builder toState(ReviewState s) { this.toState = s; return this; }
        public Builder actionType(ReviewActionType a) { this.actionType = a; return this; }
        public Builder actorRole(ReviewActorRole r) { this.actorRole = r; return this; }
        public Builder actorReference(String r) { this.actorReference = r; return this; }
        public Builder occurredAt(LocalDateTime t) { this.occurredAt = t; return this; }
        public Builder safeReasonCode(String c) { this.safeReasonCode = c; return this; }
        public Builder safeMessage(String m) { this.safeMessage = m; return this; }
        public Builder sanitizedNote(String n) { this.sanitizedNote = n; return this; }

        public AdminReviewAuditTimelineItem build() { return new AdminReviewAuditTimelineItem(this); }
    }
}
