package com.anadoluatlasi.backend.content.repository;

import com.anadoluatlasi.backend.content.entity.ContentDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-001: İçerik taslağı repository'si.
 * ContentProject bazlı listeleme ve en son taslak bulma sorguları sağlar.
 */
@Repository
public interface ContentDraftRepository extends JpaRepository<ContentDraft, UUID> {

    /**
     * Belirli bir projeye ait tüm taslakları oluşturulma tarihine göre en yeniden eskiye listeler.
     */
    List<ContentDraft> findByContentProjectIdOrderByCreatedAtDesc(UUID contentProjectId);

    /**
     * Belirli bir projeye ait en son oluşturulan taslağı döndürür.
     */
    Optional<ContentDraft> findFirstByContentProjectIdOrderByCreatedAtDesc(UUID contentProjectId);
}
