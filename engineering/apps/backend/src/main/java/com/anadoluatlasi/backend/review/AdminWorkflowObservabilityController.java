package com.anadoluatlasi.backend.review;

import com.anadoluatlasi.backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FP-017 Task 03: Internal admin workflow observability controller.
 *
 * <p>Read-only GET endpoints only — no POST, PUT, DELETE.
 * Returns safe derived observability views without mutation or side effects.</p>
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
 * <p>Endpoints: 2 GET (summary + diagnostics).
 * Combined total: 21 (9 POST + 12 GET).</p>
 *
 * <p>Safe error handling — no stack trace, no provider detail.</p>
 */
@RestController
@RequestMapping("/api/admin/workflow-observability")
@RequiredArgsConstructor
@Tag(name = "Admin Workflow Observability", description = "Dahili iş akışı gözlemlenebilirlik (Admin, salt okunur)")
public class AdminWorkflowObservabilityController {

    private final WorkflowObservabilityService observabilityService;

    /**
     * Get aggregate workflow observability summary.
     * Read-only — no mutation, no side effects.
     *
     * @return safe observability summary with counts and diagnostics
     */
    @GetMapping("/summary")
    @Operation(summary = "Dahili iş akışı gözlemlenebilirlik özeti (salt okunur)")
    public ResponseEntity<ApiResponse<SafeWorkflowObservabilitySummary>> getSummary() {

        SafeWorkflowObservabilitySummary summary = observabilityService.getSummary();

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get workflow diagnostic items needing attention.
     * Read-only — no mutation, no side effects.
     *
     * @param targetType optional target type filter
     * @return safe diagnostic item list
     */
    @GetMapping("/diagnostics")
    @Operation(summary = "Dahili iş akışı tanılama öğeleri (salt okunur)")
    public ResponseEntity<ApiResponse<List<SafeWorkflowDiagnosticItem>>> getDiagnostics(
            @RequestParam(required = false) ReviewTargetType targetType) {

        List<SafeWorkflowDiagnosticItem> diagnostics = observabilityService.getDiagnostics(targetType);

        return ResponseEntity.ok(ApiResponse.success(diagnostics));
    }
}
