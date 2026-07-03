package com.anadoluatlasi.backend.application.platformpackage;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * FP-004: Platform paketi taslağı düzenleme komut nesnesi.
 * Service katmanı DTO'sudur — REST API'de henüz kullanılmaz.
 *
 * <p>Null alanlar güncellenmez (partial update semantiği).</p>
 * <p>Provider metadata, source references, status, version doğrudan düzenlenemez.</p>
 */
@Value
@Builder
public class PlatformPackageDraftEditCommand {

    // ── Common Fields ──

    /** Paket başlığı */
    String title;

    /** Platform açıklaması / caption */
    String captionOrDescription;

    /** Hashtag listesi (JSON array) */
    String hashtagsJson;

    /** Call-to-action metni */
    String callToAction;

    /** Hedef süre (saniye) */
    Integer targetDurationSeconds;

    /** Aspect ratio */
    String aspectRatio;

    /** İçerik yapısı notları */
    String contentStructureNotes;

    /** Sahne kullanım rehberi */
    String sceneUsageNotes;

    /** Admin review notları */
    String reviewNotes;

    // ── YouTube Long-Form Metadata ──

    /** YT Long: SEO-optimize video başlığı */
    String videoTitle;

    /** YT Long: detaylı video açıklaması */
    String longDescription;

    /** YT Long: YouTube chapters (JSON array) */
    String chapterOutlineJson;

    /** YT Long: sabitlenecek yorum */
    String pinnedCommentSuggestion;

    /** YT Long: thumbnail metin */
    String thumbnailTextSuggestion;

    // ── Short-Form / Reels Metadata ──

    /** Short/Reels: ilk 3 saniye hook */
    String shortHook;

    /** Short/Reels: kısa açıklama */
    String shortCaption;

    /** Short/Reels: ekran üzeri metinler (JSON array) */
    String overlayTextSuggestionsJson;

    /** Short/Reels: platform-spesifik hashtag seti (JSON array) */
    String shortHashtagSetJson;

    /** Short/Reels: kısa CTA */
    String shortCta;

    /** Short/Reels: süre rehberi */
    String durationGuidance;

    // ── Scene Usage Edits ──

    /** Sahne kullanım düzenlemeleri (opsiyonel) */
    List<PlatformPackageSceneUsageEditCommand> sceneUsages;
}
