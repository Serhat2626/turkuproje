package com.anadoluatlasi.backend.imagegen.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-007: Generated asset onaylama isteği DTO'su.
 *
 * <p>Yasak alanlar: status, providerPayload, publishFlag, readyForPublish.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveGeneratedAssetRequest {

    /** Opsiyonel onay notu */
    private String reviewNote;
}
