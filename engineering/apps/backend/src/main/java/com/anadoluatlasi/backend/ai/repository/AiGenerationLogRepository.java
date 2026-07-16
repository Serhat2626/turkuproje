package com.anadoluatlasi.backend.ai.repository;

import com.anadoluatlasi.backend.ai.entity.AiGenerationLog;
import com.anadoluatlasi.backend.ai.entity.AiGenerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * AiGenerationLog entity'si için repository.
 */
@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    List<AiGenerationLog> findByContentProjectIdOrderByCreatedAtDesc(UUID contentProjectId);

    List<AiGenerationLog> findByStatus(AiGenerationStatus status);

    // TürküVideo proje maliyet sorguları
    List<AiGenerationLog> findByTurkuProjectIdOrderByCreatedAtDesc(UUID turkuProjectId);

    @Query("SELECT COALESCE(SUM(l.cost), 0) FROM AiGenerationLog l WHERE l.turkuProjectId = :projectId")
    BigDecimal sumCostByTurkuProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT l.stepName, COUNT(l), COALESCE(SUM(l.cost), 0) " +
           "FROM AiGenerationLog l WHERE l.turkuProjectId = :projectId " +
           "GROUP BY l.stepName")
    List<Object[]> costBreakdownByTurkuProjectId(@Param("projectId") UUID projectId);
}
