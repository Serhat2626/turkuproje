package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.StoryboardAgentResponse;
import com.anadoluatlasi.backend.ai.contract.StoryboardSceneOutput;
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
 * FP-003 Task 09: OpenAI model çıktısını StoryboardAgentResponse'a dönüştüren parser.
 *
 * <p>Provider-internal sınıftır. REST'e expose edilmez.
 * Raw model çıktısı dışarıya sızdırılmaz.</p>
 *
 * <p>Parser kuralları:</p>
 * <ul>
 *   <li>Strict JSON bekler</li>
 *   <li>Markdown code fence varsa temizler</li>
 *   <li>storyboardTitle zorunludur</li>
 *   <li>En az 1 scene zorunludur</li>
 *   <li>Her scene'de visualDescription zorunludur</li>
 *   <li>Scene number'lar sıralı ve unique olmalıdır</li>
 *   <li>Unsafe/refusal içerik tespit eder</li>
 * </ul>
 */
@Slf4j
class OpenAiStoryboardResponseParser {

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

    // Beat type mapping (tolerant — unknown types → null)
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
     * @return başarılı StoryboardAgentResponse veya failure StoryboardAgentResponse
     */
    static StoryboardAgentResponse parse(String rawContent, String providerType, String modelName) {
        // 1. Empty check
        if (rawContent == null || rawContent.isBlank()) {
            log.warn("OpenAI storyboard yanıtı boş.");
            return failureResponse("EMPTY_AI_RESPONSE", "AI yanıtı boş döndü.", true, providerType, modelName);
        }

        // 2. Refusal/unsafe detection
        String trimmed = rawContent.trim();
        if (isRefusalContent(trimmed)) {
            log.warn("OpenAI storyboard yanıtı güvenli olmayan içerik/refusal tespit edildi.");
            return failureResponse("AI_RESPONSE_UNSAFE_CONTENT",
                    "AI modeli güvenlik nedeniyle yanıt vermeyi reddetti.", false, providerType, modelName);
        }

        // 3. Strip markdown code fence
        String jsonText = stripCodeFence(trimmed);

        // 4. Parse JSON
        OpenAiStoryboardRawResponse parsed;
        try {
            parsed = OBJECT_MAPPER.readValue(jsonText, OpenAiStoryboardRawResponse.class);
        } catch (Exception e) {
            log.warn("OpenAI storyboard yanıtı JSON parse hatası: {}", e.getMessage());
            return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                    "AI yanıtı geçerli JSON formatında değil.", true, providerType, modelName);
        }

        // 5. Validate title
        if (parsed.getStoryboardTitle() == null || parsed.getStoryboardTitle().isBlank()) {
            log.warn("OpenAI storyboard yanıtında storyboardTitle eksik.");
            return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                    "AI yanıtında storyboard başlığı (storyboardTitle) eksik.", true, providerType, modelName);
        }

        // 6. Validate scenes present
        if (parsed.getScenes() == null || parsed.getScenes().isEmpty()) {
            log.warn("OpenAI storyboard yanıtında scenes eksik.");
            return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                    "AI yanıtında sahne listesi eksik.", true, providerType, modelName);
        }

        // 7. Validate and map scenes
        List<StoryboardSceneOutput> sceneOutputs = new ArrayList<>();
        Set<Integer> seenNumbers = new HashSet<>();

        for (OpenAiStoryboardRawResponse.RawScene rawScene : parsed.getScenes()) {
            // sceneNumber zorunlu
            if (rawScene.getSceneNumber() == null || rawScene.getSceneNumber() < 1) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında geçersiz sahne numarası.", true, providerType, modelName);
            }

            // Duplicate scene number kontrolü
            if (!seenNumbers.add(rawScene.getSceneNumber())) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında tekrarlanan sahne numarası: " + rawScene.getSceneNumber(), true, providerType, modelName);
            }

            // visualDescription zorunlu
            if (rawScene.getVisualDescription() == null || rawScene.getVisualDescription().isBlank()) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında sahne " + rawScene.getSceneNumber() + " için visualDescription eksik.", true,
                        providerType, modelName);
            }

            // sourceBeatType tolerant mapping
            ScenarioBeatType beatType = null;
            int beatOrder = 0;
            if (rawScene.getSourceBeatType() != null && !rawScene.getSourceBeatType().isBlank()) {
                beatType = BEAT_TYPE_MAP.get(rawScene.getSourceBeatType().toUpperCase());
                if (beatType != null) {
                    beatOrder = beatType.getOrder();
                }
            }
            if (rawScene.getSourceBeatOrder() != null) {
                beatOrder = rawScene.getSourceBeatOrder();
            }

            sceneOutputs.add(StoryboardSceneOutput.builder()
                    .sceneNumber(rawScene.getSceneNumber())
                    .sourceBeatType(beatType)
                    .sourceBeatOrder(beatOrder)
                    .sceneTitle(rawScene.getSceneTitle())
                    .visualDescription(rawScene.getVisualDescription())
                    .narrationText(rawScene.getNarrationText())
                    .onScreenText(rawScene.getOnScreenText())
                    .cameraDirection(rawScene.getCameraDirection())
                    .transitionNote(rawScene.getTransitionNote())
                    .mood(rawScene.getMood())
                    .location(rawScene.getLocation())
                    .characters(rawScene.getCharacters())
                    .estimatedDurationSeconds(
                            rawScene.getEstimatedDurationSeconds() != null ? rawScene.getEstimatedDurationSeconds() : 0)
                    .mediaGenerationNotes(null) // FP-003 kapsamında media generation yok
                    .build());
        }

        // 8. Scene number'ları sırala
        sceneOutputs.sort(Comparator.comparingInt(StoryboardSceneOutput::getSceneNumber));

        // 9. Ascending order kontrolü
        for (int i = 0; i < sceneOutputs.size(); i++) {
            if (sceneOutputs.get(i).getSceneNumber() != i + 1) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında sahne numaraları sıralı değil (1'den başlamalı, ardışık olmalı).", true,
                        providerType, modelName);
            }
        }

        // 10. Build success response
        return StoryboardAgentResponse.builder()
                .success(true)
                .storyboardTitle(parsed.getStoryboardTitle())
                .summary(parsed.getSummary())
                .targetPlatform(parsed.getTargetPlatform())
                .aspectRatio(parsed.getAspectRatio())
                .estimatedDurationSeconds(parsed.getEstimatedDurationSeconds())
                .visualStyleGuide(parsed.getVisualStyleGuide())
                .narrationStyle(parsed.getNarrationStyle())
                .scenes(sceneOutputs)
                .providerType(providerType)
                .modelName(modelName)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────

    private static StoryboardAgentResponse failureResponse(
            String errorCode, String message, boolean retryable, String providerType, String modelName) {
        return StoryboardAgentResponse.builder()
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
    static class OpenAiStoryboardRawResponse {
        private String storyboardTitle;
        private String summary;
        private String targetPlatform;
        private String aspectRatio;
        private Integer estimatedDurationSeconds;
        private String visualStyleGuide;
        private String narrationStyle;
        private List<RawScene> scenes;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class RawScene {
            private Integer sceneNumber;
            private String sourceBeatType;
            private Integer sourceBeatOrder;
            private String sceneTitle;
            private String visualDescription;
            private String narrationText;
            private String onScreenText;
            private String cameraDirection;
            private String transitionNote;
            private String mood;
            private String location;
            private String characters;
            private Integer estimatedDurationSeconds;
        }
    }
}
