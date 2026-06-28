package com.anadoluatlasi.backend.ai.repository;

import com.anadoluatlasi.backend.ai.entity.PromptCategory;
import com.anadoluatlasi.backend.ai.entity.PromptTemplate;
import com.anadoluatlasi.backend.ai.entity.PromptTemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PromptTemplate entity'si için repository.
 */
@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    List<PromptTemplate> findByCategory(PromptCategory category);

    List<PromptTemplate> findByStatus(PromptTemplateStatus status);

    Optional<PromptTemplate> findByNameAndVersion(String name, Integer version);

    List<PromptTemplate> findByNameOrderByVersionDesc(String name);
}
