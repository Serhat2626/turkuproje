package com.anadoluatlasi.backend.application.visualasset;

import com.anadoluatlasi.backend.ai.contract.*;
import com.anadoluatlasi.backend.ai.gateway.AiVisualAssetAgentGateway;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import com.anadoluatlasi.backend.storyboard.repository.StoryboardDraftRepository;
import com.anadoluatlasi.backend.visualasset.entity.*;
import com.anadoluatlasi.backend.visualasset.repository.VisualAssetPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FP-006: Visual Asset Plan generation service.
 *
 * <p>APPROVED StoryboardDraft'tan görsel varlık planı üretir.
 * AI gateway'i çağırır, yanıtı validate eder, aggregate'i persist eder.</p>
 *
 * <p>Bu service yalnızca metin prompt planı üretir ve persist eder.
 * Image generation API çağırmaz, görsel dosya üretmez,
 * MediaAsset/GeneratedAsset oluşturmaz.</p>
 *
 * <p>Üretilen plan status = AI_GENERATED. Auto-approve yapılmaz.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisualAssetPlanGenerationService {

    private final StoryboardDraftRepository storyboardDraftRepository;
    private final VisualAssetPlanRepository visualAssetPlanRepository;
    private final AiVisualAssetAgentGateway aiVisualAssetAgentGateway;

    /**
     * APPROVED StoryboardDraft'tan görsel varlık planı üretir.
     *
     * @param storyboardDraftId kaynak storyboard ID
     * @param requestedByAdminId istekte bulunan admin
     * @return üretim sonucu
     * @throws NotFoundException storyboard bulunamazsa
     * @throws BusinessException storyboard APPROVED değilse
     * @throws ValidationException storyboard'da sahne yoksa veya AI yanıtı geçersizse
     */
    @Transactional
    public VisualAssetPlanGenerationResult generateVisualAssetPlan(
            UUID storyboardDraftId, String requestedByAdminId) {

        log.info("FP-006: Görsel varlık planı üretimi başlıyor. StoryboardDraft ID: {}, Admin: {}",
                storyboardDraftId, requestedByAdminId);

        // 1. Source entity yükleme
        StoryboardDraft storyboard = storyboardDraftRepository.findById(storyboardDraftId)
                .orElseThrow(() -> new NotFoundException("StoryboardDraft", storyboardDraftId));

        // 2. APPROVED status kontrolü
        validateApprovedStoryboard(storyboard);

        // 3. Kaynak referanslar
        ContentProject project = storyboard.getContentProject();

        // 4. Sahne validasyonu
        List<StoryboardScene> orderedScenes = getOrderedScenes(storyboard);
        validateScenesNotEmpty(storyboard, orderedScenes);

        // 5. Versiyon hesaplama
        int nextVersion = visualAssetPlanRepository
                .findMaxVersionBySourceStoryboardDraftId(storyboard.getId()) + 1;

        // 6. AI agent isteği oluşturma
        VisualAssetPlanAgentRequest agentRequest = buildAgentRequest(
                storyboard, project, orderedScenes, requestedByAdminId);

        // 7. Gateway çağrısı
        VisualAssetPlanAgentResponse gatewayResponse;
        try {
            gatewayResponse = aiVisualAssetAgentGateway.generateVisualAssetPlan(agentRequest);
        } catch (Exception ex) {
            log.error("FP-006: AI gateway çağrısında beklenmeyen hata. StoryboardDraft ID: {}",
                    storyboardDraftId, ex);
            throw new BusinessException(
                    "Görsel varlık planı üretimi sırasında AI servisi hatası oluştu.");
        }

        // 8. Gateway başarı/hata kontrolü
        if (!gatewayResponse.isSuccess()) {
            return handleGatewayFailure(storyboard, project, nextVersion, gatewayResponse);
        }

        // 9. Yanıt validasyonu
        validateGatewayResponse(gatewayResponse, orderedScenes.size());

        // 10. Aggregate oluşturma ve persist
        return handleSuccess(storyboard, project, orderedScenes, nextVersion,
                gatewayResponse, requestedByAdminId);
    }

    // ── Source Validation ─────────────────────────────────────────────

    private void validateApprovedStoryboard(StoryboardDraft storyboard) {
        if (storyboard.getStatus() != StoryboardDraftStatus.APPROVED) {
            throw new BusinessException(String.format(
                    "Görsel varlık planı üretimi için onaylanmış (APPROVED) bir storyboard gereklidir. "
                    + "Mevcut durum: %s, StoryboardDraft ID: %s",
                    storyboard.getStatus(), storyboard.getId()));
        }
    }

    private List<StoryboardScene> getOrderedScenes(StoryboardDraft storyboard) {
        // Sahne listesini force-initialize ediyoruz (lazy loading)
        List<StoryboardScene> scenes = storyboard.getScenes();
        if (scenes == null) {
            return Collections.emptyList();
        }
        return scenes.stream()
                .sorted(Comparator.comparingInt(StoryboardScene::getSceneNumber))
                .collect(Collectors.toList());
    }

    private void validateScenesNotEmpty(StoryboardDraft storyboard, List<StoryboardScene> scenes) {
        if (scenes.isEmpty()) {
            throw new ValidationException(String.format(
                    "Görsel varlık planı üretimi için storyboard en az bir sahne içermelidir. "
                    + "StoryboardDraft ID: %s", storyboard.getId()));
        }
    }

    // ── Request Building ──────────────────────────────────────────────

    private VisualAssetPlanAgentRequest buildAgentRequest(
            StoryboardDraft storyboard,
            ContentProject project,
            List<StoryboardScene> orderedScenes,
            String requestedByAdminId) {

        List<VisualAssetSceneInput> sceneInputs = orderedScenes.stream()
                .map(this::mapSceneToInput)
                .collect(Collectors.toList());

        VisualAssetThumbnailInput thumbnailInput = VisualAssetThumbnailInput.builder()
                .title(storyboard.getTitle())
                .storyHook(storyboard.getSummary())
                .preferredAspectRatio(storyboard.getAspectRatio() != null
                        ? storyboard.getAspectRatio() : "16:9")
                .emotionalTone("merak ve nostalji")
                .textOverlayPolicy("bold title, max 5 words")
                .build();

        return VisualAssetPlanAgentRequest.builder()
                .requestId(UUID.randomUUID())
                .storyboardDraftId(storyboard.getId())
                .contentProjectId(project.getId())
                .title(storyboard.getTitle())
                .summary(storyboard.getSummary())
                .targetPlatform(storyboard.getTargetPlatform())
                .aspectRatio(storyboard.getAspectRatio())
                .estimatedDurationSeconds(storyboard.getEstimatedDurationSeconds())
                .visualStyleGuide(storyboard.getVisualStyleGuide())
                .narrationStyle(storyboard.getNarrationStyle())
                .scenes(sceneInputs)
                .thumbnailContext(thumbnailInput)
                .languagePolicy("EN prompt + TR context")
                .culturalContextPolicy("Anadolu kültürel doğruluk")
                .safetyPolicy("production-safe, no graphic imagery")
                .requestedBy(requestedByAdminId)
                .build();
    }

    private VisualAssetSceneInput mapSceneToInput(StoryboardScene scene) {
        return VisualAssetSceneInput.builder()
                .storyboardSceneId(scene.getId())
                .sceneNumber(scene.getSceneNumber())
                .sourceBeatType(scene.getSourceBeatType() != null
                        ? scene.getSourceBeatType().name() : null)
                .sourceBeatOrder(scene.getSourceBeatOrder())
                .sceneTitle(scene.getSceneTitle())
                .visualDescription(scene.getVisualDescription())
                .narrationText(scene.getNarrationText())
                .onScreenText(scene.getOnScreenText())
                .cameraDirection(scene.getCameraDirection())
                .transitionNote(scene.getTransitionNote())
                .mood(scene.getMood())
                .location(scene.getLocation())
                .characters(scene.getCharacters())
                .estimatedDurationSeconds(scene.getEstimatedDurationSeconds())
                .mediaGenerationNotes(scene.getMediaGenerationNotes())
                .build();
    }

    // ── Response Validation ───────────────────────────────────────────

    private void validateGatewayResponse(VisualAssetPlanAgentResponse response,
                                          int expectedSceneCount) {
        List<String> errors = new ArrayList<>();

        if (response.getScenePlans() == null) {
            errors.add("AI yanıtında sahne planları null");
        } else {
            if (response.getScenePlans().size() != expectedSceneCount) {
                errors.add(String.format("AI yanıtında sahne sayısı uyuşmuyor: beklenen=%d, gelen=%d",
                        expectedSceneCount, response.getScenePlans().size()));
            }
            for (int i = 0; i < response.getScenePlans().size(); i++) {
                VisualAssetSceneOutput sceneOutput = response.getScenePlans().get(i);
                if (sceneOutput.getEnglishImagePrompt() == null
                        || sceneOutput.getEnglishImagePrompt().isBlank()) {
                    errors.add("Sahne " + (i + 1) + ": englishImagePrompt boş");
                }
                if (sceneOutput.getTurkishVisualContext() == null
                        || sceneOutput.getTurkishVisualContext().isBlank()) {
                    errors.add("Sahne " + (i + 1) + ": turkishVisualContext boş");
                }
            }
        }

        if (response.getThumbnailPlan() == null) {
            errors.add("AI yanıtında thumbnail planı null");
        } else if (response.getThumbnailPlan().getThumbnailPrompt() == null
                || response.getThumbnailPlan().getThumbnailPrompt().isBlank()) {
            errors.add("Thumbnail planında thumbnailPrompt boş");
        }

        if (!errors.isEmpty()) {
            String errorDetail = String.join("; ", errors);
            log.warn("FP-006: AI yanıt validasyonu başarısız: {}", errorDetail);
            throw new ValidationException(
                    "AI yanıtı geçerli bir görsel varlık planı üretmedi: " + errorDetail);
        }
    }

    // ── Success Handler ───────────────────────────────────────────────

    private VisualAssetPlanGenerationResult handleSuccess(
            StoryboardDraft storyboard,
            ContentProject project,
            List<StoryboardScene> orderedScenes,
            int version,
            VisualAssetPlanAgentResponse response,
            String requestedByAdminId) {

        // Parent aggregate oluştur
        VisualAssetPlan plan = VisualAssetPlan.builder()
                .contentProject(project)
                .sourceStoryboardDraft(storyboard)
                .version(version)
                .status(VisualAssetPlanStatus.AI_GENERATED)
                .title(response.getPlanTitle())
                .summary(storyboard.getSummary())
                .overallVisualStyle(response.getOverallVisualStyle())
                .aspectRatioStrategy(response.getAspectRatioStrategy())
                .culturalAuthenticityGuidance(response.getCulturalAuthenticityGuidance())
                .characterContinuityGuidance(response.getCharacterContinuityGuidance())
                .locationContinuityGuidance(response.getLocationContinuityGuidance())
                .colorPaletteGuidance(response.getColorPaletteGuidance())
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType(response.getProviderType())
                .modelName(response.getModelName())
                .build();

        // Sahne planlarını oluştur
        Map<UUID, StoryboardScene> sceneById = orderedScenes.stream()
                .collect(Collectors.toMap(StoryboardScene::getId, s -> s, (a, b) -> a));

        for (VisualAssetSceneOutput sceneOutput : response.getScenePlans()) {
            StoryboardScene sourceScene = sceneOutput.getSourceStoryboardSceneId() != null
                    ? sceneById.get(sceneOutput.getSourceStoryboardSceneId())
                    : null;

            ScenarioBeatType beatType = null;
            if (sceneOutput.getSourceBeatType() != null) {
                try {
                    beatType = ScenarioBeatType.valueOf(sceneOutput.getSourceBeatType());
                } catch (IllegalArgumentException e) {
                    log.warn("Bilinmeyen beat type: {}", sceneOutput.getSourceBeatType());
                }
            }

            SceneVisualPlan scenePlan = SceneVisualPlan.builder()
                    .visualAssetPlan(plan)
                    .sourceStoryboardScene(sourceScene)
                    .sceneNumber(sceneOutput.getSceneNumber())
                    .sourceBeatType(beatType)
                    .sceneTitle(sceneOutput.getSceneTitle())
                    .englishImagePrompt(sceneOutput.getEnglishImagePrompt())
                    .turkishVisualContext(sceneOutput.getTurkishVisualContext())
                    .negativePrompt(sceneOutput.getNegativePrompt())
                    .styleDirective(sceneOutput.getStyleDirective())
                    .aspectRatio(sceneOutput.getAspectRatio())
                    .compositionNotes(sceneOutput.getCompositionNotes())
                    .cameraFraming(sceneOutput.getCameraFraming())
                    .lightingNotes(sceneOutput.getLightingNotes())
                    .colorPalette(sceneOutput.getColorPalette())
                    .mood(sceneOutput.getMood())
                    .location(sceneOutput.getLocation())
                    .characters(sceneOutput.getCharacters())
                    .culturalDetails(sceneOutput.getCulturalDetails())
                    .textOverlayGuidance(sceneOutput.getTextOverlayGuidance())
                    .imageGenerationToolNotes(sceneOutput.getImageGenerationToolNotes())
                    .safetyNotes(sceneOutput.getSafetyNotes())
                    .build();

            plan.getScenePlans().add(scenePlan);
        }

        // Thumbnail plan oluştur
        VisualAssetThumbnailOutput thumbOut = response.getThumbnailPlan();
        StoryboardScene thumbSourceScene = thumbOut.getSourceStoryboardSceneId() != null
                ? sceneById.get(thumbOut.getSourceStoryboardSceneId())
                : null;

        ThumbnailPlan thumbnailPlan = ThumbnailPlan.builder()
                .visualAssetPlan(plan)
                .sourceStoryboardScene(thumbSourceScene)
                .sourceSceneNumber(thumbOut.getSourceSceneNumber())
                .thumbnailPrompt(thumbOut.getThumbnailPrompt())
                .negativePrompt(thumbOut.getNegativePrompt())
                .textOverlayGuidance(thumbOut.getTextOverlayGuidance())
                .compositionNotes(thumbOut.getCompositionNotes())
                .emotionalHook(thumbOut.getEmotionalHook())
                .aspectRatio(thumbOut.getAspectRatio())
                .styleDirective(thumbOut.getStyleDirective())
                .colorPalette(thumbOut.getColorPalette())
                .build();

        plan.setThumbnailPlan(thumbnailPlan);

        // Persist
        plan = visualAssetPlanRepository.save(plan);

        log.info("FP-006: Görsel varlık planı başarıyla üretildi. Plan ID: {}, Version: {}, "
                + "Scene count: {}, Thumbnail: {}",
                plan.getId(), plan.getVersion(), plan.getScenePlans().size(), true);

        return VisualAssetPlanGenerationResult.builder()
                .success(true)
                .contentProjectId(project.getId())
                .storyboardDraftId(storyboard.getId())
                .visualAssetPlanId(plan.getId())
                .version(plan.getVersion())
                .status(plan.getStatus().name())
                .planTitle(plan.getTitle())
                .scenePlanCount(plan.getScenePlans().size())
                .hasThumbnailPlan(true)
                .message("Görsel varlık planı başarıyla üretildi")
                .build();
    }

    // ── Failure Handler ───────────────────────────────────────────────

    private VisualAssetPlanGenerationResult handleGatewayFailure(
            StoryboardDraft storyboard,
            ContentProject project,
            int version,
            VisualAssetPlanAgentResponse response) {

        String errorCode = response.getErrorCode() != null
                ? response.getErrorCode() : "UNKNOWN_AI_ERROR";
        String errorMessage = response.getErrorMessage() != null
                ? response.getErrorMessage() : "AI servisi bilinmeyen hata döndürdü";

        log.warn("FP-006: AI gateway başarısız yanıt döndürdü. ErrorCode: {}, Retryable: {}, "
                + "StoryboardDraft ID: {}",
                errorCode, response.getRetryable(), storyboard.getId());

        // Task 06: AI failure → plan persist etme, sadece error result dön
        return VisualAssetPlanGenerationResult.builder()
                .success(false)
                .contentProjectId(project.getId())
                .storyboardDraftId(storyboard.getId())
                .version(version)
                .status("FAILED")
                .message(errorMessage)
                .errorCode(errorCode)
                .build();
    }
}
