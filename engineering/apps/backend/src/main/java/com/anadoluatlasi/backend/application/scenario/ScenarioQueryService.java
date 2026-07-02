package com.anadoluatlasi.backend.application.scenario;

import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.scenario.entity.ScenarioDraft;
import com.anadoluatlasi.backend.scenario.repository.ScenarioDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * FP-002: Senaryo taslağı read-only query service.
 * Controller'dan çağrılır, iş kuralı içermez.
 */
@Service
@RequiredArgsConstructor
public class ScenarioQueryService {

    private final ScenarioDraftRepository scenarioDraftRepository;

    /**
     * Content Project'e ait tüm senaryo taslaklarını version desc sıralı döndürür.
     */
    @Transactional(readOnly = true)
    public List<ScenarioDraft> listByContentProject(UUID contentProjectId) {
        return scenarioDraftRepository.findByContentProjectIdOrderByVersionDesc(contentProjectId);
    }

    /**
     * Senaryo taslağını beat'leriyle birlikte döndürür.
     */
    @Transactional(readOnly = true)
    public ScenarioDraft getDetail(UUID scenarioDraftId) {
        ScenarioDraft draft = scenarioDraftRepository.findById(scenarioDraftId)
                .orElseThrow(() -> new NotFoundException("ScenarioDraft", scenarioDraftId));
        // Force lazy load
        draft.getBeats().size();
        return draft;
    }
}
