package com.anadoluatlasi.backend.knowledge.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
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
 * Kaynak referansı entity'si.
 * Bir bilgi öğesine ait araştırma kaynağını temsil eder.
 * Kitap, makale, web sayfası gibi farklı kaynak türlerini
 * ve güvenilirlik seviyelerini içerir.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "source_references")
public class SourceReference extends BaseEntity {

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
}
