package com.anadoluatlasi.backend.knowledge.controller;

import com.anadoluatlasi.backend.application.knowledge.create.CreateKnowledgeItemCommand;
import com.anadoluatlasi.backend.application.knowledge.create.CreateKnowledgeItemResult;
import com.anadoluatlasi.backend.application.knowledge.create.CreateKnowledgeItemUseCase;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.knowledge.controller.dto.CreateKnowledgeItemRequest;
import com.anadoluatlasi.backend.knowledge.controller.dto.KnowledgeItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bilgi öğesi (KnowledgeItem) REST controller.
 * Yalnızca UseCase çağırır. Repository veya entity kullanmaz.
 */
@RestController
@RequestMapping("/api/knowledge-items")
@RequiredArgsConstructor
@Tag(name = "Knowledge Items", description = "Bilgi bankası yönetimi")
public class KnowledgeItemController {

    private final CreateKnowledgeItemUseCase createKnowledgeItemUseCase;

    @PostMapping
    @Operation(summary = "Yeni bilgi öğesi oluştur", description = "Bilgi bankasına yeni bir kültürel bilgi öğesi ekler")
    public ResponseEntity<ApiResponse<KnowledgeItemResponse>> create(
            @Valid @RequestBody CreateKnowledgeItemRequest request) {

        // Request → Command
        CreateKnowledgeItemCommand command = CreateKnowledgeItemCommand.builder()
                .title(request.getTitle())
                .type(request.getType())
                .language(request.getLanguage())
                .summary(request.getSummary())
                .build();

        // UseCase çağrısı
        CreateKnowledgeItemResult result = createKnowledgeItemUseCase.execute(command);

        // Result → Response
        KnowledgeItemResponse response = KnowledgeItemResponse.builder()
                .id(result.getId())
                .title(result.getTitle())
                .slug(result.getSlug())
                .type(result.getType())
                .status(result.getStatus())
                .language(result.getLanguage())
                .createdAt(result.getCreatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
