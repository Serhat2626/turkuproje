package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FP-004: Platform paketi üretim agent'ından dönen yanıt contract'ı.
 *
 * <p>Bu contract dahili AI/application sınırında kullanılır.
 * REST katmanına expose edilmez.</p>
 *
 * <p>Raw provider payload, raw prompt ve API key/secret asla dahil edilmez.</p>
 *
 * <p>success=true ise packages alanı dolu, success=false ise hata alanları dolu.</p>
 */
@Value
@Builder
public class PlatformPackageAgentResponse {

    /** İşlem başarılı mı */
    boolean success;

    /** Platform paketleri */
    List<PlatformPackageOutput> packages;

    /** AI uyarıları */
    List<String> warnings;

    /** Safe provider metadata: provider tipi */
    String providerType;

    /** Safe provider metadata: model adı */
    String modelName;

    /** Hata kodu (başarısızsa) */
    String errorCode;

    /** Sanitized hata mesajı (başarısızsa) */
    String errorMessage;

    /** Retry yapılabilir mi */
    Boolean retryable;

    /** Üretim zamanı */
    LocalDateTime generatedAt;
}
