package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FP-001 Task 24: OpenAI Chat Completions HTTP client.
 *
 * <p>Provider-internal sınıf — REST'e expose edilmez.
 * API key Authorization header olarak eklenir, loglanmaz, prompt/response body loglanmaz.</p>
 */
@Slf4j
class OpenAiClient {

    private final RestClient restClient;
    private final AiProviderProperties.OpenAi config;

    OpenAiClient(AiProviderProperties.OpenAi config) {
        this.config = config;
        this.restClient = buildRestClient(config);
    }

    /**
     * Test'te RestClient inject etmek için paket-internal constructor.
     */
    OpenAiClient(RestClient restClient, AiProviderProperties.OpenAi config) {
        this.restClient = restClient;
        this.config = config;
    }

    /**
     * Chat completions çağrısı yapar.
     *
     * @param messages  prompt builder tarafından oluşturulan mesajlar
     * @param requestId correlation ID
     * @param projectId proje referansı
     * @return başarılı veya hata sonucu
     */
    OpenAiCallResult call(List<OpenAiChatMessage> messages, UUID requestId, UUID projectId) {
        long startMs = System.currentTimeMillis();

        try {
            // ── Request body oluştur ──
            ChatCompletionRequest requestBody = ChatCompletionRequest.builder()
                    .model(config.getModel())
                    .messages(messages.stream()
                            .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                            .toList())
                    .temperature(config.getTemperature())
                    .maxOutputTokens(config.getMaxOutputTokens())
                    .build();

            // ── HTTP çağrısı ──
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            long durationMs = System.currentTimeMillis() - startMs;
            log.info("OpenAI çağrısı başarılı: requestId={}, model={}, durationMs={}",
                    requestId, config.getModel(), durationMs);

            // ── Response extraction ──
            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                return OpenAiCallResult.failure(AiErrorType.EMPTY_AI_RESPONSE,
                        "OpenAI yanıtında choices boş döndü.", true, durationMs);
            }

            ChatCompletionResponse.Choice firstChoice = response.getChoices().get(0);

            // Content filter refusal kontrolü
            if ("content_filter".equals(firstChoice.getFinishReason())) {
                return OpenAiCallResult.failure(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT,
                        "OpenAI content filter yanıtı reddetti.", false, durationMs);
            }

            if (firstChoice.getMessage() == null || firstChoice.getMessage().getContent() == null) {
                return OpenAiCallResult.failure(AiErrorType.EMPTY_AI_RESPONSE,
                        "OpenAI yanıtında message.content boş döndü.", true, durationMs);
            }

            // Refusal alanı (OpenAI moderation)
            if (firstChoice.getMessage().getRefusal() != null
                    && !firstChoice.getMessage().getRefusal().isBlank()) {
                return OpenAiCallResult.failure(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT,
                        "OpenAI model refusal: " + firstChoice.getMessage().getRefusal(), false, durationMs);
            }

            return OpenAiCallResult.success(firstChoice.getMessage().getContent(), durationMs,
                    response.getUsage() != null ? formatUsage(response.getUsage()) : null);

        } catch (RestClientResponseException e) {
            long durationMs = System.currentTimeMillis() - startMs;
            return mapHttpError(e, requestId, durationMs);

        } catch (ResourceAccessException e) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.warn("OpenAI ağ hatası: requestId={}, durationMs={}, error={}",
                    requestId, durationMs, e.getMessage());

            // Timeout veya bağlantı hatası
            boolean isTimeout = e.getMessage() != null &&
                    (e.getMessage().contains("timed out") || e.getMessage().contains("Timeout") ||
                     e.getMessage().contains("Read timed out") || e.getMessage().contains("connect timed out"));

            AiErrorType errorType = isTimeout ? AiErrorType.AI_PROVIDER_TIMEOUT : AiErrorType.AI_PROVIDER_UNAVAILABLE;
            return OpenAiCallResult.failure(errorType,
                    "OpenAI bağlantı hatası: " + e.getMessage(), true, durationMs);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.error("OpenAI beklenmeyen hata: requestId={}, durationMs={}, error={}",
                    requestId, durationMs, e.getMessage());
            return OpenAiCallResult.failure(AiErrorType.UNKNOWN_AI_ERROR,
                    "OpenAI beklenmeyen hata: " + e.getClass().getSimpleName(), true, durationMs);
        }
    }

    // ── HTTP Error Mapping ──────────────────────────────────

    private OpenAiCallResult mapHttpError(RestClientResponseException e, UUID requestId, long durationMs) {
        HttpStatusCode status = e.getStatusCode();
        int code = status.value();

        log.warn("OpenAI HTTP hatası: requestId={}, status={}, durationMs={}",
                requestId, code, durationMs);

        return switch (code) {
            case 401, 403 -> OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_AUTH_ERROR,
                    "OpenAI kimlik doğrulama hatası (HTTP " + code + ").", false, durationMs);
            case 429 -> OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_RATE_LIMIT,
                    "OpenAI rate limit aşıldı (HTTP 429).", true, durationMs);
            case 408 -> OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_TIMEOUT,
                    "OpenAI zaman aşımı (HTTP 408).", true, durationMs);
            case 500, 502, 503, 504 -> OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_UNAVAILABLE,
                    "OpenAI sunucu hatası (HTTP " + code + ").", true, durationMs);
            default -> OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_INTERNAL_ERROR,
                    "OpenAI HTTP hatası (HTTP " + code + ").", true, durationMs);
        };
    }

    // ── RestClient Builder ──────────────────────────────────

    private static RestClient buildRestClient(AiProviderProperties.OpenAi config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.min(config.getTimeoutSeconds(), 10)));
        factory.setReadTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));

        return RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .requestFactory(factory)
                .build();
    }

    private String formatUsage(ChatCompletionResponse.Usage usage) {
        return "input:" + usage.getPromptTokens() + ",output:" + usage.getCompletionTokens();
    }

    // ── Internal DTOs ───────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class ChatCompletionRequest {
        private String model;
        private List<Map<String, String>> messages;
        private double temperature;
        @JsonProperty("max_output_tokens")
        private int maxOutputTokens;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatCompletionResponse {
        private List<Choice> choices;
        private Usage usage;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Choice {
            private Message message;
            @JsonProperty("finish_reason")
            private String finishReason;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Message {
            private String role;
            private String content;
            private String refusal;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Usage {
            @JsonProperty("prompt_tokens")
            private int promptTokens;
            @JsonProperty("completion_tokens")
            private int completionTokens;
            @JsonProperty("total_tokens")
            private int totalTokens;
        }
    }
}
