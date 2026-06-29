package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.ContentAgentError;
import com.anadoluatlasi.backend.ai.contract.ContentAgentRequest;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiGatewayResult;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-001: OpenAI Content Agent Gateway implementasyonu.
 *
 * <p>Bu sınıf gerçek OpenAI API çağrısı yapacak gateway'dir.
 * Task 22: Skeleton oluşturuldu.
 * Task 23: PromptBuilder ve ResponseParser entegre edildi.
 * Gerçek HTTP çağrısı henüz yapılmaz — Task 24+ ile tamamlanacaktır.</p>
 *
 * <p>Gateway kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu sınıf içinde kalır (AiGenerationService'e sızmaz)</li>
 *   <li>Request/Response contract'ları provider bağımsızdır</li>
 *   <li>Exception fırlatmak yerine AiGatewayResult döndürülür</li>
 *   <li>Proje statüsünü değiştirmez — sadece result döndürür</li>
 *   <li>Raw prompt/completion saklanmaz ve REST'e expose edilmez</li>
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

        // ── 1. API key doğrulama ──
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

        // ── 2. Prompt construction ──
        List<OpenAiChatMessage> messages = OpenAiPromptBuilder.buildMessages(request);
        log.debug("OpenAI prompt oluşturuldu: {} mesaj, promptVersion={}",
                messages.size(), OpenAiPromptBuilder.getPromptVersion());

        // ── 3. Real API call — henüz implement edilmedi ──
        // Task 24+ ile HTTP client eklenerek gerçek çağrı yapılacak.
        // Şu an skeleton modu: prompt build edilir ama çağrı yapılmaz.
        log.warn("OpenAI gateway SKELETON modu — gerçek API çağrısı henüz implement edilmedi. " +
                "requestId={}, promptVersion={}", request.getRequestId(), OpenAiPromptBuilder.getPromptVersion());

        return AiGatewayResult.failure(ContentAgentError.builder()
                .requestId(request.getRequestId())
                .projectId(request.getProjectId())
                .errorType(AiErrorType.AI_PROVIDER_UNAVAILABLE)
                .message("OpenAI gateway henüz tam olarak implement edilmedi. " +
                        "Prompt oluşturuldu ancak HTTP çağrısı yapılmadı. Task 24+ ile tamamlanacaktır.")
                .retryable(true)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
