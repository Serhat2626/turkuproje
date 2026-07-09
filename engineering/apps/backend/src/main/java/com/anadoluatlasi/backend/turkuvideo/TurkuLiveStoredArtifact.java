package com.anadoluatlasi.backend.turkuvideo;

/**
 * LIVE-003: Per-scene stored artifact entry.
 * Internal opaque ref only — no URL/key/path/base64.
 */
public final class TurkuLiveStoredArtifact {
    private final int sceneNo;
    private final String opaqueInternalRef;
    private final String artifactLabel;
    private final String formatHint;
    private final int widthHint;
    private final int heightHint;
    private final boolean verified;
    private final String verificationNote;

    public TurkuLiveStoredArtifact(int sceneNo, String opaqueInternalRef, String artifactLabel,
                                    String formatHint, int widthHint, int heightHint,
                                    boolean verified, String verificationNote) {
        this.sceneNo = sceneNo;
        this.opaqueInternalRef = opaqueInternalRef;
        this.artifactLabel = artifactLabel;
        this.formatHint = formatHint;
        this.widthHint = widthHint;
        this.heightHint = heightHint;
        this.verified = verified;
        this.verificationNote = verificationNote;
    }

    public int getSceneNo() { return sceneNo; }
    public String getOpaqueInternalRef() { return opaqueInternalRef; }
    public String getArtifactLabel() { return artifactLabel; }
    public String getFormatHint() { return formatHint; }
    public int getWidthHint() { return widthHint; }
    public int getHeightHint() { return heightHint; }
    public boolean isVerified() { return verified; }
    public String getVerificationNote() { return verificationNote; }
}
