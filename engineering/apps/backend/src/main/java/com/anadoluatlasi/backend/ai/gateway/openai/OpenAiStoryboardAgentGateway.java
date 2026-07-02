package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.StoryboardAgentRequest;
import com.anadoluatlasi.backend.ai.contract.StoryboardAgentResponse;
import com.anadoluatlasi.backend.ai.gateway.AiStoryboardAgentGateway;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-003 Task 09: OpenAI Storyboard Agent Gateway implementasyonu.
 *
 * <p>AiStoryboardAgentGateway interface'ini implement eder.
 * {@code ai.provider=openai} olduğunda AiGatewayConfig tarafından bean olarak kaydedilir.</p>
 *
 * <p>Gateway kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu sınıf içinde kalır</li>
 *   <li>Request/Response contract'ları provider bağımsızdır</li>
 *   <li>Raw prompt/completion saklanmaz ve REST'e expose edilmez</li>
 *   <li>API key loglanmaz</li>
 *   <li>Storyboard onayı yapmaz — sadece candidate üretir</li>
 *   <li>Medya dosyası üretmez — sadece sahne açıklamaları üretir</li>
 * </ul>
 */
@Slf4j
public class OpenAiStoryboardAgentGateway implements AiStoryboardAgentGateway {

    private final AiProviderProperties.OpenAi config;
    private final OpenAiClient client;

    public OpenAiStoryboardAgentGateway(AiProviderProperties.OpenAi config) {
        this.config = config;
        this.client = new OpenAiClient(config);
    }

    /**
     * Test'te OpenAiClient inject etmek için paket-internal constructor.
     */
    OpenAiStoryboardAgentGateway(AiProviderProperties.OpenAi config, OpenAiClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public StoryboardAgentResponse generateStoryboard(StoryboardAgentRequest request) {
        log.info("OpenAI storyboard gateway çağrıldı: requestId={}, contentProjectId={}, model={}",
                request.getRequestId(), request.getContentProjectId(), config.getModel());

        // ── 1. API key doğrulama ──
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.error("OpenAI API key yapılandırılmamış. requestId={}", request.getRequestId());
            return StoryboardAgentResponse.builder()
                    .success(false)
                    .errorCode("AI_PROVIDER_CONFIG_ERROR")
                    .errorMessage("OpenAI API key yapılandırılmamış.")
                    .retryable(false)
                    .providerType("OPENAI")
                    .modelName(config.getModel())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // ── 2. Prompt construction ──
        List<OpenAiChatMessage> messages = OpenAiStoryboardPromptBuilder.buildMessages(request);
        String promptVersion = OpenAiStoryboardPromptBuilder.getPromptVersion();
        log.debug("OpenAI storyboard prompt oluşturuldu: {} mesaj, promptVersion={}",
                messages.size(), promptVersion);

        // ── 3. HTTP çağrısı ──
        OpenAiCallResult callResult;
        try {
            callResult = client.call(messages, request.getRequestId(), request.getContentProjectId());
        } catch (Exception ex) {
            log.error("OpenAI storyboard gateway beklenmeyen hata: requestId={}, error={}",
                    request.getRequestId(), ex.getMessage());
            return StoryboardAgentResponse.builder()
                    .success(false)
                    .errorCode("UNKNOWN_AI_ERROR")
                    .errorMessage("Storyboard üretim servisi beklenmeyen hata.")
                    .retryable(true)
                    .providerType("OPENAI")
                    .modelName(config.getModel())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // ── 4. HTTP hata kontrolü ──
        if (!callResult.isSuccess()) {
            log.warn("OpenAI storyboard HTTP çağrısı başarısız: requestId={}, error={}, retryable={}",
                    request.getRequestId(), callResult.getErrorType(), callResult.isRetryable());
            return StoryboardAgentResponse.builder()
                    .success(false)
                    .errorCode(callResult.getErrorType() != null ? callResult.getErrorType().name() : "UNKNOWN_AI_ERROR")
                    .errorMessage(callResult.getErrorMessage())
                    .retryable(callResult.isRetryable())
                    .providerType("OPENAI")
                    .modelName(config.getModel())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // ── 5. Response parsing + validation ──
        StoryboardAgentResponse parsedResponse = OpenAiStoryboardResponseParser.parse(
                callResult.getContent(), "OPENAI", config.getModel());

        if (parsedResponse.isSuccess()) {
            log.info("OpenAI storyboard başarılı: requestId={}, contentProjectId={}, title={}, scenes={}",
                    request.getRequestId(), request.getContentProjectId(),
                    parsedResponse.getStoryboardTitle(),
                    parsedResponse.getScenes() != null ? parsedResponse.getScenes().size() : 0);
        } else {
            log.warn("OpenAI storyboard parse/validation hatası: requestId={}, errorCode={}",
                    request.getRequestId(), parsedResponse.getErrorCode());
        }

        return parsedResponse;
    }
}
