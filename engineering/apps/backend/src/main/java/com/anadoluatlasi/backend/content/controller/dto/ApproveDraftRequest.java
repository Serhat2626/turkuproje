package com.anadoluatlasi.backend.content.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-001: Draft onaylama isteği DTO'su.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveDraftRequest {

    /** Opsiyonel onay notu */
    private String reviewNote;
}
