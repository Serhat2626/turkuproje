package com.anadoluatlasi.backend.content.repository;

import com.anadoluatlasi.backend.content.entity.Storyboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Storyboard repository'si.
 * Proje bazlı versiyon sıralaması sorguları sağlar.
 */
@Repository
public interface StoryboardRepository extends JpaRepository<Storyboard, UUID> {

    List<Storyboard> findByContentProjectIdOrderByVersionDesc(UUID contentProjectId);

    Optional<Storyboard> findFirstByContentProjectIdOrderByVersionDesc(UUID contentProjectId);
}
