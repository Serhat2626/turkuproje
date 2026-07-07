package com.anadoluatlasi.backend.turkuvideo;

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

    public TurkuSceneImageRequest(int sceneNo, String safePromptSummary,
                                   String safeNegativeNotes, String styleHint,
                                   String aspectRatioHint) {
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
