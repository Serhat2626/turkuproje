package com.anadoluatlasi.backend.content.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-001: Revizyon talebi isteği DTO'su.
 * Review notu zorunludur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestRevisionRequest {

    @NotBlank(message = "Revizyon notu boş olamaz")
    private String reviewNote;
}
