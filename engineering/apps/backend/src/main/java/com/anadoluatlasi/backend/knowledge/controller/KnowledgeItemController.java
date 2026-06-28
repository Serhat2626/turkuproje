package com.anadoluatlasi.backend.knowledge.controller;

import com.anadoluatlasi.backend.application.knowledge.CreateKnowledgeItemCommand;
import com.anadoluatlasi.backend.application.knowledge.KnowledgeItemResult;
import com.anadoluatlasi.backend.application.knowledge.KnowledgeItemService;
import com.anadoluatlasi.backend.application.knowledge.UpdateKnowledgeItemCommand;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.knowledge.controller.dto.ActionStatusResponse;
import com.anadoluatlasi.backend.knowledge.controller.dto.CreateKnowledgeItemRequest;
import com.anadoluatlasi.backend.knowledge.controller.dto.KnowledgeItemResponse;
import com.anadoluatlasi.backend.knowledge.controller.dto.KnowledgeItemSummaryResponse;
import com.anadoluatlasi.backend.knowledge.controller.dto.UpdateKnowledgeItemRequest;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FP-001: Admin Knowledge Item REST controller.
 * Yalnızca KnowledgeItemService çağırır. İş kuralı, repository veya entity kullanmaz.
 */
@RestController
@RequestMapping("/api/admin/knowledge-items")
@RequiredArgsConstructor
@Tag(name = "Admin Knowledge Items", description = "Bilgi bankası yönetimi (Admin)")
public class KnowledgeItemController {

    private final KnowledgeItemService knowledgeItemService;

    // ── POST /api/admin/knowledge-items ─────────────────────

    @PostMapping
    @Operation(summary = "Yeni bilgi öğesi oluştur")
    public ResponseEntity<ApiResponse<KnowledgeItemResponse>> create(
            @Valid @RequestBody CreateKnowledgeItemRequest request) {

        CreateKnowledgeItemCommand command = CreateKnowledgeItemCommand.builder()
                .title(request.getTitle())
                .type(request.getType())
                .language(request.getLanguage())
                .summary(request.getSummary())
                .description(request.getDescription())
                .category(request.getCategory())
                .city(request.getCity())
                .district(request.getDistrict())
                .locationText(request.getLocationText())
                .notes(request.getNotes())
                .build();

        KnowledgeItemResult result = knowledgeItemService.create(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(result)));
    }

    // ── GET /api/admin/knowledge-items ──────────────────────

    @GetMapping
    @Operation(summary = "Bilgi öğelerini listele", description = "Opsiyonel status filtresi ile listeleme")
    public ResponseEntity<ApiResponse<List<KnowledgeItemSummaryResponse>>> list(
            @RequestParam(required = false) String status) {

        KnowledgeItemStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            statusFilter = KnowledgeItemStatus.valueOf(status);
        }

        List<KnowledgeItemSummaryResponse> summaries = knowledgeItemService.list(statusFilter)
                .stream()
                .map(this::toSummaryResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    // ── GET /api/admin/knowledge-items/{id} ─────────────────

    @GetMapping("/{knowledgeItemId}")
    @Operation(summary = "Bilgi öğesi detayı")
    public ResponseEntity<ApiResponse<KnowledgeItemResponse>> getDetail(
            @PathVariable UUID knowledgeItemId) {

        KnowledgeItemResult result = knowledgeItemService.getDetail(knowledgeItemId);

        return ResponseEntity.ok(ApiResponse.success(toResponse(result)));
    }

    // ── PUT /api/admin/knowledge-items/{id} ─────────────────

    @PutMapping("/{knowledgeItemId}")
    @Operation(summary = "Bilgi öğesini güncelle")
    public ResponseEntity<ApiResponse<KnowledgeItemResponse>> update(
            @PathVariable UUID knowledgeItemId,
            @Valid @RequestBody UpdateKnowledgeItemRequest request) {

        UpdateKnowledgeItemCommand command = UpdateKnowledgeItemCommand.builder()
                .title(request.getTitle())
                .type(request.getType())
                .language(request.getLanguage())
                .summary(request.getSummary())
                .description(request.getDescription())
                .category(request.getCategory())
                .city(request.getCity())
                .district(request.getDistrict())
                .locationText(request.getLocationText())
                .notes(request.getNotes())
                .build();

        KnowledgeItemResult result = knowledgeItemService.update(knowledgeItemId, command);

        return ResponseEntity.ok(ApiResponse.success(toResponse(result)));
    }

    // ── POST /api/admin/knowledge-items/{id}/mark-ready ─────

    @PostMapping("/{knowledgeItemId}/mark-ready")
    @Operation(summary = "Bilgi öğesini READY statüsüne geçir")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> markReady(
            @PathVariable UUID knowledgeItemId) {

        KnowledgeItemResult result = knowledgeItemService.markReady(knowledgeItemId);

        ActionStatusResponse action = ActionStatusResponse.builder()
                .id(result.getId())
                .status(result.getStatus())
                .message("Bilgi öğesi READY statüsüne geçirildi")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(action));
    }

    // ── POST /api/admin/knowledge-items/{id}/archive ────────

    @PostMapping("/{knowledgeItemId}/archive")
    @Operation(summary = "Bilgi öğesini arşivle")
    public ResponseEntity<ApiResponse<ActionStatusResponse>> archive(
            @PathVariable UUID knowledgeItemId) {

        KnowledgeItemResult result = knowledgeItemService.archive(knowledgeItemId);

        ActionStatusResponse action = ActionStatusResponse.builder()
                .id(result.getId())
                .status(result.getStatus())
                .message("Bilgi öğesi arşivlendi")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(action));
    }

    // ── Private Mapping Helpers ─────────────────────────────

    private KnowledgeItemResponse toResponse(KnowledgeItemResult result) {
        return KnowledgeItemResponse.builder()
                .id(result.getId())
                .title(result.getTitle())
                .slug(result.getSlug())
                .type(result.getType())
                .summary(result.getSummary())
                .description(result.getDescription())
                .category(result.getCategory())
                .city(result.getCity())
                .district(result.getDistrict())
                .locationText(result.getLocationText())
                .notes(result.getNotes())
                .status(result.getStatus())
                .language(result.getLanguage())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }

    private KnowledgeItemSummaryResponse toSummaryResponse(KnowledgeItemResult result) {
        return KnowledgeItemSummaryResponse.builder()
                .id(result.getId())
                .title(result.getTitle())
                .slug(result.getSlug())
                .type(result.getType())
                .category(result.getCategory())
                .city(result.getCity())
                .status(result.getStatus())
                .language(result.getLanguage())
                .createdAt(result.getCreatedAt())
                .build();
    }
}
