# TürküProje - Proje Context & Kurallar

## Proje Özeti
AI destekli çok kanallı içerik üretim SaaS platformu. İlk niş: **Türkü Hikâyeleri / Anadolu Hikâyeleri**.
Kullanıcı bir konu girer (ör. "Çanakkale Türküsünün Hikâyesi"), sistem araştırma özeti, senaryo, storyboard, görsel/video promptları, seslendirme metni, thumbnail, SEO metinleri ve platform-spesifik içerikler üretir.

## Teknik Stack
- **Backend**: Java 21, Spring Boot 4.1.0, PostgreSQL, Redis, Docker
- **AI**: OpenAI API (veya alternatif LLM) - prompt template sistemi ile
- **Frontend**: React veya Next.js (sonraki faz)
- **Media**: FFmpeg, TTS API, Görsel/Video üretim API'leri (sonraki faz)
- **Build**: Maven, Lombok
- **Base Package**: `com.startup.turkuproje`

## Veritabanı Tabloları (MVP)
| Tablo | Açıklama |
|-------|----------|
| projects | Ana proje entity. Her içerik bir proje. |
| research_notes | Konu hakkında AI araştırma çıktısı |
| scripts | YouTube video senaryosu |
| scenes | Storyboard sahneleri |
| scene_prompts | Her sahne için görsel/video promptu |
| generated_assets | Üretilen medya dosyaları |
| music_assets | Müzik dosyaları + lisans bilgisi |
| voice_assets | Seslendirme dosyaları |
| publishing_packages | Yayın paketi (SEO, açıklama vs.) |
| platform_posts | Platform bazlı çıktılar |
| prompt_templates | Versiyonlanmış AI prompt şablonları |
| ai_generation_logs | AI çağrı logları |

## Project Entity Alanları
id, title, topic, niche, language, status, target_platforms, video_duration, created_at, updated_at

## Project Durumları (State Machine)
DRAFT → RESEARCHED → SCRIPT_GENERATED → STORYBOARD_GENERATED → MEDIA_PROMPTS_GENERATED → VOICE_READY → VIDEO_READY → PUBLISHED

## Ana Modüller
1. **Project Module**: Proje CRUD, durum yönetimi
2. **Research Module**: AI ile konu araştırması, halüsinasyon uyarısı
3. **Script Module**: Senaryo üretimi (Hook → Giriş → Hikâye → Duygusal Zirve → Kapanış → CTA)
4. **Storyboard Module**: Sahne bazlı bölme (scene_no, duration, narration, visual, mood, camera, transition, music)
5. **Prompt Module**: Sahne başına görsel/video prompt (TR açıklama, EN prompt, negative prompt, aspect ratio, stil)
6. **Media Generation Module**: Görsel/video/ses üretimi (sonraki faz)
7. **Video Assembly Module**: FFmpeg ile birleştirme (sonraki faz)
8. **Publishing Module**: Platform bazlı yayın paketi üretimi

## Prompt Akışı (Sıralı)
1. Research Prompt → 2. Script Prompt → 3. Storyboard Prompt → 4. Scene Visual Prompt → 5. Thumbnail Prompt → 6. SEO Prompt → 7. Shorts Prompt → 8. Instagram/Facebook Caption Prompt

Her prompt **versiyonlanmalıdır**.

## MVP Hedefi
- Konu girişi → Senaryo üretimi → Storyboard → Sahne promptları → SEO paketi
- Yarı otomatik: Kullanıcı çıktıları onaylar/düzenler
- Video üretimi ilk fazda manuel

## Telif Kuralları
- Hazır sanatçı kayıtları KULLANILMAZ
- Sadece: public domain, CC lisanslı, kullanıcı yüklediği veya özel yapım
- Her müzik dosyasında lisans bilgisi zorunlu

## Kodlama Kuralları
- Package yapısı: `com.startup.turkuproje.{module}.{layer}` (entity, repository, service, controller, dto)
- Lombok kullanılır (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
- Entity'lerde JPA auditing (@CreatedDate, @LastModifiedDate)
- DTO/Entity ayrımı yapılır
- REST API: `/api/v1/{resource}` formatı
- Türkçe yorumlar, İngilizce kod
- Exception handling: global @ControllerAdvice
