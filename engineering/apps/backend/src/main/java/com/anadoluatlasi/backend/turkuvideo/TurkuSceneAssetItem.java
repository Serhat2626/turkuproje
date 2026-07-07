package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

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

    public TurkuSceneAssetItem(int sceneNo, String assetPurpose, String visualPromptSummary,
                                String negativePromptSafetyNotes, String styleHint,
                                String mood, String aspectRatioHint,
                                boolean needsHumanReview, boolean copyrightReviewRequired) {
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
