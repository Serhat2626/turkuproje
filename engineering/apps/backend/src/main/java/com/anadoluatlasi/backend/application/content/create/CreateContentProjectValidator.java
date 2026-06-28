package com.anadoluatlasi.backend.application.content.create;

import com.anadoluatlasi.backend.common.exception.NotFoundException;
import com.anadoluatlasi.backend.common.exception.ValidationException;
import com.anadoluatlasi.backend.content.entity.ContentType;
import com.anadoluatlasi.backend.content.entity.TargetPlatform;
import com.anadoluatlasi.backend.content.repository.ContentProjectRepository;
import com.anadoluatlasi.backend.knowledge.repository.KnowledgeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * İçerik projesi oluşturma iş kuralı doğrulayıcısı.
 * Teknik validasyon dışında kalan iş kurallarını kontrol eder.
 */
@Component
@RequiredArgsConstructor
public class CreateContentProjectValidator {

    private final KnowledgeItemRepository knowledgeItemRepository;
    private final ContentProjectRepository contentProjectRepository;

    /**
     * Command'ı iş kurallarına göre doğrular.
     *
     * @param command oluşturulacak içerik projesinin verileri
     * @throws ValidationException iş kuralı ihlali varsa
     * @throws NotFoundException bilgi öğesi bulunamazsa
     */
    public void validate(CreateContentProjectCommand command) {
        // İçerik türü geçerli mi?
        validateContentType(command.getContentType());

        // Hedef platform geçerli mi?
        validateTargetPlatform(command.getTargetPlatform());

        // Bilgi öğesi mevcut mu?
        if (!knowledgeItemRepository.existsById(command.getKnowledgeItemId())) {
            throw new NotFoundException("KnowledgeItem", command.getKnowledgeItemId());
        }

        // Aynı slug var mı?
        String slug = generateSlug(command.getTitle());
        if (contentProjectRepository.findBySlug(slug).isPresent()) {
            throw new ValidationException("Bu başlığa sahip bir içerik projesi zaten mevcut: " + command.getTitle());
        }
    }

    private void validateContentType(String contentType) {
        try {
            ContentType.valueOf(contentType);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Geçersiz içerik türü: " + contentType);
        }
    }

    private void validateTargetPlatform(String targetPlatform) {
        try {
            TargetPlatform.valueOf(targetPlatform);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Geçersiz hedef platform: " + targetPlatform);
        }
    }

    /**
     * Başlıktan URL-uyumlu slug üretir.
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
