package com.anadoluatlasi.backend.application.knowledge.create;

import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItem;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemStatus;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemType;
import com.anadoluatlasi.backend.knowledge.repository.KnowledgeItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yeni bir bilgi öğesi (KnowledgeItem) oluşturma iş akışı.
 *
 * <p>Sorumluluklar:</p>
 * <ul>
 *   <li>Validasyon çağrısı</li>
 *   <li>Entity oluşturma</li>
 *   <li>Repository kaydetme</li>
 *   <li>Result döndürme</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateKnowledgeItemUseCase {

    private final KnowledgeItemRepository knowledgeItemRepository;
    private final CreateKnowledgeItemValidator validator;

    /**
     * Yeni bir bilgi öğesi oluşturur.
     *
     * @param command oluşturma verileri
     * @return oluşturulan bilgi öğesinin sonuç bilgisi
     */
    @Transactional
    public CreateKnowledgeItemResult execute(CreateKnowledgeItemCommand command) {
        log.debug("Bilgi öğesi oluşturuluyor: {}", command.getTitle());

        // 1. İş kuralı doğrulaması
        validator.validate(command);

        // 2. Entity oluştur
        KnowledgeItem entity = KnowledgeItem.builder()
                .title(command.getTitle())
                .slug(CreateKnowledgeItemValidator.generateSlug(command.getTitle()))
                .type(KnowledgeItemType.valueOf(command.getType()))
                .summary(command.getSummary())
                .status(KnowledgeItemStatus.DRAFT)
                .language(command.getLanguage())
                .build();

        // 3. Kaydet
        KnowledgeItem saved = knowledgeItemRepository.save(entity);

        log.info("Bilgi öğesi oluşturuldu: id={}, title={}", saved.getId(), saved.getTitle());

        // 4. Result döndür (Entity dış dünyaya açılmaz)
        return CreateKnowledgeItemResult.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .slug(saved.getSlug())
                .type(saved.getType().name())
                .status(saved.getStatus().name())
                .language(saved.getLanguage())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
