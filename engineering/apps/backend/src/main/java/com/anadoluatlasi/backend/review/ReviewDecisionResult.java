package com.anadoluatlasi.backend.review;

import java.util.Objects;

/**
 * FP-012 Task 03: Immutable result of a review domain service decision.
 *
 * <p>No persistence annotations, no DTO/controller annotation.
 * No public URL/signed URL/publish/export semantics.
 * toString is safe.</p>
 */
public final class ReviewDecisionResult {

    private final ReviewState previousState;
    private final ReviewState newState;
    private final ReviewActionType actionType;
    private final ReviewActorRole actorRole;
    private final boolean changed;
    private final ReviewAuditRecord auditRecord;
    private final String safeMessage;
    private final String safeReasonCode;

    private ReviewDecisionResult(Builder builder) {
        this.previousState = Objects.requireNonNull(builder.previousState, "previousState must not be null");
        this.newState = Objects.requireNonNull(builder.newState, "newState must not be null");
        this.actionType = Objects.requireNonNull(builder.actionType, "actionType must not be null");
        this.actorRole = Objects.requireNonNull(builder.actorRole, "actorRole must not be null");
        this.changed = builder.changed;
        this.auditRecord = builder.auditRecord;
        this.safeMessage = builder.safeMessage;
        this.safeReasonCode = builder.safeReasonCode;
    }

    public ReviewState getPreviousState() { return previousState; }
    public ReviewState getNewState() { return newState; }
    public ReviewActionType getActionType() { return actionType; }
    public ReviewActorRole getActorRole() { return actorRole; }
    public boolean isChanged() { return changed; }
    public ReviewAuditRecord getAuditRecord() { return auditRecord; }
    public String getSafeMessage() { return safeMessage; }
    public String getSafeReasonCode() { return safeReasonCode; }

    /**
     * Safe toString — contains only enum names, safe codes/messages.
     * Never contains secrets, provider URLs, storage keys, raw payloads, or base64.
     */
    @Override
    public String toString() {
        return "ReviewDecisionResult{" +
                "previousState=" + previousState +
                ", newState=" + newState +
                ", actionType=" + actionType +
                ", actorRole=" + actorRole +
                ", changed=" + changed +
                ", safeMessage='" + safeMessage + '\'' +
                ", safeReasonCode='" + safeReasonCode + '\'' +
                '}';
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ReviewState previousState;
        private ReviewState newState;
        private ReviewActionType actionType;
        private ReviewActorRole actorRole;
        private boolean changed;
        private ReviewAuditRecord auditRecord;
        private String safeMessage;
        private String safeReasonCode;

        public Builder previousState(ReviewState s) { this.previousState = s; return this; }
        public Builder newState(ReviewState s) { this.newState = s; return this; }
        public Builder actionType(ReviewActionType a) { this.actionType = a; return this; }
        public Builder actorRole(ReviewActorRole r) { this.actorRole = r; return this; }
        public Builder changed(boolean c) { this.changed = c; return this; }
        public Builder auditRecord(ReviewAuditRecord r) { this.auditRecord = r; return this; }
        public Builder safeMessage(String m) { this.safeMessage = m; return this; }
        public Builder safeReasonCode(String c) { this.safeReasonCode = c; return this; }

        public ReviewDecisionResult build() { return new ReviewDecisionResult(this); }
    }
}
