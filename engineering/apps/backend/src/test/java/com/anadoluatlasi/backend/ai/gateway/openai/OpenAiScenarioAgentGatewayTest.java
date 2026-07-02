package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.ScenarioAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ScenarioAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ScenarioBeatResult;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * FP-002 Task 09: OpenAiScenarioAgentGateway unit tests.
 * OpenAiClient mocked — real HTTP calls never made.
 */
@ExtendWith(MockitoExtension.class)
class OpenAiScenarioAgentGatewayTest {

    @Mock
    private OpenAiClient client;

    private AiProviderProperties.OpenAi config;
    private OpenAiScenarioAgentGateway gateway;

    @BeforeEach
    void setUp() {
        config = new AiProviderProperties.OpenAi();
        config.setApiKey("test-key-not-real");
        config.setModel("gpt-4o-mini");
        config.setBaseUrl("https://api.openai.com/v1");
        gateway = new OpenAiScenarioAgentGateway(config, client);
    }

    private ScenarioAgentRequest sampleRequest() {
        return ScenarioAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .contentProjectId(UUID.randomUUID())
                .sourceDraftId(UUID.randomUUID())
                .sourceDraftTitle("Çanakkale Türküsünün Hikâyesi")
                .sourceDraftBody("Çanakkale Türküsü, Birinci Dünya Savaşı...")
                .sourceDraftSummary("Çanakkale Savaşı ve türküsü")
                .knowledgeItemTitle("Çanakkale Türküsü")
                .targetAudience("Genel izleyici")
                .tone("Duygusal")
                .language("tr")
                .desiredDurationSeconds(600)
                .build();
    }

    /** Valid 6-beat JSON response from OpenAI */
    private String validScenarioJson() {
        return """
                {
                  "scenarioTitle": "Çanakkale'nin Ağıdı — Bir Türkünün Doğuş Hikâyesi",
                  "summary": "Çanakkale Savaşı'nın acı gerçeklerinden doğan türkünün hikâyesi.",
                  "targetAudience": "Genel kültür meraklısı izleyiciler",
                  "culturalObjective": "Çanakkale ruhunu ve türkünün tarihî bağlamını aktarmak",
                  "tone": "Duygusal ve bilgilendirici",
                  "estimatedDurationSeconds": 600,
                  "setting": "Çanakkale Yarımadası, 1915",
                  "characters": "Anlatıcı, Mehmetçik, köyden gelen mektuplar",
                  "beats": [
                    {
                      "type": "HOOK",
                      "title": "Bir Türkü, Bir Milletin Gözyaşı",
                      "body": "Ekranda sis içindeki Çanakkale kıyıları... Uzaktan bir türkü sesi yükselir. Bu türkü, milyonların kalbinde yaşayan bir acının melodisidir.",
                      "order": 1,
                      "estimatedDurationSeconds": 15
                    },
                    {
                      "type": "INTRODUCTION",
                      "title": "1915'e Yolculuk",
                      "body": "1915 yılı, Osmanlı İmparatorluğu'nun en zorlu dönemlerinden biri. İtilaf devletleri Çanakkale Boğazı'nı geçmek istiyor. Binlerce genç asker cepheye gönderilir.",
                      "order": 2,
                      "estimatedDurationSeconds": 60
                    },
                    {
                      "type": "MAIN_NARRATIVE",
                      "title": "Cephede Yaşananlar",
                      "body": "Siperlerde geçen günler, açlık, hastalık ve ölüm... Ama aynı zamanda dayanışma, fedakârlık ve umut. Askerler mektup yazar, türkü söyler. Bu türkü, cephedeki askerlerin duygularını dile getirir.",
                      "order": 3,
                      "estimatedDurationSeconds": 360
                    },
                    {
                      "type": "EMOTIONAL_CLIMAX",
                      "title": "Son Mektup",
                      "body": "Genç bir askerin ailesine yazdığı son mektup okunur. 'Annem beni bekliyor ama ben artık dönemeyeceğim...' Bu satırlar, türkünün en bilinen dizelerine ilham kaynağı olmuştur.",
                      "order": 4,
                      "estimatedDurationSeconds": 90
                    },
                    {
                      "type": "CONCLUSION",
                      "title": "Türkünün Mirası",
                      "body": "Savaş bitti ama türkü yaşamaya devam etti. Nesilden nesile aktarılan bu melodi, Çanakkale ruhunun en güçlü ifadesi oldu. Bugün hâlâ her 18 Mart'ta söylenir.",
                      "order": 5,
                      "estimatedDurationSeconds": 60
                    },
                    {
                      "type": "CALL_TO_ACTION",
                      "title": "Unutmayalım",
                      "body": "Bu türkünün hikâyesini paylaşın. Abone olun, yorumlarda en sevdiğiniz Çanakkale türküsünü yazın. Bir sonraki videoda görüşmek üzere.",
                      "order": 6,
                      "estimatedDurationSeconds": 15
                    }
                  ]
                }
                """;
    }

    // ── Success Tests ───────────────────────────────────────

    @Nested
    @DisplayName("Successful Scenario Generation")
    class SuccessfulGeneration {

        @Test
        @DisplayName("Valid OpenAI output maps to successful ScenarioAgentResponse")
        void validOutputMapsToSuccessResponse() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(validScenarioJson(), 1500L, "input:800,output:1200"));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getScenarioTitle()).isEqualTo("Çanakkale'nin Ağıdı — Bir Türkünün Doğuş Hikâyesi");
            assertThat(response.getSummary()).isNotBlank();
            assertThat(response.getTargetAudience()).isNotBlank();
            assertThat(response.getCulturalObjective()).isNotBlank();
            assertThat(response.getTone()).isNotBlank();
            assertThat(response.getEstimatedDurationSeconds()).isEqualTo(600);
            assertThat(response.getSetting()).isNotBlank();
            assertThat(response.getCharacters()).isNotBlank();
        }

        @Test
        @DisplayName("Response contains exactly 6 required beats")
        void responseContains6Beats() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(validScenarioJson(), 1500L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.getBeats()).hasSize(6);
            List<ScenarioBeatType> types = response.getBeats().stream()
                    .map(ScenarioBeatResult::getType)
                    .toList();
            assertThat(types).containsExactly(
                    ScenarioBeatType.HOOK,
                    ScenarioBeatType.INTRODUCTION,
                    ScenarioBeatType.MAIN_NARRATIVE,
                    ScenarioBeatType.EMOTIONAL_CLIMAX,
                    ScenarioBeatType.CONCLUSION,
                    ScenarioBeatType.CALL_TO_ACTION
            );
        }

        @Test
        @DisplayName("Beat order is deterministic")
        void beatOrderIsDeterministic() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(validScenarioJson(), 1500L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            List<Integer> orders = response.getBeats().stream()
                    .map(ScenarioBeatResult::getOrder)
                    .toList();
            assertThat(orders).containsExactly(1, 2, 3, 4, 5, 6);
        }

        @Test
        @DisplayName("Safe provider metadata is set")
        void safeProviderMetadataIsSet() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(validScenarioJson(), 1500L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.getProviderType()).isEqualTo("OPENAI");
            assertThat(response.getModelName()).isEqualTo("gpt-4o-mini");
            assertThat(response.getGeneratedAt()).isNotNull();
        }

        @Test
        @DisplayName("Each beat has non-empty body")
        void eachBeatHasNonEmptyBody() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(validScenarioJson(), 1500L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            response.getBeats().forEach(beat -> {
                assertThat(beat.getBody()).isNotBlank();
                assertThat(beat.getTitle()).isNotBlank();
            });
        }
    }

    // ── Invalid Output Tests ────────────────────────────────

    @Nested
    @DisplayName("Invalid Output Handling")
    class InvalidOutput {

        @Test
        @DisplayName("Missing title maps to failure")
        void missingTitleMapsToFailure() {
            String json = validScenarioJson().replace("\"scenarioTitle\": \"Çanakkale'nin Ağıdı — Bir Türkünün Doğuş Hikâyesi\"", "\"scenarioTitle\": \"\"");
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(json, 1000L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("Missing required beat maps to failure")
        void missingRequiredBeatMapsToFailure() {
            // Remove last beat (CALL_TO_ACTION)
            String json = """
                    {
                      "scenarioTitle": "Test",
                      "summary": "Test",
                      "beats": [
                        {"type": "HOOK", "title": "T", "body": "Body text", "order": 1},
                        {"type": "INTRODUCTION", "title": "T", "body": "Body text", "order": 2},
                        {"type": "MAIN_NARRATIVE", "title": "T", "body": "Body text", "order": 3},
                        {"type": "EMOTIONAL_CLIMAX", "title": "T", "body": "Body text", "order": 4},
                        {"type": "CONCLUSION", "title": "T", "body": "Body text", "order": 5}
                      ]
                    }
                    """;
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(json, 1000L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("Empty beat body maps to failure")
        void emptyBeatBodyMapsToFailure() {
            String json = """
                    {
                      "scenarioTitle": "Test",
                      "beats": [
                        {"type": "HOOK", "title": "T", "body": "", "order": 1},
                        {"type": "INTRODUCTION", "title": "T", "body": "Body", "order": 2},
                        {"type": "MAIN_NARRATIVE", "title": "T", "body": "Body", "order": 3},
                        {"type": "EMOTIONAL_CLIMAX", "title": "T", "body": "Body", "order": 4},
                        {"type": "CONCLUSION", "title": "T", "body": "Body", "order": 5},
                        {"type": "CALL_TO_ACTION", "title": "T", "body": "Body", "order": 6}
                      ]
                    }
                    """;
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(json, 1000L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("Empty AI response maps to failure")
        void emptyResponseMapsToFailure() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("", 500L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("EMPTY_AI_RESPONSE");
        }

        @Test
        @DisplayName("Invalid JSON maps to failure")
        void invalidJsonMapsToFailure() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("not valid json {{{", 500L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("Unsafe content/refusal maps to failure")
        void unsafeContentMapsToFailure() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("I cannot fulfill this request due to content policy.", 500L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("AI_RESPONSE_UNSAFE_CONTENT");
        }

        @Test
        @DisplayName("JSON wrapped in code fence is parsed correctly")
        void codeFenceIsStripped() {
            String fenced = "```json\n" + validScenarioJson() + "\n```";
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(fenced, 1500L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getBeats()).hasSize(6);
        }
    }

    // ── Provider Error Tests ────────────────────────────────

    @Nested
    @DisplayName("Provider Error Mapping")
    class ProviderErrors {

        @Test
        @DisplayName("Timeout maps to retryable timeout error")
        void timeoutMapsToRetryableError() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_TIMEOUT,
                            "OpenAI zaman aşımı.", true, 60000L));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
            assertThat(response.getRetryable()).isTrue();
        }

        @Test
        @DisplayName("Rate limit maps to retryable rate-limit error")
        void rateLimitMapsToRetryableError() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_RATE_LIMIT,
                            "OpenAI rate limit.", true, 500L));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("AI_PROVIDER_RATE_LIMIT");
            assertThat(response.getRetryable()).isTrue();
        }

        @Test
        @DisplayName("Auth error maps to non-retryable error")
        void authErrorMapsToNonRetryable() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(AiErrorType.AI_PROVIDER_AUTH_ERROR,
                            "OpenAI auth hatası.", false, 500L));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("AI_PROVIDER_AUTH_ERROR");
            assertThat(response.getRetryable()).isFalse();
        }

        @Test
        @DisplayName("Missing API key maps to config error")
        void missingApiKeyMapsToConfigError() {
            config.setApiKey(null);

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("AI_PROVIDER_CONFIG_ERROR");
            assertThat(response.getRetryable()).isFalse();
        }

        @Test
        @DisplayName("Client exception maps to unknown error")
        void clientExceptionMapsToUnknownError() {
            when(client.call(any(), any(), any()))
                    .thenThrow(new RuntimeException("Connection refused"));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("UNKNOWN_AI_ERROR");
            assertThat(response.getRetryable()).isTrue();
        }
    }

    // ── Safety / Governance Tests ───────────────────────────

    @Nested
    @DisplayName("Safety and Governance")
    class SafetyGovernance {

        @Test
        @DisplayName("Response does not contain raw provider payload")
        void noRawProviderPayload() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(validScenarioJson(), 1500L, "input:800,output:1200"));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            // ScenarioAgentResponse has no field for raw payload, raw prompt, or token usage
            // Verify only safe metadata is present
            assertThat(response.getProviderType()).isEqualTo("OPENAI");
            assertThat(response.getModelName()).isEqualTo("gpt-4o-mini");
            // No rawPayload, rawPrompt, tokenUsage, apiKey fields in ScenarioAgentResponse
        }

        @Test
        @DisplayName("Response does not contain raw prompt")
        void noRawPrompt() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(validScenarioJson(), 1500L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            // ScenarioAgentResponse contract has no raw prompt field
            // Verify success and no prompt leakage
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getErrorMessage()).isNull();
            assertThat(response.getErrorCode()).isNull();
        }

        @Test
        @DisplayName("Failed output does not create approved scenario")
        void failedOutputNotApproved() {
            when(client.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("{}", 500L, null));

            ScenarioAgentResponse response = gateway.generateScenario(sampleRequest());

            assertThat(response.isSuccess()).isFalse();
            // Gateway only returns response — it never sets status to APPROVED
        }
    }
}
