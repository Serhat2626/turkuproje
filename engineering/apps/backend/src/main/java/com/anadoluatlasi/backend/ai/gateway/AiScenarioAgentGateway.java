package com.anadoluatlasi.backend.ai.gateway;

import com.anadoluatlasi.backend.ai.contract.ScenarioAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ScenarioAgentResponse;

/**
 * FP-002: AI Scenario Agent gateway interface.
 *
 * <p>Senaryo üretim provider SDK bağımlılığını application layer'dan izole eder.
 * FP-001 AiContentAgentGateway'den bağımsızdır — ayrı sorumluluk.</p>
 *
 * <p>Gateway contract kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu interface'e sızmamalıdır.</li>
 *   <li>Request/Response contract'ları provider bağımsızdır.</li>
 *   <li>Exception fırlatmak yerine ScenarioAgentResponse döndürülür (success/failure içerir).</li>
 *   <li>API key/secret asla bu contract üzerinden taşınmaz.</li>
 * </ul>
 */
public interface AiScenarioAgentGateway {

    /**
     * Verilen context'e göre AI destekli senaryo üretir.
     *
     * @param request Scenario Agent'a gönderilecek istek
     * @return başarılı yanıt veya hata bilgisi içeren sonuç
     */
    ScenarioAgentResponse generateScenario(ScenarioAgentRequest request);
}
