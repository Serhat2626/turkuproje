package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Internal request model for provider output retrieval.
 *
 * FP-009 — Provider Output Retrieval contract.
 *
 * This model carries backend-internal context needed to retrieve
 * provider-generated image output. It intentionally does NOT contain
 * provider URL, signed URL, auth tokens, or any sensitive data.
 *
 * The actual provider output URL is passed separately through the
 * ProviderOutputRetrievalClient interface and is never part of this
 * request model — ensuring it cannot leak through serialization,
 * logging, or DTO mapping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderOutputRetrievalRequest {

    /** Target media asset for retrieval. */
    private UUID mediaAssetId;

    /** Target generated asset for retrieval. */
    private UUID generatedAssetId;

    /** Provider name/type (e.g., "openai", "mock"). Safe metadata. */
    private String providerAlias;

    /** End-to-end trace identifier. */
    private String correlationId;
}
