package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-046: Safe storyboard scene input for render plan adapter.
 * Metadata-only. No lyrics/script/prompt/user media/path/command.
 */
public final class TurkuStoryboardSceneRenderInput {
    private final String opaqueSceneRef;
    private final int sceneOrder;
    private final int targetDurationSeconds;
    private final int targetFps;
    private final int targetWidth;
    private final int targetHeight;
    private final String targetFormat;

    public TurkuStoryboardSceneRenderInput(String opaqueSceneRef, int sceneOrder,
            int targetDurationSeconds, int targetFps, int targetWidth, int targetHeight,
            String targetFormat) {
        this.opaqueSceneRef = opaqueSceneRef;
        this.sceneOrder = sceneOrder;
        this.targetDurationSeconds = targetDurationSeconds;
        this.targetFps = targetFps;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.targetFormat = targetFormat != null ? targetFormat : "mp4";
    }

    public String getOpaqueSceneRef() { return opaqueSceneRef; }
    public int getSceneOrder() { return sceneOrder; }
    public int getTargetDurationSeconds() { return targetDurationSeconds; }
    public int getTargetFps() { return targetFps; }
    public int getTargetWidth() { return targetWidth; }
    public int getTargetHeight() { return targetHeight; }
    public String getTargetFormat() { return targetFormat; }

    @Override
    public String toString() {
        return "SceneInput{ref=" + opaqueSceneRef + ", order=" + sceneOrder
                + ", dur=" + targetDurationSeconds + "s, " + targetWidth + "x" + targetHeight + "}";
    }
}
