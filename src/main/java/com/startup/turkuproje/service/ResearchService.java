package com.startup.turkuproje.service;

import com.startup.turkuproje.dto.ApproveResearchRequest;
import com.startup.turkuproje.dto.ResearchResponse;
import com.startup.turkuproje.entity.*;
import com.startup.turkuproje.exception.ResourceNotFoundException;
import com.startup.turkuproje.repository.ResearchNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Araştırma servisi - konu hakkında AI ile araştırma yapar
@Service
@RequiredArgsConstructor
@Slf4j
public class ResearchService {

    private final ProjectService projectService;
    private final PromptTemplateService promptTemplateService;
    private final AiService aiService;
    private final ResearchNoteRepository researchNoteRepository;

    /**
     * Proje konusu hakkında AI ile araştırma yap.
     */
    @Transactional
    public ResearchResponse generateResearch(UUID projectId) {
        Project project = projectService.findProjectById(projectId);
        log.info("Araştırma başlatıldı: projectId={}, topic={}", projectId, project.getTopic());

        // Aktif RESEARCH template'ini al
        PromptTemplate template = promptTemplateService.getActiveTemplate(PromptType.RESEARCH);

        // Template değişkenlerini doldur
        Map<String, String> variables = new HashMap<>();
        variables.put("topic", project.getTopic());
        variables.put("language", project.getLanguage());
        variables.put("title", project.getTitle());
        variables.put("niche", project.getNiche());

        String resolvedPrompt = promptTemplateService.resolveTemplate(
                template.getTemplateContent(), variables);

        // AI'ya gönder
        String aiResponse = aiService.generate(project, PromptType.RESEARCH, resolvedPrompt);

        // Versiyon belirle
        int nextVersion = researchNoteRepository.countByProjectId(projectId) + 1;

        // Kaydet
        ResearchNote note = ResearchNote.builder()
                .project(project)
                .content(aiResponse)
                .version(nextVersion)
                .build();

        ResearchNote saved = researchNoteRepository.save(note);
        log.info("Araştırma kaydedildi: noteId={}, version={}", saved.getId(), saved.getVersion());

        // Proje durumunu güncelle
        projectService.updateStatus(projectId, ProjectStatus.RESEARCHED);

        return toResponse(saved);
    }

    // Araştırmayı onayla veya düzelt
    @Transactional
    public ResearchResponse approveResearch(UUID projectId, UUID researchId, ApproveResearchRequest request) {
        projectService.findProjectById(projectId);
        ResearchNote note = researchNoteRepository.findById(researchId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchNote", "id", researchId));

        note.setApproved(request.getApproved());
        if (request.getUserNotes() != null) {
            note.setUserNotes(request.getUserNotes());
        }

        ResearchNote saved = researchNoteRepository.save(note);
        log.info("Araştırma onaylandı: noteId={}, approved={}", saved.getId(), saved.getApproved());
        return toResponse(saved);
    }

    // Projeye ait araştırmaları listele
    public List<ResearchResponse> listResearch(UUID projectId) {
        projectService.findProjectById(projectId);
        return researchNoteRepository.findByProjectIdOrderByVersionDesc(projectId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Tek araştırma detayı
    public ResearchResponse getResearch(UUID projectId, UUID researchId) {
        projectService.findProjectById(projectId);
        ResearchNote note = researchNoteRepository.findById(researchId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchNote", "id", researchId));
        return toResponse(note);
    }

    // Onaylı araştırmayı getir (senaryo üretimi için)
    public ResearchNote getApprovedResearch(UUID projectId) {
        return researchNoteRepository.findFirstByProjectIdAndApprovedTrue(projectId)
                .orElse(researchNoteRepository.findFirstByProjectIdOrderByVersionDesc(projectId));
    }

    private ResearchResponse toResponse(ResearchNote note) {
        return ResearchResponse.builder()
                .id(note.getId())
                .projectId(note.getProject().getId())
                .content(note.getContent())
                .summary(note.getSummary())
                .region(note.getRegion())
                .historicalPeriod(note.getHistoricalPeriod())
                .emotionalTone(note.getEmotionalTone())
                .uncertainties(note.getUncertainties())
                .approved(note.getApproved())
                .userNotes(note.getUserNotes())
                .version(note.getVersion())
                .createdAt(note.getCreatedAt())
                .build();
    }
}
