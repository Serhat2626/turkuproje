package com.anadoluatlasi.backend.turkuvideo;

import java.util.List;
import java.util.UUID;

/**
 * FP-019 Task 04: Image generation batch.
 * Safe internal references only — no base64, no public URLs, no storage keys.
 */
public final class TurkuSceneImageBatch {

    private final UUID batchId;
    private final UUID projectId;
    private TurkuImageBatchStatus status;
    private final List<TurkuSceneImageRequest> imageRequests;
    private List<TurkuSceneImageResult> imageResults;
    private final List<String> reviewFlags;

    public TurkuSceneImageBatch(UUID batchId, UUID projectId,
                                 TurkuImageBatchStatus status,
                                 List<TurkuSceneImageRequest> imageRequests,
                                 List<String> reviewFlags) {
        this.batchId = batchId;
        this.projectId = projectId;
        this.status = status;
        this.imageRequests = imageRequests != null ? List.copyOf(imageRequests) : List.of();
        this.imageResults = List.of();
        this.reviewFlags = reviewFlags != null ? List.copyOf(reviewFlags) : List.of();
    }

    public UUID getBatchId() { return batchId; }
    public UUID getProjectId() { return projectId; }
    public TurkuImageBatchStatus getStatus() { return status; }
    public List<TurkuSceneImageRequest> getImageRequests() { return imageRequests; }
    public List<TurkuSceneImageResult> getImageResults() { return imageResults; }
    public List<String> getReviewFlags() { return reviewFlags; }

    public void setStatus(TurkuImageBatchStatus status) { this.status = status; }
    public void setImageResults(List<TurkuSceneImageResult> results) {
        this.imageResults = results != null ? List.copyOf(results) : List.of();
    }
}
