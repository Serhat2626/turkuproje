package com.anadoluatlasi.backend.turkuvideo;


import java.util.List;

/**
 * FP-019 Task 03: Storyboard plan — scene-by-scene visual plan.
 * No public URLs, no copyrighted media, no raw provider data.
 */
public final class TurkuStoryboardPlan {

    private final List<TurkuStoryboardScene> scenes;
    private final int totalEstimatedDurationSeconds;
    private final List<String> reviewFlags;

    public TurkuStoryboardPlan(List<TurkuStoryboardScene> scenes, int totalEstimatedDurationSeconds,
                                List<String> reviewFlags) {
        this.scenes = scenes != null ? List.copyOf(scenes) : List.of();
        this.totalEstimatedDurationSeconds = totalEstimatedDurationSeconds;
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
    }

    public List<TurkuStoryboardScene> getScenes() { return scenes; }
    public int getTotalEstimatedDurationSeconds() { return totalEstimatedDurationSeconds; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
