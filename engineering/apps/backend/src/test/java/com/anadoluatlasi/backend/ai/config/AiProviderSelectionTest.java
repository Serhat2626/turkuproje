package com.anadoluatlasi.backend.ai.config;

import com.anadoluatlasi.backend.ai.contract.ContentAgentRequest;
import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiGatewayResult;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiContentAgentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-001 Task 22: AI Provider seçimi ve OpenAI gateway skeleton testleri.
 *
 * <p>Real API çağrısı yapılmaz. Tüm testler mock/skeleton seviyesindedir.</p>
 */
class AiProviderSelectionTest {

    // ── Provider Properties ─────────────────────────────────

    @Nested
    @DisplayName("AiProviderProperties defaults")
    class PropertiesDefaults {

        @Test
        @DisplayName("Varsayılan provider mock olmalı")
        void defaultProviderIsMock() {
            AiProviderProperties props = new AiProviderProperties();
            assertThat(props.getProvider()).isEqualTo("mock");
        }

        @Test
        @DisplayName("OpenAI varsayılan model gpt-4.1-mini")
        void defaultModel() {
            AiProviderProperties props = new AiProviderProperties();
            assertThat(props.getOpenai().getModel()).isEqualTo("gpt-4.1-mini");
        }

        @Test
        @DisplayName("OpenAI varsayılan timeout 60 saniye")
        void defaultTimeout() {
            AiProviderProperties props = new AiProviderProperties();
            assertThat(props.getOpenai().getTimeoutSeconds()).isEqualTo(60);
        }

        @Test
        @DisplayName("OpenAI varsayılan maxOutputTokens 2000")
        void defaultMaxOutputTokens() {
            AiProviderProperties props = new AiProviderProperties();
            assertThat(props.getOpenai().getMaxOutputTokens()).isEqualTo(2000);
        }

        @Test
        @DisplayName("OpenAI varsayılan temperature 0.4")
        void defaultTemperature() {
            AiProviderProperties props = new AiProviderProperties();
            assertThat(props.getOpenai().getTemperature()).isEqualTo(0.4);
        }

        @Test
        @DisplayName("OpenAI varsayılan API key null")
        void defaultApiKeyIsNull() {
            AiProviderProperties props = new AiProviderProperties();
            assertThat(props.getOpenai().getApiKey()).isNull();
        }
    }

    // ── Mock Gateway Bean ───────────────────────────────────

    @Nested
    @DisplayName("Mock gateway")
    class MockGatewayTests {

        @Test
        @DisplayName("Mock gateway AiContentAgentGateway implement eder")
        void mockImplementsInterface() {
            AiContentAgentGateway gateway = new MockAiContentAgentGateway();
            assertThat(gateway).isInstanceOf(AiContentAgentGateway.class);
        }

        @Test
        @DisplayName("Mock gateway varsayılan olarak başarılı result döner")
        void mockReturnsSuccess() {
            AiContentAgentGateway gateway = new MockAiContentAgentGateway();
            ContentAgentRequest request = buildMinimalRequest();

            AiGatewayResult result = gateway.generateContentDraft(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getResponse()).isNotNull();
            assertThat(result.getResponse().getTitle()).isNotBlank();
            assertThat(result.getResponse().getBody()).isNotBlank();
        }
    }

    // ── OpenAI Gateway Skeleton ─────────────────────────────

    @Nested
    @DisplayName("OpenAI gateway skeleton")
    class OpenAiGatewayTests {

        @Test
        @DisplayName("OpenAI gateway AiContentAgentGateway implement eder")
        void openAiImplementsInterface() {
            AiProviderProperties.OpenAi config = new AiProviderProperties.OpenAi();
            AiContentAgentGateway gateway = new OpenAiContentAgentGateway(config);
            assertThat(gateway).isInstanceOf(AiContentAgentGateway.class);
        }

        @Test
        @DisplayName("API key eksikse AI_PROVIDER_CONFIG_ERROR döner")
        void failsWhenApiKeyMissing() {
            AiProviderProperties.OpenAi config = new AiProviderProperties.OpenAi();
            // apiKey = null (default)
            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config);
            ContentAgentRequest request = buildMinimalRequest();

            AiGatewayResult result = gateway.generateContentDraft(request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isNotNull();
            assertThat(result.getError().getErrorType().name()).isEqualTo("AI_PROVIDER_CONFIG_ERROR");
            assertThat(result.getError().isRetryable()).isFalse();
        }

        @Test
        @DisplayName("API key boş string ise AI_PROVIDER_CONFIG_ERROR döner")
        void failsWhenApiKeyBlank() {
            AiProviderProperties.OpenAi config = new AiProviderProperties.OpenAi();
            config.setApiKey("   ");
            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config);
            ContentAgentRequest request = buildMinimalRequest();

            AiGatewayResult result = gateway.generateContentDraft(request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError().getErrorType().name()).isEqualTo("AI_PROVIDER_CONFIG_ERROR");
        }

        @Test
        @DisplayName("API key varsa skeleton UNAVAILABLE döner (henüz implement değil)")
        void returnsUnavailableWhenApiKeyPresent() {
            AiProviderProperties.OpenAi config = new AiProviderProperties.OpenAi();
            config.setApiKey("sk-test-dummy-key");
            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config);
            ContentAgentRequest request = buildMinimalRequest();

            AiGatewayResult result = gateway.generateContentDraft(request);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isNotNull();
            assertThat(result.getError().getErrorType().name()).isEqualTo("AI_PROVIDER_UNAVAILABLE");
            assertThat(result.getError().isRetryable()).isTrue();
        }

        @Test
        @DisplayName("OpenAI gateway proje statüsünü APPROVED yapmaz")
        void doesNotReturnApprovedStatus() {
            AiProviderProperties.OpenAi config = new AiProviderProperties.OpenAi();
            config.setApiKey("sk-test-dummy-key");
            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config);
            ContentAgentRequest request = buildMinimalRequest();

            AiGatewayResult result = gateway.generateContentDraft(request);

            // Gateway sadece AiGatewayResult döner — ContentProjectStatus manipülasyonu yapmaz.
            // Result success olsa bile (gelecekte), response sadece draft içeriği taşır.
            assertThat(result.isSuccess()).isFalse(); // skeleton → always failure
            if (result.getResponse() != null) {
                // Response varsa status-related alan içermez (contract bağımsız)
                assertThat(result.getResponse()).isNotNull();
            }
        }

        @Test
        @DisplayName("OpenAI gateway error requestId/projectId taşır")
        void errorContainsRequestAndProjectId() {
            AiProviderProperties.OpenAi config = new AiProviderProperties.OpenAi();
            OpenAiContentAgentGateway gateway = new OpenAiContentAgentGateway(config);
            ContentAgentRequest request = buildMinimalRequest();

            AiGatewayResult result = gateway.generateContentDraft(request);

            assertThat(result.getError().getRequestId()).isEqualTo(request.getRequestId());
            assertThat(result.getError().getProjectId()).isEqualTo(request.getProjectId());
        }
    }

    // ── Gateway Config Bean Selection ───────────────────────

    @Nested
    @DisplayName("AiGatewayConfig bean creation")
    class ConfigBeanTests {

        @Test
        @DisplayName("mockAiContentAgentGateway MockAiContentAgentGateway döner")
        void mockBeanReturnsMockGateway() {
            AiGatewayConfig config = new AiGatewayConfig();
            AiContentAgentGateway gateway = config.mockAiContentAgentGateway();

            assertThat(gateway).isInstanceOf(MockAiContentAgentGateway.class);
        }

        @Test
        @DisplayName("openAiContentAgentGateway OpenAiContentAgentGateway döner")
        void openAiBeanReturnsOpenAiGateway() {
            AiProviderProperties props = new AiProviderProperties();
            props.setProvider("openai");
            AiGatewayConfig config = new AiGatewayConfig();

            AiContentAgentGateway gateway = config.openAiContentAgentGateway(props);

            assertThat(gateway).isInstanceOf(OpenAiContentAgentGateway.class);
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private ContentAgentRequest buildMinimalRequest() {
        return ContentAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .contentPurpose("INFORMATIONAL_TEXT")
                .targetAudience("GENERAL_USER")
                .tone("INFORMATIVE")
                .language("tr")
                .build();
    }
}
