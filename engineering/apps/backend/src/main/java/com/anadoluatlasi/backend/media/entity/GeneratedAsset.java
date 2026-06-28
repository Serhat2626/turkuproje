package com.anadoluatlasi.backend.media.entity;

import com.anadoluatlasi.backend.content.entity.ContentProject;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI veya diğer araçlarla üretilen medya varlıklarını temsil eden entity.
 * Her üretilen varlık, bir MediaAsset ile ilişkilendirilir ve üretim
 * yöntemi, prompt, model ve maliyet bilgilerini içerir.
 */
@Entity
@Table(name = "generated_assets")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedAsset {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

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
    }
}
