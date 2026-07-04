package com.anadoluatlasi.backend.imagegen.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FP-007: Generated asset reddetme isteği DTO'su.
 *
 * <p>Rejection reason zorunludur. Generic status update değildir;
 * yalnızca explicit reject action.</p>
 *
 * <p>Yasak alanlar: status, providerPayload, publishFlag.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectGeneratedAssetRequest {

    @NotBlank(message = "Red sebebi zorunludur")
    private String rejectionReason;
}
