package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-023 Task 02: FFmpeg output specification.
 * Opaque output reference. No absolute path, no URL, no storage key.
 */
public final class TurkuFfmpegOutputSpec {
    private final String outputRef;
    private final String targetFormat;
    private final String targetResolution;
    private final int targetFps;
    private final int maxDurationSeconds;

    public TurkuFfmpegOutputSpec(String outputRef, String targetFormat,
                                  String targetResolution, int targetFps,
                                  int maxDurationSeconds) {
        this.outputRef = outputRef;
        this.targetFormat = targetFormat;
        this.targetResolution = targetResolution;
        this.targetFps = targetFps;
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public String getOutputRef() { return outputRef; }
    public String getTargetFormat() { return targetFormat; }
    public String getTargetResolution() { return targetResolution; }
    public int getTargetFps() { return targetFps; }
    public int getMaxDurationSeconds() { return maxDurationSeconds; }
}
