package com.startup.turkuproje.data;

import com.startup.turkuproje.entity.PromptTemplate;
import com.startup.turkuproje.entity.PromptType;
import com.startup.turkuproje.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Uygulama başlangıcında varsayılan prompt şablonlarını yükler
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PromptTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        // Eğer hiç SCRIPT şablonu yoksa varsayılanı oluştur
        if (templateRepository.findByTypeAndActiveTrue(PromptType.SCRIPT).isEmpty()) {
            createDefaultScriptTemplate();
            log.info("Varsayılan SCRIPT prompt şablonu oluşturuldu");
        }

        // Eğer hiç RESEARCH şablonu yoksa varsayılanı oluştur
        if (templateRepository.findByTypeAndActiveTrue(PromptType.RESEARCH).isEmpty()) {
            createDefaultResearchTemplate();
            log.info("Varsayılan RESEARCH prompt şablonu oluşturuldu");
        }

        log.info("DataInitializer tamamlandı");
    }

    private void createDefaultScriptTemplate() {
        String content = """
                Sen bir YouTube içerik yazarısın. Aşağıdaki konu hakkında Türkçe, belgesel tarzında,
                duygusal ve kültürel hassasiyete uygun bir YouTube video senaryosu yaz.

                Konu: {{topic}}
                Dil: {{language}}
                Hedef süre: {{duration}} dakika
                Stil: Sinematik, duygusal, belgesel

                Senaryo şu bölümlerden oluşmalı:
                1. HOOK: İzleyicinin dikkatini çeken açılış cümlesi (15-20 saniye)
                2. GİRİŞ: Konuya giriş ve tarihsel/kültürel bağlam (30-45 saniye)
                3. HİKÂYE: Ana anlatım - olaylar, karakterler, mekanlar detaylı (3-6 dakika)
                4. DUYGUSAL ZİRVE: En çarpıcı/dokunaklı bölüm (1-2 dakika)
                5. KAPANIŞ: Öğrenilen ders, günümüze yansıması veya mesaj (30 saniye)
                6. CTA: Abone ol çağrısı ve sonraki video teaser (15 saniye)

                Araştırma bilgileri (varsa kullan):
                {{research}}

                Kullanıcı notları (varsa dikkat et):
                {{user_notes}}

                Kurallar:
                - Belgesel tonunda, abartısız ve saygılı bir dil kullan.
                - Tarihsel bilgileri doğru ver; emin olmadığın yerlerde "rivayete göre" gibi ifadeler kullan.
                - Kültürel hassasiyete dikkat et.
                - Her bölümü [HOOK], [GİRİŞ], [HİKÂYE], [DUYGUSAL_ZİRVE], [KAPANIŞ], [CTA] etiketleriyle ayır.
                - Seslendirme için uygun, akıcı ve dinlenmesi kolay bir metin yaz.
                """;

        PromptTemplate template = PromptTemplate.builder()
                .name("script_generation_v1")
                .type(PromptType.SCRIPT)
                .templateContent(content)
                .version(1)
                .active(true)
                .build();

        templateRepository.save(template);
    }

    private void createDefaultResearchTemplate() {
        String content = """
                Sen bir kültür tarihçisi ve araştırmacısın. Aşağıdaki konu hakkında detaylı bir araştırma özeti hazırla.

                Konu: {{topic}}
                Dil: {{language}}

                Şu bilgileri içermelidir:
                1. Konunun kısa özeti
                2. Yöresi / coğrafi bağlamı
                3. Bilinen varyantları (varsa)
                4. Tarihsel bağlam ve dönem
                5. Duygu tonu ve atmosfer
                6. Önemli kişiler veya karakterler
                7. Dikkat edilmesi gereken belirsizlikler (halüsinasyon riski olan bilgiler)

                Kurallar:
                - Emin olmadığın bilgileri "[BELİRSİZ]" etiketi ile işaretle.
                - Sadece güvenilir kaynaklara dayanan bilgiler ver.
                - Rivayetleri gerçeklerden ayır.
                - Akademik ama anlaşılır bir dil kullan.
                """;

        PromptTemplate template = PromptTemplate.builder()
                .name("research_v1")
                .type(PromptType.RESEARCH)
                .templateContent(content)
                .version(1)
                .active(true)
                .build();

        templateRepository.save(template);
    }
}
