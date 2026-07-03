package com.anadoluatlasi.backend.platformpackage.export;

import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FP-005: Markdown Brief formatter.
 *
 * <p>İnsan-okunabilir, bölümlere ayrılmış markdown çıktı üretir.
 * Platform-spesifik alanlar paket tipine göre seçilir.
 * Null optional alanlar atlanır.</p>
 */
@Component
public class MarkdownBriefPlatformPackageExportFormatter implements PlatformPackageExportFormatter {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public PlatformPackageExportType supports() {
        return PlatformPackageExportType.MARKDOWN_BRIEF;
    }

    @Override
    public String format(PlatformPackageDraft draft, PlatformPackageExportMetadata metadata) {
        StringBuilder sb = new StringBuilder();

        // ── Header ──
        sb.append("# ").append(draft.getTitle()).append("\n\n");

        // ── Genel Bilgiler ──
        sb.append("## Genel Bilgiler\n\n");
        sb.append("| Özellik | Değer |\n");
        sb.append("|---------|-------|\n");
        sb.append("| Platform | ").append(draft.getPackageType().name()).append(" |\n");
        sb.append("| Versiyon | ").append(draft.getVersion()).append(" |\n");
        appendTableRowIfPresent(sb, "Aspect Ratio", draft.getAspectRatio());
        if (draft.getTargetDurationSeconds() != null) {
            sb.append("| Hedef Süre | ").append(draft.getTargetDurationSeconds()).append(" sn |\n");
        }
        appendTableRowIfPresent(sb, "Onaylayan", metadata.getApprovedBy());
        if (metadata.getApprovedAt() != null) {
            sb.append("| Onay Tarihi | ").append(metadata.getApprovedAt().format(DT_FORMAT)).append(" |\n");
        }
        sb.append("\n---\n\n");

        // ── Platform-Specific Content ──
        if (draft.getPackageType() == PlatformPackageType.YOUTUBE_LONG_FORM) {
            appendYoutubeLongFormSections(sb, draft);
        } else {
            appendShortFormSections(sb, draft);
        }

        // ── Common: Caption/Description ──
        appendSectionIfPresent(sb, "Açıklama / Caption", draft.getCaptionOrDescription());

        // ── Common: Hashtags ──
        String hashtags = JsonFieldParser.joinParsed(draft.getHashtagsJson());
        appendSectionIfPresent(sb, "Hashtag'ler", hashtags);

        // ── Common: CTA ──
        appendSectionIfPresent(sb, "CTA", draft.getCallToAction());

        // ── Scene Usage ──
        appendSceneUsages(sb, metadata.getSceneUsages());

        // ── Production Notes ──
        boolean hasNotes = hasContent(draft.getContentStructureNotes())
                || hasContent(draft.getSceneUsageNotes())
                || hasContent(draft.getReviewNotes());
        if (hasNotes) {
            sb.append("## Production Notları\n\n");
            appendTextIfPresent(sb, draft.getContentStructureNotes());
            appendTextIfPresent(sb, draft.getSceneUsageNotes());
            appendTextIfPresent(sb, draft.getReviewNotes());
            sb.append("\n---\n\n");
        }

        // ── Traceability Footer ──
        appendTraceabilityFooter(sb, draft, metadata);

        return sb.toString();
    }

    // ── YouTube Long-Form sections ──

    private void appendYoutubeLongFormSections(StringBuilder sb, PlatformPackageDraft draft) {
        appendSectionIfPresent(sb, "Video Başlığı", draft.getVideoTitle());
        appendSectionIfPresent(sb, "Uzun Açıklama", draft.getLongDescription());

        String chapters = JsonFieldParser.joinParsedLines(draft.getChapterOutlineJson());
        appendSectionIfPresent(sb, "Bölüm Zaman Damgaları", chapters);

        String tags = JsonFieldParser.joinParsed(draft.getTagsJson());
        appendSectionIfPresent(sb, "Etiketler", tags);

        appendSectionIfPresent(sb, "Sabitlenecek Yorum", draft.getPinnedCommentSuggestion());
        appendSectionIfPresent(sb, "Thumbnail Metni", draft.getThumbnailTextSuggestion());
    }

    // ── Short-form / Reels sections ──

    private void appendShortFormSections(StringBuilder sb, PlatformPackageDraft draft) {
        appendSectionIfPresent(sb, "Hook", draft.getShortHook());
        appendSectionIfPresent(sb, "Kısa Açıklama", draft.getShortCaption());

        String overlays = JsonFieldParser.joinParsedLines(draft.getOverlayTextSuggestionsJson());
        appendSectionIfPresent(sb, "Overlay Text Önerileri", overlays);

        String shortHashtags = JsonFieldParser.joinParsed(draft.getShortHashtagSetJson());
        appendSectionIfPresent(sb, "Hashtag Seti", shortHashtags);

        appendSectionIfPresent(sb, "CTA (Kısa)", draft.getShortCta());
        appendSectionIfPresent(sb, "Süre Yönlendirmesi", draft.getDurationGuidance());
    }

    // ── Scene Usage Table ──

    private void appendSceneUsages(StringBuilder sb, List<PlatformPackageExportSceneUsageDto> usages) {
        if (usages == null || usages.isEmpty()) {
            return;
        }
        sb.append("## Sahne Kullanımı\n\n");
        sb.append("| # | Tip | Dahil | Adaptasyon Notu |\n");
        sb.append("|---|-----|-------|-----------------|\n");
        for (PlatformPackageExportSceneUsageDto u : usages) {
            sb.append("| ").append(u.getSceneNumber())
                    .append(" | ").append(u.getUsageType())
                    .append(" | ").append(Boolean.TRUE.equals(u.getIncluded()) ? "✅" : "❌")
                    .append(" | ").append(u.getAdaptationNote() != null ? u.getAdaptationNote() : "—")
                    .append(" |\n");
        }
        sb.append("\n---\n\n");
    }

    // ── Traceability Footer ──

    private void appendTraceabilityFooter(StringBuilder sb, PlatformPackageDraft draft,
                                          PlatformPackageExportMetadata metadata) {
        sb.append("## Kaynak Referansları\n\n");
        sb.append("| Referans | Değer |\n");
        sb.append("|----------|-------|\n");
        sb.append("| Content Project | ").append(metadata.getContentProjectId()).append(" |\n");
        sb.append("| Source Storyboard | ").append(metadata.getSourceStoryboardDraftId()).append(" |\n");
        sb.append("| Platform Package | ").append(draft.getId()).append(" |\n");
        sb.append("| Package Type | ").append(draft.getPackageType().name()).append(" |\n");
        sb.append("| Version | ").append(draft.getVersion()).append(" |\n");
        sb.append("\n---\n\n");
        sb.append("*Bu export Anadolu Atlası platformu tarafından otomatik olarak oluşturulmuştur.*\n");
    }

    // ── Helpers ──

    private void appendSectionIfPresent(StringBuilder sb, String heading, String content) {
        if (hasContent(content)) {
            sb.append("## ").append(heading).append("\n\n");
            sb.append(content).append("\n\n---\n\n");
        }
    }

    private void appendTableRowIfPresent(StringBuilder sb, String label, String value) {
        if (hasContent(value)) {
            sb.append("| ").append(label).append(" | ").append(value).append(" |\n");
        }
    }

    private void appendTextIfPresent(StringBuilder sb, String text) {
        if (hasContent(text)) {
            sb.append(text).append("\n\n");
        }
    }

    private boolean hasContent(String value) {
        return value != null && !value.isBlank();
    }
}
