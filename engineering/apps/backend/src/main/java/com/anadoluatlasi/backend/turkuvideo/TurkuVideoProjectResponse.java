package com.anadoluatlasi.backend.turkuvideo;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-019 Task 02: Project detail response DTO. Safe fields only.
 */
public final class TurkuVideoProjectResponse {
    private final UUID id;
    private final String turkuName;
    private final String region;
    private final String sourceNotes;
    private final String mood;
    private final Integer targetDurationMinutes;
    private final String narrationStyle;
    private final String visualStyle;
    private final TurkuVideoWorkflowStage workflowStage;
    private final TurkuSourceConfidence sourceConfidence;
    private final boolean needsHumanReview;
    private final boolean copyrightReviewRequired;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private TurkuVideoProjectResponse(TurkuVideoProject p) {
        this.id = p.getId();
        this.turkuName = p.getTurkuName();
        this.region = p.getRegion();
        this.sourceNotes = p.getSourceNotes();
        this.mood = p.getMood();
        this.targetDurationMinutes = p.getTargetDurationMinutes();
        this.narrationStyle = p.getNarrationStyle();
        this.visualStyle = p.getVisualStyle();
        this.workflowStage = p.getWorkflowStage();
        this.sourceConfidence = p.getSourceConfidence();
        this.needsHumanReview = p.isNeedsHumanReview();
        this.copyrightReviewRequired = p.isCopyrightReviewRequired();
        this.createdAt = p.getCreatedAt();
        this.updatedAt = p.getUpdatedAt();
    }

    public static TurkuVideoProjectResponse from(TurkuVideoProject p) {
        return new TurkuVideoProjectResponse(p);
    }

    public UUID getId() { return id; }
    public String getTurkuName() { return turkuName; }
    public String getRegion() { return region; }
    public String getSourceNotes() { return sourceNotes; }
    public String getMood() { return mood; }
    public Integer getTargetDurationMinutes() { return targetDurationMinutes; }
    public String getNarrationStyle() { return narrationStyle; }
    public String getVisualStyle() { return visualStyle; }
    public TurkuVideoWorkflowStage getWorkflowStage() { return workflowStage; }
    public TurkuSourceConfidence getSourceConfidence() { return sourceConfidence; }
    public boolean isNeedsHumanReview() { return needsHumanReview; }
    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
