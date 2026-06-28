package com.anadoluatlasi.backend.knowledge.repository;

import com.anadoluatlasi.backend.knowledge.entity.SourceReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Kaynak referansları için repository arayüzü.
 * Bilgi öğesi ID'sine göre kaynakları sorgulamayı sağlar.
 */
@Repository
public interface SourceReferenceRepository extends JpaRepository<SourceReference, UUID> {

    /**
     * Belirtilen bilgi öğesine ait kaynak referanslarını listeler.
     */
    List<SourceReference> findByKnowledgeItemId(UUID knowledgeItemId);

    /**
     * Belirtilen bilgi öğesine ait kaynak referanslarını
     * oluşturulma tarihine göre en yeniden eskiye sıralar.
     */
    List<SourceReference> findByKnowledgeItemIdOrderByCreatedAtDesc(UUID knowledgeItemId);
}
