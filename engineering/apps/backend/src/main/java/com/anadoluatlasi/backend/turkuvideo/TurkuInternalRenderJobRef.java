package com.anadoluatlasi.backend.turkuvideo;

import java.util.UUID;

/**
 * FP-025 Task 02: Opaque internal render job reference.
 * Format: internal-render-job:{uuid}
 * Safe toString — no path/storage/provider leak.
 */
public final class TurkuInternalRenderJobRef {

    private static final String PREFIX = "internal-render-job:";
    private final UUID jobId;

    private TurkuInternalRenderJobRef(UUID jobId) {
        this.jobId = jobId;
    }

    public static TurkuInternalRenderJobRef create() {
        return new TurkuInternalRenderJobRef(UUID.randomUUID());
    }

    public static TurkuInternalRenderJobRef of(UUID jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("Job ID cannot be null");
        }
        return new TurkuInternalRenderJobRef(jobId);
    }

    /**
     * Parse from opaque ref string. Returns null if invalid.
     */
    public static TurkuInternalRenderJobRef parse(String ref) {
        if (ref == null || ref.isBlank()) return null;
        if (!ref.startsWith(PREFIX)) return null;
        String uuidPart = ref.substring(PREFIX.length());
        try {
            return new TurkuInternalRenderJobRef(UUID.fromString(uuidPart));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Validate ref string without creating instance.
     */
    public static boolean isValid(String ref) {
        return parse(ref) != null;
    }

    public UUID getJobId() { return jobId; }

    public String toOpaqueRef() {
        return PREFIX + jobId;
    }

    @Override
    public String toString() {
        return toOpaqueRef();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return jobId.equals(((TurkuInternalRenderJobRef) o).jobId);
    }

    @Override
    public int hashCode() { return jobId.hashCode(); }
}
