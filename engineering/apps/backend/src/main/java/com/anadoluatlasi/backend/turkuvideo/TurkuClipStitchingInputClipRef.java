package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-040: Input clip reference for stitching. Metadata-only.
 */
public final class TurkuClipStitchingInputClipRef {
    private final String opaqueClipRef;
    private final int clipOrder;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final String format;
    private final boolean verified;

    public TurkuClipStitchingInputClipRef(String opaqueClipRef, int clipOrder,
            int durationSeconds, int fps, int width, int height,
            long sizeBytes, String format, boolean verified) {
        this.opaqueClipRef = opaqueClipRef;
        this.clipOrder = clipOrder;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.format = format != null ? format : "mp4";
        this.verified = verified;
    }

    public String getOpaqueClipRef() { return opaqueClipRef; }
    public int getClipOrder() { return clipOrder; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeBytes() { return sizeBytes; }
    public String getFormat() { return format; }
    public boolean isVerified() { return verified; }
}
