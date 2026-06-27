package com.startup.turkuproje.repository;

import com.startup.turkuproje.entity.ResearchNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResearchNoteRepository extends JpaRepository<ResearchNote, UUID> {

    List<ResearchNote> findByProjectIdOrderByVersionDesc(UUID projectId);

    Optional<ResearchNote> findFirstByProjectIdAndApprovedTrue(UUID projectId);

    ResearchNote findFirstByProjectIdOrderByVersionDesc(UUID projectId);

    int countByProjectId(UUID projectId);
}
