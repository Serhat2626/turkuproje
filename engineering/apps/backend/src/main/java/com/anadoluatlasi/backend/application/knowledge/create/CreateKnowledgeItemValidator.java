package com.anadoluatlasi.backend.application.knowledge.create;

import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.knowledge.entity.KnowledgeItemType;
import com.anadoluatlasi.backend.knowledge.repository.KnowledgeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bilgi öğesi oluşturma iş kuralı doğrulayıcısı.
 * Teknik validasyon (Bean Validation) dışında kalan iş kurallarını kontrol eder.
 */
@Component
@RequiredArgsConstructor
public class CreateKnowledgeItemValidator {

    private final KnowledgeItemRepository knowledgeItemRepository;

    /**
     * Command'ı iş kurallarına göre doğrular.
     *
     * @param command oluşturulacak bilgi öğesinin verileri
     * @throws ValidationException iş kuralı ihlali varsa
     */
    public void validate(CreateKnowledgeItemCommand command) {
        // Tür geçerli mi?
        validateType(command.getType());

        // Aynı başlıktan slug üretildiğinde çakışma var mı?
        String slug = generateSlug(command.getTitle());
        if (knowledgeItemRepository.findBySlug(slug).isPresent()) {
            throw new ValidationException("Bu başlığa sahip bir bilgi öğesi zaten mevcut: " + command.getTitle());
        }
    }

    /**
     * Verilen tür değerinin geçerli bir KnowledgeItemType olup olmadığını kontrol eder.
     */
    private void validateType(String type) {
        try {
            KnowledgeItemType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Geçersiz bilgi öğesi türü: " + type);
        }
    }

    /**
     * Başlıktan URL-uyumlu slug üretir.
     * Türkçe karakterler dönüştürülür, boşluklar tire ile değiştirilir.
     */
    static String generateSlug(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        return title.toLowerCase()
                .replace("ç", "c").replace("ğ", "g").replace("ı", "i")
                .replace("ö", "o").replace("ş", "s").replace("ü", "u")
                .replace("Ç", "c").replace("Ğ", "g").replace("İ", "i")
                .replace("Ö", "o").replace("Ş", "s").replace("Ü", "u")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
