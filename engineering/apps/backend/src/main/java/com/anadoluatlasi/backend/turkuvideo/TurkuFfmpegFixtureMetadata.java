package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-031 Task 01: Safe fixture metadata for local execution result.
 * Metadata-only — no actual output path/binary exposed.
 */
public final class TurkuFfmpegFixtureMetadata {
    private final String outputRef;
    private final String format;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long sizeMB;
    private final boolean boundsValid;

    public TurkuFfmpegFixtureMetadata(String outputRef, String format,
            int durationSeconds, int fps, int width, int height, long sizeMB) {
        this.outputRef = outputRef;
        this.format = format != null ? format : "mp4";
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.sizeMB = sizeMB;
        boolean formatOk = "mp4".equalsIgnoreCase(format) || "webm".equalsIgnoreCase(format);
        this.boundsValid = formatOk
                && durationSeconds >= 1 && durationSeconds <= 600
                && fps >= 1 && fps <= 60
                && width >= 1 && width <= 3840
                && height >= 1 && height <= 2160
                && sizeMB >= 0 && sizeMB <= 1000;
    }

    public String getOutputRef() { return outputRef; }
    public String getFormat() { return format; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getSizeMB() { return sizeMB; }
    public boolean isBoundsValid() { return boundsValid; }
}
