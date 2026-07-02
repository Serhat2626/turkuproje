package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.ScenarioAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ScenarioAgentResponse;
import com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-002 Task 09: OpenAI Scenario Agent Gateway implementasyonu.
 *
 * <p>AiScenarioAgentGateway interface'ini implement eder.
 * {@code ai.provider=openai} olduğunda AiGatewayConfig tarafından bean olarak kaydedilir.</p>
 *
 * <p>Gateway kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu sınıf içinde kalır</li>
 *   <li>Request/Response contract'ları provider bağımsızdır</li>
 *   <li>Raw prompt/completion saklanmaz ve REST'e expose edilmez</li>
 *   <li>API key loglanmaz</li>
 *   <li>Senaryo onayı yapmaz — sadece candidate üretir</li>
 * </ul>
 */
@Slf4j
public class OpenAiScenarioAgentGateway implements AiScenarioAgentGateway {

    private final AiProviderProperties.OpenAi config;
    private final OpenAiClient client;

    public OpenAiScenarioAgentGateway(AiProviderProperties.OpenAi config) {
        this.config = config;
        this.client = new OpenAiClient(config);
    }

    /**
     * Test'te OpenAiClient inject etmek için paket-internal constructor.
     */
    OpenAiScenarioAgentGateway(AiProviderProperties.OpenAi config, OpenAiClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public ScenarioAgentResponse generateScenario(ScenarioAgentRequest request) {
        log.info("OpenAI scenario gateway çağrıldı: requestId={}, contentProjectId={}, model={}",
                request.getRequestId(), request.getContentProjectId(), config.getModel());

        // ── 1. API key doğrulama ──
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.error("OpenAI API key yapılandırılmamış. requestId={}", request.getRequestId());
            return ScenarioAgentResponse.builder()
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
        List<OpenAiChatMessage> messages = OpenAiScenarioPromptBuilder.buildMessages(request);
        String promptVersion = OpenAiScenarioPromptBuilder.getPromptVersion();
        log.debug("OpenAI scenario prompt oluşturuldu: {} mesaj, promptVersion={}",
                messages.size(), promptVersion);

        // ── 3. HTTP çağrısı ──
        OpenAiCallResult callResult;
        try {
            callResult = client.call(messages, request.getRequestId(), request.getContentProjectId());
        } catch (Exception ex) {
            log.error("OpenAI scenario gateway beklenmeyen hata: requestId={}, error={}",
                    request.getRequestId(), ex.getMessage());
            return ScenarioAgentResponse.builder()
                    .success(false)
                    .errorCode("UNKNOWN_AI_ERROR")
                    .errorMessage("Senaryo üretim servisi beklenmeyen hata.")
                    .retryable(true)
                    .providerType("OPENAI")
                    .modelName(config.getModel())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // ── 4. HTTP hata kontrolü ──
        if (!callResult.isSuccess()) {
            log.warn("OpenAI scenario HTTP çağrısı başarısız: requestId={}, error={}, retryable={}",
                    request.getRequestId(), callResult.getErrorType(), callResult.isRetryable());
            return ScenarioAgentResponse.builder()
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
        ScenarioAgentResponse parsedResponse = OpenAiScenarioResponseParser.parse(
                callResult.getContent(), "OPENAI", config.getModel());

        if (parsedResponse.isSuccess()) {
            log.info("OpenAI scenario başarılı: requestId={}, contentProjectId={}, title={}",
                    request.getRequestId(), request.getContentProjectId(), parsedResponse.getScenarioTitle());
        } else {
            log.warn("OpenAI scenario parse/validation hatası: requestId={}, errorCode={}",
                    request.getRequestId(), parsedResponse.getErrorCode());
        }

        return parsedResponse;
    }
}
