package com.anadoluatlasi.backend.application.ai;

import com.anadoluatlasi.backend.ai.contract.ContentAgentError;
import com.anadoluatlasi.backend.ai.contract.ContentAgentKnowledgeItemPayload;
import com.anadoluatlasi.backend.ai.contract.ContentAgentRequest;
import com.anadoluatlasi.backend.ai.contract.ContentAgentResponse;
import com.anadoluatlasi.backend.ai.contract.ContentAgentRetryMetadata;
import com.anadoluatlasi.backend.ai.contract.HumanFeedbackPayload;
import com.anadoluatlasi.backend.ai.contract.PreviousDraftPayload;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;
import com.anadoluatlasi.backend.ai.entity.AiGenerationRequest;
import com.anadoluatlasi.backend.ai.entity.AiGenerationStatus;
import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiGatewayResult;
import com.anadoluatlasi.backend.ai.repository.AiGenerationRequestRepository;
import com.anadoluatlasi.backend.common.exception.BusinessException;
import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.content.entity.ContentDraft;
import com.anadoluatlasi.backend.content.entity.ContentDraftStatus;
import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.GeneratedBy;
import com.anadoluatlasi.backend.content.repository.ContentDraftRepository;
import com.anadoluatlasi.backend.content.repository.ContentProjectRepository;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.review.GenerationReviewHandoffAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-001: AI destekli içerik üretim iş akışını yöneten application service.
 *
 * <p>Operasyonlar:</p>
 * <ul>
 *   <li>generateAiDraft — CREATED/REVISION_REQUESTED projeden AI draft üretimi</li>
 *   <li>retryAiDraft — FAILED projede yeniden AI denemesi (max 3)</li>
 * </ul>
 *
 * <p>İç sorumluluklar:</p>
 * <ul>
 *   <li>ContentAgentRequest hazırlama</li>
 *   <li>AiGenerationRequest kaydı oluşturma</li>
 *   <li>Project status güncelleme (AI_REQUESTED → AI_PROCESSING → DRAFT_READY/FAILED)</li>
 *   <li>Gateway çağrısı</li>
 *   <li>Response validation (MVP kuralları)</li>
 *   <li>ContentDraft oluşturma (yalnızca validation başarılıysa)</li>
 *   <li>Error bilgisi normalize edip kaydetme</li>
 * </ul>
 *
 * <p>Bu service asla:</p>
 * <ul>
 *   <li>Draft'ı otomatik onaylamaz (APPROVED yapmaz)</li>
 *   <li>Projeyi READY_FOR_PUBLISH yapmaz</li>
 *   <li>Human review'ı bypass etmez</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationService {

    private static final int MAX_RETRY_COUNT = 3;
    private static final int MIN_BODY_LENGTH = 100;

    private final ContentProjectRepository contentProjectRepository;
    private final ContentDraftRepository contentDraftRepository;
    private final AiGenerationRequestRepository aiGenerationRequestRepository;
    private final AiContentAgentGateway aiContentAgentGateway;
    private final GenerationReviewHandoffAdapter generationReviewHandoffAdapter;

    // ── Generate AI Draft ───────────────────────────────────

    /**
     * CREATED veya REVISION_REQUESTED projeden AI draft üretir.
     * Revision durumunda retryCount sıfırlanır (revision ≠ retry).
     */
    @Transactional
    public AiGenerationResult generateAiDraft(UUID contentProjectId) {
        return generateAiDraft(contentProjectId, null, null);
    }

    /**
     * CREATED veya REVISION_REQUESTED projeden AI draft üretir.
     * Revision durumunda humanFeedback ve previousDraft bilgisi verilebilir.
     *
     * @param contentProjectId proje ID
     * @param humanFeedbackNote revizyon notu (opsiyonel)
     * @param feedbackType revizyon türü (opsiyonel)
     */
    @Transactional
    public AiGenerationResult generateAiDraft(UUID contentProjectId,
                                               String humanFeedbackNote,
                                               String feedbackType) {
        ContentProject project = findProjectOrThrow(contentProjectId);

        // Statü kontrolü
        if (project.getStatus() != ContentProjectStatus.CREATED
                && project.getStatus() != ContentProjectStatus.REVISION_REQUESTED) {
            throw new BusinessException(
                    String.format("AI draft üretimi yapılamaz. Mevcut statü: %s, Beklenen: CREATED veya REVISION_REQUESTED",
                            project.getStatus()));
        }

        // Revision ise retryCount sıfırla (revision ≠ retry)
        if (project.getStatus() == ContentProjectStatus.REVISION_REQUESTED) {
            project.setRetryCount(0);
        }

        // Önceki hata bilgisini temizle
        project.setLastErrorType(null);
        project.setLastErrorMessage(null);

        return executeAiGeneration(project, humanFeedbackNote, feedbackType);
    }

    // ── Retry AI Draft ──────────────────────────────────────

    /**
     * FAILED projede yeniden AI denemesi yapar.
     * Max 3 deneme. Non-retryable hatalar retry yapılamaz.
     */
    @Transactional
    public AiGenerationResult retryAiDraft(UUID contentProjectId) {
        ContentProject project = findProjectOrThrow(contentProjectId);

        // Statü kontrolü
        if (project.getStatus() != ContentProjectStatus.FAILED) {
            throw new BusinessException(
                    String.format("AI retry yapılamaz. Mevcut statü: %s, Beklenen: FAILED",
                            project.getStatus()));
        }

        // Retry limit kontrolü
        if (project.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new BusinessException(
                    String.format("Maksimum retry sayısına ulaşıldı: %d/%d",
                            project.getRetryCount(), MAX_RETRY_COUNT));
        }

        // Non-retryable hata kontrolü
        if (project.getLastErrorType() != null && !isRetryable(project.getLastErrorType())) {
            throw new BusinessException(
                    String.format("Bu hata tipi retry edilemez: %s", project.getLastErrorType()));
        }

        return executeAiGeneration(project, null, null);
    }

    // ── Core Execution ──────────────────────────────────────

    private AiGenerationResult executeAiGeneration(ContentProject project,
                                                    String humanFeedbackNote,
                                                    String feedbackType) {
        KnowledgeItem ki = project.getKnowledgeItem();
        UUID requestId = UUID.randomUUID();

        // 1. AttemptNumber hesapla
        int attemptNumber = aiGenerationRequestRepository
                .findMaxAttemptNumberByContentProjectId(project.getId())
                .orElse(0) + 1;

        // 2. AiGenerationRequest oluştur
        AiGenerationRequest aiRequest = AiGenerationRequest.builder()
                .contentProject(project)
                .attemptNumber(attemptNumber)
                .status(AiGenerationStatus.REQUESTED)
                .build();
        aiRequest = aiGenerationRequestRepository.save(aiRequest);

        // 3. Project status → AI_REQUESTED
        project.setStatus(ContentProjectStatus.AI_REQUESTED);
        contentProjectRepository.save(project);
        log.info("AI üretim talep edildi: projectId={}, requestId={}, attempt={}",
                project.getId(), requestId, attemptNumber);

        // 4. ContentAgentRequest hazırla
        ContentAgentRequest agentRequest = buildAgentRequest(project, ki, requestId,
                attemptNumber, humanFeedbackNote, feedbackType);

        // 5. Project status → AI_PROCESSING
        project.setStatus(ContentProjectStatus.AI_PROCESSING);
        aiRequest.setStatus(AiGenerationStatus.PROCESSING);
        contentProjectRepository.save(project);
        aiGenerationRequestRepository.save(aiRequest);

        // 6. Gateway çağrısı
        AiGatewayResult gatewayResult = aiContentAgentGateway.generateContentDraft(agentRequest);

        // 7. Sonucu işle
        if (gatewayResult.isSuccess()) {
            return handleSuccess(project, aiRequest, gatewayResult.getResponse());
        } else {
            return handleFailure(project, aiRequest, gatewayResult.getError());
        }
    }

    // ── Success Handler ─────────────────────────────────────

    private AiGenerationResult handleSuccess(ContentProject project,
                                              AiGenerationRequest aiRequest,
                                              ContentAgentResponse response) {
        // MVP Validation
        List<String> validationErrors = validateResponse(response);

        if (!validationErrors.isEmpty()) {
            String errorMsg = "AI yanıt validasyonu başarısız: " + String.join(", ", validationErrors);
            log.warn("Validation hatası: projectId={}, errors={}", project.getId(), validationErrors);

            return markAsFailed(project, aiRequest,
                    AiErrorType.INVALID_AI_RESPONSE_FORMAT, errorMsg);
        }

        // Provider metadata kaydet
        if (response.getProviderMetadata() != null) {
            aiRequest.setProviderName(response.getProviderMetadata().getProviderName());
            aiRequest.setModelName(response.getProviderMetadata().getModelName());
            aiRequest.setDurationMs(response.getProviderMetadata().getDurationMs());
        }

        // AiGenerationRequest → COMPLETED
        aiRequest.setStatus(AiGenerationStatus.COMPLETED);
        aiRequest.setCompletedAt(LocalDateTime.now());
        aiGenerationRequestRepository.save(aiRequest);

        // ContentDraft oluştur
        ContentDraft draft = ContentDraft.builder()
                .contentProject(project)
                .title(response.getTitle())
                .body(response.getBody())
                .summary(response.getSummary())
                .status(ContentDraftStatus.GENERATED)
                .generatedBy(GeneratedBy.AI_CONTENT_AGENT)
                .build();
        draft = contentDraftRepository.save(draft);

        // ContentProject → DRAFT_READY
        project.setStatus(ContentProjectStatus.DRAFT_READY);
        project.setLastErrorType(null);
        project.setLastErrorMessage(null);
        contentProjectRepository.save(project);

        log.info("AI draft başarıyla oluşturuldu: projectId={}, draftId={}",
                project.getId(), draft.getId());

        // FP-013 Task 05: Hand off content project to review (non-fatal)
        generationReviewHandoffAdapter.handoffContentProject(
                project.getId(), project.getTitle(), draft.getSummary());

        return AiGenerationResult.builder()
                .success(true)
                .contentProjectId(project.getId())
                .contentDraftId(draft.getId())
                .projectStatus(project.getStatus().name())
                .draftTitle(draft.getTitle())
                .message("AI draft başarıyla oluşturuldu")
                .build();
    }

    // ── Failure Handler ─────────────────────────────────────

    private AiGenerationResult handleFailure(ContentProject project,
                                              AiGenerationRequest aiRequest,
                                              ContentAgentError error) {
        log.warn("AI üretim hatası: projectId={}, errorType={}, retryable={}",
                project.getId(), error.getErrorType(), error.isRetryable());

        return markAsFailed(project, aiRequest, error.getErrorType(), error.getMessage());
    }

    private AiGenerationResult markAsFailed(ContentProject project,
                                             AiGenerationRequest aiRequest,
                                             AiErrorType errorType,
                                             String errorMessage) {
        // AiGenerationRequest → FAILED
        aiRequest.setStatus(AiGenerationStatus.FAILED);
        aiRequest.setErrorType(errorType);
        aiRequest.setErrorMessage(errorMessage);
        aiRequest.setCompletedAt(LocalDateTime.now());
        aiGenerationRequestRepository.save(aiRequest);

        // ContentProject → FAILED + hata bilgisi + retryCount artır
        project.setStatus(ContentProjectStatus.FAILED);
        project.setLastErrorType(errorType);
        project.setLastErrorMessage(errorMessage);
        project.setRetryCount(project.getRetryCount() + 1);
        contentProjectRepository.save(project);

        return AiGenerationResult.builder()
                .success(false)
                .contentProjectId(project.getId())
                .projectStatus(project.getStatus().name())
                .errorType(errorType.name())
                .message(errorMessage)
                .retryable(isRetryable(errorType))
                .retryCount(project.getRetryCount())
                .maxRetryCount(MAX_RETRY_COUNT)
                .build();
    }

    // ── MVP Validation ──────────────────────────────────────

    private List<String> validateResponse(ContentAgentResponse response) {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();

        // TITLE_REQUIRED
        if (response.getTitle() == null || response.getTitle().isBlank()) {
            errors.add("TITLE_REQUIRED");
        }

        // BODY_REQUIRED
        if (response.getBody() == null || response.getBody().isBlank()) {
            errors.add("BODY_REQUIRED");
        }

        // BODY_MIN_LENGTH_100
        if (response.getBody() != null && !response.getBody().isBlank()
                && response.getBody().length() < MIN_BODY_LENGTH) {
            errors.add("BODY_MIN_LENGTH_100");
        }

        return errors;
    }

    // ── Agent Request Builder ───────────────────────────────

    private ContentAgentRequest buildAgentRequest(ContentProject project,
                                                   KnowledgeItem ki,
                                                   UUID requestId,
                                                   int attemptNumber,
                                                   String humanFeedbackNote,
                                                   String feedbackType) {
        ContentAgentRequest.ContentAgentRequestBuilder builder = ContentAgentRequest.builder()
                .requestId(requestId)
                .projectId(project.getId())
                .knowledgeItem(buildKnowledgeItemPayload(ki))
                .contentPurpose(project.getContentPurpose() != null ? project.getContentPurpose().name() : null)
                .targetAudience(project.getTargetAudience() != null ? project.getTargetAudience().name() : null)
                .tone(project.getTone() != null ? project.getTone().name() : null)
                .language(project.getLanguage())
                .formatHint(project.getFormatHint())
                .lengthHint(project.getLengthHint());

        // Retry metadata (attemptNumber > 1 ise)
        if (attemptNumber > 1) {
            builder.retryMetadata(ContentAgentRetryMetadata.builder()
                    .attemptNumber(attemptNumber)
                    .maxAttempts(MAX_RETRY_COUNT)
                    .previousErrorType(project.getLastErrorType() != null
                            ? project.getLastErrorType().name() : null)
                    .lastAttemptAt(LocalDateTime.now())
                    .manualRetryRequired(project.getRetryCount() >= MAX_RETRY_COUNT)
                    .build());
        }

        // Human feedback (revision durumunda)
        if (humanFeedbackNote != null && !humanFeedbackNote.isBlank()) {
            builder.humanFeedback(HumanFeedbackPayload.builder()
                    .feedbackType(feedbackType != null ? feedbackType : "CUSTOM")
                    .note(humanFeedbackNote)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // Previous draft (varsa)
        contentDraftRepository.findFirstByContentProjectIdOrderByCreatedAtDesc(project.getId())
                .ifPresent(prevDraft -> builder.previousDraft(PreviousDraftPayload.builder()
                        .draftId(prevDraft.getId().toString())
                        .title(prevDraft.getTitle())
                        .body(prevDraft.getBody())
                        .summary(prevDraft.getSummary())
                        .status(prevDraft.getStatus().name())
                        .build()));

        return builder.build();
    }

    private ContentAgentKnowledgeItemPayload buildKnowledgeItemPayload(KnowledgeItem ki) {
        String location = null;
        if (ki.getCity() != null || ki.getDistrict() != null) {
            location = (ki.getCity() != null ? ki.getCity() : "")
                    + (ki.getDistrict() != null ? " / " + ki.getDistrict() : "");
            location = location.trim();
            if (location.startsWith("/")) {
                location = location.substring(1).trim();
            }
        }

        return ContentAgentKnowledgeItemPayload.builder()
                .id(ki.getId())
                .title(ki.getTitle())
                .description(ki.getDescription())
                .category(ki.getCategory())
                .location(location)
                .notes(ki.getNotes())
                .build();
    }

    // ── Retry Logic Helpers ─────────────────────────────────

    private boolean isRetryable(AiErrorType errorType) {
        return switch (errorType) {
            case AI_PROVIDER_TIMEOUT,
                 AI_PROVIDER_UNAVAILABLE,
                 AI_PROVIDER_RATE_LIMIT,
                 AI_PROVIDER_INTERNAL_ERROR,
                 EMPTY_AI_RESPONSE,
                 INVALID_AI_RESPONSE_FORMAT,
                 AI_RESPONSE_TOO_SHORT,
                 UNKNOWN_AI_ERROR -> true;

            case AI_PROVIDER_AUTH_ERROR,
                 AI_PROVIDER_CONFIG_ERROR,
                 AI_RESPONSE_UNSAFE_CONTENT,
                 AI_RESPONSE_LANGUAGE_MISMATCH,
                 AI_RESPONSE_PURPOSE_MISMATCH,
                 PROJECT_CANCELLED,
                 KNOWLEDGE_ITEM_ARCHIVED,
                 HUMAN_REJECTED -> false;
        };
    }

    // ── Common Helpers ──────────────────────────────────────

    private ContentProject findProjectOrThrow(UUID id) {
        return contentProjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ContentProject", id));
    }
}
