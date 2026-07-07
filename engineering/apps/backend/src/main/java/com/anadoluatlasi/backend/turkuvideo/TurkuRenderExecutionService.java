package com.anadoluatlasi.backend.turkuvideo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FP-022 Task 03: Render execution service.
 *
 * <p>Executes render jobs through a gateway (stub by default).
 * No real video rendering. No binary file. No export/publish.</p>
 */
public class TurkuRenderExecutionService {

    private final TurkuVideoProjectRepository repository;
    private final TurkuRenderRunnerGateway renderRunnerGateway;

    public TurkuRenderExecutionService(TurkuVideoProjectRepository repository,
                                        TurkuRenderRunnerGateway renderRunnerGateway) {
        this.repository = repository;
        this.renderRunnerGateway = renderRunnerGateway;
    }

    /**
     * Run render job through gateway. Requires RENDER_JOB_PREPARED.
     */
    public TurkuRenderExecutionResponse runRenderJob(UUID projectId) {
        TurkuVideoProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getWorkflowStage() != TurkuVideoWorkflowStage.RENDER_JOB_PREPARED) {
            throw new IllegalStateException(
                    "Run render job requires RENDER_JOB_PREPARED, current: "
                    + project.getWorkflowStage());
        }

        TurkuRenderJob job = project.getRenderJob();
        if (job == null) {
            throw new IllegalStateException("Render job is required");
        }

        TurkuRenderManifest manifest = project.getRenderManifest();
        if (manifest == null) {
            throw new IllegalStateException("Render manifest is required");
        }

        // Build execution plan from manifest
        TurkuRenderExecutionPlan executionPlan = new TurkuRenderExecutionPlan(
                UUID.randomUUID(),
                manifest.getScenes().size(),
                manifest.getTotalDurationSeconds(),
                manifest.getTargetResolution(),
                manifest.getTargetFps(),
                manifest.getTargetFormat(),
                manifest.getTotalDurationSeconds() * 0.5,
                "deterministic-stub",
                "DETERMINISTIC_STUB");

        // Advance to RUNNING
        project.advanceTo(TurkuVideoWorkflowStage.RENDER_JOB_RUNNING);
        job.setStatus(TurkuRenderJobStatus.RUNNING);
        repository.save(project);

        // Execute through gateway
        TurkuRenderExecutionResult result;
        try {
            result = renderRunnerGateway.execute(executionPlan);
        } catch (Exception e) {
            // Failed safe
            return handleFailure(project, job, TurkuRenderFailureCode.RENDERER_INTERNAL_ERROR,
                    "Renderer error: " + e.getMessage());
        }

        if (!result.isSuccess()) {
            return handleFailure(project, job,
                    result.getFailureCode() != null ? result.getFailureCode()
                            : TurkuRenderFailureCode.RENDERER_INTERNAL_ERROR,
                    result.getFailureMessage());
        }

        // Success path
        TurkuRenderAttempt attempt = new TurkuRenderAttempt(
                UUID.randomUUID(), job.getAttempts().size() + 1,
                Instant.now().minusMillis(result.getDurationMs()), Instant.now(),
                TurkuRenderJobStatus.COMPLETED_INTERNAL, null,
                result.getDurationMs(), "deterministic-stub");

        job.addAttempt(attempt);
        job.setStatus(TurkuRenderJobStatus.COMPLETED_INTERNAL);
        if (result.getArtifact() != null) {
            job.setArtifact(result.getArtifact());
        }
        project.advanceTo(TurkuVideoWorkflowStage.RENDER_JOB_COMPLETED_INTERNAL);
        repository.save(project);

        List<String> reviewFlags = new ArrayList<>();
        reviewFlags.add("HUMAN_REVIEW_REQUIRED");
        reviewFlags.add("COPYRIGHT_REVIEW_REQUIRED");
        reviewFlags.add("NOT_PUBLISH_READY");
        reviewFlags.add("NOT_EXPORT_READY");
        reviewFlags.add("INTERNAL_RENDER_ONLY");
        reviewFlags.add("COMPLETED_INTERNAL");

        String artifactRef = result.getArtifact() != null
                ? result.getArtifact().getArtifactRef() : null;

        return new TurkuRenderExecutionResponse(
                projectId, job.getJobId(), attempt.getAttemptId(),
                TurkuRenderJobStatus.COMPLETED_INTERNAL,
                project.getWorkflowStage(),
                result.getSafeTraceRef(), artifactRef,
                reviewFlags,
                "Dahili render tamamlandı — yayın/dışa aktarım yok",
                null);
    }

    private TurkuRenderExecutionResponse handleFailure(TurkuVideoProject project,
                                                         TurkuRenderJob job,
                                                         TurkuRenderFailureCode failureCode,
                                                         String failureMessage) {
        TurkuRenderAttempt failedAttempt = new TurkuRenderAttempt(
                UUID.randomUUID(), job.getAttempts().size() + 1,
                Instant.now(), Instant.now(),
                TurkuRenderJobStatus.FAILED_SAFE, failureCode, 0, "deterministic-stub");

        job.addAttempt(failedAttempt);
        job.setStatus(TurkuRenderJobStatus.FAILED_SAFE);
        project.advanceTo(TurkuVideoWorkflowStage.RENDER_JOB_FAILED_SAFE);
        repository.save(project);

        List<String> reviewFlags = new ArrayList<>();
        reviewFlags.add("HUMAN_REVIEW_REQUIRED");
        reviewFlags.add("NOT_PUBLISH_READY");
        reviewFlags.add("NOT_EXPORT_READY");
        reviewFlags.add("RENDER_FAILED_SAFE");

        return new TurkuRenderExecutionResponse(
                project.getId(), job.getJobId(), failedAttempt.getAttemptId(),
                TurkuRenderJobStatus.FAILED_SAFE,
                project.getWorkflowStage(),
                null, null, reviewFlags,
                "Render başarısız (güvenli kapanış): " + failureMessage,
                failureCode);
    }

    /**
     * Get render execution safety status for a project.
     */
    public TurkuRenderExecutionSafetyResponse getRenderExecutionSafety(UUID projectId) {
        TurkuVideoProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        TurkuRenderJob job = project.getRenderJob();

        List<String> safetyFlags = new ArrayList<>();
        safetyFlags.add("NO_REAL_RENDER");
        safetyFlags.add("NO_BINARY_OUTPUT");
        safetyFlags.add("NO_PUBLIC_DELIVERY");
        safetyFlags.add("NO_EXPORT");

        List<String> reviewFlags = new ArrayList<>();
        reviewFlags.add("HUMAN_REVIEW_REQUIRED");
        reviewFlags.add("COPYRIGHT_REVIEW_REQUIRED");
        reviewFlags.add("NOT_PUBLISH_READY");
        reviewFlags.add("NOT_EXPORT_READY");
        reviewFlags.add("INTERNAL_ONLY");

        List<TurkuRenderFailureCode> failureCodes = new ArrayList<>();
        List<String> blockedReasons = new ArrayList<>();

        if (job != null && job.getStatus() == TurkuRenderJobStatus.FAILED_SAFE) {
            job.getAttempts().stream()
                    .filter(a -> a.getFailureCode() != null)
                    .forEach(a -> failureCodes.add(a.getFailureCode()));
        }

        TurkuRenderExecutionSafetyEnvelope envelope = new TurkuRenderExecutionSafetyEnvelope(
                "DETERMINISTIC_STUB", safetyFlags, reviewFlags, failureCodes, blockedReasons);

        TurkuRenderJobStatus jobStatus = job != null ? job.getStatus() : null;
        UUID jobId = job != null ? job.getJobId() : null;

        return new TurkuRenderExecutionSafetyResponse(
                projectId, jobId, jobStatus, project.getWorkflowStage(),
                envelope, "Render güvenlik durumu: dahili, yayın/dışa aktarım yok");
    }
}
