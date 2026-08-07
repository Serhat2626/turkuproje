package com.anadoluatlasi.backend.turkuvideo;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * FP-021 Task 02: Video composition plan — assembles storyboard + scene images +
 * narration plan + timing into an internal composition package.
 *
 * <p>No lyrics, no copyrighted audio/video, no rendered video file,
 * no downloadable file, no public/signed/CDN URL, no storage key,
 * no base64/binary, no raw provider payload.</p>
 */
public final class TurkuVideoCompositionPlan {

    private final UUID compositionId;
    private final UUID projectId;
    private final int totalDurationSeconds;
    private final List<TurkuVideoCompositionScene> scenes;
    private final List<TurkuNarrationSegment> narrationSegments;
    private final TurkuSceneTimingPlan timingPlan;
    private final TurkuTransitionPlan transitionPlan;
    private final boolean humanReviewRequired;
    private final boolean copyrightReviewRequired;
    private final boolean autoApproved;
    private final List<String> reviewFlags;

    public TurkuVideoCompositionPlan(UUID compositionId, UUID projectId,
                                      int totalDurationSeconds,
                                      List<TurkuVideoCompositionScene> scenes,
                                      List<TurkuNarrationSegment> narrationSegments,
                                      TurkuSceneTimingPlan timingPlan,
                                      TurkuTransitionPlan transitionPlan,
                                      List<String> reviewFlags) {
        this(compositionId, projectId, totalDurationSeconds, scenes, narrationSegments,
             timingPlan, transitionPlan, true, true, false, reviewFlags);
    }

    @JsonCreator
    public TurkuVideoCompositionPlan(
            @JsonProperty("compositionId") UUID compositionId,
            @JsonProperty("projectId") UUID projectId,
            @JsonProperty("totalDurationSeconds") int totalDurationSeconds,
            @JsonProperty("scenes") List<TurkuVideoCompositionScene> scenes,
            @JsonProperty("narrationSegments") List<TurkuNarrationSegment> narrationSegments,
            @JsonProperty("timingPlan") TurkuSceneTimingPlan timingPlan,
            @JsonProperty("transitionPlan") TurkuTransitionPlan transitionPlan,
            @JsonProperty("humanReviewRequired") boolean humanReviewRequired,
            @JsonProperty("copyrightReviewRequired") boolean copyrightReviewRequired,
            @JsonProperty("autoApproved") boolean autoApproved,
            @JsonProperty("reviewFlags") List<String> reviewFlags) {
        this.compositionId = compositionId;
        this.projectId = projectId;
        this.totalDurationSeconds = totalDurationSeconds;
        this.scenes = scenes != null ? List.copyOf(scenes) : List.of();
        this.narrationSegments = narrationSegments != null ? List.copyOf(narrationSegments) : List.of();
        this.timingPlan = timingPlan;
        this.transitionPlan = transitionPlan;
        this.humanReviewRequired = humanReviewRequired;
        this.copyrightReviewRequired = copyrightReviewRequired;
        this.autoApproved = autoApproved;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
    }

    public UUID getCompositionId() { return compositionId; }
    public UUID getProjectId() { return projectId; }
    public int getTotalDurationSeconds() { return totalDurationSeconds; }
    public List<TurkuVideoCompositionScene> getScenes() { return scenes; }
    public List<TurkuNarrationSegment> getNarrationSegments() { return narrationSegments; }
    public TurkuSceneTimingPlan getTimingPlan() { return timingPlan; }
    public TurkuTransitionPlan getTransitionPlan() { return transitionPlan; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
