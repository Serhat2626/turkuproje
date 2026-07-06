package com.anadoluatlasi.backend.review;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FP-017 Task 02: Safe immutable DTO for aggregate workflow observability summary.
 *
 * <p>Provides aggregate counts and diagnostic items derived from existing
 * review records, pipeline aggregation, readiness, and preview services.</p>
 *
 * <p>This DTO is internal-only. It does NOT imply publication, export,
 * or public delivery readiness.</p>
 *
 * <p>Safe fields only — no forbidden exposure.</p>
 */
@Getter
@Builder
public class SafeWorkflowObservabilitySummary {

    /** Count of items per ReviewState. */
    private final Map<ReviewState, Integer> countsByReviewState;

    /** Count of items per SafePipelineStage. */
    private final Map<SafePipelineStage, Integer> countsByPipelineStage;

    /** Count of items per InternalApprovalReadinessAvailability. */
    private final Map<InternalApprovalReadinessAvailability, Integer> countsByReadinessAvailability;

    /** Items with safe preview available. */
    private final int previewAvailableCount;

    /** Items without safe preview. */
    private final int previewUnavailableCount;

    /** Items with active revision request. */
    private final int revisionRequestedCount;

    /** Items in UNAVAILABLE pipeline stage. */
    private final int unavailableCount;

    /** Diagnostic items needing attention. */
    private final List<SafeWorkflowDiagnosticItem> diagnostics;

    /** Safe warning messages (e.g., degraded data source). */
    private final List<String> safeWarnings;

    private final LocalDateTime updatedAt;

    // ── Factory methods ────────────────────────────────────────────

    /**
     * Factory for empty/degraded summary.
     */
    public static SafeWorkflowObservabilitySummary empty(String... warnings) {
        return SafeWorkflowObservabilitySummary.builder()
                .countsByReviewState(Map.of())
                .countsByPipelineStage(Map.of())
                .countsByReadinessAvailability(Map.of())
                .previewAvailableCount(0)
                .previewUnavailableCount(0)
                .revisionRequestedCount(0)
                .unavailableCount(0)
                .diagnostics(List.of())
                .safeWarnings(List.of(warnings))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public String toString() {
        return "SafeWorkflowObservabilitySummary{"
                + "countsByReviewState=" + countsByReviewState
                + ", countsByPipelineStage=" + countsByPipelineStage
                + ", countsByReadinessAvailability=" + countsByReadinessAvailability
                + ", previewAvailableCount=" + previewAvailableCount
                + ", previewUnavailableCount=" + previewUnavailableCount
                + ", revisionRequestedCount=" + revisionRequestedCount
                + ", unavailableCount=" + unavailableCount
                + ", diagnosticsCount=" + (diagnostics != null ? diagnostics.size() : 0)
                + ", safeWarnings=" + safeWarnings
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
