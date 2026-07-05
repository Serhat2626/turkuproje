package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Data;

/**
 * FP-012 Task 11: Safe single action availability.
 *
 * <p>No publish/export/generic status/public-signed URL/provider/storage/config fields.</p>
 */
@Data
@Builder
public class AdminReviewAvailableAction {

    private ReviewActionType actionType;
    private boolean enabled;
    private String disabledReasonCode;
    private String safeLabel;
    private String safeDescription;

    @Override
    public String toString() {
        return "AdminReviewAvailableAction{" +
                "actionType=" + actionType +
                ", enabled=" + enabled +
                '}';
    }
}
