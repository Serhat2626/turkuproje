package com.anadoluatlasi.backend.ai.gateway;

import com.anadoluatlasi.backend.ai.contract.StoryboardAgentRequest;
import com.anadoluatlasi.backend.ai.contract.StoryboardAgentResponse;

/**
 * FP-003: AI Storyboard Agent gateway interface.
 *
 * <p>Storyboard üretim provider SDK bağımlılığını application layer'dan izole eder.
 * FP-001 AiContentAgentGateway ve FP-002 AiScenarioAgentGateway'den bağımsızdır
 * — ayrı sorumluluk.</p>
 *
 * <p>Gateway contract kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu interface'e sızmamalıdır.</li>
 *   <li>Request/Response contract'ları provider bağımsızdır.</li>
 *   <li>Exception fırlatmak yerine StoryboardAgentResponse döndürülür (success/failure içerir).</li>
 *   <li>API key/secret asla bu contract üzerinden taşınmaz.</li>
 * </ul>
 */
public interface AiStoryboardAgentGateway {

    /**
     * Verilen context'e göre AI destekli storyboard üretir.
     *
     * @param request Storyboard Agent'a gönderilecek istek
     * @return başarılı yanıt veya hata bilgisi içeren sonuç
     */
    StoryboardAgentResponse generateStoryboard(StoryboardAgentRequest request);
}
