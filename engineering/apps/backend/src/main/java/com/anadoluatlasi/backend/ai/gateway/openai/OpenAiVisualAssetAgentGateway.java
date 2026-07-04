package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.VisualAssetPlanAgentRequest;
import com.anadoluatlasi.backend.ai.contract.VisualAssetPlanAgentResponse;
import com.anadoluatlasi.backend.ai.gateway.AiVisualAssetAgentGateway;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-006 Task 09: OpenAI Visual Asset Plan Agent Gateway implementasyonu.
 *
 * <p>AiVisualAssetAgentGateway interface'ini implement eder.
 * {@code ai.provider=openai} olduğunda AiGatewayConfig tarafından bean olarak kaydedilir.</p>
 *
 * <p>Bu gateway yalnızca metin prompt planı üretir. Image generation API
 * (DALL-E, Midjourney, Stable Diffusion) çağırmaz, görsel dosya üretmez,
 * MediaAsset/GeneratedAsset oluşturmaz.</p>
 *
 * <p>Gateway kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu sınıf içinde kalır</li>
 *   <li>Request/Response contract'ları provider bağımsızdır</li>
 *   <li>Raw prompt/completion saklanmaz ve REST'e expose edilmez</li>
 *   <li>API key loglanmaz</li>
 *   <li>Görsel plan onayı yapmaz — sadece AI_GENERATED candidate üretir</li>
 *   <li>READY_FOR_PUBLISH transition yapmaz</li>
 *   <li>Medya dosyası üretmez, image URL oluşturmaz</li>
 *   <li>Yayın yapmaz</li>
 * </ul>
 */
@Slf4j
public class OpenAiVisualAssetAgentGateway implements AiVisualAssetAgentGateway {

    private final AiProviderProperties.OpenAi config;
    private final OpenAiClient client;

    public OpenAiVisualAssetAgentGateway(AiProviderProperties.OpenAi config) {
        this.config = config;
        this.client = new OpenAiClient(config);
    }

    /**
     * Test'te OpenAiClient inject etmek için paket-internal constructor.
     */
    OpenAiVisualAssetAgentGateway(AiProviderProperties.OpenAi config, OpenAiClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public VisualAssetPlanAgentResponse generateVisualAssetPlan(VisualAssetPlanAgentRequest request) {
        log.info("OpenAI visual asset gateway çağrıldı: requestId={}, contentProjectId={}, model={}, sceneCount={}",
                request.getRequestId(), request.getContentProjectId(), config.getModel(),
                request.getScenes() != null ? request.getScenes().size() : 0);

        // ── 1. API key doğrulama ──
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.error("OpenAI API key yapılandırılmamış. requestId={}", request.getRequestId());
            return VisualAssetPlanAgentResponse.builder()
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
        List<OpenAiChatMessage> messages = OpenAiVisualAssetPromptBuilder.buildMessages(request);
        String promptVersion = OpenAiVisualAssetPromptBuilder.getPromptVersion();
        log.debug("OpenAI visual asset prompt oluşturuldu: {} mesaj, promptVersion={}",
                messages.size(), promptVersion);

        // ── 3. HTTP çağrısı ──
        OpenAiCallResult callResult;
        try {
            callResult = client.call(messages, request.getRequestId(), request.getContentProjectId());
        } catch (Exception ex) {
            log.error("OpenAI visual asset gateway beklenmeyen hata: requestId={}, error={}",
                    request.getRequestId(), ex.getMessage());
            return VisualAssetPlanAgentResponse.builder()
                    .success(false)
                    .errorCode("UNKNOWN_AI_ERROR")
                    .errorMessage("Görsel varlık planı üretim servisi beklenmeyen hata.")
                    .retryable(true)
                    .providerType("OPENAI")
                    .modelName(config.getModel())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // ── 4. HTTP hata kontrolü ──
        if (!callResult.isSuccess()) {
            log.warn("OpenAI visual asset HTTP çağrısı başarısız: requestId={}, error={}, retryable={}",
                    request.getRequestId(), callResult.getErrorType(), callResult.isRetryable());
            return VisualAssetPlanAgentResponse.builder()
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
        int expectedSceneCount = request.getScenes() != null ? request.getScenes().size() : 0;
        VisualAssetPlanAgentResponse parsedResponse = OpenAiVisualAssetResponseParser.parse(
                callResult.getContent(), expectedSceneCount, "OPENAI", config.getModel());

        if (parsedResponse.isSuccess()) {
            log.info("OpenAI visual asset başarılı: requestId={}, contentProjectId={}, scenePlanCount={}",
                    request.getRequestId(), request.getContentProjectId(),
                    parsedResponse.getScenePlans() != null ? parsedResponse.getScenePlans().size() : 0);
        } else {
            log.warn("OpenAI visual asset parse/validation hatası: requestId={}, errorCode={}",
                    request.getRequestId(), parsedResponse.getErrorCode());
        }

        return parsedResponse;
    }
}
