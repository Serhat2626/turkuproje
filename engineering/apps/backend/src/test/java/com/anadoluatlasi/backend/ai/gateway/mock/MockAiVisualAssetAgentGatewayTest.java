package com.anadoluatlasi.backend.ai.gateway.mock;

import com.anadoluatlasi.backend.ai.contract.*;
import com.anadoluatlasi.backend.ai.gateway.AiVisualAssetAgentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-006: Mock Visual Asset gateway ve AI contract DTO unit testleri.
 *
 * <p>Spring context yüklenmez — saf unit testler.</p>
 */
class MockAiVisualAssetAgentGatewayTest {

    private MockAiVisualAssetAgentGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new MockAiVisualAssetAgentGateway();
    }

    // ─── DTO Builder Testleri ─────────────────────────────────────────

    @Nested
    @DisplayName("Contract DTO builder testleri")
    class DtoBuilderTests {

        @Test
        @DisplayName("Request builder sahnelerle birlikte çalışır")
        void requestBuilderCreatesRequestWithScenes() {
            VisualAssetPlanAgentRequest request = createSampleRequest(2);

            assertThat(request).isNotNull();
            assertThat(request.getStoryboardDraftId()).isNotNull();
            assertThat(request.getContentProjectId()).isNotNull();
            assertThat(request.getTitle()).isEqualTo("Çanakkale Türküsü");
            assertThat(request.getScenes()).hasSize(2);
        }

        @Test
        @DisplayName("Scene input builder sahne alanlarını korur")
        void sceneInputPreservesFields() {
            UUID sceneId = UUID.randomUUID();
            VisualAssetSceneInput scene = VisualAssetSceneInput.builder()
                    .storyboardSceneId(sceneId)
                    .sceneNumber(1)
                    .sourceBeatType("HOOK")
                    .sourceBeatOrder(1)
                    .sceneTitle("Açılış")
                    .visualDescription("Anadolu köyünde gün doğumu")
                    .narrationText("Bir zamanlar...")
                    .mood("nostalgic")
                    .location("Anadolu köyü")
                    .characters("Yaşlı kadın")
                    .cameraDirection("wide shot")
                    .estimatedDurationSeconds(15)
                    .build();

            assertThat(scene.getStoryboardSceneId()).isEqualTo(sceneId);
            assertThat(scene.getSceneNumber()).isEqualTo(1);
            assertThat(scene.getSourceBeatType()).isEqualTo("HOOK");
            assertThat(scene.getVisualDescription()).isEqualTo("Anadolu köyünde gün doğumu");
            assertThat(scene.getMood()).isEqualTo("nostalgic");
            assertThat(scene.getLocation()).isEqualTo("Anadolu köyü");
        }

        @Test
        @DisplayName("Thumbnail input builder thumbnail context oluşturur")
        void thumbnailInputBuilderWorks() {
            VisualAssetThumbnailInput thumbnail = VisualAssetThumbnailInput.builder()
                    .title("Test Video")
                    .storyHook("Dramatik açılış")
                    .preferredAspectRatio("16:9")
                    .emotionalTone("merak")
                    .textOverlayPolicy("bold title")
                    .build();

            assertThat(thumbnail.getTitle()).isEqualTo("Test Video");
            assertThat(thumbnail.getEmotionalTone()).isEqualTo("merak");
        }

        @Test
        @DisplayName("Response success builder çalışır")
        void responseSuccessBuilderWorks() {
            VisualAssetPlanAgentResponse response = VisualAssetPlanAgentResponse.builder()
                    .success(true)
                    .planTitle("Test Plan")
                    .providerType("MOCK")
                    .modelName("mock-visual-asset-planner")
                    .build();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getPlanTitle()).isEqualTo("Test Plan");
            assertThat(response.getProviderType()).isEqualTo("MOCK");
        }

        @Test
        @DisplayName("Response error builder çalışır")
        void responseErrorBuilderWorks() {
            VisualAssetPlanAgentResponse response = VisualAssetPlanAgentResponse.builder()
                    .success(false)
                    .errorCode("AI_PROVIDER_TIMEOUT")
                    .errorMessage("Provider timeout")
                    .retryable(true)
                    .providerType("MOCK")
                    .build();

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
            assertThat(response.getRetryable()).isTrue();
        }
    }

    // ─── Mock Gateway Success ─────────────────────────────────────────

    @Nested
    @DisplayName("Mock gateway SUCCESS modu")
    class MockSuccessTests {

        @Test
        @DisplayName("Geçerli istek için success response döner")
        void shouldReturnSuccessForValidRequest() {
            VisualAssetPlanAgentRequest request = createSampleRequest(3);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getErrorCode()).isNull();
            assertThat(response.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Her input sahnesi için bir output scene plan üretir")
        void shouldProduceOneOutputPerInputScene() {
            VisualAssetPlanAgentRequest request = createSampleRequest(4);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getScenePlans()).hasSize(4);
        }

        @Test
        @DisplayName("sourceStoryboardSceneId korunur")
        void shouldPreserveSourceStoryboardSceneId() {
            VisualAssetPlanAgentRequest request = createSampleRequest(2);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            for (int i = 0; i < request.getScenes().size(); i++) {
                VisualAssetSceneInput input = request.getScenes().get(i);
                VisualAssetSceneOutput output = response.getScenePlans().get(i);
                assertThat(output.getSourceStoryboardSceneId())
                        .isEqualTo(input.getStoryboardSceneId());
            }
        }

        @Test
        @DisplayName("sceneNumber sıralaması korunur")
        void shouldPreserveSceneNumberOrdering() {
            VisualAssetPlanAgentRequest request = createSampleRequest(3);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            for (int i = 0; i < response.getScenePlans().size(); i++) {
                assertThat(response.getScenePlans().get(i).getSceneNumber())
                        .isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("Her sahne için boş olmayan englishImagePrompt üretir")
        void shouldProduceNonEmptyEnglishImagePrompt() {
            VisualAssetPlanAgentRequest request = createSampleRequest(2);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            for (VisualAssetSceneOutput scene : response.getScenePlans()) {
                assertThat(scene.getEnglishImagePrompt()).isNotBlank();
            }
        }

        @Test
        @DisplayName("Her sahne için boş olmayan turkishVisualContext üretir")
        void shouldProduceNonEmptyTurkishVisualContext() {
            VisualAssetPlanAgentRequest request = createSampleRequest(2);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            for (VisualAssetSceneOutput scene : response.getScenePlans()) {
                assertThat(scene.getTurkishVisualContext()).isNotBlank();
            }
        }

        @Test
        @DisplayName("Her sahne için negativePrompt üretir")
        void shouldProduceNegativePrompt() {
            VisualAssetPlanAgentRequest request = createSampleRequest(2);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            for (VisualAssetSceneOutput scene : response.getScenePlans()) {
                assertThat(scene.getNegativePrompt()).isNotBlank();
            }
        }

        @Test
        @DisplayName("Thumbnail plan üretir")
        void shouldProduceThumbnailPlan() {
            VisualAssetPlanAgentRequest request = createSampleRequest(2);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.getThumbnailPlan()).isNotNull();
            assertThat(response.getThumbnailPlan().getThumbnailPrompt()).isNotBlank();
            assertThat(response.getThumbnailPlan().getNegativePrompt()).isNotBlank();
        }

        @Test
        @DisplayName("providerType = MOCK")
        void shouldUseMockProviderType() {
            VisualAssetPlanAgentRequest request = createSampleRequest(1);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.getProviderType()).isEqualTo("MOCK");
        }

        @Test
        @DisplayName("Safe modelName kullanır")
        void shouldUseSafeModelName() {
            VisualAssetPlanAgentRequest request = createSampleRequest(1);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.getModelName()).isEqualTo("mock-visual-asset-planner");
        }

        @Test
        @DisplayName("generatedAt dolu")
        void shouldIncludeGeneratedAt() {
            VisualAssetPlanAgentRequest request = createSampleRequest(1);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.getGeneratedAt()).isNotNull();
        }

        @Test
        @DisplayName("Plan-level fields üretilir")
        void shouldProducePlanLevelFields() {
            VisualAssetPlanAgentRequest request = createSampleRequest(2);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.getPlanTitle()).isNotBlank();
            assertThat(response.getOverallVisualStyle()).isNotBlank();
            assertThat(response.getCulturalAuthenticityGuidance()).isNotBlank();
            assertThat(response.getCharacterContinuityGuidance()).isNotBlank();
            assertThat(response.getLocationContinuityGuidance()).isNotBlank();
            assertThat(response.getColorPaletteGuidance()).isNotBlank();
        }

        @Test
        @DisplayName("Scene output tüm stil/kompozisyon alanlarını içerir")
        void shouldProduceStyleAndCompositionFields() {
            VisualAssetPlanAgentRequest request = createSampleRequest(1);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            VisualAssetSceneOutput scene = response.getScenePlans().get(0);
            assertThat(scene.getStyleDirective()).isNotBlank();
            assertThat(scene.getAspectRatio()).isNotBlank();
            assertThat(scene.getCompositionNotes()).isNotBlank();
            assertThat(scene.getCameraFraming()).isNotBlank();
            assertThat(scene.getLightingNotes()).isNotBlank();
            assertThat(scene.getColorPalette()).isNotBlank();
            assertThat(scene.getCulturalDetails()).isNotBlank();
            assertThat(scene.getImageGenerationToolNotes()).isNotBlank();
            assertThat(scene.getSafetyNotes()).isNotBlank();
        }
    }

    // ─── Mock Gateway Empty Scenes ───────────────────────────────────

    @Nested
    @DisplayName("Mock gateway boş sahneler")
    class MockEmptyScenesTests {

        @Test
        @DisplayName("Boş sahne listesiyle failure döner")
        void shouldHandleEmptyScenesSafely() {
            VisualAssetPlanAgentRequest request = VisualAssetPlanAgentRequest.builder()
                    .storyboardDraftId(UUID.randomUUID())
                    .contentProjectId(UUID.randomUUID())
                    .title("Test")
                    .scenes(Collections.emptyList())
                    .build();

            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isNotBlank();
        }

        @Test
        @DisplayName("Null sahne listesiyle failure döner")
        void shouldHandleNullScenesSafely() {
            VisualAssetPlanAgentRequest request = VisualAssetPlanAgentRequest.builder()
                    .storyboardDraftId(UUID.randomUUID())
                    .contentProjectId(UUID.randomUUID())
                    .title("Test")
                    .scenes(null)
                    .build();

            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.isSuccess()).isFalse();
        }
    }

    // ─── Mock Gateway Failure Modes ──────────────────────────────────

    @Nested
    @DisplayName("Mock gateway failure modları")
    class MockFailureModeTests {

        @Test
        @DisplayName("TIMEOUT modu retryable failure döner")
        void shouldHandleTimeoutMode() {
            gateway.setMode(MockVisualAssetGatewayMode.TIMEOUT);
            VisualAssetPlanAgentRequest request = createSampleRequest(1);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
            assertThat(response.getRetryable()).isTrue();
            assertThat(response.getProviderType()).isEqualTo("MOCK");
        }

        @Test
        @DisplayName("EMPTY_RESPONSE modu retryable failure döner")
        void shouldHandleEmptyResponseMode() {
            gateway.setMode(MockVisualAssetGatewayMode.EMPTY_RESPONSE);
            VisualAssetPlanAgentRequest request = createSampleRequest(1);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("EMPTY_AI_RESPONSE");
            assertThat(response.getRetryable()).isTrue();
        }

        @Test
        @DisplayName("INVALID_FORMAT modu retryable failure döner")
        void shouldHandleInvalidFormatMode() {
            gateway.setMode(MockVisualAssetGatewayMode.INVALID_FORMAT);
            VisualAssetPlanAgentRequest request = createSampleRequest(1);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("UNSAFE_CONTENT modu non-retryable failure döner")
        void shouldHandleUnsafeContentMode() {
            gateway.setMode(MockVisualAssetGatewayMode.UNSAFE_CONTENT);
            VisualAssetPlanAgentRequest request = createSampleRequest(1);
            VisualAssetPlanAgentResponse response = gateway.generateVisualAssetPlan(request);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo("AI_RESPONSE_UNSAFE_CONTENT");
            assertThat(response.getRetryable()).isFalse();
        }
    }

    // ─── Forbidden Fields Safety ─────────────────────────────────────

    @Nested
    @DisplayName("Yasak alanlar güvenlik kontrolü")
    class ForbiddenFieldsSafety {

        @Test
        @DisplayName("Response DTO'da rawPrompt/providerRequest/providerResponse alanı yok")
        void shouldNotHaveForbiddenFieldsOnResponse() {
            assertDtoDoesNotHaveField(VisualAssetPlanAgentResponse.class, "rawPrompt");
            assertDtoDoesNotHaveField(VisualAssetPlanAgentResponse.class, "providerRequest");
            assertDtoDoesNotHaveField(VisualAssetPlanAgentResponse.class, "providerResponse");
            assertDtoDoesNotHaveField(VisualAssetPlanAgentResponse.class, "providerPayload");
            assertDtoDoesNotHaveField(VisualAssetPlanAgentResponse.class, "apiKey");
            assertDtoDoesNotHaveField(VisualAssetPlanAgentResponse.class, "secret");
        }

        @Test
        @DisplayName("Scene output DTO'da imageUrl/objectKey/bucket/mediaAssetId yok")
        void shouldNotHaveMediaFieldsOnSceneOutput() {
            assertDtoDoesNotHaveField(VisualAssetSceneOutput.class, "imageUrl");
            assertDtoDoesNotHaveField(VisualAssetSceneOutput.class, "objectKey");
            assertDtoDoesNotHaveField(VisualAssetSceneOutput.class, "bucket");
            assertDtoDoesNotHaveField(VisualAssetSceneOutput.class, "mediaAssetId");
            assertDtoDoesNotHaveField(VisualAssetSceneOutput.class, "generatedAssetId");
        }

        @Test
        @DisplayName("Thumbnail output DTO'da imageUrl/mediaAssetId yok")
        void shouldNotHaveMediaFieldsOnThumbnailOutput() {
            assertDtoDoesNotHaveField(VisualAssetThumbnailOutput.class, "imageUrl");
            assertDtoDoesNotHaveField(VisualAssetThumbnailOutput.class, "mediaAssetId");
            assertDtoDoesNotHaveField(VisualAssetThumbnailOutput.class, "generatedAssetId");
        }

        @Test
        @DisplayName("Request DTO'da rawPrompt/apiKey yok")
        void shouldNotHaveForbiddenFieldsOnRequest() {
            assertDtoDoesNotHaveField(VisualAssetPlanAgentRequest.class, "rawPrompt");
            assertDtoDoesNotHaveField(VisualAssetPlanAgentRequest.class, "apiKey");
            assertDtoDoesNotHaveField(VisualAssetPlanAgentRequest.class, "secret");
        }
    }

    // ─── Interface Verification ──────────────────────────────────────

    @Nested
    @DisplayName("Interface ve provider doğrulaması")
    class InterfaceVerification {

        @Test
        @DisplayName("Mock gateway AiVisualAssetAgentGateway implement eder")
        void shouldImplementGatewayInterface() {
            assertThat(gateway).isInstanceOf(AiVisualAssetAgentGateway.class);
        }

        @Test
        @DisplayName("OpenAI provider sınıfı bu task'ta oluşturulmadı")
        void openAiProviderShouldNotExistYet() {
            // OpenAI provider Task 09'da eklenecek — henüz mevcut değil
            try {
                Class.forName("com.anadoluatlasi.backend.ai.gateway.openai.OpenAiVisualAssetAgentGateway");
                // Eğer sınıf bulunursa test başarısız
                assertThat(false).as("OpenAI Visual Asset provider should not exist yet").isTrue();
            } catch (ClassNotFoundException e) {
                // Beklenen durum — sınıf henüz yok
                assertThat(true).isTrue();
            }
        }

        @Test
        @DisplayName("Mock gateway mode değiştirilebilir")
        void shouldAllowModeChange() {
            assertThat(gateway.getMode()).isEqualTo(MockVisualAssetGatewayMode.SUCCESS);

            gateway.setMode(MockVisualAssetGatewayMode.TIMEOUT);
            assertThat(gateway.getMode()).isEqualTo(MockVisualAssetGatewayMode.TIMEOUT);

            gateway.setMode(MockVisualAssetGatewayMode.SUCCESS);
            assertThat(gateway.getMode()).isEqualTo(MockVisualAssetGatewayMode.SUCCESS);
        }
    }

    // ─── Helper Methods ──────────────────────────────────────────────

    private VisualAssetPlanAgentRequest createSampleRequest(int sceneCount) {
        List<VisualAssetSceneInput> scenes = new java.util.ArrayList<>();
        String[] beatTypes = {"HOOK", "INTRODUCTION", "MAIN_NARRATIVE",
                "EMOTIONAL_CLIMAX", "CONCLUSION", "CALL_TO_ACTION"};

        for (int i = 1; i <= sceneCount; i++) {
            scenes.add(VisualAssetSceneInput.builder()
                    .storyboardSceneId(UUID.randomUUID())
                    .sceneNumber(i)
                    .sourceBeatType(beatTypes[(i - 1) % beatTypes.length])
                    .sourceBeatOrder(i)
                    .sceneTitle("Sahne " + i + " — Test Sahnesi")
                    .visualDescription("Anadolu köyünde sahne " + i + " görselleştirmesi")
                    .narrationText("Anlatıcı metni sahne " + i)
                    .mood("nostalgic")
                    .location("Anadolu köyü")
                    .characters("Yaşlı köylü")
                    .cameraDirection(i == 1 ? "wide shot" : "medium shot")
                    .estimatedDurationSeconds(15 + i * 5)
                    .build());
        }

        return VisualAssetPlanAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .storyboardDraftId(UUID.randomUUID())
                .contentProjectId(UUID.randomUUID())
                .title("Çanakkale Türküsü")
                .summary("Çanakkale destanının hikâyesi")
                .targetPlatform("YOUTUBE_LONG")
                .aspectRatio("16:9")
                .estimatedDurationSeconds(180)
                .visualStyleGuide("Cinematic Anatolian")
                .narrationStyle("Dramatic")
                .scenes(scenes)
                .thumbnailContext(VisualAssetThumbnailInput.builder()
                        .title("Çanakkale Türküsü")
                        .storyHook("Dramatik açılış")
                        .preferredAspectRatio("16:9")
                        .emotionalTone("merak ve nostalji")
                        .build())
                .languagePolicy("EN prompt + TR context")
                .culturalContextPolicy("Anatolian authenticity")
                .safetyPolicy("production-safe")
                .requestedBy("admin")
                .build();
    }

    private void assertDtoDoesNotHaveField(Class<?> dtoClass, String fieldName) {
        boolean hasField = Arrays.stream(dtoClass.getDeclaredFields())
                .anyMatch(f -> f.getName().equals(fieldName));
        assertThat(hasField)
                .as("DTO %s should NOT have field '%s'", dtoClass.getSimpleName(), fieldName)
                .isFalse();
    }
}
