package com.anadoluatlasi.backend.media.repository;

import com.anadoluatlasi.backend.media.entity.AssetType;
import com.anadoluatlasi.backend.media.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * MediaAsset entity'si için repository.
 */
@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    List<MediaAsset> findByAssetType(AssetType assetType);

    List<MediaAsset> findAllByOrderByCreatedAtDesc();
}
