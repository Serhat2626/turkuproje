package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-035: Safe artifact format enum. No path/command.
 */
public enum TurkuRenderArtifactFormat {
    MP4,
    WEBM,
    UNKNOWN;

    public static TurkuRenderArtifactFormat fromString(String s) {
        if (s == null) return UNKNOWN;
        return switch (s.toLowerCase()) {
            case "mp4" -> MP4;
            case "webm" -> WEBM;
            default -> UNKNOWN;
        };
    }

    public boolean isAllowed() {
        return this == MP4 || this == WEBM;
    }
}
