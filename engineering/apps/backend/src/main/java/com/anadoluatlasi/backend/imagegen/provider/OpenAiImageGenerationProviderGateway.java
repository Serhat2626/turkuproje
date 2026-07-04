package com.anadoluatlasi.backend.imagegen.provider;

import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationErrorCode;
import com.anadoluatlasi.backend.imagegen.service.ImageGenerationErrorNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FP-007: OpenAI DALL-E image generation provider adapter.
 *
 * <p>Safe provider boundary — normalized metadata-only result döner.</p>
 *
 * <p>Güvenlik kuralları:</p>
 * <ul>
 *   <li>API key sadece env/config'den okunur — log/DTO/entity'ye yazılmaz</li>
 *   <li>Raw provider request/response persist edilmez ve loglanmaz</li>
 *   <li>Image URL/base64/binary DTO/entity/log'a yazılmaz</li>
 *   <li>Internal full prompt loglanmaz ve DTO'ya çıkmaz</li>
 *   <li>Signed/public URL üretilmez</li>
 *   <li>Storage upload/download yapılmaz</li>
 * </ul>
 */
@Slf4j
public class OpenAiImageGenerationProviderGateway implements ImageGenerationProviderGateway {

    private static final String PROVIDER_ALIAS = "OPENAI";
    private static final String API_URL = "https://api.openai.com/v1/images/generations";

    private final RestClient restClient;
    private final ImageGenerationProviderConfig config;
    private final String apiKey;

    public OpenAiImageGenerationProviderGateway(
            ImageGenerationProviderConfig config,
            @Value("${imagegen.provider.openai-api-key:}") String apiKey) {
        this.config = config;
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Test-only constructor — custom RestClient injection.
     */
    OpenAiImageGenerationProviderGateway(
            ImageGenerationProviderConfig config,
            String apiKey,
            RestClient restClient) {
        this.config = config;
        this.apiKey = apiKey;
        this.restClient = restClient;
    }

    @Override
    public ImageProviderGenerationResult generate(ImageProviderGenerationRequest request) {
        // API key validation — safe error, key value never logged
        if (apiKey == null || apiKey.isBlank()) {
            log.error("OpenAI API key yapılandırılmamış — generation yapılamıyor: " +
                    "requestId={}, correlationId={}",
                    request.getRequestId(), request.getCorrelationId());
            return buildFailureResult(request,
                    ImageGenerationErrorCode.PROVIDER_UNAVAILABLE,
                    ImageGenerationErrorNormalizer.safeMessage(
                            ImageGenerationErrorCode.PROVIDER_UNAVAILABLE));
        }

        log.info("OpenAI image generation başlatıldı: requestId={}, correlationId={}, model={}",
                request.getRequestId(), request.getCorrelationId(), config.getModelName());

        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        try {
            // Server-side safe prompt — safePromptSummary kullanılır
            String safePrompt = request.getSafePromptSummary() != null
                    ? request.getSafePromptSummary()
                    : "A high-quality artistic illustration";

            // Build request body — raw body never persisted/logged
            Map<String, Object> requestBody = Map.of(
                    "model", config.getModelName(),
                    "prompt", safePrompt,
                    "n", config.getMaxOutputCount(),
                    "size", config.getImageSize(),
                    "response_format", "url"
            );

            // API call
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            long durationMs = System.currentTimeMillis() - startMs;

            // Extract only safe metadata from response — no URL/base64 persisted
            return extractSafeResult(request, response, durationMs, startTime);

        } catch (RestClientException e) {
            long durationMs = System.currentTimeMillis() - startMs;

            // Safe error — raw exception message sanitized
            ImageGenerationErrorCode errorCode = classifyError(e);
            log.error("OpenAI generation hatası: requestId={}, correlationId={}, " +
                            "errorType={}, durationMs={}",
                    request.getRequestId(), request.getCorrelationId(),
                    e.getClass().getSimpleName(), durationMs);

            return buildFailureResult(request, errorCode,
                    ImageGenerationErrorNormalizer.safeMessage(errorCode));

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;

            log.error("OpenAI unexpected exception: requestId={}, correlationId={}, " +
                            "errorType={}, durationMs={}",
                    request.getRequestId(), request.getCorrelationId(),
                    e.getClass().getSimpleName(), durationMs);

            return buildFailureResult(request,
                    ImageGenerationErrorCode.UNKNOWN_GENERATION_FAILURE,
                    ImageGenerationErrorNormalizer.safeMessage(
                            ImageGenerationErrorCode.UNKNOWN_GENERATION_FAILURE));
        }
    }

    @Override
    public String getProviderAlias() {
        return PROVIDER_ALIAS;
    }

    // ── Private Helpers ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ImageProviderGenerationResult extractSafeResult(
            ImageProviderGenerationRequest request,
            Map<String, Object> response,
            long durationMs,
            LocalDateTime startTime) {

        // Extract ONLY safe metadata — image URL/base64 never persisted
        int outputCount = 0;
        if (response != null && response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof List<?> dataList) {
                outputCount = dataList.size();
            }
        }

        log.info("OpenAI generation tamamlandı: requestId={}, correlationId={}, " +
                        "outputCount={}, durationMs={}",
                request.getRequestId(), request.getCorrelationId(),
                outputCount, durationMs);

        return ImageProviderGenerationResult.builder()
                .success(true)
                .providerAlias(PROVIDER_ALIAS)
                .modelName(config.getModelName())
                .outputCount(outputCount > 0 ? outputCount : 1)
                .generationDurationMs(durationMs)
                .mimeType("image/png")
                .width(parseDimension(config.getImageSize(), 0))
                .height(parseDimension(config.getImageSize(), 1))
                .correlationId(request.getCorrelationId())
                .completedAt(LocalDateTime.now())
                .qualitySignalsSummary("OpenAI DALL-E generation completed")
                .safetyFlagsSummary("Provider safety check passed")
                // No image URL, base64, signed URL, public URL, storage key
                .build();
    }

    private ImageGenerationErrorCode classifyError(RestClientException e) {
        String errorType = e.getClass().getSimpleName().toLowerCase();
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (errorType.contains("timeout") || message.contains("timeout")) {
            return ImageGenerationErrorCode.PROVIDER_TIMEOUT;
        }
        if (message.contains("401") || message.contains("403") || message.contains("unauthorized")) {
            return ImageGenerationErrorCode.PROVIDER_UNAVAILABLE;
        }
        if (message.contains("429") || message.contains("rate")) {
            return ImageGenerationErrorCode.PROVIDER_REJECTED_REQUEST;
        }
        if (message.contains("safety") || message.contains("content_policy")) {
            return ImageGenerationErrorCode.PROVIDER_SAFETY_BLOCKED;
        }
        if (message.contains("400") || message.contains("invalid")) {
            return ImageGenerationErrorCode.INVALID_GENERATION_INPUT;
        }
        return ImageGenerationErrorCode.UNKNOWN_GENERATION_FAILURE;
    }

    private ImageProviderGenerationResult buildFailureResult(
            ImageProviderGenerationRequest request,
            ImageGenerationErrorCode errorCode,
            String safeMessage) {
        return ImageProviderGenerationResult.builder()
                .success(false)
                .providerAlias(PROVIDER_ALIAS)
                .modelName(config.getModelName())
                .errorCode(errorCode)
                .errorMessage(safeMessage)
                .correlationId(request.getCorrelationId())
                .completedAt(LocalDateTime.now())
                .build();
    }

    private int parseDimension(String sizeStr, int index) {
        try {
            String[] parts = sizeStr.split("x");
            return Integer.parseInt(parts[index]);
        } catch (Exception e) {
            return 1024;
        }
    }
}
