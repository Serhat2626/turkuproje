package com.anadoluatlasi.backend.turkuvideo;

/**
 * RP-004: Single timeline item — safe metadata only.
 */
public final class TurkuAnimaticTimelineItem {
    private final int sceneNumber;
    private final int startMs;
    private final int durationMs;
    private final String transitionType;
    private final String cameraMovement;
    private final String visualPacingSummary;
    private final String narrativeBeatSummary;

    public TurkuAnimaticTimelineItem(int sceneNumber, int startMs, int durationMs,
                                      String transitionType, String cameraMovement,
                                      String visualPacingSummary, String narrativeBeatSummary) {
        this.sceneNumber = sceneNumber;
        this.startMs = startMs;
        this.durationMs = durationMs;
        this.transitionType = transitionType;
        this.cameraMovement = cameraMovement;
        this.visualPacingSummary = visualPacingSummary;
        this.narrativeBeatSummary = narrativeBeatSummary;
    }

    public int getSceneNumber() { return sceneNumber; }
    public int getStartMs() { return startMs; }
    public int getDurationMs() { return durationMs; }
    public String getTransitionType() { return transitionType; }
    public String getCameraMovement() { return cameraMovement; }
    public String getVisualPacingSummary() { return visualPacingSummary; }
    public String getNarrativeBeatSummary() { return narrativeBeatSummary; }
}
