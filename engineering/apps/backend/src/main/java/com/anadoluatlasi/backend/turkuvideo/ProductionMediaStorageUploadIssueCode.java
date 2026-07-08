package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-002: Upload pipeline issue codes.
 */
public enum ProductionMediaStorageUploadIssueCode {
    ARTIFACT_NOT_VERIFIED,
    ARTIFACT_LIFECYCLE_REJECTED,
    ARTIFACT_LIFECYCLE_FAILED,
    FORMAT_NOT_ALLOWED,
    SIZE_EXCEEDS_LIMIT,
    CHECKSUM_MISSING,
    GATEWAY_UNAVAILABLE,
    GATEWAY_REJECTED,
    GATEWAY_FAILED,
    METADATA_INCOMPLETE,
    INTERNAL_ERROR
}
