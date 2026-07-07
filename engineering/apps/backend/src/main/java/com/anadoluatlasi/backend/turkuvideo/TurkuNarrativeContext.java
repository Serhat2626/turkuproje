package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 03: Distilled narrative seed for scenario generation.
 */
public final class TurkuNarrativeContext {

    private final String hookAngle;
    private final String emotionalArc;
    private final String narrativeStructure;
    private final String targetAudience;
    private final String toneGuide;

    public TurkuNarrativeContext(String hookAngle, String emotionalArc,
                                 String narrativeStructure, String targetAudience,
                                 String toneGuide) {
        this.hookAngle = hookAngle;
        this.emotionalArc = emotionalArc;
        this.narrativeStructure = narrativeStructure;
        this.targetAudience = targetAudience;
        this.toneGuide = toneGuide;
    }

    public String getHookAngle() { return hookAngle; }
    public String getEmotionalArc() { return emotionalArc; }
    public String getNarrativeStructure() { return narrativeStructure; }
    public String getTargetAudience() { return targetAudience; }
    public String getToneGuide() { return toneGuide; }
}
