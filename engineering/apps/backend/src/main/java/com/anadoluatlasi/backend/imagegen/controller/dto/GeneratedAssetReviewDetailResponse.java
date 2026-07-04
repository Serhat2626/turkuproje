package com.anadoluatlasi.backend.imagegen.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * FP-007: Generated asset review detail aggregate DTO.
 *
 * <p>Admin review ekranı için gerekli tüm güvenli bilgileri
 * tek bir yanıtta döner: generated asset, generation request,
 * generation result, review geçmişi ve lineage özeti.</p>
 *
 * <p>Image preview için previewAvailable flag kullanılır;
 * signed/public URL bu DTO'da döndürülmez.
 * Preview serve backend proxy endpoint üzerinden yapılacaktır.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneratedAssetReviewDetailResponse {
    private ImageGeneratedAssetResponse generatedAsset;
    private ImageGenerationRequestResponse generationRequest;
    private ImageGenerationResultResponse generationResult;
    private List<GeneratedAssetReviewResponse> reviewHistory;

    // ── Lineage Summary ─────────────────────────────────────────────
    private UUID contentProjectId;
    private UUID visualAssetPlanId;
    private UUID visualAssetItemId;
}
