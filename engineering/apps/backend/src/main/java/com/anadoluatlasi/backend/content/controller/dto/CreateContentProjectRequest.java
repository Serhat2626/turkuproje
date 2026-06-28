package com.anadoluatlasi.backend.content.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * İçerik projesi oluşturma isteği DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContentProjectRequest {

    @NotNull(message = "Bilgi öğesi ID boş olamaz")
    private UUID knowledgeItemId;

    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir")
    private String title;

    @NotBlank(message = "İçerik türü boş olamaz")
    private String contentType;

    @NotBlank(message = "Hedef platform boş olamaz")
    private String targetPlatform;

    @NotBlank(message = "Dil boş olamaz")
    @Size(max = 10, message = "Dil kodu en fazla 10 karakter olabilir")
    private String language;
}
