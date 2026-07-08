package com.anadoluatlasi.backend.turkuvideo;

/**
 * RP-002: A single scene in a safe storyboard draft.
 * Metadata-only: purpose, mood, visual direction. No lyrics/prompt/script.
 */
public final class TurkuStoryboardDraftScene {
    private final int sceneNumber;
    private final String purpose;
    private final String mood;
    private final String visualDirection;
    private final int durationMs;

    public TurkuStoryboardDraftScene(int sceneNumber, String purpose, String mood,
                                      String visualDirection, int durationMs) {
        this.sceneNumber = sceneNumber;
        this.purpose = purpose;
        this.mood = mood;
        this.visualDirection = visualDirection;
        this.durationMs = durationMs;
    }

    public int getSceneNumber() { return sceneNumber; }
    public String getPurpose() { return purpose; }
    public String getMood() { return mood; }
    public String getVisualDirection() { return visualDirection; }
    public int getDurationMs() { return durationMs; }
}
