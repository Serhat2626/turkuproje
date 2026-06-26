package com.startup.turkuproje.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Yeni proje oluşturma isteği
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    // İçerik konusu (zorunlu)
    @NotBlank(message = "Konu (topic) boş olamaz")
    private String topic;

    // Proje başlığı (opsiyonel - boşsa topic'ten türetilir)
    private String title;

    // İçerik nişi (varsayılan: turku_hikayeleri)
    @Builder.Default
    private String niche = "turku_hikayeleri";

    // İçerik dili (varsayılan: tr)
    @Builder.Default
    private String language = "tr";

    // Hedef platformlar (varsayılan: youtube)
    @Builder.Default
    private String targetPlatforms = "youtube";

    // Hedef video süresi dakika (varsayılan: 8)
    @Builder.Default
    private Integer videoDuration = 8;
}
