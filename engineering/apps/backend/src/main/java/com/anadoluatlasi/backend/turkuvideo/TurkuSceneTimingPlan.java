package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * FP-021 Task 02: Scene timing plan — duration breakdown.
 */
public final class TurkuSceneTimingPlan {

    private final double totalDurationSeconds;
    private final int targetDurationSeconds;
    private final double durationDelta;
    private final Map<Integer, Double> sceneDurations;
    private final boolean hasTimingConflict;

    public TurkuSceneTimingPlan(double totalDurationSeconds, int targetDurationSeconds,
                                 Map<Integer, Double> sceneDurations, boolean hasTimingConflict) {
        this(totalDurationSeconds, targetDurationSeconds, totalDurationSeconds - targetDurationSeconds,
             sceneDurations, hasTimingConflict);
    }

    @JsonCreator
    public TurkuSceneTimingPlan(
            @JsonProperty("totalDurationSeconds") double totalDurationSeconds,
            @JsonProperty("targetDurationSeconds") int targetDurationSeconds,
            @JsonProperty("durationDelta") double durationDelta,
            @JsonProperty("sceneDurations") Map<Integer, Double> sceneDurations,
            @JsonProperty("hasTimingConflict") boolean hasTimingConflict) {
        this.totalDurationSeconds = totalDurationSeconds;
        this.targetDurationSeconds = targetDurationSeconds;
        this.durationDelta = durationDelta;
        this.sceneDurations = sceneDurations != null ? Map.copyOf(sceneDurations) : Map.of();
        this.hasTimingConflict = hasTimingConflict;
    }

    public double getTotalDurationSeconds() { return totalDurationSeconds; }
    public int getTargetDurationSeconds() { return targetDurationSeconds; }
    public double getDurationDelta() { return durationDelta; }
    public Map<Integer, Double> getSceneDurations() { return sceneDurations; }
    public boolean isHasTimingConflict() { return hasTimingConflict; }
}
