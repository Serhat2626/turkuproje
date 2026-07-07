package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-023 Task 02: FFmpeg input specification.
 * Internal references only. No file paths, no URLs, no secrets.
 */
public final class TurkuFfmpegInputSpec {
    private final String internalRef;
    private final int sceneNo;
    private final double durationSeconds;
    private final String format;

    public TurkuFfmpegInputSpec(String internalRef, int sceneNo,
                                 double durationSeconds, String format) {
        this.internalRef = internalRef;
        this.sceneNo = sceneNo;
        this.durationSeconds = durationSeconds;
        this.format = format;
    }

    public String getInternalRef() { return internalRef; }
    public int getSceneNo() { return sceneNo; }
    public double getDurationSeconds() { return durationSeconds; }
    public String getFormat() { return format; }
}
