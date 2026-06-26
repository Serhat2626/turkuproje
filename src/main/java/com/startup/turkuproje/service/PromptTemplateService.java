package com.startup.turkuproje.service;

import com.startup.turkuproje.dto.PromptTemplateDto;
import com.startup.turkuproje.entity.PromptTemplate;
import com.startup.turkuproje.entity.PromptType;
import com.startup.turkuproje.exception.ResourceNotFoundException;
import com.startup.turkuproje.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Prompt şablon yönetimi ve değişken injection
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTemplateService {

    private final PromptTemplateRepository templateRepository;

    // Yeni şablon oluştur
    @Transactional
    public PromptTemplateDto createTemplate(PromptTemplateDto dto) {
        // Aynı tipte aktif şablon varsa, onu pasife çek
        templateRepository.findByTypeAndActiveTrue(dto.getType())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    templateRepository.save(existing);
                    log.info("Eski şablon pasife alındı: id={}, type={}", existing.getId(), existing.getType());
                });

        PromptTemplate template = PromptTemplate.builder()
                .name(dto.getName())
                .type(dto.getType())
                .templateContent(dto.getTemplateContent())
                .version(dto.getVersion() != null ? dto.getVersion() : 1)
                .active(true)
                .build();

        PromptTemplate saved = templateRepository.save(template);
        log.info("Yeni prompt şablonu oluşturuldu: id={}, name={}, type={}", saved.getId(), saved.getName(), saved.getType());
        return toDto(saved);
    }

    // Tüm şablonları listele
    public List<PromptTemplateDto> listTemplates() {
        return templateRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Şablon güncelle
    @Transactional
    public PromptTemplateDto updateTemplate(UUID id, PromptTemplateDto dto) {
        PromptTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromptTemplate", "id", id));

        template.setName(dto.getName());
        template.setTemplateContent(dto.getTemplateContent());
        if (dto.getActive() != null) {
            template.setActive(dto.getActive());
        }

        PromptTemplate saved = templateRepository.save(template);
        return toDto(saved);
    }

    // Belirli tip için aktif şablonu getir
    public PromptTemplate getActiveTemplate(PromptType type) {
        return templateRepository.findByTypeAndActiveTrue(type)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PromptTemplate", "type", type + " (aktif şablon bulunamadı)"));
    }

    // Şablon içindeki değişkenleri değerlerle değiştir
    public String resolveTemplate(String templateContent, Map<String, String> variables) {
        String resolved = templateContent;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return resolved;
    }

    private PromptTemplateDto toDto(PromptTemplate template) {
        return PromptTemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .type(template.getType())
                .templateContent(template.getTemplateContent())
                .version(template.getVersion())
                .active(template.getActive())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
