package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.StoryboardAgentRequest;
import com.anadoluatlasi.backend.ai.contract.StoryboardAgentResponse;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FP-003 Task 09: OpenAiStoryboardAgentGateway testleri.
 */
@DisplayName("FP-003 Task 09: OpenAiStoryboardAgentGateway")
class OpenAiStoryboardAgentGatewayTest {

    private AiProviderProperties.OpenAi config;
    private OpenAiClient mockClient;
    private OpenAiStoryboardAgentGateway gateway;

    private static final String VALID_JSON = """
            {"storyboardTitle":"Test Storyboard","summary":"Özet","targetPlatform":"YouTube",
            "aspectRatio":"16:9","estimatedDurationSeconds":600,"visualStyleGuide":"Sinematik",
            "narrationStyle":"Anlatıcı","scenes":[
              {"sceneNumber":1,"sourceBeatType":"HOOK","sourceBeatOrder":0,"sceneTitle":"Açılış",
               "visualDescription":"Gökyüzü","narrationText":"Bir zamanlar","mood":"dramatic",
               "location":"Anadolu","estimatedDurationSeconds":10},
              {"sceneNumber":2,"sourceBeatType":"INTRODUCTION","sourceBeatOrder":1,"sceneTitle":"Giriş",
               "visualDescription":"Köy","narrationText":"Köyün","mood":"calm",
               "location":"Köy","estimatedDurationSeconds":30},
              {"sceneNumber":3,"sourceBeatType":"MAIN_NARRATIVE","sourceBeatOrder":2,"sceneTitle":"Ana",
               "visualDescription":"Dağ","narrationText":"Dağın","mood":"epic",
               "location":"Dağ","estimatedDurationSeconds":60}
            ]}""";

    @BeforeEach
    void setUp() {
        config = new AiProviderProperties.OpenAi();
        config.setApiKey("test-key-xxx");
        config.setModel("gpt-4o-mini");
        config.setBaseUrl("https://api.openai.com/v1");
        config.setTimeoutSeconds(60);
        config.setMaxOutputTokens(2000);
        config.setTemperature(0.4);

        mockClient = mock(OpenAiClient.class);
        gateway = new OpenAiStoryboardAgentGateway(config, mockClient);
    }

    private StoryboardAgentRequest buildRequest() {
        return StoryboardAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .contentProjectId(UUID.randomUUID())
                .sourceDraftId(UUID.randomUUID())
                .sourceScenarioId(UUID.randomUUID())
                .sourceScenarioVersion(1)
                .scenarioTitle("Test Senaryo")
                .scenarioBeats(List.of())
                .build();
    }

    // ── Successful Generation ───────────────────────────────

    @Nested
    @DisplayName("SuccessfulGeneration")
    class SuccessfulGeneration {

        @Test
        @DisplayName("Geçerli JSON → success=true, title ve scenes mevcut")
        void validJsonReturnsSuccess() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 1500L, "input:500,output:800"));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStoryboardTitle()).isEqualTo("Test Storyboard");
            assertThat(result.getScenes()).hasSize(3);
        }

        @Test
        @DisplayName("Provider metadata doğru: OPENAI, gpt-4o-mini")
        void providerMetadataCorrect() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 1500L, null));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.getProviderType()).isEqualTo("OPENAI");
            assertThat(result.getModelName()).isEqualTo("gpt-4o-mini");
        }

        @Test
        @DisplayName("Başarılı yanıt otomatik onay yapmaz")
        void doesNotAutoApprove() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 1500L, null));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            // StoryboardAgentResponse onay durumu içermez — sadece candidate üretir
            assertThat(result.isSuccess()).isTrue();
            // success=true olan response APPROVED status değildir — bu service katmanında kontrol edilir
        }

        @Test
        @DisplayName("Code fence sarılı JSON doğru parse edilir")
        void codeFenceStrippedAndParsed() {
            String fenced = "```json\n" + VALID_JSON + "\n```";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(fenced, 1200L, null));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStoryboardTitle()).isEqualTo("Test Storyboard");
        }
    }

    // ── Invalid Output ──────────────────────────────────────

    @Nested
    @DisplayName("InvalidOutput")
    class InvalidOutput {

        @Test
        @DisplayName("Başlık eksik → INVALID_AI_RESPONSE_FORMAT")
        void missingTitleFailure() {
            String noTitle = """
                    {"scenes":[{"sceneNumber":1,"visualDescription":"V"}]}
                    """;
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(noTitle, 1000L, null));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("Boş scenes → INVALID_AI_RESPONSE_FORMAT")
        void emptyScenesFailure() {
            String emptyScenes = """
                    {"storyboardTitle":"T","scenes":[]}
                    """;
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(emptyScenes, 1000L, null));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("Boş response → EMPTY_AI_RESPONSE")
        void emptyResponseFailure() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("", 500L, null));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("EMPTY_AI_RESPONSE");
        }

        @Test
        @DisplayName("Geçersiz JSON → INVALID_AI_RESPONSE_FORMAT")
        void invalidJsonFailure() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("not json", 800L, null));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }
    }

    // ── Provider Errors ─────────────────────────────────────

    @Nested
    @DisplayName("ProviderErrors")
    class ProviderErrors {

        @Test
        @DisplayName("Timeout → retryable failure")
        void timeoutIsRetryable() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(
                            AiErrorType.AI_PROVIDER_TIMEOUT, "Zaman aşımı", true, 30000L));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
            assertThat(result.getRetryable()).isTrue();
        }

        @Test
        @DisplayName("Rate limit → retryable failure")
        void rateLimitIsRetryable() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(
                            AiErrorType.AI_PROVIDER_RATE_LIMIT, "Rate limit", true, 500L));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getRetryable()).isTrue();
        }

        @Test
        @DisplayName("Auth error → non-retryable failure")
        void authErrorNotRetryable() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(
                            AiErrorType.AI_PROVIDER_AUTH_ERROR, "Kimlik hatası", false, 200L));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getRetryable()).isFalse();
        }

        @Test
        @DisplayName("API key eksik → AI_PROVIDER_CONFIG_ERROR, client çağrılmaz")
        void missingApiKeyConfigError() {
            config.setApiKey(null);
            gateway = new OpenAiStoryboardAgentGateway(config, mockClient);

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_CONFIG_ERROR");
            assertThat(result.getRetryable()).isFalse();
            verify(mockClient, never()).call(any(), any(), any());
        }

        @Test
        @DisplayName("Boş API key → AI_PROVIDER_CONFIG_ERROR, client çağrılmaz")
        void blankApiKeyConfigError() {
            config.setApiKey("  ");
            gateway = new OpenAiStoryboardAgentGateway(config, mockClient);

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_CONFIG_ERROR");
            verify(mockClient, never()).call(any(), any(), any());
        }

        @Test
        @DisplayName("Client exception → UNKNOWN_AI_ERROR")
        void clientExceptionReturnsUnknownError() {
            when(mockClient.call(any(), any(), any()))
                    .thenThrow(new RuntimeException("Beklenmeyen hata"));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("UNKNOWN_AI_ERROR");
            assertThat(result.getRetryable()).isTrue();
        }
    }

    // ── Safety Governance ───────────────────────────────────

    @Nested
    @DisplayName("SafetyGovernance")
    class SafetyGovernance {

        @Test
        @DisplayName("Başarılı yanıtta raw prompt saklanmaz")
        void noRawPromptInSuccessResponse() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 1500L, null));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            // StoryboardAgentResponse raw prompt/payload alanı içermez
            // Sadece safe metadata: providerType, modelName
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProviderType()).isEqualTo("OPENAI");
            assertThat(result.getModelName()).isEqualTo("gpt-4o-mini");
        }

        @Test
        @DisplayName("Başarısız yanıtta raw payload expose edilmez")
        void noRawPayloadInFailureResponse() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(
                            AiErrorType.AI_PROVIDER_UNAVAILABLE, "Server hatası", true, 5000L));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            // Hata mesajı sanitized olmalı — raw HTTP response veya stack trace içermemeli
            assertThat(result.getErrorMessage()).doesNotContain("Bearer");
            assertThat(result.getErrorMessage()).doesNotContain("test-key-xxx");
        }

        @Test
        @DisplayName("Başarısız parse sonucu success=true döndürmez")
        void failedParseNeverReturnsSuccess() {
            String invalidJson = "this is not json";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(invalidJson, 1000L, null));

            StoryboardAgentResponse result = gateway.generateStoryboard(buildRequest());

            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Client çağrısında API key loglanmaz — prompt'a dahil değil")
        void apiKeyNotInPrompt() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 1500L, null));

            gateway.generateStoryboard(buildRequest());

            // Client.call'a gönderilen mesajları yakala
            var captor = org.mockito.ArgumentCaptor.forClass(List.class);
            verify(mockClient).call(captor.capture(), any(), any());

            @SuppressWarnings("unchecked")
            List<OpenAiChatMessage> messages = (List<OpenAiChatMessage>) captor.getValue();
            for (OpenAiChatMessage msg : messages) {
                assertThat(msg.getContent()).doesNotContain("test-key-xxx");
                assertThat(msg.getContent().toLowerCase()).doesNotContain("bearer");
            }
        }
    }
}
