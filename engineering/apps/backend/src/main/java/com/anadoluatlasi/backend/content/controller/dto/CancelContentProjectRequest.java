package com.anadoluatlasi.backend.content.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-001: Proje iptal isteği DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelContentProjectRequest {

    /** Opsiyonel iptal notu */
    private String note;
}
