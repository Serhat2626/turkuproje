package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.ContentAgentError;
import com.anadoluatlasi.backend.ai.contract.ContentAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ContentAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ProviderExecutionMetadata;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiGatewayResult;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-001: OpenAI Content Agent Gateway implementasyonu.
 *
 * <p>Task 22: Skeleton oluşturuldu.
 * Task 23: PromptBuilder ve ResponseParser entegre edildi.
 * Task 24: HTTP client, error mapping ve tam akış implementasyonu.</p>
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
    private final OpenAiClient client;

    public OpenAiContentAgentGateway(AiProviderProperties.OpenAi config) {
        this.config = config;
        this.client = new OpenAiClient(config);
    }

    /**
     * Test'te OpenAiClient inject etmek için paket-internal constructor.
     */
    OpenAiContentAgentGateway(AiProviderProperties.OpenAi config, OpenAiClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public AiGatewayResult generateContentDraft(ContentAgentRequest request) {
        log.info("OpenAI gateway çağrıldı: requestId={}, projectId={}, model={}",
                request.getRequestId(), request.getProjectId(), config.getModel());

        // ── 1. API key doğrulama ──
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.error("OpenAI API key yapılandırılmamış. requestId={}", request.getRequestId());
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
        String promptVersion = OpenAiPromptBuilder.getPromptVersion();
        log.debug("OpenAI prompt oluşturuldu: {} mesaj, promptVersion={}",
                messages.size(), promptVersion);

        // ── 3. HTTP çağrısı ──
        OpenAiCallResult callResult = client.call(messages, request.getRequestId(), request.getProjectId());

        // ── 4. HTTP hata kontrolü ──
        if (!callResult.isSuccess()) {
            log.warn("OpenAI HTTP çağrısı başarısız: requestId={}, error={}, retryable={}",
                    request.getRequestId(), callResult.getErrorType(), callResult.isRetryable());
            return AiGatewayResult.failure(ContentAgentError.builder()
                    .requestId(request.getRequestId())
                    .projectId(request.getProjectId())
                    .errorType(callResult.getErrorType())
                    .message(callResult.getErrorMessage())
                    .retryable(callResult.isRetryable())
                    .occurredAt(LocalDateTime.now())
                    .build());
        }

        // ── 5. Response parsing ──
        OpenAiParseResult parseResult = OpenAiResponseParser.parse(
                callResult.getContent(), request.getRequestId(), request.getProjectId());

        if (!parseResult.isSuccess()) {
            log.warn("OpenAI yanıt parse hatası: requestId={}, error={}, retryable={}",
                    request.getRequestId(), parseResult.getErrorType(), parseResult.isRetryable());
            return AiGatewayResult.failure(ContentAgentError.builder()
                    .requestId(request.getRequestId())
                    .projectId(request.getProjectId())
                    .errorType(parseResult.getErrorType())
                    .message(parseResult.getErrorMessage())
                    .retryable(parseResult.isRetryable())
                    .occurredAt(LocalDateTime.now())
                    .build());
        }

        // ── 6. Success — provider metadata ekle ──
        ContentAgentResponse response = parseResult.getResponse();

        ContentAgentResponse enrichedResponse = ContentAgentResponse.builder()
                .requestId(response.getRequestId())
                .projectId(response.getProjectId())
                .title(response.getTitle())
                .body(response.getBody())
                .summary(response.getSummary())
                .suggestedTags(response.getSuggestedTags())
                .warnings(response.getWarnings())
                .qualityNotes(response.getQualityNotes())
                .generationReason(response.getGenerationReason())
                .providerMetadata(ProviderExecutionMetadata.builder()
                        .providerName("openai")
                        .modelName(config.getModel())
                        .durationMs(callResult.getDurationMs())
                        .tokenUsage(callResult.getTokenUsage())
                        .rawResponseStored(false)
                        .promptVersion(promptVersion)
                        .build())
                .build();

        log.info("OpenAI başarılı: requestId={}, projectId={}, durationMs={}, title={}",
                request.getRequestId(), request.getProjectId(),
                callResult.getDurationMs(), enrichedResponse.getTitle());

        return AiGatewayResult.success(enrichedResponse);
    }
}
