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
 * FP-001: İçerik projesi oluşturma isteği DTO'su.
 * Status alanı yoktur — yeni projeler her zaman CREATED olarak başlar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContentProjectRequest {

    @NotNull(message = "Bilgi öğesi ID boş olamaz")
    private UUID knowledgeItemId;

    /** İçerik üretim amacı (ContentPurpose enum adı) */
    private String contentPurpose;

    /** Hedef kitle (TargetAudience enum adı) */
    private String targetAudience;

    /** Yazım tonu (ContentTone enum adı) */
    private String tone;

    @NotBlank(message = "Dil boş olamaz")
    @Size(max = 10, message = "Dil kodu en fazla 10 karakter olabilir")
    private String language;

    /** Format ipucu */
    private String formatHint;

    /** Uzunluk ipucu */
    private String lengthHint;
}
