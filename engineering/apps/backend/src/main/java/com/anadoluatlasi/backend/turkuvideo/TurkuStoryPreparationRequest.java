package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 03: Request DTO for story preparation.
 * Optional narrative hints from admin. No lyrics, no credentials.
 */
public final class TurkuStoryPreparationRequest {
    private String narrativeAngleHint;
    private String additionalContextHint;

    public TurkuStoryPreparationRequest() {}
    public String getNarrativeAngleHint() { return narrativeAngleHint; }
    public void setNarrativeAngleHint(String v) { this.narrativeAngleHint = v; }
    public String getAdditionalContextHint() { return additionalContextHint; }
    public void setAdditionalContextHint(String v) { this.additionalContextHint = v; }
}
