package com.anadoluatlasi.backend.turkuvideo;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementasyonu — DEVRE DIŞI (JPA adapter aktif).
 * Test'lerde kullanılmak üzere korunuyor.
 *
 * <p>Production'da JpaTurkuVideoProjectRepositoryAdapter (@Primary) kullanılır.</p>
 */
// @Repository — devre dışı, JpaTurkuVideoProjectRepositoryAdapter @Primary olarak aktif
public class InMemoryTurkuVideoProjectRepository implements TurkuVideoProjectRepository {

    private final Map<UUID, TurkuVideoProject> store = new ConcurrentHashMap<>();

    @Override
    public TurkuVideoProject save(TurkuVideoProject project) {
        store.put(project.getId(), project);
        return project;
    }

    @Override
    public Optional<TurkuVideoProject> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<TurkuVideoProject> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public List<TurkuVideoProject> findByWorkflowStage(TurkuVideoWorkflowStage workflowStage) {
        return store.values().stream()
                .filter(p -> p.getWorkflowStage() == workflowStage)
                .collect(Collectors.toList());
    }

    @Override
    public List<TurkuVideoProject> findByNeedsHumanReviewTrue() {
        return store.values().stream()
                .filter(TurkuVideoProject::isNeedsHumanReview)
                .collect(Collectors.toList());
    }

    @Override
    public List<TurkuVideoProject> findByTurkuNameContainingIgnoreCase(String turkuName) {
        String lower = turkuName.toLowerCase();
        return store.values().stream()
                .filter(p -> p.getTurkuName() != null && p.getTurkuName().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    @Override
    public List<TurkuVideoProject> findBySourceConfidence(TurkuSourceConfidence sourceConfidence) {
        return store.values().stream()
                .filter(p -> p.getSourceConfidence() == sourceConfidence)
                .collect(Collectors.toList());
    }

    /** Test desteği — tüm veriyi temizle */
    public void clear() {
        store.clear();
    }
}
