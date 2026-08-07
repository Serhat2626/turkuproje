package com.anadoluatlasi.backend.turkuvideo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FP-019 Task 04: Per-scene image generation request.
 * Safe opaque reference only — no provider/storage/base64 data.
 */
public final class TurkuSceneImageRequest {

    private final int sceneNo;
    private final String safePromptSummary;
    private final String safeNegativeNotes;
    private final String styleHint;
    private final String aspectRatioHint;

    @JsonCreator
    public TurkuSceneImageRequest(@JsonProperty("sceneNo") int sceneNo,
                                   @JsonProperty("safePromptSummary") String safePromptSummary,
                                   @JsonProperty("safeNegativeNotes") String safeNegativeNotes,
                                   @JsonProperty("styleHint") String styleHint,
                                   @JsonProperty("aspectRatioHint") String aspectRatioHint) {
        this.sceneNo = sceneNo;
        this.safePromptSummary = safePromptSummary;
        this.safeNegativeNotes = safeNegativeNotes;
        this.styleHint = styleHint;
        this.aspectRatioHint = aspectRatioHint;
    }

    public int getSceneNo() { return sceneNo; }
    public String getSafePromptSummary() { return safePromptSummary; }
    public String getSafeNegativeNotes() { return safeNegativeNotes; }
    public String getStyleHint() { return styleHint; }
    public String getAspectRatioHint() { return aspectRatioHint; }
}
