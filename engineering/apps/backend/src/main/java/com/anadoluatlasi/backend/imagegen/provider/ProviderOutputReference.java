package com.anadoluatlasi.backend.imagegen.provider;

/**
 * Transient internal holder for provider generation result + provider output URL.
 *
 * FP-009 Task 07 — Provider URL Gap Resolution.
 *
 * This object is strictly backend-internal and short-lived:
 * <ul>
 *   <li>NOT an entity — no JPA mapping, no DB persistence</li>
 *   <li>NOT a DTO — never serialized to API response</li>
 *   <li>NOT logged — toString() intentionally excludes the URL</li>
 *   <li>The providerOutputUrl field is used once for retrieval then discarded</li>
 * </ul>
 *
 * The provider URL is separated from {@link ImageProviderGenerationResult}
 * to ensure the safe result model remains clean and cannot accidentally
 * expose the URL through serialization, mapping, or logging.
 */
public record ProviderOutputReference(
        ImageProviderGenerationResult generationResult,
        String providerOutputUrl
) {

    /**
     * Safe toString — intentionally excludes provider URL.
     * Only shows whether a URL is present, not the actual value.
     */
    @Override
    public String toString() {
        return "ProviderOutputReference{" +
                "generationResult=" + (generationResult != null ? "present" : "null") +
                ", hasProviderOutputUrl=" + (providerOutputUrl != null && !providerOutputUrl.isBlank()) +
                '}';
    }
}
