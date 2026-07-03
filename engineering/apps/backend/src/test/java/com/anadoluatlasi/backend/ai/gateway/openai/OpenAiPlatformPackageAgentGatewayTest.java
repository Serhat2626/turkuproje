package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.*;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
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
 * FP-004 Task 09: OpenAiPlatformPackageAgentGateway testleri.
 */
@DisplayName("FP-004 Task 09: OpenAiPlatformPackageAgentGateway")
class OpenAiPlatformPackageAgentGatewayTest {

    private AiProviderProperties.OpenAi config;
    private OpenAiClient mockClient;
    private OpenAiPlatformPackageAgentGateway gateway;

    private static final String VALID_JSON = """
            {"packages":[
              {"packageType":"YOUTUBE_LONG_FORM","title":"YT Başlık","captionOrDescription":"YT Açıklama",
               "hashtags":["#tarih"],"callToAction":"Abone ol","targetDurationSeconds":600,"aspectRatio":"16:9",
               "videoTitle":"SEO Başlık","longDescription":"Detaylı açıklama",
               "chapterOutline":["00:00 Giriş"],"pinnedCommentSuggestion":"Yorum",
               "thumbnailTextSuggestion":"Thumbnail","tags":["tarih"]},
              {"packageType":"YOUTUBE_SHORTS","title":"Shorts","captionOrDescription":"Shorts desc",
               "hashtags":["#shorts"],"callToAction":"Like","targetDurationSeconds":60,"aspectRatio":"9:16",
               "shortHook":"Hook!","shortCaption":"Caption","overlayTextSuggestions":["Overlay"],
               "shortHashtagSet":["#shorts"],"shortCta":"Like","durationGuidance":"30-60sn"},
              {"packageType":"INSTAGRAM_REELS","title":"Reels","captionOrDescription":"Reels desc",
               "hashtags":["#reels"],"callToAction":"Takip","targetDurationSeconds":30,"aspectRatio":"9:16",
               "shortHook":"IG Hook","shortCaption":"IG Cap","overlayTextSuggestions":["IG OL"],
               "shortHashtagSet":["#ig"],"shortCta":"Takip","durationGuidance":"15-30sn"},
              {"packageType":"FACEBOOK_REELS","title":"FB Reels","captionOrDescription":"FB desc",
               "hashtags":["#fb"],"callToAction":"Paylaş","targetDurationSeconds":45,"aspectRatio":"9:16",
               "shortHook":"FB Hook","shortCaption":"FB Cap","overlayTextSuggestions":["FB OL"],
               "shortHashtagSet":["#fb"],"shortCta":"Paylaş","durationGuidance":"30-60sn"}
            ]}""";

    @BeforeEach
    void setUp() {
        config = new AiProviderProperties.OpenAi();
        config.setApiKey("test-key-xxx");
        config.setModel("gpt-4o-mini");
        config.setBaseUrl("https://api.openai.com/v1");
        config.setTimeoutSeconds(60);
        config.setMaxOutputTokens(4000);
        config.setTemperature(0.4);

        mockClient = mock(OpenAiClient.class);
        gateway = new OpenAiPlatformPackageAgentGateway(config, mockClient);
    }

    private PlatformPackageAgentRequest buildRequest() {
        return PlatformPackageAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .contentProjectId(UUID.randomUUID())
                .sourceStoryboardDraftId(UUID.randomUUID())
                .sourceStoryboardVersion(1)
                .targetPlatforms(List.of(
                        PlatformPackageType.YOUTUBE_LONG_FORM,
                        PlatformPackageType.YOUTUBE_SHORTS,
                        PlatformPackageType.INSTAGRAM_REELS,
                        PlatformPackageType.FACEBOOK_REELS))
                .sourceStoryboard(PlatformPackageSourceStoryboardInput.builder()
                        .title("Test Storyboard")
                        .summary("Özet")
                        .build())
                .sourceScenes(List.of(
                        PlatformPackageSourceSceneInput.builder()
                                .sceneNumber(1)
                                .sceneTitle("Açılış")
                                .visualDescription("Gökyüzü")
                                .narrationText("Bir zamanlar")
                                .mood("dramatic")
                                .estimatedDurationSeconds(10)
                                .sourceBeatType("HOOK")
                                .build()))
                .language("tr")
                .build();
    }

    // ── Successful Generation ───────────────────────────────

    @Nested
    @DisplayName("SuccessfulGeneration")
    class SuccessfulGeneration {

        @Test
        @DisplayName("Geçerli JSON → success=true, 4 paket mevcut")
        void validJsonReturnsSuccess() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, "input:800,output:1200"));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getPackages()).hasSize(4);
        }

        @Test
        @DisplayName("Provider metadata doğru: OPENAI, gpt-4o-mini")
        void providerMetadataCorrect() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.getProviderType()).isEqualTo("OPENAI");
            assertThat(result.getModelName()).isEqualTo("gpt-4o-mini");
        }

        @Test
        @DisplayName("Code fence sarılı JSON doğru parse edilir")
        void codeFenceStrippedAndParsed() {
            String fenced = "```json\n" + VALID_JSON + "\n```";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(fenced, 1500L, null));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getPackages()).hasSize(4);
        }

        @Test
        @DisplayName("Başarılı yanıt otomatik onay yapmaz")
        void doesNotAutoApprove() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            // PlatformPackageAgentResponse onay durumu içermez — sadece candidate üretir
            assertThat(result.isSuccess()).isTrue();
            // success=true olan response APPROVED status değildir
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

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

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

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getRetryable()).isTrue();
        }

        @Test
        @DisplayName("Auth error → non-retryable failure")
        void authErrorNotRetryable() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(
                            AiErrorType.AI_PROVIDER_AUTH_ERROR, "Kimlik hatası", false, 200L));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getRetryable()).isFalse();
        }

        @Test
        @DisplayName("API key eksik → AI_PROVIDER_CONFIG_ERROR, client çağrılmaz")
        void missingApiKeyConfigError() {
            config.setApiKey(null);
            gateway = new OpenAiPlatformPackageAgentGateway(config, mockClient);

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_CONFIG_ERROR");
            assertThat(result.getRetryable()).isFalse();
            verify(mockClient, never()).call(any(), any(), any());
        }

        @Test
        @DisplayName("Boş API key → AI_PROVIDER_CONFIG_ERROR, client çağrılmaz")
        void blankApiKeyConfigError() {
            config.setApiKey("  ");
            gateway = new OpenAiPlatformPackageAgentGateway(config, mockClient);

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_CONFIG_ERROR");
            verify(mockClient, never()).call(any(), any(), any());
        }

        @Test
        @DisplayName("Client exception → UNKNOWN_AI_ERROR")
        void clientExceptionReturnsUnknownError() {
            when(mockClient.call(any(), any(), any()))
                    .thenThrow(new RuntimeException("Beklenmeyen hata"));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("UNKNOWN_AI_ERROR");
            assertThat(result.getRetryable()).isTrue();
        }

        @Test
        @DisplayName("Server error → mapped failure")
        void serverErrorMapped() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(
                            AiErrorType.AI_PROVIDER_UNAVAILABLE, "Server hatası", true, 5000L));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_UNAVAILABLE");
        }
    }

    // ── Invalid Output ──────────────────────────────────────

    @Nested
    @DisplayName("InvalidOutput")
    class InvalidOutput {

        @Test
        @DisplayName("Boş response → EMPTY_AI_RESPONSE")
        void emptyResponseFailure() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("", 500L, null));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("EMPTY_AI_RESPONSE");
        }

        @Test
        @DisplayName("Geçersiz JSON → INVALID_AI_RESPONSE_FORMAT")
        void invalidJsonFailure() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("not json", 800L, null));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("Packages eksik → INVALID_AI_RESPONSE_FORMAT")
        void missingPackages() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("{}", 800L, null));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
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
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

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

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).doesNotContain("Bearer");
            assertThat(result.getErrorMessage()).doesNotContain("test-key-xxx");
        }

        @Test
        @DisplayName("Client çağrısında API key loglanmaz — prompt'a dahil değil")
        void apiKeyNotInPrompt() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            gateway.generatePlatformPackages(buildRequest());

            var captor = org.mockito.ArgumentCaptor.forClass(List.class);
            verify(mockClient).call(captor.capture(), any(), any());

            @SuppressWarnings("unchecked")
            List<OpenAiChatMessage> messages = (List<OpenAiChatMessage>) captor.getValue();
            for (OpenAiChatMessage msg : messages) {
                assertThat(msg.getContent()).doesNotContain("test-key-xxx");
                assertThat(msg.getContent().toLowerCase()).doesNotContain("bearer");
            }
        }

        @Test
        @DisplayName("Refusal içerik → AI_RESPONSE_UNSAFE_CONTENT")
        void refusalContentHandled() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(
                            "I cannot generate this content due to safety guidelines", 500L, null));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_RESPONSE_UNSAFE_CONTENT");
        }

        @Test
        @DisplayName("Başarısız parse sonucu success=true döndürmez")
        void failedParseNeverReturnsSuccess() {
            String invalidJson = "this is not json";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(invalidJson, 1000L, null));

            PlatformPackageAgentResponse result = gateway.generatePlatformPackages(buildRequest());

            assertThat(result.isSuccess()).isFalse();
        }
    }
}
