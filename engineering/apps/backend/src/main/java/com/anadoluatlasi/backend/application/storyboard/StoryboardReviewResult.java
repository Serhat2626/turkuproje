package com.anadoluatlasi.backend.application.storyboard;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-003: Storyboard review operasyonlarının sonuç nesnesi.
 * Edit, approve, reject işlemlerinin ortak dönüş tipi.
 * FP-002 ScenarioReviewResult pattern'ı ile tutarlıdır.
 */
@Value
@Builder
public class StoryboardReviewResult {

    /** İşlem başarılı mı */
    boolean success;

    /** StoryboardDraft ID */
    UUID storyboardDraftId;

    /** Content Project ID */
    UUID contentProjectId;

    /** Güncel status */
    String status;

    /** Storyboard başlığı */
    String storyboardTitle;

    /** Sonuç mesajı */
    String message;
}
