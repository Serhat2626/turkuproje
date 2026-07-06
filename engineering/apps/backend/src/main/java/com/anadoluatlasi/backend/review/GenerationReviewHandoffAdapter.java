package com.anadoluatlasi.backend.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * FP-013 Task 05: Safe adapter that wraps {@link ReviewHandoffService}
 * for generation/orchestration completion points.
 *
 * <p>Each method creates a safe {@link ReviewHandoffRequest} and delegates
 * to {@link ReviewHandoffService#handoffForReview(ReviewHandoffRequest)}.
 * All exceptions are caught at WARN level — handoff failure is non-fatal
 * and must never block generation success.</p>
 *
 * <p>This adapter does NOT:</p>
 * <ul>
 *   <li>Publish, export, or auto-approve any content</li>
 *   <li>Generate public/signed URLs or CDN references</li>
 *   <li>Expose provider URLs, storage keys, or secrets</li>
 *   <li>Bypass human review</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationReviewHandoffAdapter {

    private final ReviewHandoffService reviewHandoffService;

    /**
     * Hand off a completed scenario draft to the review workflow.
     *
     * @param scenarioDraftId scenario draft ID
     * @param title           safe title for audit
     * @param summary         safe summary for audit
     * @return handoff result, or null if handoff failed (non-fatal)
     */
    public ReviewHandoffResult handoffScenario(UUID scenarioDraftId, String title, String summary) {
        try {
            ReviewHandoffRequest request = ReviewHandoffRequest.createSafe(
                    ReviewTargetType.SCENARIO,
                    scenarioDraftId,
                    ReviewHandoffSource.SCENARIO_GENERATION,
                    ReviewActorRole.SYSTEM,
                    "system:scenario-generation",
                    title,
                    summary);
            ReviewHandoffResult result = reviewHandoffService.handoffForReview(request);
            log.info("FP-013: Senaryo review handoff başarılı: scenarioDraftId={}, state={}",
                    scenarioDraftId, result.getCurrentState());
            return result;
        } catch (Exception ex) {
            log.warn("FP-013: Senaryo review handoff başarısız (non-fatal): scenarioDraftId={}, error={}",
                    scenarioDraftId, ex.getMessage());
            return null;
        }
    }

    /**
     * Hand off a completed storyboard draft to the review workflow.
     *
     * @param storyboardDraftId storyboard draft ID
     * @param title             safe title for audit
     * @param summary           safe summary for audit
     * @return handoff result, or null if handoff failed (non-fatal)
     */
    public ReviewHandoffResult handoffStoryboard(UUID storyboardDraftId, String title, String summary) {
        try {
            ReviewHandoffRequest request = ReviewHandoffRequest.createSafe(
                    ReviewTargetType.STORYBOARD,
                    storyboardDraftId,
                    ReviewHandoffSource.STORYBOARD_GENERATION,
                    ReviewActorRole.SYSTEM,
                    "system:storyboard-generation",
                    title,
                    summary);
            ReviewHandoffResult result = reviewHandoffService.handoffForReview(request);
            log.info("FP-013: Storyboard review handoff başarılı: storyboardDraftId={}, state={}",
                    storyboardDraftId, result.getCurrentState());
            return result;
        } catch (Exception ex) {
            log.warn("FP-013: Storyboard review handoff başarısız (non-fatal): storyboardDraftId={}, error={}",
                    storyboardDraftId, ex.getMessage());
            return null;
        }
    }

    /**
     * Hand off a completed platform package draft to the review workflow.
     *
     * @param packageDraftId package draft ID
     * @param title          safe title for audit
     * @param summary        safe summary for audit
     * @return handoff result, or null if handoff failed (non-fatal)
     */
    public ReviewHandoffResult handoffPackage(UUID packageDraftId, String title, String summary) {
        try {
            ReviewHandoffRequest request = ReviewHandoffRequest.createSafe(
                    ReviewTargetType.PACKAGE,
                    packageDraftId,
                    ReviewHandoffSource.PACKAGE_GENERATION,
                    ReviewActorRole.SYSTEM,
                    "system:package-generation",
                    title,
                    summary);
            ReviewHandoffResult result = reviewHandoffService.handoffForReview(request);
            log.info("FP-013: Platform paket review handoff başarılı: packageDraftId={}, state={}",
                    packageDraftId, result.getCurrentState());
            return result;
        } catch (Exception ex) {
            log.warn("FP-013: Platform paket review handoff başarısız (non-fatal): packageDraftId={}, error={}",
                    packageDraftId, ex.getMessage());
            return null;
        }
    }

    /**
     * Hand off a completed image generated asset to the review workflow.
     *
     * @param generatedAssetId generated asset ID
     * @param title            safe title for audit
     * @param summary          safe summary for audit
     * @return handoff result, or null if handoff failed (non-fatal)
     */
    public ReviewHandoffResult handoffImageAsset(UUID generatedAssetId, String title, String summary) {
        try {
            ReviewHandoffRequest request = ReviewHandoffRequest.createSafe(
                    ReviewTargetType.IMAGE_ASSET,
                    generatedAssetId,
                    ReviewHandoffSource.IMAGE_GENERATION,
                    ReviewActorRole.SYSTEM,
                    "system:image-generation",
                    title,
                    summary);
            ReviewHandoffResult result = reviewHandoffService.handoffForReview(request);
            log.info("FP-013: Image asset review handoff başarılı: generatedAssetId={}, state={}",
                    generatedAssetId, result.getCurrentState());
            return result;
        } catch (Exception ex) {
            log.warn("FP-013: Image asset review handoff başarısız (non-fatal): generatedAssetId={}, error={}",
                    generatedAssetId, ex.getMessage());
            return null;
        }
    }

    /**
     * Hand off a completed content project to the review workflow.
     *
     * @param contentProjectId content project ID
     * @param title            safe title for audit
     * @param summary          safe summary for audit
     * @return handoff result, or null if handoff failed (non-fatal)
     */
    public ReviewHandoffResult handoffContentProject(UUID contentProjectId, String title, String summary) {
        try {
            ReviewHandoffRequest request = ReviewHandoffRequest.createSafe(
                    ReviewTargetType.CONTENT_PROJECT,
                    contentProjectId,
                    ReviewHandoffSource.CONTENT_PROJECT_GENERATION,
                    ReviewActorRole.SYSTEM,
                    "system:content-project-generation",
                    title,
                    summary);
            ReviewHandoffResult result = reviewHandoffService.handoffForReview(request);
            log.info("FP-013: Content project review handoff başarılı: contentProjectId={}, state={}",
                    contentProjectId, result.getCurrentState());
            return result;
        } catch (Exception ex) {
            log.warn("FP-013: Content project review handoff başarısız (non-fatal): contentProjectId={}, error={}",
                    contentProjectId, ex.getMessage());
            return null;
        }
    }
}
