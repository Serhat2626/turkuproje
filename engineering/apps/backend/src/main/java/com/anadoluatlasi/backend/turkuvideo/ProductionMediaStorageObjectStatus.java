package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-001: Safe object statuses. No READY_FOR_PUBLISH/COMPLETED/PUBLIC/CDN_READY.
 */
public enum ProductionMediaStorageObjectStatus {
    CREATED_INTERNAL_ONLY,
    ACCEPTED_FOR_PRIVATE_STORAGE,
    PRIVATE_OBJECT_REF_CREATED,
    METADATA_VERIFIED,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
