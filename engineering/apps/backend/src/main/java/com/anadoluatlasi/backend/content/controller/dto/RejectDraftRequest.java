package com.anadoluatlasi.backend.content.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-001: Draft reddetme isteği DTO'su.
 * Red notu zorunludur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectDraftRequest {

    @NotBlank(message = "Red notu boş olamaz")
    private String reviewNote;
}
