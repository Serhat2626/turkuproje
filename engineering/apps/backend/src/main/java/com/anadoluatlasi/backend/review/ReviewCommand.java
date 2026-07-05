package com.anadoluatlasi.backend.review;

import java.util.Objects;

/**
 * FP-012 Task 03: Immutable command representing a requested review action.
 *
 * <p>No JPA, no DTO/controller annotation, no persistence wiring.
 * toString is safe — no secrets, provider URLs, storage keys, raw payloads, or base64.</p>
 */
public final class ReviewCommand {

    private final ReviewState currentState;
    private final ReviewState requestedState;
    private final ReviewActionType actionType;
    private final ReviewActorRole actorRole;
    private final String actorReference;
    private final String note;
    private final String reasonCode;

    private ReviewCommand(Builder builder) {
        this.currentState = Objects.requireNonNull(builder.currentState, "currentState must not be null");
        this.actionType = Objects.requireNonNull(builder.actionType, "actionType must not be null");
        this.actorRole = Objects.requireNonNull(builder.actorRole, "actorRole must not be null");
        this.requestedState = builder.requestedState;
        this.actorReference = builder.actorReference;
        this.note = ReviewTextSafety.sanitize(builder.note);
        this.reasonCode = ReviewTextSafety.sanitizeReasonCode(builder.reasonCode);
    }

    public ReviewState getCurrentState() { return currentState; }
    public ReviewState getRequestedState() { return requestedState; }
    public ReviewActionType getActionType() { return actionType; }
    public ReviewActorRole getActorRole() { return actorRole; }
    public String getActorReference() { return actorReference; }
    public String getNote() { return note; }
    public String getReasonCode() { return reasonCode; }

    /**
     * Safe toString — contains only enum names and safe references.
     * Never contains secrets, provider URLs, storage keys, raw payloads, or base64.
     */
    @Override
    public String toString() {
        return "ReviewCommand{" +
                "currentState=" + currentState +
                ", requestedState=" + requestedState +
                ", actionType=" + actionType +
                ", actorRole=" + actorRole +
                ", actorReference='" + actorReference + '\'' +
                ", hasNote=" + (note != null && !note.isBlank()) +
                ", hasReasonCode=" + (reasonCode != null && !reasonCode.isBlank()) +
                '}';
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ReviewState currentState;
        private ReviewState requestedState;
        private ReviewActionType actionType;
        private ReviewActorRole actorRole;
        private String actorReference;
        private String note;
        private String reasonCode;

        public Builder currentState(ReviewState s) { this.currentState = s; return this; }
        public Builder requestedState(ReviewState s) { this.requestedState = s; return this; }
        public Builder actionType(ReviewActionType a) { this.actionType = a; return this; }
        public Builder actorRole(ReviewActorRole r) { this.actorRole = r; return this; }
        public Builder actorReference(String ref) { this.actorReference = ref; return this; }
        public Builder note(String n) { this.note = n; return this; }
        public Builder reasonCode(String c) { this.reasonCode = c; return this; }

        public ReviewCommand build() { return new ReviewCommand(this); }
    }
}
