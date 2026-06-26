package com.startup.turkuproje.controller;

import com.startup.turkuproje.dto.ApiResponse;
import com.startup.turkuproje.dto.ScriptResponse;
import com.startup.turkuproje.service.ScriptGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Senaryo üretim ve listeleme endpoint'leri
@RestController
@RequestMapping("/api/v1/projects/{projectId}/scripts")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptGenerationService scriptGenerationService;

    // AI ile senaryo üret
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ScriptResponse>> generateScript(
            @PathVariable UUID projectId) {
        ScriptResponse response = scriptGenerationService.generateScript(projectId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Senaryo başarıyla üretildi", response));
    }

    // Projeye ait senaryoları listele
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScriptResponse>>> listScripts(
            @PathVariable UUID projectId) {
        List<ScriptResponse> scripts = scriptGenerationService.listScripts(projectId);
        return ResponseEntity.ok(ApiResponse.success(scripts));
    }

    // Senaryo detayı
    @GetMapping("/{scriptId}")
    public ResponseEntity<ApiResponse<ScriptResponse>> getScript(
            @PathVariable UUID projectId,
            @PathVariable UUID scriptId) {
        ScriptResponse response = scriptGenerationService.getScript(projectId, scriptId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
