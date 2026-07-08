package com.anadoluatlasi.backend.turkuvideo;

import java.util.*;

/**
 * FP-037: Scene render plan service.
 * Validates scene plans and assembles bundles via FP-036.
 * No ProcessBuilder. No paths/commands/content.
 */
public class TurkuSceneRenderPlanService {

    private static final int MAX_SCENES_PER_BATCH = 20;
    private static final int MAX_DURATION_SECONDS = 5;
    private static final int MAX_FPS = 30;
    private static final int MAX_WIDTH = 1280;
    private static final int MAX_HEIGHT = 720;

    private final TurkuRenderBundleManifestService bundleService;

    public TurkuSceneRenderPlanService(TurkuRenderBundleManifestService bundleService) {
        this.bundleService = bundleService != null ? bundleService : new TurkuRenderBundleManifestService();
    }

    public TurkuSceneRenderPlanService() {
        this(new TurkuRenderBundleManifestService());
    }

    /**
     * Validate a single scene render plan.
     */
    public TurkuSceneRenderPlan validate(String opaqueSceneRef, String opaquePlanRef,
            int sceneOrder, int durationSeconds, int fps, int width, int height,
            List<TurkuSceneRenderArtifactExpectation> expectations) {

        var issues = new ArrayList<TurkuSceneRenderPlanIssueCode>();
        var safeExpectations = expectations != null ? expectations : List.<TurkuSceneRenderArtifactExpectation>of();

        if (opaqueSceneRef == null || opaqueSceneRef.isBlank())
            issues.add(TurkuSceneRenderPlanIssueCode.SCENE_REF_MISSING);
        if (opaquePlanRef == null || opaquePlanRef.isBlank())
            issues.add(TurkuSceneRenderPlanIssueCode.PLAN_REF_MISSING);
        if (sceneOrder < 1)
            issues.add(TurkuSceneRenderPlanIssueCode.SCENE_ORDER_INVALID);
        if (durationSeconds < 1 || durationSeconds > MAX_DURATION_SECONDS)
            issues.add(TurkuSceneRenderPlanIssueCode.DURATION_EXCEEDS_LIMIT);
        if (fps < 1 || fps > MAX_FPS)
            issues.add(TurkuSceneRenderPlanIssueCode.FPS_EXCEEDS_LIMIT);
        if (width < 1 || width > MAX_WIDTH)
            issues.add(TurkuSceneRenderPlanIssueCode.WIDTH_EXCEEDS_LIMIT);
        if (height < 1 || height > MAX_HEIGHT)
            issues.add(TurkuSceneRenderPlanIssueCode.HEIGHT_EXCEEDS_LIMIT);

        // Primary video expectation
        long primaryCount = safeExpectations.stream()
                .filter(e -> e.getRole() == TurkuRenderBundleArtifactRole.PRIMARY_VIDEO_CLIP).count();
        if (primaryCount == 0)
            issues.add(TurkuSceneRenderPlanIssueCode.MISSING_PRIMARY_VIDEO_EXPECTATION);
        else if (primaryCount > 1)
            issues.add(TurkuSceneRenderPlanIssueCode.DUPLICATE_PRIMARY_VIDEO_EXPECTATION);

        // Unknown roles
        if (safeExpectations.stream().anyMatch(e -> !e.getRole().isAllowed()))
            issues.add(TurkuSceneRenderPlanIssueCode.UNSUPPORTED_ARTIFACT_ROLE);

        var status = issues.isEmpty()
                ? TurkuSceneRenderPlanStatus.READY_FOR_BUNDLE_ASSEMBLY
                : TurkuSceneRenderPlanStatus.REJECTED_UNSAFE;

        return new TurkuSceneRenderPlan(opaqueSceneRef, opaquePlanRef,
                sceneOrder, durationSeconds, fps, width, height,
                safeExpectations, status, issues);
    }

    /**
     * Validate a batch of scene orders for uniqueness/count.
     */
    public List<TurkuSceneRenderPlanIssueCode> validateBatch(List<Integer> sceneOrders) {
        var issues = new ArrayList<TurkuSceneRenderPlanIssueCode>();
        if (sceneOrders == null) return issues;
        if (sceneOrders.size() > MAX_SCENES_PER_BATCH)
            issues.add(TurkuSceneRenderPlanIssueCode.TOO_MANY_SCENES);
        var seen = new HashSet<Integer>();
        for (int order : sceneOrders) {
            if (!seen.add(order))
                issues.add(TurkuSceneRenderPlanIssueCode.SCENE_ORDER_DUPLICATE);
        }
        return issues;
    }

    /**
     * Assemble bundle from validated plan + artifact refs.
     * Uses FP-036 bundle manifest service.
     */
    public TurkuRenderBundleManifest assembleBundle(TurkuSceneRenderPlan plan,
            List<TurkuRenderBundleArtifactRef> artifactRefs) {

        // Plan must be ready
        if (plan == null || plan.getStatus() != TurkuSceneRenderPlanStatus.READY_FOR_BUNDLE_ASSEMBLY)
            return bundleService.build(null, List.of());

        var safeRefs = artifactRefs != null ? artifactRefs : List.<TurkuRenderBundleArtifactRef>of();

        // Check required expectations are met
        for (var expectation : plan.getArtifactExpectations()) {
            if (expectation.isRequired()) {
                boolean found = safeRefs.stream()
                        .anyMatch(r -> r.getRole() == expectation.getRole());
                if (!found) {
                    // Missing required artifact — pass through so bundle service detects it
                    return bundleService.build(plan.getOpaquePlanRef(), safeRefs);
                }
            }
        }

        // Check role matches
        for (var ref : safeRefs) {
            boolean expectedRole = plan.getArtifactExpectations().stream()
                    .anyMatch(e -> e.getRole() == ref.getRole());
            if (!expectedRole) {
                // Unexpected role — still pass through, bundle service handles UNKNOWN
            }
        }

        return bundleService.build(plan.getOpaquePlanRef(), safeRefs);
    }
}
