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

    // TürküVideo proje ilişkisi (nullable — eski kayıtlarla uyumluluk)
    @Column(name = "turku_project_id")
    private java.util.UUID turkuProjectId;

    // Pipeline adım adı: image_generation, kling_video, tts, content_generation
    @Column(name = "step_name", length = 50)
    private String stepName;

    // Hangi sahne için (nullable — content_generation gibi sahne-bağımsız adımlar)
    @Column(name = "scene_no")
    private Integer sceneNo;

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

    // Birim bilgisi (megapixels, seconds, characters)
    @Column(name = "input_units", precision = 12, scale = 4)
    private BigDecimal inputUnits;

    @Column(name = "unit_type", length = 30)
    private String unitType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AiGenerationStatus status;
}
