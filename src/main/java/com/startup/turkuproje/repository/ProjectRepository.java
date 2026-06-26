package com.startup.turkuproje.repository;

import com.startup.turkuproje.entity.Project;
import com.startup.turkuproje.entity.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    // Duruma göre projeleri listele
    List<Project> findByStatus(ProjectStatus status);

    // Son oluşturulandan eskiye sırala
    List<Project> findAllByOrderByCreatedAtDesc();
}
