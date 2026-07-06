package com.anadoluatlasi.backend.application.platformpackage;

import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentRequest;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageAgentResponse;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageKnowledgeContextInput;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageOutput;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageSceneUsageOutput;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageSourceSceneInput;
import com.anadoluatlasi.backend.ai.contract.PlatformPackageSourceStoryboardInput;
import com.anadoluatlasi.backend.ai.gateway.AiPlatformPackageAgentGateway;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageSceneUsage;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageStatus;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import com.anadoluatlasi.backend.platformpackage.repository.PlatformPackageDraftRepository;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardScene;
import com.anadoluatlasi.backend.storyboard.repository.StoryboardDraftRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.anadoluatlasi.backend.review.GenerationReviewHandoffAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FP-004: Platform paketi üretim iş akışını yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>generatePlatformPackages — Approved StoryboardDraft'tan platform paketleri üretimi</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>Paketleri otomatik onaylamaz (APPROVED yapmaz)</li>
 *   <li>Projeyi READY_FOR_PUBLISH yapmaz</li>
 *   <li>Human review'ı bypass etmez</li>
 *   <li>Raw prompt/payload/secret saklamaz</li>
 *   <li>Gerçek media dosyası üretmez</li>
 *   <li>Platform'a yayın yapmaz</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformPackageGenerationService {

    /** Varsayılan hedef platformlar */
    private static final List<PlatformPackageType> DEFAULT_TARGET_PLATFORMS = List.of(
            PlatformPackageType.YOUTUBE_LONG_FORM,
            PlatformPackageType.YOUTUBE_SHORTS,
            PlatformPackageType.INSTAGRAM_REELS,
            PlatformPackageType.FACEBOOK_REELS
    );

    private final StoryboardDraftRepository storyboardDraftRepository;
    private final PlatformPackageDraftRepository platformPackageDraftRepository;
    private final AiPlatformPackageAgentGateway aiPlatformPackageAgentGateway;
    private final ObjectMapper objectMapper;
    private final GenerationReviewHandoffAdapter generationReviewHandoffAdapter;

    /**
     * Approved StoryboardDraft'tan platform paketleri üretir.
     *
     * <p>Prerequisite: StoryboardDraft.status == APPROVED olmalıdır.
     * Bu kontrol, onaylanmamış storyboard'dan paket üretimini engeller.</p>
     *
     * @param storyboardDraftId paket üretilecek storyboard
     * @param requestedByAdminId talep eden admin
     * @return üretim sonucu
     */
    @Transactional
    public PlatformPackageGenerationResult generatePlatformPackages(
            UUID storyboardDraftId, String requestedByAdminId) {

        // 1. StoryboardDraft yükle
        StoryboardDraft storyboard = storyboardDraftRepository.findById(storyboardDraftId)
                .orElseThrow(() -> new NotFoundException("StoryboardDraft", storyboardDraftId));

        // 2. Approved prerequisite check
        validateApprovedStoryboard(storyboard);

        // 3. Scene validation
        // Force initialize scenes (open-in-view=false)
        List<StoryboardScene> scenes = storyboard.getScenes();
        if (scenes == null || scenes.isEmpty()) {
            throw new BusinessException(
                    String.format("Platform paket üretimi için storyboard scene'leri gereklidir. "
                            + "Storyboard ID: %s", storyboard.getId()));
        }
        // Force load - ensure collection is initialized
        int sceneCount = scenes.size();

        // 4. Source references
        ContentProject project = storyboard.getContentProject();

        // 5. Build agent request
        UUID requestId = UUID.randomUUID();
        PlatformPackageAgentRequest agentRequest = buildAgentRequest(
                project, storyboard, scenes, requestId, requestedByAdminId);

        log.info("Platform paket üretimi başlatıldı: storyboardId={}, projectId={}, requestId={}, scenes={}",
                storyboardDraftId, project.getId(), requestId, sceneCount);

        // 6. Gateway çağrısı
        PlatformPackageAgentResponse gatewayResponse;
        try {
            gatewayResponse = aiPlatformPackageAgentGateway.generatePlatformPackages(agentRequest);
        } catch (Exception ex) {
            log.error("Platform paket gateway çağrısı exception: storyboardId={}, error={}",
                    storyboardDraftId, ex.getMessage());
            return buildErrorResult(project, storyboard,
                    "UNKNOWN_AI_ERROR", "Platform paket üretim servisi beklenmeyen hata.");
        }

        // 7. Başarı/hata işle
        if (gatewayResponse.isSuccess()) {
            return handleSuccess(project, storyboard, scenes, gatewayResponse, requestedByAdminId);
        } else {
            return buildErrorResult(project, storyboard,
                    gatewayResponse.getErrorCode(), gatewayResponse.getErrorMessage());
        }
    }

    // ── Approved Storyboard Check ─────────────────────────

    private void validateApprovedStoryboard(StoryboardDraft storyboard) {
        if (storyboard.getStatus() != StoryboardDraftStatus.APPROVED) {
            throw new BusinessException(
                    String.format("Platform paket üretimi için onaylanmış (APPROVED) bir storyboard taslağı gereklidir. "
                            + "Mevcut durum: %s, Storyboard ID: %s",
                            storyboard.getStatus(), storyboard.getId()));
        }
    }

    // ── Success Handler ─────────────────────────────────────

    private PlatformPackageGenerationResult handleSuccess(
            ContentProject project,
            StoryboardDraft storyboard,
            List<StoryboardScene> scenes,
            PlatformPackageAgentResponse response,
            String requestedByAdminId) {

        // Response validation — all-or-nothing
        List<String> validationErrors = validateResponse(response, DEFAULT_TARGET_PLATFORMS);
        if (!validationErrors.isEmpty()) {
            String errorMsg = "Platform paket yanıt validasyonu başarısız: " + String.join(", ", validationErrors);
            log.warn("Validation hatası: storyboardId={}, errors={}", storyboard.getId(), validationErrors);
            return buildErrorResult(project, storyboard, "INVALID_AI_RESPONSE_FORMAT", errorMsg);
        }

        // Persist all packages — all-or-nothing
        List<UUID> generatedIds = new ArrayList<>();
        for (PlatformPackageOutput pkgOutput : response.getPackages()) {
            PlatformPackageType packageType = PlatformPackageType.valueOf(pkgOutput.getPackageType());

            // Version hesapla
            int nextVersion = platformPackageDraftRepository
                    .findMaxVersionByProjectAndStoryboardAndType(
                            project.getId(), storyboard.getId(), packageType) + 1;

            PlatformPackageDraft draft = PlatformPackageDraft.builder()
                    .contentProject(project)
                    .sourceStoryboardDraft(storyboard)
                    .packageType(packageType)
                    .status(PlatformPackageStatus.AI_GENERATED)
                    .version(nextVersion)
                    .title(pkgOutput.getTitle())
                    .captionOrDescription(pkgOutput.getCaptionOrDescription())
                    .hashtagsJson(toJsonArray(pkgOutput.getHashtags()))
                    .callToAction(pkgOutput.getCallToAction())
                    .targetDurationSeconds(pkgOutput.getTargetDurationSeconds())
                    .aspectRatio(pkgOutput.getAspectRatio())
                    .contentStructureNotes(pkgOutput.getContentStructureNotes())
                    .sceneUsageNotes(pkgOutput.getSceneUsageNotes())
                    // YouTube Long-form
                    .videoTitle(pkgOutput.getVideoTitle())
                    .longDescription(pkgOutput.getLongDescription())
                    .chapterOutlineJson(toJsonArray(pkgOutput.getChapterOutline()))
                    .pinnedCommentSuggestion(pkgOutput.getPinnedCommentSuggestion())
                    .thumbnailTextSuggestion(pkgOutput.getThumbnailTextSuggestion())
                    .tagsJson(toJsonArray(pkgOutput.getTags()))
                    // Short-form / Reels
                    .shortHook(pkgOutput.getShortHook())
                    .shortCaption(pkgOutput.getShortCaption())
                    .overlayTextSuggestionsJson(toJsonArray(pkgOutput.getOverlayTextSuggestions()))
                    .shortHashtagSetJson(toJsonArray(pkgOutput.getShortHashtagSet()))
                    .shortCta(pkgOutput.getShortCta())
                    .durationGuidance(pkgOutput.getDurationGuidance())
                    // AI metadata
                    .providerType(response.getProviderType())
                    .modelName(response.getModelName())
                    .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                    .build();

            draft = platformPackageDraftRepository.save(draft);

            // Scene usages
            if (pkgOutput.getSceneUsages() != null && !pkgOutput.getSceneUsages().isEmpty()) {
                for (PlatformPackageSceneUsageOutput usageOutput : pkgOutput.getSceneUsages()) {
                    StoryboardScene matchedScene = findSceneByNumber(scenes, usageOutput.getSceneNumber());

                    PlatformPackageSceneUsage sceneUsage = PlatformPackageSceneUsage.builder()
                            .platformPackageDraft(draft)
                            .sceneNumber(usageOutput.getSceneNumber())
                            .sourceStoryboardScene(matchedScene)
                            .usageType(usageOutput.getUsageType())
                            .included(usageOutput.isIncluded())
                            .adaptationNote(usageOutput.getAdaptationNote())
                            .build();

                    draft.getSceneUsages().add(sceneUsage);
                }
                platformPackageDraftRepository.save(draft);
            }

            generatedIds.add(draft.getId());

            // FP-013 Task 05: Hand off each package to review workflow (non-fatal)
            generationReviewHandoffAdapter.handoffPackage(
                    draft.getId(), draft.getTitle(), draft.getCaptionOrDescription());
        }

        log.info("Platform paketleri başarıyla üretildi: storyboardId={}, packageCount={}",
                storyboard.getId(), generatedIds.size());

        return PlatformPackageGenerationResult.builder()
                .success(true)
                .contentProjectId(project.getId())
                .sourceStoryboardDraftId(storyboard.getId())
                .generatedPackageIds(generatedIds)
                .packageCount(generatedIds.size())
                .message("Platform paketleri başarıyla üretildi")
                .build();
    }

    // ── Response Validation (All-or-Nothing) ──────────────

    private List<String> validateResponse(
            PlatformPackageAgentResponse response,
            List<PlatformPackageType> expectedPlatforms) {

        List<String> errors = new ArrayList<>();

        // Packages required
        if (response.getPackages() == null || response.getPackages().isEmpty()) {
            errors.add("PACKAGES_REQUIRED");
            return errors;
        }

        // Exactly one package per requested platform
        if (response.getPackages().size() != expectedPlatforms.size()) {
            errors.add("PACKAGE_COUNT_MISMATCH_EXPECTED_" + expectedPlatforms.size()
                    + "_GOT_" + response.getPackages().size());
        }

        // Check for duplicate and unknown types
        Set<String> seenTypes = new HashSet<>();
        for (PlatformPackageOutput pkg : response.getPackages()) {
            if (pkg.getPackageType() == null || pkg.getPackageType().isBlank()) {
                errors.add("MISSING_PACKAGE_TYPE");
                continue;
            }

            // Valid enum check
            try {
                PlatformPackageType.valueOf(pkg.getPackageType());
            } catch (IllegalArgumentException e) {
                errors.add("UNKNOWN_PACKAGE_TYPE_" + pkg.getPackageType());
                continue;
            }

            // Duplicate check
            if (!seenTypes.add(pkg.getPackageType())) {
                errors.add("DUPLICATE_PACKAGE_TYPE_" + pkg.getPackageType());
            }
        }

        // Check each expected platform is present
        Set<String> responseTypes = response.getPackages().stream()
                .map(PlatformPackageOutput::getPackageType)
                .collect(Collectors.toSet());
        for (PlatformPackageType expected : expectedPlatforms) {
            if (!responseTypes.contains(expected.name())) {
                errors.add("MISSING_PLATFORM_" + expected.name());
            }
        }

        // Per-package field validation
        for (PlatformPackageOutput pkg : response.getPackages()) {
            String prefix = pkg.getPackageType() != null ? pkg.getPackageType() + "_" : "UNKNOWN_";

            if (pkg.getTitle() == null || pkg.getTitle().isBlank()) {
                errors.add(prefix + "TITLE_REQUIRED");
            }
            if (pkg.getCaptionOrDescription() == null || pkg.getCaptionOrDescription().isBlank()) {
                errors.add(prefix + "CAPTION_REQUIRED");
            }
            if (pkg.getCallToAction() == null || pkg.getCallToAction().isBlank()) {
                errors.add(prefix + "CTA_REQUIRED");
            }
        }

        // AI-supplied status must not be trusted — no status field in output contract
        // APPROVED must never come from AI — enforced by not having status in output

        return errors;
    }

    // ── Agent Request Builder ──────────────────────────────

    private PlatformPackageAgentRequest buildAgentRequest(
            ContentProject project,
            StoryboardDraft storyboard,
            List<StoryboardScene> scenes,
            UUID requestId,
            String requestedByAdminId) {

        // Source storyboard input
        PlatformPackageSourceStoryboardInput storyboardInput = PlatformPackageSourceStoryboardInput.builder()
                .title(storyboard.getTitle())
                .summary(storyboard.getSummary())
                .visualStyleGuide(storyboard.getVisualStyleGuide())
                .narrationStyle(storyboard.getNarrationStyle())
                .targetPlatform(storyboard.getTargetPlatform())
                .aspectRatio(storyboard.getAspectRatio())
                .estimatedDurationSeconds(storyboard.getEstimatedDurationSeconds())
                .totalSceneCount(scenes.size())
                .build();

        // Source scene inputs
        List<PlatformPackageSourceSceneInput> sceneInputs = scenes.stream()
                .map(scene -> PlatformPackageSourceSceneInput.builder()
                        .sceneNumber(scene.getSceneNumber())
                        .sceneId(scene.getId())
                        .sceneTitle(scene.getSceneTitle())
                        .visualDescription(scene.getVisualDescription())
                        .narrationText(scene.getNarrationText())
                        .mood(scene.getMood())
                        .estimatedDurationSeconds(scene.getEstimatedDurationSeconds())
                        .sourceBeatType(scene.getSourceBeatType() != null
                                ? scene.getSourceBeatType().name() : null)
                        .build())
                .collect(Collectors.toList());

        // Knowledge context
        PlatformPackageKnowledgeContextInput knowledgeContext = null;
        KnowledgeItem ki = project.getKnowledgeItem();
        if (ki != null) {
            knowledgeContext = PlatformPackageKnowledgeContextInput.builder()
                    .title(ki.getTitle())
                    .description(ki.getDescription())
                    .city(ki.getCity())
                    .build();
        }

        // Determine generation reason
        String generationReason = "İlk platform paket üretimi";

        return PlatformPackageAgentRequest.builder()
                .requestId(requestId)
                .contentProjectId(project.getId())
                .sourceStoryboardDraftId(storyboard.getId())
                .sourceStoryboardVersion(storyboard.getVersion())
                .targetPlatforms(DEFAULT_TARGET_PLATFORMS)
                .sourceStoryboard(storyboardInput)
                .sourceScenes(sceneInputs)
                .knowledgeContext(knowledgeContext)
                .language(project.getLanguage())
                .requestedByAdminId(requestedByAdminId)
                .generationReason(generationReason)
                .build();
    }

    // ── Error Result Builder ──────────────────────────────

    private PlatformPackageGenerationResult buildErrorResult(
            ContentProject project,
            StoryboardDraft storyboard,
            String errorCode,
            String errorMessage) {

        return PlatformPackageGenerationResult.builder()
                .success(false)
                .contentProjectId(project.getId())
                .sourceStoryboardDraftId(storyboard.getId())
                .errorCode(errorCode)
                .message(errorMessage)
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────

    private StoryboardScene findSceneByNumber(List<StoryboardScene> scenes, int sceneNumber) {
        return scenes.stream()
                .filter(s -> s.getSceneNumber() == sceneNumber)
                .findFirst()
                .orElse(null);
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("JSON dönüşüm hatası: {}", e.getMessage());
            return list.toString();
        }
    }
}
