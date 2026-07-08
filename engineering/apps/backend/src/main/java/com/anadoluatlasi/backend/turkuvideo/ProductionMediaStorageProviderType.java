package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-001: Production media storage provider types.
 * Only safe/private providers. No public CDN.
 */
public enum ProductionMediaStorageProviderType {
    FAKE_TEST_DOUBLE,
    LOCAL_NOOP,
    PRIVATE_S3_COMPATIBLE,
    PRIVATE_GCS_COMPATIBLE
}
