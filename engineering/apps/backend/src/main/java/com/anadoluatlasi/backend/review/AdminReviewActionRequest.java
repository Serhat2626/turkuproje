package com.anadoluatlasi.backend.review;

import java.util.UUID;

/**
 * FP-012 Task 06: Safe admin review action request DTO.
 *
 * <p>Uses explicit ReviewActionType — no generic arbitrary status update.
 * No controller/REST annotation. No provider URL, storage key, raw payload,
 * base64, secret, public/signed URL, or publish/export fields.</p>
 */
public final class AdminReviewActionRequest {

    private ReviewTargetType targetType;
    private UUID targetId;
    private ReviewActionType actionType;
    private ReviewActorRole actorRole;
    private String actorReference;
    private String reasonCode;
    private String note;

    public AdminReviewActionRequest() {}

    public ReviewTargetType getTargetType() { return targetType; }
    public void setTargetType(ReviewTargetType targetType) { this.targetType = targetType; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public ReviewActionType getActionType() { return actionType; }
    public void setActionType(ReviewActionType actionType) { this.actionType = actionType; }

    public ReviewActorRole getActorRole() { return actorRole; }
    public void setActorRole(ReviewActorRole actorRole) { this.actorRole = actorRole; }

    public String getActorReference() { return actorReference; }
    public void setActorReference(String actorReference) { this.actorReference = actorReference; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    /**
     * Safe toString — no secrets, provider URLs, storage keys, raw payloads, or base64.
     * Note content is hidden behind hasNote boolean.
     */
    @Override
    public String toString() {
        return "AdminReviewActionRequest{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", actionType=" + actionType +
                ", actorRole=" + actorRole +
                ", actorReference='" + actorReference + '\'' +
                ", hasNote=" + (note != null && !note.isBlank()) +
                ", hasReasonCode=" + (reasonCode != null && !reasonCode.isBlank()) +
                '}';
    }
}
