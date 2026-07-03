# 45 — FP-004 Multi-Platform Content Packaging — AI Platform Contract Design

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-004 — Multi-Platform Content Packaging
>
> **Prerequisites**: FP-001 (CLOSED), FP-002 (CLOSED), FP-003 (CLOSED)
>
> **Product Design**: [44-fp004-multi-platform-content-packaging-product-design.md](./44-fp004-multi-platform-content-packaging-product-design.md)

---

## 1. Status

**Design / Planning**

Bu doküman FP-004'ün AI platform contract tasarımıdır.
İmplementasyon henüz başlamadı. Java sınıfları, gateway'ler, provider'lar, promptlar,
REST endpoint'ler ve frontend component'ler oluşturulmamıştır.

---

## 2. Purpose

Bu doküman, onaylanmış StoryboardDraft'tan platform-spesifik içerik paketleri üretmek
için kullanılacak dahili AI agent contract'larını tanımlar.

**Kapsam:**

- Application → AI Gateway arasındaki istek/yanıt sözleşmesi
- Platform paket üretimi için kaynak veri yapısı
- AI çıktı formatı ve doğrulama kuralları
- Hata modeli ve güvenlik kısıtlamaları

**Açıklamalar:**

| # | Kural |
|---|-------|
| 1 | Bu contract'lar **dahili application/AI-platform contract'larıdır** |
| 2 | REST DTO'ları **değildir** — admin API üzerinden doğrudan expose edilmezler |
| 3 | Secret, API key veya raw provider payload **içermezler** |
| 4 | Prompt implementasyonu bu dokümanın kapsamı dışındadır |
| 5 | FP-002 `ScenarioAgentRequest/Response` ve FP-003 `StoryboardAgentRequest/Response` pattern'ı ile tutarlıdır |

---

## 3. Source Prerequisite

### Zorunlu Kaynak

| Alan | Değer |
|------|-------|
| Entity | `StoryboardDraft` |
| Status | **APPROVED** |
| Validation | Application service tarafından yapılır |

### Prerequisite Kuralları

| # | Kural |
|---|-------|
| 1 | Yalnızca `APPROVED` StoryboardDraft geçerli kaynaktır |
| 2 | `AI_GENERATED`, `NEEDS_REVIEW`, `REJECTED`, `FAILED` storyboard'lar kaynak olarak reddedilir |
| 3 | Prerequisite doğrulaması application service katmanında yapılır — gateway'e gelinmeden |
| 4 | Gateway, kaynak status doğrulaması yapmaz — bu sorumluluk service'e aittir |
| 5 | Gateway çağrılmadan önce StoryboardDraft'ın tüm scene'leri yüklenmiş olmalıdır |

---

## 4. Package Generation Mode

### Seçilen Strateji: Tek İstek — Çoklu Platform Çıktısı

**Karar:** İlk implementasyonda tek bir AI isteği ile tüm seçili platformlara ait
paketler birlikte üretilir.

**Gerekçe:**

| Avantaj | Açıklama |
|---------|----------|
| Maliyet | Tek API çağrısı, daha düşük token maliyeti |
| Tutarlılık | Tüm platform paketleri aynı context'te üretilir — mesaj tutarlılığı |
| Basitlik | Tek istek/yanıt döngüsü, basit hata yönetimi |
| Latency | Paralel çağrı yönetimi gereksiz |

**Varsayılan Hedef Platformlar:**

| Platform | Enum |
|----------|------|
| YouTube Long-form | `YOUTUBE_LONG_FORM` |
| YouTube Shorts | `YOUTUBE_SHORTS` |
| Instagram Reels | `INSTAGRAM_REELS` |
| Facebook Reels | `FACEBOOK_REELS` |

**Gelecek Esneklik:**

- Admin istek sırasında platform alt kümesi seçebilir (ör. sadece YouTube)
- İlk implementasyonda varsayılan olarak 4 platform birden üretilir
- Her platform paketi üretim sonrası bağımsız olarak review/approve/reject edilir

---

## 5. Contract DTO Proposals

### 5.1. Enum'lar

#### PlatformPackageType

```
YOUTUBE_LONG_FORM
YOUTUBE_SHORTS
INSTAGRAM_REELS
FACEBOOK_REELS
```

#### PlatformPackageStatus

```
AI_GENERATED
NEEDS_REVIEW
APPROVED
REJECTED
FAILED
```

> FP-002/FP-003 ile tutarlı. `READY_FOR_PUBLISH` eklenmez.

### 5.2. Request DTO'ları

| DTO | Açıklama |
|-----|----------|
| `PlatformPackageAgentRequest` | Ana istek — tüm platform paketleri için tek istek |
| `PlatformPackageTargetInput` | Hedef platform listesi ve platform-spesifik konfigürasyon |
| `PlatformPackageSourceStoryboardInput` | Kaynak storyboard özet bilgileri |
| `PlatformPackageSourceSceneInput` | Kaynak storyboard sahneleri |
| `PlatformPackageKnowledgeContextInput` | İçerik/bilgi context'i (konu, niş, özet) |

### 5.3. Response DTO'ları

| DTO | Açıklama |
|-----|----------|
| `PlatformPackageAgentResponse` | Ana yanıt — tüm platform paketleri |
| `PlatformPackageOutput` | Tek bir platform paketi çıktısı |
| `PlatformPackageMetadataOutput` | Platform-spesifik metadata (chapters, overlay text, vb.) |
| `PlatformPackageSceneUsageOutput` | Sahne kullanım rehberi çıktısı |
| `PlatformPackageValidationIssueOutput` | Doğrulama uyarıları (opsiyonel) |

### 5.4. İsimlendirme Notu

Entity/class isimlendirme ve Java package path'leri engineering design task'ında
sabitlenecektir. Bu doküman sadece kavramsal DTO yapılarını tanımlar.

---

## 6. Request Contract Fields

### 6.1. PlatformPackageAgentRequest (Ana İstek)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `requestId` | String (UUID) | ✅ | İstek izleme ID'si |
| `contentProjectId` | String (UUID) | ✅ | İçerik projesi ID |
| `sourceStoryboardDraftId` | String (UUID) | ✅ | Kaynak storyboard ID |
| `sourceContentDraftId` | String (UUID) | ✅ | Kaynak content draft ID (izlenebilirlik) |
| `sourceScenarioDraftId` | String (UUID) | ✅ | Kaynak senaryo ID (izlenebilirlik) |
| `requestedByAdminId` | String | ✅ | İstekte bulunan admin |
| `requestedAt` | String (ISO-8601) | ✅ | İstek zamanı |
| `locale` | String | ✅ | İçerik dili (ör. "tr", "en") |
| `toneGuidance` | String | Opsiyonel | Ton/stil rehberi (ör. "eğitici", "duygusal") |
| `targetPlatforms` | List | ✅ | Hedef platform listesi |
| `regenerationContext` | String | Opsiyonel | Yeniden üretim durumunda önceki review notları |
| `storyboard` | Object | ✅ | Kaynak storyboard verisi |
| `knowledgeContext` | Object | Opsiyonel | İçerik/bilgi context'i |

### 6.2. PlatformPackageTargetInput (targetPlatforms içinde)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `platformType` | String (enum) | ✅ | YOUTUBE_LONG_FORM / YOUTUBE_SHORTS / INSTAGRAM_REELS / FACEBOOK_REELS |

### 6.3. PlatformPackageSourceStoryboardInput (storyboard)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `title` | String | ✅ | Storyboard başlığı |
| `summary` | String | ✅ | Storyboard özeti |
| `visualStyleGuide` | String | Opsiyonel | Görsel stil rehberi |
| `narrationStyle` | String | Opsiyonel | Anlatım stili |
| `targetDurationSeconds` | Integer | Opsiyonel | Toplam hedef süre |
| `aspectRatio` | String | Opsiyonel | Aspect ratio (ör. "16:9") |
| `approvedAt` | String (ISO-8601) | Opsiyonel | Onay zamanı (audit trail) |
| `approvedBy` | String | Opsiyonel | Onaylayan admin |
| `scenes` | List | ✅ | Storyboard sahneleri |

### 6.4. PlatformPackageSourceSceneInput (scenes içinde)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `sceneNumber` | Integer | ✅ | Sahne sırası |
| `sourceBeatType` | String | ✅ | Kaynak beat tipi (HOOK, INTRODUCTION, vb.) |
| `sourceBeatOrder` | Integer | ✅ | Beat sırası |
| `sceneTitle` | String | ✅ | Sahne başlığı |
| `visualDescription` | String | ✅ | Görsel açıklama |
| `narrationText` | String | ✅ | Narration metni |
| `onScreenText` | String | Opsiyonel | Ekran üzeri metin |
| `cameraDirection` | String | Opsiyonel | Kamera yönü |
| `transitionNote` | String | Opsiyonel | Geçiş notu |
| `mood` | String | Opsiyonel | Sahne havası |
| `location` | String | Opsiyonel | Mekan |
| `characters` | String | Opsiyonel | Karakterler |
| `estimatedDurationSeconds` | Integer | ✅ | Tahmini süre |

### 6.5. PlatformPackageKnowledgeContextInput (knowledgeContext)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `topicTitle` | String | ✅ | Konu başlığı |
| `topicSummary` | String | Opsiyonel | Konu özeti |
| `niche` | String | ✅ | Niş (ör. "Anadolu Hikayeleri") |
| `cityOrRegion` | String | Opsiyonel | Şehir/bölge bilgisi |
| `contentDraftTitle` | String | Opsiyonel | Content draft başlığı |
| `contentDraftSummary` | String | Opsiyonel | Content draft özeti (tam body değil) |
| `scenarioTitle` | String | Opsiyonel | Senaryo başlığı |
| `scenarioSummary` | String | Opsiyonel | Senaryo özeti |

### 6.6. İstek'te Bulunmaması Gereken Alanlar

| Hariç Tutulan | Gerekçe |
|---------------|---------|
| Raw prompt text | Güvenlik |
| Raw provider payload | Güvenlik |
| API key / secret | Güvenlik |
| Database-only ID'ler (surrogate keys) | AI için gereksiz |
| Full content draft body | Prompt boyutu, güvenlik |
| Full scenario body | Prompt boyutu, güvenlik |
| Provider-specific config | Abstraction ihlali |

---

## 7. Response Contract Fields

### 7.1. PlatformPackageAgentResponse (Ana Yanıt)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `success` | Boolean | ✅ | Üretim başarılı mı |
| `providerType` | String | ✅ | Provider tipi (ör. "OPENAI", "MOCK") |
| `modelName` | String | ✅ | Kullanılan model (ör. "gpt-4o-mini") |
| `generatedAt` | String (ISO-8601) | ✅ | Üretim zamanı |
| `packages` | List | ✅ (success=true) | Platform paketleri |
| `errorCode` | String | ✅ (success=false) | Hata kodu |
| `errorMessage` | String | ✅ (success=false) | Hata mesajı |
| `retryable` | Boolean | ✅ (success=false) | Yeniden denenebilir mi |

### 7.2. PlatformPackageOutput (packages içinde)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `packageType` | String (enum) | ✅ | YOUTUBE_LONG_FORM / YOUTUBE_SHORTS / INSTAGRAM_REELS / FACEBOOK_REELS |
| `title` | String | ✅ | Paket başlığı (platform-optimize) |
| `captionOrDescription` | String | ✅ | Platform açıklaması / caption |
| `hashtags` | List\<String\> | ✅ | Hashtag listesi (# işareti olmadan) |
| `callToAction` | String | ✅ | CTA metni |
| `targetDurationSeconds` | Integer | ✅ | Hedef süre (saniye) |
| `aspectRatio` | String | ✅ | Aspect ratio (ör. "16:9", "9:16") |
| `contentStructureNotes` | String | Opsiyonel | İçerik yapısı notları |
| `sceneUsageNotes` | String | Opsiyonel | Sahne kullanım rehberi (metin) |
| `reviewNotes` | String | Opsiyonel | AI'ın review önerisi |
| `sceneUsages` | List | Opsiyonel | Sahne bazlı kullanım detayı |
| `platformSpecificMetadata` | Object | ✅ | Platform-spesifik metadata |

### 7.3. PlatformPackageMetadataOutput (platformSpecificMetadata)

#### YouTube Long-Form Metadata

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `videoTitle` | String | ✅ | SEO-optimize video başlığı (≤100 karakter) |
| `longDescription` | String | ✅ | Detaylı video açıklaması (≤5000 karakter) |
| `chapterOutline` | List\<String\> | Opsiyonel | YouTube chapters (ör. "00:00 Giriş") |
| `pinnedCommentSuggestion` | String | Opsiyonel | Sabitlenecek yorum önerisi |
| `thumbnailTextSuggestion` | String | Opsiyonel | Thumbnail metin önerisi (≤6 kelime) |
| `tags` | List\<String\> | ✅ | Video etiketleri |

#### YouTube Shorts Metadata

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `shortHook` | String | ✅ | İlk 3 saniye hook metni |
| `shortCaption` | String | ✅ | Kısa açıklama |
| `overlayTextSuggestions` | List\<String\> | Opsiyonel | Ekran üzeri metin önerileri |
| `hashtagSet` | List\<String\> | ✅ | Shorts-spesifik hashtag seti |
| `shortCta` | String | ✅ | Kısa CTA |
| `durationGuidance` | String | ✅ | Süre rehberi (ör. "30-45 saniye") |

#### Instagram Reels Metadata

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `shortHook` | String | ✅ | İlk 3 saniye hook metni |
| `shortCaption` | String | ✅ | Instagram caption (emoji dahil) |
| `overlayTextSuggestions` | List\<String\> | Opsiyonel | Ekran üzeri metin önerileri |
| `hashtagSet` | List\<String\> | ✅ | IG-spesifik hashtag seti (≤30) |
| `shortCta` | String | ✅ | CTA metni |
| `durationGuidance` | String | ✅ | Süre rehberi (ör. "60-90 saniye") |

#### Facebook Reels Metadata

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `shortHook` | String | ✅ | İlk 3 saniye hook metni |
| `shortCaption` | String | ✅ | Facebook caption (daha uzun format) |
| `overlayTextSuggestions` | List\<String\> | Opsiyonel | Ekran üzeri metin önerileri |
| `hashtagSet` | List\<String\> | ✅ | FB-spesifik hashtag seti |
| `shortCta` | String | ✅ | CTA metni |
| `durationGuidance` | String | ✅ | Süre rehberi |

### 7.4. PlatformPackageSceneUsageOutput (sceneUsages içinde)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `sceneNumber` | Integer | ✅ | Storyboard sahne numarası |
| `usageType` | String | ✅ | FULL / PARTIAL / ADAPTED / EXCLUDED |
| `included` | Boolean | ✅ | Bu pakette kullanılıyor mu |
| `adaptationNote` | String | Opsiyonel | Adaptasyon notu |

### 7.5. PlatformPackageValidationIssueOutput (Opsiyonel)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `field` | String | ✅ | Sorunlu alan adı |
| `issue` | String | ✅ | Sorun açıklaması |
| `severity` | String | ✅ | WARNING / INFO |

> AI, doğrulama uyarıları döndürebilir ancak bunlar bilgilendirme amaçlıdır.
> Uyarılar paket statüsünü değiştirmez.

---

## 8. AI Output Rules

| # | Kural |
|---|-------|
| 1 | AI yalnızca **JSON** formatında çıktı üretmelidir |
| 2 | AI, istenen her platform için **tam olarak bir** paket döndürmelidir |
| 3 | AI paketleri **onaylayamaz** — tüm paketler `AI_GENERATED` olarak oluşturulur |
| 4 | AI paketleri `READY_FOR_PUBLISH` olarak **işaretleyemez** |
| 5 | AI içeriği **yayınlayamaz** |
| 6 | AI video/audio/image dosyası **üretemez** |
| 7 | AI OAuth/sosyal hesap bilgisi **isteyemez** |
| 8 | AI çıktısında API key veya secret **bulunamaz** |
| 9 | AI çıktısında raw provider metadata **bulunamaz** |
| 10 | AI çıktısında raw prompt text **bulunamaz** |
| 11 | AI çıktısı platformlara özgü **karakter limitlerini** aşmamalıdır |
| 12 | AI çıktısında `status` alanı varsa **görmezden gelinir** — service belirler |

---

## 9. Validation Rules

### 9.1. Response-Level Doğrulama

| # | Kural | Hata Durumu |
|---|-------|-------------|
| 1 | Response boş olmamalı | FAILED — empty response |
| 2 | JSON parse edilebilmeli | FAILED — invalid JSON |
| 3 | `packages` alanı mevcut ve boş olmamalı | FAILED — missing packages |
| 4 | İstenen her platform için tam olarak bir paket olmalı | FAILED — missing/extra platform |

### 9.2. Per-Package Doğrulama

| # | Alan | Kural | Hata Durumu |
|---|------|-------|-------------|
| 1 | `packageType` | Bilinen enum değeri olmalı | FAILED — unknown package type |
| 2 | `title` | Boş olmamalı | FAILED — missing title |
| 3 | `captionOrDescription` | Boş olmamalı | FAILED — missing caption |
| 4 | `hashtags` | Liste olmalı | WARNING — normalize edilir |
| 5 | `callToAction` | Boş olmamalı | WARNING — varsayılan CTA atanabilir |
| 6 | `platformSpecificMetadata` | Platform tipine uygun alanlar olmalı | FAILED — invalid metadata |

### 9.3. Platform-Specific Doğrulama

| Platform | Zorunlu Metadata Alanları |
|----------|--------------------------|
| YOUTUBE_LONG_FORM | `videoTitle`, `longDescription`, `tags` |
| YOUTUBE_SHORTS | `shortHook`, `shortCaption`, `hashtagSet`, `shortCta`, `durationGuidance` |
| INSTAGRAM_REELS | `shortHook`, `shortCaption`, `hashtagSet`, `shortCta`, `durationGuidance` |
| FACEBOOK_REELS | `shortHook`, `shortCaption`, `hashtagSet`, `shortCta`, `durationGuidance` |

### 9.4. Güvenlik Doğrulaması

| # | Kural |
|---|-------|
| 1 | AI yanıtındaki `status` alanı varsa görmezden gelinir |
| 2 | AI'ın `APPROVED` döndürmesi kabul edilmez |
| 3 | Bilinmeyen alanlar görmezden gelinir (strict rejection yerine ignore) |
| 4 | Malformed/boş yanıt kontrollü FAILED'e dönüştürülür |

---

## 10. Failure Model

### 10.1. Hata Senaryoları

| Senaryo | Davranış |
|---------|----------|
| Provider timeout/error | Generation FAILED — retryable=true |
| Boş AI yanıtı | Generation FAILED — retryable=true |
| Invalid JSON | Generation FAILED — retryable=true |
| Eksik platform paketi | Generation **FAILED** — all-or-nothing |
| Bilinmeyen package type | Generation FAILED |
| Zorunlu alan eksik (title, caption) | Generation **FAILED** — all-or-nothing |
| Güvensiz/reddedici içerik | Generation FAILED — retryable=false |
| Kısmi paket üretimi (3/4 platform) | Generation **FAILED** — all-or-nothing |

### 10.2. Seçilen Strateji: All-or-Nothing

**İlk implementasyon kuralı:**

> Tek bir istekte tüm platform paketleri birlikte üretilir.
> Herhangi bir platform paketi geçersizse, tüm üretim denemesi başarısız sayılır.

**Gerekçe:**

| Avantaj | Açıklama |
|---------|----------|
| Basitlik | Kısmi başarı yönetimi gereksiz |
| Tutarlılık | Tüm paketler aynı context'ten üretilir |
| Review kolaylığı | Admin ya tüm paketleri review eder ya da hiçbirini |
| Hata takibi | Tek failure noktası, audit trail basit |

**Gelecek esneklik:**

Kısmi başarı (partial success) desteği gelecek iterasyonlarda eklenebilir.
Bu durumda başarılı paketler `AI_GENERATED`, başarısız olanlar `FAILED` olarak
kaydedilebilir. İlk fazda bu karmaşıklık eklenmez.

---

## 11. Safety and Governance

| # | Kural |
|---|-------|
| 1 | AI çıktısı yalnızca **taslak** (draft) niteliğindedir |
| 2 | İnsan review **zorunludur** |
| 3 | Onay (approve) yalnızca **explicit admin action** ile yapılır |
| 4 | Red (reject) yalnızca **explicit admin action** ile yapılır |
| 5 | Generic status update endpoint **oluşturulmaz** |
| 6 | `READY_FOR_PUBLISH` statüsü **eklenmez** |
| 7 | Otomatik publishing **yapılmaz** |
| 8 | Media generation **yapılmaz** |
| 9 | Raw prompt gateway'den dışarıya **sızmaz** |
| 10 | Raw provider payload **saklanmaz** |
| 11 | API key / secret contract DTO'larında **bulunmaz** |
| 12 | REST katmanı ileride **ayrı safe DTO'lar** kullanmalıdır |
| 13 | FP-001 / FP-002 / FP-003 governance kuralları **korunur** ve değiştirilmez |

---

## 12. Provider Strategy

### 12.1. Provider Abstraction

| Bileşen | Açıklama |
|---------|----------|
| `PlatformPackageAiGateway` | Interface — application service bu interface'e bağımlıdır |
| `MockPlatformPackageAiGateway` | Mock implementasyon — geliştirme/test için |
| `OpenAiPlatformPackageAiGateway` | OpenAI implementasyon — gerçek AI çağrısı |

### 12.2. Provider Isolation Kuralları

| # | Kural |
|---|-------|
| 1 | Application service **interface'e** bağımlıdır, OpenAI'a değil |
| 2 | Mock provider geliştirme sırasında **ilk olarak** implementasyona girer |
| 3 | OpenAI provider ayrı task'ta eklenir |
| 4 | Provider-specific kod `ai/gateway/` altında **izole** kalır |
| 5 | Mock fallback `@ConditionalOnMissingBean` veya eşdeğer pattern kullanır |
| 6 | FP-002/FP-003 provider pattern'ı ile tutarlıdır |
| 7 | Her provider `providerType` ve `modelName` bilgisini response'a ekler |

### 12.3. Provider Config Pattern (FP-003 ile Tutarlı)

```
AI_PROVIDER=mock    → MockPlatformPackageAiGateway aktif
AI_PROVIDER=openai  → OpenAiPlatformPackageAiGateway aktif
```

---

## 13. Examples

> **Uyarı:** Aşağıdaki örnekler kavramsal gösterim amaçlıdır.
> Raw prompt, raw provider response, API key veya secret içermez.

### 13.1. Abbreviated Request Shape

```json
{
  "requestId": "req-abc-123",
  "contentProjectId": "cp-uuid-001",
  "sourceStoryboardDraftId": "sb-uuid-001",
  "sourceContentDraftId": "cd-uuid-001",
  "sourceScenarioDraftId": "sc-uuid-001",
  "requestedByAdminId": "admin-system",
  "requestedAt": "2026-07-03T12:00:00Z",
  "locale": "tr",
  "toneGuidance": "eğitici ve duygusal",
  "targetPlatforms": [
    { "platformType": "YOUTUBE_LONG_FORM" },
    { "platformType": "YOUTUBE_SHORTS" },
    { "platformType": "INSTAGRAM_REELS" },
    { "platformType": "FACEBOOK_REELS" }
  ],
  "storyboard": {
    "title": "Truva Atı'nın Hikayesi",
    "summary": "Antik çağın en ...",
    "visualStyleGuide": "Sinematik, sıcak tonlar",
    "narrationStyle": "Hikaye anlatıcısı",
    "targetDurationSeconds": 340,
    "aspectRatio": "16:9",
    "scenes": [
      {
        "sceneNumber": 1,
        "sourceBeatType": "HOOK",
        "sourceBeatOrder": 0,
        "sceneTitle": "Antik Çağın Hikayesi",
        "visualDescription": "Truva Atı silueti ...",
        "narrationText": "Antik çağın en ilginç ...",
        "mood": "Gizemli",
        "estimatedDurationSeconds": 10
      }
    ]
  },
  "knowledgeContext": {
    "topicTitle": "Truva Atı",
    "niche": "Anadolu Hikayeleri",
    "cityOrRegion": "Çanakkale"
  }
}
```

### 13.2. Abbreviated Response Shape

```json
{
  "success": true,
  "providerType": "OPENAI",
  "modelName": "gpt-4o-mini",
  "generatedAt": "2026-07-03T12:00:05Z",
  "packages": [
    {
      "packageType": "YOUTUBE_LONG_FORM",
      "title": "Truva Atı'nın Bilinmeyen Hikayesi | Anadolu Atlası",
      "captionOrDescription": "Antik dünyanın en ilginç ...",
      "hashtags": ["TruvaAtı", "Anadolu", "Tarih"],
      "callToAction": "Abone olmayı unutmayın!",
      "targetDurationSeconds": 340,
      "aspectRatio": "16:9",
      "contentStructureNotes": "Hook → Giriş → Ana anlatı → ...",
      "platformSpecificMetadata": {
        "videoTitle": "Truva Atı'nın Bilinmeyen Hikayesi",
        "longDescription": "Bu videoda Truva Atı'nın ...",
        "chapterOutline": ["00:00 Giriş", "00:10 Truva'nın Önemi"],
        "pinnedCommentSuggestion": "Hangi Anadolu hikayesini ...",
        "thumbnailTextSuggestion": "TRUVA ATI'NIN SIRRI",
        "tags": ["truva atı", "anadolu", "tarih"]
      }
    },
    {
      "packageType": "YOUTUBE_SHORTS",
      "title": "Truva Atı'nın Sırrı #shorts",
      "captionOrDescription": "Truva Atı hakkında ...",
      "hashtags": ["TruvaAtı", "shorts", "tarih"],
      "callToAction": "Devamı için abone ol!",
      "targetDurationSeconds": 45,
      "aspectRatio": "9:16",
      "platformSpecificMetadata": {
        "shortHook": "Truva Atı'nın gerçek hikayesini ...",
        "shortCaption": "Antik çağın en büyük ...",
        "overlayTextSuggestions": ["Truva Atı", "MÖ 1200"],
        "hashtagSet": ["TruvaAtı", "tarih", "shorts"],
        "shortCta": "Tam videoyu izle!",
        "durationGuidance": "30-45 saniye"
      },
      "sceneUsages": [
        { "sceneNumber": 1, "usageType": "ADAPTED", "included": true, "adaptationNote": "Hook 5 saniyeye kısaltıldı" },
        { "sceneNumber": 4, "usageType": "PARTIAL", "included": true, "adaptationNote": "Duygusal zirve sahnesinin özeti" }
      ]
    }
  ]
}
```

> **Not:** Yukarıdaki JSON örnekleri kısaltılmıştır. Gerçek yanıtta tüm 4 platform paketi bulunur.
> Instagram Reels ve Facebook Reels paketleri YouTube Shorts ile benzer yapıdadır.

---

## 14. Out of Scope

| # | Kapsam Dışı | Gerekçe |
|---|-------------|---------|
| 1 | REST DTO'ları | Engineering design task'ında tanımlanacak |
| 2 | Frontend DTO'ları | Frontend task'ında tanımlanacak |
| 3 | Database schema | Engineering design task'ında tanımlanacak |
| 4 | Prompt implementasyonu | Provider task'ında implementasyona girecek |
| 5 | OpenAI provider implementasyonu | Ayrı provider task |
| 6 | Publishing API entegrasyonları | Gelecek feature |
| 7 | Media generation | Gelecek feature |
| 8 | Image/video/audio dosyaları | Gelecek feature |
| 9 | Raw provider payload storage | Güvenlik kısıtlaması |
| 10 | Prompt yönetim UI | Ayrı OPS feature |

---

## 15. Open Questions

| # | Soru | Etki | Önerilen Karar |
|---|------|------|----------------|
| 1 | All-or-nothing mı, yoksa per-platform partial success mı? | Hata yönetimi karmaşıklığı | İlk fazda **all-or-nothing** |
| 2 | Tek AI isteği mi, yoksa platform başına ayrı istek mi? | Maliyet, latency, tutarlılık | İlk fazda **tek istek** |
| 3 | Hashtag'ler `List<String>` mi yoksa tek `String` mi? | Parser basitliği | **List\<String\>** (normalize edilebilir) |
| 4 | Platform-specific metadata strongly typed mı yoksa flexible JSON mı? | Type safety vs esneklik | **Strongly typed** (platform enum başına ayrı class) |
| 5 | Tek platform için regeneration desteklensin mi? | UX, maliyet | İlk fazda **hayır** — tüm paketler birlikte |
| 6 | Parser bilinmeyen alanları reject mi etsin, ignore mu? | Forward compatibility | **Ignore** (uyarı log'lanır) |
| 7 | Scene usage output zorunlu mu opsiyonel mi? | AI prompt karmaşıklığı | **Opsiyonel** — ilk fazda sadece short-form paketlerde |
| 8 | Long-form chapters varsayılan olarak üretilsin mi? | İçerik zenginliği | **Evet** — opsiyonel alan olarak üretilir |

---

## 16. Document History

| Tarih | Değişiklik |
|-------|-----------|
| 2026-07-03 | İlk oluşturma — AI Platform Contract Design |
