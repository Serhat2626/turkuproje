package com.anadoluatlasi.backend.ai.repository;

import com.anadoluatlasi.backend.ai.entity.AiGenerationLog;
import com.anadoluatlasi.backend.ai.entity.AiGenerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * AiGenerationLog entity'si için repository.
 */
@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    List<AiGenerationLog> findByContentProjectIdOrderByCreatedAtDesc(UUID contentProjectId);

    List<AiGenerationLog> findByStatus(AiGenerationStatus status);
}
