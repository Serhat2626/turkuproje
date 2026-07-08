package com.anadoluatlasi.backend.turkuvideo;

/**
 * FP-035: Artifact verification issue codes.
 */
public enum TurkuRenderArtifactIssueCode {
    NONE,
    FORMAT_NOT_ALLOWED,
    DURATION_EXCEEDS_LIMIT,
    FPS_EXCEEDS_LIMIT,
    WIDTH_EXCEEDS_LIMIT,
    HEIGHT_EXCEEDS_LIMIT,
    SIZE_EXCEEDS_LIMIT,
    METADATA_MISSING,
    REF_MISSING
}
