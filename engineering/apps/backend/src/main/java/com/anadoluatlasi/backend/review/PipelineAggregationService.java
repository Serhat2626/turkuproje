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
import java.util.Optional;
import java.util.UUID;

/**
 * FP-016 Task 02: Read-only service that derives safe pipeline aggregate views
 * from existing review records, preview service, and internal readiness service.
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
 *
 * <p>APPROVED_FOR_INTERNAL_USE is internal editorial approval only.
 * INTERNAL_READY does NOT imply publication, export, or public delivery.</p>
 */
@Service
public class PipelineAggregationService {

    private static final Logger log = LoggerFactory.getLogger(PipelineAggregationService.class);

    /** Maximum items returned in a single overview query. */
    private static final int MAX_OVERVIEW_ITEMS = 200;

    private final ReviewRecordRepository reviewRecordRepository;
    private final ReviewAuditEventRepository auditEventRepository;
    private final InternalApprovalReadinessService readinessService;
    private final AdminPreviewService adminPreviewService;

    public PipelineAggregationService(ReviewRecordRepository reviewRecordRepository,
                                       ReviewAuditEventRepository auditEventRepository,
                                       InternalApprovalReadinessService readinessService,
                                       AdminPreviewService adminPreviewService) {
        this.reviewRecordRepository = reviewRecordRepository;
        this.auditEventRepository = auditEventRepository;
        this.readinessService = readinessService;
        this.adminPreviewService = adminPreviewService;
    }

    // ── Pipeline Overview ──────────────────────────────────────────

    /**
     * Get full pipeline overview with optional filters.
     * Read-only. No mutation. No provider/storage calls.
     *
     * @param targetTypeFilter optional target type filter (null = all)
     * @param stageFilter      optional pipeline stage filter (null = all)
     * @param limit            max items (capped at {@link #MAX_OVERVIEW_ITEMS})
     * @return safe pipeline overview with aggregate counts
     */
    @Transactional(readOnly = true)
    public SafePipelineOverview getOverview(ReviewTargetType targetTypeFilter,
                                            SafePipelineStage stageFilter,
                                            int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_OVERVIEW_ITEMS));
        List<String> warnings = new ArrayList<>();

        List<ReviewRecordEntity> records;
        try {
            records = findRecords(targetTypeFilter);
        } catch (Exception e) {
            log.warn("FP-016: Pipeline overview record query failed safely: {}", safeLogMessage(e));
            return SafePipelineOverview.builder()
                    .items(List.of())
                    .totalCount(0)
                    .countsByStage(Map.of())
                    .countsByTargetType(Map.of())
                    .safeWarnings(List.of("RECORD_QUERY_FAILED"))
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        List<SafePipelineAggregateItem> items = new ArrayList<>();
        for (ReviewRecordEntity record : records) {
            try {
                SafePipelineAggregateItem item = aggregateItem(record);
                if (stageFilter == null || item.getPipelineStage() == stageFilter) {
                    items.add(item);
                }
            } catch (Exception e) {
                log.warn("FP-016: Pipeline item aggregation failed safely for {}/{}: {}",
                        record.getReviewTargetType(), record.getReviewTargetId(),
                        safeLogMessage(e));
                items.add(SafePipelineAggregateItem.unavailable(
                        record.getReviewTargetType(), record.getReviewTargetId(),
                        "AGGREGATION_FAILED"));
                warnings.add("ITEM_DEGRADED:" + record.getReviewTargetType()
                        + "/" + record.getReviewTargetId());
            }
        }

        // Apply limit after filtering
        int totalBeforeLimit = items.size();
        if (items.size() > safeLimit) {
            items = items.subList(0, safeLimit);
            warnings.add("RESULT_TRUNCATED");
        }

        // Compute counts from full list (before limit)
        Map<SafePipelineStage, Integer> countsByStage = computeCountsByStage(items);
        Map<ReviewTargetType, Integer> countsByTargetType = computeCountsByTargetType(items);

        return SafePipelineOverview.builder()
                .items(items)
                .totalCount(totalBeforeLimit)
                .countsByStage(countsByStage)
                .countsByTargetType(countsByTargetType)
                .safeWarnings(warnings)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── Single Item Detail ─────────────────────────────────────────

    /**
     * Get pipeline aggregate detail for a single target.
     * Read-only. No mutation. No provider/storage calls.
     *
     * @param targetType target type (must not be null)
     * @param targetId   target ID (must not be null)
     * @return safe pipeline aggregate item
     */
    @Transactional(readOnly = true)
    public SafePipelineAggregateItem getItemDetail(ReviewTargetType targetType, UUID targetId) {
        if (targetType == null || targetId == null) {
            return SafePipelineAggregateItem.unavailable(targetType, targetId, "INVALID_TARGET");
        }

        Optional<ReviewRecordEntity> record =
                reviewRecordRepository.findByReviewTargetTypeAndReviewTargetId(targetType, targetId);

        if (record.isEmpty()) {
            return SafePipelineAggregateItem.unavailable(targetType, targetId, "NO_REVIEW_RECORD");
        }

        try {
            return aggregateItem(record.get());
        } catch (Exception e) {
            log.warn("FP-016: Pipeline item detail failed safely for {}/{}: {}",
                    targetType, targetId, safeLogMessage(e));
            return SafePipelineAggregateItem.unavailable(targetType, targetId, "AGGREGATION_FAILED");
        }
    }

    // ── Internal Aggregation Logic ─────────────────────────────────

    /**
     * Aggregate a single review record into a safe pipeline item.
     * Combines review state, preview, readiness, and revision detection.
     */
    SafePipelineAggregateItem aggregateItem(ReviewRecordEntity record) {
        ReviewTargetType targetType = record.getReviewTargetType();
        UUID targetId = record.getReviewTargetId();
        ReviewState reviewState = record.getCurrentState();

        // Preview availability (safe, no provider calls)
        boolean previewAvailable = checkPreviewAvailability(targetType, targetId);

        // Internal readiness (safe, read-only)
        InternalApprovalReadinessAvailability readinessAvailability =
                checkReadinessAvailability(targetType, targetId);

        // Revision detection (safe, from audit trail)
        boolean revisionRequested = detectRevisionRequested(record);

        // Derive pipeline stage
        SafePipelineStage stage = derivePipelineStage(
                reviewState, readinessAvailability, revisionRequested);

        // Safe blocking reasons
        List<String> blockingReasons = deriveBlockingReasons(
                stage, reviewState, previewAvailable, readinessAvailability);

        // Safe next hints
        List<String> nextHints = deriveNextHints(stage, readinessAvailability);

        // Safe metadata from record
        String safeTitle = ReviewTextSafety.sanitize(record.getReviewTargetType() + ":" + record.getReviewTargetId());
        String safeSummary = ReviewTextSafety.sanitize(record.getSafeMessage());

        return SafePipelineAggregateItem.builder()
                .targetType(targetType)
                .targetId(targetId)
                .pipelineStage(stage)
                .reviewState(reviewState)
                .previewAvailable(previewAvailable)
                .readinessAvailability(readinessAvailability)
                .revisionRequested(revisionRequested)
                .safeTitle(safeTitle)
                .safeSummary(safeSummary)
                .safeBlockingReasons(blockingReasons)
                .safeNextInternalHints(nextHints)
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    /**
     * Derive safe pipeline stage from review state, readiness, and revision status.
     *
     * <p>Stage derivation logic:</p>
     * <ul>
     *   <li>null/DRAFT/AI_GENERATED → GENERATED</li>
     *   <li>NEEDS_REVIEW → REVIEW_PENDING</li>
     *   <li>IN_REVIEW → IN_REVIEW</li>
     *   <li>REJECTED → REJECTED</li>
     *   <li>APPROVED_FOR_INTERNAL_USE + READY → INTERNAL_READY</li>
     *   <li>APPROVED_FOR_INTERNAL_USE → INTERNALLY_APPROVED</li>
     *   <li>CHANGES_REQUESTED + revisionRequested → INTERNAL_REVISION_REQUESTED</li>
     *   <li>CHANGES_REQUESTED → CHANGES_REQUESTED</li>
     * </ul>
     */
    SafePipelineStage derivePipelineStage(ReviewState reviewState,
                                            InternalApprovalReadinessAvailability readinessAvailability,
                                            boolean revisionRequested) {
        if (reviewState == null) {
            return SafePipelineStage.GENERATED;
        }

        return switch (reviewState) {
            case DRAFT, AI_GENERATED -> SafePipelineStage.GENERATED;
            case NEEDS_REVIEW -> SafePipelineStage.REVIEW_PENDING;
            case IN_REVIEW -> SafePipelineStage.IN_REVIEW;
            case REJECTED -> SafePipelineStage.REJECTED;
            case APPROVED_FOR_INTERNAL_USE -> {
                if (readinessAvailability == InternalApprovalReadinessAvailability.READY) {
                    yield SafePipelineStage.INTERNAL_READY;
                }
                yield SafePipelineStage.INTERNALLY_APPROVED;
            }
            case CHANGES_REQUESTED -> {
                if (revisionRequested) {
                    yield SafePipelineStage.INTERNAL_REVISION_REQUESTED;
                }
                yield SafePipelineStage.CHANGES_REQUESTED;
            }
        };
    }

    // ── Safe Data Retrieval ────────────────────────────────────────

    private boolean checkPreviewAvailability(ReviewTargetType targetType, UUID targetId) {
        try {
            var preview = adminPreviewService.getPreview(targetType, targetId);
            return preview != null && preview.isPreviewAvailable();
        } catch (Exception e) {
            log.debug("FP-016: Preview check failed safely for {}/{}", targetType, targetId);
            return false;
        }
    }

    private InternalApprovalReadinessAvailability checkReadinessAvailability(
            ReviewTargetType targetType, UUID targetId) {
        try {
            var readiness = readinessService.getReadiness(targetType, targetId);
            return readiness != null ? readiness.getAvailability()
                    : InternalApprovalReadinessAvailability.UNAVAILABLE;
        } catch (Exception e) {
            log.debug("FP-016: Readiness check failed safely for {}/{}", targetType, targetId);
            return InternalApprovalReadinessAvailability.UNAVAILABLE;
        }
    }

    /**
     * Detect if a revision was requested after internal approval.
     * Checks audit trail for REQUEST_INTERNAL_REVISION action after APPROVE_FOR_INTERNAL_USE.
     * If audit is unavailable, falls back to false (safe default).
     */
    boolean detectRevisionRequested(ReviewRecordEntity record) {
        if (record.getCurrentState() != ReviewState.CHANGES_REQUESTED) {
            return false;
        }

        // Check if last action was REQUEST_INTERNAL_REVISION
        if (record.getLastActionType() == ReviewActionType.REQUEST_INTERNAL_REVISION) {
            return true;
        }

        // Fallback: check audit trail for approval before current CHANGES_REQUESTED
        try {
            List<ReviewAuditEventEntity> events =
                    auditEventRepository.findByReviewRecordIdOrderByOccurredAtAsc(record.getId());
            boolean wasApproved = false;
            for (ReviewAuditEventEntity event : events) {
                if (event.getActionType() == ReviewActionType.APPROVE_FOR_INTERNAL_USE) {
                    wasApproved = true;
                }
                if (wasApproved && event.getActionType() == ReviewActionType.REQUEST_INTERNAL_REVISION) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("FP-016: Audit trail check failed safely for record {}", record.getId());
        }

        return false;
    }

    // ── Derived Blocking Reasons ───────────────────────────────────

    private List<String> deriveBlockingReasons(SafePipelineStage stage, ReviewState reviewState,
                                                boolean previewAvailable,
                                                InternalApprovalReadinessAvailability readinessAvailability) {
        List<String> reasons = new ArrayList<>();

        if (stage == SafePipelineStage.REVIEW_PENDING) {
            reasons.add("REVIEW_PENDING");
        }
        if (stage == SafePipelineStage.IN_REVIEW) {
            reasons.add("REVIEW_IN_PROGRESS");
        }
        if (stage == SafePipelineStage.CHANGES_REQUESTED) {
            reasons.add("CHANGES_REQUESTED");
        }
        if (stage == SafePipelineStage.INTERNAL_REVISION_REQUESTED) {
            reasons.add("INTERNAL_REVISION_REQUESTED");
        }
        if (stage == SafePipelineStage.REJECTED) {
            reasons.add("REJECTED");
        }
        if (stage == SafePipelineStage.UNAVAILABLE) {
            reasons.add("TARGET_UNAVAILABLE");
        }
        if (!previewAvailable && stage != SafePipelineStage.GENERATED
                && stage != SafePipelineStage.UNAVAILABLE) {
            reasons.add("PREVIEW_UNAVAILABLE");
        }
        if (readinessAvailability == InternalApprovalReadinessAvailability.NOT_APPROVED
                && reviewState != null && reviewState.isApprovedForInternalUse()) {
            reasons.add("READINESS_NOT_CONFIRMED");
        }

        return reasons;
    }

    // ── Derived Next Hints ─────────────────────────────────────────

    private List<String> deriveNextHints(SafePipelineStage stage,
                                          InternalApprovalReadinessAvailability readinessAvailability) {
        List<String> hints = new ArrayList<>();

        switch (stage) {
            case GENERATED -> hints.add("SUBMIT_FOR_REVIEW");
            case REVIEW_PENDING -> hints.add("AWAIT_REVIEWER");
            case IN_REVIEW -> hints.add("AWAIT_REVIEW_DECISION");
            case CHANGES_REQUESTED -> hints.add("ADDRESS_CHANGES");
            case INTERNALLY_APPROVED -> hints.add("CHECK_INTERNAL_READINESS");
            case INTERNAL_READY -> hints.add("INTERNAL_USE_AVAILABLE");
            case INTERNAL_REVISION_REQUESTED -> hints.add("ADDRESS_REVISION_REQUEST");
            case REJECTED -> hints.add("REVIEW_REJECTION_DETAILS");
            case UNAVAILABLE -> hints.add("INVESTIGATE_TARGET");
        }

        return hints;
    }

    // ── Internal Helpers ───────────────────────────────────────────

    private List<ReviewRecordEntity> findRecords(ReviewTargetType targetTypeFilter) {
        if (targetTypeFilter != null) {
            return reviewRecordRepository.findByReviewTargetType(targetTypeFilter);
        }
        return reviewRecordRepository.findAll();
    }

    private Map<SafePipelineStage, Integer> computeCountsByStage(List<SafePipelineAggregateItem> items) {
        Map<SafePipelineStage, Integer> counts = new EnumMap<>(SafePipelineStage.class);
        for (SafePipelineAggregateItem item : items) {
            counts.merge(item.getPipelineStage(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<ReviewTargetType, Integer> computeCountsByTargetType(List<SafePipelineAggregateItem> items) {
        Map<ReviewTargetType, Integer> counts = new EnumMap<>(ReviewTargetType.class);
        for (SafePipelineAggregateItem item : items) {
            if (item.getTargetType() != null) {
                counts.merge(item.getTargetType(), 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * Safe log message — strip potential stack traces and secrets.
     */
    private String safeLogMessage(Exception e) {
        if (e == null) return "unknown";
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        return ReviewTextSafety.sanitize(msg);
    }
}
