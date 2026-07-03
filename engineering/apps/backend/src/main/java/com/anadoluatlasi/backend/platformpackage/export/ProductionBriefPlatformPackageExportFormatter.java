package com.anadoluatlasi.backend.platformpackage.export;

import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FP-005: Production Brief formatter.
 *
 * <p>Editör, tasarımcı veya video editörü için yapılandırılmış handoff dokümanı üretir.
 * İçerik yapısı, sahne planı, copy-ready alanlar ve yayın kontrol listesi içerir.</p>
 */
@Component
public class ProductionBriefPlatformPackageExportFormatter implements PlatformPackageExportFormatter {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public PlatformPackageExportType supports() {
        return PlatformPackageExportType.PRODUCTION_BRIEF;
    }

    @Override
    public String format(PlatformPackageDraft draft, PlatformPackageExportMetadata metadata) {
        StringBuilder sb = new StringBuilder();

        // ── Header ──
        sb.append("# PRODUCTION BRIEF\n\n");

        // ── Project Info ──
        sb.append("## Proje Bilgileri\n\n");
        sb.append("| Özellik | Değer |\n");
        sb.append("|---------|-------|\n");
        sb.append("| Proje ID | ").append(metadata.getContentProjectId()).append(" |\n");
        sb.append("| Platform | ").append(draft.getPackageType().name()).append(" |\n");
        sb.append("| Paket Versiyonu | v").append(draft.getVersion()).append(" |\n");
        if (draft.getTargetDurationSeconds() != null) {
            sb.append("| Hedef Süre | ").append(draft.getTargetDurationSeconds()).append(" sn |\n");
        }
        if (hasContent(draft.getAspectRatio())) {
            sb.append("| Aspect Ratio | ").append(draft.getAspectRatio()).append(" |\n");
        }
        if (metadata.getApprovedBy() != null) {
            sb.append("| Onaylayan | ").append(metadata.getApprovedBy()).append(" |\n");
        }
        if (metadata.getApprovedAt() != null) {
            sb.append("| Onay Tarihi | ").append(metadata.getApprovedAt().format(DT_FORMAT)).append(" |\n");
        }
        sb.append("\n---\n\n");

        // ── Objective ──
        sb.append("## Amaç\n\n");
        sb.append("Bu brief, onaylanmış platform paketinin üretim sürecine teslimi için hazırlanmıştır. ");
        sb.append("İçerik operatörü, video editörü veya tasarımcı bu brief'i referans alarak ");
        sb.append("production workflow'u başlatabilir.\n\n---\n\n");

        // ── Content Title ──
        sb.append("## İçerik Başlığı\n\n");
        sb.append(draft.getTitle()).append("\n\n");

        // ── Content Summary ──
        if (hasContent(draft.getCaptionOrDescription())) {
            sb.append("## İçerik Özeti\n\n");
            sb.append(draft.getCaptionOrDescription()).append("\n\n");
        }
        if (draft.getPackageType() == PlatformPackageType.YOUTUBE_LONG_FORM
                && hasContent(draft.getLongDescription())) {
            sb.append(draft.getLongDescription()).append("\n\n");
        }

        // ── Content Structure ──
        if (hasContent(draft.getContentStructureNotes())) {
            sb.append("## İçerik Yapısı Notları\n\n");
            sb.append(draft.getContentStructureNotes()).append("\n\n---\n\n");
        }

        // ── Scene Plan ──
        appendScenePlan(sb, metadata.getSceneUsages(), draft.getSceneUsageNotes());

        // ── Copy-Ready Content ──
        sb.append("## Copy-Ready İçerik\n\n");
        if (draft.getPackageType() == PlatformPackageType.YOUTUBE_LONG_FORM) {
            appendCopyReadyYtLf(sb, draft);
        } else {
            appendCopyReadyShort(sb, draft);
        }

        // ── Editor Notes ──
        if (hasContent(draft.getReviewNotes())) {
            sb.append("## Editör / Tasarımcı Notları\n\n");
            sb.append(draft.getReviewNotes()).append("\n\n---\n\n");
        }

        // ── Publishing Checklist ──
        appendPublishingChecklist(sb, draft);

        // ── Traceability ──
        sb.append("## Kaynak Referansları\n\n");
        sb.append("| Referans | Değer |\n");
        sb.append("|----------|-------|\n");
        sb.append("| Content Project | ").append(metadata.getContentProjectId()).append(" |\n");
        sb.append("| Storyboard | ").append(metadata.getSourceStoryboardDraftId()).append(" |\n");
        sb.append("| Platform Package | ").append(draft.getId()).append(" v").append(draft.getVersion()).append(" |\n");
        sb.append("\n---\n\n");
        sb.append("*Bu production brief Anadolu Atlası platformu tarafından otomatik olarak oluşturulmuştur.*\n");

        return sb.toString();
    }

    // ── Scene Plan ──

    private void appendScenePlan(StringBuilder sb, List<PlatformPackageExportSceneUsageDto> usages,
                                  String sceneUsageNotes) {
        if ((usages == null || usages.isEmpty()) && !hasContent(sceneUsageNotes)) {
            return;
        }
        sb.append("## Sahne Planı\n\n");
        if (usages != null && !usages.isEmpty()) {
            sb.append("| # | Kullanım Tipi | Dahil | Adaptasyon Notu |\n");
            sb.append("|---|--------------|-------|-----------------|\n");
            for (PlatformPackageExportSceneUsageDto u : usages) {
                sb.append("| ").append(u.getSceneNumber())
                        .append(" | ").append(u.getUsageType())
                        .append(" | ").append(Boolean.TRUE.equals(u.getIncluded()) ? "✅" : "❌")
                        .append(" | ").append(u.getAdaptationNote() != null ? u.getAdaptationNote() : "—")
                        .append(" |\n");
            }
            sb.append("\n");
        }
        if (hasContent(sceneUsageNotes)) {
            sb.append("**Sahne Kullanım Notları:** ").append(sceneUsageNotes).append("\n\n");
        }
        sb.append("---\n\n");
    }

    // ── Copy-Ready YouTube Long-Form ──

    private void appendCopyReadyYtLf(StringBuilder sb, PlatformPackageDraft draft) {
        appendSubSection(sb, "Başlık", draft.getVideoTitle());
        appendSubSection(sb, "Açıklama", draft.getLongDescription());

        String hashtags = JsonFieldParser.joinParsed(draft.getHashtagsJson());
        appendSubSection(sb, "Hashtag'ler", hashtags);

        String tags = JsonFieldParser.joinParsed(draft.getTagsJson());
        appendSubSection(sb, "Etiketler", tags);

        appendSubSection(sb, "CTA", draft.getCallToAction());

        String chapters = JsonFieldParser.joinParsedLines(draft.getChapterOutlineJson());
        appendSubSection(sb, "Bölümler", chapters);

        appendSubSection(sb, "Sabitlenecek Yorum", draft.getPinnedCommentSuggestion());
        appendSubSection(sb, "Thumbnail Metni", draft.getThumbnailTextSuggestion());
        sb.append("---\n\n");
    }

    // ── Copy-Ready Short/Reels ──

    private void appendCopyReadyShort(StringBuilder sb, PlatformPackageDraft draft) {
        appendSubSection(sb, "Hook", draft.getShortHook());
        appendSubSection(sb, "Caption", draft.getShortCaption());

        String shortHashtags = JsonFieldParser.joinParsed(draft.getShortHashtagSetJson());
        appendSubSection(sb, "Hashtag'ler", shortHashtags);

        appendSubSection(sb, "CTA", draft.getShortCta());

        String overlays = JsonFieldParser.joinParsedLines(draft.getOverlayTextSuggestionsJson());
        appendSubSection(sb, "Overlay Metinleri", overlays);

        appendSubSection(sb, "Süre Yönlendirmesi", draft.getDurationGuidance());
        sb.append("---\n\n");
    }

    // ── Publishing Checklist ──

    private void appendPublishingChecklist(StringBuilder sb, PlatformPackageDraft draft) {
        sb.append("## Yayın Kontrol Listesi\n\n");

        if (draft.getPackageType() == PlatformPackageType.YOUTUBE_LONG_FORM) {
            sb.append("- [ ] Video başlığı kopyalandı\n");
            sb.append("- [ ] Açıklama kopyalandı\n");
            sb.append("- [ ] Hashtag'ler eklendi\n");
            sb.append("- [ ] Etiketler eklendi\n");
            sb.append("- [ ] Thumbnail hazırlandı\n");
            sb.append("- [ ] CTA metni eklendi\n");
            sb.append("- [ ] Bölüm zaman damgaları eklendi\n");
            sb.append("- [ ] Sabitlenecek yorum hazırlandı\n");
        } else {
            sb.append("- [ ] Hook metni hazırlandı\n");
            sb.append("- [ ] Caption kopyalandı\n");
            sb.append("- [ ] Hashtag'ler eklendi\n");
            sb.append("- [ ] CTA metni eklendi\n");
            sb.append("- [ ] Overlay metinleri eklendi\n");
        }
        sb.append("- [ ] Aspect ratio kontrol edildi");
        if (hasContent(draft.getAspectRatio())) {
            sb.append(" (").append(draft.getAspectRatio()).append(")");
        }
        sb.append("\n");
        if (draft.getTargetDurationSeconds() != null) {
            sb.append("- [ ] Süre kontrol edildi (").append(draft.getTargetDurationSeconds()).append(" sn)\n");
        }
        sb.append("\n---\n\n");
    }

    // ── Helpers ──

    private void appendSubSection(StringBuilder sb, String label, String content) {
        if (hasContent(content)) {
            sb.append("### ").append(label).append("\n");
            sb.append(content).append("\n\n");
        }
    }

    private boolean hasContent(String value) {
        return value != null && !value.isBlank();
    }
}
