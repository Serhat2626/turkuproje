package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

/**
 * FP-019 Task 03: Scenario plan — 6-beat video narrative structure.
 * No lyrics, no copyrighted media, no public URLs.
 */
@JsonDeserialize(builder = TurkuScenarioPlan.Builder.class)
public final class TurkuScenarioPlan {

    private final String title;
    private final String logline;
    private final String narrationTone;
    private final Integer targetDurationHintSeconds;
    private final int sceneCountHint;
    private final List<String> scenarioBeats;
    private final List<String> reviewFlags;

    private TurkuScenarioPlan(Builder b) {
        this.title = b.title;
        this.logline = b.logline;
        this.narrationTone = b.narrationTone;
        this.targetDurationHintSeconds = b.targetDurationHintSeconds;
        this.sceneCountHint = b.sceneCountHint;
        this.scenarioBeats = b.scenarioBeats != null ? List.copyOf(b.scenarioBeats) : List.of();
        this.reviewFlags = b.reviewFlags != null ? List.copyOf(b.reviewFlags) : List.of();
    }

    public String getTitle() { return title; }
    public String getLogline() { return logline; }
    public String getNarrationTone() { return narrationTone; }
    public Integer getTargetDurationHintSeconds() { return targetDurationHintSeconds; }
    public int getSceneCountHint() { return sceneCountHint; }
    public List<String> getScenarioBeats() { return scenarioBeats; }
    public List<String> getReviewFlags() { return reviewFlags; }

    public static Builder builder() { return new Builder(); }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String title;
        private String logline;
        private String narrationTone;
        private Integer targetDurationHintSeconds;
        private int sceneCountHint;
        private List<String> scenarioBeats;
        private List<String> reviewFlags;

        public Builder title(String v) { this.title = v; return this; }
        public Builder logline(String v) { this.logline = v; return this; }
        public Builder narrationTone(String v) { this.narrationTone = v; return this; }
        public Builder targetDurationHintSeconds(Integer v) { this.targetDurationHintSeconds = v; return this; }
        public Builder sceneCountHint(int v) { this.sceneCountHint = v; return this; }
        public Builder scenarioBeats(List<String> v) { this.scenarioBeats = v; return this; }
        public Builder reviewFlags(List<String> v) { this.reviewFlags = v; return this; }

        public TurkuScenarioPlan build() { return new TurkuScenarioPlan(this); }
    }
}
