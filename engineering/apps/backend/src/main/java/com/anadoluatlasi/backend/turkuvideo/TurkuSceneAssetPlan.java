package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * FP-019 Task 04: Scene asset plan — collection of asset items per storyboard.
 * No public URLs, no base64, no storage keys.
 */
public final class TurkuSceneAssetPlan {

    private final UUID planId;
    private final UUID projectId;
    private final List<TurkuSceneAssetItem> items;
    private final List<String> reviewFlags;

    @JsonCreator
    public TurkuSceneAssetPlan(
            @JsonProperty("planId") UUID planId,
            @JsonProperty("projectId") UUID projectId,
            @JsonProperty("items") List<TurkuSceneAssetItem> items,
            @JsonProperty("reviewFlags") List<String> reviewFlags) {
        this.planId = planId;
        this.projectId = projectId;
        this.items = items != null ? List.copyOf(items) : List.of();
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
    }

    public UUID getPlanId() { return planId; }
    public UUID getProjectId() { return projectId; }
    public List<TurkuSceneAssetItem> getItems() { return items; }
    public List<String> getReviewFlags() { return reviewFlags; }
}
