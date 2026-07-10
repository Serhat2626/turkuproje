package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * Türkü video projesi üretilen içerik yanıtı.
 * Hikâye, senaryo ve storyboard içeriğini döndürür.
 */
public final class TurkuVideoContentResponse {

    private final UUID projectId;
    private final String turkuName;
    private final String workflowStage;

    // Hikâye
    private final StoryContent story;

    // Senaryo
    private final ScenarioContent scenario;

    // Storyboard
    private final StoryboardContent storyboard;

    private TurkuVideoContentResponse(UUID projectId, String turkuName, String workflowStage,
                                       StoryContent story, ScenarioContent scenario,
                                       StoryboardContent storyboard) {
        this.projectId = projectId;
        this.turkuName = turkuName;
        this.workflowStage = workflowStage;
        this.story = story;
        this.scenario = scenario;
        this.storyboard = storyboard;
    }

    public static TurkuVideoContentResponse from(TurkuVideoProject project) {
        StoryContent story = null;
        if (project.getStoryPackage() != null) {
            TurkuStoryPackage sp = project.getStoryPackage();
            story = new StoryContent(
                    sp.getStorySummary(),
                    sp.getOriginSummary(),
                    sp.getEmotionalTheme(),
                    sp.getCulturalSignificance(),
                    sp.getNarrativeAngle(),
                    sp.getRegionContext(),
                    sp.getSourceConfidence() != null ? sp.getSourceConfidence().name() : "UNKNOWN",
                    sp.getReviewFlags()
            );
        }

        ScenarioContent scenario = null;
        if (project.getScenarioPlan() != null) {
            TurkuScenarioPlan sc = project.getScenarioPlan();
            scenario = new ScenarioContent(
                    sc.getTitle(),
                    sc.getLogline(),
                    sc.getNarrationTone(),
                    sc.getTargetDurationHintSeconds() != null ? sc.getTargetDurationHintSeconds() : 0,
                    sc.getScenarioBeats(),
                    sc.getReviewFlags()
            );
        }

        StoryboardContent storyboard = null;
        if (project.getStoryboardPlan() != null) {
            TurkuStoryboardPlan sb = project.getStoryboardPlan();
            List<SceneContent> scenes = sb.getScenes().stream()
                    .map(s -> new SceneContent(
                            s.getSceneNo(),
                            s.getSceneTitle(),
                            s.getVisualDescription(),
                            s.getNarrationSummary(),
                            s.getMood(),
                            s.getEstimatedDurationSeconds(),
                            s.getCameraDirection(),
                            s.getTransitionHint(),
                            s.getMusicNote()
                    )).toList();
            storyboard = new StoryboardContent(
                    scenes,
                    sb.getTotalEstimatedDurationSeconds(),
                    sb.getReviewFlags()
            );
        }

        return new TurkuVideoContentResponse(
                project.getId(),
                project.getTurkuName(),
                project.getWorkflowStage() != null ? project.getWorkflowStage().name() : "UNKNOWN",
                story, scenario, storyboard
        );
    }

    // Getters
    public UUID getProjectId() { return projectId; }
    public String getTurkuName() { return turkuName; }
    public String getWorkflowStage() { return workflowStage; }
    public StoryContent getStory() { return story; }
    public ScenarioContent getScenario() { return scenario; }
    public StoryboardContent getStoryboard() { return storyboard; }

    // ── Inner DTOs ──

    public record StoryContent(
            String storySummary,
            String originSummary,
            String emotionalTheme,
            String culturalSignificance,
            String narrativeAngle,
            String regionContext,
            String sourceConfidence,
            List<String> reviewFlags
    ) {}

    public record ScenarioContent(
            String title,
            String logline,
            String narrationTone,
            int targetDurationSeconds,
            List<String> scenarioBeats,
            List<String> reviewFlags
    ) {}

    public record StoryboardContent(
            List<SceneContent> scenes,
            int totalDurationSeconds,
            List<String> reviewFlags
    ) {}

    public record SceneContent(
            int sceneNo,
            String sceneTitle,
            String visualDescription,
            String narrationSummary,
            String mood,
            int durationSeconds,
            String cameraDirection,
            String transitionHint,
            String musicNote
    ) {}
}
