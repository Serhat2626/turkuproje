package com.anadoluatlasi.backend.platformpackage.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * FP-004: Platform paketi sahne kullanım detayı entity'si.
 *
 * <p>Her storyboard sahnesinin belirli bir platform paketinde nasıl
 * kullanıldığını/adapte edildiğini tanımlar.</p>
 *
 * <p>Kullanım tipleri:</p>
 * <ul>
 *   <li>{@code PRIMARY} — Sahne olduğu gibi kullanılır</li>
 *   <li>{@code ADAPTED} — Sahne adapte edilerek kullanılır</li>
 *   <li>{@code PARTIAL} — Sahnenin bir kısmı kullanılır</li>
 *   <li>{@code OMITTED} — Sahne bu pakette kullanılmaz</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "platform_package_scene_usages")
public class PlatformPackageSceneUsage extends BaseEntity {

    /** Bu kullanım kaydının ait olduğu platform paketi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_package_draft_id", nullable = false)
    private PlatformPackageDraft platformPackageDraft;

    /** Storyboard sahne numarası */
    @Column(name = "scene_number", nullable = false)
    private Integer sceneNumber;

    /** Kaynak storyboard scene referansı (opsiyonel) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_storyboard_scene_id")
    private StoryboardScene sourceStoryboardScene;

    /** Kullanım tipi (PRIMARY, ADAPTED, PARTIAL, OMITTED) */
    @Column(name = "usage_type", nullable = false, length = 20)
    private String usageType;

    /** Bu pakette kullanılıyor mu */
    @Column(name = "included", nullable = false)
    private Boolean included;

    /** Adaptasyon notu */
    @Column(name = "adaptation_note", columnDefinition = "TEXT")
    private String adaptationNote;
}
