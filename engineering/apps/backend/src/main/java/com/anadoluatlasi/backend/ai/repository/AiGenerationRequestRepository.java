package com.anadoluatlasi.backend.ai.repository;

import com.anadoluatlasi.backend.ai.entity.AiGenerationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-001: AI üretim isteği repository'si.
 * ContentProject bazlı listeleme, en son istek bulma ve deneme numarası sorguları sağlar.
 */
@Repository
public interface AiGenerationRequestRepository extends JpaRepository<AiGenerationRequest, UUID> {

    /**
     * Belirli bir projeye ait tüm AI isteklerini oluşturulma tarihine göre en yeniden eskiye listeler.
     */
    List<AiGenerationRequest> findByContentProjectIdOrderByCreatedAtDesc(UUID contentProjectId);

    /**
     * Belirli bir projeye ait en son AI isteğini döndürür.
     */
    Optional<AiGenerationRequest> findFirstByContentProjectIdOrderByCreatedAtDesc(UUID contentProjectId);

    /**
     * Belirli bir proje için en yüksek deneme numarasını döndürür.
     * Hiç istek yoksa null döner; service katmanında 0 olarak yorumlanmalıdır.
     */
    @Query("SELECT MAX(r.attemptNumber) FROM AiGenerationRequest r WHERE r.contentProject.id = :projectId")
    Optional<Integer> findMaxAttemptNumberByContentProjectId(@Param("projectId") UUID contentProjectId);
}
