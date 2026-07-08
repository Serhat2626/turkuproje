package com.anadoluatlasi.backend.turkuvideo;

/**
 * PD-001: Gateway interface for production media storage providers.
 * Private storage only. No public/signed/CDN URL generation.
 */
public interface ProductionMediaStorageGateway {

    /**
     * Store a verified artifact metadata as private object.
     * Returns metadata-only result — no raw payload, no URL.
     */
    ProductionMediaStorageResult storePrivate(
            String opaqueArtifactRef, String mediaFormat,
            int durationSeconds, int fps, int width, int height,
            long sizeBytes, boolean checksumPresent, String opaqueChecksumRef);

    /**
     * Return the provider type.
     */
    ProductionMediaStorageProviderType getProviderType();
}
