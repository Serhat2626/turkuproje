package com.anadoluatlasi.backend.ai.gateway.mock;

import com.anadoluatlasi.backend.ai.contract.*;
import com.anadoluatlasi.backend.ai.gateway.AiVisualAssetAgentGateway;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FP-006: Deterministic mock Visual Asset Plan gateway.
 *
 * <p>Harici IO, AI provider çağrısı veya image API çağrısı yapmaz.
 * Aynı input aynı output üretir (deterministik).</p>
 *
 * <p>Test ve local smoke test için kullanılır.
 * providerType = "MOCK", modelName = "mock-visual-asset-planner".</p>
 *
 * <p>Image URL, dosya, MediaAsset veya GeneratedAsset üretmez.</p>
 */
@Slf4j
public class MockAiVisualAssetAgentGateway implements AiVisualAssetAgentGateway {

    private static final String PROVIDER_TYPE = "MOCK";
    private static final String MODEL_NAME = "mock-visual-asset-planner";
    private static final String DEFAULT_NEGATIVE_PROMPT =
            "modern logos, low quality, blurry, distorted faces, extra limbs, text artifacts, "
            + "anachronistic objects, plastic, neon lights";

    private volatile MockVisualAssetGatewayMode currentMode;

    public MockAiVisualAssetAgentGateway() {
        this(MockVisualAssetGatewayMode.SUCCESS);
    }

    public MockAiVisualAssetAgentGateway(MockVisualAssetGatewayMode initialMode) {
        this.currentMode = initialMode;
    }

    public void setMode(MockVisualAssetGatewayMode mode) {
        this.currentMode = mode;
        log.info("Mock Visual Asset gateway mode changed to: {}", mode);
    }

    public MockVisualAssetGatewayMode getMode() {
        return currentMode;
    }

    @Override
    public VisualAssetPlanAgentResponse generateVisualAssetPlan(VisualAssetPlanAgentRequest request) {
        log.info("Mock Visual Asset gateway called with mode: {}, storyboardDraftId: {}",
                currentMode, request != null ? request.getStoryboardDraftId() : "null");

        return switch (currentMode) {
            case SUCCESS -> handleSuccess(request);
            case TIMEOUT -> handleTimeout(request);
            case EMPTY_RESPONSE -> handleEmptyResponse(request);
            case INVALID_FORMAT -> handleInvalidFormat(request);
            case UNSAFE_CONTENT -> handleUnsafeContent(request);
        };
    }

    // ── Success Handler ──────────────────────────────────────────────

    private VisualAssetPlanAgentResponse handleSuccess(VisualAssetPlanAgentRequest request) {
        if (request == null || request.getScenes() == null || request.getScenes().isEmpty()) {
            return buildFailureResponse(
                    "EMPTY_INPUT_SCENES",
                    "Request must include at least one scene",
                    false
            );
        }

        List<VisualAssetSceneOutput> scenePlans = new ArrayList<>();
        for (VisualAssetSceneInput scene : request.getScenes()) {
            scenePlans.add(buildSceneOutput(scene, request));
        }

        // Thumbnail references first scene (hook) by default
        VisualAssetSceneInput hookScene = request.getScenes().get(0);
        VisualAssetThumbnailOutput thumbnailPlan = buildThumbnailOutput(request, hookScene);

        String planTitle = (request.getTitle() != null ? request.getTitle() : "Untitled")
                + " — Görsel Varlık Planı";

        return VisualAssetPlanAgentResponse.builder()
                .success(true)
                .planTitle(planTitle)
                .overallVisualStyle("Cinematic, warm-toned Anatolian visual style with "
                        + "natural lighting and earth-tone color palette")
                .aspectRatioStrategy(request.getAspectRatio() != null
                        ? request.getAspectRatio() + " primary"
                        : "16:9 primary")
                .culturalAuthenticityGuidance("Anadolu kültürel bağlamı doğru yansıtılmalı: "
                        + "dönem kıyafetleri, geleneksel mimari, doğal malzemeler. "
                        + "Anakronistik nesnelerden kaçınılmalı.")
                .characterContinuityGuidance("Karakterler sahneler arası tutarlı fiziksel "
                        + "özelliklerde olmalı: yüz, saç, kıyafet, beden yapısı")
                .locationContinuityGuidance("Mekanlar tutarlı mimari ve coğrafi detaylarla "
                        + "gösterilmeli: aynı köy, aynı ev, aynı manzara")
                .colorPaletteGuidance("Warm earth tones: ochre, terracotta, olive green, "
                        + "golden amber. Cool accents for emotional contrast.")
                .scenePlans(scenePlans)
                .thumbnailPlan(thumbnailPlan)
                .warnings(Collections.emptyList())
                .providerType(PROVIDER_TYPE)
                .modelName(MODEL_NAME)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private VisualAssetSceneOutput buildSceneOutput(VisualAssetSceneInput scene,
                                                     VisualAssetPlanAgentRequest request) {
        String title = scene.getSceneTitle() != null ? scene.getSceneTitle() : "Scene " + scene.getSceneNumber();
        String visual = scene.getVisualDescription() != null ? scene.getVisualDescription() : title;
        String moodVal = scene.getMood() != null ? scene.getMood() : "nostalgic";
        String locationVal = scene.getLocation() != null ? scene.getLocation() : "Anatolian landscape";
        String charactersVal = scene.getCharacters() != null ? scene.getCharacters() : "";
        String cameraVal = scene.getCameraDirection() != null ? scene.getCameraDirection() : "medium shot";

        // Build realistic English image prompt from source data
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("A cinematic ").append(cameraVal).append(" of ");
        promptBuilder.append(visual).append(". ");
        if (!locationVal.isEmpty()) {
            promptBuilder.append("Set in ").append(locationVal).append(". ");
        }
        if (!charactersVal.isEmpty()) {
            promptBuilder.append("Featuring ").append(charactersVal).append(". ");
        }
        promptBuilder.append("Mood: ").append(moodVal).append(". ");
        promptBuilder.append("Style: cinematic, warm Anatolian lighting, photorealistic.");

        // Build Turkish visual context
        String turkishContext = title + " — " + visual
                + ". Atmosfer: " + moodVal
                + ". Mekan: " + locationVal + ".";

        return VisualAssetSceneOutput.builder()
                .sourceStoryboardSceneId(scene.getStoryboardSceneId())
                .sceneNumber(scene.getSceneNumber())
                .sourceBeatType(scene.getSourceBeatType())
                .sceneTitle(title)
                .englishImagePrompt(promptBuilder.toString())
                .turkishVisualContext(turkishContext)
                .negativePrompt(DEFAULT_NEGATIVE_PROMPT)
                .styleDirective("cinematic")
                .aspectRatio(request.getAspectRatio() != null ? request.getAspectRatio() : "16:9")
                .compositionNotes("Rule of thirds, " + cameraVal + ", natural framing")
                .cameraFraming(cameraVal)
                .lightingNotes("Golden hour, warm natural light, soft shadows")
                .colorPalette("Warm earth tones: ochre, terracotta, olive, amber")
                .mood(moodVal)
                .location(locationVal)
                .characters(charactersVal)
                .culturalDetails("Anadolu kültürel bağlamı — dönemine uygun kıyafet ve mimari")
                .textOverlayGuidance("No text overlay for this scene")
                .imageGenerationToolNotes("Optimized for DALL-E 3 or Stable Diffusion XL")
                .safetyNotes("Production-safe content, no graphic imagery")
                .build();
    }

    private VisualAssetThumbnailOutput buildThumbnailOutput(VisualAssetPlanAgentRequest request,
                                                             VisualAssetSceneInput hookScene) {
        String title = request.getTitle() != null ? request.getTitle() : "Untitled";
        String emotionalTone = request.getThumbnailContext() != null
                && request.getThumbnailContext().getEmotionalTone() != null
                ? request.getThumbnailContext().getEmotionalTone()
                : "merak ve nostalji";

        return VisualAssetThumbnailOutput.builder()
                .thumbnailPrompt("A dramatic close-up portrait for \"" + title + "\". "
                        + "Cinematic composition, eye contact with viewer, warm Anatolian lighting. "
                        + "Emotional tone: " + emotionalTone + ". "
                        + "High visual impact, YouTube-optimized.")
                .negativePrompt(DEFAULT_NEGATIVE_PROMPT)
                .textOverlayGuidance("Bold title text centered top, max 5 words, "
                        + "high contrast against background")
                .compositionNotes("Face or subject close-up, shallow depth of field, "
                        + "blurred background, eye level")
                .emotionalHook(emotionalTone)
                .aspectRatio("16:9")
                .styleDirective("cinematic portrait")
                .colorPalette("Warm tones with dark contrast for text readability")
                .sourceSceneNumber(hookScene.getSceneNumber())
                .sourceStoryboardSceneId(hookScene.getStoryboardSceneId())
                .build();
    }

    // ── Failure Handlers ─────────────────────────────────────────────

    private VisualAssetPlanAgentResponse handleTimeout(VisualAssetPlanAgentRequest request) {
        log.warn("Mock Visual Asset gateway simulating TIMEOUT");
        return buildFailureResponse("AI_PROVIDER_TIMEOUT",
                "AI provider did not respond within the expected timeframe", true);
    }

    private VisualAssetPlanAgentResponse handleEmptyResponse(VisualAssetPlanAgentRequest request) {
        log.warn("Mock Visual Asset gateway simulating EMPTY_RESPONSE");
        return buildFailureResponse("EMPTY_AI_RESPONSE",
                "AI provider returned an empty response", true);
    }

    private VisualAssetPlanAgentResponse handleInvalidFormat(VisualAssetPlanAgentRequest request) {
        log.warn("Mock Visual Asset gateway simulating INVALID_FORMAT");
        return buildFailureResponse("INVALID_AI_RESPONSE_FORMAT",
                "AI provider response could not be parsed into expected format", true);
    }

    private VisualAssetPlanAgentResponse handleUnsafeContent(VisualAssetPlanAgentRequest request) {
        log.warn("Mock Visual Asset gateway simulating UNSAFE_CONTENT");
        return buildFailureResponse("AI_RESPONSE_UNSAFE_CONTENT",
                "AI provider response contained content that did not pass safety filters", false);
    }

    private VisualAssetPlanAgentResponse buildFailureResponse(String errorCode,
                                                               String errorMessage,
                                                               boolean retryable) {
        return VisualAssetPlanAgentResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryable(retryable)
                .providerType(PROVIDER_TYPE)
                .modelName(MODEL_NAME)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
