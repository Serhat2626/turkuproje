package com.anadoluatlasi.backend.turkuvideo;

import java.util.Set;

/**
 * PD-007: Policy for controlled public playback.
 */
public final class ProductionMediaPublicPlaybackPolicy {
    private static final Set<String> ALLOWED_FORMATS = Set.of("MP4", "WEBM");
    private static final long MAX_SIZE = 500L * 1024 * 1024;

    public boolean isGateApproved(ProductionMediaDeliveryGateStatus status) {
        return status == ProductionMediaDeliveryGateStatus.APPROVED_FOR_CONTROLLED_PUBLIC_DELIVERY_PREP;
    }

    public boolean isStorageVerified(ProductionMediaStorageObjectStatus status) {
        return status == ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED
                || status == ProductionMediaStorageObjectStatus.METADATA_VERIFIED;
    }

    public boolean isFormatAllowed(String format) {
        return format != null && ALLOWED_FORMATS.contains(format.toUpperCase());
    }

    public boolean isSizeAllowed(long sizeBytes) {
        return sizeBytes > 0 && sizeBytes <= MAX_SIZE;
    }
}
