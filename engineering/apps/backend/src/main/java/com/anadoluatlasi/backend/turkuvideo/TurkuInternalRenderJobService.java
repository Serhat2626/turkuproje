package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-025 Task 03: Internal render job service.
 * Pure/stateless. No persistence, no FFmpeg, no filesystem, no provider.
 */
public class TurkuInternalRenderJobService {

    private final TurkuInternalRenderJobTransitionValidator transitionValidator;

    public TurkuInternalRenderJobService() {
        this.transitionValidator = new TurkuInternalRenderJobTransitionValidator();
    }

    public TurkuInternalRenderJobService(TurkuInternalRenderJobTransitionValidator validator) {
        this.transitionValidator = validator;
    }

    // ── Job Creation ──

    /**
     * Create a safe initial job summary in DRAFT state.
     */
    public TurkuInternalRenderJobSummary createDraft() {
        var ref = TurkuInternalRenderJobRef.create();
        return new TurkuInternalRenderJobSummary(
                ref.toOpaqueRef(),
                TurkuInternalRenderJobStatus.DRAFT,
                0,
                TurkuInternalRenderJobProgress.StageLabel.HAZIRLANIYOR.getLabel(),
                "trace:" + UUID.randomUUID());
    }

    // ── Explicit Action Dispatcher ──

    /**
     * Apply an explicit lifecycle action. Returns transition result + updated summary.
     * No generic setStatus/updateStatus/patchStatus.
     */
    public TurkuInternalRenderJobActionResult applyAction(
            TurkuInternalRenderJobSummary current,
            TurkuInternalRenderJobAction action) {

        if (current == null) {
            return TurkuInternalRenderJobActionResult.denied(
                    null, action,
                    TurkuInternalRenderJobReasonCode.JOB_REF_INVALID);
        }

        var transitionResult = transitionValidator.validate(current.getStatus(), action);

        if (!transitionResult.isAllowed()) {
            return TurkuInternalRenderJobActionResult.denied(
                    current, action, transitionResult.getReasonCode());
        }

        var progress = progressForState(transitionResult.getToState());
        var updated = new TurkuInternalRenderJobSummary(
                current.getJobRef(),
                transitionResult.getToState(),
                progress.getProgressPercent(),
                progress.getCurrentStage().getLabel(),
                current.getSafeTraceRef());

        return TurkuInternalRenderJobActionResult.allowed(updated, action);
    }

    // ── Readiness Snapshot ──

    /**
     * Produce readiness snapshot based on FP-024 safety chain.
     */
    public TurkuInternalRenderReadinessSnapshot readiness(
            boolean configValid, boolean workspaceSafe,
            boolean executionBoundaryAvailable, boolean artifactVerifiable) {

        List<TurkuInternalRenderReadinessIssue> issues = new ArrayList<>();

        if (!configValid) issues.add(TurkuInternalRenderReadinessIssue.CONFIG_INVALID);
        if (!workspaceSafe) issues.add(TurkuInternalRenderReadinessIssue.WORKSPACE_REF_INVALID);
        if (!executionBoundaryAvailable) issues.add(TurkuInternalRenderReadinessIssue.EXECUTION_BOUNDARY_DISABLED);
        if (!artifactVerifiable) issues.add(TurkuInternalRenderReadinessIssue.ARTIFACT_VERIFICATION_FAILED);

        // Always require human review
        issues.add(TurkuInternalRenderReadinessIssue.HUMAN_REVIEW_REQUIRED);
        // Always forbid public delivery
        issues.add(TurkuInternalRenderReadinessIssue.PUBLIC_DELIVERY_FORBIDDEN);
        // Always forbid auto-approval
        issues.add(TurkuInternalRenderReadinessIssue.AUTO_APPROVAL_FORBIDDEN);

        TurkuInternalRenderReadinessStatus status;
        if (!configValid) {
            status = TurkuInternalRenderReadinessStatus.BLOCKED_BY_CONFIG;
        } else if (!workspaceSafe) {
            status = TurkuInternalRenderReadinessStatus.BLOCKED_BY_WORKSPACE;
        } else if (!executionBoundaryAvailable) {
            status = TurkuInternalRenderReadinessStatus.BLOCKED_BY_EXECUTION_BOUNDARY;
        } else if (!artifactVerifiable) {
            status = TurkuInternalRenderReadinessStatus.BLOCKED_BY_ARTIFACT_VERIFICATION;
        } else {
            status = TurkuInternalRenderReadinessStatus.READY_INTERNAL_ONLY;
        }

        return new TurkuInternalRenderReadinessSnapshot(status, issues);
    }

    // ── FP-024 Safety Chain Snapshot ──

    /**
     * Produce FP-024 safety chain snapshot. Safe booleans and labels only.
     */
    public TurkuInternalRenderSafetyChainSnapshot safetyChain(
            boolean configValid, String configModeLabel,
            boolean workspaceSafe,
            boolean executionBoundarySafe, String executionModeLabel,
            boolean artifactVerified, String verificationStatusLabel) {

        return new TurkuInternalRenderSafetyChainSnapshot(
                configValid, workspaceSafe, executionBoundarySafe, artifactVerified,
                configModeLabel, executionModeLabel, verificationStatusLabel);
    }

    // ── Progress Snapshots ──

    /**
     * Produce safe progress snapshot for a given state.
     */
    public TurkuInternalRenderJobProgress progressForState(TurkuInternalRenderJobStatus state) {
        if (state == null) state = TurkuInternalRenderJobStatus.DRAFT;
        return switch (state) {
            case DRAFT -> new TurkuInternalRenderJobProgress(
                    "", state, 0, TurkuInternalRenderJobProgress.StageLabel.HAZIRLANIYOR);
            case READY_FOR_INTERNAL_RENDER -> new TurkuInternalRenderJobProgress(
                    "", state, 10, TurkuInternalRenderJobProgress.StageLabel.YAPILANDIRMA_DOGRULANIYOR);
            case QUEUED_INTERNAL -> new TurkuInternalRenderJobProgress(
                    "", state, 25, TurkuInternalRenderJobProgress.StageLabel.CALISMA_ALANI_HAZIRLANIYOR);
            case RUNNING_INTERNAL -> new TurkuInternalRenderJobProgress(
                    "", state, 60, TurkuInternalRenderJobProgress.StageLabel.DAHILI_RENDER_CALISIYOR);
            case SUCCEEDED_INTERNAL -> new TurkuInternalRenderJobProgress(
                    "", state, 100, TurkuInternalRenderJobProgress.StageLabel.TAMAMLANDI);
            case FAILED_INTERNAL -> new TurkuInternalRenderJobProgress(
                    "", state, 0, TurkuInternalRenderJobProgress.StageLabel.HAZIRLANIYOR);
            case CANCELLED_INTERNAL -> new TurkuInternalRenderJobProgress(
                    "", state, 0, TurkuInternalRenderJobProgress.StageLabel.HAZIRLANIYOR);
            case EXPIRED_INTERNAL -> new TurkuInternalRenderJobProgress(
                    "", state, 0, TurkuInternalRenderJobProgress.StageLabel.HAZIRLANIYOR);
        };
    }
}
