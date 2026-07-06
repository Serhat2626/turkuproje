package com.anadoluatlasi.backend.review;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * FP-016 Task 03: Internal admin pipeline aggregation controller.
 *
 * <p>Read-only GET endpoints only — no POST, PUT, DELETE.
 * Returns safe derived pipeline views without mutation or side effects.</p>
 *
 * <p>This controller does NOT:</p>
 * <ul>
 *   <li>Mutate review state or create audit records</li>
 *   <li>Call external providers, storage, or generation services</li>
 *   <li>Create public URLs, signed URLs, or CDN entries</li>
 *   <li>Trigger approval, rejection, or status updates</li>
 *   <li>Expose provider URLs, storage keys, raw payloads, or base64</li>
 * </ul>
 *
 * <p>Endpoints: 2 GET (pipeline overview + pipeline detail).
 * Combined with AdminReviewController: 19 total (9 POST + 10 GET).</p>
 *
 * <p>Safe error handling — no stack trace, no provider detail.</p>
 */
@RestController
@RequestMapping("/api/admin/pipeline")
@RequiredArgsConstructor
@Tag(name = "Admin Pipeline Aggregation", description = "Dahili pipeline görünüm (Admin, salt okunur)")
public class AdminPipelineController {

    private final PipelineAggregationService pipelineAggregationService;

    /**
     * Get pipeline overview with optional filters.
     * Read-only aggregation — no mutation, no side effects.
     *
     * @param targetType optional target type filter
     * @param stage      optional pipeline stage filter
     * @param limit      max items (default 50, max 200)
     * @return safe pipeline overview
     */
    @GetMapping("/overview")
    @Operation(summary = "Dahili pipeline genel görünümü (salt okunur)")
    public ResponseEntity<ApiResponse<SafePipelineOverview>> getOverview(
            @RequestParam(required = false) ReviewTargetType targetType,
            @RequestParam(required = false) SafePipelineStage stage,
            @RequestParam(defaultValue = "50") int limit) {

        SafePipelineOverview overview = pipelineAggregationService.getOverview(
                targetType, stage, limit);

        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    /**
     * Get pipeline detail for a single target.
     * Read-only aggregation — no mutation, no side effects.
     *
     * @param targetType review target type
     * @param targetId   target entity ID
     * @return safe pipeline aggregate item
     */
    @GetMapping("/{targetType}/{targetId}")
    @Operation(summary = "Dahili pipeline hedef detayı (salt okunur)")
    public ResponseEntity<ApiResponse<SafePipelineAggregateItem>> getItemDetail(
            @PathVariable ReviewTargetType targetType,
            @PathVariable UUID targetId) {

        SafePipelineAggregateItem item = pipelineAggregationService.getItemDetail(
                targetType, targetId);

        return ResponseEntity.ok(ApiResponse.success(item));
    }
}
