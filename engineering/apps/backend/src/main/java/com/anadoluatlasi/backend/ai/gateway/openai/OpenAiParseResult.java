package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.ContentAgentResponse;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FP-001 Task 23: OpenAI response parser sonucu.
 *
 * <p>Provider-internal — başarılı parse veya yapısal hata bilgisi taşır.
 * Gateway tarafından AiGatewayResult'a dönüştürülür.</p>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class OpenAiParseResult {

    private final boolean success;
    private final ContentAgentResponse response;
    private final AiErrorType errorType;
    private final String errorMessage;
    private final boolean retryable;

    static OpenAiParseResult success(ContentAgentResponse response) {
        return new OpenAiParseResult(true, response, null, null, false);
    }

    static OpenAiParseResult failure(AiErrorType errorType, String message, boolean retryable) {
        return new OpenAiParseResult(false, null, errorType, message, retryable);
    }
}
