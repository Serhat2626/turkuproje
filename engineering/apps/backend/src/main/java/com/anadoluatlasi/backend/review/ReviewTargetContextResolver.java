package com.anadoluatlasi.backend.review;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * FP-013 Task 03: Resolves safe target context for preview based on
 * ReviewTargetType + targetId.
 *
 * <p>Uses existing ReviewRecordEntity data only. No external calls,
 * no storage/provider calls, no raw payload access, no provider URL,
 * no storage key, no base64, no public/signed URL, no publish/export.</p>
 *
 * <p>This is a foundation — actual domain entity lookup (e.g., ScenarioDraft,
 * StoryboardDraft) will be added in later tasks when safe integration points
 * are established.</p>
 */
@Service
public class ReviewTargetContextResolver {

    private final ReviewRecordRepository reviewRecordRepository;

    public ReviewTargetContextResolver(ReviewRecordRepository reviewRecordRepository) {
        this.reviewRecordRepository = reviewRecordRepository;
    }

    /**
     * Resolve safe target context from existing review record.
     *
     * <p>Returns generic safe context derived from ReviewRecordEntity.
     * Does not call storage/provider/external systems.</p>
     *
     * @param targetType review target type
     * @param targetId   target entity ID
     * @return resolved context, or empty if no review record exists
     */
    @Transactional(readOnly = true)
    public Optional<ResolvedTargetContext> resolve(ReviewTargetType targetType, UUID targetId) {
        if (targetType == null || targetId == null) {
            return Optional.empty();
        }

        return reviewRecordRepository
                .findByReviewTargetTypeAndReviewTargetId(targetType, targetId)
                .map(record -> ResolvedTargetContext.builder()
                        .targetType(record.getReviewTargetType())
                        .targetId(record.getReviewTargetId())
                        .currentState(record.getCurrentState())
                        .safeTitle(buildSafeTitle(record))
                        .safeSummary(buildSafeSummary(record))
                        .previewMode(resolvePreviewMode(record.getReviewTargetType()))
                        .updatedAt(record.getUpdatedAt())
                        .build());
    }

    // ── Safe title/summary builders ────────────────────────────────

    private String buildSafeTitle(ReviewRecordEntity record) {
        String prefix = switch (record.getReviewTargetType()) {
            case CONTENT_PROJECT -> "Proje";
            case SCENARIO -> "Senaryo";
            case STORYBOARD -> "Storyboard";
            case PACKAGE -> "Paket";
            case IMAGE_ASSET -> "Görsel";
        };
        return ReviewTextSafety.sanitize(prefix + ": " + record.getReviewTargetId());
    }

    private String buildSafeSummary(ReviewRecordEntity record) {
        return ReviewTextSafety.sanitize(
                record.getReviewTargetType().name() + " — Durum: " + record.getCurrentState().name());
    }

    private AdminPreviewMode resolvePreviewMode(ReviewTargetType targetType) {
        return switch (targetType) {
            case CONTENT_PROJECT -> AdminPreviewMode.TEXT_SUMMARY;
            case SCENARIO -> AdminPreviewMode.SCENARIO_SUMMARY;
            case STORYBOARD -> AdminPreviewMode.STORYBOARD_SUMMARY;
            case PACKAGE -> AdminPreviewMode.PACKAGE_SUMMARY;
            case IMAGE_ASSET -> AdminPreviewMode.IMAGE_ASSET;
        };
    }

    // ── Resolved context value object ──────────────────────────────

    @lombok.Getter
    @lombok.Builder
    public static class ResolvedTargetContext {
        private final ReviewTargetType targetType;
        private final UUID targetId;
        private final ReviewState currentState;
        private final String safeTitle;
        private final String safeSummary;
        private final AdminPreviewMode previewMode;
        private final java.time.LocalDateTime updatedAt;

        @Override
        public String toString() {
            return "ResolvedTargetContext{" +
                    "targetType=" + targetType +
                    ", targetId=" + targetId +
                    ", currentState=" + currentState +
                    ", previewMode=" + previewMode +
                    '}';
        }
    }
}
