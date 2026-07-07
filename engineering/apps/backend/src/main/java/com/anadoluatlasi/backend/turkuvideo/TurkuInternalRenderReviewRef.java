package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-026 Task 02: Opaque human review reference.
 * Format: internal-render-review:{uuid}
 * Safe toString — no path/storage/provider leak.
 */
public final class TurkuInternalRenderReviewRef {

    private static final String PREFIX = "internal-render-review:";
    private final UUID reviewId;

    private TurkuInternalRenderReviewRef(UUID reviewId) {
        this.reviewId = reviewId;
    }

    public static TurkuInternalRenderReviewRef create() {
        return new TurkuInternalRenderReviewRef(UUID.randomUUID());
    }

    public static TurkuInternalRenderReviewRef of(UUID reviewId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("Review ID cannot be null");
        }
        return new TurkuInternalRenderReviewRef(reviewId);
    }

    public static TurkuInternalRenderReviewRef parse(String ref) {
        if (ref == null || ref.isBlank()) return null;
        if (!ref.startsWith(PREFIX)) return null;
        String uuidPart = ref.substring(PREFIX.length());
        try {
            return new TurkuInternalRenderReviewRef(UUID.fromString(uuidPart));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isValid(String ref) {
        return parse(ref) != null;
    }

    public UUID getReviewId() { return reviewId; }

    public String toOpaqueRef() {
        return PREFIX + reviewId;
    }

    @Override
    public String toString() {
        return toOpaqueRef();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return reviewId.equals(((TurkuInternalRenderReviewRef) o).reviewId);
    }

    @Override
    public int hashCode() { return reviewId.hashCode(); }
}
