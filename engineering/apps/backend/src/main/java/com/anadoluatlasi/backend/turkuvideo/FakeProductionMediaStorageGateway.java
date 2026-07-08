package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * PD-001: Fake/test-double production media storage provider.
 * Creates private object refs from metadata only — no real upload.
 */
public class FakeProductionMediaStorageGateway implements ProductionMediaStorageGateway {

    private final ProductionMediaStoragePolicy policy = new ProductionMediaStoragePolicy();
    private int storeCount = 0;

    @Override
    public ProductionMediaStorageResult storePrivate(
            String opaqueArtifactRef, String mediaFormat,
            int durationSeconds, int fps, int width, int height,
            long sizeBytes, boolean checksumPresent, String opaqueChecksumRef) {

        var issues = policy.validate(mediaFormat, sizeBytes, checksumPresent);
        if (!issues.isEmpty()) {
            return ProductionMediaStorageResult.rejected(
                    issues.toArray(new ProductionMediaStorageIssueCode[0]));
        }

        storeCount++;
        var objectRef = new ProductionMediaStorageObjectRef(
                UUID.randomUUID(),
                "fake-obj:" + UUID.randomUUID().toString().substring(0, 8),
                opaqueArtifactRef,
                ProductionMediaStorageProviderType.FAKE_TEST_DOUBLE,
                ProductionMediaStorageMode.PRIVATE_ONLY,
                ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                mediaFormat.toUpperCase(),
                durationSeconds, fps, width, height, sizeBytes,
                checksumPresent, opaqueChecksumRef);

        return new ProductionMediaStorageResult(
                ProductionMediaStorageObjectStatus.PRIVATE_OBJECT_REF_CREATED,
                List.of(), objectRef);
    }

    @Override
    public ProductionMediaStorageProviderType getProviderType() {
        return ProductionMediaStorageProviderType.FAKE_TEST_DOUBLE;
    }

    public int getStoreCount() { return storeCount; }
}
