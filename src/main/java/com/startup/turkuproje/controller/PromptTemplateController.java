package com.startup.turkuproje.controller;

import com.startup.turkuproje.dto.ApiResponse;
import com.startup.turkuproje.dto.PromptTemplateDto;
import com.startup.turkuproje.service.PromptTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Prompt şablon yönetim endpoint'leri
@RestController
@RequestMapping("/api/v1/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    // Yeni şablon oluştur
    @PostMapping
    public ResponseEntity<ApiResponse<PromptTemplateDto>> createTemplate(
            @Valid @RequestBody PromptTemplateDto dto) {
        PromptTemplateDto response = promptTemplateService.createTemplate(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prompt şablonu oluşturuldu", response));
    }

    // Tüm şablonları listele
    @GetMapping
    public ResponseEntity<ApiResponse<List<PromptTemplateDto>>> listTemplates() {
        List<PromptTemplateDto> templates = promptTemplateService.listTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    // Şablon güncelle
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromptTemplateDto>> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody PromptTemplateDto dto) {
        PromptTemplateDto response = promptTemplateService.updateTemplate(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Prompt şablonu güncellendi", response));
    }
}
