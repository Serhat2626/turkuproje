package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * FP-021 Task 02: Transition plan — scene-to-scene transition directives.
 */
public final class TurkuTransitionPlan {

    private final List<TransitionDirective> transitions;
    private final String defaultTransition;

    @JsonCreator
    public TurkuTransitionPlan(
            @JsonProperty("transitions") List<TransitionDirective> transitions,
            @JsonProperty("defaultTransition") String defaultTransition) {
        this.transitions = transitions != null ? List.copyOf(transitions) : List.of();
        this.defaultTransition = defaultTransition;
    }

    public List<TransitionDirective> getTransitions() { return transitions; }
    public String getDefaultTransition() { return defaultTransition; }

    /**
     * Single transition directive between two scenes.
     */
    public static final class TransitionDirective {
        private final int fromScene;
        private final int toScene;
        private final String type;
        private final int durationMs;

        @JsonCreator
        public TransitionDirective(
                @JsonProperty("fromScene") int fromScene,
                @JsonProperty("toScene") int toScene,
                @JsonProperty("type") String type,
                @JsonProperty("durationMs") int durationMs) {
            this.fromScene = fromScene;
            this.toScene = toScene;
            this.type = type;
            this.durationMs = durationMs;
        }

        public int getFromScene() { return fromScene; }
        public int getToScene() { return toScene; }
        public String getType() { return type; }
        public int getDurationMs() { return durationMs; }
    }
}
