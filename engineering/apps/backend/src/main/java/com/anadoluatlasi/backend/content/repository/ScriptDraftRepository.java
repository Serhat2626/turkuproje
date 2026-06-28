package com.anadoluatlasi.backend.content.repository;

import com.anadoluatlasi.backend.content.entity.ScriptDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Senaryo taslağı repository'si.
 * Proje bazlı versiyon sıralaması ve sayım sorguları sağlar.
 */
@Repository
public interface ScriptDraftRepository extends JpaRepository<ScriptDraft, UUID> {

    List<ScriptDraft> findByContentProjectIdOrderByVersionDesc(UUID contentProjectId);

    Optional<ScriptDraft> findFirstByContentProjectIdOrderByVersionDesc(UUID contentProjectId);

    long countByContentProjectId(UUID contentProjectId);
}
