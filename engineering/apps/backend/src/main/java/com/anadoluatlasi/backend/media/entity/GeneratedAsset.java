package com.anadoluatlasi.backend.media.entity;

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
 * AI veya diğer araçlarla üretilen medya varlıklarını temsil eden entity.
 * Her üretilen varlık, bir MediaAsset ile ilişkilendirilir ve üretim
 * yöntemi, prompt, model ve maliyet bilgilerini içerir.
 */
@Entity
@Table(name = "generated_assets")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedAsset extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_asset_id", nullable = false)
    private MediaAsset mediaAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_project_id")
    private ContentProject contentProject;

    @Enumerated(EnumType.STRING)
    @Column(name = "generator_type", nullable = false, length = 50)
    private GeneratorType generatorType;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "model", length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 50)
    private GenerationStatus generationStatus;

    @Column(name = "cost", precision = 12, scale = 6)
    private BigDecimal cost;
}
