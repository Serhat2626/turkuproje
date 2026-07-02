package com.anadoluatlasi.backend.ai.gateway.mock;

import com.anadoluatlasi.backend.ai.contract.StoryboardAgentRequest;
import com.anadoluatlasi.backend.ai.contract.StoryboardAgentResponse;
import com.anadoluatlasi.backend.ai.contract.StoryboardSceneOutput;
import com.anadoluatlasi.backend.ai.contract.StoryboardSourceBeatInput;
import com.anadoluatlasi.backend.ai.gateway.AiStoryboardAgentGateway;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FP-003: Mock AI Storyboard Agent Gateway implementasyonu.
 *
 * <p>Gerçek AI provider'a bağlanmadan storyboard workflow'unu test edilebilir hale getirir.
 * Database erişimi yapmaz, status güncellemez, draft oluşturmaz.
 * Sadece StoryboardAgentRequest → StoryboardAgentResponse dönüşümü yapar.</p>
 *
 * <p>Desteklenen senaryolar:</p>
 * <ul>
 *   <li>SUCCESS — deterministic başarılı scene yanıtı</li>
 *   <li>TIMEOUT — provider timeout hatası (retryable)</li>
 *   <li>EMPTY_RESPONSE — boş yanıt hatası (retryable)</li>
 *   <li>INVALID_FORMAT — geçersiz format hatası (retryable)</li>
 *   <li>UNSAFE_CONTENT — güvenli olmayan içerik hatası (non-retryable)</li>
 * </ul>
 *
 * <p>Bu gateway gerçek media dosyası (image/video/audio) üretmez.
 * mediaGenerationNotes yalnızca gelecek handoff metadata'sıdır.</p>
 */
@Slf4j
public class MockAiStoryboardAgentGateway implements AiStoryboardAgentGateway {

    private volatile MockStoryboardGatewayMode currentMode;

    public MockAiStoryboardAgentGateway() {
        this(MockStoryboardGatewayMode.SUCCESS);
    }

    public MockAiStoryboardAgentGateway(MockStoryboardGatewayMode initialMode) {
        this.currentMode = initialMode;
    }

    /**
     * Aktif modu değiştirir. Test/geliştirme amacıyla kullanılır.
     */
    public void setMode(MockStoryboardGatewayMode mode) {
        log.info("Mock Storyboard AI mod değiştirildi: {} → {}", this.currentMode, mode);
        this.currentMode = mode;
    }

    public MockStoryboardGatewayMode getMode() {
        return currentMode;
    }

    @Override
    public StoryboardAgentResponse generateStoryboard(StoryboardAgentRequest request) {
        log.info("Mock Storyboard AI çağrısı: requestId={}, mod={}", request.getRequestId(), currentMode);

        return switch (currentMode) {
            case SUCCESS -> handleSuccess(request);
            case TIMEOUT -> handleTimeout(request);
            case EMPTY_RESPONSE -> handleEmptyResponse(request);
            case INVALID_FORMAT -> handleInvalidFormat(request);
            case UNSAFE_CONTENT -> handleUnsafeContent(request);
        };
    }

    // ── SUCCESS ────────────────────────────────────────────

    private StoryboardAgentResponse handleSuccess(StoryboardAgentRequest request) {
        String subjectTitle = request.getScenarioTitle() != null
                ? request.getScenarioTitle()
                : "Bilinmeyen Konu";

        String storyboardTitle = subjectTitle + " — Storyboard";

        // Beat başına bir scene üret (desiredSceneCount override varsa kullan)
        List<StoryboardSceneOutput> scenes = new ArrayList<>();
        List<StoryboardSourceBeatInput> beats = request.getScenarioBeats();

        if (beats != null && !beats.isEmpty()) {
            int sceneNumber = 1;
            for (StoryboardSourceBeatInput beat : beats) {
                scenes.add(buildSceneForBeat(beat, sceneNumber, subjectTitle));
                sceneNumber++;
            }
        }

        int totalDuration = scenes.stream()
                .mapToInt(StoryboardSceneOutput::getEstimatedDurationSeconds)
                .sum();

        String summary = String.format("%s hakkında sahne bazlı görsel storyboard.", subjectTitle);

        log.info("Mock Storyboard AI başarılı yanıt: requestId={}, scenes={}",
                request.getRequestId(), scenes.size());

        return StoryboardAgentResponse.builder()
                .success(true)
                .storyboardTitle(storyboardTitle)
                .summary(summary)
                .targetPlatform(request.getTargetPlatform() != null ? request.getTargetPlatform() : "YouTube")
                .aspectRatio(request.getAspectRatio() != null ? request.getAspectRatio() : "16:9")
                .estimatedDurationSeconds(totalDuration)
                .scenes(scenes)
                .visualStyleGuide("Tarihi dramatik tarz, sıcak renk paleti, sinematik compositing")
                .narrationStyle("Hikâye anlatıcı, sakin ve etkileyici ton")
                .warnings(List.of())
                .qualityNotes("Mock AI tarafından üretildi. Gerçek AI entegrasyonunda bu alan kalite notları içerecek.")
                .providerType("MOCK")
                .modelName("mock-storyboard-v1")
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private StoryboardSceneOutput buildSceneForBeat(
            StoryboardSourceBeatInput beat, int sceneNumber, String subjectTitle) {

        String beatLabel = beat.getBeatType() != null ? beat.getBeatType().getLabel() : "Sahne";

        return StoryboardSceneOutput.builder()
                .sceneNumber(sceneNumber)
                .sourceBeatType(beat.getBeatType())
                .sourceBeatOrder(beat.getOrderIndex())
                .sceneTitle(String.format("%s — %s", beatLabel, subjectTitle))
                .visualDescription(String.format(
                        "Sahne %d: %s bölümü için görsel açıklama. %s konusuyla ilgili etkileyici görüntü.",
                        sceneNumber, beatLabel, subjectTitle))
                .narrationText(String.format(
                        "%s bölümünün narration metni. %s",
                        beatLabel, beat.getBeatBody() != null ? beat.getBeatBody().substring(0, Math.min(100, beat.getBeatBody().length())) + "..." : ""))
                .onScreenText(sceneNumber == 1 ? subjectTitle : null)
                .cameraDirection(getCameraDirectionForScene(sceneNumber))
                .transitionNote(getTransitionForScene(sceneNumber))
                .mood(getMoodForBeat(beat))
                .location(subjectTitle + " — tarihi mekan")
                .characters("Anlatıcı")
                .estimatedDurationSeconds(beat.getEstimatedDurationSeconds() != null
                        ? beat.getEstimatedDurationSeconds() : 30)
                .mediaGenerationNotes(String.format(
                        "Sahne %d: %s beat'i için media üretim notu (gelecek handoff)", sceneNumber, beatLabel))
                .build();
    }

    private String getCameraDirectionForScene(int sceneNumber) {
        return switch (sceneNumber) {
            case 1 -> "Drone wide shot → slow zoom in";
            case 2 -> "Medium shot, yavaş pan";
            case 3 -> "Close-up detaylar, slow motion";
            case 4 -> "Dramatik açı, düşük kamera";
            case 5 -> "Wide establishing shot";
            case 6 -> "Medium close-up, doğrudan bakış";
            default -> "Standard medium shot";
        };
    }

    private String getTransitionForScene(int sceneNumber) {
        return switch (sceneNumber) {
            case 1 -> "fade in";
            case 6 -> "fade out";
            default -> "dissolve";
        };
    }

    private String getMoodForBeat(StoryboardSourceBeatInput beat) {
        if (beat.getBeatType() == null) return "Nötr";
        return switch (beat.getBeatType()) {
            case HOOK -> "Merak uyandıran, gerilimli";
            case INTRODUCTION -> "Açıklayıcı, sıcak";
            case MAIN_NARRATIVE -> "Derinlikli, anlatımcı";
            case EMOTIONAL_CLIMAX -> "Duygusal, dramatik";
            case CONCLUSION -> "Huzurlu, düşündürücü";
            case CALL_TO_ACTION -> "Enerjik, davet eden";
        };
    }

    // ── TIMEOUT ────────────────────────────────────────────

    private StoryboardAgentResponse handleTimeout(StoryboardAgentRequest request) {
        log.warn("Mock Storyboard AI timeout simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("AI_PROVIDER_TIMEOUT",
                "Storyboard üretimi zaman aşımına uğradı. (Mock simülasyon)", true);
    }

    // ── EMPTY_RESPONSE ─────────────────────────────────────

    private StoryboardAgentResponse handleEmptyResponse(StoryboardAgentRequest request) {
        log.warn("Mock Storyboard AI boş yanıt simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("EMPTY_AI_RESPONSE",
                "Storyboard üretilemedi — boş yanıt. (Mock simülasyon)", true);
    }

    // ── INVALID_FORMAT ─────────────────────────────────────

    private StoryboardAgentResponse handleInvalidFormat(StoryboardAgentRequest request) {
        log.warn("Mock Storyboard AI geçersiz format simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("INVALID_AI_RESPONSE_FORMAT",
                "Storyboard geçersiz formatta üretildi. (Mock simülasyon)", true);
    }

    // ── UNSAFE_CONTENT ─────────────────────────────────────

    private StoryboardAgentResponse handleUnsafeContent(StoryboardAgentRequest request) {
        log.warn("Mock Storyboard AI güvenli olmayan içerik simülasyonu: requestId={}", request.getRequestId());
        return buildFailureResponse("AI_RESPONSE_UNSAFE_CONTENT",
                "Storyboard güvenlik kontrolünden geçemedi. (Mock simülasyon)", false);
    }

    // ── Failure Helper ─────────────────────────────────────

    private StoryboardAgentResponse buildFailureResponse(String errorCode, String errorMessage, boolean retryable) {
        return StoryboardAgentResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .retryable(retryable)
                .providerType("MOCK")
                .modelName("mock-storyboard-v1")
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
