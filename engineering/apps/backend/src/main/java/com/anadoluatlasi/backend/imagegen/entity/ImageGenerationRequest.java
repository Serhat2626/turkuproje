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
 * FP-007: Image generation request entity.
 *
 * <p>Bir SceneVisualPlan veya ThumbnailPlan için oluşturulan
 * image generation talebi. APPROVED VisualAssetPlan kaynak olarak alınır.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>rawProviderRequestPayload alanı yoktur.</li>
 *   <li>rawProviderResponsePayload alanı yoktur.</li>
 *   <li>apiKey / secret alanı yoktur.</li>
 *   <li>internalFullPrompt alanı yoktur.</li>
 *   <li>safePromptSummary yalnızca Layer 1 (ürün çıktısı) bilgisi içerir.</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "image_generation_requests")
public class ImageGenerationRequest extends BaseEntity {

    // ── Lineage References ──────────────────────────────────────────

    @Column(name = "content_project_id", nullable = false)
    private UUID contentProjectId;

    @Column(name = "visual_asset_plan_id", nullable = false)
    private UUID visualAssetPlanId;

    @Column(name = "visual_asset_item_id", nullable = false)
    private UUID visualAssetItemId;

    // ── Request Type & Status ───────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private ImageGenerationItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ImageGenerationRequestStatus status;

    // ── Idempotency ─────────────────────────────────────────────────

    @Column(name = "idempotency_key", nullable = false, length = 255, unique = true)
    private String idempotencyKey;

    // ── Safe AI Metadata (allowlist only) ────────────────────────────

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "safe_prompt_summary", columnDefinition = "TEXT")
    private String safePromptSummary;

    // ── Error (normalized) ──────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "error_code", length = 50)
    private ImageGenerationErrorCode errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ── Observability ───────────────────────────────────────────────

    @Column(name = "correlation_id")
    private UUID correlationId;

    // ── Audit ────────────────────────────────────────────────────────

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
