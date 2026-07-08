package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-001: Issue codes for production media storage operations.
 */
public enum ProductionMediaStorageIssueCode {
    UNSUPPORTED_FORMAT,
    OVER_SIZE_LIMIT,
    MISSING_CHECKSUM,
    ARTIFACT_NOT_VERIFIED,
    ARTIFACT_REJECTED,
    PROVIDER_UNAVAILABLE,
    PROVIDER_INTERNAL_ERROR,
    POLICY_VIOLATION,
    METADATA_INCOMPLETE,
    INTERNAL_ERROR
}
