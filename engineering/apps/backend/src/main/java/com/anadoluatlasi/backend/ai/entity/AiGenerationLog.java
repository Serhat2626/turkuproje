package com.anadoluatlasi.backend.ai.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * AI üretim çağrılarının loglarını tutan entity.
 * Her bir AI API çağrısı için sağlayıcı, model, maliyet ve durum bilgilerini kaydeder.
 * İsteğe bağlı olarak bir içerik projesi ve prompt şablonu ile ilişkilendirilebilir.
 */
@Entity
@Table(name = "ai_generation_logs")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerationLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_project_id")
    private ContentProject contentProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_id")
    private PromptTemplate promptTemplate;

    @Column(name = "provider", nullable = false, length = 100)
    private String provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    @Column(name = "cost", precision = 12, scale = 6)
    private BigDecimal cost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AiGenerationStatus status;
}
