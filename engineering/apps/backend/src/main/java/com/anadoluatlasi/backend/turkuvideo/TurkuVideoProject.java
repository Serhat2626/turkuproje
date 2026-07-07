package com.anadoluatlasi.backend.turkuvideo;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-019 Task 02: Türkü video project domain object.
 *
 * <p>In-memory representation for the bootstrap phase. Does not require
 * database migration — stored in a ConcurrentHashMap repository.</p>
 *
 * <p>Does not store/expose: lyrics, copyrighted media, public URLs,
 * signed URLs, CDN URLs, storage keys, provider payloads, base64,
 * secrets, tokens, JWT, session, or credentials.</p>
 */
public final class TurkuVideoProject {

    private final UUID id;
    private final String turkuName;
    private final String region;
    private final String sourceNotes;
    private final String mood;
    private final Integer targetDurationMinutes;
    private final String narrationStyle;
    private final String visualStyle;
    private TurkuVideoWorkflowStage workflowStage;
    private TurkuSourceConfidence sourceConfidence;
    private boolean needsHumanReview;
    private boolean copyrightReviewRequired;
    private final String createdByActorReference;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Planning outputs (Task 03) ──
    private TurkuStoryPackage storyPackage;
    private TurkuScenarioPlan scenarioPlan;
    private TurkuStoryboardPlan storyboardPlan;

    // ── Asset outputs (Task 04) ──
    private TurkuSceneAssetPlan sceneAssetPlan;
    private TurkuSceneImageBatch imageBatch;

    // ── Provider mode (FP-020 Task 02) ──
    private TurkuProviderMode providerMode = TurkuProviderMode.DETERMINISTIC_STUB;

    // ── Composition (FP-021 Task 02) ──
    private TurkuVideoCompositionPlan compositionPlan;

    // ── Render (FP-021 Task 03) ──
    private TurkuRenderManifest renderManifest;

    private TurkuVideoProject(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.turkuName = builder.turkuName;
        this.region = builder.region;
        this.sourceNotes = builder.sourceNotes;
        this.mood = builder.mood;
        this.targetDurationMinutes = builder.targetDurationMinutes;
        this.narrationStyle = builder.narrationStyle;
        this.visualStyle = builder.visualStyle;
        this.workflowStage = builder.workflowStage != null ? builder.workflowStage : TurkuVideoWorkflowStage.DRAFT_CREATED;
        this.sourceConfidence = builder.sourceConfidence != null ? builder.sourceConfidence : TurkuSourceConfidence.UNKNOWN;
        this.needsHumanReview = builder.needsHumanReview;
        this.copyrightReviewRequired = builder.copyrightReviewRequired;
        this.createdByActorReference = builder.createdByActorReference;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    // ── Getters ──

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
    public String getCreatedByActorReference() { return createdByActorReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public TurkuStoryPackage getStoryPackage() { return storyPackage; }
    public TurkuScenarioPlan getScenarioPlan() { return scenarioPlan; }
    public TurkuStoryboardPlan getStoryboardPlan() { return storyboardPlan; }

    public void setStoryPackage(TurkuStoryPackage v) { this.storyPackage = v; }
    public void setScenarioPlan(TurkuScenarioPlan v) { this.scenarioPlan = v; }
    public void setStoryboardPlan(TurkuStoryboardPlan v) { this.storyboardPlan = v; }
    public TurkuSceneAssetPlan getSceneAssetPlan() { return sceneAssetPlan; }
    public TurkuSceneImageBatch getImageBatch() { return imageBatch; }
    public void setSceneAssetPlan(TurkuSceneAssetPlan v) { this.sceneAssetPlan = v; }
    public void setImageBatch(TurkuSceneImageBatch v) { this.imageBatch = v; }
    public TurkuProviderMode getProviderMode() { return providerMode; }
    public void setProviderMode(TurkuProviderMode v) { this.providerMode = v; }
    public TurkuVideoCompositionPlan getCompositionPlan() { return compositionPlan; }
    public void setCompositionPlan(TurkuVideoCompositionPlan v) { this.compositionPlan = v; }
    public TurkuRenderManifest getRenderManifest() { return renderManifest; }
    public void setRenderManifest(TurkuRenderManifest v) { this.renderManifest = v; }

    // ── Stage Mutations (controlled) ──

    public void advanceTo(TurkuVideoWorkflowStage newStage) {
        this.workflowStage = newStage;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "TurkuVideoProject{" +
                "id=" + id +
                ", turkuName='" + turkuName + '\'' +
                ", workflowStage=" + workflowStage +
                ", sourceConfidence=" + sourceConfidence +
                ", needsHumanReview=" + needsHumanReview +
                ", copyrightReviewRequired=" + copyrightReviewRequired +
                '}';
    }

    // ── Builder ──

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String turkuName;
        private String region;
        private String sourceNotes;
        private String mood;
        private Integer targetDurationMinutes;
        private String narrationStyle;
        private String visualStyle;
        private TurkuVideoWorkflowStage workflowStage;
        private TurkuSourceConfidence sourceConfidence;
        private boolean needsHumanReview = true;
        private boolean copyrightReviewRequired = true;
        private String createdByActorReference;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder turkuName(String v) { this.turkuName = v; return this; }
        public Builder region(String v) { this.region = v; return this; }
        public Builder sourceNotes(String v) { this.sourceNotes = v; return this; }
        public Builder mood(String v) { this.mood = v; return this; }
        public Builder targetDurationMinutes(Integer v) { this.targetDurationMinutes = v; return this; }
        public Builder narrationStyle(String v) { this.narrationStyle = v; return this; }
        public Builder visualStyle(String v) { this.visualStyle = v; return this; }
        public Builder workflowStage(TurkuVideoWorkflowStage v) { this.workflowStage = v; return this; }
        public Builder sourceConfidence(TurkuSourceConfidence v) { this.sourceConfidence = v; return this; }
        public Builder needsHumanReview(boolean v) { this.needsHumanReview = v; return this; }
        public Builder copyrightReviewRequired(boolean v) { this.copyrightReviewRequired = v; return this; }
        public Builder createdByActorReference(String v) { this.createdByActorReference = v; return this; }
        public Builder createdAt(LocalDateTime v) { this.createdAt = v; return this; }

        public TurkuVideoProject build() {
            if (turkuName == null || turkuName.isBlank()) {
                throw new IllegalArgumentException("turkuName is required");
            }
            return new TurkuVideoProject(this);
        }
    }
}
