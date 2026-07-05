package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Safe internal result for retrieval → validation orchestration.
 *
 * FP-009 Task 06 — Orchestration result after provider output retrieval
 * and media validation. Does NOT contain provider URL, storage key,
 * signed/public URL, base64, or raw payloads.
 *
 * On success: validated image bytes are available for future storage handoff.
 * On failure: safe error code and sanitized message describe the failure category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalValidationResult {

    /** Whether both retrieval and validation succeeded. */
    private boolean success;

    /**
     * Validated image bytes — backend-internal only.
     * Available only on success. Used by future storage tasks.
     * Must NEVER appear in any DTO, API response, or log.
     */
    private byte[] validatedImageBytes;

    /** Detected/validated MIME type (e.g., "image/png"). */
    private String detectedMimeType;

    /** Validated byte size. */
    private long fileSizeBytes;

    /** Image width from validation. */
    private int width;

    /** Image height from validation. */
    private int height;

    /** SHA-256 checksum from validation. */
    private String checksumSha256;

    /** Phase where failure occurred: RETRIEVAL or VALIDATION. Null on success. */
    private FailurePhase failurePhase;

    /** Safe error code on failure. Uses existing ImageStorageErrorCode. */
    private ImageStorageErrorCode errorCode;

    /** Safe, sanitized error message on failure. */
    private String errorMessage;

    /** End-to-end trace identifier. */
    private String correlationId;

    /** Whether the failure is potentially retryable. */
    private boolean retryable;

    /** Orchestration completion timestamp. */
    private LocalDateTime completedAt;

    /**
     * Internal enum indicating which phase failed.
     * Not a persisted lifecycle status — purely internal diagnostic.
     */
    public enum FailurePhase {
        RETRIEVAL,
        VALIDATION
    }
}
