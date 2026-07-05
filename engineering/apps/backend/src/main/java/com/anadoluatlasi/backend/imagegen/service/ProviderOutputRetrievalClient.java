package com.anadoluatlasi.backend.imagegen.service;

/**
 * Internal interface for retrieving provider-generated image output.
 *
 * FP-009 — Provider Output Retrieval contract.
 *
 * This interface defines the safe boundary for downloading image bytes
 * from a provider's temporary output reference. Implementations must:
 *
 * <ul>
 *   <li>Accept the provider output URL only as a transient method parameter</li>
 *   <li>Never persist the provider URL</li>
 *   <li>Never expose the provider URL in any return value, DTO, or log</li>
 *   <li>Never expose raw provider response details</li>
 *   <li>Never expose auth tokens, API keys, or secrets</li>
 *   <li>Return only a safe {@link ProviderOutputRetrievalResult}</li>
 *   <li>Sanitize all error messages before returning</li>
 * </ul>
 *
 * The providerOutputUrl parameter is intentionally separated from
 * {@link ProviderOutputRetrievalRequest} to prevent the URL from
 * being serialized, logged, or mapped into any DTO through the
 * request object.
 */
public interface ProviderOutputRetrievalClient {

    /**
     * Retrieve provider-generated image output.
     *
     * @param providerOutputUrl transient provider URL — must not be persisted, logged, or exposed.
     *                          This parameter is backend-internal only.
     * @param request safe retrieval context (asset IDs, correlation ID, provider alias).
     * @return safe retrieval result with image bytes (on success) or safe error (on failure).
     *         The result never contains the provider URL.
     */
    ProviderOutputRetrievalResult retrieve(String providerOutputUrl,
                                           ProviderOutputRetrievalRequest request);
}
