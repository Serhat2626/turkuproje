package com.anadoluatlasi.backend.scenario.entity;

import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
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
 * FP-002: Senaryo taslağı entity'si.
 *
 * <p>Bir Content Project'e bağlı olarak AI veya insan tarafından üretilen
 * yapılandırılmış video senaryosunu temsil eder. Her senaryo 6 beat içerir.</p>
 *
 * <p>Göverişim kuralları:</p>
 * <ul>
 *   <li>AI çıktısı hiçbir zaman APPROVED olmaz.</li>
 *   <li>Onay/red explicit admin eylemi gerektirir.</li>
 *   <li>Raw provider payload veya prompt saklanmaz.</li>
 *   <li>Provider metadata sadece safe bilgi içerir (providerType, modelName).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scenario_drafts")
@EntityListeners(AuditingEntityListener.class)
public class ScenarioDraft {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Bu senaryonun ait olduğu içerik projesi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_project_id", nullable = false)
    private ContentProject contentProject;

    /** Senaryonun üretildiği kaynak approved draft */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_draft_id", nullable = false)
    private ContentDraft sourceDraft;

    /** Aynı project içinde senaryo version numarası */
    @Column(name = "version", nullable = false)
    private Integer version;

    /** Senaryo yaşam döngüsü durumu */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ScenarioDraftStatus status;

    /** Senaryo başlığı */
    @Column(name = "title", length = 500)
    private String title;

    /** Kısa özet */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /** Hedef kitle */
    @Column(name = "target_audience", length = 100)
    private String targetAudience;

    /** Kültürel/öğretici amaç */
    @Column(name = "cultural_objective", columnDefinition = "TEXT")
    private String culturalObjective;

    /** Anlatım tonu */
    @Column(name = "tone", length = 100)
    private String tone;

    /** Tahmini video süresi (saniye) */
    @Column(name = "estimated_duration_seconds")
    private Integer estimatedDurationSeconds;

    /** Mekan/lokasyon */
    @Column(name = "setting", length = 500)
    private String setting;

    /** Karakterler */
    @Column(name = "characters", columnDefinition = "TEXT")
    private String characters;

    /** Admin review notları */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    /** Senaryoyu kim üretti */
    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 50)
    private GeneratedBy generatedBy;

    /** Safe provider metadata: provider tipi (ör. "openai", "mock") */
    @Column(name = "provider_type", length = 50)
    private String providerType;

    /** Safe provider metadata: model adı (ör. "gpt-4o-mini") */
    @Column(name = "model_name", length = 100)
    private String modelName;

    /** AI uyarıları (JSON formatında) */
    @Column(name = "warnings_json", columnDefinition = "TEXT")
    private String warningsJson;

    /** Senaryo beat'leri (6 adet) */
    @Builder.Default
    @OneToMany(mappedBy = "scenarioDraft", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<ScenarioBeat> beats = new ArrayList<>();

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
