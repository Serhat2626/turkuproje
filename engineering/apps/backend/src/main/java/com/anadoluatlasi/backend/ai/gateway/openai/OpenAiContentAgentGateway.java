package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.ContentAgentError;
import com.anadoluatlasi.backend.ai.contract.ContentAgentRequest;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiGatewayResult;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * FP-001: OpenAI Content Agent Gateway implementasyonu — SKELETON.
 *
 * <p>Bu sınıf gerçek OpenAI API çağrısı yapacak gateway'in iskeletidir.
 * Şu an tam implementasyon yapılmamıştır — API key doğrulaması yapar,
 * ancak gerçek çağrı yerine yapılandırma hatası döndürür.</p>
 *
 * <p>Tam implementasyon Task 23 (Prompt Builder) ve Task 24 (Error Mapping) ile gelecektir.</p>
 *
 * <p>Gateway kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu sınıf içinde kalır (AiGenerationService'e sızmaz)</li>
 *   <li>Request/Response contract'ları provider bağımsızdır</li>
 *   <li>Exception fırlatmak yerine AiGatewayResult döndürülür</li>
 *   <li>Proje statüsünü değiştirmez — sadece result döndürür</li>
 * </ul>
 */
@Slf4j
public class OpenAiContentAgentGateway implements AiContentAgentGateway {

    private final AiProviderProperties.OpenAi config;

    public OpenAiContentAgentGateway(AiProviderProperties.OpenAi config) {
        this.config = config;
    }

    @Override
    public AiGatewayResult generateContentDraft(ContentAgentRequest request) {
        log.info("OpenAI gateway çağrıldı: requestId={}, projectId={}, model={}",
                request.getRequestId(), request.getProjectId(), config.getModel());

        // ── API key doğrulama ──
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.error("OpenAI API key yapılandırılmamış. ai.openai.api-key ayarlanmalıdır.");
            return AiGatewayResult.failure(ContentAgentError.builder()
                    .requestId(request.getRequestId())
                    .projectId(request.getProjectId())
                    .errorType(AiErrorType.AI_PROVIDER_CONFIG_ERROR)
                    .message("OpenAI API key yapılandırılmamış. ai.openai.api-key environment variable ayarlanmalıdır.")
                    .retryable(false)
                    .occurredAt(LocalDateTime.now())
                    .build());
        }

        // ── Skeleton: gerçek çağrı henüz implementasyon dışı ──
        // Task 23 ile PromptBuilder, Task 24 ile Error Mapping eklenecek.
        log.warn("OpenAI gateway SKELETON modu — gerçek API çağrısı henüz implement edilmedi. requestId={}",
                request.getRequestId());

        return AiGatewayResult.failure(ContentAgentError.builder()
                .requestId(request.getRequestId())
                .projectId(request.getProjectId())
                .errorType(AiErrorType.AI_PROVIDER_UNAVAILABLE)
                .message("OpenAI gateway henüz tam olarak implement edilmedi. Task 23/24 ile tamamlanacaktır.")
                .retryable(true)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
