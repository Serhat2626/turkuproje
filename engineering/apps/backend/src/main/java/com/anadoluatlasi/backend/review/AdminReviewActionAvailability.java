package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * FP-012 Task 11: Safe action availability container.
 *
 * <p>No publish/export/provider/storage/public-signed URL fields.
 * APPROVED_FOR_INTERNAL_USE remains internal-only.</p>
 */
@Data
@Builder
public class AdminReviewActionAvailability {

    private ReviewTargetType targetType;
    private UUID targetId;
    private ReviewState currentState;
    private ReviewActorRole actorRole;
    private List<AdminReviewAvailableAction> availableActions;
    private String safeMessage;

    @Override
    public String toString() {
        return "AdminReviewActionAvailability{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", currentState=" + currentState +
                ", actorRole=" + actorRole +
                ", actionCount=" + (availableActions != null ? availableActions.size() : 0) +
                '}';
    }
}
