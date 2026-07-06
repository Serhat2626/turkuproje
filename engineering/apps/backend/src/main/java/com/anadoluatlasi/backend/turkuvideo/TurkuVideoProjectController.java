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
    private final AdminActorResolver actorResolver;

    public TurkuVideoProjectController(TurkuVideoProjectBootstrapService bootstrapService,
                                        AdminActorResolver actorResolver) {
        this.bootstrapService = bootstrapService;
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

    // ── Actor Resolution ──

    private ResolvedAdminActor resolveBootstrapActor(ReviewActorRole role, String reference) {
        // SYSTEM cannot bootstrap projects from request body
        boolean systemAllowed = false;

        if (role == null) {
            // Default to ADMIN for bootstrap if no role provided
            role = ReviewActorRole.ADMIN;
        }

        AdminActorResolutionRequest resolutionRequest = AdminActorResolutionRequest.builder()
                .requestedActorRole(role)
                .requestedActorReference(reference)
                .fallbackAllowed(true)
                .systemInternalAllowed(systemAllowed)
                .build();

        ResolvedAdminActor actor = actorResolver.resolve(resolutionRequest);

        // Boundary enforcement: TURKU_PROJECT_BOOTSTRAP allows ADMIN, REVIEWER
        // CREATOR is allowed for bootstrap (can start their own project)
        // SYSTEM is blocked
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
                 "UNSAFE_INPUT", "FIELD_TOO_LONG", "INVALID_DURATION" -> HttpStatus.BAD_REQUEST;
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
