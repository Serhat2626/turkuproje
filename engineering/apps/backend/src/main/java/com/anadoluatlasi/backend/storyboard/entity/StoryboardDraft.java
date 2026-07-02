package com.anadoluatlasi.backend.storyboard.entity;

import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-003: Storyboard taslağı entity'si.
 *
 * <p>Bir Content Project'e bağlı olarak, onaylanmış bir ScenarioDraft'tan
 * AI veya insan tarafından üretilen yapılandırılmış storyboard'u temsil eder.
 * Her storyboard sıralı scene'lerden oluşur.</p>
 *
 * <p>Göverişim kuralları:</p>
 * <ul>
 *   <li>AI çıktısı hiçbir zaman APPROVED olmaz.</li>
 *   <li>Onay/red explicit admin eylemi gerektirir.</li>
 *   <li>Raw provider payload veya prompt saklanmaz.</li>
 *   <li>Provider metadata sadece safe bilgi içerir (providerType, modelName).</li>
 *   <li>Gerçek media dosyası (image/video/audio) oluşturulmaz veya saklanmaz.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "storyboard_drafts")
@EntityListeners(AuditingEntityListener.class)
public class StoryboardDraft {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Bu storyboard'un ait olduğu içerik projesi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_project_id", nullable = false)
    private ContentProject contentProject;

    /** Storyboard'un kaynak approved content draft'ı */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_draft_id", nullable = false)
    private ContentDraft sourceDraft;

    /** Storyboard'un üretildiği kaynak approved scenario draft */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_scenario_draft_id", nullable = false)
    private ScenarioDraft sourceScenarioDraft;

    /** Kaynak senaryo versiyonu */
    @Column(name = "source_scenario_version", nullable = false)
    private Integer sourceScenarioVersion;

    /** Aynı project içinde storyboard version numarası */
    @Column(name = "version", nullable = false)
    private Integer version;

    /** Storyboard yaşam döngüsü durumu */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StoryboardDraftStatus status;

    /** Storyboard başlığı */
    @Column(name = "title", length = 500)
    private String title;

    /** Kısa özet */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /** Hedef platform (YouTube, Instagram Reels, vb.) */
    @Column(name = "target_platform", length = 100)
    private String targetPlatform;

    /** Görüntü oranı (16:9, 9:16, 1:1) */
    @Column(name = "aspect_ratio", length = 20)
    private String aspectRatio;

    /** Toplam tahmini süre (saniye) */
    @Column(name = "estimated_duration_seconds")
    private Integer estimatedDurationSeconds;

    /** Genel görsel stil kılavuzu */
    @Column(name = "visual_style_guide", columnDefinition = "TEXT")
    private String visualStyleGuide;

    /** Narration stil notu */
    @Column(name = "narration_style", length = 500)
    private String narrationStyle;

    /** Admin review notları */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    /** Storyboard'u kim üretti */
    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 50)
    private GeneratedBy generatedBy;

    /** Safe provider metadata: provider tipi (ör. "openai", "mock") */
    @Column(name = "provider_type", length = 50)
    private String providerType;

    /** Safe provider metadata: model adı (ör. "gpt-4o-mini") */
    @Column(name = "model_name", length = 100)
    private String modelName;

    /** Storyboard scene'leri (sıralı) */
    @Builder.Default
    @OneToMany(mappedBy = "storyboardDraft", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sceneNumber ASC")
    private List<StoryboardScene> scenes = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Onay zamanı */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Red zamanı */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    /** Son düzenleme zamanı */
    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    /** Oluşturan admin */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /** Onaylayan admin */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    /** Reddeden admin */
    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    /** Düzenleyen admin */
    @Column(name = "edited_by", length = 100)
    private String editedBy;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
