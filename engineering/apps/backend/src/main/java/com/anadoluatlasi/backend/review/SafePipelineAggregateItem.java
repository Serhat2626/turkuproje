package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-016 Task 02: Safe immutable DTO for a single pipeline aggregate item.
 *
 * <p>Represents a derived, read-only view of where a target stands in
 * the internal pipeline. Combines review state, preview availability,
 * internal readiness, and revision request status.</p>
 *
 * <p>This DTO is internal-only. It does NOT imply publication, export,
 * or public delivery eligibility.</p>
 *
 * <p>Safe fields only — no forbidden exposure.</p>
 *
 * <p>All text fields sanitized via {@link ReviewTextSafety}.</p>
 */
@Getter
@Builder
public class SafePipelineAggregateItem {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final SafePipelineStage pipelineStage;
    private final ReviewState reviewState;
    private final boolean previewAvailable;
    private final InternalApprovalReadinessAvailability readinessAvailability;
    private final boolean revisionRequested;

    // Safe metadata
    private final String safeTitle;
    private final String safeSummary;

    // Safe blocking/hints
    private final List<String> safeBlockingReasons;
    private final List<String> safeNextInternalHints;

    private final LocalDateTime updatedAt;

    // ── Factory methods ────────────────────────────────────────────

    /**
     * Factory for unavailable/missing target.
     */
    public static SafePipelineAggregateItem unavailable(
            ReviewTargetType targetType, UUID targetId, String reason) {
        return SafePipelineAggregateItem.builder()
                .targetType(targetType)
                .targetId(targetId)
                .pipelineStage(SafePipelineStage.UNAVAILABLE)
                .reviewState(null)
                .previewAvailable(false)
                .readinessAvailability(InternalApprovalReadinessAvailability.UNAVAILABLE)
                .revisionRequested(false)
                .safeTitle(null)
                .safeSummary(null)
                .safeBlockingReasons(reason != null ? List.of(reason) : List.of("TARGET_UNAVAILABLE"))
                .safeNextInternalHints(List.of())
                .updatedAt(null)
                .build();
    }

    /**
     * Safe toString — no forbidden fields.
     */
    @Override
    public String toString() {
        return "SafePipelineAggregateItem{"
                + "targetType=" + targetType
                + ", targetId=" + targetId
                + ", pipelineStage=" + pipelineStage
                + ", reviewState=" + reviewState
                + ", previewAvailable=" + previewAvailable
                + ", readinessAvailability=" + readinessAvailability
                + ", revisionRequested=" + revisionRequested
                + '}';
    }
}
