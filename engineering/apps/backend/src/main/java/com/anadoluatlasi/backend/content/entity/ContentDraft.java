package com.anadoluatlasi.backend.content.entity;

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
 * FP-001 İçerik taslağı entity'si.
 * AI veya insan tarafından oluşturulmuş içerik taslağını temsil eder.
 * Bir ContentProject'e bağlıdır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_drafts")
@EntityListeners(AuditingEntityListener.class)
public class ContentDraft {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Bu taslağın ait olduğu içerik projesi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_project_id", nullable = false)
    private ContentProject contentProject;

    /** Taslak başlığı */
    @Column(name = "title", length = 500)
    private String title;

    /** Taslak gövde metni */
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** Kısa özet */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /** Taslak durumu */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ContentDraftStatus status;

    /** Taslağı kim oluşturdu */
    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 50)
    private GeneratedBy generatedBy;

    /** AI uyarıları (JSON formatında) */
    @Column(name = "warnings_json", columnDefinition = "TEXT")
    private String warningsJson;

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
