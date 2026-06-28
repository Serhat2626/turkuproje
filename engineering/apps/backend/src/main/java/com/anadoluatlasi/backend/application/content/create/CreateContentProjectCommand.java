package com.anadoluatlasi.backend.application.content.create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Yeni bir içerik projesi (ContentProject) oluşturmak için gerekli giriş verisi.
 * İş kuralı içermez, yalnızca veri taşır.
 */
@Value
@Builder
public class CreateContentProjectCommand {

    @NotNull(message = "Bilgi öğesi ID boş olamaz")
    UUID knowledgeItemId;

    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir")
    String title;

    @NotBlank(message = "İçerik türü boş olamaz")
    String contentType;

    @NotBlank(message = "Hedef platform boş olamaz")
    String targetPlatform;

    @NotBlank(message = "Dil boş olamaz")
    @Size(max = 10, message = "Dil kodu en fazla 10 karakter olabilir")
    String language;
}
