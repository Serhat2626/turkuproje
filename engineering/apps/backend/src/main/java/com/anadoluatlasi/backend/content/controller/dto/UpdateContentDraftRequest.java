package com.anadoluatlasi.backend.content.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-001: Draft güncelleme isteği DTO'su.
 * Status alanı yoktur — düzenleme sonrası draft otomatik EDITED olur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContentDraftRequest {

    /** Yeni başlık */
    private String title;

    /** Yeni içerik metni */
    private String body;

    /** Yeni özet */
    private String summary;
}
