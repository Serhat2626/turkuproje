package com.anadoluatlasi.backend.turkuvideo;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-020 Task 04: Provider-backed image batch execution service.
 *
 * <p>Runs image generation for each scene through TurkuGenerationProviderGateway.
 * Every item wrapped in safety envelope. Policy enforced pre/post.
 * Output sanitized. Opaque internal refs only.</p>
 *
 * <p>No base64, no public/signed/CDN URLs, no storage keys, no raw provider data.</p>
 */
@Service
public class TurkuProviderImageBatchService {

    private final TurkuVideoProjectRepository repository;
    private final TurkuGenerationProviderGateway gateway;
    private final TurkuProviderSafetySanitizer sanitizer;
    private final TurkuProviderSafetyPolicy safetyPolicy;

    public TurkuProviderImageBatchService(
            TurkuVideoProjectRepository repository,
            TurkuGenerationProviderGateway gateway,
            TurkuProviderSafetySanitizer sanitizer,
            TurkuProviderSafetyPolicy safetyPolicy) {
        this.repository = repository;
        this.gateway = gateway;
        this.sanitizer = sanitizer;
        this.safetyPolicy = safetyPolicy;
    }

    public TurkuImageBatchRunResponse runImageBatch(UUID projectId,
                                                     TurkuImageBatchRunRequest request) {

        TurkuVideoProject project = getProject(projectId);

        // Validate stage — must have image batch prepared
        if (project.getWorkflowStage() != TurkuVideoWorkflowStage.IMAGE_BATCH_PREPARED) {
            throw TurkuVideoProjectException.invalidStageTransition(
                    project.getWorkflowStage(),
                    TurkuVideoWorkflowStage.IMAGE_BATCH_PREPARED);
        }

        if (project.getImageBatch() == null || project.getImageBatch().getImageRequests().isEmpty()) {
            throw TurkuVideoProjectException.missingPrerequisite(
                    "Görsel batch hazırlanmalıdır (prepare-image-batch)");
        }

        TurkuProviderMode mode = safetyPolicy.resolveProviderMode(project);

        // Check mode blocking
        TurkuImageBatchRunResponse modeBlock = checkModeBlocking(projectId, mode,
                project.getImageBatch());
        if (modeBlock != null) return modeBlock;

        // Sanitize request input
        sanitizeRequest(request);

        TurkuSceneImageBatch batch = project.getImageBatch();
        List<TurkuImageBatchRunSceneStatus> sceneStatuses = new ArrayList<>();
        List<TurkuSceneImageResult> imageResults = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        TurkuProviderTraceReference lastTrace = null;

        // Run each scene through gateway
        for (TurkuSceneImageRequest sceneReq : batch.getImageRequests()) {

            TurkuGenerationSafetyEnvelope envelope = TurkuGenerationSafetyEnvelope.createDefault(
                    projectId, "IMAGE_GENERATION", mode);
            safetyPolicy.enforcePreGenerationPolicy(envelope);

            TurkuImageGenerationRequest gatewayReq = TurkuImageGenerationRequest.builder()
                    .projectId(projectId)
                    .sceneNo(sceneReq.getSceneNo())
                    .safePromptSummary(sceneReq.getSafePromptSummary())
                    .safeNegativeNotes(sceneReq.getSafeNegativeNotes())
                    .styleHint(sceneReq.getStyleHint())
                    .aspectRatioHint(sceneReq.getAspectRatioHint())
                    .build();

            TurkuImageGenerationResult result = gateway.generateImage(gatewayReq, envelope);

            sanitizer.sanitizeEnvelope(envelope);
            safetyPolicy.enforcePostGenerationPolicy(envelope);

            lastTrace = result.getTraceRef();

            TurkuImageBatchRunSceneStatus sceneStatus = TurkuImageBatchRunSceneStatus.builder()
                    .sceneNo(sceneReq.getSceneNo())
                    .success(result.isSuccess())
                    .safeInternalRef(result.getSafeInternalRef())
                    .safeErrorCode(result.getSafeErrorCode())
                    .needsHumanReview(result.isNeedsHumanReview())
                    .build();
            sceneStatuses.add(sceneStatus);

            imageResults.add(new TurkuSceneImageResult(
                    sceneReq.getSceneNo(),
                    result.getSafeInternalRef(),
                    result.isSuccess(),
                    result.getSafeErrorCode()));

            if (result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        // Update batch and project state
        batch.setImageResults(imageResults);

        TurkuImageBatchStatus batchStatus;
        TurkuVideoWorkflowStage newStage;
        if (failureCount == 0) {
            batchStatus = TurkuImageBatchStatus.COMPLETED;
            newStage = TurkuVideoWorkflowStage.IMAGE_BATCH_COMPLETED;
        } else if (successCount > 0) {
            batchStatus = TurkuImageBatchStatus.PARTIAL;
            newStage = TurkuVideoWorkflowStage.IMAGE_BATCH_PARTIAL;
        } else {
            batchStatus = TurkuImageBatchStatus.FAILED;
            newStage = TurkuVideoWorkflowStage.IMAGE_BATCH_PARTIAL;
        }
        batch.setStatus(batchStatus);
        project.advanceTo(newStage);
        repository.save(project);

        List<String> flags = new ArrayList<>(List.of(
                "HUMAN_REVIEW_REQUIRED",
                "COPYRIGHT_REVIEW_REQUIRED",
                "LYRICS_NOT_INCLUDED"));
        if (failureCount > 0) flags.add("PARTIAL_FAILURE");

        return TurkuImageBatchRunResponse.builder()
                .projectId(projectId)
                .batchId(batch.getBatchId())
                .providerMode(mode)
                .batchStatus(batchStatus)
                .currentStage(newStage)
                .safeOutputSummary("Batch çalıştırıldı: " + successCount + " başarılı, "
                        + failureCount + " başarısız, toplam " + batch.getImageRequests().size())
                .traceRef(lastTrace)
                .requestCount(batch.getImageRequests().size())
                .successCount(successCount)
                .failureCount(failureCount)
                .humanReviewRequired(true)
                .autoApproved(false)
                .copyrightReviewRequired(true)
                .sceneStatuses(sceneStatuses)
                .reviewFlags(flags)
                .safeWarnings(failureCount > 0
                        ? List.of("Bazı sahneler üretilemedi: " + failureCount + " başarısız")
                        : List.of())
                .build();
    }

    // ── Private Helpers ─────────────────────────────────────

    private TurkuVideoProject getProject(UUID id) {
        return repository.findById(id)
                .orElseThrow(TurkuVideoProjectException::projectNotFound);
    }

    private TurkuImageBatchRunResponse checkModeBlocking(
            UUID projectId, TurkuProviderMode mode, TurkuSceneImageBatch batch) {

        if (mode == TurkuProviderMode.PROVIDER_DISABLED
                || mode == TurkuProviderMode.PROVIDER_FAILED_SAFE
                || mode == TurkuProviderMode.HUMAN_REVIEW_REQUIRED) {

            String failureCode = switch (mode) {
                case PROVIDER_DISABLED -> "PROVIDER_DISABLED";
                case PROVIDER_FAILED_SAFE -> "PROVIDER_FAILED_SAFE";
                case HUMAN_REVIEW_REQUIRED -> "HUMAN_REVIEW_PENDING";
                default -> "UNKNOWN";
            };

            return TurkuImageBatchRunResponse.builder()
                    .projectId(projectId)
                    .batchId(batch.getBatchId())
                    .providerMode(mode)
                    .batchStatus(TurkuImageBatchStatus.BLOCKED)
                    .safeOutputSummary("Sağlayıcı modu üretim için uygun değil: " + mode)
                    .safeFailureCode(failureCode)
                    .requestCount(batch.getImageRequests().size())
                    .successCount(0)
                    .failureCount(0)
                    .humanReviewRequired(true)
                    .autoApproved(false)
                    .copyrightReviewRequired(true)
                    .sceneStatuses(List.of())
                    .reviewFlags(List.of("HUMAN_REVIEW_REQUIRED", "COPYRIGHT_REVIEW_REQUIRED"))
                    .safeWarnings(List.of("Batch engellendi: " + mode))
                    .build();
        }
        return null;
    }

    private void sanitizeRequest(TurkuImageBatchRunRequest request) {
        if (request == null) return;
        if (request.getStyleOverrideHint() != null) {
            if (sanitizer.containsPromptInjection(request.getStyleOverrideHint())) {
                throw new TurkuVideoProjectException(
                        "Prompt injection tespit edildi", "PROMPT_INJECTION_DETECTED");
            }
            if (sanitizer.containsForbiddenPattern(request.getStyleOverrideHint())) {
                throw new TurkuVideoProjectException(
                        "Güvenlik ihlali: yasaklı desen tespit edildi", "UNSAFE_INPUT");
            }
        }
    }
}
