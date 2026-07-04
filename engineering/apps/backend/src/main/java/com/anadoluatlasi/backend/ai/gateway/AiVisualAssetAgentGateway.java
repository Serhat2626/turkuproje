package com.anadoluatlasi.backend.ai.gateway;

import com.anadoluatlasi.backend.ai.contract.VisualAssetPlanAgentRequest;
import com.anadoluatlasi.backend.ai.contract.VisualAssetPlanAgentResponse;

/**
 * FP-006: AI Visual Asset Plan Agent gateway interface.
 *
 * <p>Görsel varlık planı üretim provider SDK bağımlılığını application layer'dan izole eder.
 * FP-001 – FP-004 gateway'lerden bağımsızdır — ayrı sorumluluk.</p>
 *
 * <p>Bu gateway yalnızca metin prompt üretir. Image generation API (DALL-E, Midjourney,
 * Stable Diffusion) çağırmaz, görsel dosya üretmez, MediaAsset/GeneratedAsset oluşturmaz.</p>
 *
 * <p>Gateway contract kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu interface'e sızmamalıdır.</li>
 *   <li>Request/Response contract'ları provider bağımsızdır.</li>
 *   <li>Exception fırlatmak yerine VisualAssetPlanAgentResponse döndürülür (success/failure içerir).</li>
 *   <li>API key/secret asla bu contract üzerinden taşınmaz.</li>
 *   <li>Raw internal prompt, provider request/response asla expose edilmez.</li>
 * </ul>
 */
public interface AiVisualAssetAgentGateway {

    /**
     * Verilen storyboard context'ine göre AI destekli görsel varlık planı üretir.
     *
     * <p>Üretilen {@code englishImagePrompt} ve {@code turkishVisualContext} ürün çıktısıdır.
     * İç orchestration prompt'u ürün çıktısı değildir ve response'ta yer almaz.</p>
     *
     * @param request görsel plan agent'a gönderilecek istek
     * @return başarılı prompt planı veya hata bilgisi içeren sonuç
     */
    VisualAssetPlanAgentResponse generateVisualAssetPlan(VisualAssetPlanAgentRequest request);
}
