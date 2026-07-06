package com.anadoluatlasi.backend.turkuvideo;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FP-019 Task 02: In-memory repository for Türkü video projects.
 *
 * <p>No database migration required. Will be replaced with JPA repository
 * when schema changes are scoped in a later feature.</p>
 */
public class TurkuVideoProjectRepository {

    private final Map<UUID, TurkuVideoProject> store = new ConcurrentHashMap<>();

    public TurkuVideoProject save(TurkuVideoProject project) {
        store.put(project.getId(), project);
        return project;
    }

    public Optional<TurkuVideoProject> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    public int count() {
        return store.size();
    }

    /** Test support only */
    public void clear() {
        store.clear();
    }
}
