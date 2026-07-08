package com.anadoluatlasi.backend.turkuvideo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FP-038: Multi-scene render orchestration service.
 * Validates scene plan batches, produces metadata-only job plans.
 * No ProcessBuilder. No paths/commands/content. No FFmpeg execution.
 */
public class TurkuMultiSceneRenderOrchestrationService {

    private static final int MAX_SCENES = 20;
    private static final int MAX_DURATION_PER_SCENE = 5;
    private static final int MAX_TOTAL_DURATION = 60;
    private static final int MAX_FPS = 30;
    private static final int MAX_WIDTH = 1280;
    private static final int MAX_HEIGHT = 720;

    /**
     * Build job plan from validated scene render plans.
     */
    public TurkuMultiSceneRenderJobPlan buildJobPlan(String opaqueJobRef,
            List<TurkuSceneRenderPlan> scenePlans) {

        var issues = new ArrayList<TurkuMultiSceneRenderJobIssueCode>();
        var safePlans = scenePlans != null ? scenePlans : List.<TurkuSceneRenderPlan>of();

        // 1. Job ref
        if (opaqueJobRef == null || opaqueJobRef.isBlank())
            issues.add(TurkuMultiSceneRenderJobIssueCode.JOB_REF_MISSING);

        // 2. Empty
        if (safePlans.isEmpty()) {
            issues.add(TurkuMultiSceneRenderJobIssueCode.EMPTY_SCENE_LIST);
            return new TurkuMultiSceneRenderJobPlan(opaqueJobRef,
                    TurkuMultiSceneRenderJobStatus.REJECTED_UNSAFE, List.of(), issues);
        }

        // 3. Too many
        if (safePlans.size() > MAX_SCENES)
            issues.add(TurkuMultiSceneRenderJobIssueCode.TOO_MANY_SCENES);

        // 4. Sort by sceneOrder
        var sorted = safePlans.stream()
                .sorted(Comparator.comparingInt(TurkuSceneRenderPlan::getSceneOrder))
                .toList();

        // 5. Scene order checks
        var orders = sorted.stream().map(TurkuSceneRenderPlan::getSceneOrder).toList();
        var orderSet = new HashSet<>(orders);
        if (orderSet.size() != orders.size())
            issues.add(TurkuMultiSceneRenderJobIssueCode.SCENE_ORDER_DUPLICATE);
        if (orders.stream().anyMatch(o -> o < 1))
            issues.add(TurkuMultiSceneRenderJobIssueCode.SCENE_ORDER_NOT_POSITIVE);
        // Contiguity: must be 1,2,3,...,N
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i) != i + 1) {
                issues.add(TurkuMultiSceneRenderJobIssueCode.SCENE_ORDER_NOT_CONTIGUOUS);
                break;
            }
        }

        // 6. Scene ref checks
        var sceneRefs = sorted.stream().map(TurkuSceneRenderPlan::getOpaqueSceneRef).toList();
        if (sceneRefs.stream().anyMatch(r -> r == null || r.isBlank()))
            issues.add(TurkuMultiSceneRenderJobIssueCode.SCENE_REF_MISSING);
        var refSet = new HashSet<>(sceneRefs);
        if (refSet.size() != sceneRefs.size())
            issues.add(TurkuMultiSceneRenderJobIssueCode.SCENE_REF_DUPLICATE);

        // 7. Per-scene validation
        for (var plan : sorted) {
            if (plan.getStatus() == TurkuSceneRenderPlanStatus.FAILED_SAFE)
                issues.add(TurkuMultiSceneRenderJobIssueCode.SCENE_FAILED);
            else if (plan.getStatus() == TurkuSceneRenderPlanStatus.REJECTED_UNSAFE)
                issues.add(TurkuMultiSceneRenderJobIssueCode.SCENE_REJECTED);
            else if (plan.getStatus() != TurkuSceneRenderPlanStatus.READY_FOR_BUNDLE_ASSEMBLY)
                issues.add(TurkuMultiSceneRenderJobIssueCode.SCENE_NOT_READY);

            if (plan.getExpectedDurationSeconds() > MAX_DURATION_PER_SCENE)
                issues.add(TurkuMultiSceneRenderJobIssueCode.DURATION_PER_SCENE_EXCEEDS_LIMIT);
            if (plan.getExpectedFps() > MAX_FPS)
                issues.add(TurkuMultiSceneRenderJobIssueCode.FPS_EXCEEDS_LIMIT);
            if (plan.getExpectedWidth() > MAX_WIDTH || plan.getExpectedHeight() > MAX_HEIGHT)
                issues.add(TurkuMultiSceneRenderJobIssueCode.RESOLUTION_EXCEEDS_LIMIT);

            boolean hasPrimary = plan.getArtifactExpectations().stream()
                    .anyMatch(e -> e.getRole() == TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP);
            if (!hasPrimary)
                issues.add(TurkuMultiSceneRenderJobIssueCode.MISSING_PRIMARY_VIDEO);
        }

        // 8. Total duration
        int totalDuration = sorted.stream().mapToInt(TurkuSceneRenderPlan::getExpectedDurationSeconds).sum();
        if (totalDuration > MAX_TOTAL_DURATION)
            issues.add(TurkuMultiSceneRenderJobIssueCode.TOTAL_DURATION_EXCEEDS_LIMIT);

        // 9. Build scene refs
        var sceneRefList = sorted.stream()
                .map(TurkuMultiSceneRenderSceneRef::fromPlan)
                .toList();

        // 10. Determine status
        TurkuMultiSceneRenderJobStatus status;
        if (issues.stream().anyMatch(i -> i == TurkuMultiSceneRenderJobIssueCode.SCENE_FAILED))
            status = TurkuMultiSceneRenderJobStatus.FAILED_SAFE;
        else if (!issues.isEmpty())
            status = TurkuMultiSceneRenderJobStatus.REJECTED_UNSAFE;
        else
            status = TurkuMultiSceneRenderJobStatus.READY_FOR_LOCAL_MANUAL_RENDER;

        return new TurkuMultiSceneRenderJobPlan(opaqueJobRef, status, sceneRefList, issues);
    }
}
