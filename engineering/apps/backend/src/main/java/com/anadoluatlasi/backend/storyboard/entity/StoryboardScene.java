package com.anadoluatlasi.backend.storyboard.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * FP-003: Storyboard scene entity'si.
 *
 * <p>Bir StoryboardDraft içindeki tekil sahneyi temsil eder.
 * Her scene, kaynak ScenarioBeat'e referans verir ve sahne bazlı
 * görsel planlama bilgisi içerir.</p>
 *
 * <p>Scene içeriği yalnızca açıklayıcı (descriptive) metindir.
 * FP-003 gerçek image/video/audio üretmez.
 * {@code mediaGenerationNotes} yalnızca gelecek media workflow handoff metadata'sıdır.</p>
 *
 * <p>Source beat type olarak mevcut {@link ScenarioBeatType} enum'u
 * yeniden kullanılır — beat tipleri FP-002'de tanımlı ve sabittir,
 * storyboard scene'ler doğrudan bu beat'lere referans verir.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "storyboard_scenes")
public class StoryboardScene extends BaseEntity {

    /** Bu scene'in ait olduğu storyboard taslağı */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storyboard_draft_id", nullable = false)
    private StoryboardDraft storyboardDraft;

    /** Sıra numarası (1-based, deterministik) */
    @Column(name = "scene_number", nullable = false)
    private Integer sceneNumber;

    /** Kaynak beat tipi (HOOK, INTRODUCTION, vb.) — ScenarioBeatType reuse */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_beat_type", nullable = false, length = 50)
    private ScenarioBeatType sourceBeatType;

    /** Kaynak beat sıra numarası */
    @Column(name = "source_beat_order", nullable = false)
    private Integer sourceBeatOrder;

    /** Sahne başlığı */
    @Column(name = "scene_title", nullable = false, length = 500)
    private String sceneTitle;

    /** Sahne görsel açıklaması — ne gösterilecek */
    @Column(name = "visual_description", nullable = false, columnDefinition = "TEXT")
    private String visualDescription;

    /** Voiceover / narration metni */
    @Column(name = "narration_text", columnDefinition = "TEXT")
    private String narrationText;

    /** Ekranda gösterilecek metin (alt yazı, başlık vb.) */
    @Column(name = "on_screen_text", columnDefinition = "TEXT")
    private String onScreenText;

    /** Kamera hareketi / açısı (wide shot, close-up, pan, zoom) */
    @Column(name = "camera_direction", length = 500)
    private String cameraDirection;

    /** Sahne geçiş notu (fade, cut, dissolve) */
    @Column(name = "transition_note", length = 200)
    private String transitionNote;

    /** Sahne atmosferi / duygu */
    @Column(name = "mood", length = 200)
    private String mood;

    /** Sahne mekanı */
    @Column(name = "location", length = 500)
    private String location;

    /** Sahnedeki karakterler/özneler */
    @Column(name = "characters", columnDefinition = "TEXT")
    private String characters;

    /** Sahne tahmini süresi (saniye) */
    @Column(name = "estimated_duration_seconds", nullable = false)
    private Integer estimatedDurationSeconds;

    /**
     * Gelecek media üretimi için notlar — yalnızca handoff metadata.
     * FP-003'te gerçek media dosyası üretilmez.
     */
    @Column(name = "media_generation_notes", columnDefinition = "TEXT")
    private String mediaGenerationNotes;
}
