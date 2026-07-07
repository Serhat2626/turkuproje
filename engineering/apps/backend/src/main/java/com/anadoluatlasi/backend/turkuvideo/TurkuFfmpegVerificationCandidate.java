package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;

/**
 * FP-028 Task 02: Verification candidate summary for FP-024 artifact verification.
 * Metadata-only. No binary/base64/path.
 */
public final class TurkuFfmpegVerificationCandidate {
    private final String outputRef;
    private final String jobRef;
    private final String format;
    private final int durationSeconds;
    private final int fps;
    private final int width;
    private final int height;
    private final long estimatedSizeMB;
    private final boolean boundsValid;
    private final boolean internalOnly;
    private final boolean publicDeliveryAvailable;
    private final boolean autoApproved;
    private final List<String> safetyFlags;

    public TurkuFfmpegVerificationCandidate(String outputRef, String jobRef, String format,
            int durationSeconds, int fps, int width, int height, long estimatedSizeMB) {
        this.outputRef = outputRef;
        this.jobRef = jobRef;
        this.format = format;
        this.durationSeconds = durationSeconds;
        this.fps = fps;
        this.width = width;
        this.height = height;
        this.estimatedSizeMB = estimatedSizeMB;
        this.boundsValid = isFormatAllowed(format)
                && durationSeconds >= 1 && durationSeconds <= 600
                && fps >= 1 && fps <= 60
                && width >= 1 && width <= 3840
                && height >= 1 && height <= 2160
                && estimatedSizeMB >= 0 && estimatedSizeMB <= 1000;
        this.internalOnly = true;
        this.publicDeliveryAvailable = false;
        this.autoApproved = false;
        this.safetyFlags = List.of("INTERNAL_ONLY", "NO_PUBLISH", "NO_EXPORT",
                "NO_DOWNLOAD", "NO_PUBLIC_URL", "METADATA_ONLY");
    }

    private static boolean isFormatAllowed(String f) {
        return "mp4".equalsIgnoreCase(f) || "webm".equalsIgnoreCase(f);
    }

    public String getOutputRef() { return outputRef; }
    public String getJobRef() { return jobRef; }
    public String getFormat() { return format; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getFps() { return fps; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getEstimatedSizeMB() { return estimatedSizeMB; }
    public boolean isBoundsValid() { return boundsValid; }
    public boolean isInternalOnly() { return internalOnly; }
    public boolean isPublicDeliveryAvailable() { return publicDeliveryAvailable; }
    public boolean isAutoApproved() { return autoApproved; }
    public List<String> getSafetyFlags() { return safetyFlags; }
}
