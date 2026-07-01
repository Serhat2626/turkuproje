package com.anadoluatlasi.backend.scenario.repository;

import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-002: Senaryo taslağı repository'si.
 * Content Project bazlı listeleme ve version yönetimi sorguları sağlar.
 */
@Repository
public interface ScenarioDraftRepository extends JpaRepository<ScenarioDraft, UUID> {

    /**
     * Belirli bir projeye ait tüm senaryo taslaklarını version'a göre en yeniden eskiye listeler.
     */
    List<ScenarioDraft> findByContentProjectIdOrderByVersionDesc(UUID contentProjectId);

    /**
     * Belirli bir projeye ait en yüksek version numaralı senaryo taslağını döndürür.
     */
    Optional<ScenarioDraft> findFirstByContentProjectIdOrderByVersionDesc(UUID contentProjectId);

    /**
     * Belirli bir projeye ait en yüksek version numarasını döndürür.
     */
    @Query("SELECT COALESCE(MAX(sd.version), 0) FROM ScenarioDraft sd WHERE sd.contentProject.id = :projectId")
    int findMaxVersionByContentProjectId(@Param("projectId") UUID projectId);

    /**
     * Belirli bir projeye ait onaylanmış senaryoları listeler.
     */
    List<ScenarioDraft> findByContentProjectIdAndStatus(UUID contentProjectId, ScenarioDraftStatus status);
}
