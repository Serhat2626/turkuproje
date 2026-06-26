package com.startup.turkuproje.repository;

import com.startup.turkuproje.entity.Script;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScriptRepository extends JpaRepository<Script, UUID> {

    // Projeye ait senaryoları versiyona göre sırala
    List<Script> findByProjectIdOrderByVersionDesc(UUID projectId);

    // Projeye ait en son versiyon senaryoyu getir
    Script findFirstByProjectIdOrderByVersionDesc(UUID projectId);

    // Projeye ait senaryo sayısı
    int countByProjectId(UUID projectId);
}
