package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentResponse;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageOutput;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageSceneUsageOutput;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
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
 * FP-004 Task 09: OpenAI model çıktısını PlatformPackageAgentResponse'a dönüştüren parser.
 *
 * <p>Provider-internal sınıftır. REST'e expose edilmez.
 * Raw model çıktısı dışarıya sızdırılmaz.</p>
 *
 * <p>Parser kuralları:</p>
 * <ul>
 *   <li>Strict JSON bekler</li>
 *   <li>Markdown code fence varsa temizler</li>
 *   <li>Her platform için tam bir paket zorunludur</li>
 *   <li>Duplicate package type reddedilir</li>
 *   <li>AI-supplied status/APPROVED/READY_FOR_PUBLISH güvenilmez</li>
 *   <li>Unsafe/refusal içerik tespit eder</li>
 * </ul>
 */
@Slf4j
class OpenAiPlatformPackageResponseParser {

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

    private static final Map<String, PlatformPackageType> PACKAGE_TYPE_MAP = Map.of(
            "YOUTUBE_LONG_FORM", PlatformPackageType.YOUTUBE_LONG_FORM,
            "YOUTUBE_SHORTS", PlatformPackageType.YOUTUBE_SHORTS,
            "INSTAGRAM_REELS", PlatformPackageType.INSTAGRAM_REELS,
            "FACEBOOK_REELS", PlatformPackageType.FACEBOOK_REELS
    );

    private static final Set<PlatformPackageType> LONG_FORM_TYPES = Set.of(PlatformPackageType.YOUTUBE_LONG_FORM);
    private static final Set<PlatformPackageType> SHORT_FORM_TYPES = Set.of(
            PlatformPackageType.YOUTUBE_SHORTS,
            PlatformPackageType.INSTAGRAM_REELS,
            PlatformPackageType.FACEBOOK_REELS
    );

    /**
     * Raw model text çıktısını parse eder.
     */
    static PlatformPackageAgentResponse parse(String rawContent, String providerType, String modelName) {
        // 1. Empty check
        if (rawContent == null || rawContent.isBlank()) {
            log.warn("OpenAI platform package yanıtı boş.");
            return failureResponse("EMPTY_AI_RESPONSE", "AI yanıtı boş döndü.", true, providerType, modelName);
        }

        // 2. Refusal/unsafe detection
        String trimmed = rawContent.trim();
        if (isRefusalContent(trimmed)) {
            log.warn("OpenAI platform package yanıtı güvenli olmayan içerik/refusal tespit edildi.");
            return failureResponse("AI_RESPONSE_UNSAFE_CONTENT",
                    "AI modeli güvenlik nedeniyle yanıt vermeyi reddetti.", false, providerType, modelName);
        }

        // 3. Strip markdown code fence
        String jsonText = stripCodeFence(trimmed);

        // 4. Parse JSON
        RawPlatformPackageResponse parsed;
        try {
            parsed = OBJECT_MAPPER.readValue(jsonText, RawPlatformPackageResponse.class);
        } catch (Exception e) {
            log.warn("OpenAI platform package yanıtı JSON parse hatası: {}", e.getMessage());
            return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                    "AI yanıtı geçerli JSON formatında değil.", true, providerType, modelName);
        }

        // 5. Validate packages list exists
        if (parsed.getPackages() == null || parsed.getPackages().isEmpty()) {
            log.warn("OpenAI platform package yanıtında packages listesi eksik veya boş.");
            return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                    "AI yanıtında platform paket listesi eksik.", true, providerType, modelName);
        }

        // 6. Validate and map packages
        List<PlatformPackageOutput> outputs = new ArrayList<>();
        Set<String> seenTypes = new HashSet<>();

        for (RawPlatformPackageResponse.RawPackage rawPkg : parsed.getPackages()) {
            // Package type zorunlu
            if (rawPkg.getPackageType() == null || rawPkg.getPackageType().isBlank()) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında paket tipi (packageType) eksik.", true, providerType, modelName);
            }

            // Package type mapping
            PlatformPackageType pkgType = PACKAGE_TYPE_MAP.get(rawPkg.getPackageType().toUpperCase());
            if (pkgType == null) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında bilinmeyen paket tipi: " + rawPkg.getPackageType(), true, providerType, modelName);
            }

            // Duplicate check
            if (!seenTypes.add(pkgType.name())) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında tekrarlanan paket tipi: " + pkgType.name(), true, providerType, modelName);
            }

            // AI-supplied status validation — reject APPROVED/READY_FOR_PUBLISH
            if (rawPkg.getStatus() != null) {
                String status = rawPkg.getStatus().toUpperCase();
                if ("APPROVED".equals(status) || "READY_FOR_PUBLISH".equals(status)) {
                    log.warn("AI yanıtında güvensiz status tespit edildi: {} — reddediliyor.", status);
                    return failureResponse("AI_RESPONSE_UNSAFE_CONTENT",
                            "AI yanıtında güvensiz status tespit edildi: " + status, false, providerType, modelName);
                }
            }

            // Common field validations
            if (isBlank(rawPkg.getTitle())) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında " + pkgType.name() + " paketi için title eksik.", true, providerType, modelName);
            }
            if (isBlank(rawPkg.getCaptionOrDescription())) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında " + pkgType.name() + " paketi için captionOrDescription eksik.", true, providerType, modelName);
            }
            if (isBlank(rawPkg.getCallToAction())) {
                return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                        "AI yanıtında " + pkgType.name() + " paketi için callToAction eksik.", true, providerType, modelName);
            }

            // YouTube Long-Form specific validations
            if (LONG_FORM_TYPES.contains(pkgType)) {
                if (isBlank(rawPkg.getVideoTitle())) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için videoTitle eksik.", true, providerType, modelName);
                }
                if (isBlank(rawPkg.getLongDescription())) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için longDescription eksik.", true, providerType, modelName);
                }
                if (rawPkg.getChapterOutline() == null || rawPkg.getChapterOutline().isEmpty()) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için chapterOutline eksik.", true, providerType, modelName);
                }
                if (isBlank(rawPkg.getPinnedCommentSuggestion())) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için pinnedCommentSuggestion eksik.", true, providerType, modelName);
                }
                if (isBlank(rawPkg.getThumbnailTextSuggestion())) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için thumbnailTextSuggestion eksik.", true, providerType, modelName);
                }
            }

            // Short-Form / Reels specific validations
            if (SHORT_FORM_TYPES.contains(pkgType)) {
                if (isBlank(rawPkg.getShortHook())) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için shortHook eksik.", true, providerType, modelName);
                }
                if (isBlank(rawPkg.getShortCaption())) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için shortCaption eksik.", true, providerType, modelName);
                }
                if (rawPkg.getOverlayTextSuggestions() == null || rawPkg.getOverlayTextSuggestions().isEmpty()) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için overlayTextSuggestions eksik.", true, providerType, modelName);
                }
                if (rawPkg.getShortHashtagSet() == null || rawPkg.getShortHashtagSet().isEmpty()) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için shortHashtagSet eksik.", true, providerType, modelName);
                }
                if (isBlank(rawPkg.getShortCta())) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için shortCta eksik.", true, providerType, modelName);
                }
                if (isBlank(rawPkg.getDurationGuidance())) {
                    return failureResponse("INVALID_AI_RESPONSE_FORMAT",
                            "AI yanıtında " + pkgType.name() + " paketi için durationGuidance eksik.", true, providerType, modelName);
                }
            }

            // Map scene usages
            List<PlatformPackageSceneUsageOutput> sceneUsages = null;
            if (rawPkg.getSceneUsages() != null) {
                sceneUsages = rawPkg.getSceneUsages().stream()
                        .map(su -> PlatformPackageSceneUsageOutput.builder()
                                .sceneNumber(su.getSceneNumber() != null ? su.getSceneNumber() : 0)
                                .usageType(normalizeUsageType(su.getUsageType()))
                                .included(su.getIncluded() != null ? su.getIncluded() : true)
                                .adaptationNote(su.getAdaptationNote())
                                .build())
                        .collect(Collectors.toList());
            }

            // Build output
            outputs.add(PlatformPackageOutput.builder()
                    .packageType(pkgType.name())
                    .title(rawPkg.getTitle())
                    .captionOrDescription(rawPkg.getCaptionOrDescription())
                    .hashtags(rawPkg.getHashtags())
                    .callToAction(rawPkg.getCallToAction())
                    .targetDurationSeconds(rawPkg.getTargetDurationSeconds())
                    .aspectRatio(rawPkg.getAspectRatio())
                    .contentStructureNotes(rawPkg.getContentStructureNotes())
                    .sceneUsageNotes(rawPkg.getSceneUsageNotes())
                    .videoTitle(rawPkg.getVideoTitle())
                    .longDescription(rawPkg.getLongDescription())
                    .chapterOutline(rawPkg.getChapterOutline())
                    .pinnedCommentSuggestion(rawPkg.getPinnedCommentSuggestion())
                    .thumbnailTextSuggestion(rawPkg.getThumbnailTextSuggestion())
                    .tags(rawPkg.getTags())
                    .shortHook(rawPkg.getShortHook())
                    .shortCaption(rawPkg.getShortCaption())
                    .overlayTextSuggestions(rawPkg.getOverlayTextSuggestions())
                    .shortHashtagSet(rawPkg.getShortHashtagSet())
                    .shortCta(rawPkg.getShortCta())
                    .durationGuidance(rawPkg.getDurationGuidance())
                    .sceneUsages(sceneUsages)
                    .build());
        }

        // 7. Build success response
        return PlatformPackageAgentResponse.builder()
                .success(true)
                .packages(outputs)
                .providerType(providerType)
                .modelName(modelName)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────

    private static PlatformPackageAgentResponse failureResponse(
            String errorCode, String message, boolean retryable, String providerType, String modelName) {
        return PlatformPackageAgentResponse.builder()
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeUsageType(String raw) {
        if (raw == null || raw.isBlank()) return "PRIMARY";
        String upper = raw.toUpperCase();
        return switch (upper) {
            case "PRIMARY", "ADAPTED", "PARTIAL", "OMITTED" -> upper;
            default -> "PRIMARY";
        };
    }

    // ── Internal Raw Response DTO ────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RawPlatformPackageResponse {
        private List<RawPackage> packages;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class RawPackage {
            private String packageType;
            private String status; // AI-supplied — validated and rejected if unsafe
            private String title;
            private String captionOrDescription;
            private List<String> hashtags;
            private String callToAction;
            private Integer targetDurationSeconds;
            private String aspectRatio;
            private String contentStructureNotes;
            private String sceneUsageNotes;
            // YouTube Long-Form
            private String videoTitle;
            private String longDescription;
            private List<String> chapterOutline;
            private String pinnedCommentSuggestion;
            private String thumbnailTextSuggestion;
            private List<String> tags;
            // Short-Form / Reels
            private String shortHook;
            private String shortCaption;
            private List<String> overlayTextSuggestions;
            private List<String> shortHashtagSet;
            private String shortCta;
            private String durationGuidance;
            // Scene usages
            private List<RawSceneUsage> sceneUsages;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class RawSceneUsage {
            private Integer sceneNumber;
            private String usageType;
            private Boolean included;
            private String adaptationNote;
        }
    }
}
