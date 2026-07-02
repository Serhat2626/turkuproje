package com.anadoluatlasi.backend.storyboard.repository;

import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-003: Storyboard taslağı repository'si.
 * Content Project bazlı listeleme, version yönetimi ve source scenario sorguları sağlar.
 */
@Repository
public interface StoryboardDraftRepository extends JpaRepository<StoryboardDraft, UUID> {

    /**
     * Belirli bir projeye ait tüm storyboard taslaklarını version'a göre en yeniden eskiye listeler.
     */
    List<StoryboardDraft> findByContentProjectIdOrderByVersionDesc(UUID contentProjectId);

    /**
     * Belirli bir kaynak senaryoya ait tüm storyboard taslaklarını version'a göre listeler.
     */
    List<StoryboardDraft> findBySourceScenarioDraftIdOrderByVersionDesc(UUID sourceScenarioDraftId);

    /**
     * Belirli bir projeye ait en yüksek version numaralı storyboard taslağını döndürür.
     */
    Optional<StoryboardDraft> findFirstByContentProjectIdOrderByVersionDesc(UUID contentProjectId);

    /**
     * Belirli bir projeye ait en yüksek version numarasını döndürür.
     */
    @Query("SELECT COALESCE(MAX(sd.version), 0) FROM StoryboardDraft sd WHERE sd.contentProject.id = :projectId")
    int findMaxVersionByContentProjectId(@Param("projectId") UUID projectId);

    /**
     * Belirli bir projeye ait belirli statüdeki storyboard taslaklarını listeler.
     */
    List<StoryboardDraft> findByContentProjectIdAndStatus(UUID contentProjectId, StoryboardDraftStatus status);

    /**
     * Belirli bir kaynak senaryoya ait belirli statüdeki storyboard taslaklarını listeler.
     */
    List<StoryboardDraft> findBySourceScenarioDraftIdAndStatus(UUID sourceScenarioDraftId, StoryboardDraftStatus status);
}
