package com.anadoluatlasi.backend.ai.contract;

import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: AI Platform tarafından normalize edilmiş hata contract'ı.
 * Provider'ın raw hata mesajı Product veya Admin Panel'e doğrudan sızmamalıdır.
 */
@Value
@Builder
public class ContentAgentError {

    /** AI request referansı */
    UUID requestId;

    /** Content Project referansı */
    UUID projectId;

    /** Normalize edilmiş hata tipi */
    AiErrorType errorType;

    /** İnsan tarafından anlaşılabilir hata mesajı */
    String message;

    /** Retry yapılabilir mi */
    boolean retryable;

    /** Provider kaynaklı hata kodu (opsiyonel) */
    String providerErrorCode;

    /** Kısıtlı provider hata detayı (opsiyonel) */
    String providerMessage;

    /** Hata zamanı */
    LocalDateTime occurredAt;
}
