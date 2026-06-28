package com.anadoluatlasi.backend.content.repository;

import com.anadoluatlasi.backend.content.entity.StoryboardScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Storyboard sahnesi repository'si.
 * Storyboard bazlı sahne sıralaması ve sayım sorguları sağlar.
 */
@Repository
public interface StoryboardSceneRepository extends JpaRepository<StoryboardScene, UUID> {

    List<StoryboardScene> findByStoryboardIdOrderBySceneOrder(UUID storyboardId);

    long countByStoryboardId(UUID storyboardId);
}
