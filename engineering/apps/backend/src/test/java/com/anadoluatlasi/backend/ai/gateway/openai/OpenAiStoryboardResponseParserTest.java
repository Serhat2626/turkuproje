package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.StoryboardAgentResponse;
import com.anadoluatlasi.backend.ai.contract.StoryboardSceneOutput;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-003 Task 09: OpenAiStoryboardResponseParser testleri.
 *
 * <p>Real API çağrısı yapılmaz. JSON parse, validasyon ve hata tespiti test edilir.</p>
 */
class OpenAiStoryboardResponseParserTest {

    private static final String PROVIDER = "OPENAI";
    private static final String MODEL = "gpt-4o";

    // ── Valid JSON ───────────────────────────────────────────

    @Nested
    @DisplayName("Valid JSON parsing")
    class ValidJson {

        @Test
        @DisplayName("Geçerli 3-scene JSON başarıyla parse edilmeli")
        void shouldParseValidJsonWithThreeScenes() {
            String json = buildValidThreeSceneJson();

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStoryboardTitle()).isEqualTo("Test Storyboard");
            assertThat(result.getScenes()).hasSize(3);
        }

        @Test
        @DisplayName("providerType ve modelName doğru aktarılmalı")
        void shouldPassThroughProviderAndModel() {
            String json = buildValidSingleSceneJson();

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getProviderType()).isEqualTo(PROVIDER);
            assertThat(result.getModelName()).isEqualTo(MODEL);
        }

        @Test
        @DisplayName("Başarılı parse sonucu raw provider payload içermemeli")
        void shouldNotContainRawProviderPayload() {
            String json = buildValidSingleSceneJson();

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isTrue();
            // errorCode, errorMessage ve retryable başarılı yanıtlarda null olmalı
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
            assertThat(result.getRetryable()).isNull();
        }
    }

    // ── Empty Response ──────────────────────────────────────

    @Nested
    @DisplayName("Empty response")
    class EmptyResponse {

        @Test
        @DisplayName("null yanıt EMPTY_AI_RESPONSE döndürmeli, retryable=true")
        void shouldRejectNull() {
            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(null, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("EMPTY_AI_RESPONSE");
            assertThat(result.getRetryable()).isTrue();
        }

        @Test
        @DisplayName("Boş string EMPTY_AI_RESPONSE döndürmeli, retryable=true")
        void shouldRejectEmptyString() {
            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse("", PROVIDER, MODEL);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("EMPTY_AI_RESPONSE");
            assertThat(result.getRetryable()).isTrue();
        }
    }

    // ── Missing Required Fields ─────────────────────────────

    @Nested
    @DisplayName("Missing required fields")
    class MissingFields {

        @Test
        @DisplayName("storyboardTitle eksikse INVALID_AI_RESPONSE_FORMAT")
        void shouldRejectMissingTitle() {
            String json = """
                    {
                      "summary": "Özet",
                      "scenes": [
                        {
                          "sceneNumber": 1,
                          "visualDescription": "Gökyüzü"
                        }
                      ]
                    }
                    """;

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("scenes listesi boşsa INVALID_AI_RESPONSE_FORMAT")
        void shouldRejectEmptyScenes() {
            String json = """
                    {
                      "storyboardTitle": "Başlık",
                      "scenes": []
                    }
                    """;

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("scenes alanı tamamen eksikse INVALID_AI_RESPONSE_FORMAT")
        void shouldRejectMissingScenes() {
            String json = """
                    {
                      "storyboardTitle": "Başlık"
                    }
                    """;

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }
    }

    // ── Scene Validation ────────────────────────────────────

    @Nested
    @DisplayName("Scene validation")
    class SceneValidation {

        @Test
        @DisplayName("visualDescription eksik sahne INVALID_AI_RESPONSE_FORMAT, hata sahne numarasını içermeli")
        void shouldRejectMissingVisualDescriptionWithSceneNumber() {
            String json = """
                    {
                      "storyboardTitle": "Başlık",
                      "scenes": [
                        {
                          "sceneNumber": 1,
                          "visualDescription": "Gökyüzü"
                        },
                        {
                          "sceneNumber": 2,
                          "sceneTitle": "İkinci sahne"
                        }
                      ]
                    }
                    """;

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
            assertThat(result.getErrorMessage()).contains("2");
        }

        @Test
        @DisplayName("Tekrarlanan sahne numarası INVALID_AI_RESPONSE_FORMAT")
        void shouldRejectDuplicateSceneNumbers() {
            String json = """
                    {
                      "storyboardTitle": "Başlık",
                      "scenes": [
                        {
                          "sceneNumber": 1,
                          "visualDescription": "Gökyüzü"
                        },
                        {
                          "sceneNumber": 1,
                          "visualDescription": "Dağ manzarası"
                        }
                      ]
                    }
                    """;

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }

        @Test
        @DisplayName("Ardışık olmayan sahne numaraları (1,3) INVALID_AI_RESPONSE_FORMAT")
        void shouldRejectNonSequentialSceneNumbers() {
            String json = """
                    {
                      "storyboardTitle": "Başlık",
                      "scenes": [
                        {
                          "sceneNumber": 1,
                          "visualDescription": "Gökyüzü"
                        },
                        {
                          "sceneNumber": 3,
                          "visualDescription": "Dağ manzarası"
                        }
                      ]
                    }
                    """;

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
        }
    }

    // ── Invalid JSON ────────────────────────────────────────

    @Nested
    @DisplayName("Invalid JSON")
    class InvalidJson {

        @Test
        @DisplayName("Geçersiz JSON INVALID_AI_RESPONSE_FORMAT, retryable=true")
        void shouldRejectInvalidJson() {
            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(
                    "{Bu bir JSON değil}", PROVIDER, MODEL);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AI_RESPONSE_FORMAT");
            assertThat(result.getRetryable()).isTrue();
        }
    }

    // ── Unsafe/Refusal Content ──────────────────────────────

    @Nested
    @DisplayName("Unsafe/refusal content")
    class UnsafeContent {

        @Test
        @DisplayName("Refusal mesajı AI_RESPONSE_UNSAFE_CONTENT, retryable=false")
        void shouldDetectRefusal() {
            String refusal = "I cannot generate this content due to safety guidelines.";

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(refusal, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo("AI_RESPONSE_UNSAFE_CONTENT");
            assertThat(result.getRetryable()).isFalse();
        }
    }

    // ── Markdown Code Fence ─────────────────────────────────

    @Nested
    @DisplayName("Markdown code fence")
    class CodeFence {

        @Test
        @DisplayName("```json ... ``` içindeki JSON başarıyla parse edilmeli")
        void shouldParseJsonInCodeFence() {
            String fenced = "```json\n" + buildValidSingleSceneJson() + "\n```";

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(fenced, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStoryboardTitle()).isEqualTo("Test Storyboard");
        }

        @Test
        @DisplayName("stripCodeFence temiz JSON'u değiştirmemeli")
        void stripShouldNotAffectCleanJson() {
            String json = buildValidSingleSceneJson();
            assertThat(OpenAiStoryboardResponseParser.stripCodeFence(json)).isEqualTo(json);
        }
    }

    // ── Beat Type Mapping ───────────────────────────────────

    @Nested
    @DisplayName("Beat type mapping")
    class BeatTypeMapping {

        @Test
        @DisplayName("HOOK → ScenarioBeatType.HOOK doğru map edilmeli")
        void shouldMapHookBeatType() {
            String json = buildValidSingleSceneJson();

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isTrue();
            StoryboardSceneOutput scene = result.getScenes().get(0);
            assertThat(scene.getSourceBeatType()).isEqualTo(ScenarioBeatType.HOOK);
        }

        @Test
        @DisplayName("Bilinmeyen sourceBeatType → null (tolerant)")
        void shouldReturnNullForUnknownBeatType() {
            String json = """
                    {
                      "storyboardTitle": "Test Storyboard",
                      "summary": "Özet",
                      "scenes": [
                        {
                          "sceneNumber": 1,
                          "sourceBeatType": "UNKNOWN_TYPE",
                          "sourceBeatOrder": 0,
                          "visualDescription": "Gökyüzü manzarası",
                          "estimatedDurationSeconds": 10
                        }
                      ]
                    }
                    """;

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getScenes().get(0).getSourceBeatType()).isNull();
        }
    }

    // ── Scene Ordering & mediaGenerationNotes ───────────────

    @Nested
    @DisplayName("Scene ordering and output fields")
    class SceneOutputFields {

        @Test
        @DisplayName("Sahneler sceneNumber'a göre sıralanmalı")
        void shouldSortScenesBySceneNumber() {
            // JSON'da sahne 2, sahne 1 sırasıyla ver — parser sıralamalı
            String json = """
                    {
                      "storyboardTitle": "Test Storyboard",
                      "summary": "Özet",
                      "scenes": [
                        {
                          "sceneNumber": 2,
                          "sourceBeatType": "INTRODUCTION",
                          "visualDescription": "Dağ manzarası",
                          "estimatedDurationSeconds": 15
                        },
                        {
                          "sceneNumber": 1,
                          "sourceBeatType": "HOOK",
                          "visualDescription": "Gökyüzü manzarası",
                          "estimatedDurationSeconds": 10
                        }
                      ]
                    }
                    """;

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getScenes()).hasSize(2);
            assertThat(result.getScenes().get(0).getSceneNumber()).isEqualTo(1);
            assertThat(result.getScenes().get(1).getSceneNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("mediaGenerationNotes output'ta her zaman null olmalı")
        void shouldAlwaysHaveNullMediaGenerationNotes() {
            String json = buildValidThreeSceneJson();

            StoryboardAgentResponse result = OpenAiStoryboardResponseParser.parse(json, PROVIDER, MODEL);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getScenes())
                    .allSatisfy(scene ->
                            assertThat(scene.getMediaGenerationNotes()).isNull());
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private String buildValidSingleSceneJson() {
        return """
                {
                  "storyboardTitle": "Test Storyboard",
                  "summary": "Test özet",
                  "targetPlatform": "YouTube",
                  "aspectRatio": "16:9",
                  "estimatedDurationSeconds": 600,
                  "visualStyleGuide": "Sinematik",
                  "narrationStyle": "Anlatıcı",
                  "scenes": [
                    {
                      "sceneNumber": 1,
                      "sourceBeatType": "HOOK",
                      "sourceBeatOrder": 0,
                      "sceneTitle": "Açılış",
                      "visualDescription": "Gökyüzü manzarası",
                      "narrationText": "Bir zamanlar...",
                      "mood": "dramatic",
                      "location": "Anadolu",
                      "estimatedDurationSeconds": 10
                    }
                  ]
                }
                """;
    }

    private String buildValidThreeSceneJson() {
        return """
                {
                  "storyboardTitle": "Test Storyboard",
                  "summary": "Test özet",
                  "targetPlatform": "YouTube",
                  "aspectRatio": "16:9",
                  "estimatedDurationSeconds": 600,
                  "visualStyleGuide": "Sinematik",
                  "narrationStyle": "Anlatıcı",
                  "scenes": [
                    {
                      "sceneNumber": 1,
                      "sourceBeatType": "HOOK",
                      "sourceBeatOrder": 0,
                      "sceneTitle": "Açılış",
                      "visualDescription": "Gökyüzü manzarası",
                      "narrationText": "Bir zamanlar...",
                      "mood": "dramatic",
                      "location": "Anadolu",
                      "estimatedDurationSeconds": 10
                    },
                    {
                      "sceneNumber": 2,
                      "sourceBeatType": "INTRODUCTION",
                      "sourceBeatOrder": 1,
                      "sceneTitle": "Tanıtım",
                      "visualDescription": "Köy meydanı",
                      "narrationText": "Bu köyde...",
                      "mood": "nostalgic",
                      "location": "Köy",
                      "estimatedDurationSeconds": 20
                    },
                    {
                      "sceneNumber": 3,
                      "sourceBeatType": "MAIN_NARRATIVE",
                      "sourceBeatOrder": 2,
                      "sceneTitle": "Ana Hikâye",
                      "visualDescription": "Dağ yolu",
                      "narrationText": "Yolculuk başladı...",
                      "mood": "adventurous",
                      "location": "Dağ",
                      "estimatedDurationSeconds": 30
                    }
                  ]
                }
                """;
    }
}
