package com.anadoluatlasi.backend.imagegen.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * FP-007: Image generation başlatma isteği DTO'su.
 *
 * <p>Admin, APPROVED VisualAssetPlan üzerinden generation başlatmak
 * için bu DTO'yu kullanır. Prompt üretimi server-side yapılır;
 * bu DTO doğrudan prompt taşımaz.</p>
 *
 * <p>Yasak alanlar: prompt, internalPrompt, rawPrompt, providerPayload,
 * providerConfig, apiKey, secret, modelCredential, storagePath,
 * signedUrl, publicUrl.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartImageGenerationRequest {

    @NotNull(message = "Visual asset plan ID zorunludur")
    private UUID visualAssetPlanId;

    /** Belirli bir item için generation başlatılacaksa; null ise tüm plan item'ları */
    private UUID visualAssetItemId;

    /** İstemci tarafından sağlanabilir; null ise server-side üretilir */
    private String idempotencyKey;

    /** Opsiyonel correlation ID */
    private UUID correlationId;
}
