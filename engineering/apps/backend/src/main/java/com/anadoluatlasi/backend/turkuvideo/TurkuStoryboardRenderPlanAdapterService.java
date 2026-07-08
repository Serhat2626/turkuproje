package com.anadoluatlasi.backend.turkuvideo;

import java.util.*;

/**
 * FP-046: Adapter service — converts safe storyboard metadata into FP-037 scene render plans.
 * No ProcessBuilder. No FFmpeg. No paths/commands/content.
 */
public class TurkuStoryboardRenderPlanAdapterService {

    private static final int MAX_SCENES = 20;
    private static final int MAX_DURATION = 5;
    private static final int MAX_FPS = 30;
    private static final int MAX_WIDTH = 1280;
    private static final int MAX_HEIGHT = 720;

    private final TurkuSceneRenderPlanService planService;

    public TurkuStoryboardRenderPlanAdapterService(TurkuSceneRenderPlanService planService) {
        this.planService = planService != null ? planService : new TurkuSceneRenderPlanService();
    }

    public TurkuStoryboardRenderPlanAdapterService() {
        this(new TurkuSceneRenderPlanService());
    }

    /**
     * Convert storyboard scene inputs into validated scene render plans.
     */
    public TurkuStoryboardRenderPlanAdapterResult adapt(String opaqueStoryboardRef,
            String opaqueStoryRef, List<TurkuStoryboardSceneRenderInput> sceneInputs) {

        String adapterRef = "adapter:" + UUID.randomUUID();
        var issues = new ArrayList<TurkuStoryboardRenderAdapterIssueCode>();
        var safe = sceneInputs != null ? sceneInputs : List.<TurkuStoryboardSceneRenderInput>of();

        // Validate refs
        if (opaqueStoryboardRef == null || opaqueStoryboardRef.isBlank())
            issues.add(TurkuStoryboardRenderAdapterIssueCode.STORYBOARD_REF_MISSING);
        if (opaqueStoryRef == null || opaqueStoryRef.isBlank())
            issues.add(TurkuStoryboardRenderAdapterIssueCode.STORY_REF_MISSING);

        // Validate scene count
        if (safe.isEmpty()) {
            issues.add(TurkuStoryboardRenderAdapterIssueCode.NO_SCENES);
            return fail(adapterRef, opaqueStoryboardRef, opaqueStoryRef, issues);
        }
        if (safe.size() > MAX_SCENES) {
            issues.add(TurkuStoryboardRenderAdapterIssueCode.TOO_MANY_SCENES);
            return fail(adapterRef, opaqueStoryboardRef, opaqueStoryRef, issues);
        }

        // Validate scene orders
        var orderSet = new HashSet<Integer>();
        for (var input : safe) {
            if (input.getOpaqueSceneRef() == null || input.getOpaqueSceneRef().isBlank())
                issues.add(TurkuStoryboardRenderAdapterIssueCode.SCENE_REF_MISSING);
            if (input.getSceneOrder() < 1)
                issues.add(TurkuStoryboardRenderAdapterIssueCode.SCENE_ORDER_INVALID);
            if (!orderSet.add(input.getSceneOrder()))
                issues.add(TurkuStoryboardRenderAdapterIssueCode.SCENE_ORDER_DUPLICATE);
            if (input.getTargetDurationSeconds() < 1 || input.getTargetDurationSeconds() > MAX_DURATION)
                issues.add(TurkuStoryboardRenderAdapterIssueCode.DURATION_EXCEEDS_LIMIT);
            if (input.getTargetFps() < 1 || input.getTargetFps() > MAX_FPS)
                issues.add(TurkuStoryboardRenderAdapterIssueCode.FPS_EXCEEDS_LIMIT);
            if (input.getTargetWidth() < 1 || input.getTargetWidth() > MAX_WIDTH)
                issues.add(TurkuStoryboardRenderAdapterIssueCode.WIDTH_EXCEEDS_LIMIT);
            if (input.getTargetHeight() < 1 || input.getTargetHeight() > MAX_HEIGHT)
                issues.add(TurkuStoryboardRenderAdapterIssueCode.HEIGHT_EXCEEDS_LIMIT);
        }

        if (!issues.isEmpty())
            return fail(adapterRef, opaqueStoryboardRef, opaqueStoryRef, issues);

        // Convert to FP-037 scene render plans
        var primaryExpectation = new TurkuSceneRenderArtifactExpectation(
                TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP, true);
        var plans = new ArrayList<TurkuSceneRenderPlan>();
        for (var input : safe) {
            var plan = planService.validate(input.getOpaqueSceneRef(),
                    "plan:" + UUID.randomUUID(), input.getSceneOrder(),
                    input.getTargetDurationSeconds(), input.getTargetFps(),
                    input.getTargetWidth(), input.getTargetHeight(),
                    List.of(primaryExpectation));
            if (plan.getStatus() != TurkuSceneRenderPlanStatus.READY_FOR_BUNDLE_ASSEMBLY) {
                issues.add(TurkuStoryboardRenderAdapterIssueCode.SCENE_PLAN_VALIDATION_FAILED);
                return fail(adapterRef, opaqueStoryboardRef, opaqueStoryRef, issues);
            }
            plans.add(plan);
        }

        return new TurkuStoryboardRenderPlanAdapterResult(adapterRef,
                opaqueStoryboardRef, opaqueStoryRef,
                TurkuStoryboardRenderAdapterStatus.READY_FOR_INTERNAL_RENDER_CHAIN,
                List.of(), plans, plans.size());
    }

    private TurkuStoryboardRenderPlanAdapterResult fail(String ref, String sbRef, String storyRef,
            List<TurkuStoryboardRenderAdapterIssueCode> issues) {
        return new TurkuStoryboardRenderPlanAdapterResult(ref, sbRef, storyRef,
                issues.stream().anyMatch(i ->
                        i == TurkuStoryboardRenderAdapterIssueCode.STORYBOARD_REF_MISSING
                        || i == TurkuStoryboardRenderAdapterIssueCode.STORY_REF_MISSING)
                        ? TurkuStoryboardRenderAdapterStatus.REJECTED_UNSAFE
                        : TurkuStoryboardRenderAdapterStatus.FAILED_SAFE,
                issues, List.of(), 0);
    }
}
