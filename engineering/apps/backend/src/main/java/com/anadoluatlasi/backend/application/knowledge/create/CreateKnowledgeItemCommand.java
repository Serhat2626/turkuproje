package com.anadoluatlasi.backend.application.knowledge.create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/**
 * Yeni bir bilgi öğesi (KnowledgeItem) oluşturmak için gerekli giriş verisi.
 * İş kuralı içermez, yalnızca veri taşır.
 */
@Value
@Builder
public class CreateKnowledgeItemCommand {

    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir")
    String title;

    @NotBlank(message = "Tür boş olamaz")
    String type;

    @NotBlank(message = "Dil boş olamaz")
    @Size(max = 10, message = "Dil kodu en fazla 10 karakter olabilir")
    String language;

    String summary;
}
