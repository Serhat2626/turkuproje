package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-019 Task 03: Single storyboard scene. No public URLs, no media data.
 */
public final class TurkuStoryboardScene {

    private final int sceneNo;
    private final String sceneTitle;
    private final String visualDescription;
    private final String narrationSummary;
    private final String mood;
    private final int estimatedDurationSeconds;
    private final String cameraDirection;
    private final String transitionHint;
    private final String musicNote;

    public TurkuStoryboardScene(int sceneNo, String sceneTitle, String visualDescription,
                                 String narrationSummary, String mood, int estimatedDurationSeconds,
                                 String cameraDirection, String transitionHint, String musicNote) {
        this.sceneNo = sceneNo;
        this.sceneTitle = sceneTitle;
        this.visualDescription = visualDescription;
        this.narrationSummary = narrationSummary;
        this.mood = mood;
        this.estimatedDurationSeconds = estimatedDurationSeconds;
        this.cameraDirection = cameraDirection;
        this.transitionHint = transitionHint;
        this.musicNote = musicNote;
    }

    public int getSceneNo() { return sceneNo; }
    public String getSceneTitle() { return sceneTitle; }
    public String getVisualDescription() { return visualDescription; }
    public String getNarrationSummary() { return narrationSummary; }
    public String getMood() { return mood; }
    public int getEstimatedDurationSeconds() { return estimatedDurationSeconds; }
    public String getCameraDirection() { return cameraDirection; }
    public String getTransitionHint() { return transitionHint; }
    public String getMusicNote() { return musicNote; }
}
