package com.startup.turkuproje.repository;

import com.startup.turkuproje.entity.AiGenerationLog;
import com.startup.turkuproje.entity.PromptType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    // Projeye ait tüm AI logları
    List<AiGenerationLog> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    // Projeye ait belirli bir tipteki loglar
    List<AiGenerationLog> findByProjectIdAndPromptType(UUID projectId, PromptType promptType);
}
