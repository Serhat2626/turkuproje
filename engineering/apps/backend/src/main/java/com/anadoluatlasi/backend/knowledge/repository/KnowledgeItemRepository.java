package com.anadoluatlasi.backend.knowledge.repository;

import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemStatus;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bilgi bankası öğeleri için repository arayüzü.
 * Slug, durum, tür ve oluşturulma tarihine göre sorgular sağlar.
 */
@Repository
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, UUID> {

    /**
     * Slug değerine göre bilgi öğesi bulur.
     */
    Optional<KnowledgeItem> findBySlug(String slug);

    /**
     * Belirtilen duruma sahip bilgi öğelerini listeler.
     */
    List<KnowledgeItem> findByStatus(KnowledgeItemStatus status);

    /**
     * Belirtilen türe sahip bilgi öğelerini listeler.
     */
    List<KnowledgeItem> findByType(KnowledgeItemType type);

    /**
     * Belirtilen tür ve duruma sahip bilgi öğelerini listeler.
     */
    List<KnowledgeItem> findByTypeAndStatus(KnowledgeItemType type, KnowledgeItemStatus status);

    /**
     * Tüm bilgi öğelerini oluşturulma tarihine göre en yeniden eskiye sıralar.
     */
    List<KnowledgeItem> findAllByOrderByCreatedAtDesc();
}
