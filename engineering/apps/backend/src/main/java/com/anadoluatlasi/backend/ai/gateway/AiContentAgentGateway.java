package com.anadoluatlasi.backend.ai.gateway;

import com.anadoluatlasi.backend.ai.contract.ContentAgentRequest;

/**
 * FP-001: AI Content Agent gateway interface.
 *
 * <p>Bu interface, AI provider SDK bağımlılığını application layer'dan izole eder.
 * Tüm AI provider implementasyonları (mock, OpenAI, Anthropic, vb.) bu interface'i
 * implement eder.</p>
 *
 * <p>Gateway contract kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu interface'e sızmamalıdır.</li>
 *   <li>Request/Response contract'ları provider bağımsızdır.</li>
 *   <li>Exception fırlatmak yerine {@link AiGatewayResult} döndürülür.</li>
 *   <li>Timeout, rate limit vb. hatalar AiGatewayResult.failure() ile sarılır.</li>
 * </ul>
 */
public interface AiContentAgentGateway {

    /**
     * Verilen context'e göre AI destekli içerik draft'ı üretir.
     *
     * @param request AI Content Agent'a gönderilecek istek
     * @return başarılı yanıt veya hata bilgisi içeren sonuç
     */
    AiGatewayResult generateContentDraft(ContentAgentRequest request);
}
