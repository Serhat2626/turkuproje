package com.anadoluatlasi.backend.ai.gateway;

import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentRequest;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentResponse;

/**
 * FP-004: AI Platform Package Agent gateway interface.
 *
 * <p>Platform paketi üretim provider SDK bağımlılığını application layer'dan izole eder.
 * FP-001 AiContentAgentGateway, FP-002 AiScenarioAgentGateway ve
 * FP-003 AiStoryboardAgentGateway'den bağımsızdır — ayrı sorumluluk.</p>
 *
 * <p>Gateway contract kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu interface'e sızmamalıdır.</li>
 *   <li>Request/Response contract'ları provider bağımsızdır.</li>
 *   <li>Exception fırlatmak yerine PlatformPackageAgentResponse döndürülür (success/failure içerir).</li>
 *   <li>API key/secret asla bu contract üzerinden taşınmaz.</li>
 * </ul>
 */
public interface AiPlatformPackageAgentGateway {

    /**
     * Verilen context'e göre AI destekli platform paketleri üretir.
     *
     * @param request Platform Package Agent'a gönderilecek istek
     * @return başarılı yanıt (packages dolu) veya hata bilgisi içeren sonuç
     */
    PlatformPackageAgentResponse generatePlatformPackages(PlatformPackageAgentRequest request);
}
