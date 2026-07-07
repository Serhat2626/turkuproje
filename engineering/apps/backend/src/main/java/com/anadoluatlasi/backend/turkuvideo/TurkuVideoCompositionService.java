package com.anadoluatlasi.backend.turkuvideo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FP-021 Task 02: Composition plan service.
 *
 * <p>Assembles a video composition plan from existing storyboard + scene asset/image batch data.
 * No real video rendering. No lyrics. No export/publish.</p>
 */
public class TurkuVideoCompositionService {

    private final TurkuVideoProjectRepository repository;

    public TurkuVideoCompositionService(TurkuVideoProjectRepository repository) {
        this.repository = repository;
    }

    /**
     * Prepare a composition plan from the existing project data.
     * Requires IMAGE_BATCH_COMPLETED or IMAGE_BATCH_PARTIAL stage.
     */
    public TurkuVideoCompositionPlan prepareCompositionPlan(UUID projectId,
                                                             TurkuCompositionPlanRequest request) {
        TurkuVideoProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Project not found: " + projectId));

        // Validate stage
        TurkuVideoWorkflowStage stage = project.getWorkflowStage();
        if (stage != TurkuVideoWorkflowStage.IMAGE_BATCH_COMPLETED
                && stage != TurkuVideoWorkflowStage.IMAGE_BATCH_PARTIAL) {
            throw new IllegalStateException(
                    "Composition plan requires IMAGE_BATCH_COMPLETED or IMAGE_BATCH_PARTIAL, " +
                    "current stage: " + stage);
        }

        // Require storyboard
        TurkuStoryboardPlan storyboard = project.getStoryboardPlan();
        if (storyboard == null || storyboard.getScenes().isEmpty()) {
            throw new IllegalStateException("Storyboard plan is required for composition");
        }

        // Build composition scenes from storyboard
        List<TurkuVideoCompositionScene> compositionScenes = storyboard.getScenes().stream()
                .map(scene -> buildCompositionScene(scene, project))
                .collect(Collectors.toList());

        // Build narration segments
        List<TurkuNarrationSegment> narrationSegments = storyboard.getScenes().stream()
                .map(this::buildNarrationSegment)
                .collect(Collectors.toList());

        // Build timing plan
        int targetDuration = project.getTargetDurationMinutes() != null
                ? project.getTargetDurationMinutes() * 60
                : storyboard.getTotalEstimatedDurationSeconds();

        Map<Integer, Double> sceneDurations = new LinkedHashMap<>();
        double totalDuration = 0;
        for (TurkuStoryboardScene scene : storyboard.getScenes()) {
            double d = scene.getEstimatedDurationSeconds();
            sceneDurations.put(scene.getSceneNo(), d);
            totalDuration += d;
        }
        boolean hasConflict = Math.abs(totalDuration - targetDuration) > targetDuration * 0.2;
        TurkuSceneTimingPlan timingPlan = new TurkuSceneTimingPlan(
                totalDuration, targetDuration, sceneDurations, hasConflict);

        // Build transition plan
        List<TurkuTransitionPlan.TransitionDirective> transitions = new ArrayList<>();
        List<TurkuStoryboardScene> scenes = storyboard.getScenes();
        for (int i = 0; i < scenes.size() - 1; i++) {
            transitions.add(new TurkuTransitionPlan.TransitionDirective(
                    scenes.get(i).getSceneNo(),
                    scenes.get(i + 1).getSceneNo(),
                    scenes.get(i).getTransitionHint() != null
                            ? scenes.get(i).getTransitionHint() : "CROSS_FADE",
                    500));
        }
        TurkuTransitionPlan transitionPlan = new TurkuTransitionPlan(transitions, "CROSS_FADE");

        // Review flags
        List<String> reviewFlags = new ArrayList<>();
        reviewFlags.add("HUMAN_REVIEW_REQUIRED");
        reviewFlags.add("COPYRIGHT_REVIEW_REQUIRED");
        reviewFlags.add("LYRICS_NOT_INCLUDED");
        reviewFlags.add("NO_RENDER_PRODUCED");
        if (stage == TurkuVideoWorkflowStage.IMAGE_BATCH_PARTIAL) {
            reviewFlags.add("IMAGE_BATCH_PARTIAL");
        }
        if (hasConflict) {
            reviewFlags.add("TIMING_CONFLICT");
        }

        // Build composition plan
        TurkuVideoCompositionPlan plan = new TurkuVideoCompositionPlan(
                UUID.randomUUID(), projectId,
                (int) totalDuration, compositionScenes, narrationSegments,
                timingPlan, transitionPlan, reviewFlags);

        // Advance stage
        project.setCompositionPlan(plan);
        project.advanceTo(TurkuVideoWorkflowStage.COMPOSITION_PLAN_PREPARED);
        repository.save(project);

        return plan;
    }

    private TurkuVideoCompositionScene buildCompositionScene(TurkuStoryboardScene scene,
                                                               TurkuVideoProject project) {
        // Derive safe image ref from image batch if available
        String imageRef = null;
        if (project.getImageBatch() != null
                && project.getImageBatch().getImageResults() != null) {
            imageRef = project.getImageBatch().getImageResults().stream()
                    .filter(r -> r.getSceneNo() == scene.getSceneNo())
                    .findFirst()
                    .map(r -> "internal-ref:" + r.getSafeInternalRef())
                    .orElse(null);
        }

        return new TurkuVideoCompositionScene(
                scene.getSceneNo(),
                scene.getSceneTitle(),
                scene.getVisualDescription(),
                scene.getNarrationSummary(),
                scene.getMood(),
                scene.getEstimatedDurationSeconds(),
                imageRef,
                scene.getTransitionHint() != null ? scene.getTransitionHint() : "CROSS_FADE",
                "CROSS_FADE",
                scene.getCameraDirection());
    }

    private TurkuNarrationSegment buildNarrationSegment(TurkuStoryboardScene scene) {
        return new TurkuNarrationSegment(
                scene.getSceneNo(),
                scene.getSceneNo(),
                scene.getNarrationSummary(),
                scene.getEstimatedDurationSeconds(),
                "documentary",
                "tr",
                false);
    }
}
