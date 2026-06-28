package com.anadoluatlasi.backend.content.repository;

import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.TargetPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * İçerik projesi repository'si.
 * Slug, durum, bilgi öğesi ve platform bazlı sorgular sağlar.
 */
@Repository
public interface ContentProjectRepository extends JpaRepository<ContentProject, UUID> {

    Optional<ContentProject> findBySlug(String slug);

    List<ContentProject> findByStatus(ContentProjectStatus status);

    List<ContentProject> findByTargetPlatform(TargetPlatform platform);

    List<ContentProject> findByKnowledgeItemId(UUID knowledgeItemId);

    List<ContentProject> findAllByOrderByCreatedAtDesc();

    /**
     * FP-001: Belirli bir bilgi öğesine ait projeleri duruma göre filtreler.
     */
    List<ContentProject> findByKnowledgeItemIdAndStatus(UUID knowledgeItemId, ContentProjectStatus status);
}
