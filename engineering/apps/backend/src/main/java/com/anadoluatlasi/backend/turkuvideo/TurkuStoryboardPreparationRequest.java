package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-019 Task 03: Request DTO for storyboard preparation.
 */
public final class TurkuStoryboardPreparationRequest {
    private String visualStyleHint;
    private String cameraStyleHint;

    public TurkuStoryboardPreparationRequest() {}
    public String getVisualStyleHint() { return visualStyleHint; }
    public void setVisualStyleHint(String v) { this.visualStyleHint = v; }
    public String getCameraStyleHint() { return cameraStyleHint; }
    public void setCameraStyleHint(String v) { this.cameraStyleHint = v; }
}
