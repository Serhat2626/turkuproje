package com.anadoluatlasi.backend.turkuvideo;

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

    public TurkuVideoCompositionScene(int sceneNo, String sceneTitle, String visualDescription,
                                       String narrationText, String mood, double durationSeconds,
                                       String imageRef, String transitionIn, String transitionOut,
                                       String cameraDirection) {
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
