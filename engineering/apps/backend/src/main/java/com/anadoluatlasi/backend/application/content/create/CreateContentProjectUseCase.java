package com.anadoluatlasi.backend.application.content.create;

import com.anadoluatlasi.backend.content.entity.ContentProject;
import com.anadoluatlasi.backend.content.entity.ContentProjectStatus;
import com.anadoluatlasi.backend.content.entity.ContentType;
import com.anadoluatlasi.backend.content.entity.TargetPlatform;
import com.anadoluatlasi.backend.content.repository.ContentProjectRepository;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.knowledge.repository.KnowledgeItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yeni bir içerik projesi (ContentProject) oluşturma iş akışı.
 *
 * <p>Sorumluluklar:</p>
 * <ul>
 *   <li>Validasyon çağrısı</li>
 *   <li>KnowledgeItem referansı yükleme</li>
 *   <li>Entity oluşturma</li>
 *   <li>Repository kaydetme</li>
 *   <li>Result döndürme</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateContentProjectUseCase {

    private final ContentProjectRepository contentProjectRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final CreateContentProjectValidator validator;

    /**
     * Yeni bir içerik projesi oluşturur.
     *
     * @param command oluşturma verileri
     * @return oluşturulan içerik projesinin sonuç bilgisi
     */
    @Transactional
    public CreateContentProjectResult execute(CreateContentProjectCommand command) {
        log.debug("İçerik projesi oluşturuluyor: {}", command.getTitle());

        // 1. İş kuralı doğrulaması
        validator.validate(command);

        // 2. KnowledgeItem referansını yükle
        KnowledgeItem knowledgeItem = knowledgeItemRepository
                .getReferenceById(command.getKnowledgeItemId());

        // 3. Entity oluştur
        ContentProject entity = ContentProject.builder()
                .knowledgeItem(knowledgeItem)
                .title(command.getTitle())
                .slug(CreateContentProjectValidator.generateSlug(command.getTitle()))
                .contentType(ContentType.valueOf(command.getContentType()))
                .targetPlatform(TargetPlatform.valueOf(command.getTargetPlatform()))
                .status(ContentProjectStatus.CREATED)
                .language(command.getLanguage())
                .build();

        // 4. Kaydet
        ContentProject saved = contentProjectRepository.save(entity);

        log.info("İçerik projesi oluşturuldu: id={}, title={}", saved.getId(), saved.getTitle());

        // 5. Result döndür (Entity dış dünyaya açılmaz)
        return CreateContentProjectResult.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .slug(saved.getSlug())
                .contentType(saved.getContentType().name())
                .targetPlatform(saved.getTargetPlatform().name())
                .status(saved.getStatus().name())
                .language(saved.getLanguage())
                .knowledgeItemId(command.getKnowledgeItemId())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
