package com.anadoluatlasi.backend.imagegen.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-007: Provider-agnostic image generation request model.
 *
 * <p>Provider gateway'e gönderilen güvenli input. Raw prompt,
 * API key, secret, provider config, storage path taşımaz.</p>
 *
 * <p>Bu model internal provider boundary input'tur.
 * Admin-facing DTO değildir.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageProviderGenerationRequest {
    private UUID requestId;
    private UUID visualAssetPlanId;
    private UUID visualAssetItemId;
    private UUID contentProjectId;
    private String itemType;
    private String safePromptSummary;
    private UUID correlationId;
    private LocalDateTime requestedAt;
}
