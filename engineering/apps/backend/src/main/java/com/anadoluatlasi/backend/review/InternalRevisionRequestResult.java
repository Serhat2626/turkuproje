package com.anadoluatlasi.backend.review;

import java.time.LocalDateTime;

/**
 * FP-015 Task 02: Safe immutable result of an internal revision request.
 *
 * <p>Contains only safe fields — no secrets, URLs, storage keys, base64, payloads.</p>
 *
 * <p>Does NOT contain:</p>
 * <ul>
 *   <li>publicUrl / signedUrl / cdnUrl</li>
 *   <li>providerUrl / storageKey / bucket / objectPath</li>
 *   <li>rawPayload / base64 / bytes</li>
 *   <li>secret / apiKey / accessKey / token</li>
 *   <li>publishStatus / exportStatus / publishedAt / exportedAt</li>
 *   <li>stackTrace / exceptionClass</li>
 * </ul>
 */
public final class InternalRevisionRequestResult {

    private final ReviewState previousState;
    private final ReviewState currentState;
    private final String actionApplied;
    private final String safeReasonCode;
    private final String safeMessage;
    private final LocalDateTime updatedAt;
    private final boolean appendedNote;
    private final boolean auditRecorded;

    private InternalRevisionRequestResult(Builder builder) {
        this.previousState = builder.previousState;
        this.currentState = builder.currentState;
        this.actionApplied = builder.actionApplied;
        this.safeReasonCode = builder.safeReasonCode;
        this.safeMessage = builder.safeMessage;
        this.updatedAt = builder.updatedAt;
        this.appendedNote = builder.appendedNote;
        this.auditRecorded = builder.auditRecorded;
    }

    public ReviewState getPreviousState() { return previousState; }
    public ReviewState getCurrentState() { return currentState; }
    public String getActionApplied() { return actionApplied; }
    public String getSafeReasonCode() { return safeReasonCode; }
    public String getSafeMessage() { return safeMessage; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isAppendedNote() { return appendedNote; }
    public boolean isAuditRecorded() { return auditRecorded; }

    /**
     * Safe toString — no secrets, no URLs, no storage keys.
     */
    @Override
    public String toString() {
        return "InternalRevisionRequestResult{previousState=" + previousState
                + ", currentState=" + currentState
                + ", actionApplied=" + actionApplied
                + ", appendedNote=" + appendedNote
                + ", auditRecorded=" + auditRecorded
                + '}';
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ReviewState previousState;
        private ReviewState currentState;
        private String actionApplied;
        private String safeReasonCode;
        private String safeMessage;
        private LocalDateTime updatedAt;
        private boolean appendedNote;
        private boolean auditRecorded;

        private Builder() {}

        public Builder previousState(ReviewState val) { previousState = val; return this; }
        public Builder currentState(ReviewState val) { currentState = val; return this; }
        public Builder actionApplied(String val) { actionApplied = val; return this; }
        public Builder safeReasonCode(String val) { safeReasonCode = val; return this; }
        public Builder safeMessage(String val) { safeMessage = val; return this; }
        public Builder updatedAt(LocalDateTime val) { updatedAt = val; return this; }
        public Builder appendedNote(boolean val) { appendedNote = val; return this; }
        public Builder auditRecorded(boolean val) { auditRecorded = val; return this; }

        public InternalRevisionRequestResult build() { return new InternalRevisionRequestResult(this); }
    }
}
