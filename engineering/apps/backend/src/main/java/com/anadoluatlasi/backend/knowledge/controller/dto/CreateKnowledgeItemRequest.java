package com.anadoluatlasi.backend.knowledge.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bilgi öğesi oluşturma isteği DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeItemRequest {

    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir")
    private String title;

    @NotBlank(message = "Tür boş olamaz")
    private String type;

    @NotBlank(message = "Dil boş olamaz")
    @Size(max = 10, message = "Dil kodu en fazla 10 karakter olabilir")
    private String language;

    private String summary;
}
