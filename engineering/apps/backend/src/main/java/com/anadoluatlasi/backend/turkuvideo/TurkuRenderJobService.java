package com.anadoluatlasi.backend.turkuvideo;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TurkuRenderJobService {

    private final TurkuVideoProjectRepository repository;
    private final TurkuRenderManifestService renderManifestService;

    public TurkuRenderJobService(TurkuVideoProjectRepository repository,
                                  TurkuRenderManifestService renderManifestService) {
        this.repository = repository;
        this.renderManifestService = renderManifestService;
    }

    public TurkuRenderJobResponse prepareRenderJob(UUID projectId) {
        TurkuVideoProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getWorkflowStage() != TurkuVideoWorkflowStage.RENDER_MANIFEST_PREPARED) {
            throw new IllegalStateException(
                    "Render job requires RENDER_MANIFEST_PREPARED, current: " + project.getWorkflowStage());
        }

        TurkuRenderManifest manifest = project.getRenderManifest();
        if (manifest == null) {
            throw new IllegalStateException("Render manifest is required");
        }

        TurkuRenderReadinessReport readiness = renderManifestService.evaluateReadiness(projectId);
        if (readiness.getStatus() == TurkuRenderReadinessStatus.BLOCKED) {
            throw new IllegalStateException("Render readiness is BLOCKED: " + readiness.getIssues());
        }

        List<String> reviewFlags = new ArrayList<>();
        reviewFlags.add("HUMAN_REVIEW_REQUIRED");
        reviewFlags.add("COPYRIGHT_REVIEW_REQUIRED");
        reviewFlags.add("NOT_PUBLISH_READY");
        reviewFlags.add("NOT_EXPORT_READY");
        reviewFlags.add("INTERNAL_RENDER_ONLY");

        String manifestRef = "internal-manifest:" + manifest.getManifestId();
        TurkuRenderJob job = new TurkuRenderJob(UUID.randomUUID(), projectId, manifestRef, reviewFlags);

        project.setRenderJob(job);
        project.advanceTo(TurkuVideoWorkflowStage.RENDER_JOB_PREPARED);
        repository.save(project);

        return new TurkuRenderJobResponse(
                projectId, job.getJobId(), job.getStatus(),
                project.getWorkflowStage(), manifestRef,
                0, reviewFlags, "Dahili render job hazirlandir — yayin/disa aktarim yok");
    }

    public TurkuRenderJobResponse getRenderJob(UUID projectId) {
        TurkuVideoProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        TurkuRenderJob job = project.getRenderJob();
        if (job == null) {
            return new TurkuRenderJobResponse(
                    projectId, null, null,
                    project.getWorkflowStage(), null,
                    0, List.of("NO_RENDER_JOB"),
                    "Render job henuz olusturulmadi");
        }

        return new TurkuRenderJobResponse(
                projectId, job.getJobId(), job.getStatus(),
                project.getWorkflowStage(), job.getManifestRef(),
                job.getAttempts().size(), job.getReviewFlags(),
                "Render job durumu: " + job.getStatus());
    }
}
