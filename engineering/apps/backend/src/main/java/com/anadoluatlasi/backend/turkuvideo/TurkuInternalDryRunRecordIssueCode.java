package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-051: Issue codes for internal dry-run records.
 */
public enum TurkuInternalDryRunRecordIssueCode {
    TRIGGER_REJECTED,
    DRY_RUN_CHAIN_FAILED,
    METADATA_CAPTURE_FAILED,
    INVALID_FIXTURE_INPUT,
    SCENE_COUNT_MISMATCH,
    CLIP_COUNT_MISMATCH,
    STITCH_NOT_PRESENT,
    REVIEW_PACKAGE_MISSING,
    ADMIN_VISIBILITY_BLOCKED,
    INTERNAL_ERROR
}
