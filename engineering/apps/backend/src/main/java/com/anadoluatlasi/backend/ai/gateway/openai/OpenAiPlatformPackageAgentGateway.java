package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentRequest;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentResponse;
import com.anadoluatlasi.backend.ai.gateway.AiPlatformPackageAgentGateway;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-004 Task 09: OpenAI Platform Package Agent Gateway implementasyonu.
 *
 * <p>AiPlatformPackageAgentGateway interface'ini implement eder.
 * {@code ai.provider=openai} olduğunda AiGatewayConfig tarafından bean olarak kaydedilir.</p>
 *
 * <p>Gateway kuralları:</p>
 * <ul>
 *   <li>Provider SDK import'ları bu sınıf içinde kalır</li>
 *   <li>Request/Response contract'ları provider bağımsızdır</li>
 *   <li>Raw prompt/completion saklanmaz ve REST'e expose edilmez</li>
 *   <li>API key loglanmaz</li>
 *   <li>Platform paketi onayı yapmaz — sadece AI_GENERATED candidate üretir</li>
 *   <li>READY_FOR_PUBLISH transition yapmaz</li>
 *   <li>Medya dosyası üretmez</li>
 *   <li>Yayın yapmaz</li>
 * </ul>
 */
@Slf4j
public class OpenAiPlatformPackageAgentGateway implements AiPlatformPackageAgentGateway {

    private final AiProviderProperties.OpenAi config;
    private final OpenAiClient client;

    public OpenAiPlatformPackageAgentGateway(AiProviderProperties.OpenAi config) {
        this.config = config;
        this.client = new OpenAiClient(config);
    }

    /**
     * Test'te OpenAiClient inject etmek için paket-internal constructor.
     */
    OpenAiPlatformPackageAgentGateway(AiProviderProperties.OpenAi config, OpenAiClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public PlatformPackageAgentResponse generatePlatformPackages(PlatformPackageAgentRequest request) {
        log.info("OpenAI platform package gateway çağrıldı: requestId={}, contentProjectId={}, model={}, platforms={}",
                request.getRequestId(), request.getContentProjectId(), config.getModel(),
                request.getTargetPlatforms());

        // ── 1. API key doğrulama ──
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.error("OpenAI API key yapılandırılmamış. requestId={}", request.getRequestId());
            return PlatformPackageAgentResponse.builder()
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
        List<OpenAiChatMessage> messages = OpenAiPlatformPackagePromptBuilder.buildMessages(request);
        String promptVersion = OpenAiPlatformPackagePromptBuilder.getPromptVersion();
        log.debug("OpenAI platform package prompt oluşturuldu: {} mesaj, promptVersion={}",
                messages.size(), promptVersion);

        // ── 3. HTTP çağrısı ──
        OpenAiCallResult callResult;
        try {
            callResult = client.call(messages, request.getRequestId(), request.getContentProjectId());
        } catch (Exception ex) {
            log.error("OpenAI platform package gateway beklenmeyen hata: requestId={}, error={}",
                    request.getRequestId(), ex.getMessage());
            return PlatformPackageAgentResponse.builder()
                    .success(false)
                    .errorCode("UNKNOWN_AI_ERROR")
                    .errorMessage("Platform paketi üretim servisi beklenmeyen hata.")
                    .retryable(true)
                    .providerType("OPENAI")
                    .modelName(config.getModel())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // ── 4. HTTP hata kontrolü ──
        if (!callResult.isSuccess()) {
            log.warn("OpenAI platform package HTTP çağrısı başarısız: requestId={}, error={}, retryable={}",
                    request.getRequestId(), callResult.getErrorType(), callResult.isRetryable());
            return PlatformPackageAgentResponse.builder()
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
        PlatformPackageAgentResponse parsedResponse = OpenAiPlatformPackageResponseParser.parse(
                callResult.getContent(), "OPENAI", config.getModel());

        if (parsedResponse.isSuccess()) {
            log.info("OpenAI platform package başarılı: requestId={}, contentProjectId={}, packageCount={}",
                    request.getRequestId(), request.getContentProjectId(),
                    parsedResponse.getPackages() != null ? parsedResponse.getPackages().size() : 0);
        } else {
            log.warn("OpenAI platform package parse/validation hatası: requestId={}, errorCode={}",
                    request.getRequestId(), parsedResponse.getErrorCode());
        }

        return parsedResponse;
    }
}
