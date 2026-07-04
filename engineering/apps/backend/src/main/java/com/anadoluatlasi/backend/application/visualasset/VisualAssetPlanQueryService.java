package com.anadoluatlasi.backend.application.visualasset;

import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.visualasset.entity.VisualAssetPlan;
import com.anadoluatlasi.backend.visualasset.repository.VisualAssetPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * FP-006: Visual Asset Plan read-only query service.
 * Controller'dan çağrılır, iş kuralı içermez.
 * FP-003 StoryboardQueryService pattern'ı ile tutarlıdır.
 *
 * <p>Bu service raw prompt, provider payload, API key veya secret döndürmez.
 * MediaAsset veya GeneratedAsset referansı içermez.</p>
 */
@Service
@RequiredArgsConstructor
public class VisualAssetPlanQueryService {

    private final VisualAssetPlanRepository visualAssetPlanRepository;

    /**
     * Belirli storyboard'a ait tüm görsel planları version desc sıralı döndürür.
     */
    @Transactional(readOnly = true)
    public List<VisualAssetPlan> listByStoryboardDraft(UUID storyboardDraftId) {
        List<VisualAssetPlan> plans = visualAssetPlanRepository
                .findBySourceStoryboardDraftIdOrderByVersionDesc(storyboardDraftId);
        // Force lazy load: controller'da open-in-view=false, session kapanır
        plans.forEach(this::forceLoadChildren);
        return plans;
    }

    /**
     * Belirli projeye ait tüm görsel planları version desc sıralı döndürür.
     */
    @Transactional(readOnly = true)
    public List<VisualAssetPlan> listByContentProject(UUID contentProjectId) {
        List<VisualAssetPlan> plans = visualAssetPlanRepository
                .findByContentProjectIdOrderByVersionDesc(contentProjectId);
        // Force lazy load
        plans.forEach(this::forceLoadChildren);
        return plans;
    }

    /**
     * Görsel planı scene plan'ları ve thumbnail plan'ıyla birlikte döndürür.
     */
    @Transactional(readOnly = true)
    public VisualAssetPlan getDetail(UUID planId) {
        VisualAssetPlan plan = visualAssetPlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("VisualAssetPlan", planId));
        // Force lazy load
        forceLoadChildren(plan);
        plan.getContentProject().getId();
        plan.getSourceStoryboardDraft().getId();
        return plan;
    }

    private void forceLoadChildren(VisualAssetPlan plan) {
        plan.getScenePlans().size();
        if (plan.getThumbnailPlan() != null) {
            plan.getThumbnailPlan().getId();
        }
    }
}
