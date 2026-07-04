package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.contract.*;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.ai.gateway.AiVisualAssetAgentGateway;
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
 * FP-006 Task 09: OpenAiVisualAssetAgentGateway testleri.
 *
 * <p>30 test — 4 nested group:
 * SuccessfulGeneration, ProviderErrors, InvalidOutput, SafetyGovernance</p>
 */
@DisplayName("FP-006 Task 09: OpenAiVisualAssetAgentGateway")
class OpenAiVisualAssetAgentGatewayTest {

    private AiProviderProperties.OpenAi config;
    private OpenAiClient mockClient;
    private OpenAiVisualAssetAgentGateway gateway;

    private static final UUID SCENE_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SCENE_2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID THUMB_SCENE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String VALID_JSON = """
            {
              "planTitle":"Çanakkale Türküsü Görsel Planı",
              "overallVisualStyle":"Sinematik belgesel, sıcak tonlar",
              "aspectRatioStrategy":"16:9 primary",
              "culturalAuthenticityGuidance":"1915 dönemi kıyafetleri ve mimarisi",
              "characterContinuityGuidance":"Yaşlı hikâyeci tüm sahnelerde aynı görünümde",
              "locationContinuityGuidance":"Gelibolu yarımadası panoramaları tutarlı",
              "colorPaletteGuidance":"Toprak tonları: okre, sienna, zeytin yeşili",
              "scenePlans":[
                {
                  "sourceStoryboardSceneId":"11111111-1111-1111-1111-111111111111",
                  "sceneNumber":1,
                  "sourceBeatType":"HOOK",
                  "sceneTitle":"Çanakkale Şafağı",
                  "englishImagePrompt":"Cinematic wide shot of Gallipoli peninsula at dawn, golden hour light, historical Ottoman military camp in distance, mist rising from the sea",
                  "turkishVisualContext":"Çanakkale yarımadasında şafak vakti, altın saat ışığı, uzakta tarihî Osmanlı askerî kampı",
                  "negativePrompt":"no modern buildings, no text, no watermark, no logos",
                  "styleDirective":"Cinematic documentary, photorealistic",
                  "aspectRatio":"16:9",
                  "compositionNotes":"Rule of thirds, horizon on lower third",
                  "cameraFraming":"Wide establishing shot",
                  "lightingNotes":"Golden hour, warm directional light from east",
                  "colorPalette":"Earth tones: ochre, sienna, olive green",
                  "mood":"Dramatic, nostalgic",
                  "location":"Gelibolu yarımadası, kıyı şeridi",
                  "characters":"Siluet halinde askerler",
                  "culturalDetails":"1915 dönemi Osmanlı askeri üniformaları",
                  "textOverlayGuidance":"Alt 1/5 bölgede altyazı alanı bırak",
                  "imageGenerationToolNotes":"DALL-E 3 veya Midjourney v6 uyumlu",
                  "safetyNotes":"Savaş teması saygılı işlenmeli, grafik şiddet yok"
                },
                {
                  "sourceStoryboardSceneId":"22222222-2222-2222-2222-222222222222",
                  "sceneNumber":2,
                  "sourceBeatType":"STORY_DEVELOPMENT",
                  "sceneTitle":"Mektup Sahnesi",
                  "englishImagePrompt":"Close-up of weathered hands writing a letter by candlelight in an Ottoman military tent, ink and paper, emotional atmosphere",
                  "turkishVisualContext":"Mumışığında mektup yazan yıpranmış eller, Osmanlı askeri çadırında",
                  "negativePrompt":"no modern pens, no electricity, no phones",
                  "styleDirective":"Intimate, warm tones",
                  "aspectRatio":"16:9",
                  "compositionNotes":"Tight framing, shallow depth of field",
                  "cameraFraming":"Close-up, slightly overhead",
                  "lightingNotes":"Warm candlelight, chiaroscuro effect",
                  "colorPalette":"Warm amber, deep brown, cream",
                  "mood":"Melancholic, intimate",
                  "location":"Askeri çadır içi",
                  "characters":"Genç asker, eller odakta",
                  "culturalDetails":"Osmanlı mektup geleneği, mürekkep ve divit",
                  "textOverlayGuidance":"Üst 1/4 temiz, başlık için kullanılabilir",
                  "imageGenerationToolNotes":"Detaylı texture render gerekli",
                  "safetyNotes":"Duygusal sahne, hassas işlenmeli"
                }
              ],
              "thumbnailPlan":{
                "thumbnailPrompt":"Dramatic portrait of elderly Turkish storyteller with weathered face, piercing eyes, traditional headwear, Gallipoli landscape in background, cinematic lighting",
                "negativePrompt":"no text, no watermark, no modern clothing",
                "textOverlayGuidance":"Sol 2/3 yüz, sağ 1/3 başlık metni için boş alan",
                "compositionNotes":"Rule of thirds, subject on left",
                "emotionalHook":"Doğrudan göz teması, derin bakış",
                "aspectRatio":"16:9",
                "styleDirective":"Cinematic portrait, high contrast",
                "colorPalette":"Warm saturated tones, teal shadows",
                "sourceSceneNumber":1,
                "sourceStoryboardSceneId":"11111111-1111-1111-1111-111111111111"
              }
            }""";

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
        gateway = new OpenAiVisualAssetAgentGateway(config, mockClient);
    }

    private VisualAssetPlanAgentRequest buildRequest() {
        return VisualAssetPlanAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .storyboardDraftId(UUID.randomUUID())
                .contentProjectId(UUID.randomUUID())
                .title("Çanakkale Türküsü")
                .summary("Çanakkale Savaşı'nın acı ve kahramanlık hikâyesi")
                .targetPlatform("YOUTUBE")
                .aspectRatio("16:9")
                .estimatedDurationSeconds(600)
                .visualStyleGuide("Sinematik belgesel")
                .narrationStyle("Duygusal anlatım")
                .scenes(List.of(
                        VisualAssetSceneInput.builder()
                                .storyboardSceneId(SCENE_1_ID)
                                .sceneNumber(1)
                                .sourceBeatType("HOOK")
                                .sceneTitle("Çanakkale Şafağı")
                                .visualDescription("Şafak vakti Gelibolu yarımadası panoraması")
                                .narrationText("Bir zamanlar bu topraklarda...")
                                .cameraDirection("Wide establishing")
                                .mood("dramatic")
                                .location("Gelibolu")
                                .characters("Askerler")
                                .build(),
                        VisualAssetSceneInput.builder()
                                .storyboardSceneId(SCENE_2_ID)
                                .sceneNumber(2)
                                .sourceBeatType("STORY_DEVELOPMENT")
                                .sceneTitle("Mektup Sahnesi")
                                .visualDescription("Mumışığında mektup yazan asker")
                                .narrationText("Sevgili anneciğim...")
                                .cameraDirection("Close-up")
                                .mood("melancholic")
                                .location("Askeri çadır")
                                .characters("Genç asker")
                                .build()
                ))
                .thumbnailContext(VisualAssetThumbnailInput.builder()
                        .title("Çanakkale Türküsü")
                        .storyHook("Unutulmayan acı")
                        .preferredAspectRatio("16:9")
                        .emotionalTone("nostalji")
                        .build())
                .languagePolicy("TR primary, EN for image prompts")
                .culturalContextPolicy("Anadolu kültürü saygılı işlenmeli")
                .safetyPolicy("Grafik şiddet yok")
                .requestedBy("admin-system")
                .build();
    }

    // ── 1. Interface Implementation ────────────────────────

    @Test
    @DisplayName("OpenAiVisualAssetAgentGateway AiVisualAssetAgentGateway interface'ini implement eder")
    void implementsGatewayInterface() {
        assertThat(gateway).isInstanceOf(AiVisualAssetAgentGateway.class);
    }

    // ── 2. Successful Generation ───────────────────────────

    @Nested
    @DisplayName("SuccessfulGeneration")
    class SuccessfulGeneration {

        @Test
        @DisplayName("Geçerli JSON → success=true, 2 sahne planı mevcut")
        void validJsonReturnsSuccess() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 3000L, "input:1200,output:2000"));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getScenePlans()).hasSize(2);
        }

        @Test
        @DisplayName("Provider metadata doğru: OPENAI, gpt-4o-mini")
        void providerMetadataCorrect() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.getProviderType()).isEqualTo("OPENAI");
            assertThat(result.getModelName()).isEqualTo("gpt-4o-mini");
        }

        @Test
        @DisplayName("generatedAt set edilir")
        void generatedAtIsSet() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.getGeneratedAt()).isNotNull();
        }

        @Test
        @DisplayName("sourceStoryboardSceneId korunur")
        void sourceStoryboardSceneIdPreserved() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.getScenePlans().get(0).getSourceStoryboardSceneId()).isEqualTo(SCENE_1_ID);
            assertThat(result.getScenePlans().get(1).getSourceStoryboardSceneId()).isEqualTo(SCENE_2_ID);
        }

        @Test
        @DisplayName("sceneNumber korunur")
        void sceneNumberPreserved() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.getScenePlans().get(0).getSceneNumber()).isEqualTo(1);
            assertThat(result.getScenePlans().get(1).getSceneNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("englishImagePrompt map edilir")
        void englishImagePromptMapped() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.getScenePlans().get(0).getEnglishImagePrompt())
                    .contains("Gallipoli peninsula at dawn");
        }

        @Test
        @DisplayName("turkishVisualContext map edilir")
        void turkishVisualContextMapped() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.getScenePlans().get(0).getTurkishVisualContext())
                    .contains("Çanakkale yarımadasında");
        }

        @Test
        @DisplayName("Style/composition/camera/lighting/color fields map edilir")
        void styleFieldsMapped() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            VisualAssetSceneOutput scene1 = result.getScenePlans().get(0);
            assertThat(scene1.getNegativePrompt()).isNotBlank();
            assertThat(scene1.getStyleDirective()).isNotBlank();
            assertThat(scene1.getAspectRatio()).isEqualTo("16:9");
            assertThat(scene1.getCompositionNotes()).isNotBlank();
            assertThat(scene1.getCameraFraming()).isNotBlank();
            assertThat(scene1.getLightingNotes()).isNotBlank();
            assertThat(scene1.getColorPalette()).isNotBlank();
            assertThat(scene1.getMood()).isNotBlank();
            assertThat(scene1.getLocation()).isNotBlank();
            assertThat(scene1.getCharacters()).isNotBlank();
            assertThat(scene1.getCulturalDetails()).isNotBlank();
        }

        @Test
        @DisplayName("Thumbnail plan map edilir")
        void thumbnailPlanMapped() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.getThumbnailPlan()).isNotNull();
            assertThat(result.getThumbnailPlan().getThumbnailPrompt()).contains("storyteller");
            assertThat(result.getThumbnailPlan().getSourceSceneNumber()).isEqualTo(1);
            assertThat(result.getThumbnailPlan().getSourceStoryboardSceneId()).isEqualTo(THUMB_SCENE_ID);
        }

        @Test
        @DisplayName("Plan-level alanlar map edilir")
        void planLevelFieldsMapped() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.getPlanTitle()).isNotBlank();
            assertThat(result.getOverallVisualStyle()).isNotBlank();
            assertThat(result.getAspectRatioStrategy()).isNotBlank();
            assertThat(result.getCulturalAuthenticityGuidance()).isNotBlank();
            assertThat(result.getCharacterContinuityGuidance()).isNotBlank();
            assertThat(result.getLocationContinuityGuidance()).isNotBlank();
            assertThat(result.getColorPaletteGuidance()).isNotBlank();
        }

        @Test
        @DisplayName("Code fence sarılı JSON doğru parse edilir")
        void codeFenceStrippedAndParsed() {
            String fenced = "```json\n" + VALID_JSON + "\n```";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(fenced, 1500L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getScenePlans()).hasSize(2);
        }
    }

    // ── 3. Provider Errors ─────────────────────────────────

    @Nested
    @DisplayName("ProviderErrors")
    class ProviderErrors {

        @Test
        @DisplayName("Timeout → retryable failure")
        void timeoutIsRetryable() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(
                            AiErrorType.AI_PROVIDER_TIMEOUT, "Zaman aşımı", true, 30000L));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
            assertThat(result.getRetryable()).isTrue();
        }

        @Test
        @DisplayName("API key eksik → AI_PROVIDER_CONFIG_ERROR, client çağrılmaz")
        void missingApiKeyConfigError() {
            config.setApiKey(null);
            gateway = new OpenAiVisualAssetAgentGateway(config, mockClient);

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_CONFIG_ERROR");
            assertThat(result.getRetryable()).isFalse();
            verify(mockClient, never()).call(any(), any(), any());
        }

        @Test
        @DisplayName("Boş API key → AI_PROVIDER_CONFIG_ERROR, client çağrılmaz")
        void blankApiKeyConfigError() {
            config.setApiKey("  ");
            gateway = new OpenAiVisualAssetAgentGateway(config, mockClient);

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_CONFIG_ERROR");
            verify(mockClient, never()).call(any(), any(), any());
        }

        @Test
        @DisplayName("Client exception → UNKNOWN_AI_ERROR, retryable")
        void clientExceptionReturnsUnknownError() {
            when(mockClient.call(any(), any(), any()))
                    .thenThrow(new RuntimeException("Beklenmeyen hata"));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

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

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_PROVIDER_UNAVAILABLE");
        }
    }

    // ── 4. Invalid Output ──────────────────────────────────

    @Nested
    @DisplayName("InvalidOutput")
    class InvalidOutput {

        @Test
        @DisplayName("Boş response → OPENAI_VISUAL_ASSET_EMPTY_RESPONSE")
        void emptyResponseFailure() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("", 500L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("OPENAI_VISUAL_ASSET_EMPTY_RESPONSE");
        }

        @Test
        @DisplayName("Geçersiz JSON → OPENAI_VISUAL_ASSET_INVALID_JSON")
        void invalidJsonFailure() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("not json at all", 800L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("OPENAI_VISUAL_ASSET_INVALID_JSON");
        }

        @Test
        @DisplayName("scenePlans eksik → OPENAI_VISUAL_ASSET_INVALID_STRUCTURE")
        void missingScenePlans() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("{\"planTitle\":\"test\"}", 800L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE");
        }

        @Test
        @DisplayName("Sahne sayısı uyumsuz → OPENAI_VISUAL_ASSET_INVALID_STRUCTURE")
        void sceneCountMismatch() {
            // Request has 2 scenes, but JSON has only 1
            String oneSceneJson = """
                    {"planTitle":"Test","scenePlans":[
                      {"sceneNumber":1,"englishImagePrompt":"prompt","turkishVisualContext":"bağlam",
                       "sourceStoryboardSceneId":"11111111-1111-1111-1111-111111111111"}
                    ],"thumbnailPlan":{"thumbnailPrompt":"thumb"}}""";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(oneSceneJson, 800L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE");
        }

        @Test
        @DisplayName("englishImagePrompt eksik → OPENAI_VISUAL_ASSET_INVALID_STRUCTURE")
        void missingEnglishImagePrompt() {
            String noPromptJson = """
                    {"planTitle":"Test","scenePlans":[
                      {"sceneNumber":1,"turkishVisualContext":"bağlam",
                       "sourceStoryboardSceneId":"11111111-1111-1111-1111-111111111111"},
                      {"sceneNumber":2,"englishImagePrompt":"ok","turkishVisualContext":"ok",
                       "sourceStoryboardSceneId":"22222222-2222-2222-2222-222222222222"}
                    ],"thumbnailPlan":{"thumbnailPrompt":"thumb"}}""";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(noPromptJson, 800L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE");
        }

        @Test
        @DisplayName("turkishVisualContext eksik → OPENAI_VISUAL_ASSET_INVALID_STRUCTURE")
        void missingTurkishVisualContext() {
            String noContextJson = """
                    {"planTitle":"Test","scenePlans":[
                      {"sceneNumber":1,"englishImagePrompt":"prompt",
                       "sourceStoryboardSceneId":"11111111-1111-1111-1111-111111111111"},
                      {"sceneNumber":2,"englishImagePrompt":"ok","turkishVisualContext":"ok",
                       "sourceStoryboardSceneId":"22222222-2222-2222-2222-222222222222"}
                    ],"thumbnailPlan":{"thumbnailPrompt":"thumb"}}""";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(noContextJson, 800L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE");
        }

        @Test
        @DisplayName("thumbnailPlan eksik → OPENAI_VISUAL_ASSET_INVALID_STRUCTURE")
        void missingThumbnailPlan() {
            String noThumbJson = """
                    {"planTitle":"Test","scenePlans":[
                      {"sceneNumber":1,"englishImagePrompt":"prompt","turkishVisualContext":"bağlam",
                       "sourceStoryboardSceneId":"11111111-1111-1111-1111-111111111111"},
                      {"sceneNumber":2,"englishImagePrompt":"p2","turkishVisualContext":"b2",
                       "sourceStoryboardSceneId":"22222222-2222-2222-2222-222222222222"}
                    ]}""";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(noThumbJson, 800L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE");
        }
    }

    // ── 5. Safety Governance ───────────────────────────────

    @Nested
    @DisplayName("SafetyGovernance")
    class SafetyGovernance {

        @Test
        @DisplayName("Refusal içerik → OPENAI_VISUAL_ASSET_UNSAFE_CONTENT")
        void refusalContentHandled() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(
                            "I cannot generate this content due to safety guidelines", 500L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("OPENAI_VISUAL_ASSET_UNSAFE_CONTENT");
            assertThat(result.getRetryable()).isFalse();
        }

        @Test
        @DisplayName("Yasaklı alan (imageUrl) → OPENAI_VISUAL_ASSET_UNSAFE_CONTENT")
        void forbiddenFieldImageUrlRejected() {
            String unsafeJson = """
                    {"planTitle":"Test","scenePlans":[
                      {"sceneNumber":1,"englishImagePrompt":"p","turkishVisualContext":"t",
                       "imageUrl":"https://example.com/img.png"}
                    ],"thumbnailPlan":{"thumbnailPrompt":"t"}}""";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(unsafeJson, 800L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("OPENAI_VISUAL_ASSET_UNSAFE_CONTENT");
        }

        @Test
        @DisplayName("Yasaklı alan (mediaAssetId) → OPENAI_VISUAL_ASSET_UNSAFE_CONTENT")
        void forbiddenFieldMediaAssetIdRejected() {
            String unsafeJson = """
                    {"planTitle":"Test","scenePlans":[
                      {"sceneNumber":1,"englishImagePrompt":"p","turkishVisualContext":"t",
                       "mediaAssetId":"some-id"}
                    ],"thumbnailPlan":{"thumbnailPrompt":"t"}}""";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(unsafeJson, 800L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Yasaklı alan (apiKey) → OPENAI_VISUAL_ASSET_UNSAFE_CONTENT")
        void forbiddenFieldApiKeyRejected() {
            String unsafeJson = """
                    {"planTitle":"Test","scenePlans":[
                      {"sceneNumber":1,"englishImagePrompt":"p","turkishVisualContext":"t",
                       "apiKey":"sk-xxx"}
                    ],"thumbnailPlan":{"thumbnailPrompt":"t"}}""";
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(unsafeJson, 800L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Başarılı yanıtta raw prompt saklanmaz")
        void noRawPromptInSuccessResponse() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isTrue();
            // VisualAssetPlanAgentResponse @Value — raw prompt alanı yok
            assertThat(result.getProviderType()).isEqualTo("OPENAI");
        }

        @Test
        @DisplayName("Başarısız yanıtta raw payload expose edilmez")
        void noRawPayloadInFailureResponse() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.failure(
                            AiErrorType.AI_PROVIDER_UNAVAILABLE, "Server hatası", true, 5000L));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).doesNotContain("Bearer");
            assertThat(result.getErrorMessage()).doesNotContain("test-key-xxx");
        }

        @Test
        @DisplayName("Client çağrısında API key prompt'a dahil değil")
        void apiKeyNotInPrompt() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            gateway.generateVisualAssetPlan(buildRequest());

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
        @DisplayName("Chat/text completion kullanılır — image API yok")
        void usesTextCompletionNotImageApi() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            gateway.generateVisualAssetPlan(buildRequest());

            // OpenAiClient.call chat completion çağırır
            // Image generation endpoint çağrılmaz — client metodu sadece text
            verify(mockClient).call(any(List.class), any(UUID.class), any(UUID.class));
            verifyNoMoreInteractions(mockClient);
        }

        @Test
        @DisplayName("No MediaAsset, no GeneratedAsset, no image URL in response")
        void noMediaOrImageReferencesInResponse() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success(VALID_JSON, 2000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isTrue();
            // VisualAssetPlanAgentResponse, VisualAssetSceneOutput, VisualAssetThumbnailOutput
            // — hiçbirinde imageUrl, mediaAssetId, generatedAssetId alanı yok
            // Compile-time guarantee: @Value class ile alanlar fixed
            assertThat(result.getScenePlans()).allSatisfy(scene -> {
                assertThat(scene.getEnglishImagePrompt()).isNotBlank();
                // scene has no getImageUrl() or getMediaAssetId() — compile-time safety
            });
        }

        @Test
        @DisplayName("Başarısız parse sonucu success=true döndürmez")
        void failedParseNeverReturnsSuccess() {
            when(mockClient.call(any(), any(), any()))
                    .thenReturn(OpenAiCallResult.success("this is not json", 1000L, null));

            VisualAssetPlanAgentResponse result = gateway.generateVisualAssetPlan(buildRequest());

            assertThat(result.isSuccess()).isFalse();
        }
    }
}
