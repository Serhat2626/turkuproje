package com.startup.turkuproje.controller;

import com.startup.turkuproje.dto.ApiResponse;
import com.startup.turkuproje.dto.ApproveResearchRequest;
import com.startup.turkuproje.dto.ResearchResponse;
import com.startup.turkuproje.service.ResearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Araştırma endpoint'leri
@RestController
@RequestMapping("/api/v1/projects/{projectId}/research")
@RequiredArgsConstructor
public class ResearchController {

    private final ResearchService researchService;

    // AI ile araştırma başlat
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ResearchResponse>> generateResearch(
            @PathVariable UUID projectId) {
        ResearchResponse response = researchService.generateResearch(projectId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Araştırma başarıyla üretildi", response));
    }

    // Araştırmayı onayla/düzelt
    @PutMapping("/{researchId}/approve")
    public ResponseEntity<ApiResponse<ResearchResponse>> approveResearch(
            @PathVariable UUID projectId,
            @PathVariable UUID researchId,
            @RequestBody ApproveResearchRequest request) {
        ResearchResponse response = researchService.approveResearch(projectId, researchId, request);
        return ResponseEntity.ok(ApiResponse.success("Araştırma onaylandı", response));
    }

    // Araştırmaları listele
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResearchResponse>>> listResearch(
            @PathVariable UUID projectId) {
        List<ResearchResponse> research = researchService.listResearch(projectId);
        return ResponseEntity.ok(ApiResponse.success(research));
    }

    // Araştırma detayı
    @GetMapping("/{researchId}")
    public ResponseEntity<ApiResponse<ResearchResponse>> getResearch(
            @PathVariable UUID projectId,
            @PathVariable UUID researchId) {
        ResearchResponse response = researchService.getResearch(projectId, researchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
