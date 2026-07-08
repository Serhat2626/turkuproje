package com.anadoluatlasi.backend.turkuvideo;

import java.util.Set;

/**
 * PD-006: Delivery gate policy. Validates all prerequisites before granting eligibility.
 */
public final class ProductionMediaDeliveryGatePolicy {
    private static final Set<String> ALLOWED_FORMATS = Set.of("MP4", "WEBM");
    private static final long MAX_SIZE = 500L * 1024 * 1024;

    public boolean isHumanDecisionApproved(ProductionMediaReviewDecisionStatus status) {
        return status == ProductionMediaReviewDecisionStatus.APPROVED_FOR_INTERNAL_DELIVERY_PREP;
    }

    public boolean isStorageRefVerified(ProductionMediaStorageObjectStatus status) {
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
