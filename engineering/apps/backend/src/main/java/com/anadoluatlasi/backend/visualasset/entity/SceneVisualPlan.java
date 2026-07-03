package com.anadoluatlasi.backend.visualasset.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * FP-006: Scene Visual Plan entity.
 *
 * <p>Bir storyboard sahnesi için detaylı görsel plan ve image-generation-ready prompt.
 * VisualAssetPlan'ın child entity'sidir.</p>
 *
 * <p>{@code englishImagePrompt} ve {@code turkishVisualContext} alanları ürün çıktısıdır —
 * admin tarafından görüntülenir, düzenlenir ve onaylanır.</p>
 *
 * <p>Bu entity yalnızca prompt planlama metni saklar.
 * Image URL, dosya referansı, MediaAsset veya GeneratedAsset içermez.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scene_visual_plans")
public class SceneVisualPlan extends BaseEntity {

    // ── Parent Reference ─────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visual_asset_plan_id", nullable = false)
    private VisualAssetPlan visualAssetPlan;

    // ── Source Reference ─────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_storyboard_scene_id")
    private StoryboardScene sourceStoryboardScene;

    @Column(name = "scene_number", nullable = false)
    private Integer sceneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_beat_type", length = 50)
    private ScenarioBeatType sourceBeatType;

    @Column(name = "scene_title", length = 500)
    private String sceneTitle;

    // ── Image Prompt (product output) ────────────────────────────────

    @Column(name = "english_image_prompt", nullable = false, columnDefinition = "TEXT")
    private String englishImagePrompt;

    @Column(name = "turkish_visual_context", nullable = false, columnDefinition = "TEXT")
    private String turkishVisualContext;

    @Column(name = "negative_prompt", columnDefinition = "TEXT")
    private String negativePrompt;

    // ── Style & Composition ──────────────────────────────────────────

    @Column(name = "style_directive", length = 200)
    private String styleDirective;

    @Column(name = "aspect_ratio", length = 20)
    private String aspectRatio;

    @Column(name = "composition_notes", columnDefinition = "TEXT")
    private String compositionNotes;

    @Column(name = "camera_framing", length = 200)
    private String cameraFraming;

    @Column(name = "lighting_notes", columnDefinition = "TEXT")
    private String lightingNotes;

    // ── Mood & Color ─────────────────────────────────────────────────

    @Column(name = "color_palette", columnDefinition = "TEXT")
    private String colorPalette;

    @Column(name = "mood", length = 200)
    private String mood;

    // ── Content ──────────────────────────────────────────────────────

    @Column(name = "location", columnDefinition = "TEXT")
    private String location;

    @Column(name = "characters", columnDefinition = "TEXT")
    private String characters;

    @Column(name = "cultural_details", columnDefinition = "TEXT")
    private String culturalDetails;

    // ── Overlay & Tool Notes ─────────────────────────────────────────

    @Column(name = "text_overlay_guidance", columnDefinition = "TEXT")
    private String textOverlayGuidance;

    @Column(name = "image_generation_tool_notes", columnDefinition = "TEXT")
    private String imageGenerationToolNotes;

    @Column(name = "safety_notes", columnDefinition = "TEXT")
    private String safetyNotes;
}
