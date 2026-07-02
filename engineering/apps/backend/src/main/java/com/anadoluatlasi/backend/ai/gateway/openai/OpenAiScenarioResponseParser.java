package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.ScenarioAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ScenarioBeatResult;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FP-002 Task 09: OpenAI model çıktısını ScenarioAgentResponse'a dönüştüren parser.
 *
 * <p>Provider-internal sınıftır. REST'e expose edilmez.
 * Raw model çıktısı dışarıya sızdırılmaz.</p>
 *
 * <p>Parser kuralları:</p>
 * <ul>
 *   <li>Strict JSON bekler</li>
 *   <li>Markdown code fence varsa temizler</li>
 *   <li>scenarioTitle zorunludur</li>
 *   <li>Tam 6 beat zorunludur</li>
 *   <li>Her beat'te non-empty body zorunludur</li>
 *   <li>Unsafe/refusal içerik tespit eder</li>
 * </ul>
 */
@Slf4j
class OpenAiScenarioResponseParser {

    private static final int REQUIRED_BEAT_COUNT = 6;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Markdown code fence pattern
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile(
            "^\\s*```(?:json)?\\s*\\n?(.*?)\\n?\\s*```\\s*$",
            Pattern.DOTALL
    );

    // Refusal detection
    private static final List<String> REFUSAL_SIGNALS = List.of(
            "I cannot", "I can't", "I'm unable", "I am unable",
            "content policy", "safety guidelines",
            "not appropriate", "inappropriate content",
            "Bu isteği yerine getiremem", "Uygun olmayan içerik"
    );

    // Beat type → order mapping (deterministic)
    private static final Map<String, ScenarioBeatType> BEAT_TYPE_MAP = Map.of(
            "HOOK", ScenarioBeatType.HOOK,
            "INTRODUCTION", ScenarioBeatType.INTRODUCTION,
            "MAIN_NARRATIVE", ScenarioBeatType.MAIN_NARRATIVE,
            "EMOTIONAL_CLIMAX", ScenarioBeatType.EMOTIONAL_CLIMAX,
            "CONCLUSION", ScenarioBeatType.CONCLUSION,
            "CALL_TO_ACTION", ScenarioBeatType.CALL_TO_ACTION
    );

    /**
     * Raw model text çıktısını parse eder.
     *
     * @param rawContent     model output string
     * @param providerType   safe metadata
     * @param modelName      safe metadata
     * @return başarılı ScenarioAgentResponse veya failure ScenarioAgentResponse
     */
    static ScenarioAgentResponse parse(String rawContent, String providerType, String modelName) {
        // 1. Empty check
        if (rawContent == null || rawContent.isBlank()) {
            log.warn("OpenAI scenario yanıtı boş.");
            return failureResponse("EMPTY_AI_RESPONSE", "AI yanıtı boş döndü.", true, providerType, modelName);
        }

        // 2. Refusal/unsafe detection
        String trimmed = rawContent.trim();
        if (isRefusalContent(trimmed)) {
            log.warn("OpenAI scenario yanıtı güvenli olmayan içerik/refusal tespit edildi.");
            return failureResponse("AI_RESPONSE_UNSAFE_CONTENT",
                    "AI modeli güvenlik nedeniyle yanıt vermeyi reddetti.", false, providerType, modelName);
        }

        // 3. Strip markdown code fence
        String jsonText = stripCodeFence(trimmed);

        // 4. Parse JSON
        OpenAiScenarioRawResponse parsed;
        try {
            parsed = OBJECT_MAPPER.readValue(jsonText, OpenAiScenarioRawResponse.class);
        } catch (Exception e) {
            log.warn("OpenAI scenario yanıtı JSON parse hatası: {}", e.getMessage());
            return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                    "AI yanıtı geçerli JSON formatında değil.", true, providerType, modelName);
        }

        // 5. Validate title
        if (parsed.getScenarioTitle() == null || parsed.getScenarioTitle().isBlank()) {
            log.warn("OpenAI scenario yanıtında scenarioTitle eksik.");
            return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                    "AI yanıtında senaryo başlığı (scenarioTitle) eksik.", true, providerType, modelName);
        }

        // 6. Validate beats
        if (parsed.getBeats() == null || parsed.getBeats().isEmpty()) {
            log.warn("OpenAI scenario yanıtında beats eksik.");
            return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                    "AI yanıtında beat listesi eksik.", true, providerType, modelName);
        }

        if (parsed.getBeats().size() != REQUIRED_BEAT_COUNT) {
            log.warn("OpenAI scenario yanıtında {} beat var, {} bekleniyor.", parsed.getBeats().size(), REQUIRED_BEAT_COUNT);
            return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                    "AI yanıtında " + parsed.getBeats().size() + " beat var, " + REQUIRED_BEAT_COUNT + " bekleniyor.", true,
                    providerType, modelName);
        }

        // 7. Map and validate beats
        List<ScenarioBeatResult> beatResults = new ArrayList<>();
        Set<ScenarioBeatType> seenTypes = EnumSet.noneOf(ScenarioBeatType.class);

        for (OpenAiScenarioRawResponse.RawBeat rawBeat : parsed.getBeats()) {
            if (rawBeat.getType() == null || rawBeat.getType().isBlank()) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında beat type eksik.", true, providerType, modelName);
            }

            ScenarioBeatType beatType = BEAT_TYPE_MAP.get(rawBeat.getType().toUpperCase());
            if (beatType == null) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "Bilinmeyen beat tipi: " + rawBeat.getType(), true, providerType, modelName);
            }

            if (seenTypes.contains(beatType)) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "Tekrarlanan beat tipi: " + beatType.name(), true, providerType, modelName);
            }
            seenTypes.add(beatType);

            if (rawBeat.getBody() == null || rawBeat.getBody().isBlank()) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "Beat body boş: " + beatType.name(), true, providerType, modelName);
            }

            beatResults.add(ScenarioBeatResult.builder()
                    .type(beatType)
                    .title(rawBeat.getTitle())
                    .body(rawBeat.getBody())
                    .order(beatType.getOrder())
                    .estimatedDurationSeconds(rawBeat.getEstimatedDurationSeconds())
                    .build());
        }

        // 8. Verify all required types present
        for (ScenarioBeatType required : ScenarioBeatType.values()) {
            if (!seenTypes.contains(required)) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "Eksik beat tipi: " + required.name(), true, providerType, modelName);
            }
        }

        // 9. Sort beats by deterministic order
        beatResults.sort(Comparator.comparingInt(ScenarioBeatResult::getOrder));

        // 10. Build success response
        return ScenarioAgentResponse.builder()
                .success(true)
                .scenarioTitle(parsed.getScenarioTitle())
                .summary(parsed.getSummary())
                .targetAudience(parsed.getTargetAudience())
                .culturalObjective(parsed.getCulturalObjective())
                .tone(parsed.getTone())
                .estimatedDurationSeconds(parsed.getEstimatedDurationSeconds())
                .setting(parsed.getSetting())
                .characters(parsed.getCharacters())
                .beats(beatResults)
                .providerType(providerType)
                .modelName(modelName)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────

    private static ScenarioAgentResponse failureResponse(
            String errorCode, String message, boolean retryable, String providerType, String modelName) {
        return ScenarioAgentResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(message)
                .retryable(retryable)
                .providerType(providerType)
                .modelName(modelName)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    static String stripCodeFence(String input) {
        Matcher matcher = CODE_FENCE_PATTERN.matcher(input);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return input;
    }

    private static boolean isRefusalContent(String content) {
        if (content.startsWith("{") || content.startsWith("[")) {
            return false;
        }
        String lower = content.toLowerCase();
        return REFUSAL_SIGNALS.stream()
                .anyMatch(signal -> lower.contains(signal.toLowerCase()));
    }

    // ── Internal Raw Response DTO ────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenAiScenarioRawResponse {
        private String scenarioTitle;
        private String summary;
        private String targetAudience;
        private String culturalObjective;
        private String tone;
        private Integer estimatedDurationSeconds;
        private String setting;
        private String characters;
        private List<RawBeat> beats;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class RawBeat {
            private String type;
            private String title;
            private String body;
            private int order;
            private Integer estimatedDurationSeconds;
        }
    }
}
