package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 03: Request DTO for scenario preparation.
 */
public final class TurkuScenarioPreparationRequest {
    private String narrationToneHint;
    private Integer sceneCountHint;

    public TurkuScenarioPreparationRequest() {}
    public String getNarrationToneHint() { return narrationToneHint; }
    public void setNarrationToneHint(String v) { this.narrationToneHint = v; }
    public Integer getSceneCountHint() { return sceneCountHint; }
    public void setSceneCountHint(Integer v) { this.sceneCountHint = v; }
}
