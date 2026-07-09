package com.anadoluatlasi.backend.turkuvideo;

/**
 * LIVE-004: Sanitized render artifact metadata.
 * No command/path/stdout/stderr/rawLog/storage URL.
 */
public final class TurkuLiveRenderArtifact {
    private final String opaqueRenderRef;
    private final String artifactLabel;
    private final String formatHint;
    private final double durationSecondsHint;
    private final String resolutionHint;
    private final String sizeLabel;
    private final boolean verified;
    private final String verificationNote;
    private final boolean humanReviewRequired;

    public TurkuLiveRenderArtifact(String opaqueRenderRef, String artifactLabel,
                                    String formatHint, double durationSecondsHint,
                                    String resolutionHint, String sizeLabel,
                                    boolean verified, String verificationNote,
                                    boolean humanReviewRequired) {
        this.opaqueRenderRef = opaqueRenderRef;
        this.artifactLabel = artifactLabel;
        this.formatHint = formatHint;
        this.durationSecondsHint = durationSecondsHint;
        this.resolutionHint = resolutionHint;
        this.sizeLabel = sizeLabel;
        this.verified = verified;
        this.verificationNote = verificationNote;
        this.humanReviewRequired = humanReviewRequired;
    }

    public String getOpaqueRenderRef() { return opaqueRenderRef; }
    public String getArtifactLabel() { return artifactLabel; }
    public String getFormatHint() { return formatHint; }
    public double getDurationSecondsHint() { return durationSecondsHint; }
    public String getResolutionHint() { return resolutionHint; }
    public String getSizeLabel() { return sizeLabel; }
    public boolean isVerified() { return verified; }
    public String getVerificationNote() { return verificationNote; }
    public boolean isHumanReviewRequired() { return humanReviewRequired; }
}
