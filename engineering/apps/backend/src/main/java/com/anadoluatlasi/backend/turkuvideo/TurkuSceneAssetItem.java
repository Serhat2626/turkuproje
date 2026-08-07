package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FP-019 Task 04: Per-scene asset item — visual prompt plan for one scene.
 * No public URLs, no base64, no storage keys, no raw provider data.
 */
public final class TurkuSceneAssetItem {

    private final int sceneNo;
    private final String assetPurpose;
    private final String visualPromptSummary;
    private final String negativePromptSafetyNotes;
    private final String styleHint;
    private final String mood;
    private final String aspectRatioHint;
    private final boolean needsHumanReview;
    private final boolean copyrightReviewRequired;

    @JsonCreator
    public TurkuSceneAssetItem(
            @JsonProperty("sceneNo") int sceneNo,
            @JsonProperty("assetPurpose") String assetPurpose,
            @JsonProperty("visualPromptSummary") String visualPromptSummary,
            @JsonProperty("negativePromptSafetyNotes") String negativePromptSafetyNotes,
            @JsonProperty("styleHint") String styleHint,
            @JsonProperty("mood") String mood,
            @JsonProperty("aspectRatioHint") String aspectRatioHint,
            @JsonProperty("needsHumanReview") boolean needsHumanReview,
            @JsonProperty("copyrightReviewRequired") boolean copyrightReviewRequired) {
        this.sceneNo = sceneNo;
        this.assetPurpose = assetPurpose;
        this.visualPromptSummary = visualPromptSummary;
        this.negativePromptSafetyNotes = negativePromptSafetyNotes;
        this.styleHint = styleHint;
        this.mood = mood;
        this.aspectRatioHint = aspectRatioHint;
        this.needsHumanReview = needsHumanReview;
        this.copyrightReviewRequired = copyrightReviewRequired;
    }

    public int getSceneNo() { return sceneNo; }
    public String getAssetPurpose() { return assetPurpose; }
    public String getVisualPromptSummary() { return visualPromptSummary; }
    public String getNegativePromptSafetyNotes() { return negativePromptSafetyNotes; }
    public String getStyleHint() { return styleHint; }
    public String getMood() { return mood; }
    public String getAspectRatioHint() { return aspectRatioHint; }
    public boolean isNeedsHumanReview() { return needsHumanReview; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
}
