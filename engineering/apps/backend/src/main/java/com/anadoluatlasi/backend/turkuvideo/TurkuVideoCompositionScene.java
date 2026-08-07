package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FP-021 Task 02: Single scene in the video composition timeline.
 * Safe internal image ref only — no public URL, no base64, no storage key.
 */
public final class TurkuVideoCompositionScene {

    private final int sceneNo;
    private final String sceneTitle;
    private final String visualDescription;
    private final String narrationText;
    private final String mood;
    private final double durationSeconds;
    private final String imageRef;
    private final String transitionIn;
    private final String transitionOut;
    private final String cameraDirection;

    @JsonCreator
    public TurkuVideoCompositionScene(
            @JsonProperty("sceneNo") int sceneNo,
            @JsonProperty("sceneTitle") String sceneTitle,
            @JsonProperty("visualDescription") String visualDescription,
            @JsonProperty("narrationText") String narrationText,
            @JsonProperty("mood") String mood,
            @JsonProperty("durationSeconds") double durationSeconds,
            @JsonProperty("imageRef") String imageRef,
            @JsonProperty("transitionIn") String transitionIn,
            @JsonProperty("transitionOut") String transitionOut,
            @JsonProperty("cameraDirection") String cameraDirection) {
        this.sceneNo = sceneNo;
        this.sceneTitle = sceneTitle;
        this.visualDescription = visualDescription;
        this.narrationText = narrationText;
        this.mood = mood;
        this.durationSeconds = durationSeconds;
        this.imageRef = imageRef;
        this.transitionIn = transitionIn;
        this.transitionOut = transitionOut;
        this.cameraDirection = cameraDirection;
    }

    public int getSceneNo() { return sceneNo; }
    public String getSceneTitle() { return sceneTitle; }
    public String getVisualDescription() { return visualDescription; }
    public String getNarrationText() { return narrationText; }
    public String getMood() { return mood; }
    public double getDurationSeconds() { return durationSeconds; }
    public String getImageRef() { return imageRef; }
    public String getTransitionIn() { return transitionIn; }
    public String getTransitionOut() { return transitionOut; }
    public String getCameraDirection() { return cameraDirection; }
}
