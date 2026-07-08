package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.Set;

/**
 * PD-001: Policy for production media storage.
 * Private-only. No public-read. No signed/CDN URL generation.
 */
public final class ProductionMediaStoragePolicy {
    private static final Set<String> ALLOWED_FORMATS = Set.of("MP4", "WEBM");
    private static final long MAX_SIZE_BYTES = 500L * 1024 * 1024; // 500 MB

    public boolean isFormatAllowed(String format) {
        return format != null && ALLOWED_FORMATS.contains(format.toUpperCase());
    }

    public boolean isSizeAllowed(long sizeBytes) {
        return sizeBytes > 0 && sizeBytes <= MAX_SIZE_BYTES;
    }

    public List<ProductionMediaStorageIssueCode> validate(String format, long sizeBytes, boolean checksumPresent) {
        var issues = new java.util.ArrayList<ProductionMediaStorageIssueCode>();
        if (!isFormatAllowed(format)) issues.add(ProductionMediaStorageIssueCode.UNSUPPORTED_FORMAT);
        if (!isSizeAllowed(sizeBytes)) issues.add(ProductionMediaStorageIssueCode.OVER_SIZE_LIMIT);
        if (!checksumPresent) issues.add(ProductionMediaStorageIssueCode.MISSING_CHECKSUM);
        return issues;
    }

    public Set<String> getAllowedFormats() { return ALLOWED_FORMATS; }
    public long getMaxSizeBytes() { return MAX_SIZE_BYTES; }
}
