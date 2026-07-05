package com.anadoluatlasi.backend.review;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-012 Task 06: Safe admin review state summary DTO.
 *
 * <p>No raw note, no provider/storage/public delivery fields,
 * no publish/export readiness. Safe toString.</p>
 */
public final class AdminReviewStateSummary {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final ReviewState currentState;
    private final String currentReviewerActorReference;
    private final ReviewActionType lastActionType;
    private final LocalDateTime lastActionAt;
    private final String lastActionByActorReference;
    private final String safeMessage;
    private final String safeReasonCode;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private AdminReviewStateSummary(Builder builder) {
        this.targetType = builder.targetType;
        this.targetId = builder.targetId;
        this.currentState = builder.currentState;
        this.currentReviewerActorReference = builder.currentReviewerActorReference;
        this.lastActionType = builder.lastActionType;
        this.lastActionAt = builder.lastActionAt;
        this.lastActionByActorReference = builder.lastActionByActorReference;
        this.safeMessage = builder.safeMessage;
        this.safeReasonCode = builder.safeReasonCode;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public ReviewTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public ReviewState getCurrentState() { return currentState; }
    public String getCurrentReviewerActorReference() { return currentReviewerActorReference; }
    public ReviewActionType getLastActionType() { return lastActionType; }
    public LocalDateTime getLastActionAt() { return lastActionAt; }
    public String getLastActionByActorReference() { return lastActionByActorReference; }
    public String getSafeMessage() { return safeMessage; }
    public String getSafeReasonCode() { return safeReasonCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "AdminReviewStateSummary{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", currentState=" + currentState +
                ", lastActionType=" + lastActionType +
                ", safeReasonCode='" + safeReasonCode + '\'' +
                '}';
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private ReviewTargetType targetType;
        private UUID targetId;
        private ReviewState currentState;
        private String currentReviewerActorReference;
        private ReviewActionType lastActionType;
        private LocalDateTime lastActionAt;
        private String lastActionByActorReference;
        private String safeMessage;
        private String safeReasonCode;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder targetType(ReviewTargetType t) { this.targetType = t; return this; }
        public Builder targetId(UUID id) { this.targetId = id; return this; }
        public Builder currentState(ReviewState s) { this.currentState = s; return this; }
        public Builder currentReviewerActorReference(String r) { this.currentReviewerActorReference = r; return this; }
        public Builder lastActionType(ReviewActionType a) { this.lastActionType = a; return this; }
        public Builder lastActionAt(LocalDateTime t) { this.lastActionAt = t; return this; }
        public Builder lastActionByActorReference(String r) { this.lastActionByActorReference = r; return this; }
        public Builder safeMessage(String m) { this.safeMessage = m; return this; }
        public Builder safeReasonCode(String c) { this.safeReasonCode = c; return this; }
        public Builder createdAt(LocalDateTime t) { this.createdAt = t; return this; }
        public Builder updatedAt(LocalDateTime t) { this.updatedAt = t; return this; }

        public AdminReviewStateSummary build() { return new AdminReviewStateSummary(this); }
    }
}
