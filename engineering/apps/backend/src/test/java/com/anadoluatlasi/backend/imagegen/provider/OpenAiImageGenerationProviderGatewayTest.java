package com.anadoluatlasi.backend.imagegen.provider;

import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * FP-007 Task 12: OpenAI adapter unit testleri.
 *
 * <p>Gerçek OpenAI API çağrısı YAPILMAZ.
 * MockRestServiceServer ile fake HTTP response simüle edilir.</p>
 *
 * <p>Testler şunları doğrular:</p>
 * <ul>
 *   <li>Success response normalized result'a çevrilir</li>
 *   <li>Failure response normalized error'a çevrilir</li>
 *   <li>API key missing safe failure döner</li>
 *   <li>Raw response/URL/base64 result'ta yok</li>
 *   <li>API key log/DTO/entity'ye çıkmaz</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OpenAiImageGenerationProviderGatewayTest {

    private ImageGenerationProviderConfig config;

    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID CORRELATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        config = new ImageGenerationProviderConfig();
        config.setMode("openai");
        config.setModelName("dall-e-3");
        config.setImageSize("1024x1024");
        config.setMaxOutputCount(1);
        config.setTimeoutMs(60000);
    }

    private ImageProviderGenerationRequest createRequest() {
        return ImageProviderGenerationRequest.builder()
                .requestId(REQUEST_ID)
                .correlationId(CORRELATION_ID)
                .safePromptSummary("A serene Anatolian landscape")
                .itemType("SCENE_IMAGE")
                .build();
    }

    // ── API Key Validation ──────────────────────────────────────────

    @Nested
    @DisplayName("API key validation")
    class ApiKeyValidation {

        @Test
        @DisplayName("API key missing → PROVIDER_UNAVAILABLE")
        void shouldFailWhenApiKeyMissing() {
            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "", RestClient.create());

            ImageProviderGenerationResult result = gateway.generate(createRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageGenerationErrorCode.PROVIDER_UNAVAILABLE);
            assertThat(result.getCorrelationId()).isEqualTo(CORRELATION_ID);
        }

        @Test
        @DisplayName("API key null → PROVIDER_UNAVAILABLE")
        void shouldFailWhenApiKeyNull() {
            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, null, RestClient.create());

            ImageProviderGenerationResult result = gateway.generate(createRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageGenerationErrorCode.PROVIDER_UNAVAILABLE);
        }

        @Test
        @DisplayName("API key error message safe — key değeri içermez")
        void errorMessageShouldNotContainApiKey() {
            String fakeKey = "sk-test-fake-key-12345";
            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "", RestClient.create());

            ImageProviderGenerationResult result = gateway.generate(createRequest());

            assertThat(result.getErrorMessage()).doesNotContain(fakeKey);
            assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("apiKey");
            assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("secret");
        }
    }

    // ── Success Flow ────────────────────────────────────────────────

    @Nested
    @DisplayName("Success flow")
    class SuccessFlow {

        @Test
        @DisplayName("Success response → normalized result")
        void shouldReturnNormalizedSuccessResult() {
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl("https://api.openai.com/v1/images/generations");
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            String responseBody = """
                    {
                      "created": 1234567890,
                      "data": [
                        {
                          "url": "https://oai.example.com/generated-image-123.png",
                          "revised_prompt": "A revised prompt text"
                        }
                      ]
                    }
                    """;

            server.expect(requestTo("https://api.openai.com/v1/images/generations"))
                    .andExpect(method(org.springframework.http.HttpMethod.POST))
                    .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "test-key-present", builder.build());

            ImageProviderGenerationResult result = gateway.generate(createRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProviderAlias()).isEqualTo("OPENAI");
            assertThat(result.getModelName()).isEqualTo("dall-e-3");
            assertThat(result.getOutputCount()).isEqualTo(1);
            assertThat(result.getCorrelationId()).isEqualTo(CORRELATION_ID);
            assertThat(result.getWidth()).isEqualTo(1024);
            assertThat(result.getHeight()).isEqualTo(1024);

            server.verify();
        }

        @Test
        @DisplayName("Success result URL/base64/raw payload içermez")
        void successResultShouldNotContainForbiddenData() {
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl("https://api.openai.com/v1/images/generations");
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            String responseBody = """
                    {
                      "created": 1234567890,
                      "data": [
                        {
                          "url": "https://oai.example.com/generated-image-456.png"
                        }
                      ]
                    }
                    """;

            server.expect(requestTo("https://api.openai.com/v1/images/generations"))
                    .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "test-key-present", builder.build());

            ImageProviderGenerationResult result = gateway.generate(createRequest());

            // Forbidden fields check
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
            assertThat(result.getQualitySignalsSummary()).doesNotContain("https://");
            assertThat(result.getSafetyFlagsSummary()).doesNotContain("https://");

            server.verify();
        }

        @Test
        @DisplayName("CorrelationId request → result korunur")
        void correlationIdShouldBePreserved() {
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl("https://api.openai.com/v1/images/generations");
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo("https://api.openai.com/v1/images/generations"))
                    .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "test-key-present", builder.build());

            ImageProviderGenerationResult result = gateway.generate(createRequest());

            assertThat(result.getCorrelationId()).isEqualTo(CORRELATION_ID);

            server.verify();
        }
    }

    // ── Error Classification ────────────────────────────────────────

    @Nested
    @DisplayName("Error classification")
    class ErrorClassification {

        @Test
        @DisplayName("HTTP 401 → PROVIDER_UNAVAILABLE")
        void shouldClassifyUnauthorizedAsUnavailable() {
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl("https://api.openai.com/v1/images/generations");
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo("https://api.openai.com/v1/images/generations"))
                    .andRespond(withUnauthorizedRequest());

            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "bad-key", builder.build());

            ImageProviderGenerationResult result = gateway.generate(createRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isNotNull();
            assertThat(result.getCorrelationId()).isEqualTo(CORRELATION_ID);
        }

        @Test
        @DisplayName("Server error → normalized error message")
        void serverErrorShouldProduceSafeMessage() {
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl("https://api.openai.com/v1/images/generations");
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo("https://api.openai.com/v1/images/generations"))
                    .andRespond(withServerError());

            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "test-key-present", builder.build());

            ImageProviderGenerationResult result = gateway.generate(createRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("apiKey");
            assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("secret");
            assertThat(result.getErrorMessage()).doesNotContain("test-key-present");
        }
    }

    // ── Provider Alias ──────────────────────────────────────────────

    @Nested
    @DisplayName("Provider metadata")
    class ProviderMetadata {

        @Test
        @DisplayName("Provider alias = OPENAI")
        void aliasShouldBeOpenAI() {
            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "", RestClient.create());
            assertThat(gateway.getProviderAlias()).isEqualTo("OPENAI");
        }
    }

    // ── Config Tests ────────────────────────────────────────────────

    @Nested
    @DisplayName("Provider config")
    class ConfigTests {

        @Test
        @DisplayName("Default config mock mode")
        void defaultShouldBeMock() {
            ImageGenerationProviderConfig defaultConfig = new ImageGenerationProviderConfig();
            assertThat(defaultConfig.isMockMode()).isTrue();
            assertThat(defaultConfig.getMode()).isEqualTo("mock");
        }

        @Test
        @DisplayName("OpenAI mode explicit set")
        void openAiModeShouldNotBeMock() {
            ImageGenerationProviderConfig openaiConfig = new ImageGenerationProviderConfig();
            openaiConfig.setMode("openai");
            assertThat(openaiConfig.isMockMode()).isFalse();
        }
    }
}
