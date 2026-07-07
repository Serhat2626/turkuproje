package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-028 Task 02: Opaque internal FFmpeg output reference.
 * Format: internal-ffmpeg-output:{uuid}
 * No filesystem path, no storage key, no public URL.
 */
public final class TurkuFfmpegInternalOutputRef {

    private static final String PREFIX = "internal-ffmpeg-output:";
    private final UUID outputId;

    private TurkuFfmpegInternalOutputRef(UUID outputId) {
        this.outputId = outputId;
    }

    public static TurkuFfmpegInternalOutputRef create() {
        return new TurkuFfmpegInternalOutputRef(UUID.randomUUID());
    }

    public static TurkuFfmpegInternalOutputRef parse(String ref) {
        if (ref == null || ref.isBlank()) return null;
        if (!ref.startsWith(PREFIX)) return null;
        try { return new TurkuFfmpegInternalOutputRef(UUID.fromString(ref.substring(PREFIX.length()))); }
        catch (IllegalArgumentException e) { return null; }
    }

    public static boolean isValid(String ref) { return parse(ref) != null; }
    public UUID getOutputId() { return outputId; }
    public String toOpaqueRef() { return PREFIX + outputId; }
    @Override public String toString() { return toOpaqueRef(); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return outputId.equals(((TurkuFfmpegInternalOutputRef) o).outputId);
    }
    @Override public int hashCode() { return outputId.hashCode(); }
}
