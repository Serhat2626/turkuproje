package com.anadoluatlasi.backend.visualasset.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * FP-006: Thumbnail Plan entity.
 *
 * <p>Video thumbnail'i için görsel plan. VisualAssetPlan ile OneToOne ilişkide,
 * her plan için tek thumbnail planı.</p>
 *
 * <p>{@code thumbnailPrompt} alanı ürün çıktısıdır — admin tarafından görüntülenir,
 * düzenlenir ve onaylanır.</p>
 *
 * <p>Bu entity yalnızca prompt planlama metni saklar.
 * Thumbnail görsel dosyası, image URL, MediaAsset veya GeneratedAsset içermez.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "thumbnail_plans")
public class ThumbnailPlan extends BaseEntity {

    // ── Parent Reference ─────────────────────────────────────────────

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visual_asset_plan_id", nullable = false, unique = true)
    private VisualAssetPlan visualAssetPlan;

    // ── Source Reference (optional) ──────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_storyboard_scene_id")
    private StoryboardScene sourceStoryboardScene;

    @Column(name = "source_scene_number")
    private Integer sourceSceneNumber;

    // ── Thumbnail Prompt (product output) ────────────────────────────

    @Column(name = "thumbnail_prompt", nullable = false, columnDefinition = "TEXT")
    private String thumbnailPrompt;

    @Column(name = "negative_prompt", columnDefinition = "TEXT")
    private String negativePrompt;

    // ── Composition & Style ──────────────────────────────────────────

    @Column(name = "text_overlay_guidance", columnDefinition = "TEXT")
    private String textOverlayGuidance;

    @Column(name = "composition_notes", columnDefinition = "TEXT")
    private String compositionNotes;

    @Column(name = "emotional_hook", length = 500)
    private String emotionalHook;

    @Column(name = "aspect_ratio", length = 20)
    private String aspectRatio;

    @Column(name = "style_directive", length = 200)
    private String styleDirective;

    @Column(name = "color_palette", columnDefinition = "TEXT")
    private String colorPalette;
}
