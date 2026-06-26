package com.startup.turkuproje.service;

import com.startup.turkuproje.dto.ScriptResponse;
import com.startup.turkuproje.entity.*;
import com.startup.turkuproje.exception.ResourceNotFoundException;
import com.startup.turkuproje.repository.ScriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Senaryo üretim servisi - ana iş mantığı burada
@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptGenerationService {

    private final ProjectService projectService;
    private final PromptTemplateService promptTemplateService;
    private final AiService aiService;
    private final ScriptRepository scriptRepository;

    /**
     * Proje için AI ile senaryo üret.
     * 1. Projeyi yükle
     * 2. Aktif SCRIPT prompt template'ini al
     * 3. Template'e proje bilgilerini inject et
     * 4. AI'ya gönder
     * 5. Yanıtı parse et
     * 6. Script entity olarak kaydet
     * 7. Proje durumunu SCRIPT_GENERATED yap
     */
    @Transactional
    public ScriptResponse generateScript(UUID projectId) {
        // 1. Projeyi yükle
        Project project = projectService.findProjectById(projectId);
        log.info("Senaryo üretimi başlatıldı: projectId={}, topic={}", projectId, project.getTopic());

        // 2. Aktif SCRIPT template'ini al
        PromptTemplate template = promptTemplateService.getActiveTemplate(PromptType.SCRIPT);

        // 3. Template değişkenlerini doldur
        Map<String, String> variables = new HashMap<>();
        variables.put("topic", project.getTopic());
        variables.put("language", project.getLanguage());
        variables.put("duration", String.valueOf(project.getVideoDuration()));
        variables.put("title", project.getTitle());
        variables.put("niche", project.getNiche());

        String resolvedPrompt = promptTemplateService.resolveTemplate(
                template.getTemplateContent(), variables);

        // 4. AI'ya gönder
        String aiResponse = aiService.generate(project, PromptType.SCRIPT, resolvedPrompt);

        // 5. Yanıtı parse et
        ScriptSections sections = parseScriptSections(aiResponse);

        // 6. Versiyon numarasını belirle
        int nextVersion = scriptRepository.countByProjectId(projectId) + 1;

        // 7. Script entity olarak kaydet
        Script script = Script.builder()
                .project(project)
                .content(aiResponse)
                .hook(sections.hook)
                .intro(sections.intro)
                .story(sections.story)
                .climax(sections.climax)
                .closing(sections.closing)
                .callToAction(sections.callToAction)
                .version(nextVersion)
                .build();

        Script saved = scriptRepository.save(script);
        log.info("Senaryo kaydedildi: scriptId={}, version={}", saved.getId(), saved.getVersion());

        // 8. Proje durumunu güncelle
        projectService.updateStatus(projectId, ProjectStatus.SCRIPT_GENERATED);

        return toResponse(saved);
    }

    // Projeye ait senaryoları listele
    public List<ScriptResponse> listScripts(UUID projectId) {
        // Projenin varlığını kontrol et
        projectService.findProjectById(projectId);

        return scriptRepository.findByProjectIdOrderByVersionDesc(projectId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Tek senaryo detayı
    public ScriptResponse getScript(UUID projectId, UUID scriptId) {
        projectService.findProjectById(projectId);
        Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Script", "id", scriptId));
        return toResponse(script);
    }

    /**
     * AI yanıtını bölümlere ayır.
     * Etiketler: [HOOK], [GİRİŞ], [HİKÂYE], [DUYGUSAL_ZİRVE], [KAPANIŞ], [CTA]
     */
    private ScriptSections parseScriptSections(String content) {
        ScriptSections sections = new ScriptSections();

        sections.hook = extractSection(content, "[HOOK]", "[GİRİŞ]");
        sections.intro = extractSection(content, "[GİRİŞ]", "[HİKÂYE]");
        sections.story = extractSection(content, "[HİKÂYE]", "[DUYGUSAL_ZİRVE]");
        sections.climax = extractSection(content, "[DUYGUSAL_ZİRVE]", "[KAPANIŞ]");
        sections.closing = extractSection(content, "[KAPANIŞ]", "[CTA]");
        sections.callToAction = extractSection(content, "[CTA]", null);

        return sections;
    }

    // İki etiket arası metni çıkar
    private String extractSection(String content, String startTag, String endTag) {
        try {
            int startIndex = content.indexOf(startTag);
            if (startIndex == -1) return null;

            startIndex += startTag.length();

            int endIndex;
            if (endTag != null) {
                endIndex = content.indexOf(endTag, startIndex);
                if (endIndex == -1) endIndex = content.length();
            } else {
                endIndex = content.length();
            }

            return content.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            log.warn("Bölüm parse edilemedi: startTag={}, hata={}", startTag, e.getMessage());
            return null;
        }
    }

    private ScriptResponse toResponse(Script script) {
        return ScriptResponse.builder()
                .id(script.getId())
                .projectId(script.getProject().getId())
                .content(script.getContent())
                .hook(script.getHook())
                .intro(script.getIntro())
                .story(script.getStory())
                .climax(script.getClimax())
                .closing(script.getClosing())
                .callToAction(script.getCallToAction())
                .version(script.getVersion())
                .createdAt(script.getCreatedAt())
                .build();
    }

    // Yardımcı iç sınıf - parse edilen senaryo bölümleri
    private static class ScriptSections {
        String hook;
        String intro;
        String story;
        String climax;
        String closing;
        String callToAction;
    }
}
