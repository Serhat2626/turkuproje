package com.anadoluatlasi.backend.imagegen.entity;

import com.anadoluatlasi.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * FP-007: Image generation result entity.
 *
 * <p>Provider'dan dönen sonucun normalize edilmiş kaydı.
 * Raw provider response bu entity'de saklanmaz.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>rawProviderResponse alanı yoktur.</li>
 *   <li>rawProviderPayload alanı yoktur.</li>
 *   <li>signedProviderUrl alanı yoktur.</li>
 *   <li>fullInternalPrompt alanı yoktur.</li>
 *   <li>apiKey / secret alanı yoktur.</li>
 *   <li>providerRequestId internal-only metadata olarak saklanabilir ama admin-facing değildir.</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "image_generation_results")
public class ImageGenerationResult extends BaseEntity {

    // ── Lineage ─────────────────────────────────────────────────────

    @Column(name = "image_generation_request_id", nullable = false)
    private UUID imageGenerationRequestId;

    @Column(name = "visual_asset_plan_id", nullable = false)
    private UUID visualAssetPlanId;

    @Column(name = "visual_asset_item_id", nullable = false)
    private UUID visualAssetItemId;

    // ── Provider Metadata (allowlist only) ───────────────────────────

    @Column(name = "provider_type", length = 50)
    private String providerType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "provider_request_id", length = 255)
    private String providerRequestId;

    @Column(name = "generation_duration_ms")
    private Long generationDurationMs;

    // ── Result ──────────────────────────────────────────────────────

    @Column(name = "output_count")
    private Integer outputCount;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    // ── Error (normalized) ──────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "error_code", length = 50)
    private ImageGenerationErrorCode errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ── Observability ───────────────────────────────────────────────

    @Column(name = "correlation_id")
    private UUID correlationId;
}
