package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.ContentAgentResponse;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-001 Task 23: OpenAiResponseParser testleri.
 *
 * <p>Real API çağrısı yapılmaz. JSON parse ve hata tespiti test edilir.</p>
 */
class OpenAiResponseParserTest {

    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();

    // ── Valid JSON ───────────────────────────────────────────

    @Nested
    @DisplayName("Valid JSON parsing")
    class ValidJson {

        @Test
        @DisplayName("Geçerli JSON başarıyla parse edilmeli")
        void shouldParseValidJson() {
            String json = buildValidJson();

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getResponse()).isNotNull();
            assertThat(result.getResponse().getTitle()).isEqualTo("Ankara Kalesi");
            assertThat(result.getResponse().getBody()).startsWith("Ankara'nın kalbinde");
            assertThat(result.getResponse().getSummary()).isEqualTo("Kısa özet");
        }

        @Test
        @DisplayName("Suggested tags parse edilmeli")
        void shouldParseSuggestedTags() {
            String json = buildValidJson();

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.getResponse().getSuggestedTags()).containsExactly("tarih", "ankara", "kale");
        }

        @Test
        @DisplayName("Warnings parse edilmeli")
        void shouldParseWarnings() {
            String json = buildValidJson();

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.getResponse().getWarnings()).hasSize(1);
            assertThat(result.getResponse().getWarnings().get(0).getCode()).isEqualTo("NEEDS_SOURCE_CHECK");
        }

        @Test
        @DisplayName("requestId ve projectId response'a eklenmeli")
        void shouldIncludeIdsInResponse() {
            String json = buildValidJson();

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.getResponse().getRequestId()).isEqualTo(REQUEST_ID);
            assertThat(result.getResponse().getProjectId()).isEqualTo(PROJECT_ID);
        }

        @Test
        @DisplayName("qualityNotes ve generationReason parse edilmeli")
        void shouldParseQualityFields() {
            String json = buildValidJson();

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.getResponse().getQualityNotes()).isEqualTo("İyi kalite");
            assertThat(result.getResponse().getGenerationReason()).isEqualTo("Bilgilendirici yaklaşım");
        }
    }

    // ── Markdown Code Fence ─────────────────────────────────

    @Nested
    @DisplayName("Markdown code fence")
    class CodeFence {

        @Test
        @DisplayName("```json ... ``` içindeki JSON parse edilmeli")
        void shouldParseJsonInCodeFence() {
            String fenced = "```json\n" + buildValidJson() + "\n```";

            OpenAiParseResult result = OpenAiResponseParser.parse(fenced, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getResponse().getTitle()).isEqualTo("Ankara Kalesi");
        }

        @Test
        @DisplayName("``` ... ``` içindeki JSON parse edilmeli")
        void shouldParseJsonInPlainCodeFence() {
            String fenced = "```\n" + buildValidJson() + "\n```";

            OpenAiParseResult result = OpenAiResponseParser.parse(fenced, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("stripCodeFence temiz JSON'u değiştirmemeli")
        void stripShouldNotAffectCleanJson() {
            String json = buildValidJson();
            assertThat(OpenAiResponseParser.stripCodeFence(json)).isEqualTo(json);
        }
    }

    // ── Empty Response ──────────────────────────────────────

    @Nested
    @DisplayName("Empty response")
    class EmptyResponse {

        @Test
        @DisplayName("null yanıt EMPTY_AI_RESPONSE döndürmeli")
        void shouldRejectNull() {
            OpenAiParseResult result = OpenAiResponseParser.parse(null, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.EMPTY_AI_RESPONSE);
            assertThat(result.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("Boş string EMPTY_AI_RESPONSE döndürmeli")
        void shouldRejectEmptyString() {
            OpenAiParseResult result = OpenAiResponseParser.parse("", REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.EMPTY_AI_RESPONSE);
        }

        @Test
        @DisplayName("Sadece whitespace EMPTY_AI_RESPONSE döndürmeli")
        void shouldRejectWhitespace() {
            OpenAiParseResult result = OpenAiResponseParser.parse("   \n  ", REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.EMPTY_AI_RESPONSE);
        }
    }

    // ── Invalid JSON ────────────────────────────────────────

    @Nested
    @DisplayName("Invalid JSON")
    class InvalidJson {

        @Test
        @DisplayName("Geçersiz JSON INVALID_AI_RESPONSE_FORMAT döndürmeli")
        void shouldRejectInvalidJson() {
            OpenAiParseResult result = OpenAiResponseParser.parse("Bu bir JSON değil.", REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.INVALID_AI_RESPONSE_FORMAT);
            assertThat(result.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("Yarım JSON INVALID_AI_RESPONSE_FORMAT döndürmeli")
        void shouldRejectPartialJson() {
            OpenAiParseResult result = OpenAiResponseParser.parse("{\"title\": ", REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.INVALID_AI_RESPONSE_FORMAT);
        }
    }

    // ── Missing Required Fields ─────────────────────────────

    @Nested
    @DisplayName("Missing required fields")
    class MissingFields {

        @Test
        @DisplayName("Title eksikse INVALID_AI_RESPONSE_FORMAT")
        void shouldRejectMissingTitle() {
            String json = """
                    {
                      "body": "Yeterince uzun bir içerik metni olmalı ki minimum karakter kontrolünden geçsin ve test başarılı olsun.",
                      "summary": "Özet"
                    }
                    """;

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.INVALID_AI_RESPONSE_FORMAT);
        }

        @Test
        @DisplayName("Body eksikse INVALID_AI_RESPONSE_FORMAT")
        void shouldRejectMissingBody() {
            String json = """
                    {
                      "title": "Başlık",
                      "summary": "Özet"
                    }
                    """;

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.INVALID_AI_RESPONSE_FORMAT);
        }

        @Test
        @DisplayName("Boş title INVALID_AI_RESPONSE_FORMAT")
        void shouldRejectBlankTitle() {
            String json = """
                    {
                      "title": "   ",
                      "body": "Yeterince uzun bir içerik metni olmalı ki minimum karakter kontrolünden geçsin ve test başarılı olsun.",
                      "summary": "Özet"
                    }
                    """;

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.INVALID_AI_RESPONSE_FORMAT);
        }
    }

    // ── Too Short Body ──────────────────────────────────────

    @Nested
    @DisplayName("Too short body")
    class TooShort {

        @Test
        @DisplayName("50 karakterden kısa body AI_RESPONSE_TOO_SHORT")
        void shouldRejectTooShortBody() {
            String json = """
                    {
                      "title": "Başlık",
                      "body": "Kısa metin.",
                      "summary": "Özet"
                    }
                    """;

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.AI_RESPONSE_TOO_SHORT);
            assertThat(result.isRetryable()).isTrue();
        }
    }

    // ── Unsafe/Refusal Content ──────────────────────────────

    @Nested
    @DisplayName("Unsafe/refusal content")
    class UnsafeContent {

        @Test
        @DisplayName("Refusal mesajı AI_RESPONSE_UNSAFE_CONTENT")
        void shouldDetectEnglishRefusal() {
            String refusal = "I cannot generate this content due to safety guidelines.";

            OpenAiParseResult result = OpenAiResponseParser.parse(refusal, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT);
            assertThat(result.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("Türkçe refusal mesajı AI_RESPONSE_UNSAFE_CONTENT")
        void shouldDetectTurkishRefusal() {
            String refusal = "Bu isteği yerine getiremem, uygun olmayan bir talep.";

            OpenAiParseResult result = OpenAiResponseParser.parse(refusal, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT);
        }

        @Test
        @DisplayName("Content policy refusal mesajı AI_RESPONSE_UNSAFE_CONTENT")
        void shouldDetectContentPolicyRefusal() {
            String refusal = "This request violates our content policy and I'm unable to help.";

            OpenAiParseResult result = OpenAiResponseParser.parse(refusal, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT);
        }

        @Test
        @DisplayName("JSON ile başlayan geçerli yanıt refusal olarak algılanmamalı")
        void shouldNotFlagValidJsonAsRefusal() {
            String json = buildValidJson();

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    // ── Extra Fields ────────────────────────────────────────

    @Nested
    @DisplayName("Extra/unknown fields")
    class ExtraFields {

        @Test
        @DisplayName("Bilinmeyen alanlar göz ardı edilmeli")
        void shouldIgnoreUnknownFields() {
            String json = """
                    {
                      "title": "Başlık",
                      "body": "Yeterince uzun bir içerik metni olmalı ki minimum karakter kontrolünden geçsin ve test başarılı olsun.",
                      "summary": "Özet",
                      "unknownField": "değer",
                      "anotherField": 42
                    }
                    """;

            OpenAiParseResult result = OpenAiResponseParser.parse(json, REQUEST_ID, PROJECT_ID);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private String buildValidJson() {
        return """
                {
                  "title": "Ankara Kalesi",
                  "summary": "Kısa özet",
                  "body": "Ankara'nın kalbinde yükselen Ankara Kalesi, tarih boyunca pek çok medeniyete ev sahipliği yapmıştır. Roma, Bizans ve Osmanlı dönemlerinde önemli bir savunma yapısı olarak kullanılmıştır.",
                  "suggestedTags": ["tarih", "ankara", "kale"],
                  "warnings": [
                    { "code": "NEEDS_SOURCE_CHECK", "message": "Yapım tarihi için ek kaynak doğrulaması gerekli" }
                  ],
                  "qualityNotes": "İyi kalite",
                  "generationReason": "Bilgilendirici yaklaşım"
                }
                """;
    }
}
