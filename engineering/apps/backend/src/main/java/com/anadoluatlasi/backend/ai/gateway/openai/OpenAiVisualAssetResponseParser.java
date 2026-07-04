package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.VisualAssetPlanAgentResponse;
import com.anadoluatlasi.backend.ai.contract.VisualAssetSceneOutput;
import com.anadoluatlasi.backend.ai.contract.VisualAssetThumbnailOutput;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * FP-006 Task 09: OpenAI model çıktısını VisualAssetPlanAgentResponse'a dönüştüren parser.
 *
 * <p>Provider-internal sınıftır. REST'e expose edilmez.
 * Raw model çıktısı dışarıya sızdırılmaz.</p>
 *
 * <p>Parser kuralları:</p>
 * <ul>
 *   <li>Strict JSON bekler</li>
 *   <li>Markdown code fence varsa temizler</li>
 *   <li>Her sahne için englishImagePrompt ve turkishVisualContext zorunludur</li>
 *   <li>Sahne sayısı input ile eşleşmelidir</li>
 *   <li>Thumbnail planı zorunludur</li>
 *   <li>Unsafe/refusal içerik tespit eder</li>
 *   <li>Forbidden response field'ları (imageUrl, objectKey, bucket, mediaAssetId, etc.) reddeder</li>
 * </ul>
 */
@Slf4j
class OpenAiVisualAssetResponseParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile(
            "^\\s*```(?:json)?\\s*\\n?(.*?)\\n?\\s*```\\s*$",
            Pattern.DOTALL
    );

    private static final List<String> REFUSAL_SIGNALS = List.of(
            "I cannot", "I can't", "I'm unable", "I am unable",
            "content policy", "safety guidelines",
            "not appropriate", "inappropriate content",
            "Bu isteği yerine getiremem", "Uygun olmayan içerik"
    );

    private static final Set<String> FORBIDDEN_FIELD_SIGNALS = Set.of(
            "imageUrl", "image_url", "filePath", "file_path",
            "bucket", "objectKey", "object_key",
            "mediaAssetId", "media_asset_id",
            "generatedAssetId", "generated_asset_id",
            "publishUrl", "publish_url", "uploadUrl", "upload_url",
            "rawPrompt", "raw_prompt", "internalPrompt", "internal_prompt",
            "providerRequest", "provider_request", "providerResponse", "provider_response",
            "providerPayload", "provider_payload",
            "apiKey", "api_key", "secret"
    );

    /**
     * Raw model text çıktısını parse eder.
     *
     * @param rawContent  raw model çıktısı
     * @param expectedSceneCount beklenen sahne sayısı (input'tan)
     * @param providerType provider tipi
     * @param modelName   model adı
     * @return safe VisualAssetPlanAgentResponse
     */
    static VisualAssetPlanAgentResponse parse(String rawContent, int expectedSceneCount,
                                               String providerType, String modelName) {
        // 1. Empty check
        if (rawContent == null || rawContent.isBlank()) {
            log.warn("OpenAI visual asset yanıtı boş.");
            return failureResponse("OPENAI_VISUAL_ASSET_EMPTY_RESPONSE",
                    "AI yanıtı boş döndü.", true, providerType, modelName);
        }

        // 2. Refusal/unsafe detection
        String trimmed = rawContent.trim();
        if (isRefusalContent(trimmed)) {
            log.warn("OpenAI visual asset yanıtı güvenli olmayan içerik/refusal tespit edildi.");
            return failureResponse("OPENAI_VISUAL_ASSET_UNSAFE_CONTENT",
                    "AI modeli güvenlik nedeniyle yanıt vermeyi reddetti.", false, providerType, modelName);
        }

        // 3. Forbidden field detection in raw content
        String forbiddenField = detectForbiddenField(trimmed);
        if (forbiddenField != null) {
            log.warn("OpenAI visual asset yanıtında yasaklı alan tespit edildi: {}", forbiddenField);
            return failureResponse("OPENAI_VISUAL_ASSET_UNSAFE_CONTENT",
                    "AI yanıtında yasaklı alan tespit edildi: " + forbiddenField, false, providerType, modelName);
        }

        // 4. Strip markdown code fence
        String jsonText = stripCodeFence(trimmed);

        // 5. Parse JSON
        RawVisualAssetPlanResponse parsed;
        try {
            parsed = OBJECT_MAPPER.readValue(jsonText, RawVisualAssetPlanResponse.class);
        } catch (Exception e) {
            log.warn("OpenAI visual asset yanıtı JSON parse hatası: {}", e.getMessage());
            return failureResponse("OPENAI_VISUAL_ASSET_INVALID_JSON",
                    "AI yanıtı geçerli JSON formatında değil.", true, providerType, modelName);
        }

        // 6. Validate scene plans list exists
        if (parsed.getScenePlans() == null || parsed.getScenePlans().isEmpty()) {
            log.warn("OpenAI visual asset yanıtında scenePlans listesi eksik veya boş.");
            return failureResponse("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE",
                    "AI yanıtında sahne plan listesi eksik.", true, providerType, modelName);
        }

        // 7. Validate scene count matches
        if (parsed.getScenePlans().size() != expectedSceneCount) {
            log.warn("OpenAI visual asset sahne sayısı uyumsuz: beklenen={}, gelen={}",
                    expectedSceneCount, parsed.getScenePlans().size());
            return failureResponse("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE",
                    "AI yanıtında sahne sayısı uyumsuz: beklenen=" + expectedSceneCount +
                            ", gelen=" + parsed.getScenePlans().size(), true, providerType, modelName);
        }

        // 8. Validate each scene
        for (int i = 0; i < parsed.getScenePlans().size(); i++) {
            RawVisualAssetPlanResponse.RawScenePlan rawScene = parsed.getScenePlans().get(i);

            if (rawScene.getSceneNumber() == null) {
                return failureResponse("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE",
                        "AI yanıtında sahne " + (i + 1) + " için sceneNumber eksik.", true, providerType, modelName);
            }
            if (isBlank(rawScene.getEnglishImagePrompt())) {
                return failureResponse("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE",
                        "AI yanıtında sahne " + rawScene.getSceneNumber() + " için englishImagePrompt eksik.",
                        true, providerType, modelName);
            }
            if (isBlank(rawScene.getTurkishVisualContext())) {
                return failureResponse("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE",
                        "AI yanıtında sahne " + rawScene.getSceneNumber() + " için turkishVisualContext eksik.",
                        true, providerType, modelName);
            }
        }

        // 9. Validate thumbnail plan
        if (parsed.getThumbnailPlan() == null) {
            log.warn("OpenAI visual asset yanıtında thumbnailPlan eksik.");
            return failureResponse("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE",
                    "AI yanıtında thumbnail planı eksik.", true, providerType, modelName);
        }
        if (isBlank(parsed.getThumbnailPlan().getThumbnailPrompt())) {
            return failureResponse("OPENAI_VISUAL_ASSET_INVALID_STRUCTURE",
                    "AI yanıtında thumbnail için thumbnailPrompt eksik.", true, providerType, modelName);
        }

        // 10. Map to safe output
        List<VisualAssetSceneOutput> sceneOutputs = parsed.getScenePlans().stream()
                .map(OpenAiVisualAssetResponseParser::mapScene)
                .collect(Collectors.toList());

        VisualAssetThumbnailOutput thumbnailOutput = mapThumbnail(parsed.getThumbnailPlan());

        return VisualAssetPlanAgentResponse.builder()
                .success(true)
                .planTitle(parsed.getPlanTitle())
                .overallVisualStyle(parsed.getOverallVisualStyle())
                .aspectRatioStrategy(parsed.getAspectRatioStrategy())
                .culturalAuthenticityGuidance(parsed.getCulturalAuthenticityGuidance())
                .characterContinuityGuidance(parsed.getCharacterContinuityGuidance())
                .locationContinuityGuidance(parsed.getLocationContinuityGuidance())
                .colorPaletteGuidance(parsed.getColorPaletteGuidance())
                .scenePlans(sceneOutputs)
                .thumbnailPlan(thumbnailOutput)
                .providerType(providerType)
                .modelName(modelName)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Private mapping helpers ──────────────────────────────────────

    private static VisualAssetSceneOutput mapScene(RawVisualAssetPlanResponse.RawScenePlan raw) {
        UUID sceneId = null;
        if (raw.getSourceStoryboardSceneId() != null && !raw.getSourceStoryboardSceneId().isBlank()) {
            try {
                sceneId = UUID.fromString(raw.getSourceStoryboardSceneId());
            } catch (IllegalArgumentException e) {
                log.debug("Scene sourceStoryboardSceneId geçersiz UUID: {}", raw.getSourceStoryboardSceneId());
            }
        }

        return VisualAssetSceneOutput.builder()
                .sourceStoryboardSceneId(sceneId)
                .sceneNumber(raw.getSceneNumber())
                .sourceBeatType(raw.getSourceBeatType())
                .sceneTitle(raw.getSceneTitle())
                .englishImagePrompt(raw.getEnglishImagePrompt())
                .turkishVisualContext(raw.getTurkishVisualContext())
                .negativePrompt(raw.getNegativePrompt())
                .styleDirective(raw.getStyleDirective())
                .aspectRatio(raw.getAspectRatio())
                .compositionNotes(raw.getCompositionNotes())
                .cameraFraming(raw.getCameraFraming())
                .lightingNotes(raw.getLightingNotes())
                .colorPalette(raw.getColorPalette())
                .mood(raw.getMood())
                .location(raw.getLocation())
                .characters(raw.getCharacters())
                .culturalDetails(raw.getCulturalDetails())
                .textOverlayGuidance(raw.getTextOverlayGuidance())
                .imageGenerationToolNotes(raw.getImageGenerationToolNotes())
                .safetyNotes(raw.getSafetyNotes())
                .build();
    }

    private static VisualAssetThumbnailOutput mapThumbnail(RawVisualAssetPlanResponse.RawThumbnailPlan raw) {
        UUID sceneId = null;
        if (raw.getSourceStoryboardSceneId() != null && !raw.getSourceStoryboardSceneId().isBlank()) {
            try {
                sceneId = UUID.fromString(raw.getSourceStoryboardSceneId());
            } catch (IllegalArgumentException e) {
                log.debug("Thumbnail sourceStoryboardSceneId geçersiz UUID: {}", raw.getSourceStoryboardSceneId());
            }
        }

        return VisualAssetThumbnailOutput.builder()
                .thumbnailPrompt(raw.getThumbnailPrompt())
                .negativePrompt(raw.getNegativePrompt())
                .textOverlayGuidance(raw.getTextOverlayGuidance())
                .compositionNotes(raw.getCompositionNotes())
                .emotionalHook(raw.getEmotionalHook())
                .aspectRatio(raw.getAspectRatio())
                .styleDirective(raw.getStyleDirective())
                .colorPalette(raw.getColorPalette())
                .sourceSceneNumber(raw.getSourceSceneNumber())
                .sourceStoryboardSceneId(sceneId)
                .build();
    }

    // ── Helper methods ───────────────────────────────────────────────

    private static boolean isRefusalContent(String content) {
        String lower = content.toLowerCase();
        return REFUSAL_SIGNALS.stream().anyMatch(s -> lower.contains(s.toLowerCase()));
    }

    private static String detectForbiddenField(String content) {
        for (String field : FORBIDDEN_FIELD_SIGNALS) {
            if (content.contains("\"" + field + "\"")) {
                return field;
            }
        }
        return null;
    }

    static String stripCodeFence(String text) {
        Matcher matcher = CODE_FENCE_PATTERN.matcher(text);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return text;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static VisualAssetPlanAgentResponse failureResponse(
            String errorCode, String errorMessage, boolean retryable,
            String providerType, String modelName) {
        return VisualAssetPlanAgentResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryable(retryable)
                .providerType(providerType)
                .modelName(modelName)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Internal raw parse DTOs ───────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RawVisualAssetPlanResponse {
        private String planTitle;
        private String overallVisualStyle;
        private String aspectRatioStrategy;
        private String culturalAuthenticityGuidance;
        private String characterContinuityGuidance;
        private String locationContinuityGuidance;
        private String colorPaletteGuidance;
        private List<RawScenePlan> scenePlans;
        private RawThumbnailPlan thumbnailPlan;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class RawScenePlan {
            private String sourceStoryboardSceneId;
            private Integer sceneNumber;
            private String sourceBeatType;
            private String sceneTitle;
            private String englishImagePrompt;
            private String turkishVisualContext;
            private String negativePrompt;
            private String styleDirective;
            private String aspectRatio;
            private String compositionNotes;
            private String cameraFraming;
            private String lightingNotes;
            private String colorPalette;
            private String mood;
            private String location;
            private String characters;
            private String culturalDetails;
            private String textOverlayGuidance;
            private String imageGenerationToolNotes;
            private String safetyNotes;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class RawThumbnailPlan {
            private String thumbnailPrompt;
            private String negativePrompt;
            private String textOverlayGuidance;
            private String compositionNotes;
            private String emotionalHook;
            private String aspectRatio;
            private String styleDirective;
            private String colorPalette;
            private Integer sourceSceneNumber;
            private String sourceStoryboardSceneId;
        }
    }
}
