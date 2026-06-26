package com.startup.turkuproje.repository;

import com.startup.turkuproje.entity.PromptTemplate;
import com.startup.turkuproje.entity.PromptType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    // Belirli bir tip için aktif şablonu getir
    Optional<PromptTemplate> findByTypeAndActiveTrue(PromptType type);

    // Tipe göre tüm şablonları listele
    List<PromptTemplate> findByTypeOrderByVersionDesc(PromptType type);

    // Tüm aktif şablonları listele
    List<PromptTemplate> findByActiveTrue();
}
