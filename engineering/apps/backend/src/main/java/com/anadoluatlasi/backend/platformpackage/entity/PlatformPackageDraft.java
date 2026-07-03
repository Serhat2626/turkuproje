package com.anadoluatlasi.backend.platformpackage.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FP-004: Platform-spesifik içerik paketi taslağı entity'si.
 *
 * <p>Onaylanmış bir StoryboardDraft'tan üretilen, tek bir platform için
 * optimize edilmiş içerik paketi taslağını temsil eder.</p>
 *
 * <p>Platform-spesifik metadata alanları (YouTube Long-form chapters,
 * Shorts/Reels hook, overlay text vb.) doğrudan bu entity'de flat olarak
 * saklanır. Bu yaklaşım sorgu performansı, CRUD basitliği ve lazy loading
 * riski olmayan bir yapı sağlar.</p>
 *
 * <p>Governance kuralları:</p>
 * <ul>
 *   <li>AI çıktısı hiçbir zaman APPROVED olmaz.</li>
 *   <li>Onay/red explicit admin eylemi gerektirir.</li>
 *   <li>Raw provider payload veya prompt saklanmaz.</li>
 *   <li>Gerçek media dosyası (image/video/audio) oluşturulmaz veya saklanmaz.</li>
 *   <li>Publishing bilgisi saklanmaz.</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "platform_package_drafts")
public class PlatformPackageDraft extends BaseEntity {

    // ── Source References ────────────────────────────────────────

    /** Bu paketin ait olduğu içerik projesi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_project_id", nullable = false)
    private ContentProject contentProject;

    /** Kaynak approved storyboard draft */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_storyboard_draft_id", nullable = false)
    private StoryboardDraft sourceStoryboardDraft;

    // ── Package Identity ────────────────────────────────────────

    /** Platform tipi */
    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 30)
    private PlatformPackageType packageType;

    /** Paket yaşam döngüsü durumu */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlatformPackageStatus status;

    /** Aynı proje + storyboard + platform tipi içinde versiyon numarası */
    @Column(name = "version", nullable = false)
    private Integer version;

    // ── Common Package Fields ───────────────────────────────────

    /** Paket başlığı (platform-optimize) */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** Platform açıklaması / caption */
    @Column(name = "caption_or_description", nullable = false, columnDefinition = "TEXT")
    private String captionOrDescription;

    /** Hashtag listesi (JSON array string) */
    @Column(name = "hashtags_json", columnDefinition = "TEXT")
    private String hashtagsJson;

    /** Call-to-action metni */
    @Column(name = "call_to_action", length = 500)
    private String callToAction;

    /** Hedef süre (saniye) */
    @Column(name = "target_duration_seconds")
    private Integer targetDurationSeconds;

    /** Aspect ratio (ör. "16:9", "9:16") */
    @Column(name = "aspect_ratio", length = 10)
    private String aspectRatio;

    /** İçerik yapısı notları */
    @Column(name = "content_structure_notes", columnDefinition = "TEXT")
    private String contentStructureNotes;

    /** Sahne kullanım rehberi */
    @Column(name = "scene_usage_notes", columnDefinition = "TEXT")
    private String sceneUsageNotes;

    /** Review notları */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    // ── YouTube Long-Form Metadata ──────────────────────────────

    /** YT Long: SEO-optimize video başlığı */
    @Column(name = "video_title", length = 255)
    private String videoTitle;

    /** YT Long: detaylı video açıklaması */
    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    /** YT Long: YouTube chapters listesi (JSON array string) */
    @Column(name = "chapter_outline_json", columnDefinition = "TEXT")
    private String chapterOutlineJson;

    /** YT Long: sabitlenecek yorum önerisi */
    @Column(name = "pinned_comment_suggestion", columnDefinition = "TEXT")
    private String pinnedCommentSuggestion;

    /** YT Long: thumbnail metin önerisi */
    @Column(name = "thumbnail_text_suggestion", length = 100)
    private String thumbnailTextSuggestion;

    /** YT Long: video etiketleri (JSON array string) */
    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    // ── Short-Form / Reels Metadata ─────────────────────────────

    /** Short/Reels: ilk 3 saniye hook metni */
    @Column(name = "short_hook", length = 500)
    private String shortHook;

    /** Short/Reels: kısa açıklama */
    @Column(name = "short_caption", columnDefinition = "TEXT")
    private String shortCaption;

    /** Short/Reels: ekran üzeri metin önerileri (JSON array string) */
    @Column(name = "overlay_text_suggestions_json", columnDefinition = "TEXT")
    private String overlayTextSuggestionsJson;

    /** Short/Reels: platform-spesifik hashtag seti (JSON array string) */
    @Column(name = "short_hashtag_set_json", columnDefinition = "TEXT")
    private String shortHashtagSetJson;

    /** Short/Reels: kısa CTA */
    @Column(name = "short_cta", length = 500)
    private String shortCta;

    /** Short/Reels: süre rehberi */
    @Column(name = "duration_guidance", length = 100)
    private String durationGuidance;

    // ── AI Metadata ─────────────────────────────────────────────

    /** Provider tipi (ör. "OPENAI", "MOCK") */
    @Column(name = "provider_type", length = 20)
    private String providerType;

    /** Model adı (ör. "gpt-4o-mini") */
    @Column(name = "model_name", length = 50)
    private String modelName;

    /** Üretici */
    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", length = 30)
    private GeneratedBy generatedBy;

    // ── Review Lifecycle ────────────────────────────────────────

    /** Son düzenleme zamanı */
    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    /** Düzenleyen admin */
    @Column(name = "edited_by", length = 100)
    private String editedBy;

    /** Onay zamanı */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Onaylayan admin */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    /** Red zamanı */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    /** Reddeden admin */
    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    /** Red gerekçesi */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ── Scene Usages ────────────────────────────────────────────

    /** Sahne kullanım detayları */
    @Builder.Default
    @OneToMany(mappedBy = "platformPackageDraft", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sceneNumber ASC")
    private List<PlatformPackageSceneUsage> sceneUsages = new ArrayList<>();
}
