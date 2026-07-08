package com.anadoluatlasi.backend.turkuvideo;

/**
 * RPE-007: Per-scene artifact readiness entry.
 * Opaque ref only — no URL/key/path/base64.
 */
public final class TurkuSceneArtifactReadiness {
    private final int sceneNo;
    private final String opaqueArtifactRef;
    private final String artifactLabel;
    private final boolean verified;
    private final String verificationNote;

    public TurkuSceneArtifactReadiness(int sceneNo, String opaqueArtifactRef, String artifactLabel,
                                        boolean verified, String verificationNote) {
        this.sceneNo = sceneNo;
        this.opaqueArtifactRef = opaqueArtifactRef;
        this.artifactLabel = artifactLabel;
        this.verified = verified;
        this.verificationNote = verificationNote;
    }

    public int getSceneNo() { return sceneNo; }
    public String getOpaqueArtifactRef() { return opaqueArtifactRef; }
    public String getArtifactLabel() { return artifactLabel; }
    public boolean isVerified() { return verified; }
    public String getVerificationNote() { return verificationNote; }
}
