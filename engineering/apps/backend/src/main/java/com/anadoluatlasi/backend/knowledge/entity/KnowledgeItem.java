package com.anadoluatlasi.backend.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
 * Bilgi bankası öğesi entity'si.
 * Anadolu kültürüne ait konu başlıklarını temsil eder.
 * FP-001 content pipeline'ın kaynak veri modelidir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "knowledge_items")
@EntityListeners(AuditingEntityListener.class)
public class KnowledgeItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private KnowledgeItemType type;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /** FP-001: Detaylı açıklama */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** FP-001: Kategori (türkü, gelenek, tarihî olay vb.) */
    @Column(name = "category", length = 100)
    private String category;

    /** FP-001: İl bilgisi */
    @Column(name = "city", length = 100)
    private String city;

    /** FP-001: İlçe bilgisi */
    @Column(name = "district", length = 100)
    private String district;

    /** FP-001: Serbest format lokasyon metni */
    @Column(name = "location_text", length = 500)
    private String locationText;

    /** FP-001: Editör notları */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private KnowledgeItemStatus status;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

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
