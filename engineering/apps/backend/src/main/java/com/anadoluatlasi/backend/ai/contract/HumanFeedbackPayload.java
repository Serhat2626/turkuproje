package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * FP-001: İnsan editörün AI'a revizyon için verdiği geri bildirim payload'u.
 * Revizyon sürecinde AI'ın aynı içeriği tekrar üretmesini engellemek ve
 * insan yönlendirmesini context'e dahil etmek için kullanılır.
 */
@Value
@Builder
public class HumanFeedbackPayload {

    /** Geri bildirim referansı */
    String feedbackId;

    /** Revizyon türü (ör. MAKE_SHORTER, SIMPLIFY_LANGUAGE, CUSTOM) */
    String feedbackType;

    /** İnsan editör notu (zorunlu) */
    String note;

    /** Geri bildirimi veren kullanıcı referansı */
    String requestedBy;

    /** Geri bildirim zamanı */
    LocalDateTime createdAt;
}
