package com.anadoluatlasi.backend.application.visualasset;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-006: VisualAssetPlan review operasyonlarının sonuç nesnesi.
 * Edit, approve, reject işlemlerinin ortak dönüş tipi.
 * FP-003 StoryboardReviewResult pattern'ı ile tutarlıdır.
 */
@Value
@Builder
public class VisualAssetPlanReviewResult {

    /** İşlem başarılı mı */
    boolean success;

    /** VisualAssetPlan ID */
    UUID visualAssetPlanId;

    /** Content Project ID */
    UUID contentProjectId;

    /** Kaynak StoryboardDraft ID */
    UUID storyboardDraftId;

    /** Güncel status */
    String status;

    /** Plan başlığı */
    String planTitle;

    /** Sonuç mesajı */
    String message;
}
