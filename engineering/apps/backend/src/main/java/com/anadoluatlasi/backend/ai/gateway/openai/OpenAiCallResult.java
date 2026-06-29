package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * FP-001 Task 24: OpenAI HTTP çağrı sonucu.
 *
 * <p>Provider-internal — client ile gateway arasında taşınır.
 * Başarılı ise raw model text content, hata ise error bilgisi taşır.</p>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class OpenAiCallResult {

    private final boolean success;
    private final String content;
    private final String tokenUsage;
    private final AiErrorType errorType;
    private final String errorMessage;
    private final boolean retryable;
    private final long durationMs;

    static OpenAiCallResult success(String content, long durationMs, String tokenUsage) {
        return new OpenAiCallResult(true, content, tokenUsage, null, null, false, durationMs);
    }

    static OpenAiCallResult failure(AiErrorType errorType, String message, boolean retryable, long durationMs) {
        return new OpenAiCallResult(false, null, null, errorType, message, retryable, durationMs);
    }
}
