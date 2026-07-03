package com.anadoluatlasi.backend.platformpackage.repository;

import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageDraft;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageStatus;
import com.anadoluatlasi.backend.platformpackage.entity.PlatformPackageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * FP-004: Platform paketi taslağı repository'si.
 * Content Project ve StoryboardDraft bazlı listeleme, versiyon yönetimi
 * ve onay kısıtlaması sorguları sağlar.
 */
@Repository
public interface PlatformPackageDraftRepository extends JpaRepository<PlatformPackageDraft, UUID> {

    /**
     * Belirli bir projeye ait tüm platform paketlerini version'a göre en yeniden eskiye listeler.
     */
    List<PlatformPackageDraft> findByContentProjectIdOrderByVersionDesc(UUID contentProjectId);

    /**
     * Belirli bir storyboard'a ait tüm platform paketlerini version'a göre listeler.
     */
    List<PlatformPackageDraft> findBySourceStoryboardDraftIdOrderByVersionDesc(UUID sourceStoryboardDraftId);

    /**
     * Belirli bir proje + platform tipi + status kombinasyonuna göre paketleri bulur.
     * Özellikle one-approved-per-platform kuralı enforcer'ında kullanılır.
     */
    List<PlatformPackageDraft> findByContentProjectIdAndPackageTypeAndStatus(
            UUID contentProjectId,
            PlatformPackageType packageType,
            PlatformPackageStatus status
    );

    /**
     * Belirli bir proje + storyboard + platform tipi için en yüksek versiyon numarasını döndürür.
     * Yeni versiyon hesaplamasında kullanılır.
     */
    @Query("SELECT COALESCE(MAX(p.version), 0) FROM PlatformPackageDraft p " +
           "WHERE p.contentProject.id = :projectId " +
           "AND p.sourceStoryboardDraft.id = :storyboardId " +
           "AND p.packageType = :packageType")
    int findMaxVersionByProjectAndStoryboardAndType(
            @Param("projectId") UUID projectId,
            @Param("storyboardId") UUID storyboardId,
            @Param("packageType") PlatformPackageType packageType
    );

    /**
     * Belirli bir proje ve statüye göre paketleri filtreler.
     */
    List<PlatformPackageDraft> findByContentProjectIdAndStatus(UUID contentProjectId, PlatformPackageStatus status);

    /**
     * Belirli bir proje + platform tipi için APPROVED paket sayısını döndürür.
     * One-approved-per-platform kuralı kontrolünde kullanılır.
     */
    @Query("SELECT COUNT(p) FROM PlatformPackageDraft p " +
           "WHERE p.contentProject.id = :projectId " +
           "AND p.packageType = :packageType " +
           "AND p.status = :status")
    long countByProjectAndTypeAndStatus(
            @Param("projectId") UUID projectId,
            @Param("packageType") PlatformPackageType packageType,
            @Param("status") PlatformPackageStatus status
    );
}
