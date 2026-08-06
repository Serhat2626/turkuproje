package com.anadoluatlasi.backend.turkuvideo;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity — turku_video_projects tablosuna bağlı.
 * Domain object (TurkuVideoProject) ile ayrı tutulur.
 * Complex alanlar JSONB string olarak saklanır, adapter'da dönüştürülür.
 */
@Entity
@Table(name = "turku_video_projects")
public class JpaTurkuVideoProjectEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "turku_name", nullable = false, length = 200)
    private String turkuName;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "source_notes", columnDefinition = "text")
    private String sourceNotes;

    @Column(name = "mood", columnDefinition = "text")
    private String mood;

    @Column(name = "target_duration_minutes")
    private Integer targetDurationMinutes;

    @Column(name = "narration_style", columnDefinition = "text")
    private String narrationStyle;

    @Column(name = "visual_style", columnDefinition = "text")
    private String visualStyle;

    @Column(name = "workflow_stage", nullable = false, length = 50)
    private String workflowStage;

    @Column(name = "source_confidence", nullable = false, length = 50)
    private String sourceConfidence;

    @Column(name = "needs_human_review", nullable = false)
    private boolean needsHumanReview;

    @Column(name = "copyright_review_required", nullable = false)
    private boolean copyrightReviewRequired;

    @Column(name = "created_by_actor_reference", length = 100)
    private String createdByActorReference;

    @Column(name = "provider_mode", nullable = false, length = 50)
    private String providerMode;

    // ── JSONB alanları (complex nesneler) ──

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "story_package_json", columnDefinition = "jsonb")
    private String storyPackageJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scenario_plan_json", columnDefinition = "jsonb")
    private String scenarioPlanJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "storyboard_plan_json", columnDefinition = "jsonb")
    private String storyboardPlanJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scene_asset_plan_json", columnDefinition = "jsonb")
    private String sceneAssetPlanJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_batch_json", columnDefinition = "jsonb")
    private String imageBatchJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "composition_plan_json", columnDefinition = "jsonb")
    private String compositionPlanJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "render_manifest_json", columnDefinition = "jsonb")
    private String renderManifestJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "render_job_json", columnDefinition = "jsonb")
    private String renderJobJson;

    // ── Auditing ──

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── JPA requires no-arg constructor ──
    protected JpaTurkuVideoProjectEntity() {}

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTurkuName() { return turkuName; }
    public void setTurkuName(String turkuName) { this.turkuName = turkuName; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getSourceNotes() { return sourceNotes; }
    public void setSourceNotes(String sourceNotes) { this.sourceNotes = sourceNotes; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public Integer getTargetDurationMinutes() { return targetDurationMinutes; }
    public void setTargetDurationMinutes(Integer v) { this.targetDurationMinutes = v; }

    public String getNarrationStyle() { return narrationStyle; }
    public void setNarrationStyle(String narrationStyle) { this.narrationStyle = narrationStyle; }

    public String getVisualStyle() { return visualStyle; }
    public void setVisualStyle(String visualStyle) { this.visualStyle = visualStyle; }

    public String getWorkflowStage() { return workflowStage; }
    public void setWorkflowStage(String workflowStage) { this.workflowStage = workflowStage; }

    public String getSourceConfidence() { return sourceConfidence; }
    public void setSourceConfidence(String sourceConfidence) { this.sourceConfidence = sourceConfidence; }

    public boolean isNeedsHumanReview() { return needsHumanReview; }
    public void setNeedsHumanReview(boolean v) { this.needsHumanReview = v; }

    public boolean isCopyrightReviewRequired() { return copyrightReviewRequired; }
    public void setCopyrightReviewRequired(boolean v) { this.copyrightReviewRequired = v; }

    public String getCreatedByActorReference() { return createdByActorReference; }
    public void setCreatedByActorReference(String v) { this.createdByActorReference = v; }

    public String getProviderMode() { return providerMode; }
    public void setProviderMode(String providerMode) { this.providerMode = providerMode; }

    public String getStoryPackageJson() { return storyPackageJson; }
    public void setStoryPackageJson(String v) { this.storyPackageJson = v; }

    public String getScenarioPlanJson() { return scenarioPlanJson; }
    public void setScenarioPlanJson(String v) { this.scenarioPlanJson = v; }

    public String getStoryboardPlanJson() { return storyboardPlanJson; }
    public void setStoryboardPlanJson(String v) { this.storyboardPlanJson = v; }

    public String getSceneAssetPlanJson() { return sceneAssetPlanJson; }
    public void setSceneAssetPlanJson(String v) { this.sceneAssetPlanJson = v; }

    public String getImageBatchJson() { return imageBatchJson; }
    public void setImageBatchJson(String v) { this.imageBatchJson = v; }

    public String getCompositionPlanJson() { return compositionPlanJson; }
    public void setCompositionPlanJson(String v) { this.compositionPlanJson = v; }

    public String getRenderManifestJson() { return renderManifestJson; }
    public void setRenderManifestJson(String v) { this.renderManifestJson = v; }

    public String getRenderJobJson() { return renderJobJson; }
    public void setRenderJobJson(String v) { this.renderJobJson = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
