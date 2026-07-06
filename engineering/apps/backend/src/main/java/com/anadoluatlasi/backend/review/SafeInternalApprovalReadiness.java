package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-014 Task 02: Safe immutable DTO for internal approval readiness.
 *
 * <p>Represents a derived, read-only view of whether a review target is
 * internally approved and what safe internal actions are available.</p>
 *
 * <p>This DTO does NOT imply publication, export, or public delivery.
 * APPROVED_FOR_INTERNAL_USE is internal editorial approval only.</p>
 *
 * <p>No provider URL, storage key, bucket, raw payload, base64, secret,
 * public/signed URL, publish/export, bytes, or stack trace fields.</p>
 *
 * <p>All text fields sanitized via {@link ReviewTextSafety}.</p>
 */
@Getter
@Builder
public class SafeInternalApprovalReadiness {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final ReviewState reviewState;
    private final InternalApprovalReadinessAvailability availability;

    // Safe metadata
    private final String safeTitle;
    private final String safeSummary;
    private final String safeReasonCode;
    private final String safeMessage;
    private final boolean safePreviewAvailable;

    // Approval detail (populated when approved)
    private final LocalDateTime approvedAt;
    private final String approvedByRole;

    // Safe next internal actions
    private final List<SafeInternalAction> safeNextInternalActions;

    // Safe warnings
    private final List<String> safeWarnings;

    private final LocalDateTime updatedAt;

    // ── Factory methods ────────────────────────────────────────────

    /**
     * Factory for unavailable/missing target readiness.
     */
    public static SafeInternalApprovalReadiness unavailable(
            ReviewTargetType targetType, UUID targetId,
            InternalApprovalReadinessAvailability availability,
            String safeReasonCode, String safeMessage) {
        return SafeInternalApprovalReadiness.builder()
                .targetType(targetType)
                .targetId(targetId)
                .reviewState(null)
                .availability(availability)
                .safeTitle(null)
                .safeSummary(null)
                .safeReasonCode(ReviewTextSafety.sanitizeReasonCode(safeReasonCode))
                .safeMessage(ReviewTextSafety.sanitize(safeMessage))
                .safePreviewAvailable(false)
                .approvedAt(null)
                .approvedByRole(null)
                .safeNextInternalActions(List.of())
                .safeWarnings(List.of())
                .updatedAt(null)
                .build();
    }

    /**
     * Safe toString — no secrets, provider URLs, storage keys, raw payloads, or base64.
     */
    @Override
    public String toString() {
        return "SafeInternalApprovalReadiness{" +
                "targetType=" + targetType +
                ", targetId=" + targetId +
                ", reviewState=" + reviewState +
                ", availability=" + availability +
                ", safePreviewAvailable=" + safePreviewAvailable +
                '}';
    }
}
