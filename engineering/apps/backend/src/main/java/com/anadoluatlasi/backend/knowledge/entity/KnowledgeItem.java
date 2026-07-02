package com.anadoluatlasi.backend.knowledge.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Bilgi bankası öğesi entity'si.
 * Anadolu kültürüne ait konu başlıklarını temsil eder.
 * FP-001 content pipeline'ın kaynak veri modelidir.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "knowledge_items")
public class KnowledgeItem extends BaseEntity {

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
}
