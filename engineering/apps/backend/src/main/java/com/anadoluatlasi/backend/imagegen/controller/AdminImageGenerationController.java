package com.anadoluatlasi.backend.imagegen.controller;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.imagegen.controller.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * FP-007: Admin Image Generation Workflow REST controller.
 *
 * <p>Thin controller skeleton — explicit action endpoint'ler.
 * Generic status update endpoint yoktur.
 * READY_FOR_PUBLISH, publish, export endpoint yoktur.
 * Raw provider payload/response, internal prompt, API key/secret döndürülmez.</p>
 *
 * <p>Service layer henüz oluşturulmadığı için tüm endpoint'ler
 * 501 Not Implemented döner. Gerçek implementasyon sonraki tasklarda yapılacaktır.</p>
 *
 * <p>Human review explicit approve/reject action ile yapılır.
 * AI output hiçbir zaman otomatik APPROVED olamaz.</p>
 */
@RestController
@RequestMapping("/api/admin/image-generation")
@RequiredArgsConstructor
@Tag(name = "Admin Image Generation Workflow",
     description = "FP-007: Image generation ve review aksiyonları (Admin)")
public class AdminImageGenerationController {

    // ── Generation Request Endpoints ────────────────────────────────

    /**
     * APPROVED VisualAssetPlan'dan image generation başlatır.
     *
     * <p>Prompt üretimi server-side yapılır.
     * Request body doğrudan prompt taşımaz.</p>
     */
    @PostMapping("/requests")
    @Operation(summary = "Image generation başlat",
               description = "APPROVED VisualAssetPlan için image generation request oluşturur")
    public ResponseEntity<ApiResponse<ImageGenerationRequestResponse>> startGeneration(
            @Valid @RequestBody StartImageGenerationRequest request) {
        // TODO: FP-007 Task 07 — Generation Request Service
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Image generation request service henüz uygulanmadı");
    }

    /**
     * Generation request'leri listeler.
     * Opsiyonel filtre parametreleri ile daraltılabilir.
     */
    @GetMapping("/requests")
    @Operation(summary = "Generation request'leri listele")
    public ResponseEntity<ApiResponse<List<ImageGenerationRequestResponse>>> listRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID visualAssetPlanId,
            @RequestParam(required = false) UUID visualAssetItemId,
            @RequestParam(required = false) UUID contentProjectId,
            @RequestParam(required = false) UUID correlationId) {
        // TODO: FP-007 Task 07 — Generation Request Service
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Generation request listesi henüz uygulanmadı");
    }

    /**
     * Tek bir generation request detayını döner.
     */
    @GetMapping("/requests/{requestId}")
    @Operation(summary = "Generation request detayı")
    public ResponseEntity<ApiResponse<ImageGenerationRequestResponse>> getRequest(
            @PathVariable UUID requestId) {
        // TODO: FP-007 Task 07 — Generation Request Service
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Generation request detay servisi henüz uygulanmadı");
    }

    // ── Generated Asset Endpoints ───────────────────────────────────

    /**
     * Review bekleyen generated asset'leri listeler.
     * Status: GENERATED_PENDING_REVIEW.
     */
    @GetMapping("/generated-assets/pending-review")
    @Operation(summary = "Review bekleyen generated asset'leri listele")
    public ResponseEntity<ApiResponse<List<ImageGeneratedAssetResponse>>> listPendingReview() {
        // TODO: FP-007 Task 10 — Human Review Workflow
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Pending review listesi henüz uygulanmadı");
    }

    /**
     * Tüm generated asset'leri listeler.
     * Opsiyonel filtre parametreleri.
     */
    @GetMapping("/generated-assets")
    @Operation(summary = "Generated asset'leri listele")
    public ResponseEntity<ApiResponse<List<ImageGeneratedAssetResponse>>> listGeneratedAssets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID visualAssetPlanId,
            @RequestParam(required = false) UUID contentProjectId) {
        // TODO: FP-007 Task 09 — GeneratedAsset Service
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Generated asset listesi henüz uygulanmadı");
    }

    /**
     * Generated asset review detayını döner.
     *
     * <p>Asset bilgisi, generation request, generation result,
     * review geçmişi ve lineage bilgisi içerir.
     * Raw provider payload/response, internal prompt döndürülmez.</p>
     */
    @GetMapping("/generated-assets/{generatedAssetId}/review-detail")
    @Operation(summary = "Generated asset review detayı",
               description = "Asset, request, result, review geçmişi ve lineage — safe metadata only")
    public ResponseEntity<ApiResponse<GeneratedAssetReviewDetailResponse>> getReviewDetail(
            @PathVariable UUID generatedAssetId) {
        // TODO: FP-007 Task 10 — Human Review Workflow
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Review detail servisi henüz uygulanmadı");
    }

    // ── Review Action Endpoints ─────────────────────────────────────

    /**
     * Generated asset'i onaylar.
     *
     * <p>Yalnızca GENERATED_PENDING_REVIEW status'ten APPROVED'a geçiş.
     * Generic status update değildir — explicit approve action.</p>
     */
    @PostMapping("/generated-assets/{generatedAssetId}/approve")
    @Operation(summary = "Generated asset'i onayla",
               description = "Human review: explicit approve action")
    public ResponseEntity<ApiResponse<ImageGeneratedAssetResponse>> approveGeneratedAsset(
            @PathVariable UUID generatedAssetId,
            @Valid @RequestBody(required = false) ApproveGeneratedAssetRequest request) {
        // TODO: FP-007 Task 10 — Human Review Workflow
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Approve review servisi henüz uygulanmadı");
    }

    /**
     * Generated asset'i reddeder.
     *
     * <p>Yalnızca GENERATED_PENDING_REVIEW status'ten REJECTED'a geçiş.
     * Rejection reason zorunludur. Generic status update değildir.</p>
     */
    @PostMapping("/generated-assets/{generatedAssetId}/reject")
    @Operation(summary = "Generated asset'i reddet",
               description = "Human review: explicit reject action — rejection reason zorunlu")
    public ResponseEntity<ApiResponse<ImageGeneratedAssetResponse>> rejectGeneratedAsset(
            @PathVariable UUID generatedAssetId,
            @Valid @RequestBody RejectGeneratedAssetRequest request) {
        // TODO: FP-007 Task 10 — Human Review Workflow
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Reject review servisi henüz uygulanmadı");
    }

    // ── Preview Endpoint ────────────────────────────────────────────

    /**
     * Generated image preview — backend proxy.
     *
     * <p>Storage'dan serve edilen gerçek image. Admin-facing URL,
     * kalıcı public URL veya signed URL döndürülmez.
     * Image data backend üzerinden proxy edilir.</p>
     */
    @GetMapping("/generated-assets/{generatedAssetId}/preview")
    @Operation(summary = "Generated image preview",
               description = "Backend proxy — storage key/URL expose edilmez")
    public ResponseEntity<byte[]> getPreview(@PathVariable UUID generatedAssetId) {
        // TODO: FP-007 Task 09 — Storage Handoff / Preview
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Preview servisi henüz uygulanmadı");
    }

    // ── NOT IMPLEMENTED: Forbidden Endpoints ────────────────────────
    //
    // The following endpoints are EXPLICITLY excluded per FP-007 governance:
    //
    // PATCH  /generated-assets/{id}/status      → NO generic status update
    // PUT    /generated-assets/{id}              → NO generic update
    // POST   .../publish                         → NO publishing
    // POST   .../export                          → NO export automation
    // POST   .../ready-for-publish               → NO READY_FOR_PUBLISH
    // GET    .../raw-provider-response            → NO raw provider data
    // GET    .../internal-prompt                  → NO internal prompt
    // GET    .../signed-url                       → NO signed URL exposure
}
