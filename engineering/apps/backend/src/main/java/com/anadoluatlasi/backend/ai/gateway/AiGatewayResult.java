package com.anadoluatlasi.backend.ai.gateway;

import com.anadoluatlasi.backend.ai.contract.ContentAgentError;
import com.anadoluatlasi.backend.ai.contract.ContentAgentResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FP-001: AI Content Agent gateway çağrısının sonucunu temsil eder.
 * Başarı (response) veya hata (error) durumlarından birini taşır.
 *
 * <p>Kullanım:</p>
 * <pre>
 *   AiGatewayResult result = gateway.generateContentDraft(request);
 *   if (result.isSuccess()) {
 *       ContentAgentResponse response = result.getResponse();
 *   } else {
 *       ContentAgentError error = result.getError();
 *   }
 * </pre>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiGatewayResult {

    private final boolean success;
    private final ContentAgentResponse response;
    private final ContentAgentError error;

    /**
     * Başarılı sonuç oluşturur.
     */
    public static AiGatewayResult success(ContentAgentResponse response) {
        return new AiGatewayResult(true, response, null);
    }

    /**
     * Hatalı sonuç oluşturur.
     */
    public static AiGatewayResult failure(ContentAgentError error) {
        return new AiGatewayResult(false, null, error);
    }
}
