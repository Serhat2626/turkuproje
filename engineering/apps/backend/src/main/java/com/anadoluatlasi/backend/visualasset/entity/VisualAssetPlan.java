package com.anadoluatlasi.backend.visualasset.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FP-006: Visual Asset Plan entity.
 *
 * <p>Bir APPROVED StoryboardDraft'tan üretilen görsel varlık planı.
 * Sahne bazlı image-generation-ready prompt'lar, stil rehberi, renk paleti,
 * karakter/mekan süreklilik notları içerir.</p>
 *
 * <p>Bu entity yalnızca prompt planlama metni saklar.
 * Görsel dosya, image URL, MediaAsset veya GeneratedAsset referansı içermez.</p>
 *
 * <p>İlk fazda gerçek görsel üretimi (DALL-E, Midjourney, Stable Diffusion) yapılmaz.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "visual_asset_plans")
public class VisualAssetPlan extends BaseEntity {

    // ── Source References ─────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_project_id", nullable = false)
    private ContentProject contentProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_storyboard_draft_id", nullable = false)
    private StoryboardDraft sourceStoryboardDraft;

    // ── Version & Status ─────────────────────────────────────────────

    @Column(name = "version", nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VisualAssetPlanStatus status;

    // ── General Visual Style ─────────────────────────────────────────

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "overall_visual_style", columnDefinition = "TEXT")
    private String overallVisualStyle;

    @Column(name = "aspect_ratio_strategy", length = 100)
    private String aspectRatioStrategy;

    @Column(name = "cultural_authenticity_guidance", columnDefinition = "TEXT")
    private String culturalAuthenticityGuidance;

    @Column(name = "character_continuity_guidance", columnDefinition = "TEXT")
    private String characterContinuityGuidance;

    @Column(name = "location_continuity_guidance", columnDefinition = "TEXT")
    private String locationContinuityGuidance;

    @Column(name = "color_palette_guidance", columnDefinition = "TEXT")
    private String colorPaletteGuidance;

    // ── AI Metadata (safe fields only) ───────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 50)
    private GeneratedBy generatedBy;

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    // ── Review Lifecycle ─────────────────────────────────────────────

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "edited_by", length = 100)
    private String editedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ── Children ─────────────────────────────────────────────────────

    @Builder.Default
    @OneToMany(mappedBy = "visualAssetPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sceneNumber ASC")
    private List<SceneVisualPlan> scenePlans = new ArrayList<>();

    @OneToOne(mappedBy = "visualAssetPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ThumbnailPlan thumbnailPlan;
}
