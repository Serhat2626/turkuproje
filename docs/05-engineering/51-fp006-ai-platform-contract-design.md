# 51 — FP-006 Visual Asset Planning — AI Platform Contract Design

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-006 — Visual Asset Planning & Image Generation Handoff
>
> **Prerequisites**: FP-001 (CLOSED), FP-002 (CLOSED), FP-003 (CLOSED), FP-004 (CLOSED), FP-005 (CLOSED)
>
> **Product Design**: [50-fp006-visual-asset-planning-image-generation-handoff-product-design.md](./50-fp006-visual-asset-planning-image-generation-handoff-product-design.md)

---

## 1. Status

| Özellik | Değer |
|---------|-------|
| **Feature** | FP-006 — Visual Asset Planning & Image Generation Handoff |
| **Task** | 02 — AI Platform Contract Design |
| **Type** | AI Platform Contract Design |
| **Status** | Design / Planning |

Bu doküman, FP-006 Product Design dokümanını (doc 50) provider-agnostic AI
contract tasarımına dönüştürür. İmplementasyon henüz başlamadı.

---

## 2. Purpose

Bu doküman aşağıdaki görevleri yerine getirir:

1. Product Design (doc 50) kararlarını AI gateway contract'ına çevirir
2. Provider-agnostic request/response DTO tasarımını tanımlar
3. Gateway interface tasarımını belirler
4. Mock ve OpenAI provider yönlerini tanımlar
5. Validation, error handling ve güvenlik kurallarını belirler
6. Test stratejisi yönünü belirler
7. Gelecek backend entity mapping yönünü tanımlar

**Kritik Not — Bu Contract Görsel Üretimi Değildir:**

FP-006 AI contract'ı, onaylanmış storyboard sahne açıklamalarından **görsel planlama
metni ve image-generation-ready prompt'lar** üreten bir metin üretim (text generation)
contract'ıdır.

Bu contract:
- Image generation API çağırmaz (DALL-E, Midjourney, Stable Diffusion)
- Görsel dosya üretmez, indirmez, yüklemez, saklamaz
- MediaAsset/GeneratedAsset kaydı oluşturmaz
- Video/ses üretmez
- Publishing/upload/schedule yapmaz

**İki Farklı Prompt Kavramı:**

| Kavram | Contract'taki Yeri | Görünürlük |
|--------|-------------------|-----------|
| **Image prompt (ürün çıktısı)** | Response DTO'da `englishImagePrompt` alanı | ✅ Admin görebilir, düzenleyebilir, onaylayabilir |
| **Internal AI orchestration prompt** | Gateway implementation detayı | ❌ Contract'ın parçası değil, asla expose edilmez |

---

## 3. AI Platform Scope

### 3.1. In Scope

| # | Kapsam |
|---|--------|
| 1 | Görsel planlama metin üretimi (visual planning text generation) |
| 2 | Image-generation-ready prompt üretimi (İngilizce) |
| 3 | Türkçe görsel bağlam açıklaması üretimi |
| 4 | Negatif prompt üretimi |
| 5 | Stil/kompozisyon/kamera/renk yönlendirmesi üretimi |
| 6 | Kültürel/tarihsel doğruluk yönlendirmesi üretimi |
| 7 | Thumbnail prompt planlaması |
| 8 | Provider-agnostic request/response contract |
| 9 | Mock fallback provider |
| 10 | OpenAI provider yönü (metin üretim modeli, image API değil) |
| 11 | Safe metadata (providerType, modelName, generatedAt) |

### 3.2. Out of Scope

| # | Kapsam Dışı | Açıklama |
|---|-------------|----------|
| 1 | DALL-E image generation API | Görsel dosya üretimi FP-006'da yok |
| 2 | Midjourney entegrasyonu | Görsel dosya üretimi FP-006'da yok |
| 3 | Stable Diffusion entegrasyonu | Görsel dosya üretimi FP-006'da yok |
| 4 | MediaAsset kaydı oluşturma | Mevcut entity kullanılmaz |
| 5 | GeneratedAsset kaydı oluşturma | Mevcut entity kullanılmaz |
| 6 | Dosya depolama (storage) | Görsel dosya saklanmaz |
| 7 | Video üretimi/rendering | FP-006'da yok |
| 8 | Ses/seslendirme üretimi | FP-006'da yok |
| 9 | Publishing/upload/schedule | FP-006'da yok |
| 10 | READY_FOR_PUBLISH status | FP-006'da tanımlanmaz |
| 11 | Image URL üretimi | AI çıktısında image URL bulunmaz |
| 12 | Executable script üretimi | AI çıktısında script bulunmaz |

---

## 4. Gateway Design

### 4.1. Suggested Interface

```
AiVisualAssetAgentGateway
```

**Package:** `com.anadoluatlasi.backend.ai.gateway`

Mevcut gateway pattern'ini takip eder:

| Gateway | Feature | Method |
|---------|---------|--------|
| `AiContentAgentGateway` | FP-001 | `generateContentDraft(...)` |
| `AiScenarioAgentGateway` | FP-002 | `generateScenario(...)` |
| `AiStoryboardAgentGateway` | FP-003 | `generateStoryboard(...)` |
| `AiPlatformPackageAgentGateway` | FP-004 | `generatePlatformPackages(...)` |
| **`AiVisualAssetAgentGateway`** | **FP-006** | **`generateVisualAssetPlan(...)`** |

### 4.2. Suggested Method Signature (Conceptual)

```
AiVisualAssetPlanResponse generateVisualAssetPlan(AiVisualAssetPlanRequest request)
```

### 4.3. Expected Behavior

| # | Davranış |
|---|----------|
| 1 | Provider-agnostic: interface aynı, OpenAI veya mock implementasyon farklı |
| 2 | Request DTO provider SDK import'ları içermez |
| 3 | Response DTO provider SDK import'ları içermez |
| 4 | Başarılı yanıt: `success = true`, `output` dolu |
| 5 | Başarısız yanıt: `success = false`, `errorCode`/`errorMessage` dolu, `retryable` bilgisi |
| 6 | Exception fırlatmak yerine response DTO döndürülür |
| 7 | API key/secret asla contract üzerinden taşınmaz |
| 8 | Raw provider request/response asla expose edilmez |
| 9 | İç orchestration prompt response'da yer almaz |
| 10 | Üretilen `englishImagePrompt` ürün çıktısıdır — response'da yer alır |

### 4.4. Implementation Structure (Conceptual)

```
com.anadoluatlasi.backend.ai.gateway/
  └── AiVisualAssetAgentGateway.java          (interface)

com.anadoluatlasi.backend.ai.gateway.mock/
  └── MockAiVisualAssetAgentGateway.java      (mock impl)

com.anadoluatlasi.backend.ai.gateway.openai/
  └── OpenAiVisualAssetAgentGateway.java      (OpenAI impl)

com.anadoluatlasi.backend.ai.contract/
  ├── AiVisualAssetPlanRequest.java           (request DTO)
  ├── AiVisualAssetPlanResponse.java          (response DTO)
  ├── AiVisualAssetPlanOutput.java            (output container)
  ├── AiVisualSceneInput.java                 (scene input DTO)
  ├── AiSceneVisualPlanOutput.java            (scene output DTO)
  ├── AiVisualThumbnailInput.java             (thumbnail input DTO)
  └── AiThumbnailVisualPlanOutput.java        (thumbnail output DTO)
```

### 4.5. Configuration (Conceptual)

Mevcut `AiGatewayConfig` pattern'i:

| Config | Davranış |
|--------|----------|
| `@ConditionalOnProperty(name="ai.provider", havingValue="openai")` | OpenAI gateway aktif |
| `@ConditionalOnMissingBean` | Mock gateway fallback |
| Aynı `ai.provider` property | Tüm gateway'ler aynı property'yi kullanır |

---

## 5. Request Contract

### 5.1. AiVisualAssetPlanRequest

Provider-agnostic request DTO. Gateway'e gönderilen ana istek.

| Kategori | Alan | Tip | Zorunlu | Açıklama |
|----------|------|-----|---------|----------|
| **Kimlik** | requestId | UUID | ✅ | Benzersiz istek kimliği |
| | storyboardDraftId | UUID | ✅ | Kaynak storyboard ID |
| | contentProjectId | UUID | ✅ | İçerik projesi ID |
| **Context** | title | String | ✅ | Hikâye/proje başlığı |
| | summary | String | ❌ | Hikâye özeti |
| | targetPlatform | String | ❌ | Hedef platform (YouTube, IG, etc.) |
| | aspectRatio | String | ❌ | Varsayılan aspect ratio |
| | estimatedDurationSeconds | Integer | ❌ | Tahmini süre |
| | visualStyleGuide | String | ❌ | Görsel stil rehberi |
| | narrationStyle | String | ❌ | Anlatım stili |
| **Scenes** | scenes | List | ✅ | Sahne listesi (min 1) |
| **Thumbnail** | thumbnailContext | Object | ❌ | Thumbnail üretim context'i |
| **Policies** | languagePolicy | String | ❌ | Dil politikası (varsayılan: EN prompt + TR context) |
| | culturalContextPolicy | String | ❌ | Kültürel bağlam politikası |
| | safetyPolicy | String | ❌ | İçerik güvenliği politikası |
| **Audit** | requestedBy | String | ❌ | İsteği yapan admin |

### 5.2. AiVisualSceneInput

Her storyboard sahnesi için giriş verisi.

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| storyboardSceneId | UUID | ✅ | Kaynak sahne ID |
| sceneNumber | Integer | ✅ | Sahne numarası (1-based) |
| sourceBeatType | String | ✅ | Beat tipi (HOOK, INTRODUCTION, etc.) |
| sourceBeatOrder | Integer | ❌ | Beat sırası |
| sceneTitle | String | ✅ | Sahne başlığı |
| visualDescription | String | ✅ | 🔴 Sahnenin görsel tanımı — **birincil input** |
| narrationText | String | ❌ | Seslendirme metni |
| onScreenText | String | ❌ | Ekran metni |
| cameraDirection | String | ❌ | Kamera yönlendirmesi |
| transitionNote | String | ❌ | Geçiş notu |
| mood | String | ❌ | Atmosfer/duygu |
| location | String | ❌ | Sahne mekânı |
| characters | String | ❌ | Karakterler/özneler |
| estimatedDurationSeconds | Integer | ❌ | Sahne süresi |
| mediaGenerationNotes | String | ❌ | Medya üretim notları |

### 5.3. AiVisualThumbnailInput

Thumbnail üretim context'i.

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| title | String | ✅ | Video/içerik başlığı |
| storyHook | String | ❌ | Hikâyenin dikkat çekici öğesi |
| preferredAspectRatio | String | ❌ | Tercih edilen aspect ratio (varsayılan 16:9) |
| emotionalTone | String | ❌ | Duygusal ton (merak, nostalji, heyecan, etc.) |
| textOverlayPolicy | String | ❌ | Metin overlay politikası (bold title, minimal text, etc.) |

---

## 6. Response Contract

### 6.1. AiVisualAssetPlanResponse

Provider-agnostic response DTO. Gateway'den dönen ana yanıt.

| Kategori | Alan | Tip | Açıklama |
|----------|------|-----|----------|
| **Durum** | success | boolean | Başarı durumu |
| **Çıktı** | output | AiVisualAssetPlanOutput | Başarılı çıktı (success=true ise dolu) |
| **Metadata** | providerType | String | Provider tipi (OPENAI, MOCK) — safe |
| | modelName | String | Model adı (gpt-4o-mini) — safe |
| **Hata** | errorCode | String | Hata kodu (success=false ise) |
| | errorMessage | String | Safe hata mesajı (raw provider error body değil) |
| | retryable | boolean | Yeniden denenebilir mi |
| **Zaman** | generatedAt | Instant | Üretim zaman damgası |

### 6.2. AiVisualAssetPlanOutput

Başarılı üretimin çıktı container'ı.

| Kategori | Alan | Tip | Açıklama |
|----------|------|-----|----------|
| **Genel Stil** | planTitle | String | Plan başlığı |
| | overallVisualStyle | String | Bütünsel görsel stil yönlendirmesi |
| | aspectRatioStrategy | String | Aspect ratio stratejisi |
| | culturalAuthenticityGuidance | String | Kültürel/tarihsel doğruluk rehberliği |
| | characterContinuityGuidance | String | Karakter görsel süreklilik notları |
| | locationContinuityGuidance | String | Mekan görsel süreklilik notları |
| | colorPaletteGuidance | String | Genel renk paleti rehberliği |
| **Sahneler** | scenePlans | List | Sahne bazlı görsel planlar (ordered by sceneNumber) |
| **Thumbnail** | thumbnailPlan | Object | Thumbnail görsel planı |

### 6.3. AiSceneVisualPlanOutput

Her sahne için AI'ın ürettiği görsel plan çıktısı.

| Kategori | Alan | Tip | Açıklama |
|----------|------|-----|----------|
| **Referans** | sourceStoryboardSceneId | UUID | Kaynak sahne ID (input'tan korunur) |
| | sceneNumber | Integer | Sahne numarası (input'tan korunur) |
| | sourceBeatType | String | Beat tipi (input'tan korunur) |
| | sceneTitle | String | Sahne başlığı |
| **Prompt** | englishImagePrompt | String | 🔴 İngilizce image-generation-ready prompt — **ürün çıktısı** |
| | turkishVisualContext | String | 🔴 Türkçe görsel bağlam açıklaması — **ürün çıktısı** |
| | negativePrompt | String | Negatif prompt (ne olmaması gerektiği) |
| **Stil** | styleDirective | String | Görsel stil (realistic, cinematic, illustrated, etc.) |
| | aspectRatio | String | Bu sahne için aspect ratio |
| **Kompozisyon** | compositionNotes | String | Çerçeveleme, perspektif, odak noktası |
| | cameraFraming | String | Kamera açısı/çerçeveleme |
| | lightingNotes | String | Aydınlatma yönlendirmesi |
| **Mood / Renk** | colorPalette | String | Sahne renk paleti |
| | mood | String | Atmosfer/duygu |
| **İçerik** | location | String | Mekan/ortam tanımı |
| | characters | String | Karakter/özne tanımı ve görsel notları |
| | culturalDetails | String | Kültürel/tarihsel doğruluk detayları |
| **Overlay** | textOverlayGuidance | String | Ekran metni/overlay yönlendirmesi |
| **Tool Notes** | imageGenerationToolNotes | String | Image gen tool notları |
| | safetyNotes | String | İçerik güvenliği notları |

### 6.4. AiThumbnailVisualPlanOutput

Thumbnail için AI'ın ürettiği görsel plan çıktısı.

| Alan | Tip | Açıklama |
|------|-----|----------|
| thumbnailPrompt | String | 🔴 Thumbnail image-generation-ready prompt — **ürün çıktısı** |
| negativePrompt | String | Negatif prompt |
| textOverlayGuidance | String | Thumbnail metin yerleşimi rehberliği |
| compositionNotes | String | Kompozisyon notları |
| emotionalHook | String | Duygusal çekicilik notu |
| aspectRatio | String | Aspect ratio (genellikle 16:9) |
| styleDirective | String | Stil yönlendirmesi |
| colorPalette | String | Renk paleti |
| sourceSceneNumber | Integer | Referans sahne numarası (opsiyonel) |
| sourceStoryboardSceneId | UUID | Referans sahne ID (opsiyonel) |

---

## 7. Prompt Boundary and Exposure Rules

### 7.1. Contract İçinde Olan / Olmayan

| Kavram | Contract'ta mı? | Açıklama |
|--------|----------------|----------|
| `englishImagePrompt` | ✅ Response'da | Ürün çıktısı — admin görebilir, düzenleyebilir |
| `turkishVisualContext` | ✅ Response'da | Ürün çıktısı — Türkçe editör referansı |
| `negativePrompt` | ✅ Response'da | Ürün çıktısı — image gen tool yönlendirmesi |
| `thumbnailPrompt` | ✅ Response'da | Ürün çıktısı — thumbnail handoff |
| İç AI system prompt | ❌ Yok | Gateway implementation detayı, contract dışı |
| Raw provider request | ❌ Yok | Provider SDK detayı, contract dışı |
| Raw provider response | ❌ Yok | Provider SDK detayı, contract dışı |
| Provider payload | ❌ Yok | Provider SDK detayı, contract dışı |
| API key / secret | ❌ Yok | Asla contract'ta bulunmaz |
| `providerType` | ✅ Response'da | Safe metadata |
| `modelName` | ✅ Response'da | Safe metadata |

### 7.2. Uygulama Kuralları

| # | Kural |
|---|-------|
| 1 | İç orchestration prompt, response contract'ın parçası değildir |
| 2 | İç orchestration prompt, entity'lere kaydedilmez |
| 3 | İç orchestration prompt, REST API response'larında yer almaz |
| 4 | İç orchestration prompt, log'larda açık metin olarak saklanmaz |
| 5 | Raw provider request/response, entity'lere kaydedilmez |
| 6 | Raw provider request/response, REST API response'larında yer almaz |
| 7 | API key/secret hiçbir zaman contract DTO'larında bulunmaz |
| 8 | `englishImagePrompt` ürün çıktısıdır — entity'lere kaydedilir, API'den döner, UI'da gösterilir |
| 9 | `turkishVisualContext` ürün çıktısıdır — aynı kurallar geçerli |
| 10 | `thumbnailPrompt` ürün çıktısıdır — aynı kurallar geçerli |
| 11 | Error response'ta raw provider error body yer almaz |
| 12 | Error response'ta sanitized `errorMessage` döner |

---

## 8. Expected AI Instructions — Conceptual Only

Bu bölüm AI'ın beklenen davranışını kavramsal olarak tanımlar.
Gerçek prompt metni, provider-specific payload veya API anahtarı bu bölümde yer almaz.

### 8.1. AI Görevi

AI'a verilen görev kavramsal olarak şudur:

> Onaylanmış bir storyboard'un sahne açıklamalarını, image-generation-ready
> görsel prompt planlarına dönüştür.

### 8.2. Beklenen AI Davranışları

| # | Davranış |
|---|----------|
| 1 | Her sahne için İngilizce image-generation-ready prompt üret |
| 2 | Her sahne için Türkçe editör bağlam açıklaması üret |
| 3 | Her sahne için negatif prompt üret |
| 4 | Sahne sırasını koru (sceneNumber sıralaması) |
| 5 | Karakter görsel sürekliliğini koru (aynı karakter farklı sahnelerde tutarlı) |
| 6 | Mekan görsel sürekliliğini koru (aynı mekan farklı sahnelerde tutarlı) |
| 7 | Kültürel/tarihsel doğruluğu koru (Anadolu/Türk bağlamı) |
| 8 | Anakronistik veya uygunsuz görsel öğelerden kaçın |
| 9 | Thumbnail prompt'u üret (yüksek etkili, dikkat çekici) |
| 10 | Stil/kompozisyon/kamera/aydınlatma/renk notları ekle |
| 11 | Image URL veya dosya üretme |
| 12 | Yayın otomasyonu veya desteklenmeyen özellik önerme |
| 13 | Executable script veya kod üretme |

### 8.3. Prompt Dili

| Çıktı | Dil | Gerekçe |
|-------|-----|---------|
| `englishImagePrompt` | İngilizce | Image generation araçları (DALL-E, SD, MJ) İngilizce optimize |
| `turkishVisualContext` | Türkçe | Editör/tasarımcı referansı için ana dil |
| `negativePrompt` | İngilizce | Image gen araçları İngilizce optimize |
| `thumbnailPrompt` | İngilizce | Image gen araçları İngilizce optimize |
| Tüm diğer notlar | İngilizce veya Türkçe | Context'e göre AI karar verir |

### 8.4. Kültürel Bağlam Yönlendirmesi

AI, Anadolu hikâyeleri context'inde şu yönlendirmeleri dikkate almalıdır:

- Türkü hikâyeleri genellikle keder, savaş, kayıp, göç, yoksulluk veya trajedi içerir
- Bu temalar saygılı ve duyarlı şekilde görselleştirilmelidir
- Anadolu coğrafyası, mimarisi, kıyafetleri ve gelenekleri doğru yansıtılmalıdır
- Modern logolar, markalar veya anakronistik nesneler hikâye gerektirmedikçe kaçınılmalıdır
- Stereotiplerden veya saygısız tasvirlerden kaçınılmalıdır
- Güvenli olmayan veya grafik görüntü önerilerinden kaçınılmalıdır
- Görsel prompt'lar prodüksiyon güvenli olmalıdır

> **Not:** Bu bölüm kavramsal AI davranışını tanımlar.
> Gerçek AI prompt metni, provider-specific payload JSON veya API anahtarı
> bu dokümanda yer almaz.

---

## 9. Output Shape and Determinism

### 9.1. Yapısal Kurallar

| # | Kural | Açıklama |
|---|-------|----------|
| 1 | Giriş sahne sayısı = çıkış sahne planı sayısı | Her input scene için tam bir output scene plan |
| 2 | `sceneNumber` korunur | Çıkış sceneNumber, giriş sceneNumber ile eşleşir |
| 3 | `sourceStoryboardSceneId` korunur | Çıkış scene ID, giriş scene ID ile eşleşir |
| 4 | Sahne sıralaması korunur | Çıkış scenePlans, sceneNumber'a göre sıralı |
| 5 | Tek thumbnail plan | thumbnailPlan tekil bir çıktıdır |
| 6 | Null opsiyonel alanlar hata üretmez | Eksik input alanları graceful atlanır |
| 7 | Çıkış parse edilebilir olmalıdır | Yapılandırılmış format, backend entity'lere maplenebilir |

### 9.2. Zorunlu Çıkış Alanları

| Alan | Zorunlu | Açıklama |
|------|---------|----------|
| `englishImagePrompt` | ✅ Her scene'de | Boş olamaz |
| `turkishVisualContext` | ✅ Her scene'de | Boş olamaz |
| `sceneNumber` | ✅ Her scene'de | Input'tan korunur |
| `sourceStoryboardSceneId` | ✅ Her scene'de | Input'tan korunur |
| `thumbnailPrompt` | ✅ thumbnailPlan'da | İlk fazda zorunlu |
| `planTitle` | ✅ Parent'ta | Boş olamaz |
| `overallVisualStyle` | ✅ Parent'ta | Boş olamaz |

### 9.3. Opsiyonel Çıkış Alanları

Aşağıdaki alanlar AI tarafından üretilmeyebilir. Eksiklik hata oluşturmaz:

`negativePrompt`, `styleDirective`, `compositionNotes`, `cameraFraming`,
`lightingNotes`, `colorPalette`, `mood`, `location`, `characters`,
`culturalDetails`, `textOverlayGuidance`, `imageGenerationToolNotes`,
`safetyNotes`, `aspectRatioStrategy`, `characterContinuityGuidance`,
`locationContinuityGuidance`, `colorPaletteGuidance`

---

## 10. Validation Rules

### 10.1. Request Validation

| # | Kural | Başarısız → |
|---|-------|-------------|
| 1 | `storyboardDraftId` zorunlu | Reject request |
| 2 | `contentProjectId` zorunlu | Reject request |
| 3 | `title` zorunlu, boş olamaz | Reject request |
| 4 | `scenes` zorunlu, en az 1 sahne | Reject request |
| 5 | Her sahne `sceneNumber` zorunlu | Reject request |
| 6 | Her sahne `storyboardSceneId` zorunlu | Reject request |
| 7 | Her sahne `visualDescription` zorunlu veya fallback tanımlı | Reject veya fallback |
| 8 | Kaynak storyboard gateway çağrılmadan önce APPROVED kontrol edilmiş olmalı | Service layer sorumluluğu |

### 10.2. Response Validation

| # | Kural | Başarısız → |
|---|-------|-------------|
| 9 | `success = true` ise `output` dolu olmalı | Treat as failure |
| 10 | Output scene plan sayısı = input scene sayısı | Log uyarı, kısmi çıktı kabul et veya reddet |
| 11 | Her output scene `sourceStoryboardSceneId` veya `sceneNumber` ile referans | Eşleştirme hatası → log warning |
| 12 | Her output scene `englishImagePrompt` dolu olmalı | Eksik → boş scene plan oluştur veya reddet |
| 13 | Her output scene `turkishVisualContext` dolu olmalı | Eksik → boş scene plan oluştur veya reddet |
| 14 | `thumbnailPlan` `thumbnailPrompt` dolu olmalı | Eksik → thumbnail plansız devam veya warning |
| 15 | Çıktıda image URL olmamalı | Varsa temizle, log uyarı |
| 16 | Çıktıda MediaAsset ID olmamalı | Varsa temizle, log uyarı |
| 17 | Çıktı parse edilebilir yapıda olmalı | Parse hatası → treat as failure |

---

## 11. Error Handling and Retry

### 11.1. Error Cases

| Hata | Retryable | Açıklama |
|------|-----------|----------|
| Provider unavailable (timeout, network) | ✅ Evet | Geçici bağlantı hatası |
| Provider rate limit | ✅ Evet | API hız limiti |
| Invalid response shape (parse failure) | ✅ Evet | AI beklenmeyen format üretti |
| Partial scene output | ✅ Evet | AI bazı sahneleri atlattı |
| Safety refusal | ❌ Hayır | Provider içerik politikası reddi |
| Authentication failure | ❌ Hayır | API key geçersiz |
| Unsupported model | ❌ Hayır | Model mevcut değil |

### 11.2. Error Response Format

```
success: false
output: null
providerType: "OPENAI" veya "MOCK"
modelName: "gpt-4o-mini"
errorCode: "PROVIDER_TIMEOUT" / "RATE_LIMIT" / "PARSE_ERROR" / "SAFETY_REFUSAL" / "AUTH_FAILURE" / ...
errorMessage: "Sanitized, safe hata mesajı"
retryable: true / false
generatedAt: timestamp
```

### 11.3. Error Handling Kuralları

| # | Kural |
|---|-------|
| 1 | Raw provider error body response'da yer almaz |
| 2 | Raw provider error body admin UI'da gösterilmez |
| 3 | Sanitized `errorMessage` kullanılır |
| 4 | `errorCode` standardize edilmiş enum değerleri kullanır |
| 5 | `retryable` alanı admin'e retry kararı için yardımcı olur |
| 6 | Provider exception'ları gateway içinde catch edilir, response DTO'ya dönüştürülür |
| 7 | Gateway interface exception fırlatmaz, response döner |

### 11.4. Mock Fallback

Mock provider, OpenAI hatası durumunda otomatik fallback olarak çalışmaz.
`ai.provider` config değeri başlangıçta hangi provider'ın aktif olduğunu belirler.
OpenAI çağrısı başarısız olursa hata raporlanır, admin tekrar dener.

---

## 12. Mock Provider Contract

### 12.1. Beklenen Mock Davranışı

| # | Davranış |
|---|----------|
| 1 | Deterministik çıktı — aynı input aynı output |
| 2 | Her input sahne için tam bir scene visual plan |
| 3 | Sabit (stable) thumbnail plan |
| 4 | `providerType = "MOCK"` |
| 5 | `modelName = "mock-visual-asset-v1"` veya benzeri |
| 6 | Harici IO yok (network, file, database) |
| 7 | Test ve local smoke test için uygun |
| 8 | Her zaman `success = true` (hata simülasyonu ayrı test metodu ile) |

### 12.2. Mock Output Yapısı

Mock provider şu yapıda çıktı üretir:

**Parent plan:**
- `planTitle`: Input title + " — Görsel Varlık Planı"
- `overallVisualStyle`: "Cinematic, warm-toned Anatolian visual style"
- Sabit süreklilik notları ve renk paleti

**Her sahne için:**
- `englishImagePrompt`: Sahne `visualDescription` + stil template
- `turkishVisualContext`: Sahne `sceneTitle` + "— Türkçe görsel bağlam"
- `negativePrompt`: Sabit standart negatif prompt
- Sabit stil/kompozisyon/aydınlatma notları
- `sceneNumber` ve `sourceStoryboardSceneId` input'tan korunur

**Thumbnail:**
- `thumbnailPrompt`: Input title + thumbnail template
- Sabit kompozisyon ve stil notları

---

## 13. OpenAI Provider Contract Direction

### 13.1. Provider Implementation Yönü

| # | Yön |
|---|-----|
| 1 | OpenAI **metin üretim (chat completion) API** kullanır |
| 2 | OpenAI **image generation API** (DALL-E) kullanmaz |
| 3 | Varsayılan model: `gpt-4o-mini` (runtime config ile değiştirilebilir) |
| 4 | Mevcut proje stil'ini takip eder (FP-001–FP-004 gateway pattern) |
| 5 | Structured output (JSON mode veya system instruction ile) istenir |
| 6 | Provider response parse edilerek `AiVisualAssetPlanResponse`'a dönüştürülür |
| 7 | Raw prompt ve raw response expose edilmez |
| 8 | Error'lar sanitize edilir |
| 9 | API key runtime environment variable'dan okunur (CI/CD managed) |

### 13.2. OpenAI Chat Completion Kullanımı

| Özellik | Kullanım |
|---------|----------|
| Endpoint | `POST /v1/chat/completions` (metin üretim) |
| **KULLANILMAZ** | `POST /v1/images/generations` (image generation) |
| Model | `gpt-4o-mini` (varsayılan) |
| Response format | Structured JSON çıktı |
| Temperature | Düşük-orta (tutarlılık için) |
| Max tokens | Sahne sayısına göre ölçeklenen limit |

### 13.3. OpenAI Image API Yasağı

| # | Kural |
|---|-------|
| 1 | OpenAI Images API (`/v1/images/generations`) FP-006'da çağrılmaz |
| 2 | DALL-E model referansı FP-006 gateway'de kullanılmaz |
| 3 | Image URL döndüren hiçbir OpenAI endpoint çağrılmaz |
| 4 | FP-006 gateway yalnızca chat/completion metin üretim endpoint'ini kullanır |

---

## 14. Safety and Cultural Authenticity

### 14.1. İçerik Güvenliği Kuralları

| # | Kural | Açıklama |
|---|-------|----------|
| 1 | Hikâye gerektirmediği sürece anakronistik nesnelerden kaçın | Osmanlı döneminde cep telefonu gibi |
| 2 | Stereotiplerden veya saygısız tasvirlerden kaçın | Kültürel hassasiyet |
| 3 | Anadolu kültürel bağlamını doğru yansıt | Coğrafya, mimari, kıyafet, gelenek |
| 4 | Modern logo/markalardan kaçın | Hikâye bağlamı gerektirmedikçe |
| 5 | Güvenli olmayan veya grafik görüntü önerilerinden kaçın | Prodüksiyon güvenliği |
| 6 | Görsel prompt'lar prodüksiyon-safe olmalı | Genel izleyici kitlesi uygunluğu |

### 14.2. Türkü Hikâyeleri Özel Bağlamı

Türkü hikâyeleri sıklıkla ağır duygusal temalar içerir:

| Tema | Görselleştirme Yaklaşımı |
|------|--------------------------|
| Keder / Yas | Saygılı, dolaylı anlatım — yüz ifadesi, manzara, simgesel nesne |
| Savaş / Şehitlik | Dramatik ama grafik olmayan — duman, siluet, bayrak |
| Göç / Ayrılık | Yol, tren, kapı, arkaya bakış — nostalji |
| Yoksulluk | Doğal ortam, sadelik — saygılı tablo |
| Trajedi | Doğa olayları, karanlık gökyüzü — sembolik |
| Aşk / Hasret | Renk kontrastı, mesafe, ışık — lirik |

AI bu temaları ürettiği prompt'larda duyarlı ve saygılı şekilde yansıtmalıdır.

---

## 15. Integration with Future Backend Entities

AI response DTO'ları gelecekte şu backend entity'lere map'lenecektir:

| AI Response DTO | Backend Entity | Map'leme Yönü |
|-----------------|---------------|---------------|
| `AiVisualAssetPlanOutput` | `VisualAssetPlan` | Parent plan alanları |
| `AiSceneVisualPlanOutput` | `SceneVisualPlan` | Per-scene alanlar |
| `AiThumbnailVisualPlanOutput` | `ThumbnailPlan` | Thumbnail alanları |

### Map'leme Kuralları (Kavramsal)

| # | Kural |
|---|-------|
| 1 | `AiVisualAssetPlanOutput` → yeni `VisualAssetPlan` entity (status: AI_GENERATED) |
| 2 | Her `AiSceneVisualPlanOutput` → yeni `SceneVisualPlan` child entity |
| 3 | `AiThumbnailVisualPlanOutput` → yeni `ThumbnailPlan` child entity |
| 4 | `providerType` ve `modelName` → safe metadata olarak entity'ye kaydedilir |
| 5 | `generatedBy` → `GeneratedBy.AI_CONTENT_AGENT` |
| 6 | `sourceStoryboardSceneId` → entity'deki `sourceStoryboardScene` referansı |
| 7 | `sceneNumber` korunur → entity'deki `sceneNumber` |
| 8 | Raw AI prompt/request/response entity'ye kaydedilmez |

> **Not:** Entity implementasyonu bu dokümanın kapsamı dışındadır.
> Ayrı bir engineering design task'ında yapılacaktır.

---

## 16. Integration with Future Admin UI

Admin UI gelecekte şu verileri gösterecektir:

| UI Bölümü | Veri Kaynağı | Açıklama |
|-----------|-------------|----------|
| Image prompt preview | `englishImagePrompt` | Ürün çıktısı — görüntülenir, düzenlenir |
| Turkish context | `turkishVisualContext` | Editör referansı |
| Negative prompt | `negativePrompt` | Image gen tool yönlendirmesi |
| Style/composition notes | `styleDirective`, `compositionNotes`, etc. | Detay bilgileri |
| Thumbnail prompt | `thumbnailPrompt` | Thumbnail planı |
| Status badge | `status` | AI_GENERATED, NEEDS_REVIEW, APPROVED, etc. |
| Approve/reject buttons | Dedicated actions | Explicit admin eylemi |
| Edit form | Tüm prompt alanları | Admin düzenleme |

> **Not:** UI implementasyonu bu dokümanın kapsamı dışındadır.
> Ayrı bir frontend task'ında yapılacaktır.

---

## 17. Governance Rules

### 17.1. AI Output Governance

| # | Kural |
|---|-------|
| 1 | AI çıktısı AI_GENERATED ile oluşturulur, asla APPROVED |
| 2 | Otomatik onay (auto-approval) yoktur |
| 3 | Onay açık admin eylemidir (explicit approve) |
| 4 | Red açık admin eylemidir (explicit reject) |
| 5 | Düzenleme onay vermez — edit → NEEDS_REVIEW |
| 6 | Generic status update endpoint yoktur |
| 7 | READY_FOR_PUBLISH status yoktur |

### 17.2. First Phase Boundary

| # | Kural |
|---|-------|
| 8 | Gerçek görsel üretimi FP-006'da yapılmaz |
| 9 | Image generation API (DALL-E/Midjourney/SD) FP-006 gateway'de çağrılmaz |
| 10 | MediaAsset kaydı FP-006'da oluşturulmaz |
| 11 | GeneratedAsset kaydı FP-006'da oluşturulmaz |
| 12 | Görsel dosya indirme/yükleme/depolama FP-006'da yapılmaz |
| 13 | Video/ses üretimi FP-006'da yapılmaz |
| 14 | Publishing/upload/schedule FP-006'da yapılmaz |

### 17.3. Data Safety

| # | Kural |
|---|-------|
| 15 | İç AI orchestration prompt contract'ta yer almaz |
| 16 | Raw provider request entity'ye kaydedilmez, API'den dönmez |
| 17 | Raw provider response entity'ye kaydedilmez, API'den dönmez |
| 18 | Provider payload log'lara yazılmaz |
| 19 | API key/secret hiçbir zaman contract DTO'larında bulunmaz |
| 20 | Üretilen image prompt ürün çıktısıdır — gösterilir, düzenlenir, onaylanır |
| 21 | Error response'ta raw provider error body yer almaz |

### 17.4. Provider Architecture

| # | Kural |
|---|-------|
| 22 | Mock provider fallback zorunludur |
| 23 | Provider-agnostic request/response contract zorunludur |
| 24 | Provider SDK import'ları gateway interface'e sızmamalıdır |
| 25 | Gateway exception fırlatmaz — response DTO döndürür |

---

## 18. Testing Strategy Direction

### 18.1. Request Mapper Tests

| # | Test | Beklenen |
|---|------|----------|
| 1 | StoryboardDraft + scenes → AiVisualAssetPlanRequest mapping | ✅ Tüm alanlar doğru |
| 2 | Null optional fields → request still valid | ✅ Graceful |
| 3 | Empty scenes list → validation failure | ✅ Reject |

### 18.2. Response Mapper Tests

| # | Test | Beklenen |
|---|------|----------|
| 4 | AiVisualAssetPlanOutput → VisualAssetPlan entity mapping | ✅ Tüm alanlar doğru |
| 5 | Scene plan count = input scene count | ✅ Eşleşme |
| 6 | Thumbnail output → ThumbnailPlan mapping | ✅ Doğru |

### 18.3. Mock Gateway Tests

| # | Test | Beklenen |
|---|------|----------|
| 7 | Mock gateway returns success | ✅ |
| 8 | Mock output scene count = input scene count | ✅ |
| 9 | Mock output sceneNumber preserved | ✅ |
| 10 | Mock output includes thumbnail | ✅ |
| 11 | Mock output providerType = "MOCK" | ✅ |
| 12 | Mock output deterministic (same input → same output) | ✅ |

### 18.4. OpenAI Gateway Contract Tests

| # | Test | Beklenen |
|---|------|----------|
| 13 | Mocked OpenAI response → valid AiVisualAssetPlanResponse | ✅ |
| 14 | OpenAI timeout → retryable error response | ✅ |
| 15 | OpenAI invalid JSON → parse error response | ✅ |
| 16 | OpenAI safety refusal → non-retryable error | ✅ |

### 18.5. Safety Tests

| # | Test | Beklenen |
|---|------|----------|
| 17 | Response contains no raw prompt | ✅ |
| 18 | Response contains no API key | ✅ |
| 19 | Response contains no provider payload | ✅ |
| 20 | Response contains no image URL | ✅ |
| 21 | Error response contains no raw error body | ✅ |

### 18.6. Scene Count and Matching Tests

| # | Test | Beklenen |
|---|------|----------|
| 22 | 6 input scenes → 6 output scene plans | ✅ |
| 23 | Output sceneNumbers match input sceneNumbers | ✅ |
| 24 | Output sourceStoryboardSceneIds match input | ✅ |

### 18.7. Thumbnail and Null Field Tests

| # | Test | Beklenen |
|---|------|----------|
| 25 | Thumbnail plan present | ✅ |
| 26 | Null optional input fields → no exception | ✅ |
| 27 | Null optional output fields → entity still created | ✅ |

---

## 19. Open Questions

| # | Soru | Önerilen Cevap |
|---|------|----------------|
| 1 | Görsel planlama AI kullanmalı mı? | Evet. Storyboard sahne açıklamalarından image-gen-ready prompt üretimi AI-assisted |
| 2 | Image generation bu gateway'in parçası mı? | Hayır. Yalnızca metin üretim (chat completion) |
| 3 | Çıktı her input sahnesi için bir plan mı olmalı? | Evet. 1:1 eşleşme |
| 4 | Image prompt dili ne olmalı? | İngilizce prompt + Türkçe context |
| 5 | Thumbnail zorunlu mu? | İlk fazda evet |
| 6 | Mock provider zorunlu mu? | Evet. Test ve local smoke için |
| 7 | OpenAI image API kullanmalı mı? | Hayır. Asla. Yalnızca chat completion |
| 8 | Raw prompt saklanmalı mı? | Hayır. Entity'ye ve API'ye asla |
| 9 | Model gpt-4o-mini mi? | Evet, varsayılan. Runtime config ile değiştirilebilir |
| 10 | MediaAsset/GeneratedAsset kullanılmalı mı? | Hayır. Yalnızca gelecek image generation fazında |
| 11 | Sahne sayısı uyuşmazlığında ne olmalı? | Log warning, kısmi çıktıyı kabul et veya reddet (implementasyon kararı) |
| 12 | Karakter/mekan sürekliliği nasıl sağlanmalı? | AI'a tüm sahneler birlikte gönderilir, süreklilik talimatı verilir |

---

## 20. Document History

| Versiyon | Tarih | Durum | Açıklama |
|----------|-------|-------|----------|
| 1.0 | 2026-07-04 | Design / Planning | İlk FP-006 AI platform contract design dokümanı oluşturuldu |

---

> **Doküman Kuralları:**
> - İç AI orchestration prompt, provider request/response, raw payload, API key veya secret bu dokümanda yer almaz.
> - Bu doküman yalnızca provider-agnostic AI contract tasarımını içerir.
> - Kod implementasyonu bu dokümanın kapsamı dışındadır.
> - Mevcut doküman numaraları değiştirilmez.
> - Üretilen image prompt metni ürün çıktısıdır ve gösterilebilir.
> - İç AI system prompt'u ile ürün image prompt çıktısı ayrı kavramlardır.
> - OpenAI image generation API (DALL-E) bu contract'ta asla kullanılmaz.
