package com.startup.turkuproje.controller;

import com.startup.turkuproje.dto.ApiResponse;
import com.startup.turkuproje.dto.CreateProjectRequest;
import com.startup.turkuproje.dto.ProjectResponse;
import com.startup.turkuproje.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Proje CRUD endpoint'leri
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // Yeni proje oluştur
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Proje başarıyla oluşturuldu", response));
    }

    // Tüm projeleri listele
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> listProjects() {
        List<ProjectResponse> projects = projectService.listProjects();
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    // Proje detayı
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable UUID id) {
        ProjectResponse response = projectService.getProject(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
