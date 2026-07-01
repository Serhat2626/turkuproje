package com.anadoluatlasi.backend.application.scenario;

import com.anadoluatlasi.backend.ai.contract.ScenarioAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ScenarioAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ScenarioBeatResult;
import com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.content.repository.ContentDraftRepository;
import com.anadoluatlasi.backend.content.repository.ContentProjectRepository;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeat;
import com.anadoluatlasi.backend.scenario.entity.ScenarioBeatType;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import com.anadoluatlasi.backend.scenario.repository.ScenarioDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-002: Senaryo üretim iş akışını yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>generateScenarioForContentProject — Approved source draft'tan senaryo üretimi</li>
 * </ul>
 *
 * <p>İç sorumluluklar:</p>
 * <ul>
 *   <li>Approved source draft prerequisite doğrulaması</li>
 *   <li>ScenarioAgentRequest hazırlama</li>
 *   <li>Gateway çağrısı</li>
 *   <li>Response validation (6 beat, title, body)</li>
 *   <li>ScenarioDraft + ScenarioBeat persist</li>
 *   <li>Version increment</li>
 *   <li>Error mapping (FAILED status)</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>Senaryoyu otomatik onaylamaz (APPROVED yapmaz)</li>
 *   <li>Projeyi READY_FOR_PUBLISH yapmaz</li>
 *   <li>Human review'ı bypass etmez</li>
 *   <li>Raw prompt/payload/secret saklamaz</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioGenerationService {

    private static final int REQUIRED_BEAT_COUNT = 6;

    private final ContentProjectRepository contentProjectRepository;
    private final ContentDraftRepository contentDraftRepository;
    private final ScenarioDraftRepository scenarioDraftRepository;
    private final AiScenarioAgentGateway aiScenarioAgentGateway;

    /**
     * Approved source draft'tan senaryo candidate üretir.
     *
     * <p>Prerequisite: Content Project'e ait APPROVED status'lü bir ContentDraft olmalıdır.
     * Bu kontrol, FP-001 domain modeline dayanarak ContentDraft.status == APPROVED şeklinde
     * yapılır. ContentProject status'ü ayrıca kontrol edilmez çünkü ContentDraft APPROVED
     * olmadan ContentProject APPROVED olamaz.</p>
     *
     * @param contentProjectId senaryo üretilecek proje
     * @param requestedByAdminId talep eden admin (opsiyonel)
     * @return üretim sonucu
     */
    @Transactional
    public ScenarioGenerationResult generateScenarioForContentProject(
            UUID contentProjectId, String requestedByAdminId) {

        // 1. ContentProject yükle
        ContentProject project = contentProjectRepository.findById(contentProjectId)
                .orElseThrow(() -> new NotFoundException("ContentProject", contentProjectId));

        // 2. Approved source draft bul
        ContentDraft approvedDraft = findApprovedSourceDraft(project);

        // 3. ScenarioAgentRequest hazırla
        UUID requestId = UUID.randomUUID();
        ScenarioAgentRequest agentRequest = buildAgentRequest(
                project, approvedDraft, requestId, requestedByAdminId);

        // 4. Version hesapla
        int nextVersion = scenarioDraftRepository.findMaxVersionByContentProjectId(
                contentProjectId) + 1;

        log.info("Senaryo üretimi başlatıldı: projectId={}, requestId={}, version={}",
                contentProjectId, requestId, nextVersion);

        // 5. Gateway çağrısı
        ScenarioAgentResponse gatewayResponse;
        try {
            gatewayResponse = aiScenarioAgentGateway.generateScenario(agentRequest);
        } catch (Exception ex) {
            log.error("Senaryo gateway çağrısı exception: projectId={}, error={}",
                    contentProjectId, ex.getMessage());
            return handleGatewayException(project, approvedDraft, nextVersion, ex);
        }

        // 6. Başarı/hata işle
        if (gatewayResponse.isSuccess()) {
            return handleSuccess(project, approvedDraft, gatewayResponse, nextVersion, requestedByAdminId);
        } else {
            return handleFailure(project, approvedDraft, gatewayResponse, nextVersion, requestedByAdminId);
        }
    }

    // ── Approved Source Draft Check ───────────────────

    /**
     * Content Project'e ait APPROVED status'lü ContentDraft'i bulur.
     *
     * <p>FP-001 domain model truth: ContentDraft.status == APPROVED
     * bir insan tarafından explicit olarak onaylandığı anlamına gelir.
     * Bu, unapproved AI output'ından senaryo üretimini engeller.</p>
     *
     * @param project Content Project
     * @return Approved ContentDraft
     * @throws BusinessException approved draft yoksa
     */
    private ContentDraft findApprovedSourceDraft(ContentProject project) {
        List<ContentDraft> drafts = contentDraftRepository
                .findByContentProjectIdOrderByCreatedAtDesc(project.getId());

        return drafts.stream()
                .filter(d -> d.getStatus() == ContentDraftStatus.APPROVED)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        String.format("Senaryo üretimi için onaylanmış (APPROVED) bir içerik taslağı gereklidir. "
                                + "Proje ID: %s", project.getId())));
    }

    // ── Success Handler ───────────────────────────────

    private ScenarioGenerationResult handleSuccess(
            ContentProject project,
            ContentDraft approvedDraft,
            ScenarioAgentResponse response,
            int version,
            String requestedByAdminId) {

        // Response validation
        List<String> validationErrors = validateScenarioResponse(response);
        if (!validationErrors.isEmpty()) {
            String errorMsg = "Senaryo yanıt validasyonu başarısız: " + String.join(", ", validationErrors);
            log.warn("Validation hatası: projectId={}, errors={}", project.getId(), validationErrors);
            return createFailedDraft(project, approvedDraft, version, requestedByAdminId,
                    response.getProviderType(), response.getModelName(),
                    "INVALID_AI_RESPONSE_FORMAT", errorMsg);
        }

        // ScenarioDraft oluştur
        ScenarioDraft scenarioDraft = ScenarioDraft.builder()
                .contentProject(project)
                .sourceDraft(approvedDraft)
                .version(version)
                .status(ScenarioDraftStatus.AI_GENERATED)
                .title(response.getScenarioTitle())
                .summary(response.getSummary())
                .targetAudience(response.getTargetAudience())
                .culturalObjective(response.getCulturalObjective())
                .tone(response.getTone())
                .estimatedDurationSeconds(response.getEstimatedDurationSeconds())
                .setting(response.getSetting())
                .characters(response.getCharacters())
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType(response.getProviderType())
                .modelName(response.getModelName())
                .createdBy(requestedByAdminId)
                .build();

        scenarioDraft = scenarioDraftRepository.save(scenarioDraft);

        // ScenarioBeat'leri oluştur
        List<ScenarioBeat> beats = new ArrayList<>();
        for (ScenarioBeatResult beatResult : response.getBeats()) {
            ScenarioBeat beat = ScenarioBeat.builder()
                    .scenarioDraft(scenarioDraft)
                    .type(beatResult.getType())
                    .label(beatResult.getTitle())
                    .body(beatResult.getBody())
                    .orderIndex(beatResult.getOrder())
                    .estimatedDurationSeconds(beatResult.getEstimatedDurationSeconds())
                    .build();
            beats.add(beat);
        }
        scenarioDraft.getBeats().addAll(beats);
        scenarioDraft = scenarioDraftRepository.save(scenarioDraft);

        log.info("Senaryo başarıyla üretildi: projectId={}, scenarioDraftId={}, version={}, beats={}",
                project.getId(), scenarioDraft.getId(), version, beats.size());

        return ScenarioGenerationResult.builder()
                .success(true)
                .contentProjectId(project.getId())
                .scenarioDraftId(scenarioDraft.getId())
                .version(version)
                .status(ScenarioDraftStatus.AI_GENERATED.name())
                .scenarioTitle(scenarioDraft.getTitle())
                .message("Senaryo başarıyla üretildi")
                .build();
    }

    // ── Failure Handler ───────────────────────────────

    private ScenarioGenerationResult handleFailure(
            ContentProject project,
            ContentDraft approvedDraft,
            ScenarioAgentResponse response,
            int version,
            String requestedByAdminId) {

        log.warn("Senaryo üretim hatası: projectId={}, errorCode={}, retryable={}",
                project.getId(), response.getErrorCode(), response.getRetryable());

        return createFailedDraft(project, approvedDraft, version, requestedByAdminId,
                response.getProviderType(), response.getModelName(),
                response.getErrorCode(), response.getErrorMessage());
    }

    private ScenarioGenerationResult handleGatewayException(
            ContentProject project,
            ContentDraft approvedDraft,
            int version,
            Exception ex) {

        String sanitizedMessage = "Senaryo üretim servisi beklenmeyen hata.";
        return createFailedDraft(project, approvedDraft, version, null,
                null, null, "UNKNOWN_AI_ERROR", sanitizedMessage);
    }

    private ScenarioGenerationResult createFailedDraft(
            ContentProject project,
            ContentDraft approvedDraft,
            int version,
            String requestedByAdminId,
            String providerType,
            String modelName,
            String errorCode,
            String errorMessage) {

        ScenarioDraft failedDraft = ScenarioDraft.builder()
                .contentProject(project)
                .sourceDraft(approvedDraft)
                .version(version)
                .status(ScenarioDraftStatus.FAILED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .providerType(providerType)
                .modelName(modelName)
                .reviewNotes(errorMessage)
                .createdBy(requestedByAdminId)
                .build();

        failedDraft = scenarioDraftRepository.save(failedDraft);

        log.warn("FAILED senaryo kaydı oluşturuldu: projectId={}, scenarioDraftId={}, errorCode={}",
                project.getId(), failedDraft.getId(), errorCode);

        return ScenarioGenerationResult.builder()
                .success(false)
                .contentProjectId(project.getId())
                .scenarioDraftId(failedDraft.getId())
                .version(version)
                .status(ScenarioDraftStatus.FAILED.name())
                .errorCode(errorCode)
                .message(errorMessage)
                .build();
    }

    // ── Response Validation ────────────────────────────

    private List<String> validateScenarioResponse(ScenarioAgentResponse response) {
        List<String> errors = new ArrayList<>();

        // TITLE_REQUIRED
        if (response.getScenarioTitle() == null || response.getScenarioTitle().isBlank()) {
            errors.add("TITLE_REQUIRED");
        }

        // BEATS_REQUIRED
        if (response.getBeats() == null || response.getBeats().isEmpty()) {
            errors.add("BEATS_REQUIRED");
            return errors; // beat detay kontrolü anlamsız
        }

        // BEAT_COUNT_6
        if (response.getBeats().size() != REQUIRED_BEAT_COUNT) {
            errors.add("BEAT_COUNT_MUST_BE_" + REQUIRED_BEAT_COUNT);
        }

        // Her required beat tipi mevcut mu
        for (ScenarioBeatType requiredType : ScenarioBeatType.values()) {
            boolean found = response.getBeats().stream()
                    .anyMatch(b -> b.getType() == requiredType);
            if (!found) {
                errors.add("MISSING_BEAT_" + requiredType.name());
            }
        }

        // Beat body'leri boş mu
        for (ScenarioBeatResult beat : response.getBeats()) {
            if (beat.getBody() == null || beat.getBody().isBlank()) {
                errors.add("EMPTY_BEAT_BODY_" + beat.getType().name());
            }
        }

        return errors;
    }

    // ── Agent Request Builder ──────────────────────────

    private ScenarioAgentRequest buildAgentRequest(
            ContentProject project,
            ContentDraft approvedDraft,
            UUID requestId,
            String requestedByAdminId) {

        KnowledgeItem ki = project.getKnowledgeItem();
        int existingVersion = scenarioDraftRepository
                .findMaxVersionByContentProjectId(project.getId());

        ScenarioAgentRequest.ScenarioAgentRequestBuilder builder = ScenarioAgentRequest.builder()
                .requestId(requestId)
                .contentProjectId(project.getId())
                .sourceDraftId(approvedDraft.getId())
                .sourceDraftTitle(approvedDraft.getTitle())
                .sourceDraftBody(approvedDraft.getBody())
                .sourceDraftSummary(approvedDraft.getSummary())
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
            builder.existingScenarioVersion(existingVersion)
                    .generationReason("Regeneration — version " + (existingVersion + 1));
        } else {
            builder.generationReason("İlk senaryo üretimi");
        }

        return builder.build();
    }
}
