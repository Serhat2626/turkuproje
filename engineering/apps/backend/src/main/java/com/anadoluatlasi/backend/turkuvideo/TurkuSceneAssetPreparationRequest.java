package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 04: Request for scene asset plan preparation.
 */
public final class TurkuSceneAssetPreparationRequest {
    private String styleOverrideHint;
    private String aspectRatioHint;

    public TurkuSceneAssetPreparationRequest() {}
    public String getStyleOverrideHint() { return styleOverrideHint; }
    public void setStyleOverrideHint(String v) { this.styleOverrideHint = v; }
    public String getAspectRatioHint() { return aspectRatioHint; }
    public void setAspectRatioHint(String v) { this.aspectRatioHint = v; }
}
