package com.anadoluatlasi.backend.knowledge.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-001: Aksiyon sonucu yanıt DTO'su.
 * mark-ready ve archive gibi durum değişikliği endpoint'lerinin yanıtı.
 */
@Value
@Builder
public class ActionStatusResponse {

    UUID id;
    String status;
    String message;
    LocalDateTime timestamp;
}
