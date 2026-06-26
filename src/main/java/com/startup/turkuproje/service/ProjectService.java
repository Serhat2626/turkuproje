package com.startup.turkuproje.service;

import com.startup.turkuproje.dto.CreateProjectRequest;
import com.startup.turkuproje.dto.ProjectResponse;
import com.startup.turkuproje.entity.Project;
import com.startup.turkuproje.entity.ProjectStatus;
import com.startup.turkuproje.exception.ResourceNotFoundException;
import com.startup.turkuproje.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Proje CRUD işlemleri ve durum yönetimi
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;

    // Yeni proje oluştur
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        String title = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle()
                : request.getTopic();

        Project project = Project.builder()
                .title(title)
                .topic(request.getTopic())
                .niche(request.getNiche())
                .language(request.getLanguage())
                .targetPlatforms(request.getTargetPlatforms())
                .videoDuration(request.getVideoDuration())
                .build();

        Project saved = projectRepository.save(project);
        log.info("Yeni proje oluşturuldu: id={}, topic={}", saved.getId(), saved.getTopic());
        return toResponse(saved);
    }

    // Proje detayı getir
    public ProjectResponse getProject(UUID id) {
        Project project = findProjectById(id);
        return toResponse(project);
    }

    // Tüm projeleri listele
    public List<ProjectResponse> listProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Proje durumunu güncelle (dahili kullanım)
    @Transactional
    public void updateStatus(UUID projectId, ProjectStatus newStatus) {
        Project project = findProjectById(projectId);
        ProjectStatus oldStatus = project.getStatus();
        project.setStatus(newStatus);
        projectRepository.save(project);
        log.info("Proje durumu güncellendi: id={}, {} -> {}", projectId, oldStatus, newStatus);
    }

    // Proje entity'sini bul (dahili yardımcı)
    public Project findProjectById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proje", "id", id));
    }

    // Entity -> DTO dönüşümü
    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .topic(project.getTopic())
                .niche(project.getNiche())
                .language(project.getLanguage())
                .status(project.getStatus())
                .targetPlatforms(project.getTargetPlatforms())
                .videoDuration(project.getVideoDuration())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
