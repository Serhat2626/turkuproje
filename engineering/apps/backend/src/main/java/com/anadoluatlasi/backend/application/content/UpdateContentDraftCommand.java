package com.anadoluatlasi.backend.application.content;

import lombok.Builder;
import lombok.Value;

/**
 * FP-001: Draft güncelleme komutu.
 * Null alanlar güncellenmez (partial update).
 */
@Value
@Builder
public class UpdateContentDraftCommand {

    /** Yeni başlık (null ise değiştirilmez) */
    String title;

    /** Yeni içerik metni (null ise değiştirilmez) */
    String body;

    /** Yeni özet (null ise değiştirilmez) */
    String summary;
}
