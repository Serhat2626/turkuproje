package com.anadoluatlasi.backend.application.content;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * FP-001: Yeni bir içerik projesi oluşturmak için gerekli giriş verisi.
 * Status alanı yoktur — yeni projeler her zaman CREATED olarak başlar.
 */
@Value
@Builder
public class CreateContentProjectCommand {

    /** İlişkili bilgi öğesi ID (zorunlu) */
    UUID knowledgeItemId;

    /** İçerik üretim amacı (ContentPurpose enum adı) */
    String contentPurpose;

    /** Hedef kitle (TargetAudience enum adı) */
    String targetAudience;

    /** Yazım tonu (ContentTone enum adı) */
    String tone;

    /** Dil kodu (zorunlu, ör. "tr") */
    String language;

    /** Format ipucu (opsiyonel, ör. "madde listesi") */
    String formatHint;

    /** Uzunluk ipucu (opsiyonel, ör. "300 kelime") */
    String lengthHint;
}
