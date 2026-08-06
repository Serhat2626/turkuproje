package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private final String characters;

    public TurkuStoryboardScene(int sceneNo, String sceneTitle, String visualDescription,
                                 String narrationSummary, String mood, int estimatedDurationSeconds,
                                 String cameraDirection, String transitionHint, String musicNote) {
        this(sceneNo, sceneTitle, visualDescription, narrationSummary, mood, estimatedDurationSeconds, cameraDirection, transitionHint, musicNote, null);
    }

    @JsonCreator
    public TurkuStoryboardScene(
            @JsonProperty("sceneNo") int sceneNo,
            @JsonProperty("sceneTitle") String sceneTitle,
            @JsonProperty("visualDescription") String visualDescription,
            @JsonProperty("narrationSummary") String narrationSummary,
            @JsonProperty("mood") String mood,
            @JsonProperty("estimatedDurationSeconds") int estimatedDurationSeconds,
            @JsonProperty("cameraDirection") String cameraDirection,
            @JsonProperty("transitionHint") String transitionHint,
            @JsonProperty("musicNote") String musicNote,
            @JsonProperty("characters") String characters) {
        this.sceneNo = sceneNo;
        this.sceneTitle = sceneTitle;
        this.visualDescription = visualDescription;
        this.narrationSummary = narrationSummary;
        this.mood = mood;
        this.estimatedDurationSeconds = estimatedDurationSeconds;
        this.cameraDirection = cameraDirection;
        this.transitionHint = transitionHint;
        this.musicNote = musicNote;
        this.characters = characters;
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
    public String getCharacters() { return characters; }
}
