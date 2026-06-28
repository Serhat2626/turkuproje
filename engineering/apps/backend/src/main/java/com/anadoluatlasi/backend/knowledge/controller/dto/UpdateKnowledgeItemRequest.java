package com.anadoluatlasi.backend.knowledge.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-001: Bilgi öğesi güncelleme isteği DTO'su.
 * Status alanı yoktur — statü değişikliği sadece action endpoint'ler ile yapılır.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKnowledgeItemRequest {

    @NotBlank(message = "Başlık boş olamaz")
    @Size(max = 255, message = "Başlık en fazla 255 karakter olabilir")
    private String title;

    private String type;

    @Size(max = 10, message = "Dil kodu en fazla 10 karakter olabilir")
    private String language;

    private String summary;

    private String description;

    @Size(max = 100, message = "Kategori en fazla 100 karakter olabilir")
    private String category;

    @Size(max = 100, message = "İl en fazla 100 karakter olabilir")
    private String city;

    @Size(max = 100, message = "İlçe en fazla 100 karakter olabilir")
    private String district;

    @Size(max = 500, message = "Lokasyon metni en fazla 500 karakter olabilir")
    private String locationText;

    private String notes;
}
