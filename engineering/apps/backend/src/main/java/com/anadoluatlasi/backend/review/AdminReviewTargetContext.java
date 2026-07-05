package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-012 Task 10: Safe admin review target context.
 *
 * <p>Provides minimal safe context about the review target for admin UI display.
 * Derived from ReviewRecordEntity only — no provider/storage/external lookups.</p>
 *
 * <p>No provider URL, storage key, bucket/path, raw payload, base64,
 * secret, public/signed URL, publish/export fields.</p>
 */
@Data
@Builder
public class AdminReviewTargetContext {

    private ReviewTargetType targetType;
    private UUID targetId;
    private ReviewState currentState;
    private String displayTitle;
    private String displaySubtitle;
    private boolean safePreviewAvailable;
    private String safePreviewLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Safe toString — no secrets, provider URLs, storage keys, raw payloads.
     */
    @Override
    public String toString() {
        return "AdminReviewTargetContext{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", currentState=" + currentState +
                ", displayTitle='" + displayTitle + '\'' +
                '}';
    }
}
