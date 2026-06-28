package com.anadoluatlasi.backend.content.controller;

import com.anadoluatlasi.backend.application.content.create.CreateContentProjectCommand;
import com.anadoluatlasi.backend.application.content.create.CreateContentProjectResult;
import com.anadoluatlasi.backend.application.content.create.CreateContentProjectUseCase;
import com.anadoluatlasi.backend.common.api.ApiResponse;
import com.anadoluatlasi.backend.content.controller.dto.ContentProjectResponse;
import com.anadoluatlasi.backend.content.controller.dto.CreateContentProjectRequest;
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
 * İçerik projesi (ContentProject) REST controller.
 * Yalnızca UseCase çağırır. Repository veya entity kullanmaz.
 */
@RestController
@RequestMapping("/api/content-projects")
@RequiredArgsConstructor
@Tag(name = "Content Projects", description = "İçerik projesi yönetimi")
public class ContentProjectController {

    private final CreateContentProjectUseCase createContentProjectUseCase;

    @PostMapping
    @Operation(summary = "Yeni içerik projesi oluştur", description = "Bir bilgi öğesinden yeni içerik projesi başlatır")
    public ResponseEntity<ApiResponse<ContentProjectResponse>> create(
            @Valid @RequestBody CreateContentProjectRequest request) {

        // Request → Command
        CreateContentProjectCommand command = CreateContentProjectCommand.builder()
                .knowledgeItemId(request.getKnowledgeItemId())
                .title(request.getTitle())
                .contentType(request.getContentType())
                .targetPlatform(request.getTargetPlatform())
                .language(request.getLanguage())
                .build();

        // UseCase çağrısı
        CreateContentProjectResult result = createContentProjectUseCase.execute(command);

        // Result → Response
        ContentProjectResponse response = ContentProjectResponse.builder()
                .id(result.getId())
                .knowledgeItemId(result.getKnowledgeItemId())
                .title(result.getTitle())
                .slug(result.getSlug())
                .contentType(result.getContentType())
                .targetPlatform(result.getTargetPlatform())
                .status(result.getStatus())
                .language(result.getLanguage())
                .createdAt(result.getCreatedAt())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
