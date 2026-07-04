package com.anadoluatlasi.backend.imagegen.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-007: Generated image asset entity.
 *
 * <p>Image generation sonucu üretilen görselin review lifecycle kaydı.
 * İlk oluşturulduğunda mutlaka {@link GeneratedAssetStatus#GENERATED_PENDING_REVIEW}
 * olmalıdır. AI output hiçbir zaman otomatik APPROVED olamaz.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>Raw provider payload/response saklanmaz.</li>
 *   <li>Publish/export status alanı yoktur.</li>
 *   <li>READY_FOR_PUBLISH yoktur.</li>
 *   <li>Public/signed URL persistent field yoktur.</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fp007_generated_assets")
public class ImageGeneratedAsset extends BaseEntity {

    // ── Lineage References ──────────────────────────────────────────

    @Column(name = "image_generation_request_id", nullable = false)
    private UUID imageGenerationRequestId;

    @Column(name = "image_generation_result_id")
    private UUID imageGenerationResultId;

    @Column(name = "image_media_asset_id")
    private UUID imageMediaAssetId;

    @Column(name = "content_project_id", nullable = false)
    private UUID contentProjectId;

    @Column(name = "visual_asset_plan_id", nullable = false)
    private UUID visualAssetPlanId;

    @Column(name = "visual_asset_item_id", nullable = false)
    private UUID visualAssetItemId;

    // ── Status ──────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private GeneratedAssetStatus status;

    // ── Safe Metadata ───────────────────────────────────────────────

    @Column(name = "safe_prompt_summary", columnDefinition = "TEXT")
    private String safePromptSummary;

    @Column(name = "quality_signals_summary", columnDefinition = "TEXT")
    private String qualitySignalsSummary;

    @Column(name = "safety_flags_summary", columnDefinition = "TEXT")
    private String safetyFlagsSummary;

    // ── Review ──────────────────────────────────────────────────────

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
