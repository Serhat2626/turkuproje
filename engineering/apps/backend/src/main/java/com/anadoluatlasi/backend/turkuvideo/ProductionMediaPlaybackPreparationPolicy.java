package com.anadoluatlasi.backend.turkuvideo;

import java.util.Set;

/**
 * PD-003: Policy for internal playback preparation.
 */
public final class ProductionMediaPlaybackPreparationPolicy {
    private static final Set<String> SUPPORTED_FORMATS = Set.of("MP4", "WEBM");
    private static final long MAX_PLAYBACK_SIZE_BYTES = 500L * 1024 * 1024;
    private static final int MAX_DURATION_SECONDS = 3600; // 1 hour

    public boolean isFormatSupported(String format) {
        return format != null && SUPPORTED_FORMATS.contains(format.toUpperCase());
    }

    public boolean isSizeAllowed(long sizeBytes) {
        return sizeBytes > 0 && sizeBytes <= MAX_PLAYBACK_SIZE_BYTES;
    }

    public boolean isDurationAllowed(int durationSeconds) {
        return durationSeconds > 0 && durationSeconds <= MAX_DURATION_SECONDS;
    }

    public boolean isStorageStatusAccepted(ProductionMediaStorageObjectStatus status) {
        return status == ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED
                || status == ProductionMediaStorageObjectStatus.METADATA_VERIFIED;
    }
}
