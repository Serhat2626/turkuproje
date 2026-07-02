package com.anadoluatlasi.backend.application.storyboard;

import com.anadoluatlasi.backend.ai.contract.StoryboardAgentRequest;
import com.anadoluatlasi.backend.ai.contract.StoryboardAgentResponse;
import com.anadoluatlasi.backend.ai.contract.StoryboardSceneOutput;
import com.anadoluatlasi.backend.ai.contract.StoryboardSourceBeatInput;
import com.anadoluatlasi.backend.ai.gateway.AiStoryboardAgentGateway;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeat;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.scenario.repository.ScenarioDraftRepository;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import com.anadoluatlasi.backend.storyboard.repository.StoryboardDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FP-003: Storyboard üretim iş akışını yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>generateStoryboardForScenario — Approved ScenarioDraft'tan storyboard üretimi</li>
 * </ul>
 *
 * <p>İç sorumluluklar:</p>
 * <ul>
 *   <li>Approved source ScenarioDraft prerequisite doğrulaması</li>
 *   <li>StoryboardAgentRequest hazırlama</li>
 *   <li>Gateway çağrısı</li>
 *   <li>Response validation (title, scenes, visualDescription)</li>
 *   <li>StoryboardDraft + StoryboardScene persist</li>
 *   <li>Version increment</li>
 *   <li>Error mapping (FAILED status)</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>Storyboard'u otomatik onaylamaz (APPROVED yapmaz)</li>
 *   <li>Projeyi READY_FOR_PUBLISH yapmaz</li>
 *   <li>Human review'ı bypass etmez</li>
 *   <li>Raw prompt/payload/secret saklamaz</li>
 *   <li>Gerçek media dosyası (image/video/audio) üretmez</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryboardGenerationService {

    private final ScenarioDraftRepository scenarioDraftRepository;
    private final StoryboardDraftRepository storyboardDraftRepository;
    private final AiStoryboardAgentGateway aiStoryboardAgentGateway;

    /**
     * Approved ScenarioDraft'tan storyboard candidate üretir.
     *
     * <p>Prerequisite: ScenarioDraft.status == APPROVED olmalıdır.
     * Bu kontrol, AI_GENERATED/NEEDS_REVIEW/REJECTED/FAILED scenario'dan
     * storyboard üretimini engeller.</p>
     *
     * @param scenarioDraftId storyboard üretilecek senaryo
     * @param requestedByAdminId talep eden admin (opsiyonel)
     * @return üretim sonucu
     */
    @Transactional
    public StoryboardGenerationResult generateStoryboardForScenario(
            UUID scenarioDraftId, String requestedByAdminId) {

        // 1. ScenarioDraft yükle
        ScenarioDraft scenario = scenarioDraftRepository.findById(scenarioDraftId)
                .orElseThrow(() -> new NotFoundException("ScenarioDraft", scenarioDraftId));

        // 2. Approved prerequisite check
        validateApprovedScenario(scenario);

        // 3. Source references
        ContentProject project = scenario.getContentProject();
        ContentDraft sourceDraft = scenario.getSourceDraft();

        // 4. Beat validation
        validateScenarioBeats(scenario);

        // 5. StoryboardAgentRequest hazırla
        UUID requestId = UUID.randomUUID();
        StoryboardAgentRequest agentRequest = buildAgentRequest(
                project, sourceDraft, scenario, requestId, requestedByAdminId);

        // 6. Version hesapla
        int nextVersion = storyboardDraftRepository.findMaxVersionByContentProjectId(
                project.getId()) + 1;

        log.info("Storyboard üretimi başlatıldı: scenarioId={}, projectId={}, requestId={}, version={}",
                scenarioDraftId, project.getId(), requestId, nextVersion);

        // 7. Gateway çağrısı
        StoryboardAgentResponse gatewayResponse;
        try {
            gatewayResponse = aiStoryboardAgentGateway.generateStoryboard(agentRequest);
        } catch (Exception ex) {
            log.error("Storyboard gateway çağrısı exception: scenarioId={}, error={}",
                    scenarioDraftId, ex.getMessage());
            return handleGatewayException(project, sourceDraft, scenario, nextVersion, ex);
        }

        // 8. Başarı/hata işle
        if (gatewayResponse.isSuccess()) {
            return handleSuccess(project, sourceDraft, scenario, gatewayResponse, nextVersion, requestedByAdminId);
        } else {
            return handleFailure(project, sourceDraft, scenario, gatewayResponse, nextVersion, requestedByAdminId);
        }
    }

    // ── Approved Scenario Check ────────────────────────

    /**
     * ScenarioDraft'ın APPROVED olduğunu doğrular.
     *
     * <p>FP-002 domain model truth: ScenarioDraft.status == APPROVED
     * bir insan tarafından explicit olarak onaylandığı anlamına gelir.
     * Bu, onaylanmamış senaryo'dan storyboard üretimini engeller.</p>
     *
     * @param scenario ScenarioDraft
     * @throws BusinessException scenario approved değilse
     */
    private void validateApprovedScenario(ScenarioDraft scenario) {
        if (scenario.getStatus() != ScenarioDraftStatus.APPROVED) {
            throw new BusinessException(
                    String.format("Storyboard üretimi için onaylanmış (APPROVED) bir senaryo taslağı gereklidir. "
                            + "Mevcut durum: %s, Senaryo ID: %s", scenario.getStatus(), scenario.getId()));
        }
    }

    /**
     * ScenarioDraft'ın kullanılabilir beat'lere sahip olduğunu doğrular.
     */
    private void validateScenarioBeats(ScenarioDraft scenario) {
        if (scenario.getBeats() == null || scenario.getBeats().isEmpty()) {
            throw new BusinessException(
                    String.format("Storyboard üretimi için senaryo beat'leri gereklidir. "
                            + "Senaryo ID: %s", scenario.getId()));
        }
    }

    // ── Success Handler ───────────────────────────────

    private StoryboardGenerationResult handleSuccess(
            ContentProject project,
            ContentDraft sourceDraft,
            ScenarioDraft scenario,
            StoryboardAgentResponse response,
            int version,
            String requestedByAdminId) {

        // Response validation
        List<String> validationErrors = validateStoryboardResponse(response);
        if (!validationErrors.isEmpty()) {
            String errorMsg = "Storyboard yanıt validasyonu başarısız: " + String.join(", ", validationErrors);
            log.warn("Validation hatası: scenarioId={}, errors={}", scenario.getId(), validationErrors);
            return createFailedDraft(project, sourceDraft, scenario, version, requestedByAdminId,
                    response.getProviderType(), response.getModelName(),
                    "INVALID_AI_RESPONSE_FORMAT", errorMsg);
        }

        // StoryboardDraft oluştur
        StoryboardDraft storyboardDraft = StoryboardDraft.builder()
                .contentProject(project)
                .sourceDraft(sourceDraft)
                .sourceScenarioDraft(scenario)
                .sourceScenarioVersion(scenario.getVersion())
                .version(version)
                .status(StoryboardDraftStatus.AI_GENERATED)
                .title(response.getStoryboardTitle())
                .summary(response.getSummary())
                .targetPlatform(response.getTargetPlatform())
                .aspectRatio(response.getAspectRatio())
                .estimatedDurationSeconds(response.getEstimatedDurationSeconds())
                .visualStyleGuide(response.getVisualStyleGuide())
                .narrationStyle(response.getNarrationStyle())
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType(response.getProviderType())
                .modelName(response.getModelName())
                .createdBy(requestedByAdminId)
                .build();

        storyboardDraft = storyboardDraftRepository.save(storyboardDraft);

        // StoryboardScene'leri oluştur
        List<StoryboardScene> scenes = new ArrayList<>();
        for (StoryboardSceneOutput sceneOutput : response.getScenes()) {
            StoryboardScene scene = StoryboardScene.builder()
                    .storyboardDraft(storyboardDraft)
                    .sceneNumber(sceneOutput.getSceneNumber())
                    .sourceBeatType(sceneOutput.getSourceBeatType())
                    .sourceBeatOrder(sceneOutput.getSourceBeatOrder())
                    .sceneTitle(sceneOutput.getSceneTitle())
                    .visualDescription(sceneOutput.getVisualDescription())
                    .narrationText(sceneOutput.getNarrationText())
                    .onScreenText(sceneOutput.getOnScreenText())
                    .cameraDirection(sceneOutput.getCameraDirection())
                    .transitionNote(sceneOutput.getTransitionNote())
                    .mood(sceneOutput.getMood())
                    .location(sceneOutput.getLocation())
                    .characters(sceneOutput.getCharacters())
                    .estimatedDurationSeconds(sceneOutput.getEstimatedDurationSeconds())
                    .mediaGenerationNotes(sceneOutput.getMediaGenerationNotes())
                    .build();
            scenes.add(scene);
        }
        storyboardDraft.getScenes().addAll(scenes);
        storyboardDraft = storyboardDraftRepository.save(storyboardDraft);

        log.info("Storyboard başarıyla üretildi: scenarioId={}, storyboardDraftId={}, version={}, scenes={}",
                scenario.getId(), storyboardDraft.getId(), version, scenes.size());

        return StoryboardGenerationResult.builder()
                .success(true)
                .contentProjectId(project.getId())
                .storyboardDraftId(storyboardDraft.getId())
                .version(version)
                .status(StoryboardDraftStatus.AI_GENERATED.name())
                .storyboardTitle(storyboardDraft.getTitle())
                .sceneCount(scenes.size())
                .message("Storyboard başarıyla üretildi")
                .build();
    }

    // ── Failure Handler ───────────────────────────────

    private StoryboardGenerationResult handleFailure(
            ContentProject project,
            ContentDraft sourceDraft,
            ScenarioDraft scenario,
            StoryboardAgentResponse response,
            int version,
            String requestedByAdminId) {

        log.warn("Storyboard üretim hatası: scenarioId={}, errorCode={}, retryable={}",
                scenario.getId(), response.getErrorCode(), response.getRetryable());

        return createFailedDraft(project, sourceDraft, scenario, version, requestedByAdminId,
                response.getProviderType(), response.getModelName(),
                response.getErrorCode(), response.getErrorMessage());
    }

    private StoryboardGenerationResult handleGatewayException(
            ContentProject project,
            ContentDraft sourceDraft,
            ScenarioDraft scenario,
            int version,
            Exception ex) {

        String sanitizedMessage = "Storyboard üretim servisi beklenmeyen hata.";
        return createFailedDraft(project, sourceDraft, scenario, version, null,
                null, null, "UNKNOWN_AI_ERROR", sanitizedMessage);
    }

    private StoryboardGenerationResult createFailedDraft(
            ContentProject project,
            ContentDraft sourceDraft,
            ScenarioDraft scenario,
            int version,
            String requestedByAdminId,
            String providerType,
            String modelName,
            String errorCode,
            String errorMessage) {

        StoryboardDraft failedDraft = StoryboardDraft.builder()
                .contentProject(project)
                .sourceDraft(sourceDraft)
                .sourceScenarioDraft(scenario)
                .sourceScenarioVersion(scenario.getVersion())
                .version(version)
                .status(StoryboardDraftStatus.FAILED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType(providerType)
                .modelName(modelName)
                .reviewNotes(errorMessage)
                .createdBy(requestedByAdminId)
                .build();

        failedDraft = storyboardDraftRepository.save(failedDraft);

        log.warn("FAILED storyboard kaydı oluşturuldu: scenarioId={}, storyboardDraftId={}, errorCode={}",
                scenario.getId(), failedDraft.getId(), errorCode);

        return StoryboardGenerationResult.builder()
                .success(false)
                .contentProjectId(project.getId())
                .storyboardDraftId(failedDraft.getId())
                .version(version)
                .status(StoryboardDraftStatus.FAILED.name())
                .errorCode(errorCode)
                .message(errorMessage)
                .build();
    }

    // ── Response Validation ────────────────────────────

    private List<String> validateStoryboardResponse(StoryboardAgentResponse response) {
        List<String> errors = new ArrayList<>();

        // TITLE_REQUIRED
        if (response.getStoryboardTitle() == null || response.getStoryboardTitle().isBlank()) {
            errors.add("TITLE_REQUIRED");
        }

        // SCENES_REQUIRED
        if (response.getScenes() == null || response.getScenes().isEmpty()) {
            errors.add("SCENES_REQUIRED");
            return errors; // scene detay kontrolü anlamsız
        }

        // Her scene'de zorunlu alanlar
        for (StoryboardSceneOutput scene : response.getScenes()) {
            // Scene title
            if (scene.getSceneTitle() == null || scene.getSceneTitle().isBlank()) {
                errors.add("EMPTY_SCENE_TITLE_" + scene.getSceneNumber());
            }
            // Visual description zorunlu
            if (scene.getVisualDescription() == null || scene.getVisualDescription().isBlank()) {
                errors.add("EMPTY_VISUAL_DESCRIPTION_" + scene.getSceneNumber());
            }
            // Scene number valid
            if (scene.getSceneNumber() <= 0) {
                errors.add("INVALID_SCENE_NUMBER_" + scene.getSceneNumber());
            }
        }

        // Scene order deterministic check
        for (int i = 0; i < response.getScenes().size(); i++) {
            int expectedNumber = i + 1;
            if (response.getScenes().get(i).getSceneNumber() != expectedNumber) {
                errors.add("INVALID_SCENE_ORDER_EXPECTED_" + expectedNumber
                        + "_GOT_" + response.getScenes().get(i).getSceneNumber());
            }
        }

        return errors;
    }

    // ── Agent Request Builder ──────────────────────────

    private StoryboardAgentRequest buildAgentRequest(
            ContentProject project,
            ContentDraft sourceDraft,
            ScenarioDraft scenario,
            UUID requestId,
            String requestedByAdminId) {

        // Scenario beat'lerini input contract'a dönüştür
        List<StoryboardSourceBeatInput> beatInputs = scenario.getBeats().stream()
                .map(beat -> StoryboardSourceBeatInput.builder()
                        .beatType(beat.getType())
                        .beatTitle(beat.getLabel())
                        .beatBody(beat.getBody())
                        .orderIndex(beat.getOrderIndex())
                        .estimatedDurationSeconds(beat.getEstimatedDurationSeconds())
                        .build())
                .collect(Collectors.toList());

        KnowledgeItem ki = project.getKnowledgeItem();
        int existingVersion = storyboardDraftRepository
                .findMaxVersionByContentProjectId(project.getId());

        StoryboardAgentRequest.StoryboardAgentRequestBuilder builder = StoryboardAgentRequest.builder()
                .requestId(requestId)
                .contentProjectId(project.getId())
                .sourceDraftId(sourceDraft.getId())
                .sourceScenarioId(scenario.getId())
                .sourceScenarioVersion(scenario.getVersion())
                .scenarioTitle(scenario.getTitle())
                .scenarioSummary(scenario.getSummary())
                .scenarioBeats(beatInputs)
                .language(project.getLanguage())
                .requestedByAdminId(requestedByAdminId);

        // Knowledge Item context
        if (ki != null) {
            builder.knowledgeItemTitle(ki.getTitle())
                    .knowledgeItemDescription(ki.getDescription())
                    .knowledgeItemCity(ki.getCity());
        }

        // Content Project config
        if (project.getTargetAudience() != null) {
            builder.targetAudience(project.getTargetAudience().name());
        }
        if (project.getTone() != null) {
            builder.tone(project.getTone().name());
        }

        // Regeneration context
        if (existingVersion > 0) {
            builder.existingStoryboardVersion(existingVersion)
                    .generationReason("Regeneration — version " + (existingVersion + 1));
        } else {
            builder.generationReason("İlk storyboard üretimi");
        }

        return builder.build();
    }
}
