package com.anadoluatlasi.backend.knowledge.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-001: Bilgi öğesi oluşturma isteği DTO'su.
 * Status alanı yoktur — yeni kayıtlar her zaman DRAFT olarak başlar.
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

    /** FP-001 */
    private String description;

    /** FP-001 */
    @Size(max = 100, message = "Kategori en fazla 100 karakter olabilir")
    private String category;

    /** FP-001 */
    @Size(max = 100, message = "İl en fazla 100 karakter olabilir")
    private String city;

    /** FP-001 */
    @Size(max = 100, message = "İlçe en fazla 100 karakter olabilir")
    private String district;

    /** FP-001 */
    @Size(max = 500, message = "Lokasyon metni en fazla 500 karakter olabilir")
    private String locationText;

    /** FP-001 */
    private String notes;
}
