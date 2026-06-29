package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.ContentAgentResponse;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FP-001 Task 23: OpenAI model çıktısını ContentAgentResponse'a dönüştüren parser.
 *
 * <p>Provider-internal sınıftır. REST'e expose edilmez.
 * Raw model çıktısı dışarıya sızdırılmaz.</p>
 *
 * <p>Parser kuralları:</p>
 * <ul>
 *   <li>Strict JSON bekler</li>
 *   <li>Markdown code fence varsa temizler</li>
 *   <li>title ve body zorunludur</li>
 *   <li>body 50 karakterden kısa olamaz</li>
 *   <li>Unsafe/refusal içerik tespit eder</li>
 * </ul>
 */
@Slf4j
class OpenAiResponseParser {

    private static final int MIN_BODY_LENGTH = 50;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Markdown code fence pattern: ```json ... ``` veya ``` ... ```
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile(
            "^\\s*```(?:json)?\\s*\\n?(.*?)\\n?\\s*```\\s*$",
            Pattern.DOTALL
    );

    // Basit unsafe/refusal tespiti
    private static final List<String> REFUSAL_SIGNALS = List.of(
            "I cannot", "I can't", "I'm unable", "I am unable",
            "content policy", "safety guidelines",
            "not appropriate", "inappropriate content",
            "Bu isteği yerine getiremem", "Uygun olmayan içerik"
    );

    /**
     * Raw model text çıktısını parse eder.
     *
     * @param rawContent model output string
     * @param requestId  istek referansı
     * @param projectId  proje referansı
     * @return başarılı parse result veya hata
     */
    static OpenAiParseResult parse(String rawContent, UUID requestId, UUID projectId) {
        // 1. Empty check
        if (rawContent == null || rawContent.isBlank()) {
            log.warn("OpenAI yanıtı boş. requestId={}", requestId);
            return OpenAiParseResult.failure(AiErrorType.EMPTY_AI_RESPONSE,
                    "AI yanıtı boş döndü.", true);
        }

        // 2. Refusal/unsafe detection
        String trimmed = rawContent.trim();
        if (isRefusalContent(trimmed)) {
            log.warn("OpenAI yanıtı güvenli olmayan içerik/refusal tespit edildi. requestId={}", requestId);
            return OpenAiParseResult.failure(AiErrorType.AI_RESPONSE_UNSAFE_CONTENT,
                    "AI modeli güvenlik nedeniyle yanıt vermeyi reddetti.", false);
        }

        // 3. Strip markdown code fence
        String jsonText = stripCodeFence(trimmed);

        // 4. Parse JSON
        OpenAiRawResponse parsed;
        try {
            parsed = OBJECT_MAPPER.readValue(jsonText, OpenAiRawResponse.class);
        } catch (Exception e) {
            log.warn("OpenAI yanıtı JSON parse hatası. requestId={}, error={}", requestId, e.getMessage());
            return OpenAiParseResult.failure(AiErrorType.INVALID_AI_RESPONSE_FORMAT,
                    "AI yanıtı geçerli JSON formatında değil: " + e.getMessage(), true);
        }

        // 5. Validate required fields
        if (parsed.getTitle() == null || parsed.getTitle().isBlank()) {
            log.warn("OpenAI yanıtında title eksik. requestId={}", requestId);
            return OpenAiParseResult.failure(AiErrorType.INVALID_AI_RESPONSE_FORMAT,
                    "AI yanıtında başlık (title) eksik.", true);
        }

        if (parsed.getBody() == null || parsed.getBody().isBlank()) {
            log.warn("OpenAI yanıtında body eksik. requestId={}", requestId);
            return OpenAiParseResult.failure(AiErrorType.INVALID_AI_RESPONSE_FORMAT,
                    "AI yanıtında içerik (body) eksik.", true);
        }

        // 6. Min body length check
        if (parsed.getBody().length() < MIN_BODY_LENGTH) {
            log.warn("OpenAI yanıtı çok kısa: {} karakter. requestId={}", parsed.getBody().length(), requestId);
            return OpenAiParseResult.failure(AiErrorType.AI_RESPONSE_TOO_SHORT,
                    "AI yanıtı çok kısa (" + parsed.getBody().length() + " karakter). Minimum: " + MIN_BODY_LENGTH, true);
        }

        // 7. Build ContentAgentResponse
        List<ContentAgentResponse.ContentAgentWarning> warnings = Collections.emptyList();
        if (parsed.getWarnings() != null) {
            warnings = parsed.getWarnings().stream()
                    .map(w -> ContentAgentResponse.ContentAgentWarning.builder()
                            .code(w.getCode())
                            .message(w.getMessage())
                            .build())
                    .toList();
        }

        ContentAgentResponse response = ContentAgentResponse.builder()
                .requestId(requestId)
                .projectId(projectId)
                .title(parsed.getTitle())
                .body(parsed.getBody())
                .summary(parsed.getSummary())
                .suggestedTags(parsed.getSuggestedTags() != null ? parsed.getSuggestedTags() : Collections.emptyList())
                .warnings(warnings)
                .qualityNotes(parsed.getQualityNotes())
                .generationReason(parsed.getGenerationReason())
                .build();

        return OpenAiParseResult.success(response);
    }

    /**
     * Markdown code fence'i temizler.
     */
    static String stripCodeFence(String input) {
        Matcher matcher = CODE_FENCE_PATTERN.matcher(input);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return input;
    }

    /**
     * Basit refusal/unsafe content tespiti.
     */
    private static boolean isRefusalContent(String content) {
        // JSON ile başlıyorsa refusal değil
        if (content.startsWith("{") || content.startsWith("[")) {
            return false;
        }
        String lower = content.toLowerCase();
        return REFUSAL_SIGNALS.stream()
                .anyMatch(signal -> lower.contains(signal.toLowerCase()));
    }

    // ── Internal Raw Response DTO ────────────────────────

    /**
     * OpenAI model çıktısının ham JSON yapısı.
     * Provider-internal — dışarıya expose edilmez.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenAiRawResponse {
        private String title;
        private String summary;
        private String body;
        private List<String> suggestedTags;
        private List<WarningEntry> warnings;
        private String qualityNotes;
        private String generationReason;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class WarningEntry {
            private String code;
            private String message;
        }
    }
}
