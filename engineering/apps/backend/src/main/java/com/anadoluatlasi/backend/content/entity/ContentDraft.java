package com.anadoluatlasi.backend.content.entity;

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
 * FP-001 İçerik taslağı entity'si.
 * AI veya insan tarafından oluşturulmuş içerik taslağını temsil eder.
 * Bir ContentProject'e bağlıdır.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_drafts")
public class ContentDraft extends BaseEntity {

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
}
