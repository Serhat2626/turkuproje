package com.anadoluatlasi.backend.review;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * FP-013 Task 03: Internal service that creates safe admin preview DTOs
 * for review targets.
 *
 * <p>This service is internal only — NOT exposed via controller/API endpoint yet.
 * NOT wired into frontend yet.</p>
 *
 * <p>Critical rules:</p>
 * <ul>
 *   <li>Never exposes provider URLs, storage keys, raw payloads, base64</li>
 *   <li>Never creates public or signed URLs</li>
 *   <li>Never implies approval — preview is read-only context</li>
 *   <li>Never publishes or exports</li>
 *   <li>No storage/provider calls</li>
 *   <li>APPROVED_FOR_INTERNAL_USE preview remains internal only</li>
 * </ul>
 */
@Service
public class AdminPreviewService {

    private final ReviewTargetContextResolver contextResolver;

    public AdminPreviewService(ReviewTargetContextResolver contextResolver) {
        this.contextResolver = contextResolver;
    }

    /**
     * Get safe admin preview for a review target.
     *
     * <p>Does not call storage/provider. Does not create URLs.
     * Does not return raw payload, base64, or secrets.</p>
     *
     * @param targetType review target type (must not be null)
     * @param targetId   target entity ID (must not be null)
     * @return safe admin preview DTO
     */
    @Transactional(readOnly = true)
    public SafeAdminPreview getPreview(ReviewTargetType targetType, UUID targetId) {
        if (targetType == null) {
            return SafeAdminPreview.unavailable(null, targetId,
                    AdminPreviewAvailability.NOT_REVIEWABLE,
                    "PREVIEW_TARGET_TYPE_NULL", "Hedef türü belirtilmedi");
        }
        if (targetId == null) {
            return SafeAdminPreview.unavailable(targetType, null,
                    AdminPreviewAvailability.MISSING_TARGET,
                    "PREVIEW_TARGET_ID_NULL", "Hedef kimliği belirtilmedi");
        }

        var resolved = contextResolver.resolve(targetType, targetId);

        if (resolved.isEmpty()) {
            return SafeAdminPreview.unavailable(targetType, targetId,
                    AdminPreviewAvailability.MISSING_TARGET,
                    "PREVIEW_TARGET_NOT_FOUND", "İnceleme kaydı bulunamadı");
        }

        return buildPreview(resolved.get());
    }

    // ── Internal ────────────────────────────────────────────────────

    private SafeAdminPreview buildPreview(ReviewTargetContextResolver.ResolvedTargetContext ctx) {
        AdminPreviewMode mode = ctx.getPreviewMode();
        boolean previewAvailable = (mode != AdminPreviewMode.UNAVAILABLE);

        // IMAGE_ASSET: safe availability label only — no actual bytes/URL in this foundation
        boolean thumbnailAvailable = false;
        String previewLabel = buildSafePreviewLabel(mode, ctx.getCurrentState());

        return SafeAdminPreview.builder()
                .targetType(ctx.getTargetType())
                .targetId(ctx.getTargetId())
                .previewMode(mode)
                .availability(previewAvailable
                        ? AdminPreviewAvailability.AVAILABLE
                        : AdminPreviewAvailability.UNAVAILABLE)
                .previewAvailable(previewAvailable)
                .safeTitle(ctx.getSafeTitle())
                .safeSummary(ctx.getSafeSummary())
                .safeMessage(null)
                .safeReasonCode(null)
                .safePreviewLabel(previewLabel)
                .safeThumbnailAvailable(thumbnailAvailable)
                .currentReviewState(ctx.getCurrentState())
                .updatedAt(ctx.getUpdatedAt())
                .build();
    }

    private String buildSafePreviewLabel(AdminPreviewMode mode, ReviewState state) {
        String stateLabel = switch (state) {
            case DRAFT -> "Taslak";
            case AI_GENERATED -> "AI Üretildi";
            case NEEDS_REVIEW -> "İnceleme Bekliyor";
            case IN_REVIEW -> "İnceleniyor";
            case CHANGES_REQUESTED -> "Değişiklik İstendi";
            case APPROVED_FOR_INTERNAL_USE -> "Dahili Onaylı (Sadece İç Kullanım)";
            case REJECTED -> "Reddedildi";
        };

        String modeLabel = switch (mode) {
            case TEXT_SUMMARY -> "Metin Özeti";
            case SCENARIO_SUMMARY -> "Senaryo Özeti";
            case STORYBOARD_SUMMARY -> "Storyboard Özeti";
            case PACKAGE_SUMMARY -> "Paket Özeti";
            case IMAGE_ASSET -> "Görsel Bilgisi";
            case UNAVAILABLE -> "Kullanılamıyor";
        };

        return modeLabel + " — " + stateLabel;
    }
}
