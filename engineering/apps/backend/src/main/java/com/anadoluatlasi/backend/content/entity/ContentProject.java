package com.anadoluatlasi.backend.content.entity;

import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import java.util.UUID;

/**
 * FP-001 İçerik projesi entity'si.
 * Bir Knowledge Item üzerinden belirli bir amaçla AI destekli içerik üretim sürecini yönetir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_projects")
@EntityListeners(AuditingEntityListener.class)
public class ContentProject {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Bu içerik projesinin dayandığı bilgi öğesi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_item_id", nullable = false)
    private KnowledgeItem knowledgeItem;

    /** Mevcut alan: proje başlığı */
    @Column(name = "title", length = 255)
    private String title;

    /** Mevcut alan: URL-friendly kısa ad */
    @Column(name = "slug", unique = true, length = 255)
    private String slug;

    /** Mevcut alan: İçerik türü (pre-FP-001, geriye uyumluluk) */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 50)
    private ContentType contentType;

    /** Mevcut alan: Hedef platform (pre-FP-001, geriye uyumluluk) */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_platform", length = 50)
    private TargetPlatform targetPlatform;

    /** FP-001: İçerik üretim amacı */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_purpose", length = 50)
    private ContentPurpose contentPurpose;

    /** FP-001: Hedef kitle */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", length = 50)
    private TargetAudience targetAudience;

    /** FP-001: Yazım tonu */
    @Enumerated(EnumType.STRING)
    @Column(name = "tone", length = 50)
    private ContentTone tone;

    /** Dil kodu (tr, en, vb.) */
    @Column(name = "language", nullable = false, length = 10)
    private String language;

    /** FP-001: Format ipucu (ör. "madde listesi", "uzun paragraf") */
    @Column(name = "format_hint", length = 255)
    private String formatHint;

    /** FP-001: Uzunluk ipucu (ör. "300 kelime", "kısa") */
    @Column(name = "length_hint", length = 255)
    private String lengthHint;

    /** Proje durumu */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ContentProjectStatus status;

    /** FP-001: AI retry sayacı */
    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /** FP-001: Son hata tipi */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_error_type", length = 50)
    private AiErrorType lastErrorType;

    /** FP-001: Son hata mesajı */
    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
