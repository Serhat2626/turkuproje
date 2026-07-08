package com.anadoluatlasi.backend.turkuvideo;

/**
 * RPE-003: Output format/resolution policy metadata.
 */
public final class TurkuOutputFormatPolicy {
    private final String formatLabel;
    private final int width;
    private final int height;
    private final int frameRate;
    private final String policyNote;

    public TurkuOutputFormatPolicy(String formatLabel, int width, int height, int frameRate, String policyNote) {
        this.formatLabel = formatLabel;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.policyNote = policyNote;
    }

    public String getFormatLabel() { return formatLabel; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getFrameRate() { return frameRate; }
    public String getPolicyNote() { return policyNote; }
}
