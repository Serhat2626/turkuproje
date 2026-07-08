package com.anadoluatlasi.backend.turkuvideo;

/**
 * RPE-006: Fake generated image result metadata.
 * No URL, key, path, base64, binary. Opaque reference only.
 */
public final class TurkuFakeImageResultMetadata {
    private final int sceneNo;
    private final String opaqueArtifactRef;
    private final String artifactLabel;
    private final String formatHint;
    private final int widthHint;
    private final int heightHint;
    private final String generationNote;

    public TurkuFakeImageResultMetadata(int sceneNo, String opaqueArtifactRef, String artifactLabel,
                                         String formatHint, int widthHint, int heightHint, String generationNote) {
        this.sceneNo = sceneNo;
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.artifactLabel = artifactLabel;
        this.formatHint = formatHint;
        this.widthHint = widthHint;
        this.heightHint = heightHint;
        this.generationNote = generationNote;
    }

    public int getSceneNo() { return sceneNo; }
    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public String getArtifactLabel() { return artifactLabel; }
    public String getFormatHint() { return formatHint; }
    public int getWidthHint() { return widthHint; }
    public int getHeightHint() { return heightHint; }
    public String getGenerationNote() { return generationNote; }
}
