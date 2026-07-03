package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * FP-004: Tek bir platform için üretilmiş paket çıktısı.
 */
@Value
@Builder
public class PlatformPackageOutput {

    /** Platform tipi (ör. "YOUTUBE_LONG_FORM") */
    String packageType;

    /** Paket başlığı */
    String title;

    /** Platform açıklaması / caption */
    String captionOrDescription;

    /** Hashtag listesi */
    List<String> hashtags;

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

    // ── YouTube Long-Form metadata ──

    /** YT Long: SEO-optimize video başlığı */
    String videoTitle;

    /** YT Long: detaylı video açıklaması */
    String longDescription;

    /** YT Long: YouTube chapters listesi */
    List<String> chapterOutline;

    /** YT Long: sabitlenecek yorum */
    String pinnedCommentSuggestion;

    /** YT Long: thumbnail metin */
    String thumbnailTextSuggestion;

    /** YT Long: video etiketleri */
    List<String> tags;

    // ── Short-Form / Reels metadata ──

    /** Short/Reels: ilk 3 saniye hook */
    String shortHook;

    /** Short/Reels: kısa açıklama */
    String shortCaption;

    /** Short/Reels: ekran üzeri metinler */
    List<String> overlayTextSuggestions;

    /** Short/Reels: platform-spesifik hashtag seti */
    List<String> shortHashtagSet;

    /** Short/Reels: kısa CTA */
    String shortCta;

    /** Short/Reels: süre rehberi */
    String durationGuidance;

    // ── Scene usages ──

    /** Sahne kullanım detayları */
    List<PlatformPackageSceneUsageOutput> sceneUsages;
}
