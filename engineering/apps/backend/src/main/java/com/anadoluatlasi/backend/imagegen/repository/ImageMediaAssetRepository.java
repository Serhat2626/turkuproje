package com.anadoluatlasi.backend.imagegen.repository;

import com.anadoluatlasi.backend.imagegen.entity.ImageMediaAsset;
import com.anadoluatlasi.backend.imagegen.entity.ImageMediaAssetKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-007: Image media asset repository.
 *
 * <p>Generated image dosyalarının storage metadata sorgulama desteği sağlar.
 * Storage key/bucket internal-only olarak saklanır, admin-facing API'de dönmez.</p>
 */
@Repository
public interface ImageMediaAssetRepository extends JpaRepository<ImageMediaAsset, UUID> {

    /**
     * Asset kind bazlı listeleme.
     */
    List<ImageMediaAsset> findByAssetKind(ImageMediaAssetKind assetKind);

    /**
     * Checksum ile duplicate kontrolü.
     */
    Optional<ImageMediaAsset> findByChecksumSha256(String checksumSha256);

    /**
     * Storage object key ile arama. Internal kullanım.
     */
    Optional<ImageMediaAsset> findByStorageObjectKey(String storageObjectKey);
}
