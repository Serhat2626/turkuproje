package com.anadoluatlasi.backend.knowledge.entity;

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
 * Kaynak referansı entity'si.
 * Bir bilgi öğesine ait araştırma kaynağını temsil eder.
 * Kitap, makale, web sayfası gibi farklı kaynak türlerini
 * ve güvenilirlik seviyelerini içerir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "source_references")
@EntityListeners(AuditingEntityListener.class)
public class SourceReference {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_item_id", nullable = false)
    private KnowledgeItem knowledgeItem;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "author", length = 255)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reliability_level", nullable = false, length = 50)
    private ReliabilityLevel reliabilityLevel;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Kayıt oluşturulmadan önce UUID atar ve oluşturulma zamanını ayarlar.
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * Kayıt güncellenirken güncelleme zamanını ayarlar.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
