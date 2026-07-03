package com.anadoluatlasi.backend.application.storyboard;

import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.storyboard.entity.StoryboardDraft;
import com.anadoluatlasi.backend.storyboard.repository.StoryboardDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * FP-003: Storyboard taslağı read-only query service.
 * Controller'dan çağrılır, iş kuralı içermez.
 * FP-002 ScenarioQueryService pattern'ı ile tutarlıdır.
 */
@Service
@RequiredArgsConstructor
public class StoryboardQueryService {

    private final StoryboardDraftRepository storyboardDraftRepository;

    /**
     * Content Project'e ait tüm storyboard taslaklarını version desc sıralı döndürür.
     */
    @Transactional(readOnly = true)
    public List<StoryboardDraft> listByContentProject(UUID contentProjectId) {
        List<StoryboardDraft> drafts = storyboardDraftRepository.findByContentProjectIdOrderByVersionDesc(contentProjectId);
        // Force lazy load: controller'da open-in-view=false, session kapanır
        drafts.forEach(d -> d.getScenes().size());
        return drafts;
    }

    /**
     * Storyboard taslağını scene'leriyle birlikte döndürür.
     */
    @Transactional(readOnly = true)
    public StoryboardDraft getDetail(UUID storyboardDraftId) {
        StoryboardDraft draft = storyboardDraftRepository.findById(storyboardDraftId)
                .orElseThrow(() -> new NotFoundException("StoryboardDraft", storyboardDraftId));
        // Force lazy load: controller'da open-in-view=false, session kapanır
        draft.getScenes().size();
        draft.getContentProject().getId();
        draft.getSourceDraft().getId();
        draft.getSourceScenarioDraft().getId();
        return draft;
    }
}
