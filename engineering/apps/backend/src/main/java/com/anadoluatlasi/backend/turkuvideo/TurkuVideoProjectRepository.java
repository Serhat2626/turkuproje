package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FP-019: Türkü video project repository interface.
 *
 * <p>TurkuVideoProject henüz JPA Entity değil — in-memory domain object olarak kullanılır.
 * Runtime ve test'lerde InMemoryTurkuVideoProjectRepository implement eder.</p>
 */
public interface TurkuVideoProjectRepository {

    TurkuVideoProject save(TurkuVideoProject project);

    Optional<TurkuVideoProject> findById(UUID id);

    List<TurkuVideoProject> findAll();

    long count();

    void deleteAll();

    List<TurkuVideoProject> findByWorkflowStage(TurkuVideoWorkflowStage workflowStage);

    List<TurkuVideoProject> findByNeedsHumanReviewTrue();

    List<TurkuVideoProject> findByTurkuNameContainingIgnoreCase(String turkuName);

    List<TurkuVideoProject> findBySourceConfidence(TurkuSourceConfidence sourceConfidence);
}
