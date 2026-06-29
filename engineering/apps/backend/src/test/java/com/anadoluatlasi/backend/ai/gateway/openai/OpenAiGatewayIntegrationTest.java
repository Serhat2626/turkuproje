package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.ContentAgentRequest;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.ai.gateway.AiGatewayResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-001 Task 24: OpenAI gateway tam entegrasyon testleri.
 *
 * <p>Real API çağrısı yapılmaz. OpenAiClient mock'lanır.</p>
 */
class OpenAiGatewayIntegrationTest {

    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    private AiProviderProperties.OpenAi config;
    private OpenAiClient mockClient;

    @BeforeEach
    void setUp() {
        config = new AiProviderProperties.OpenAi();
        config.setApiKey("sk-test-dummy-key");
        mockClient = mock(OpenAiClient.class);
    }

    // ── Success Path ────────────────────────────────────────

    @Nested
    @DisplayName("Başarılı provider yanıtı")
    class SuccessPath {

        @Test
        @DisplayName("Geçerli JSON yanıt → AiGatewayResult.success")
        void shouldReturnSuccessForValidResponse() {
            String validJson = buildValidJson();
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(validJson, 1200L, "input:500,output:300"));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getResponse()).isNotNull();
            assertThat(result.getResponse().getTitle()).isEqualTo("Ankara Kalesi");
            assertThat(result.getResponse().getBody()).contains("Ankara");
            assertThat(result.getResponse().getSummary()).isEqualTo("Kısa özet");
        }

        @Test
        @DisplayName("Provider metadata eklenmeli")
        void shouldIncludeProviderMetadata() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(buildValidJson(), 1500L, "input:600,output:400"));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.getResponse().getProviderMetadata()).isNotNull();
            assertThat(result.getResponse().getProviderMetadata().getProviderName()).isEqualTo("openai");
            assertThat(result.getResponse().getProviderMetadata().getModelName()).isEqualTo(config.getModel());
            assertThat(result.getResponse().getProviderMetadata().getDurationMs()).isEqualTo(1500L);
            assertThat(result.getResponse().getProviderMetadata().getTokenUsage()).isEqualTo("input:600,output:400");
            assertThat(result.getResponse().getProviderMetadata().getRawResponseStored()).isFalse();
        }

        @Test
        @DisplayName("Başarılı yanıt APPROVED veya READY_FOR_PUBLISH döndürmez")
        void shouldNeverProduceApprovedOrPublishStatus() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(buildValidJson(), 800L, null));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            // Gateway sadece draft content taşır — status manipülasyonu yapmaz
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getResponse()).isNotNull();
            // ContentAgentResponse'da status alanı yok — tasarımı gereği
        }
    }

    // ── Markdown Fence ──────────────────────────────────────

    @Nested
    @DisplayName("Markdown fenced JSON")
    class MarkdownFence {

        @Test
        @DisplayName("```json ... ``` içindeki yanıt başarıyla parse edilmeli")
        void shouldParseMarkdownFencedJson() {
            String fenced = "```json\n" + buildValidJson() + "\n```";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(fenced, 900L, null));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getResponse().getTitle()).isEqualTo("Ankara Kalesi");
        }
    }

    // ── Empty Response ──────────────────────────────────────

    @Nested
    @DisplayName("Boş provider yanıtı")
    class EmptyResponse {

        @Test
        @DisplayName("Boş content → EMPTY_AI_RESPONSE")
        void shouldMapEmptyContentToError() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("", 500L, null));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.EMPTY_AI_RESPONSE);
        }

        @Test
        @DisplayName("Client EMPTY_AI_RESPONSE → gateway failure")
        void shouldPropagateClientEmptyError() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.EMPTY_AI_RESPONSE, "Choices boş", true, 300L));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.EMPTY_AI_RESPONSE);
            assertThat(result.getError().isRetryable()).isTrue();
        }
    }

    // ── Invalid JSON ────────────────────────────────────────

    @Nested
    @DisplayName("Invalid JSON yanıt")
    class InvalidJson {

        @Test
        @DisplayName("Geçersiz JSON → INVALID_AI_RESPONSE_FORMAT")
        void shouldMapInvalidJsonToError() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("Bu JSON değil", 600L, null));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.INVALID_AI_RESPONSE_FORMAT);
            assertThat(result.getError().isRetryable()).isTrue();
        }
    }

    // ── Too Short ───────────────────────────────────────────

    @Nested
    @DisplayName("Çok kısa yanıt")
    class TooShort {

        @Test
        @DisplayName("Kısa body → AI_RESPONSE_TOO_SHORT")
        void shouldMapShortBodyToError() {
            String shortJson = """
                    {"title": "Başlık", "body": "Kısa.", "summary": "Özet"}
                    """;
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(shortJson, 400L, null));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.AI_RESPONSE_TOO_SHORT);
        }
    }

    // ── Unsafe/Refusal ──────────────────────────────────────

    @Nested
    @DisplayName("Unsafe/refusal yanıt")
    class UnsafeRefusal {

        @Test
        @DisplayName("Refusal text → AI_RESPONSE_UNSAFE_CONTENT, retryable=false")
        void shouldMapRefusalToUnsafe() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("I cannot generate this content due to safety guidelines.", 200L, null));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT);
            assertThat(result.getError().isRetryable()).isFalse();
        }

        @Test
        @DisplayName("Client content_filter → AI_RESPONSE_UNSAFE_CONTENT")
        void shouldPropagateClientContentFilter() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT, "Content filter", false, 300L));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT);
            assertThat(result.getError().isRetryable()).isFalse();
        }
    }

    // ── Config Error ────────────────────────────────────────

    @Nested
    @DisplayName("Config hatası")
    class ConfigError {

        @Test
        @DisplayName("API key eksik → AI_PROVIDER_CONFIG_ERROR, HTTP call yapılmaz")
        void shouldFailWithoutApiKey() {
            config.setApiKey(null);
            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.AI_PROVIDER_CONFIG_ERROR);
            assertThat(result.getError().isRetryable()).isFalse();
            verify(mockClient, never()).call(any(), any(), any());
        }

        @Test
        @DisplayName("API key boş → AI_PROVIDER_CONFIG_ERROR, HTTP call yapılmaz")
        void shouldFailWithBlankApiKey() {
            config.setApiKey("   ");
            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.AI_PROVIDER_CONFIG_ERROR);
            verify(mockClient, never()).call(any(), any(), any());
        }
    }

    // ── HTTP 401/403 ────────────────────────────────────────

    @Nested
    @DisplayName("HTTP 401/403")
    class AuthError {

        @Test
        @DisplayName("401 → AI_PROVIDER_AUTH_ERROR, retryable=false")
        void shouldMapHttp401() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_AUTH_ERROR, "HTTP 401", false, 100L));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.AI_PROVIDER_AUTH_ERROR);
            assertThat(result.getError().isRetryable()).isFalse();
        }
    }

    // ── HTTP 429 ────────────────────────────────────────────

    @Nested
    @DisplayName("HTTP 429")
    class RateLimit {

        @Test
        @DisplayName("429 → AI_PROVIDER_RATE_LIMIT, retryable=true")
        void shouldMapHttp429() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_RATE_LIMIT, "HTTP 429", true, 50L));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.AI_PROVIDER_RATE_LIMIT);
            assertThat(result.getError().isRetryable()).isTrue();
        }
    }

    // ── HTTP 500/503 ────────────────────────────────────────

    @Nested
    @DisplayName("HTTP 500/503")
    class ServerError {

        @Test
        @DisplayName("500 → AI_PROVIDER_UNAVAILABLE, retryable=true")
        void shouldMapHttp500() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_UNAVAILABLE, "HTTP 500", true, 200L));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.AI_PROVIDER_UNAVAILABLE);
            assertThat(result.getError().isRetryable()).isTrue();
        }
    }

    // ── Timeout ─────────────────────────────────────────────

    @Nested
    @DisplayName("Timeout/Network hatası")
    class TimeoutError {

        @Test
        @DisplayName("Timeout → AI_PROVIDER_TIMEOUT, retryable=true")
        void shouldMapTimeout() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_TIMEOUT, "Read timed out", true, 60000L));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.AI_PROVIDER_TIMEOUT);
            assertThat(result.getError().isRetryable()).isTrue();
        }

        @Test
        @DisplayName("Network unavailable → AI_PROVIDER_UNAVAILABLE, retryable=true")
        void shouldMapNetworkError() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_UNAVAILABLE, "Connection refused", true, 500L));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.AI_PROVIDER_UNAVAILABLE);
        }
    }

    // ── Unknown Exception ───────────────────────────────────

    @Nested
    @DisplayName("Bilinmeyen hata")
    class UnknownError {

        @Test
        @DisplayName("Unknown → UNKNOWN_AI_ERROR, retryable=true")
        void shouldMapUnknownError() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.UNKNOWN_AI_ERROR, "Unexpected", true, 100L));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType()).isEqualTo(AiErrorType.UNKNOWN_AI_ERROR);
        }

        @Test
        @DisplayName("Hata durumunda requestId/projectId error'da taşınır")
        void errorShouldContainIds() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.UNKNOWN_AI_ERROR, "Error", true, 50L));

            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config, mockClient);
            AiGatewayResult result = gateway.generateContentDraft(buildRequest());

            assertThat(result.getError().getRequestId()).isEqualTo(REQUEST_ID);
            assertThat(result.getError().getProjectId()).isEqualTo(PROJECT_ID);
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private ContentAgentRequest buildRequest() {
        return ContentAgentRequest.builder()
                .requestId(REQUEST_ID)
                .projectId(PROJECT_ID)
                .contentPurpose("INFORMATIONAL_TEXT")
                .targetAudience("GENERAL_USER")
                .tone("INFORMATIVE")
                .language("tr")
                .build();
    }

    private String buildValidJson() {
        return """
                {
                  "title": "Ankara Kalesi",
                  "summary": "Kısa özet",
                  "body": "Ankara'nın kalbinde yükselen Ankara Kalesi, tarih boyunca pek çok medeniyete ev sahipliği yapmıştır. Roma, Bizans ve Osmanlı dönemlerinde önemli bir savunma yapısı olarak kullanılmıştır.",
                  "suggestedTags": ["tarih", "ankara"],
                  "warnings": [],
                  "qualityNotes": "İyi kalite",
                  "generationReason": "Bilgilendirici yaklaşım"
                }
                """;
    }
}
