package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-013 Task 03: Safe immutable preview DTO for admin review context.
 *
 * <p>No JPA annotations. No controller/REST annotations.
 * No provider URL, storage key, bucket, raw payload, base64, secret,
 * public/signed URL, publish/export, rawImageBytes, rawContent fields.</p>
 *
 * <p>All text fields sanitized via {@link ReviewTextSafety}.</p>
 *
 * <p>Preview does not imply approval. Preview is internal/admin-only.
 * APPROVED_FOR_INTERNAL_USE is NOT publish/export approval.</p>
 */
@Getter
@Builder
public class SafeAdminPreview {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final AdminPreviewMode previewMode;
    private final AdminPreviewAvailability availability;
    private final boolean previewAvailable;
    private final String safeTitle;
    private final String safeSummary;
    private final String safeMessage;
    private final String safeReasonCode;
    private final String safePreviewLabel;
    private final boolean safeThumbnailAvailable;
    private final ReviewState currentReviewState;
    private final LocalDateTime updatedAt;

    /**
     * Factory for unavailable preview.
     */
    public static SafeAdminPreview unavailable(ReviewTargetType targetType, UUID targetId,
                                                AdminPreviewAvailability availability,
                                                String safeReasonCode, String safeMessage) {
        return SafeAdminPreview.builder()
                .targetType(targetType)
                .targetId(targetId)
                .previewMode(AdminPreviewMode.UNAVAILABLE)
                .availability(availability)
                .previewAvailable(false)
                .safeTitle(null)
                .safeSummary(null)
                .safeMessage(ReviewTextSafety.sanitize(safeMessage))
                .safeReasonCode(ReviewTextSafety.sanitizeReasonCode(safeReasonCode))
                .safePreviewLabel("Önizleme kullanılamıyor")
                .safeThumbnailAvailable(false)
                .currentReviewState(null)
                .updatedAt(null)
                .build();
    }

    @Override
    public String toString() {
        return "SafeAdminPreview{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", previewMode=" + previewMode +
                ", availability=" + availability +
                ", previewAvailable=" + previewAvailable +
                ", currentReviewState=" + currentReviewState +
                '}';
    }
}
