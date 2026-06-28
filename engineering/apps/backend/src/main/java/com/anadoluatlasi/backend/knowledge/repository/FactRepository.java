package com.anadoluatlasi.backend.knowledge.repository;

import com.anadoluatlasi.backend.knowledge.entity.Fact;
import com.anadoluatlasi.backend.knowledge.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Olgu (fact) kayıtları için repository arayüzü.
 * Bilgi öğesi ve doğrulama durumuna göre sorgulamayı sağlar.
 */
@Repository
public interface FactRepository extends JpaRepository<Fact, UUID> {

    /**
     * Belirtilen bilgi öğesine ait olguları listeler.
     */
    List<Fact> findByKnowledgeItemId(UUID knowledgeItemId);

    /**
     * Belirtilen doğrulama durumundaki olguları listeler.
     */
    List<Fact> findByVerificationStatus(VerificationStatus status);

    /**
     * Belirtilen bilgi öğesine ait ve belirtilen doğrulama durumundaki olguları listeler.
     */
    List<Fact> findByKnowledgeItemIdAndVerificationStatus(UUID knowledgeItemId, VerificationStatus status);
}
