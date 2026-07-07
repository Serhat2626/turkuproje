package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-021 Task 04: Single scene in composition preview.
 * Safe internal refs only — no public URL, no base64, no storage key.
 */
public final class TurkuCompositionPreviewScene {

    private final int sceneNo;
    private final String sceneTitle;
    private final String narrationSummary;
    private final String imageRefLabel;
    private final double durationSeconds;
    private final String mood;
    private final String transitionIn;
    private final String transitionOut;
    private final String cameraDirection;

    public TurkuCompositionPreviewScene(int sceneNo, String sceneTitle, String narrationSummary,
                                         String imageRefLabel, double durationSeconds,
                                         String mood, String transitionIn, String transitionOut,
                                         String cameraDirection) {
        this.sceneNo = sceneNo;
        this.sceneTitle = sceneTitle;
        this.narrationSummary = narrationSummary;
        this.imageRefLabel = imageRefLabel;
        this.durationSeconds = durationSeconds;
        this.mood = mood;
        this.transitionIn = transitionIn;
        this.transitionOut = transitionOut;
        this.cameraDirection = cameraDirection;
    }

    public int getSceneNo() { return sceneNo; }
    public String getSceneTitle() { return sceneTitle; }
    public String getNarrationSummary() { return narrationSummary; }
    public String getImageRefLabel() { return imageRefLabel; }
    public double getDurationSeconds() { return durationSeconds; }
    public String getMood() { return mood; }
    public String getTransitionIn() { return transitionIn; }
    public String getTransitionOut() { return transitionOut; }
    public String getCameraDirection() { return cameraDirection; }
}
