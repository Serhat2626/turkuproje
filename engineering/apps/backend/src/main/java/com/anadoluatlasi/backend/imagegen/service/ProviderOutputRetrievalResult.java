package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Safe internal result model for provider output retrieval.
 *
 * FP-009 — Provider Output Retrieval contract.
 *
 * This result carries only backend-internal safe data.
 * It intentionally does NOT contain:
 * - Provider URL (transient, never persisted/returned)
 * - Signed URL
 * - Public URL
 * - Storage key
 * - Base64
 * - Raw provider response
 * - Auth tokens / API keys / secrets
 *
 * The imageBytes field is backend-internal only — it is used
 * for passing retrieved bytes to validation and storage, and
 * must never be exposed in any admin/user DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderOutputRetrievalResult {

    /** Whether retrieval was successful. */
    private boolean success;

    /**
     * Retrieved image bytes — backend-internal only.
     * Used for passing to validation service.
     * Must NEVER appear in any DTO, API response, or log.
     */
    private byte[] imageBytes;

    /** Detected content type from the download (e.g., "image/png"). */
    private String detectedContentType;

    /** Byte size of the downloaded content. */
    private long contentLength;

    /** Safe error code on failure. Uses existing ImageStorageErrorCode. */
    private ImageStorageErrorCode errorCode;

    /**
     * Safe, sanitized error message on failure.
     * Must be product-level — no URLs, tokens, or raw details.
     */
    private String errorMessage;

    /** End-to-end trace identifier. */
    private String correlationId;

    /** Whether the failure is potentially retryable. */
    private boolean retryable;

    /** Retrieval completion timestamp. */
    private LocalDateTime retrievedAt;
}
