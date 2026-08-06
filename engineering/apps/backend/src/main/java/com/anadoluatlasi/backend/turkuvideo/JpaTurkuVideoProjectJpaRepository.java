package com.anadoluatlasi.backend.turkuvideo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository — turku_video_projects tablosu.
 */
@Repository
public interface JpaTurkuVideoProjectJpaRepository extends JpaRepository<JpaTurkuVideoProjectEntity, UUID> {

    List<JpaTurkuVideoProjectEntity> findByWorkflowStage(String workflowStage);

    List<JpaTurkuVideoProjectEntity> findByTurkuNameContainingIgnoreCase(String turkuName);

    List<JpaTurkuVideoProjectEntity> findBySourceConfidence(String sourceConfidence);

    List<JpaTurkuVideoProjectEntity> findByNeedsHumanReviewTrue();

    List<JpaTurkuVideoProjectEntity> findAllByOrderByCreatedAtDesc();
}
