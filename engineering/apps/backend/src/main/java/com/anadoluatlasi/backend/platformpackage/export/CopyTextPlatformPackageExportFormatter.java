package com.anadoluatlasi.backend.platformpackage.export;

import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import org.springframework.stereotype.Component;

/**
 * FP-005: Copy Text formatter.
 *
 * <p>Clipboard kullanımı için optimize edilmiş düz metin çıktı üretir.
 * Markdown formatting kullanmaz. Etiketli bölümler ile admin
 * istediği alanı kolayca bulup kopyalayabilir.</p>
 */
@Component
public class CopyTextPlatformPackageExportFormatter implements PlatformPackageExportFormatter {

    private static final String SEPARATOR = "══════════════════════════════════════════════\n";
    private static final String SECTION_PREFIX = "=== ";
    private static final String SECTION_SUFFIX = " ===\n";

    @Override
    public PlatformPackageExportType supports() {
        return PlatformPackageExportType.COPY_TEXT;
    }

    @Override
    public String format(PlatformPackageDraft draft, PlatformPackageExportMetadata metadata) {
        StringBuilder sb = new StringBuilder();

        // ── Header ──
        sb.append(SEPARATOR);
        sb.append("  PLATFORM PACKAGE EXPORT — COPY TEXT\n");
        sb.append("  ").append(draft.getPackageType().name()).append(" v").append(draft.getVersion()).append("\n");
        sb.append(SEPARATOR);
        sb.append("\n");

        // ── Title ──
        appendSection(sb, "BAŞLIK", draft.getTitle());

        // ── Platform-specific ──
        if (draft.getPackageType() == PlatformPackageType.YOUTUBE_LONG_FORM) {
            appendYoutubeLongFormSections(sb, draft);
        } else {
            appendShortFormSections(sb, draft);
        }

        // ── Common fields ──
        appendSection(sb, "AÇIKLAMA", draft.getCaptionOrDescription());

        String hashtags = JsonFieldParser.joinParsed(draft.getHashtagsJson());
        appendSection(sb, "HASHTAG'LER", hashtags);

        appendSection(sb, "CTA", draft.getCallToAction());

        // ── Scene Usage Notes ──
        appendSection(sb, "SAHNE KULLANIM NOTLARI", draft.getSceneUsageNotes());
        appendSection(sb, "İÇERİK YAPISI NOTLARI", draft.getContentStructureNotes());
        appendSection(sb, "REVIEW NOTLARI", draft.getReviewNotes());

        // ── Footer ──
        sb.append(SEPARATOR);
        sb.append("  Kaynak: ").append(metadata.getContentProjectId())
                .append(" / ").append(draft.getId()).append(" v").append(draft.getVersion()).append("\n");
        sb.append(SEPARATOR);

        return sb.toString();
    }

    // ── YouTube Long-Form ──

    private void appendYoutubeLongFormSections(StringBuilder sb, PlatformPackageDraft draft) {
        appendSection(sb, "VİDEO BAŞLIĞI", draft.getVideoTitle());
        appendSection(sb, "UZUN AÇIKLAMA", draft.getLongDescription());

        String chapters = JsonFieldParser.joinParsedLines(draft.getChapterOutlineJson());
        appendSection(sb, "BÖLÜMLER", chapters);

        String tags = JsonFieldParser.joinParsed(draft.getTagsJson());
        appendSection(sb, "ETİKETLER", tags);

        appendSection(sb, "SABİTLENECEK YORUM", draft.getPinnedCommentSuggestion());
        appendSection(sb, "THUMBNAIL METNİ", draft.getThumbnailTextSuggestion());
    }

    // ── Short-form / Reels ──

    private void appendShortFormSections(StringBuilder sb, PlatformPackageDraft draft) {
        appendSection(sb, "HOOK", draft.getShortHook());
        appendSection(sb, "KISA AÇIKLAMA", draft.getShortCaption());

        String overlays = JsonFieldParser.joinParsedLines(draft.getOverlayTextSuggestionsJson());
        appendSection(sb, "OVERLAY METİNLERİ", overlays);

        String shortHashtags = JsonFieldParser.joinParsed(draft.getShortHashtagSetJson());
        appendSection(sb, "HASHTAG SETİ", shortHashtags);

        appendSection(sb, "CTA (KISA)", draft.getShortCta());
        appendSection(sb, "SÜRE YÖNLENDİRMESİ", draft.getDurationGuidance());
    }

    // ── Helper ──

    private void appendSection(StringBuilder sb, String label, String content) {
        if (content != null && !content.isBlank()) {
            sb.append(SECTION_PREFIX).append(label).append(SECTION_SUFFIX);
            sb.append(content).append("\n\n");
        }
    }
}
