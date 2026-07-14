package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.config.MinioConfig;
import com.anadoluatlasi.backend.review.AdminActorBoundaryPolicy;
import com.anadoluatlasi.backend.review.AdminActorResolutionException;
import com.anadoluatlasi.backend.review.AdminActorResolutionRequest;
import com.anadoluatlasi.backend.review.AdminActorResolver;
import com.anadoluatlasi.backend.review.ResolvedAdminActor;
import com.anadoluatlasi.backend.review.ReviewActorRole;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    @Operation(summary = "Tam video pipeline",
               description = "Tek tıkla: OpenAI hikâye+senaryo+storyboard → DALL-E görseller → TTS ses → FFmpeg video. Zaten hazır olan adımlar atlanır.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateFullVideo(@PathVariable UUID id) {
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

            if (storyboard != null && storyboard.getScenes() != null) {
                int imgSuccess = 0, imgFail = 0, imgSkipped = 0;
                int totalScenes = storyboard.getScenes().size();
                String previousVisual = null;

                for (TurkuStoryboardScene scene : storyboard.getScenes()) {
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
                            if (result.isSuccess()) imgSuccess++;
                            else imgFail++;
                        } else if (providerDraftService.getGateway() instanceof OpenAiTurkuGenerationProviderGateway openAiGateway) {
                            openAiGateway.generateImage(imgReq,
                            TurkuGenerationSafetyEnvelope.createDefault(id, "IMAGE_GENERATION", TurkuProviderMode.DETERMINISTIC_STUB));
                            imgSuccess++;
                        }

                        // Önceki sahne görselini güncelle (sonraki sahne için)
                        previousVisual = scene.getVisualDescription();
                    } catch (Exception e) {
                        imgFail++;
                        previousVisual = scene.getVisualDescription();
                    }
                }
                results.put("step2_images", imageProvider + ": ✅ " + imgSuccess + " yeni, ⏭️ " + imgSkipped + " mevcut, ❌ " + imgFail + " hata");
            }

            // ADIM 2.5: Image-to-Video (Kling AI ile hareketlendirme)
            if (falVideoService != null && storyboard != null && storyboard.getScenes() != null) {
                int vidSuccess = 0, vidFail = 0, vidSkipped = 0;
                for (TurkuStoryboardScene scene : storyboard.getScenes()) {
                    // Video klip zaten varsa atla
                    String videoPath = String.format("turku-video/%s/scenes/%d_video.mp4", id, scene.getSceneNo());
                    try {
                        minioClient.statObject(io.minio.StatObjectArgs.builder()
                                .bucket(minioConfig.getBucketName()).object(videoPath).build());
                        vidSkipped++;
                        continue;
                    } catch (Exception ignored) {}

                    // Görsel URL'sini oluştur (MinIO'dan)
                    String imagePath = String.format("turku-video/%s/scenes/%d.png", id, scene.getSceneNo());
                    try {
                        // MinIO görsel var mı kontrol
                        minioClient.statObject(io.minio.StatObjectArgs.builder()
                                .bucket(minioConfig.getBucketName()).object(imagePath).build());

                        // Geçici presigned URL oluştur (Kling erişebilsin)
                        String imageUrl = minioClient.getPresignedObjectUrl(
                                io.minio.GetPresignedObjectUrlArgs.builder()
                                        .bucket(minioConfig.getBucketName())
                                        .object(imagePath)
                                        .method(io.minio.http.Method.GET)
                                        .expiry(15, java.util.concurrent.TimeUnit.MINUTES)
                                        .build());

                        // Hareket prompt'u: sahne mood + görsel açıklama
                        String motionPrompt = "Subtle cinematic motion in scene: " +
                                scene.getVisualDescription().substring(0, Math.min(150, scene.getVisualDescription().length()));
                        if (scene.getMood() != null) {
                            motionPrompt += ". Mood: " + scene.getMood();
                        }

                        FalVideoGenerationService.VideoGenerationResult vidResult =
                                falVideoService.generateVideo(id, scene.getSceneNo(), imageUrl, motionPrompt);

                        if (vidResult.isSuccess()) vidSuccess++;
                        else vidFail++;
                    } catch (Exception e) {
                        vidFail++;
                        // Hata sayacında zaten izleniyor
                    }
                }
                results.put("step2_5_video", "Kling AI: ✅ " + vidSuccess + " video, ⏭️ " + vidSkipped + " mevcut, ❌ " + vidFail + " hata");
            } else if (falVideoService == null) {
                results.put("step2_5_video", "⏭️ Video hareketlendirme kapalı (ai.video-provider=kling yapın)");
            }

            // ADIM 3: TTS seslendirme (MinIO'da zaten varsa atla)
            boolean hasTtsProvider = (elevenLabsTtsService != null || ttsService != null);
            String ttsLabel = elevenLabsTtsService != null ? "ElevenLabs" : "OpenAI";
            if (hasTtsProvider) {
                if (storyboard != null && storyboard.getScenes() != null) {
                    boolean allAudioExists = true;
                    for (TurkuStoryboardScene scene : storyboard.getScenes()) {
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
                        TurkuTtsService.TurkuTtsResponse ttsResult;
                        if (elevenLabsTtsService != null) {
                            ttsResult = elevenLabsTtsService.generateForProject(id);
                        } else {
                            ttsResult = ttsService.generateForProject(id);
                        }
                        results.put("step3_tts", "✅ " + ttsLabel + ": " + ttsResult.getSuccessCount() + " ses, ❌ " + ttsResult.getFailureCount() + " hata");
                    }
                }
            } else {
                results.put("step3_tts", "⏭️ TTS servisi yok (ai.tts-provider ayarlayın)");
            }

            // ADIM 4: FFmpeg video birleştirme (her zaman yeniden yapılır — müzik vs. değişmiş olabilir)
            TurkuVideoAssemblyService.VideoAssemblyResult videoResult = videoAssemblyService.assembleVideo(id);
            if (videoResult.isSuccess()) {
                results.put("step4_video", "✅ " + videoResult.getSegmentCount() + " segment, " +
                        (videoResult.getVideoSizeBytes() / 1024 / 1024) + " MB");
                results.put("videoUrl", "/api/admin/turku-video-projects/" + id + "/video");
            } else {
                results.put("step4_video", "❌ " + videoResult.getError());
            }

            // Gerçek başarı durumu — video oluştu mu?
            boolean pipelineSuccess = videoResult.isSuccess();
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

    /**
     * Hikâye ve senaryo içeriğinden tutarlı karakter tanımı oluşturur.
     * Bu tanım TÜM sahnelerde aynı kalacak → AI her sahnede aynı karakterleri çizer.
     *
     * <p>Strateji: Hikâye metninden karakter isimlerini ve fiziksel betimlemelerini çıkar,
     * bunları sabit bir "character sheet" formatında prompt'a ekle.</p>
     */
    private String buildCharacterDescription(TurkuVideoProject project) {
        StringBuilder chars = new StringBuilder();

        chars.append("CRITICAL CHARACTER SHEET — USE EXACT SAME APPEARANCE IN EVERY SCENE. ");
        chars.append("DO NOT change age, face, hair, clothes between scenes. ");

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
            chars.append("Main male character: YOUNG Anatolian man, age 25-30, dark hair, ")
                 .append("short beard or clean-shaven, brown eyes, handsome. ")
                 .append("Simple 19th century village clothes: white gömlek, dark yelek, şalvar. ")
                 .append("NO turban, NO gray hair, NOT elderly. ")
                 .append("Main female character: YOUNG Anatolian woman, age 20-25, ")
                 .append("long dark hair, beautiful, traditional colorful village dress (entari). ");
        }

        // Genel yasaklar (her zaman ekle)
        chars.append("FORBIDDEN: turban, sarık, Arabic/Middle-Eastern clothing, ")
             .append("medieval armor, weapons, modern clothing, text, watermark. ");

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
