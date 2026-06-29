package com.anadoluatlasi.backend.content.controller;

import com.anadoluatlasi.backend.application.content.ContentDraftResult;
import com.anadoluatlasi.backend.application.content.ContentDraftService;
import com.anadoluatlasi.backend.application.content.UpdateContentDraftCommand;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.content.controller.dto.ContentDraftResponse;
import com.anadoluatlasi.backend.content.controller.dto.UpdateContentDraftRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * FP-001: Admin Content Draft REST controller.
 * Yalnızca ContentDraftService çağırır.
 * İş kuralı, repository veya entity kullanmaz.
 */
@RestController
@RequestMapping("/api/admin/content-drafts")
@RequiredArgsConstructor
@Tag(name = "Admin Content Drafts", description = "İçerik taslağı görüntüleme ve düzenleme (Admin)")
public class ContentDraftController {

    private final ContentDraftService contentDraftService;

    // ── GET /api/admin/content-drafts/{id} ──────────────────

    @GetMapping("/{contentDraftId}")
    @Operation(summary = "Draft detayını getir")
    public ResponseEntity<ApiResponse<ContentDraftResponse>> getDetail(
            @PathVariable UUID contentDraftId) {

        ContentDraftResult result = contentDraftService.getDetail(contentDraftId);

        return ResponseEntity.ok(ApiResponse.success(toResponse(result)));
    }

    // ── PUT /api/admin/content-drafts/{id} ──────────────────

    @PutMapping("/{contentDraftId}")
    @Operation(summary = "Draft'ı manuel düzenle")
    public ResponseEntity<ApiResponse<ContentDraftResponse>> update(
            @PathVariable UUID contentDraftId,
            @RequestBody UpdateContentDraftRequest request) {

        UpdateContentDraftCommand command = UpdateContentDraftCommand.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .summary(request.getSummary())
                .build();

        ContentDraftResult result = contentDraftService.update(contentDraftId, command);

        return ResponseEntity.ok(ApiResponse.success(toResponse(result)));
    }

    // ── Private Mapping ─────────────────────────────────────

    private ContentDraftResponse toResponse(ContentDraftResult result) {
        return ContentDraftResponse.builder()
                .id(result.getId())
                .contentProjectId(result.getContentProjectId())
                .title(result.getTitle())
                .body(result.getBody())
                .summary(result.getSummary())
                .status(result.getStatus())
                .generatedBy(result.getGeneratedBy())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}
