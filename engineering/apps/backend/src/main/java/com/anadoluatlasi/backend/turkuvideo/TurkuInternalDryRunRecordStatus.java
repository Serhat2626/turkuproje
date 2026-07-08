package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-051: Safe statuses for internal dry-run records.
 * No READY_FOR_PUBLISH / COMPLETED / AUTO_APPROVED.
 */
public enum TurkuInternalDryRunRecordStatus {
    CREATED_INTERNAL_ONLY,
    TRIGGER_ACCEPTED,
    DRY_RUN_STARTED,
    METADATA_CAPTURED,
    READY_FOR_ADMIN_HISTORY_ONLY,
    REJECTED_UNSAFE,
    FAILED_SAFE
}
