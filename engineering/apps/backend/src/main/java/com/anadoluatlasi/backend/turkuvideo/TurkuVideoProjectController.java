package com.anadoluatlasi.backend.turkuvideo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.config.MinioConfig;
import com.anadoluatlasi.backend.review.AdminActorBoundaryPolicy;
import com.anadoluatlasi.backend.review.AdminActorResolutionException;
import com.anadoluatlasi.backend.review.AdminActorResolutionRequest;
import com.anadoluatlasi.backend.review.AdminActorResolver;
import com.anadoluatlasi.backend.review.ResolvedAdminActor;
import com.anadoluatlasi.backend.review.ReviewActorRole;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FP-019 Task 02: Admin controller for Türkü video project bootstrap.
 *
 * <p>Adds exactly 3 endpoints:
 * <ul>
 *   <li>POST /admin/turku-video-projects/bootstrap — create project</li>
 *   <li>GET /admin/turku-video-projects/{id} — get project</li>
 *   <li>GET /admin/turku-video-projects/{id}/progress — get progress</li>
 * </ul>
 *
 * <p>POST routes through AdminActorResolver / AdminActorBoundaryPolicy.
 * actorRole is TEST_FALLBACK compatibility input only, NOT production auth.
 * No publish/export/public delivery. No AI generation. No unsafe fields.</p>
 */
@RestController
@RequestMapping("/api/admin/turku-video-projects")
@Tag(name = "Türkü Video Projects", description = "Türkü video proje yönetimi (dahili/admin)")
public class TurkuVideoProjectController {

    private static final Logger log = LoggerFactory.getLogger(TurkuVideoProjectController.class);

    private final TurkuVideoProjectBootstrapService bootstrapService;
    private final TurkuVideoPlanningService planningService;
    private final TurkuVideoAssetPlanningService assetPlanningService;
    private final TurkuProviderDraftPlanningService providerDraftService;
    private final TurkuProviderImageBatchService providerImageBatchService;
    private final TurkuVideoCompositionService compositionService;
    private final TurkuRenderManifestService renderManifestService;
    private final TurkuRenderJobService renderJobService;
    private final TurkuRenderExecutionService renderExecutionService;
    private final TurkuStoryboardInternalRenderChainHarness storyboardChainHarness;
    private final AdminActorResolver actorResolver;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Autowired(required = false)
    private TurkuTtsService ttsService;

    @Autowired(required = false)
    private ElevenLabsTtsService elevenLabsTtsService;

    @Autowired(required = false)
    private FalImageGenerationService falImageService;

    @Autowired(required = false)
    private FalVideoGenerationService falVideoService;

    @Autowired
    private com.anadoluatlasi.backend.ai.repository.AiGenerationLogRepository aiLogRepository;

    private final TurkuVideoAssemblyService videoAssemblyService;

    public TurkuVideoProjectController(TurkuVideoProjectBootstrapService bootstrapService,
                                        TurkuVideoPlanningService planningService,
                                        TurkuVideoAssetPlanningService assetPlanningService,
                                        TurkuProviderDraftPlanningService providerDraftService,
                                        TurkuProviderImageBatchService providerImageBatchService,
                                        TurkuVideoCompositionService compositionService,
                                        TurkuRenderManifestService renderManifestService,
                                        TurkuRenderJobService renderJobService,
                                        TurkuRenderExecutionService renderExecutionService,
                                        TurkuStoryboardInternalRenderChainHarness storyboardChainHarness,
                                        AdminActorResolver actorResolver,
                                        MinioClient minioClient,
                                        MinioConfig minioConfig,
                                        TurkuVideoAssemblyService videoAssemblyService) {
        this.bootstrapService = bootstrapService;
        this.planningService = planningService;
        this.assetPlanningService = assetPlanningService;
        this.providerDraftService = providerDraftService;
        this.providerImageBatchService = providerImageBatchService;
        this.compositionService = compositionService;
        this.renderManifestService = renderManifestService;
        this.renderJobService = renderJobService;
        this.renderExecutionService = renderExecutionService;
        this.storyboardChainHarness = storyboardChainHarness;
        this.actorResolver = actorResolver;
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        this.videoAssemblyService = videoAssemblyService;
    }

    // ── POST /admin/turku-video-projects/bootstrap ──

    @PostMapping("/bootstrap")
    @Operation(summary = "Yeni türkü video projesi oluştur",
               description = "Türkü adı ile yeni bir dahili video projesi başlatır. Yayın/dışa aktarım yok.")
    public ResponseEntity<ApiResponse<TurkuVideoBootstrapResponse>> bootstrap(
            @RequestBody TurkuVideoBootstrapRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        // ── Actor resolution via FP-018 pattern ──
        ResolvedAdminActor actor = resolveBootstrapActor(actorRole, actorReference);

        // ── Bootstrap project ──
        TurkuVideoProject project = bootstrapService.bootstrap(request, actor.getActorReference());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TurkuVideoBootstrapResponse.from(project),
                        "Türkü video projesi başarıyla oluşturuldu"));
    }

    // ── GET /admin/turku-video-projects/{id} ──

    @GetMapping("/{id}")
    @Operation(summary = "Türkü video projesi detayı",
               description = "Güvenli proje bilgilerini döndürür. Yayın/dışa aktarım verisi yok.")
    public ResponseEntity<ApiResponse<TurkuVideoProjectResponse>> getProject(
            @PathVariable UUID id) {

        TurkuVideoProject project = bootstrapService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(TurkuVideoProjectResponse.from(project)));
    }

    // ── GET /admin/turku-video-projects/{id}/content ──

    @GetMapping("/{id}/content")
    @Operation(summary = "Türkü video projesi üretilen içerik",
               description = "Hikâye, senaryo ve storyboard içeriğini döndürür.")
    public ResponseEntity<ApiResponse<TurkuVideoContentResponse>> getProjectContent(
            @PathVariable UUID id) {

        TurkuVideoProject project = bootstrapService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(TurkuVideoContentResponse.from(project)));
    }

    // ── GET /admin/turku-video-projects/{id}/progress ──

    @GetMapping("/{id}/progress")
    @Operation(summary = "Türkü video projesi ilerleme durumu",
               description = "Güvenli aşama ve ilerleme bilgisi döndürür.")
    public ResponseEntity<ApiResponse<TurkuVideoProgressResponse>> getProgress(
            @PathVariable UUID id) {

        TurkuVideoProject project = bootstrapService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(TurkuVideoProgressResponse.from(project)));
    }

    // ── GET /admin/turku-video-projects/{id}/generation-safety ──

    @GetMapping("/{id}/generation-safety")
    @Operation(summary = "Üretim güvenlik durumu",
               description = "Provider modu, üretim uygunluğu ve güvenlik bayraklarını döndürür.")
    public ResponseEntity<ApiResponse<TurkuGenerationSafetyResponse>> getGenerationSafety(
            @PathVariable UUID id) {

        TurkuVideoProject project = bootstrapService.getById(id);
        TurkuProviderMode mode = project.getProviderMode();
        TurkuVideoWorkflowStage stage = project.getWorkflowStage();

        boolean blocked = mode == TurkuProviderMode.PROVIDER_DISABLED
                || mode == TurkuProviderMode.PROVIDER_FAILED_SAFE
                || mode == TurkuProviderMode.HUMAN_REVIEW_REQUIRED;

        String blockedReason = blocked ? switch (mode) {
            case PROVIDER_DISABLED -> "Sağlayıcı devre dışı";
            case PROVIDER_FAILED_SAFE -> "Sağlayıcı güvenli hata modunda";
            case HUMAN_REVIEW_REQUIRED -> "İnsan incelemesi bekleniyor";
            default -> null;
        } : null;

        TurkuGenerationSafetyResponse resp = TurkuGenerationSafetyResponse.builder()
                .projectId(project.getId())
                .providerMode(mode)
                .currentStage(stage)
                .generationAvailable(!blocked)
                .blockedReasonSummary(blockedReason)
                .humanReviewRequired(true)
                .autoApproved(false)
                .copyrightReviewRequired(true)
                .lyricsIncluded(false)
                .canGenerateStory(!blocked && stage == TurkuVideoWorkflowStage.DRAFT_CREATED)
                .canGenerateScenario(!blocked && stage == TurkuVideoWorkflowStage.STORY_CONTEXT_PREPARED)
                .canGenerateStoryboard(!blocked && stage == TurkuVideoWorkflowStage.SCENARIO_PREPARED)
                .canGeneratePlanningBundle(!blocked && stage == TurkuVideoWorkflowStage.DRAFT_CREATED)
                .canRunImageBatch(!blocked && stage == TurkuVideoWorkflowStage.IMAGE_BATCH_PREPARED)
                .safeFailureCode(blocked ? mode.name() : null)
                .reviewFlags(java.util.List.of("HUMAN_REVIEW_REQUIRED",
                        "COPYRIGHT_REVIEW_REQUIRED", "LYRICS_NOT_INCLUDED"))
                .safeWarnings(blocked
                        ? java.util.List.of("Üretim engellendi: " + mode)
                        : java.util.List.of())
                .build();

        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    // ── POST /admin/turku-video-projects/{id}/prepare-story ──

    @PostMapping("/{id}/prepare-story")
    @Operation(summary = "Hikâye paketi hazırla",
               description = "Türkü projesi için hikâye/kültürel bağlam paketi hazırlar.")
    public ResponseEntity<ApiResponse<TurkuPreparationResponse>> prepareStory(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuStoryPreparationRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuStoryPackage story = planningService.prepareStory(id, request);
        TurkuVideoProject project = bootstrapService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(
                new TurkuPreparationResponse(id, project.getWorkflowStage(),
                        "Hikâye paketi başarıyla hazırlandı",
                        story.getSourceConfidence(), story.isNeedsHumanReview(),
                        story.isCopyrightReviewRequired(), story.getReviewFlags()),
                "Hikâye paketi hazır"));
    }

    // ── POST /admin/turku-video-projects/{id}/prepare-scenario ──

    @PostMapping("/{id}/prepare-scenario")
    @Operation(summary = "Senaryo planı hazırla",
               description = "Hikâye paketine dayalı senaryo planı hazırlar.")
    public ResponseEntity<ApiResponse<TurkuPreparationResponse>> prepareScenario(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuScenarioPreparationRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuScenarioPlan scenario = planningService.prepareScenario(id, request);
        TurkuVideoProject project = bootstrapService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(
                new TurkuPreparationResponse(id, project.getWorkflowStage(),
                        "Senaryo planı başarıyla hazırlandı",
                        project.getSourceConfidence(), project.isNeedsHumanReview(),
                        project.isCopyrightReviewRequired(), scenario.getReviewFlags()),
                "Senaryo planı hazır"));
    }

    // ── POST /admin/turku-video-projects/{id}/prepare-storyboard ──

    @PostMapping("/{id}/prepare-storyboard")
    @Operation(summary = "Storyboard planı hazırla",
               description = "Senaryoya dayalı sahne bazlı storyboard planı hazırlar.")
    public ResponseEntity<ApiResponse<TurkuPreparationResponse>> prepareStoryboard(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuStoryboardPreparationRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuStoryboardPlan storyboard = planningService.prepareStoryboard(id, request);
        TurkuVideoProject project = bootstrapService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(
                new TurkuPreparationResponse(id, project.getWorkflowStage(),
                        "Storyboard planı başarıyla hazırlandı (" + storyboard.getScenes().size() + " sahne)",
                        project.getSourceConfidence(), project.isNeedsHumanReview(),
                        project.isCopyrightReviewRequired(), storyboard.getReviewFlags()),
                "Storyboard hazır"));
    }

    // ── POST /admin/turku-video-projects/{id}/prepare-scene-assets ──

    @PostMapping("/{id}/prepare-scene-assets")
    @Operation(summary = "Sahne varlık planı hazırla",
               description = "Storyboard sahnelerine dayalı görsel varlık planı hazırlar.")
    public ResponseEntity<ApiResponse<TurkuAssetPreparationResponse>> prepareSceneAssets(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuSceneAssetPreparationRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuSceneAssetPlan plan = assetPlanningService.prepareSceneAssets(id, request);
        TurkuVideoProject project = bootstrapService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(
                new TurkuAssetPreparationResponse(id, project.getWorkflowStage(),
                        "Sahne varlık planı hazırlandı (" + plan.getItems().size() + " varlık)",
                        plan.getItems().size(), project.isNeedsHumanReview(),
                        project.isCopyrightReviewRequired(), plan.getReviewFlags()),
                "Varlık planı hazır"));
    }

    // ── POST /admin/turku-video-projects/{id}/prepare-image-batch ──

    @PostMapping("/{id}/prepare-image-batch")
    @Operation(summary = "Görsel üretim batch'i hazırla",
               description = "Sahne varlıklarına dayalı görsel üretim batch'i hazırlar.")
    public ResponseEntity<ApiResponse<TurkuImageBatchResponse>> prepareImageBatch(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuImageBatchPreparationRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuSceneImageBatch batch = assetPlanningService.prepareImageBatch(id, request);
        TurkuVideoProject project = bootstrapService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(
                new TurkuImageBatchResponse(id, batch.getBatchId(),
                        batch.getStatus(), project.getWorkflowStage(),
                        "Görsel üretim batch'i hazırlandı",
                        batch.getImageRequests().size(), batch.getImageResults().size(),
                        project.isNeedsHumanReview(), project.isCopyrightReviewRequired(),
                        batch.getReviewFlags()),
                "Batch hazır"));
    }

    // ── POST /admin/turku-video-projects/{id}/complete-image-batch ──

    @PostMapping("/{id}/complete-image-batch")
    @Operation(summary = "Görsel batch'ini tamamla/kısmi işaretle",
               description = "Görsel üretim batch'ini tamamlandı veya kısmi olarak işaretler.")
    public ResponseEntity<ApiResponse<TurkuImageBatchResponse>> completeImageBatch(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuImageBatchCompletionRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuSceneImageBatch batch = assetPlanningService.completeImageBatch(id, request);
        TurkuVideoProject project = bootstrapService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(
                new TurkuImageBatchResponse(id, batch.getBatchId(),
                        batch.getStatus(), project.getWorkflowStage(),
                        "Batch durumu güncellendi: " + batch.getStatus(),
                        batch.getImageRequests().size(), batch.getImageResults().size(),
                        project.isNeedsHumanReview(), project.isCopyrightReviewRequired(),
                        batch.getReviewFlags()),
                "Batch güncellendi"));
    }

    // ── FP-020 Task 03: Provider-Draft Generation Endpoints ──

    // ── POST /admin/turku-video-projects/{id}/generate-story ──

    @PostMapping("/{id}/generate-story")
    @Operation(summary = "Sağlayıcı ile hikâye üret",
               description = "Provider-draft yolu ile hikâye paketi üretir. Güvenlik zarfı ile sarılır.")
    public ResponseEntity<ApiResponse<TurkuProviderPlanningGenerationResponse>> generateStory(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuProviderGenerationRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuProviderPlanningGenerationResponse response = providerDraftService.generateStory(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hikâye üretimi tamamlandı"));
    }

    // ── POST /admin/turku-video-projects/{id}/generate-scenario ──

    @PostMapping("/{id}/generate-scenario")
    @Operation(summary = "Sağlayıcı ile senaryo üret",
               description = "Provider-draft yolu ile senaryo planı üretir. Güvenlik zarfı ile sarılır.")
    public ResponseEntity<ApiResponse<TurkuProviderPlanningGenerationResponse>> generateScenario(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuProviderGenerationRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuProviderPlanningGenerationResponse response = providerDraftService.generateScenario(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Senaryo üretimi tamamlandı"));
    }

    // ── POST /admin/turku-video-projects/{id}/generate-storyboard ──

    @PostMapping("/{id}/generate-storyboard")
    @Operation(summary = "Sağlayıcı ile storyboard üret",
               description = "Provider-draft yolu ile storyboard planı üretir. Güvenlik zarfı ile sarılır.")
    public ResponseEntity<ApiResponse<TurkuProviderPlanningGenerationResponse>> generateStoryboard(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuProviderGenerationRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuProviderPlanningGenerationResponse response = providerDraftService.generateStoryboard(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Storyboard üretimi tamamlandı"));
    }

    // ── POST /admin/turku-video-projects/{id}/generate-planning-bundle ──

    @PostMapping("/{id}/generate-planning-bundle")
    @Operation(summary = "Sağlayıcı ile planlama paketi üret",
               description = "Provider-draft yolu ile hikâye+senaryo+storyboard paketini sıralı üretir.")
    public ResponseEntity<ApiResponse<TurkuPlanningBundleGenerationResponse>> generatePlanningBundle(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuProviderGenerationRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuPlanningBundleGenerationResponse response = providerDraftService.generatePlanningBundle(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Planlama paketi üretimi tamamlandı"));
    }

    // ── POST /admin/turku-video-projects/{id}/run-image-batch ──

    @PostMapping("/{id}/run-image-batch")
    @Operation(summary = "Sağlayıcı ile görsel batch çalıştır",
               description = "Provider-draft yolu ile sahne görsellerini batch olarak üretir. Güvenlik zarfı ile sarılır.")
    public ResponseEntity<ApiResponse<TurkuImageBatchRunResponse>> runImageBatch(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuImageBatchRunRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuImageBatchRunResponse response = providerImageBatchService.runImageBatch(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Görsel batch çalıştırıldı"));
    }

    // ── FP-021 Task 02: Composition Plan Endpoint ──

    // ── POST /admin/turku-video-projects/{id}/prepare-composition-plan ──

    @PostMapping("/{id}/prepare-composition-plan")
    @Operation(summary = "Video kompozisyon planı hazırla",
               description = "Storyboard + sahne görselleri + anlatım planından dahili video kompozisyon planı oluşturur.")
    public ResponseEntity<ApiResponse<TurkuCompositionPlanResponse>> prepareCompositionPlan(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuCompositionPlanRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuVideoCompositionPlan plan = compositionService.prepareCompositionPlan(id, request);
        TurkuVideoProject project = bootstrapService.getById(id);

        TurkuCompositionPlanResponse response = new TurkuCompositionPlanResponse(
                project.getId(), plan.getCompositionId(),
                project.getWorkflowStage(),
                plan.getScenes().size(), plan.getTotalDurationSeconds(),
                plan.getTimingPlan().isHasTimingConflict(),
                plan.isHumanReviewRequired(), plan.isCopyrightReviewRequired(),
                plan.getReviewFlags(),
                "Kompozisyon planı hazırlandı: " + plan.getScenes().size() + " sahne");

        return ResponseEntity.ok(ApiResponse.success(response, "Kompozisyon planı hazırlandı"));
    }

    // ── FP-021 Task 03: Render Manifest + Readiness Endpoints ──

    // ── POST /admin/turku-video-projects/{id}/prepare-render-manifest ──

    @PostMapping("/{id}/prepare-render-manifest")
    @Operation(summary = "Render manifest hazırla",
               description = "Kompozisyon planından dahili render manifest oluşturur. Gerçek video üretimi yok.")
    public ResponseEntity<ApiResponse<TurkuRenderManifestResponse>> prepareRenderManifest(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuRenderManifestRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {

        resolvePlanningActor(actorRole, actorReference);
        TurkuRenderManifest manifest = renderManifestService.prepareRenderManifest(id, request);
        TurkuVideoProject project = bootstrapService.getById(id);

        TurkuRenderManifestResponse response = new TurkuRenderManifestResponse(
                project.getId(), manifest.getManifestId(),
                project.getWorkflowStage(),
                manifest.getScenes().size(), manifest.getTotalDurationSeconds(),
                manifest.getTargetResolution(), manifest.getTargetFps(),
                manifest.getTargetFormat(),
                manifest.isHumanReviewRequired(), manifest.isCopyrightReviewRequired(),
                manifest.getReviewFlags(),
                "Render manifest hazırlandı: " + manifest.getScenes().size() + " sahne");

        return ResponseEntity.ok(ApiResponse.success(response, "Render manifest hazırlandı"));
    }

    // ── GET /admin/turku-video-projects/{id}/render-readiness ──

    @GetMapping("/{id}/render-readiness")
    @Operation(summary = "Render hazırlık durumu",
               description = "Dahili render hazırlık raporu döndürür. Yayın/dışa aktarım hazırlığı değil.")
    public ResponseEntity<ApiResponse<TurkuRenderReadinessReport>> getRenderReadiness(
            @PathVariable UUID id) {

        TurkuRenderReadinessReport report = renderManifestService.evaluateReadiness(id);
        return ResponseEntity.ok(ApiResponse.success(report, "Render hazırlık raporu"));
    }

    // ── FP-021 Task 04: Composition Preview ──

    // ── GET /admin/turku-video-projects/{id}/composition-preview ──

    @GetMapping("/{id}/composition-preview")
    @Operation(summary = "Kompozisyon önizleme",
               description = "Dahili kompozisyon timeline önizleme meta verisi. Yayın/dışa aktarım hazırlığı değil.")
    public ResponseEntity<ApiResponse<TurkuCompositionPreviewResponse>> getCompositionPreview(
            @PathVariable UUID id) {

        TurkuVideoProject project = bootstrapService.getById(id);
        TurkuVideoCompositionPlan comp = project.getCompositionPlan();
        TurkuRenderManifest manifest = project.getRenderManifest();

        boolean compositionReady = comp != null;
        boolean manifestReady = manifest != null;

        java.util.List<TurkuCompositionPreviewScene> timelineScenes = new java.util.ArrayList<>();
        double totalDuration = 0;

        if (compositionReady) {
            for (var scene : comp.getScenes()) {
                timelineScenes.add(new TurkuCompositionPreviewScene(
                        scene.getSceneNo(), scene.getSceneTitle(),
                        scene.getNarrationText(),
                        scene.getImageRef() != null ? "internal-ref (mevcut)" : "referans yok",
                        scene.getDurationSeconds(), scene.getMood(),
                        scene.getTransitionIn(), scene.getTransitionOut(),
                        scene.getCameraDirection()));
                totalDuration += scene.getDurationSeconds();
            }
        }

        // Readiness summary
        String readinessStatus;
        java.util.List<String> readinessIssues = new java.util.ArrayList<>();
        if (!compositionReady) {
            readinessStatus = "BLOCKED";
            readinessIssues.add("Kompozisyon planı hazırlanmadı");
        } else if (!manifestReady) {
            readinessStatus = "BLOCKED";
            readinessIssues.add("Render manifest hazırlanmadı");
        } else {
            readinessStatus = "NEEDS_REVIEW";
            readinessIssues.add("Telif incelemesi bekliyor");
        }

        java.util.List<String> reviewFlags = java.util.List.of(
                "HUMAN_REVIEW_REQUIRED", "COPYRIGHT_REVIEW_REQUIRED",
                "NOT_PUBLISH_READY", "NOT_EXPORT_READY", "INTERNAL_PREVIEW_ONLY");

        TurkuCompositionPreviewResponse response = new TurkuCompositionPreviewResponse(
                project.getId(), project.getWorkflowStage(),
                compositionReady, manifestReady, timelineScenes,
                totalDuration, timelineScenes.size(), readinessStatus, readinessIssues,
                reviewFlags, "Dahili kompozisyon önizleme meta verisi");

        return ResponseEntity.ok(ApiResponse.success(response, "Kompozisyon önizleme"));
    }

    private ResolvedAdminActor resolveBootstrapActor(ReviewActorRole role, String reference) {
        return resolvePlanningActor(role, reference);
    }

    /**
     * Resolve admin actor for all türkü project planning actions.
     * SYSTEM is blocked. ADMIN/REVIEWER/CREATOR allowed via TEST_FALLBACK.
     */
    private ResolvedAdminActor resolvePlanningActor(ReviewActorRole role, String reference) {
        boolean systemAllowed = false;

        if (role == null) {
            role = ReviewActorRole.ADMIN;
        }

        AdminActorResolutionRequest resolutionRequest = AdminActorResolutionRequest.builder()
                .requestedActorRole(role)
                .requestedActorReference(reference)
                .fallbackAllowed(true)
                .systemInternalAllowed(systemAllowed)
                .build();

        ResolvedAdminActor actor = actorResolver.resolve(resolutionRequest);

        AdminActorBoundaryPolicy.enforce(
                AdminActorBoundaryPolicy.ActionCategory.TURKU_PROJECT_BOOTSTRAP, actor);

        return actor;
    }

    // ── POST /admin/turku-video-projects/{id}/prepare-render-job ──

    @PostMapping("/{id}/prepare-render-job")
    @Operation(summary = "Dahili render job hazırla",
               description = "Render manifest'ten dahili render job oluşturur. Yayın/dışa aktarım yok.")
    public ResponseEntity<ApiResponse<TurkuRenderJobResponse>> prepareRenderJob(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuRenderJobRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {
        resolvePlanningActor(actorRole, actorReference);
        TurkuRenderJobResponse response = renderJobService.prepareRenderJob(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── GET /admin/turku-video-projects/{id}/render-job ──

    @GetMapping("/{id}/render-job")
    @Operation(summary = "Render job durumu",
               description = "Dahili render job durumunu döndürür. Yayın/dışa aktarım yok.")
    public ResponseEntity<ApiResponse<TurkuRenderJobResponse>> getRenderJob(
            @PathVariable UUID id) {
        TurkuRenderJobResponse response = renderJobService.getRenderJob(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── POST /admin/turku-video-projects/{id}/run-render-job ──

    @PostMapping("/{id}/run-render-job")
    @Operation(summary = "Dahili render job çalıştır",
               description = "Render job'u stub runner ile çalıştırır. Gerçek render yok. Yayın/dışa aktarım yok.")
    public ResponseEntity<ApiResponse<TurkuRenderExecutionResponse>> runRenderJob(
            @PathVariable UUID id,
            @RequestBody(required = false) TurkuRenderExecutionRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {
        resolvePlanningActor(actorRole, actorReference);
        TurkuRenderExecutionResponse response = renderExecutionService.runRenderJob(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── GET /admin/turku-video-projects/{id}/render-execution-safety ──

    @GetMapping("/{id}/render-execution-safety")
    @Operation(summary = "Render çalıştırma güvenlik durumu",
               description = "Render çalıştırma güvenlik zarfını döndürür. Yayın/dışa aktarım yok.")
    public ResponseEntity<ApiResponse<TurkuRenderExecutionSafetyResponse>> getRenderExecutionSafety(
            @PathVariable UUID id) {
        TurkuRenderExecutionSafetyResponse response = renderExecutionService.getRenderExecutionSafety(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── POST /admin/turku-video-projects/{id}/trigger-internal-dry-run ──

    @PostMapping("/{id}/trigger-internal-dry-run")
    @Operation(summary = "Dahili hikaye-video dry-run tetikle",
               description = "FP-047 storyboard render zincirini fake/test-double ile çalıştırır. Gerçek render yok. Yayın/dışa aktarım yok.")
    public ResponseEntity<ApiResponse<TurkuInternalDryRunTriggerResponse>> triggerInternalDryRun(
            @PathVariable UUID id,
            @RequestBody TurkuInternalDryRunTriggerRequest request,
            @RequestParam(required = false) ReviewActorRole actorRole,
            @RequestParam(required = false) String actorReference) {
        resolvePlanningActor(actorRole, actorReference);
        var fixture = new TurkuStoryboardInternalRenderChainFixture(
                request.getOpaqueStoryboardRef(),
                request.getOpaqueStoryRef(),
                request.getSceneInputs());
        var summary = storyboardChainHarness.execute(fixture);
        var response = new TurkuInternalDryRunTriggerResponse(summary);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── GET /admin/turku-video-projects/{id}/internal-dry-run-status ──

    @GetMapping("/{id}/internal-dry-run-status")
    @Operation(summary = "Dahili dry-run durumu",
               description = "Dahili dry-run meta veri durumunu döndürür. Yayın/dışa aktarım yok.")
    public ResponseEntity<ApiResponse<TurkuInternalDryRunTriggerResponse>> getInternalDryRunStatus(
            @PathVariable UUID id) {
        // Fake fixture — returns metadata-only summary
        var fixture = TurkuStoryboardInternalRenderChainFixture.turkuFixture(3);
        var summary = storyboardChainHarness.execute(fixture);
        var response = new TurkuInternalDryRunTriggerResponse(summary);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── TTS Generation ──

    @PostMapping("/{id}/generate-tts")
    @Operation(summary = "TTS seslendirme üret",
               description = "Tüm storyboard sahneleri için ses dosyası üretir.")
    public ResponseEntity<ApiResponse<TurkuTtsService.TurkuTtsResponse>> generateTts(
            @PathVariable UUID id) {
        if (ttsService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("TTS servisi yapılandırılmamış (ai.provider=openai gerekli)"));
        }
        TurkuTtsService.TurkuTtsResponse result = ttsService.generateForProject(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── DALL-E Batch Image Generation ──

    @PostMapping("/{id}/generate-images")
    @Operation(summary = "Tüm sahneler için DALL-E görsel üret",
               description = "Storyboard'daki her sahne için DALL-E 3 ile görsel üretir ve MinIO'ya kaydeder.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateAllImages(@PathVariable UUID id) {
        TurkuVideoProject project = bootstrapService.getById(id);
        TurkuStoryboardPlan storyboard = project.getStoryboardPlan();
        if (storyboard == null || storyboard.getScenes() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Storyboard hazır değil"));
        }

        int success = 0, fail = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (TurkuStoryboardScene scene : storyboard.getScenes()) {
            try {
                TurkuImageGenerationRequest imgReq = TurkuImageGenerationRequest.builder()
                        .projectId(id)
                        .sceneNo(scene.getSceneNo())
                        .safePromptSummary(scene.getVisualDescription())
                        .styleHint("cinematic watercolor, Anatolian documentary")
                        .safeNegativeNotes("text, watermark, logo, modern elements")
                        .aspectRatioHint("16:9")
                        .build();

                if (providerDraftService.getGateway() instanceof OpenAiTurkuGenerationProviderGateway openAiGateway) {
                    openAiGateway.generateImage(imgReq,
                            TurkuGenerationSafetyEnvelope.createDefault(id, "IMAGE_GENERATION", TurkuProviderMode.DETERMINISTIC_STUB));
                    success++;
                } else {
                    errors.add("Sahne " + scene.getSceneNo() + ": OpenAI gateway aktif değil");
                    fail++;
                }
            } catch (Exception e) {
                fail++;
                errors.add("Sahne " + scene.getSceneNo() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = Map.of(
                "successCount", success,
                "failureCount", fail,
                "totalScenes", storyboard.getScenes().size(),
                "errors", errors
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Full Pipeline: Content → Images → TTS → Video ──

    @PostMapping("/{id}/generate-full-video")
    @Operation(summary = "Video pipeline (preview veya tam)",
               description = "previewMode=true (varsayılan): sadece ilk 5 sahne işlenir (~$3). " +
                       "previewMode=false: tüm sahneler (~$15). Kling paralel çalışır.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateFullVideo(
            @PathVariable UUID id,
            @RequestParam(value = "previewMode", defaultValue = "true") boolean previewMode,
            @RequestParam(value = "includeKling", defaultValue = "true") boolean includeKling) {
        Map<String, Object> results = new java.util.LinkedHashMap<>();

        try {
            TurkuVideoProject project = bootstrapService.getById(id);

            // ADIM 1: İçerik üretimi (hikâye + senaryo + storyboard)
            if ("DRAFT_CREATED".equals(project.getWorkflowStage().name())) {
                results.put("step1_content", "Üretiliyor...");
                providerDraftService.generatePlanningBundle(id, new TurkuProviderGenerationRequest());
                project = bootstrapService.getById(id); // Refresh
                results.put("step1_content", "✅ Tamamlandı");
            } else {
                results.put("step1_content", "⏭️ Zaten mevcut (atlandı)");
            }

            // ADIM 2: DALL-E görseller (MinIO'da zaten varsa atla)
            TurkuStoryboardPlan storyboard = project.getStoryboardPlan();
            String imageProvider = falImageService != null ? "Fal.ai Flux Pro" : "OpenAI gpt-image-1";

            // Hikâye bağlamını hazırla — tüm görsellerde tutarlılık için
            String storyContext = "";
            String characterDesc = "";
            if (project.getStoryPackage() != null) {
                TurkuStoryPackage sp = project.getStoryPackage();
                // Hikâye özetinden bağlam çıkar
                if (sp.getStorySummary() != null) {
                    storyContext = sp.getStorySummary().length() > 500
                            ? sp.getStorySummary().substring(0, 500)
                            : sp.getStorySummary();
                }
                // Duygusal tema ekle
                if (sp.getEmotionalTheme() != null) {
                    storyContext += ". Emotional theme: " + sp.getEmotionalTheme();
                }
                if (sp.getRegionContext() != null) {
                    storyContext += ". Setting: " + sp.getRegionContext();
                }
            }

            // Karakter tanımı — hikâyeden detaylı fiziksel tanım çıkar
            // Bu tanım TÜM sahnelerde aynı kalacak → karakter tutarlılığı
            characterDesc = buildCharacterDescription(project);

            int imgSuccess = 0, imgFail = 0, imgSkipped = 0;
            // Preview modu: sadece ilk 5 sahne işlenir (Kling/TTS/FFmpeg dahil)
            int previewLimit = previewMode ? 5 : (storyboard != null && storyboard.getScenes() != null ? storyboard.getScenes().size() : 0);
            java.util.List<TurkuStoryboardScene> scenesToProcess = (storyboard != null && storyboard.getScenes() != null)
                    ? storyboard.getScenes().stream().filter(s -> s.getSceneNo() <= previewLimit).toList()
                    : java.util.List.of();

            if (storyboard != null && storyboard.getScenes() != null) {
                int totalScenes = storyboard.getScenes().size();
                String previousVisual = null;

                if (previewMode) {
                    results.put("mode", "🎬 PREVIEW (ilk " + previewLimit + " sahne / " + totalScenes + " toplam)");
                    log.info("PREVIEW MODU: ilk {} sahne işlenecek (toplam {})", previewLimit, totalScenes);
                } else {
                    results.put("mode", "🎬 TAM VİDEO (" + totalScenes + " sahne)");
                    log.info("TAM VİDEO MODU: {} sahne işlenecek", totalScenes);
                }

                for (TurkuStoryboardScene scene : scenesToProcess) {
                    String imagePath = String.format("turku-video/%s/scenes/%d.png", id, scene.getSceneNo());
                    // MinIO'da zaten varsa atla
                    boolean exists = false;
                    try {
                        minioClient.statObject(io.minio.StatObjectArgs.builder()
                                .bucket(minioConfig.getBucketName()).object(imagePath).build());
                        exists = true;
                    } catch (Exception ignored) {}

                    if (exists) {
                        imgSkipped++;
                        previousVisual = scene.getVisualDescription();
                        continue;
                    }

                    try {
                        TurkuImageGenerationRequest imgReq = TurkuImageGenerationRequest.builder()
                                .projectId(id)
                                .sceneNo(scene.getSceneNo())
                                .totalScenes(totalScenes)
                                .safePromptSummary(scene.getVisualDescription())
                                .styleHint("photorealistic cinematic film still, Anatolian historical drama")
                                .safeNegativeNotes("text, watermark, logo, modern elements, cartoon, anime, illustration")
                                .aspectRatioHint("16:9")
                                .storyContext(storyContext)
                                .characterDescription(characterDesc)
                                .previousSceneVisual(previousVisual)
                                .sceneMood(scene.getMood())
                                .narrationText(scene.getNarrationSummary())
                                .sceneTitle(scene.getSceneTitle())
                                .build();

                        // Fal.ai varsa onu kullan (Flux Pro), yoksa OpenAI (gpt-image-1)
                        if (falImageService != null) {
                            TurkuImageGenerationResult result = falImageService.generateImage(imgReq,
                                    TurkuGenerationSafetyEnvelope.createDefault(id, "IMAGE_GENERATION", TurkuProviderMode.DETERMINISTIC_STUB));
                            if (result.isSuccess()) {
                                imgSuccess++;
                                logAiCost(id, "image_generation", scene.getSceneNo(), "fal.ai", "flux-pro-v1.1",
                                        new java.math.BigDecimal("0.04"), java.math.BigDecimal.ONE, "megapixels",
                                        com.anadoluatlasi.backend.ai.entity.AiGenerationStatus.COMPLETED,
                                        "Sahne " + scene.getSceneNo() + " görseli");
                            } else {
                                imgFail++;
                                logAiCost(id, "image_generation", scene.getSceneNo(), "fal.ai", "flux-pro-v1.1",
                                        new java.math.BigDecimal("0.04"), java.math.BigDecimal.ONE, "megapixels",
                                        com.anadoluatlasi.backend.ai.entity.AiGenerationStatus.FAILED,
                                        "Hata: " + result.getSafeErrorCode());
                                log.error("Görsel üretim hatası: sahne={}, errorCode={}", scene.getSceneNo(), result.getSafeErrorCode());
                            }
                        } else if (providerDraftService.getGateway() instanceof OpenAiTurkuGenerationProviderGateway openAiGateway) {
                            openAiGateway.generateImage(imgReq,
                            TurkuGenerationSafetyEnvelope.createDefault(id, "IMAGE_GENERATION", TurkuProviderMode.DETERMINISTIC_STUB));
                            imgSuccess++;
                            logAiCost(id, "image_generation", scene.getSceneNo(), "openai", "gpt-image-1",
                                    new java.math.BigDecimal("0.04"), java.math.BigDecimal.ONE, "megapixels",
                                    com.anadoluatlasi.backend.ai.entity.AiGenerationStatus.COMPLETED,
                                    "Sahne " + scene.getSceneNo() + " görseli (OpenAI)");
                        }

                        // Önceki sahne görselini güncelle (sonraki sahne için)
                        previousVisual = scene.getVisualDescription();
                    } catch (Exception e) {
                        imgFail++;
                        previousVisual = scene.getVisualDescription();
                        log.error("Görsel üretim exception: sahne={}, error={}", scene.getSceneNo(), e.getMessage(), e);
                    }
                }
                results.put("step2_images", imageProvider + ": ✅ " + imgSuccess + " yeni, ⏭️ " + imgSkipped + " mevcut, ❌ " + imgFail + " hata");
            }

            // ═══ BAĞIMLILIK GRAFİĞİ ═══
            // Görseller → Kling Video (bağımlı) → FFmpeg (bağımlı)
            // Storyboard → TTS (BAĞIMSIZ — görselden etkilenmez)
            //
            // imgSuccess + imgSkipped = kullanılabilir görsel sayısı
            // Eğer 0 ise: Kling ve FFmpeg anlamsız, ama TTS çalışmalı
            int availableImages = imgSuccess + imgSkipped;
            boolean imagesAvailable = availableImages > 0;

            // ═══ MALİYET TAHMİNİ ═══
            // Sadece YENİ üretilen öğeleri say (skip edilenler maliyet oluşturmaz)
            double imgCost = imgSuccess * 0.05;  // Flux Pro ~$0.05/resim
            log.info("💰 Görsel maliyeti: {} yeni × $0.05 = ${}, {} atlandı (ücretsiz)",
                    imgSuccess, String.format("%.2f", imgCost), imgSkipped);

            // ADIM 2.5: Image-to-Video (Kling AI) — PARALEL, SADECE HERO SAHNELER
            // Kling maliyeti: $1.40/klip × 35 = $49/proje → çok pahalı!
            // Çözüm: Sadece "hero" sahneleri canlandır (~6-8 sahne = $8-11)
            // Geri kalan sahneler FFmpeg Ken Burns (zoom/pan) ile — ücretsiz
            if (!includeKling) {
                results.put("step2_5_video", "⏭️ Kling AI atlandı. ?includeKling=true ile açılabilir");
            } else if (!imagesAvailable) {
                log.warn("Kling AI atlanıyor: kullanılabilir görsel yok");
                results.put("step2_5_video", "⏭️ Görsel yok — Kling atlandı");
            } else if (falVideoService != null && storyboard != null && storyboard.getScenes() != null) {
                int vidSkipped = 0;
                int vidKenBurns = 0;  // Hero olmayan sahneler — FFmpeg Ken Burns ile
                java.util.List<FalVideoGenerationService.QueuedJob> klingJobs = new java.util.ArrayList<>();

                // ═══ HERO SAHNE SEÇİMİ ═══
                int totalSceneCount = storyboard.getScenes().size();
                java.util.Set<Integer> heroScenes = new java.util.HashSet<>();

                if (previewMode) {
                    // Preview: sadece sahne 1 ve son preview sahnesi (max 2 Kling = ~$2.80)
                    heroScenes.add(1);
                    heroScenes.add(Math.min(5, totalSceneCount));
                    log.info("Kling PREVIEW: sadece sahne {} (max 2 animasyon, ~$2.80)", heroScenes);
                } else {
                    // Tam video: ilk, son, her 5., duygusal doruklar
                    heroScenes.add(1);
                    heroScenes.add(totalSceneCount);
                    for (int s = 5; s < totalSceneCount; s += 5) heroScenes.add(s);
                    for (TurkuStoryboardScene sc : storyboard.getScenes()) {
                        String m = sc.getMood() != null ? sc.getMood().toLowerCase() : "";
                        if (m.contains("climax") || m.contains("emotional") || m.contains("dramatic")
                                || m.contains("intense") || m.contains("peak") || m.contains("doruk")
                                || m.contains("hüzün") || m.contains("aşk") || m.contains("kavuşma")) {
                            heroScenes.add(sc.getSceneNo());
                        }
                    }
                    log.info("Kling TAM: {} hero sahne / {} toplam (tahmini maliyet: ~${})",
                            heroScenes.size(), totalSceneCount, String.format("%.2f", heroScenes.size() * 1.40));
                }

                // Preview modunda sadece preview sahnelerini işle
                java.util.List<TurkuStoryboardScene> klingScenesToProcess = previewMode
                        ? scenesToProcess
                        : storyboard.getScenes();

                for (TurkuStoryboardScene scene : klingScenesToProcess) {
                    // Hero sahne değilse → Ken Burns (ücretsiz)
                    if (!heroScenes.contains(scene.getSceneNo())) {
                        vidKenBurns++;
                        continue;
                    }

                    // Video klip zaten varsa atla
                    String videoPath = String.format("turku-video/%s/scenes/%d_video.mp4", id, scene.getSceneNo());
                    try {
                        minioClient.statObject(io.minio.StatObjectArgs.builder()
                                .bucket(minioConfig.getBucketName()).object(videoPath).build());
                        vidSkipped++;
                        continue;
                    } catch (Exception ignored) {}

                    // Görsel var mı?
                    String imagePath = String.format("turku-video/%s/scenes/%d.png", id, scene.getSceneNo());
                    boolean sceneImageExists = false;
                    try {
                        minioClient.statObject(io.minio.StatObjectArgs.builder()
                                .bucket(minioConfig.getBucketName()).object(imagePath).build());
                        sceneImageExists = true;
                    } catch (Exception ignored) {}

                    if (!sceneImageExists) {
                        klingJobs.add(FalVideoGenerationService.QueuedJob.failed(id, scene.getSceneNo(), "Görsel yok"));
                        continue;
                    }

                    try {
                        byte[] imageBytes;
                        try (var stream = minioClient.getObject(io.minio.GetObjectArgs.builder()
                                .bucket(minioConfig.getBucketName()).object(imagePath).build())) {
                            imageBytes = stream.readAllBytes();
                        }
                        String imageUrl = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(imageBytes);

                        String motionPrompt = "Subtle cinematic motion in scene: " +
                                scene.getVisualDescription().substring(0, Math.min(150, scene.getVisualDescription().length()));
                        if (scene.getMood() != null) {
                            motionPrompt += ". Mood: " + scene.getMood();
                        }

                        klingJobs.add(falVideoService.submitToQueue(id, scene.getSceneNo(), imageUrl, motionPrompt));
                    } catch (Exception e) {
                        klingJobs.add(FalVideoGenerationService.QueuedJob.failed(id, scene.getSceneNo(), e.getMessage()));
                    }
                }

                double klingCost = klingJobs.stream().filter(FalVideoGenerationService.QueuedJob::isSubmitted).count() * 1.40;
                log.info("💰 Kling tahmini maliyet: {} hero sahne × $1.40 = ${}, {} Ken Burns (ücretsiz), {} mevcut (atlandı)",
                        klingJobs.stream().filter(FalVideoGenerationService.QueuedJob::isSubmitted).count(),
                        String.format("%.2f", klingCost), vidKenBurns, vidSkipped);

                // FAZA 2: Tüm işleri PARALEL olarak bekle ve indir
                if (!klingJobs.isEmpty()) {
                    java.util.List<FalVideoGenerationService.VideoGenerationResult> vidResults =
                            falVideoService.pollAndDownloadAll(klingJobs);

                    long vidSuccess = vidResults.stream().filter(FalVideoGenerationService.VideoGenerationResult::isSuccess).count();
                    long vidFail = vidResults.stream().filter(r -> !r.isSuccess()).count();

                    results.put("step2_5_video", "Kling (hero): ✅ " + vidSuccess + " animasyon, 🎞️ " + vidKenBurns + " Ken Burns, ⏭️ " + vidSkipped + " mevcut, ❌ " + vidFail + " hata");
                    results.put("step2_5_cost", String.format("~$%.2f (sadece hero sahneler)", vidSuccess * 1.40));
                } else if (vidSkipped > 0) {
                    results.put("step2_5_video", "⏭️ Tüm hero video klipleri mevcut (" + vidSkipped + " atlandı, " + vidKenBurns + " Ken Burns)");
                } else {
                    results.put("step2_5_video", "🎞️ " + vidKenBurns + " sahne Ken Burns ile (Kling hero sahne yok)");
                }
            } else if (falVideoService == null) {
                results.put("step2_5_video", "⏭️ Video hareketlendirme kapalı (ai.video-provider=kling yapın)");
            }

            // ADIM 3: TTS seslendirme (MinIO'da zaten varsa atla)
            boolean hasTtsProvider = (elevenLabsTtsService != null || ttsService != null);
            String ttsLabel = elevenLabsTtsService != null ? "ElevenLabs" : "OpenAI";
            if (hasTtsProvider) {
                if (storyboard != null && storyboard.getScenes() != null) {
                    // Preview modunda sadece preview sahnelerinin sesini kontrol et
                    java.util.List<TurkuStoryboardScene> ttsScenes = previewMode
                            ? scenesToProcess
                            : storyboard.getScenes();

                    boolean allAudioExists = true;
                    for (TurkuStoryboardScene scene : ttsScenes) {
                        String audioPath = String.format("turku-video/%s/audio/%d.mp3", id, scene.getSceneNo());
                        try {
                            minioClient.statObject(io.minio.StatObjectArgs.builder()
                                    .bucket(minioConfig.getBucketName()).object(audioPath).build());
                        } catch (Exception e) {
                            allAudioExists = false;
                            break;
                        }
                    }

                    if (allAudioExists) {
                        results.put("step3_tts", "⏭️ Tüm ses dosyaları mevcut (atlandı)");
                    } else {
                        // ElevenLabs varsa onu kullan, yoksa OpenAI
                        // Preview modunda sadece previewLimit kadar sahne üretilir
                        TurkuTtsService.TurkuTtsResponse ttsResult;
                        if (elevenLabsTtsService != null) {
                            ttsResult = elevenLabsTtsService.generateForProject(id, previewLimit);
                        } else {
                            ttsResult = ttsService.generateForProject(id);
                        }
                        results.put("step3_tts", "✅ " + ttsLabel + ": " + ttsResult.getSuccessCount() + " ses, ❌ " + ttsResult.getFailureCount() + " hata");
                    }
                }
            } else {
                results.put("step3_tts", "⏭️ TTS servisi yok (ai.tts-provider ayarlayın)");
            }

            // ADIM 4: FFmpeg video birleştirme — GÖRSELLERE BAĞIMLI
            if (!imagesAvailable) {
                log.warn("FFmpeg atlanıyor: kullanılabilir görsel yok");
                results.put("step4_video", "⏭️ Görsel yok — video oluşturulamaz. Fal.ai API key/bakiye kontrolü yapın.");
            } else {
                TurkuVideoAssemblyService.VideoAssemblyResult videoResult = videoAssemblyService.assembleVideo(id);
                if (videoResult.isSuccess()) {
                    results.put("step4_video", "✅ " + videoResult.getSegmentCount() + " segment, " +
                            (videoResult.getVideoSizeBytes() / 1024 / 1024) + " MB");
                    results.put("videoUrl", "/api/admin/turku-video-projects/" + id + "/video");
                } else {
                    results.put("step4_video", "❌ " + videoResult.getError());
                }
            }

            // Gerçek başarı durumu
            boolean pipelineSuccess = imagesAvailable && results.containsKey("videoUrl");
            boolean hasErrors = false;

            // Adım sonuçlarını tarayarak hataları tespit et
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                if (entry.getValue() instanceof String val && val.contains("❌")) {
                    hasErrors = true;
                    break;
                }
            }

            results.put("success", pipelineSuccess);
            results.put("hasErrors", hasErrors);
            if (!imagesAvailable) {
                results.put("failReason", "Görsel üretimi başarısız. Fal.ai API key/bakiye kontrolü yapın.");
            }
            return ResponseEntity.ok(ApiResponse.success(results));

        } catch (Exception e) {
            results.put("success", false);
            results.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.success(results));
        }
    }

    // ── Media Serving ──

    @GetMapping("/{id}/scenes/{sceneNo}/image")
    @Operation(summary = "Sahne görseli", description = "MinIO'dan sahne görselini döndürür.")
    public ResponseEntity<byte[]> getSceneImage(
            @PathVariable UUID id, @PathVariable int sceneNo) {
        String path = String.format("turku-video/%s/scenes/%d.png", id, sceneNo);
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName()).object(path).build())) {
            byte[] bytes = stream.readAllBytes();
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/scenes/{sceneNo}/audio")
    @Operation(summary = "Sahne seslendirmesi", description = "MinIO'dan sahne ses dosyasını döndürür.")
    public ResponseEntity<byte[]> getSceneAudio(
            @PathVariable UUID id, @PathVariable int sceneNo) {
        String path = String.format("turku-video/%s/audio/%d.mp3", id, sceneNo);
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName()).object(path).build())) {
            byte[] bytes = stream.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Video Assembly ──

    @PostMapping("/{id}/assemble-video")
    @Operation(summary = "Video birleştir",
               description = "Sahne görselleri ve TTS seslerini FFmpeg ile birleştirerek video üretir.")
    public ResponseEntity<ApiResponse<TurkuVideoAssemblyService.VideoAssemblyResult>> assembleVideo(
            @PathVariable UUID id) {
        TurkuVideoAssemblyService.VideoAssemblyResult result = videoAssemblyService.assembleVideo(id);
        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(result.getError()));
        }
    }

    @GetMapping("/{id}/video")
    @Operation(summary = "Video izle", description = "MinIO'dan üretilen videoyu stream eder.")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> getVideo(@PathVariable UUID id) {
        String path = String.format("turku-video/%s/video/final.mp4", id);
        try {
            // Önce dosya boyutunu öğren
            io.minio.StatObjectResponse stat = minioClient.statObject(
                    io.minio.StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName()).object(path).build());

            // Stream olarak aç (büyük dosyalar için bellek tasarrufu)
            var stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName()).object(path).build());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .contentLength(stat.size())
                    .header("Content-Disposition", "inline; filename=turku-video.mp4")
                    .header("Accept-Ranges", "bytes")
                    .body(new org.springframework.core.io.InputStreamResource(stream));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Music Upload ──

    @PostMapping(value = "/{id}/upload-music", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Müzik yükle",
            description = "Fon müziği veya sözlü türkü kaydı yükler. type: 'fon' veya 'turku'")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadMusic(
            @PathVariable UUID id,
            @RequestParam("type") String type,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        if (!"fon".equals(type) && !"turku".equals(type)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("type 'fon' veya 'turku' olmalı"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Dosya boş"));
        }

        // Dosya uzantısını belirle
        String originalName = file.getOriginalFilename();
        String ext = "mp3";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }

        String minioPath = String.format("turku-video/%s/music/%s.%s", id, type, ext);

        try {
            minioClient.putObject(io.minio.PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(minioPath)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : "audio/mpeg")
                    .build());

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "path", minioPath,
                    "type", type,
                    "size", String.valueOf(file.getSize()),
                    "message", type.equals("fon")
                            ? "Fon müziği yüklendi — video üretiminde arka planda çalacak"
                            : "Türkü kaydı yüklendi — videonun sonunda tam sesle çalacak"
            )));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Müzik yükleme hatası: " + e.getMessage()));
        }
    }

    // ── GET Music Status ──

    @GetMapping("/{id}/music-status")
    @Operation(summary = "Müzik durumu", description = "Yüklenmiş müzik dosyalarının durumunu döner")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMusicStatus(@PathVariable UUID id) {
        Map<String, Object> status = new java.util.HashMap<>();

        // Desteklenen uzantılar
        String[] extensions = {"mp3", "wav", "ogg", "m4a", "flac", "aac"};

        // Fon müziği kontrolü — herhangi bir uzantıyla yüklenmiş mi?
        boolean fonFound = false;
        for (String ext : extensions) {
            if (checkMinioExists(String.format("turku-video/%s/music/fon.%s", id, ext))) {
                fonFound = true;
                break;
            }
        }
        status.put("fonUploaded", fonFound);

        // Türkü kaydı kontrolü
        boolean turkuFound = false;
        for (String ext : extensions) {
            if (checkMinioExists(String.format("turku-video/%s/music/turku.%s", id, ext))) {
                turkuFound = true;
                break;
            }
        }
        status.put("turkuUploaded", turkuFound);

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    // ── MinIO Asset Management ──

    @GetMapping("/{id}/assets")
    @Operation(summary = "Proje varlıklarını listele",
               description = "MinIO'daki tüm proje varlıklarını kategorize ederek döndürür.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listAssets(@PathVariable UUID id) {
        String prefix = String.format("turku-video/%s/", id);
        java.util.List<Map<String, Object>> images = new java.util.ArrayList<>();
        java.util.List<Map<String, Object>> videos = new java.util.ArrayList<>();
        java.util.List<Map<String, Object>> audio = new java.util.ArrayList<>();
        long totalSize = 0;

        try {
            var results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .prefix(prefix)
                    .recursive(true)
                    .build());

            for (Result<io.minio.messages.Item> result : results) {
                io.minio.messages.Item item = result.get();
                String objectName = item.objectName();
                long size = item.size();
                totalSize += size;

                // Sahne numarasını çıkar
                Integer sceneNo = extractSceneNo(objectName);

                Map<String, Object> assetInfo = new java.util.LinkedHashMap<>();
                assetInfo.put("path", objectName);
                assetInfo.put("size", size);
                assetInfo.put("sizeHuman", humanReadableSize(size));
                if (sceneNo != null) assetInfo.put("sceneNo", sceneNo);
                assetInfo.put("lastModified", item.lastModified() != null ? item.lastModified().toString() : null);

                if (objectName.endsWith(".png") || objectName.endsWith(".jpg") || objectName.endsWith(".jpeg")) {
                    images.add(assetInfo);
                } else if (objectName.endsWith(".mp4")) {
                    videos.add(assetInfo);
                } else if (objectName.endsWith(".mp3") || objectName.endsWith(".wav")) {
                    audio.add(assetInfo);
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("MinIO listeleme hatası: " + e.getMessage()));
        }

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("projectId", id);
        response.put("totalAssets", images.size() + videos.size() + audio.size());
        response.put("totalSize", totalSize);
        response.put("totalSizeHuman", humanReadableSize(totalSize));
        response.put("images", Map.of("count", images.size(), "items", images));
        response.put("videos", Map.of("count", videos.size(), "items", videos));
        response.put("audio", Map.of("count", audio.size(), "items", audio));

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}/assets")
    @Operation(summary = "Proje varlıklarını sil",
               description = "Belirtilen tipteki varlıkları siler. type: all/images/videos/audio. sceneNos ile filtrelenebilir.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAssets(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(required = false) List<Integer> sceneNos) {

        String prefix = String.format("turku-video/%s/", id);
        java.util.List<String> deleted = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();

        try {
            var results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .prefix(prefix)
                    .recursive(true)
                    .build());

            for (Result<io.minio.messages.Item> result : results) {
                io.minio.messages.Item item = result.get();
                String objectName = item.objectName();

                // Tip filtreleme
                boolean matches = switch (type.toLowerCase()) {
                    case "all" -> true;
                    case "images" -> objectName.endsWith(".png") || objectName.endsWith(".jpg") || objectName.endsWith(".jpeg");
                    case "videos" -> objectName.endsWith(".mp4");
                    case "audio" -> objectName.endsWith(".mp3") || objectName.endsWith(".wav");
                    default -> false;
                };

                if (!matches) continue;

                // Sahne numarası filtreleme
                if (sceneNos != null && !sceneNos.isEmpty()) {
                    Integer sceneNo = extractSceneNo(objectName);
                    if (sceneNo == null || !sceneNos.contains(sceneNo)) continue;
                }

                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build());
                    deleted.add(objectName);
                } catch (Exception e) {
                    errors.add(objectName + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("MinIO silme hatası: " + e.getMessage()));
        }

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("projectId", id);
        response.put("deletedCount", deleted.size());
        response.put("deletedFiles", deleted);
        if (!errors.isEmpty()) response.put("errors", errors);

        return ResponseEntity.ok(ApiResponse.success(response,
                deleted.size() + " varlık silindi"));
    }

    @GetMapping("/{id}/assets/preview/{type}/{sceneNo}")
    @Operation(summary = "Varlık önizleme/stream",
               description = "MinIO'dan varlığı doğru Content-Type ile stream eder. type: image/video/audio")
    public ResponseEntity<byte[]> previewAsset(
            @PathVariable UUID id,
            @PathVariable String type,
            @PathVariable int sceneNo) {

        String path;
        MediaType contentType;

        switch (type.toLowerCase()) {
            case "image" -> {
                path = String.format("turku-video/%s/scenes/%d.png", id, sceneNo);
                contentType = MediaType.IMAGE_PNG;
            }
            case "video" -> {
                path = String.format("turku-video/%s/scenes/%d_video.mp4", id, sceneNo);
                contentType = MediaType.parseMediaType("video/mp4");
            }
            case "audio" -> {
                path = String.format("turku-video/%s/audio/%d.mp3", id, sceneNo);
                contentType = MediaType.parseMediaType("audio/mpeg");
            }
            default -> {
                return ResponseEntity.badRequest().build();
            }
        }

        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName()).object(path).build())) {
            byte[] bytes = stream.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .contentLength(bytes.length)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Integer extractSceneNo(String objectName) {
        // turku-video/{id}/scenes/3.png → 3
        // turku-video/{id}/scenes/3_video.mp4 → 3
        // turku-video/{id}/audio/3.mp3 → 3
        try {
            String fileName = objectName.substring(objectName.lastIndexOf('/') + 1);
            String baseName = fileName.split("\\.")[0]; // "3" or "3_video"
            String numPart = baseName.split("_")[0]; // "3"
            return Integer.parseInt(numPart);
        } catch (Exception e) {
            return null;
        }
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private boolean checkMinioExists(String path) {
        try {
            minioClient.statObject(io.minio.StatObjectArgs.builder()
                    .bucket(minioConfig.getBucketName()).object(path).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Exception Handling ──

    @ExceptionHandler(TurkuVideoProjectException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectException(TurkuVideoProjectException ex) {
        HttpStatus status = switch (ex.getSafeErrorCode()) {
            case "PROJECT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "TURKU_NAME_REQUIRED", "TURKU_NAME_TOO_LONG",
                 "UNSAFE_INPUT", "FIELD_TOO_LONG", "INVALID_DURATION",
                 "INVALID_STAGE_TRANSITION", "MISSING_PREREQUISITE",
                 "PROMPT_INJECTION_DETECTED" -> HttpStatus.BAD_REQUEST;
            case "SANITIZATION_CRITICAL_VIOLATION", "SANITIZATION_SECRET_VIOLATION",
                 "POLICY_AUTO_APPROVED_VIOLATION", "POLICY_HUMAN_REVIEW_VIOLATION",
                 "POLICY_LYRICS_VIOLATION", "POLICY_COPYRIGHT_VIOLATION" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AdminActorResolutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleActorException(AdminActorResolutionException ex) {
        HttpStatus status = switch (ex.getSafeErrorCode()) {
            case "SYSTEM_NOT_ALLOWED", "BOUNDARY_POLICY_VIOLATION" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage()));
    }

    // ── Karakter Tanımı Builder ──────────────────────────────────────────

    // ── Cost Tracking ──

    @GetMapping("/{id}/costs")
    @Operation(summary = "Proje maliyet analizi", description = "Bu proje için tüm AI çağrı maliyetlerini döner")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProjectCosts(@PathVariable UUID id) {
        bootstrapService.getById(id);

        Map<String, Object> costData = new java.util.LinkedHashMap<>();
        java.math.BigDecimal totalCost = aiLogRepository.sumCostByTurkuProjectId(id);
        costData.put("totalCost", totalCost);

        java.util.List<Object[]> breakdown = aiLogRepository.costBreakdownByTurkuProjectId(id);
        java.util.List<Map<String, Object>> breakdownList = new java.util.ArrayList<>();
        for (Object[] row : breakdown) {
            Map<String, Object> step = new java.util.LinkedHashMap<>();
            step.put("step", row[0]);
            step.put("count", row[1]);
            step.put("cost", row[2]);
            breakdownList.add(step);
        }
        costData.put("breakdown", breakdownList);

        java.util.List<com.anadoluatlasi.backend.ai.entity.AiGenerationLog> logs =
                aiLogRepository.findByTurkuProjectIdOrderByCreatedAtDesc(id);
        java.util.List<Map<String, Object>> logList = new java.util.ArrayList<>();
        for (var logEntry : logs.stream().limit(50).toList()) {
            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("step", logEntry.getStepName());
            entry.put("sceneNo", logEntry.getSceneNo());
            entry.put("model", logEntry.getModel());
            entry.put("cost", logEntry.getCost());
            entry.put("status", logEntry.getStatus());
            entry.put("createdAt", logEntry.getCreatedAt());
            logList.add(entry);
        }
        costData.put("logs", logList);

        return ResponseEntity.ok(ApiResponse.success(costData));
    }

    /**
     * AI maliyet logunu DB'ye kaydeder.
     */
    private void logAiCost(UUID projectId, String stepName, Integer sceneNo,
                           String provider, String model,
                           java.math.BigDecimal cost, java.math.BigDecimal inputUnits, String unitType,
                           com.anadoluatlasi.backend.ai.entity.AiGenerationStatus status,
                           String inputSummary) {
        try {
            com.anadoluatlasi.backend.ai.entity.AiGenerationLog logEntry =
                    com.anadoluatlasi.backend.ai.entity.AiGenerationLog.builder()
                            .turkuProjectId(projectId)
                            .stepName(stepName)
                            .sceneNo(sceneNo)
                            .provider(provider)
                            .model(model)
                            .cost(cost)
                            .inputUnits(inputUnits)
                            .unitType(unitType)
                            .status(status)
                            .inputSummary(inputSummary)
                            .build();
            aiLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("AI maliyet logu kaydedilemedi: {}", e.getMessage());
        }
    }

    /**
     * Hikâye ve senaryo içeriğinden tutarlı karakter tanımı oluşturur.
     * Bu tanım TÜM sahnelerde aynı kalacak → AI her sahnede aynı karakterleri çizer.
     *
     * <p>Strateji: Hikâye metninden karakter isimlerini ve fiziksel betimlemelerini çıkar,
     * bunları sabit bir "character sheet" formatında prompt'a ekle.</p>
     */
    private String buildCharacterDescription(TurkuVideoProject project) {
        StringBuilder chars = new StringBuilder();

        chars.append("FIXED CHARACTER SHEET — EVERY SCENE MUST USE THESE EXACT PHYSICAL TRAITS. ");
        chars.append("NEVER change hair, face, age, beard, clothing between scenes. ");
        chars.append("Male character = ALWAYS male appearance. Female character = ALWAYS female appearance. ");

        // GPT-üretimi karakter çizelgesi varsa DOĞRUDAN kullan (en iyi seçenek)
        if (project.getStoryPackage() != null
                && project.getStoryPackage().getCharacterSheet() != null
                && project.getStoryPackage().getCharacterSheet().length() > 100) {

            String sheet = project.getStoryPackage().getCharacterSheet();
            // Markdown formatını düz İngilizce prompt'a çevir
            String englishSheet = convertCharacterSheetToPrompt(sheet);
            chars.append(englishSheet);

        } else {
            // Fallback: GPT karakter çizelgesi üretmemişse genel tanım
            chars.append("MAIN MALE CHARACTER (Aşık Sümmani): Turkish man, age 25, ")
                 .append("short dark wavy hair at collar length, light stubble beard, deep brown eyes, ")
                 .append("wheat-colored skin, strong jawline, athletic build. ")
                 .append("ALWAYS wears: dark brown wool vest over cream linen shirt, charcoal şalvar, ")
                 .append("red woven sash at waist, worn leather çarık boots. ")
                 .append("ALWAYS carries a bağlama (Turkish saz) on his back or in hands. ")
                 .append("MAIN FEMALE CHARACTER (Mihriban): Turkish woman, age 20, ")
                 .append("long dark braided hair past waist, olive skin, almond-shaped dark eyes, ")
                 .append("delicate face, slender build. ")
                 .append("ALWAYS wears: deep burgundy embroidered entari dress, ")
                 .append("cream yemeni headscarf loosely draped, silver coin necklace, thin gold bangles. ");
        }

        // Genel yasaklar (her zaman ekle)
        chars.append("GENDER RULE: When scene has a man and woman together, ")
             .append("one MUST clearly be male and the other MUST clearly be female. ")
             .append("FORBIDDEN: turban, sarık, Arabic/Middle-Eastern clothing, ")
             .append("medieval armor, weapons, modern clothing, guitar, Western instruments. ");

        return chars.toString();
    }

    /**
     * Türkçe markdown karakter çizelgesini AI image prompt'a uygun
     * İngilizce düz metne çevirir.
     */
    private String convertCharacterSheetToPrompt(String sheet) {
        StringBuilder prompt = new StringBuilder();

        // Satır satır işle — key: value formatını topla
        String[] lines = sheet.split("\\n");
        String currentCharacter = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // Karakter başlığı (### ile başlayan)
            if (trimmed.startsWith("###") || trimmed.startsWith("## ")) {
                if (currentCharacter != null) prompt.append(". ");
                currentCharacter = trimmed.replaceAll("^#+\\s*", "").trim();
                if (!currentCharacter.toLowerCase().contains("mekan")
                        && !currentCharacter.toLowerCase().contains("dönem")) {
                    prompt.append(currentCharacter.toUpperCase()).append(": ");
                } else {
                    prompt.append("SETTING: ");
                }
                continue;
            }

            // **Key:** Value formatını çıkar
            if (trimmed.startsWith("**") && trimmed.contains(":**")) {
                String value = trimmed.replaceAll("\\*\\*[^*]+:\\*\\*\\s*", "").trim();
                if (!value.isBlank() && !value.startsWith("[")) {
                    String key = trimmed.replaceAll("\\*\\*([^*]+):\\*\\*.*", "$1").trim();
                    // İngilizce çevir (temel alanlar)
                    switch (key.toLowerCase()) {
                        case "i̇sim", "isim" -> prompt.append("Name: ").append(value).append(", ");
                        case "yaş" -> prompt.append("Age: ").append(value).append(", ");
                        case "fiziksel" -> prompt.append("Appearance: ").append(value).append(", ");
                        case "kıyafet" -> prompt.append("Clothing: ").append(value).append(", ");
                        case "ayırt edici" -> prompt.append("Distinctive: ").append(value).append(", ");
                        case "i̇fade", "ifade" -> prompt.append("Expression: ").append(value).append(", ");
                        case "dönem" -> prompt.append("Era: ").append(value).append(", ");
                        case "bölge" -> prompt.append("Region: ").append(value).append(", ");
                        case "mekan detayları" -> prompt.append("Location details: ").append(value).append(", ");
                        case "mevsim" -> prompt.append("Season: ").append(value).append(", ");
                        case "renk paleti" -> prompt.append("Color palette: ").append(value).append(", ");
                        default -> prompt.append(value).append(", ");
                    }
                }
            }
        }

        // Çok uzunsa kırp (prompt limiti)
        String result = prompt.toString();
        if (result.length() > 1500) {
            result = result.substring(0, 1500);
        }

        return result;
    }
}
