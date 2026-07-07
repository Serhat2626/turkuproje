package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-032: Safe local-only FFmpeg availability status.
 * Does NOT expose executable path.
 */
public enum TurkuFfmpegLocalAvailabilityStatus {
    AVAILABLE,
    UNAVAILABLE,
    DISABLED,
    UNSUPPORTED_ENVIRONMENT
}
