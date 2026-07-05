package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mock implementation of {@link ProviderOutputRetrievalClient}.
 *
 * FP-009 — Provider Output Retrieval contract (mock/test-safe implementation).
 *
 * This mock client does NOT perform any real network retrieval.
 * It returns a controlled test-safe response:
 * - Valid PNG bytes (minimal 1x1 pixel PNG) on non-null URL input.
 * - Safe failure on null/blank URL input.
 *
 * Safety guarantees:
 * - Does not call any external provider.
 * - Does not require config/secrets.
 * - Does not persist, log, or expose the provider URL.
 * - Returns sanitized error messages only.
 */
@Slf4j
@Component
public class MockProviderOutputRetrievalClient implements ProviderOutputRetrievalClient {

    /**
     * Minimal valid 1x1 pixel PNG image bytes.
     * Used as test-safe mock output.
     */
    static final byte[] MOCK_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,         // IHDR chunk
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,         // 1x1 pixel
            0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, // 8-bit RGB
            (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, // IDAT chunk
            0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF,
            (byte) 0xC0, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01,
            (byte) 0xE2, 0x21, (byte) 0xBC, 0x33,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,         // IEND chunk
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    static final String MOCK_CONTENT_TYPE = "image/png";

    @Override
    public ProviderOutputRetrievalResult retrieve(String providerOutputUrl,
                                                   ProviderOutputRetrievalRequest request) {
        String correlationId = request != null ? request.getCorrelationId() : null;

        // Safety: log only safe context, never the URL
        log.info("[PROVIDER_RETRIEVAL] Mock retrieval requested. correlationId={}, providerAlias={}",
                correlationId,
                request != null ? request.getProviderAlias() : "unknown");

        // Fail safely on null/blank URL
        if (providerOutputUrl == null || providerOutputUrl.isBlank()) {
            log.warn("[PROVIDER_RETRIEVAL] Mock retrieval failed: provider output URL is null/blank. correlationId={}",
                    correlationId);
            return ProviderOutputRetrievalResult.builder()
                    .success(false)
                    .errorCode(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE)
                    .errorMessage(ImageStorageErrorNormalizer.safeMessage(
                            ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE))
                    .correlationId(correlationId)
                    .retryable(false)
                    .retrievedAt(LocalDateTime.now())
                    .build();
        }

        // Fail safely on null request
        if (request == null) {
            return ProviderOutputRetrievalResult.builder()
                    .success(false)
                    .errorCode(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE)
                    .errorMessage(ImageStorageErrorNormalizer.safeMessage(
                            ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE,
                            "Retrieval request context is missing"))
                    .retryable(false)
                    .retrievedAt(LocalDateTime.now())
                    .build();
        }

        // Mock success: return controlled PNG bytes
        log.info("[PROVIDER_RETRIEVAL] Mock retrieval success. correlationId={}, contentType={}, contentLength={}",
                correlationId, MOCK_CONTENT_TYPE, MOCK_PNG_BYTES.length);

        return ProviderOutputRetrievalResult.builder()
                .success(true)
                .imageBytes(MOCK_PNG_BYTES)
                .detectedContentType(MOCK_CONTENT_TYPE)
                .contentLength(MOCK_PNG_BYTES.length)
                .correlationId(correlationId)
                .retryable(false)
                .retrievedAt(LocalDateTime.now())
                .build();
    }
}
