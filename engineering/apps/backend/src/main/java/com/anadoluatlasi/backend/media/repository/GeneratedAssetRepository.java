package com.anadoluatlasi.backend.media.repository;

import com.anadoluatlasi.backend.media.entity.GeneratedAsset;
import com.anadoluatlasi.backend.media.entity.GenerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * GeneratedAsset entity'si için repository.
 */
@Repository
public interface GeneratedAssetRepository extends JpaRepository<GeneratedAsset, UUID> {

    List<GeneratedAsset> findByContentProjectId(UUID contentProjectId);

    List<GeneratedAsset> findByGenerationStatus(GenerationStatus status);

    List<GeneratedAsset> findByMediaAssetId(UUID mediaAssetId);
}
