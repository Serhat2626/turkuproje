package com.anadoluatlasi.backend.turkuvideo;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-019 Task 04: Asset planning service — scene asset plan + image batch.
 *
 * <p>Uses deterministic internal generation (stubs). No external image provider.
 * Provider-ready seams available for future integration.</p>
 *
 * <p>No base64, no public URLs, no storage keys, no raw provider data.</p>
 */
@Service
public class TurkuVideoAssetPlanningService {

    private final TurkuVideoProjectRepository repository;

    public TurkuVideoAssetPlanningService(TurkuVideoProjectRepository repository) {
        this.repository = repository;
    }

    // ── Scene Asset Plan ────────────────────────────────────

    public TurkuSceneAssetPlan prepareSceneAssets(UUID projectId,
                                                   TurkuSceneAssetPreparationRequest request) {
        TurkuVideoProject project = getProject(projectId);
        validateStage(project, TurkuVideoWorkflowStage.STORYBOARD_PREPARED,
                TurkuVideoWorkflowStage.SCENE_ASSET_PLAN_PREPARED);

        if (project.getStoryboardPlan() == null || project.getStoryboardPlan().getScenes().isEmpty()) {
            throw TurkuVideoProjectException.missingPrerequisite("Storyboard sahneleri mevcut olmalıdır");
        }

        if (request != null) {
            validateOptionalHint(request.getStyleOverrideHint(), "styleOverrideHint", 200);
            validateOptionalHint(request.getAspectRatioHint(), "aspectRatioHint", 50);
        }

        String styleHint = (request != null && request.getStyleOverrideHint() != null)
                ? request.getStyleOverrideHint()
                : (project.getVisualStyle() != null ? project.getVisualStyle() : "Sinematik Anadolu");

        String aspectRatio = (request != null && request.getAspectRatioHint() != null)
                ? request.getAspectRatioHint() : "16:9";

        List<TurkuSceneAssetItem> items = new ArrayList<>();
        for (TurkuStoryboardScene scene : project.getStoryboardPlan().getScenes()) {
            items.add(new TurkuSceneAssetItem(
                    scene.getSceneNo(),
                    "Sahne " + scene.getSceneNo() + " görseli",
                    buildVisualPromptSummary(scene, styleHint),
                    "Gerçek kişi portresi kullanma, telif ihlali riski olan logo/marka kullanma",
                    styleHint,
                    scene.getMood(),
                    aspectRatio,
                    true, // needsHumanReview
                    true  // copyrightReviewRequired
            ));
        }

        List<String> reviewFlags = buildAssetReviewFlags(project);

        TurkuSceneAssetPlan plan = new TurkuSceneAssetPlan(
                UUID.randomUUID(), projectId, items, reviewFlags);

        project.setSceneAssetPlan(plan);
        project.advanceTo(TurkuVideoWorkflowStage.SCENE_ASSET_PLAN_PREPARED);
        repository.save(project);

        return plan;
    }

    // ── Image Batch Preparation ─────────────────────────────

    public TurkuSceneImageBatch prepareImageBatch(UUID projectId,
                                                    TurkuImageBatchPreparationRequest request) {
        TurkuVideoProject project = getProject(projectId);
        validateStage(project, TurkuVideoWorkflowStage.SCENE_ASSET_PLAN_PREPARED,
                TurkuVideoWorkflowStage.IMAGE_BATCH_PREPARED);

        if (project.getSceneAssetPlan() == null || project.getSceneAssetPlan().getItems().isEmpty()) {
            throw TurkuVideoProjectException.missingPrerequisite("Sahne varlık planı mevcut olmalıdır");
        }

        if (request != null) {
            validateOptionalHint(request.getProviderHint(), "providerHint", 100);
        }

        // Deterministic stub — create image requests from asset items
        List<TurkuSceneImageRequest> imageRequests = new ArrayList<>();
        for (TurkuSceneAssetItem item : project.getSceneAssetPlan().getItems()) {
            imageRequests.add(new TurkuSceneImageRequest(
                    item.getSceneNo(),
                    item.getVisualPromptSummary(),
                    item.getNegativePromptSafetyNotes(),
                    item.getStyleHint(),
                    item.getAspectRatioHint()
            ));
        }

        List<String> reviewFlags = new ArrayList<>();
        reviewFlags.add("LYRICS_NOT_INCLUDED");
        reviewFlags.add("COPYRIGHT_REVIEW_REQUIRED");
        reviewFlags.add("HUMAN_REVIEW_REQUIRED");
        reviewFlags.add("PUBLIC_DELIVERY_BLOCKED");

        TurkuSceneImageBatch batch = new TurkuSceneImageBatch(
                UUID.randomUUID(), projectId,
                TurkuImageBatchStatus.PREPARED,
                imageRequests, reviewFlags);

        // Deterministic stub — generate safe placeholder results
        List<TurkuSceneImageResult> stubResults = new ArrayList<>();
        for (TurkuSceneImageRequest req : imageRequests) {
            stubResults.add(new TurkuSceneImageResult(
                    req.getSceneNo(),
                    "internal-ref:" + UUID.randomUUID().toString().substring(0, 8),
                    true,
                    null
            ));
        }
        batch.setImageResults(stubResults);

        project.setImageBatch(batch);
        project.advanceTo(TurkuVideoWorkflowStage.IMAGE_BATCH_PREPARED);
        repository.save(project);

        return batch;
    }

    // ── Image Batch Completion ───────────────────────────────

    public TurkuSceneImageBatch completeImageBatch(UUID projectId,
                                                     TurkuImageBatchCompletionRequest request) {
        TurkuVideoProject project = getProject(projectId);
        validateStage(project, TurkuVideoWorkflowStage.IMAGE_BATCH_PREPARED,
                request != null && request.isPartial()
                        ? TurkuVideoWorkflowStage.IMAGE_BATCH_PARTIAL
                        : TurkuVideoWorkflowStage.IMAGE_BATCH_COMPLETED);

        if (project.getImageBatch() == null) {
            throw TurkuVideoProjectException.missingPrerequisite("Görsel üretim batch'i mevcut olmalıdır");
        }

        if (request != null) {
            validateOptionalHint(request.getSafeCompletionNote(), "safeCompletionNote", 500);
        }

        TurkuSceneImageBatch batch = project.getImageBatch();

        if (request != null && request.isPartial()) {
            batch.setStatus(TurkuImageBatchStatus.PARTIAL);
            project.advanceTo(TurkuVideoWorkflowStage.IMAGE_BATCH_PARTIAL);
        } else {
            batch.setStatus(TurkuImageBatchStatus.COMPLETED);
            project.advanceTo(TurkuVideoWorkflowStage.IMAGE_BATCH_COMPLETED);
        }

        repository.save(project);
        return batch;
    }

    // ── Validation Helpers ──────────────────────────────────

    private TurkuVideoProject getProject(UUID id) {
        return repository.findById(id)
                .orElseThrow(TurkuVideoProjectException::projectNotFound);
    }

    private void validateStage(TurkuVideoProject project,
                                TurkuVideoWorkflowStage required,
                                TurkuVideoWorkflowStage target) {
        if (project.getWorkflowStage() != required) {
            throw TurkuVideoProjectException.invalidStageTransition(
                    project.getWorkflowStage(), target);
        }
    }

    private void validateOptionalHint(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) return;
        if (value.length() > maxLength) {
            throw TurkuVideoProjectException.fieldTooLong(fieldName, maxLength);
        }
        if (value.matches("(?i).*(javascript:|<script|on\\w+=|password|secret|token|bearer|authorization|eyJ[A-Za-z0-9]|data:image|base64,).*")) {
            throw TurkuVideoProjectException.unsafeInput(fieldName);
        }
    }

    private String buildVisualPromptSummary(TurkuStoryboardScene scene, String style) {
        return style + " tarzında, " + scene.getMood() + " atmosferinde " +
                scene.getVisualDescription() + " — Sahne " + scene.getSceneNo();
    }

    private List<String> buildAssetReviewFlags(TurkuVideoProject project) {
        List<String> flags = new ArrayList<>();
        flags.add("LYRICS_NOT_INCLUDED");
        flags.add("COPYRIGHT_REVIEW_REQUIRED");
        flags.add("HUMAN_REVIEW_REQUIRED");
        flags.add("PUBLIC_DELIVERY_BLOCKED");
        if (project.getSourceConfidence() == TurkuSourceConfidence.UNKNOWN
                || project.getSourceConfidence() == TurkuSourceConfidence.LOW) {
            flags.add("SOURCE_REVIEW_REQUIRED");
        }
        flags.add("CULTURAL_CONTEXT_REVIEW_REQUIRED");
        return flags;
    }
}
