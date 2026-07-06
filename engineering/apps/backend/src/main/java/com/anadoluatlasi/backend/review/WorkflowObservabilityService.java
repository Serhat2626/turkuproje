package com.anadoluatlasi.backend.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * FP-017 Task 02: Read-only service for internal workflow observability.
 *
 * <p>Derives aggregate health metrics and diagnostic items from existing
 * review records, pipeline aggregation, readiness, and preview services.</p>
 *
 * <p>This service is derived and read-only — it does NOT:</p>
 * <ul>
 *   <li>Mutate review state or create audit records</li>
 *   <li>Call external providers, storage, or generation services</li>
 *   <li>Create public URLs, signed URLs, or CDN entries</li>
 *   <li>Trigger approval, rejection, or status updates</li>
 *   <li>Trigger generation or regeneration</li>
 *   <li>Expose provider URLs, storage keys, raw payloads, or base64</li>
 * </ul>
 */
@Service
public class WorkflowObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowObservabilityService.class);

    /** Default staleness threshold in days. */
    private static final int STALE_THRESHOLD_DAYS = 7;

    private final PipelineAggregationService pipelineService;
    private final ReviewRecordRepository reviewRecordRepository;

    public WorkflowObservabilityService(PipelineAggregationService pipelineService,
                                         ReviewRecordRepository reviewRecordRepository) {
        this.pipelineService = pipelineService;
        this.reviewRecordRepository = reviewRecordRepository;
    }

    // ── Summary ────────────────────────────────────────────────────

    /**
     * Get aggregate workflow observability summary.
     * Read-only. No mutation. No provider/storage calls.
     *
     * @return safe observability summary with counts and diagnostics
     */
    @Transactional(readOnly = true)
    public SafeWorkflowObservabilitySummary getSummary() {
        List<String> warnings = new ArrayList<>();
        List<SafeWorkflowDiagnosticItem> diagnostics = new ArrayList<>();

        // ── Pipeline overview (all items, no filter) ──
        SafePipelineOverview overview;
        try {
            overview = pipelineService.getOverview(null, null, 200);
        } catch (Exception e) {
            log.warn("FP-017: Pipeline overview query failed safely: {}", safeLogMessage(e));
            return SafeWorkflowObservabilitySummary.empty("PIPELINE_QUERY_FAILED");
        }

        // ── Review state counts ──
        Map<ReviewState, Integer> countsByReviewState = new EnumMap<>(ReviewState.class);
        Map<SafePipelineStage, Integer> countsByPipelineStage = new EnumMap<>(SafePipelineStage.class);
        Map<InternalApprovalReadinessAvailability, Integer> countsByReadiness = new EnumMap<>(InternalApprovalReadinessAvailability.class);
        int previewAvailable = 0;
        int previewUnavailable = 0;
        int revisionRequested = 0;
        int unavailable = 0;

        for (SafePipelineAggregateItem item : overview.getItems()) {
            // Pipeline stage counts
            countsByPipelineStage.merge(item.getPipelineStage(), 1, Integer::sum);

            // Review state counts
            if (item.getReviewState() != null) {
                countsByReviewState.merge(item.getReviewState(), 1, Integer::sum);
            }

            // Readiness availability counts
            if (item.getReadinessAvailability() != null) {
                countsByReadiness.merge(item.getReadinessAvailability(), 1, Integer::sum);
            }

            // Preview
            if (item.isPreviewAvailable()) {
                previewAvailable++;
            } else {
                previewUnavailable++;
            }

            // Revision
            if (item.isRevisionRequested()) {
                revisionRequested++;
            }

            // Unavailable
            if (item.getPipelineStage() == SafePipelineStage.UNAVAILABLE) {
                unavailable++;
                diagnostics.add(buildDiagnostic(
                        SafeWorkflowDiagnosticType.PIPELINE_UNAVAILABLE,
                        item, "PIPELINE_UNAVAILABLE",
                        "Pipeline durumu belirlenemiyor",
                        List.of(SafeOperationalControlHint.REFRESH_OVERVIEW,
                                SafeOperationalControlHint.REQUEST_MANUAL_INVESTIGATION)));
            }

            // Preview unavailable diagnostic
            if (!item.isPreviewAvailable()
                    && item.getPipelineStage() != SafePipelineStage.UNAVAILABLE
                    && item.getPipelineStage() != SafePipelineStage.GENERATED) {
                diagnostics.add(buildDiagnostic(
                        SafeWorkflowDiagnosticType.PREVIEW_UNAVAILABLE,
                        item, "PREVIEW_UNAVAILABLE",
                        "Önizleme kullanılamıyor",
                        List.of(SafeOperationalControlHint.VIEW_SAFE_PREVIEW)));
            }

            // Readiness unavailable diagnostic
            if (item.getReadinessAvailability() == InternalApprovalReadinessAvailability.UNAVAILABLE
                    && item.getPipelineStage() == SafePipelineStage.INTERNALLY_APPROVED) {
                diagnostics.add(buildDiagnostic(
                        SafeWorkflowDiagnosticType.READINESS_UNAVAILABLE,
                        item, "READINESS_UNAVAILABLE",
                        "Dahili hazırlık durumu belirlenemiyor",
                        List.of(SafeOperationalControlHint.RECHECK_READINESS)));
            }

            // Revision requested diagnostic
            if (item.isRevisionRequested()) {
                diagnostics.add(buildDiagnostic(
                        SafeWorkflowDiagnosticType.REVISION_REQUESTED,
                        item, "REVISION_REQUESTED",
                        "Revizyon istendi — takip gerekiyor",
                        List.of(SafeOperationalControlHint.REVIEW_PIPELINE_DETAIL)));
            }

            // Blocked internal workflow (multiple blocking reasons)
            if (item.getSafeBlockingReasons() != null && item.getSafeBlockingReasons().size() > 1) {
                diagnostics.add(buildDiagnostic(
                        SafeWorkflowDiagnosticType.BLOCKED_INTERNAL_WORKFLOW,
                        item, "BLOCKED",
                        "Birden fazla engel: " + item.getSafeBlockingReasons().size(),
                        List.of(SafeOperationalControlHint.REVIEW_PIPELINE_DETAIL,
                                SafeOperationalControlHint.REQUEST_MANUAL_INVESTIGATION)));
            }
        }

        // ── Stale items ──
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(STALE_THRESHOLD_DAYS);
            List<ReviewRecordEntity> staleRecords = reviewRecordRepository.findAll().stream()
                    .filter(r -> r.getUpdatedAt() != null && r.getUpdatedAt().isBefore(threshold))
                    .filter(r -> r.getCurrentState() != ReviewState.REJECTED
                            && r.getCurrentState() != ReviewState.APPROVED_FOR_INTERNAL_USE)
                    .toList();
            for (ReviewRecordEntity stale : staleRecords) {
                diagnostics.add(SafeWorkflowDiagnosticItem.builder()
                        .diagnosticType(SafeWorkflowDiagnosticType.STALE_REVIEW)
                        .targetType(stale.getReviewTargetType())
                        .targetId(stale.getReviewTargetId())
                        .pipelineStage(SafePipelineStage.UNAVAILABLE)
                        .reviewState(stale.getCurrentState())
                        .safeReasonCode("STALE_REVIEW")
                        .safeMessage("Son güncelleme " + STALE_THRESHOLD_DAYS + " günden eski")
                        .safeRecommendedHints(List.of(
                                SafeOperationalControlHint.REVIEW_PIPELINE_DETAIL,
                                SafeOperationalControlHint.REQUEST_MANUAL_INVESTIGATION))
                        .updatedAt(stale.getUpdatedAt())
                        .build());
            }
        } catch (Exception e) {
            log.warn("FP-017: Stale item check failed safely: {}", safeLogMessage(e));
            warnings.add("STALE_CHECK_DEGRADED");
        }

        // ── Propagate pipeline warnings ──
        if (overview.getSafeWarnings() != null) {
            warnings.addAll(overview.getSafeWarnings());
        }

        return SafeWorkflowObservabilitySummary.builder()
                .countsByReviewState(countsByReviewState)
                .countsByPipelineStage(countsByPipelineStage)
                .countsByReadinessAvailability(countsByReadiness)
                .previewAvailableCount(previewAvailable)
                .previewUnavailableCount(previewUnavailable)
                .revisionRequestedCount(revisionRequested)
                .unavailableCount(unavailable)
                .diagnostics(diagnostics)
                .safeWarnings(warnings)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── Diagnostics ────────────────────────────────────────────────

    /**
     * Get diagnostics for a specific target type.
     * Derived from full summary, filtered.
     */
    @Transactional(readOnly = true)
    public List<SafeWorkflowDiagnosticItem> getDiagnostics(ReviewTargetType targetType) {
        SafeWorkflowObservabilitySummary summary = getSummary();
        if (targetType == null) {
            return summary.getDiagnostics();
        }
        return summary.getDiagnostics().stream()
                .filter(d -> d.getTargetType() == targetType)
                .toList();
    }

    // ── Private helpers ────────────────────────────────────────────

    private SafeWorkflowDiagnosticItem buildDiagnostic(
            SafeWorkflowDiagnosticType type,
            SafePipelineAggregateItem item,
            String reasonCode,
            String message,
            List<SafeOperationalControlHint> hints) {
        return SafeWorkflowDiagnosticItem.builder()
                .diagnosticType(type)
                .targetType(item.getTargetType())
                .targetId(item.getTargetId())
                .pipelineStage(item.getPipelineStage())
                .reviewState(item.getReviewState())
                .safeReasonCode(reasonCode)
                .safeMessage(message)
                .safeRecommendedHints(hints)
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private String safeLogMessage(Exception e) {
        if (e == null) return "UNKNOWN_ERROR";
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return e.getClass().getSimpleName();
        // Truncate and sanitize
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}
