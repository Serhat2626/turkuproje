package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.review.AdminActorBoundaryPolicy;
import com.anadoluatlasi.backend.review.AdminActorResolutionException;
import com.anadoluatlasi.backend.review.AdminActorResolutionRequest;
import com.anadoluatlasi.backend.review.AdminActorResolver;
import com.anadoluatlasi.backend.review.ResolvedAdminActor;
import com.anadoluatlasi.backend.review.ReviewActorRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping("/admin/turku-video-projects")
@Tag(name = "Türkü Video Projects", description = "Türkü video proje yönetimi (dahili/admin)")
public class TurkuVideoProjectController {

    private final TurkuVideoProjectBootstrapService bootstrapService;
    private final TurkuVideoPlanningService planningService;
    private final TurkuVideoAssetPlanningService assetPlanningService;
    private final AdminActorResolver actorResolver;

    public TurkuVideoProjectController(TurkuVideoProjectBootstrapService bootstrapService,
                                        TurkuVideoPlanningService planningService,
                                        TurkuVideoAssetPlanningService assetPlanningService,
                                        AdminActorResolver actorResolver) {
        this.bootstrapService = bootstrapService;
        this.planningService = planningService;
        this.assetPlanningService = assetPlanningService;
        this.actorResolver = actorResolver;
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

    // ── GET /admin/turku-video-projects/{id}/progress ──

    @GetMapping("/{id}/progress")
    @Operation(summary = "Türkü video projesi ilerleme durumu",
               description = "Güvenli aşama ve ilerleme bilgisi döndürür.")
    public ResponseEntity<ApiResponse<TurkuVideoProgressResponse>> getProgress(
            @PathVariable UUID id) {

        TurkuVideoProject project = bootstrapService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(TurkuVideoProgressResponse.from(project)));
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

    // ── Exception Handling ──

    @ExceptionHandler(TurkuVideoProjectException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectException(TurkuVideoProjectException ex) {
        HttpStatus status = switch (ex.getSafeErrorCode()) {
            case "PROJECT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "TURKU_NAME_REQUIRED", "TURKU_NAME_TOO_LONG",
                 "UNSAFE_INPUT", "FIELD_TOO_LONG", "INVALID_DURATION",
                 "INVALID_STAGE_TRANSITION", "MISSING_PREREQUISITE" -> HttpStatus.BAD_REQUEST;
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
}
