package com.anadoluatlasi.backend.turkuvideo;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * FP-019 Task 02: Progress response DTO. Safe stage/progress data only.
 */
public final class TurkuVideoProgressResponse {
    private final UUID id;
    private final TurkuVideoWorkflowStage currentStage;
    private final List<TurkuVideoWorkflowStage> completedStages;
    private final String nextAvailableAction;
    private final String safeProgressSummary;

    public TurkuVideoProgressResponse(UUID id, TurkuVideoWorkflowStage currentStage,
                                       List<TurkuVideoWorkflowStage> completedStages,
                                       String nextAvailableAction, String safeProgressSummary) {
        this.id = id;
        this.currentStage = currentStage;
        this.completedStages = completedStages;
        this.nextAvailableAction = nextAvailableAction;
        this.safeProgressSummary = safeProgressSummary;
    }

    public UUID getId() { return id; }
    public TurkuVideoWorkflowStage getCurrentStage() { return currentStage; }
    public List<TurkuVideoWorkflowStage> getCompletedStages() { return completedStages; }
    public String getNextAvailableAction() { return nextAvailableAction; }
    public String getSafeProgressSummary() { return safeProgressSummary; }

    public static TurkuVideoProgressResponse from(TurkuVideoProject project) {
        TurkuVideoWorkflowStage stage = project.getWorkflowStage();
        List<TurkuVideoWorkflowStage> completed = computeCompleted(stage);
        String next = computeNextAction(stage);
        String summary = "Aşama: " + stage + " (" + completed.size() + " aşama tamamlandı)";
        return new TurkuVideoProgressResponse(project.getId(), stage, completed, next, summary);
    }

    private static List<TurkuVideoWorkflowStage> computeCompleted(TurkuVideoWorkflowStage current) {
        TurkuVideoWorkflowStage[] ordered = {
                TurkuVideoWorkflowStage.DRAFT_CREATED,
                TurkuVideoWorkflowStage.STORY_CONTEXT_PREPARED,
                TurkuVideoWorkflowStage.SCENARIO_PREPARED,
                TurkuVideoWorkflowStage.STORYBOARD_PREPARED,
                TurkuVideoWorkflowStage.SCENE_ASSET_PLAN_PREPARED,
                TurkuVideoWorkflowStage.IMAGE_BATCH_PREPARED,
                TurkuVideoWorkflowStage.IMAGE_BATCH_COMPLETED,
                TurkuVideoWorkflowStage.COMPOSITION_PLAN_PREPARED,
                TurkuVideoWorkflowStage.RENDER_MANIFEST_PREPARED,
                TurkuVideoWorkflowStage.INTERNAL_PREVIEW_READY
        };
        int idx = Arrays.asList(ordered).indexOf(current);
        if (idx <= 0) return List.of();
        return List.of(Arrays.copyOfRange(ordered, 0, idx));
    }

    private static String computeNextAction(TurkuVideoWorkflowStage stage) {
        return switch (stage) {
            case DRAFT_CREATED -> "prepare-story";
            case STORY_CONTEXT_PREPARED -> "prepare-scenario";
            case SCENARIO_PREPARED -> "prepare-storyboard";
            case STORYBOARD_PREPARED -> "prepare-scene-assets";
            case SCENE_ASSET_PLAN_PREPARED -> "prepare-image-batch";
            case IMAGE_BATCH_PREPARED, IMAGE_BATCH_PARTIAL -> "İlerleme bekleniyor";
            case IMAGE_BATCH_COMPLETED -> "prepare-composition-plan";
            case COMPOSITION_PLAN_PREPARED -> "prepare-render-manifest";
            case RENDER_MANIFEST_PREPARED -> "Render manifest hazır — hazırlık kontrolü";
            case INTERNAL_PREVIEW_READY -> "Tamamlandı — dahili önizleme hazır";
            case NEEDS_REVISION -> "Revizyon gerekli";
            case BLOCKED -> "Engellendi";
            case ARCHIVED -> "Arşivlendi";
        };
    }
}
