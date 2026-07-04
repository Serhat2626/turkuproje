# 55 — FP-007 Image Generation & Media Asset Pipeline — AI Platform Contract Design

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-007 — Image Generation & Media Asset Pipeline
>
> **Task**: Task 02 — AI Platform Contract Design (documentation-only)
>
> **Prerequisites**: FP-006 (CLOSED), FP-007 Task 01 Product Design (DONE)
>
> **Product Design Source**: [54-fp007-image-generation-media-asset-pipeline-product-design.md](54-fp007-image-generation-media-asset-pipeline-product-design.md)

---

## 1. Status / Scope

| Alan | Değer |
|------|-------|
| **Task** | FP-007 Task 02 — AI Platform Contract Design |
| **Tür** | Documentation-only |
| **Kaynak** | FP-007 Task 01 Product Design dokümanı |
| **Kod değişikliği** | ❌ Yok |
| **Runtime değişikliği** | ❌ Yok |
| **Migration** | ❌ Yok |
| **Provider implementation** | ❌ Bu taskta yapılmaz |
| **Hedef** | Engineering Design için contract baseline sağlamak |

Bu doküman, FP-007 Product Design dokümanında tanımlanan image generation ve media asset pipeline akışı için **AI Platform seviyesinde provider-agnostic contract tasarımını** içerir.

Gerçek provider SDK entegrasyonu (OpenAI Image API, Replicate, Stability AI vb.) bu doküman kapsamında **değildir**. Bu doküman, Engineering Design aşamasında uygulanacak contract sınırlarını belirler.

---

## 2. AI Platform Role in FP-007

AI Platform, image generation pipeline'ında **provider-agnostic boundary** rolü üstlenir.

### Sorumluluklar

| # | Sorumluluk |
|---|-----------|
| R1 | APPROVED VisualAssetPlan'dan gelen generation intent'i provider-agnostic request contract'ına dönüştürmek |
| R2 | Provider seçimini ve provider-specific payload detaylarını **internal boundary** içinde tutmak |
| R3 | Raw provider payload/response **expose etmemek** |
| R4 | Provider output'unu güvenli, normalize edilmiş bir result contract'a çevirmek |
| R5 | GeneratedAsset / MediaAsset engineering layer'a **safe handoff** yapılmasını sağlamak |
| R6 | Normalized error contract üretmek |

### Sorumluluk Dışı

| # | AI Platform'un yapmadığı |
|---|--------------------------|
| X1 | Human review kararı vermez |
| X2 | AI output'u APPROVED yapmaz |
| X3 | Publish/export status üretmez |
| X4 | READY_FOR_PUBLISH status'ü kullanmaz |
| X5 | Generic status update sağlamaz |
| X6 | Storage persistence'ı doğrudan yönetmez (handoff contract verir) |
| X7 | Generated image'ı otomatik onaylamaz |

### Sınır Diyagramı

```
Engineering Layer (Service)
    │
    ▼
AI Platform Boundary
    │
    ├── Input: ImageGenerationRequest (provider-agnostic)
    │
    ├── Internal: Provider Adapter (OpenAI / Replicate / Mock / ...)
    │       │
    │       ├── Provider-specific request construction ──→ Provider API
    │       └── Provider-specific response parsing     ←── Provider API
    │
    ├── Output: ImageGenerationResult (normalized, safe)
    │
    └── Handoff: StorageHandoffPayload → Engineering Layer
```

---

## 3. Input Boundary

### Kaynak

AI Platform'a gelen input **yalnızca APPROVED VisualAssetPlan** kaynağından türetilmiş olabilir. Draft, NEEDS_REVIEW veya REJECTED plan input olarak kabul edilmez.

### Conceptual Input Contract'lar

#### ImageGenerationIntent

Bir VisualAssetPlan'ın tüm sahne ve thumbnail planları için toplu generation niyetini ifade eder.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `intentId` | UUID | Benzersiz intent tanımlayıcı |
| `visualAssetPlanId` | UUID | Kaynak APPROVED plan |
| `contentProjectId` | UUID | Bağlı content project |
| `items` | List | Üretilecek image item listesi |
| `requestedBy` | String | Talebi başlatan admin/system |
| `requestedAt` | Timestamp | Talep zamanı |

#### ImageGenerationRequest

Tek bir sahne veya thumbnail görseli için provider-agnostic generation request'i.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `requestId` | UUID | Benzersiz request tanımlayıcı |
| `intentId` | UUID | Bağlı intent |
| `sourceVisualAssetPlanId` | UUID | Kaynak plan |
| `sourceVisualAssetItemId` | UUID | Kaynak SceneVisualPlan veya ThumbnailPlan ID |
| `itemType` | Enum | `SCENE_IMAGE` veya `THUMBNAIL_IMAGE` |
| `promptInstructions` | PromptInstructionBundle | Güvenli prompt paketi |
| `generationConstraints` | GenerationConstraints | Üretim kısıtları |
| `idempotencyKey` | String | İdempotency garantisi için key |
| `requestedBy` | String | Talebi başlatan |
| `requestedAt` | Timestamp | Talep zamanı |

#### PromptInstructionBundle

Provider'a iletilecek prompt bilgilerinin güvenli paketi.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `primaryPrompt` | String | englishImagePrompt — ana image generation prompt |
| `negativePrompt` | String | negativePrompt — istenmeyen elementler |
| `styleDirective` | String | Stil yönlendirmesi |
| `compositionNotes` | String | Kompozisyon yönlendirmesi |
| `cameraFraming` | String | Kamera açısı / framing |
| `lightingNotes` | String | Işık yönlendirmesi |
| `colorPalette` | String | Renk paleti |
| `culturalContext` | String | Kültürel bağlam notu |
| `safetyDirective` | String | Güvenlik yönlendirmesi |

#### GenerationConstraints

| Alan | Tip | Açıklama |
|------|-----|----------|
| `aspectRatio` | String | Hedef en-boy oranı (ör. "16:9", "1:1") |
| `targetWidth` | Integer | Hedef genişlik (px), opsiyonel |
| `targetHeight` | Integer | Hedef yükseklik (px), opsiyonel |
| `outputFormat` | String | Tercih edilen format (ör. "png", "webp") |
| `qualityLevel` | String | Kalite seviyesi (ör. "standard", "hd") |
| `safetyLevel` | String | Güvenlik seviyesi |

### Input Kuralları

| Kural | Açıklama |
|-------|----------|
| Provider-agnostic olmalı | Request contract hiçbir provider SDK alanına bağımlı değil |
| Internal prompt admin/user-facing olmamalı | Orchestration prompt expose edilmez |
| Allowlist yaklaşımı | Prompt üretimi için kullanılacak alanlar açıkça listelenir |
| Raw VisualAssetPlan dump yasak | Tüm plan alanları provider'a aktarılmaz, filtrelenir |
| Sensitive/internal fields filtrelenir | Provider-internal veya governance alanları dışarıda kalır |
| Secret input contract'ta bulunmaz | API key, token, credential hiçbir input'ta yer almaz |

---

## 4. Provider-Agnostic Request Contract

### Contract Özeti

Provider-agnostic request, AI Platform internal boundary'de provider adapter tarafından provider-specific payload'a dönüştürülür. Bu dönüşüm **tamamen internal**'dır ve dış katmanlara expose edilmez.

### Conceptual Request Fields

```
ImageGenerationRequest
├── requestId              : UUID         — benzersiz request id
├── intentId               : UUID         — bağlı intent
├── sourceVisualAssetPlanId: UUID         — kaynak plan
├── sourceVisualAssetItemId: UUID         — kaynak scene/thumbnail
├── contentProjectId       : UUID         — bağlı content project
├── itemType               : Enum         — SCENE_IMAGE | THUMBNAIL_IMAGE
├── promptInstructions     : Bundle       — güvenli prompt paketi
├── generationConstraints  : Constraints  — boyut, format, kalite
├── idempotencyKey         : String       — idempotency garantisi
├── requestedBy            : String       — talebi başlatan
└── requestedAt            : Timestamp    — talep zamanı
```

### Request Contract Kuralları

| Kural | Açıklama |
|-------|----------|
| Conceptual contract alanlarıdır | Java class tasarımı Engineering Design'da yapılacak |
| Provider SDK payload ile birebir eşleşmek zorunda değil | Adapter dönüşüm yapar |
| Provider-specific parametreler admin-facing değil | Internal adapter boundary |
| `generationCount` küçük ve kontrollü | Bulk generation dışarıda |
| Regeneration/variation akışı dışarıda | FP-007'de yok |
| API key, secret, raw provider config yer almaz | Güvenlik kuralı |

---

## 5. Prompt Safety / Redaction Strategy

### Prompt Katmanları

AI Platform'da prompt bilgisi üç katmanda ele alınır:

```
┌─────────────────────────────────────────────────────┐
│  Layer 1: Product Prompt (ürün çıktısı)             │
│  englishImagePrompt, negativePrompt, styleDirective  │
│  ──→ Admin/UI'da görülebilir                        │
├─────────────────────────────────────────────────────┤
│  Layer 2: System/Orchestration Prompt (internal)     │
│  Provider'a giden system instruction, safety prefix  │
│  ──→ Admin/UI'da GÖSTERİLMEZ                       │
├─────────────────────────────────────────────────────┤
│  Layer 3: Provider-Specific Payload (internal)       │
│  SDK body, API parameters, signed URLs               │
│  ──→ Admin/UI'da GÖSTERİLMEZ, loglanmaz            │
└─────────────────────────────────────────────────────┘
```

### SafePromptSummary

Review ve audit amaçlı güvenli bir prompt özeti oluşturulabilir.

| Alan | Açıklama |
|------|----------|
| `primaryPromptPreview` | englishImagePrompt metninin kendisi — zaten ürün çıktısı |
| `negativePromptPreview` | negativePrompt metninin kendisi — zaten ürün çıktısı |
| `styleHint` | styleDirective özeti |
| `constraintsSummary` | Boyut, format, kalite özeti |

> **Kural**: SafePromptSummary, Layer 1 (ürün çıktısı) alanlarından türetilir. Layer 2 ve Layer 3 bilgisi hiçbir zaman SafePromptSummary'ye dahil edilmez.

### Redaction Kuralları

| # | Kural |
|---|-------|
| P1 | Internal prompt (system instruction) admin/user'a gösterilmez |
| P2 | Provider'a giden prompt ile sistemde tutulacak safe summary ayrıdır |
| P3 | SafePromptSummary yalnızca review/audit için güvenli özetdir |
| P4 | Raw prompt saklanacaksa internal-only olmalı; product/admin yüzeyine çıkmamalı |
| P5 | Prompt input'a dahil edilecek alanlar allowlist ile belirlenir |
| P6 | Raw provider payload loglanmaz |
| P7 | Prompt içinde secret, token, internal config, provider key, raw system instruction bulunmaz |
| P8 | Redaction policy Engineering aşamasında enforce edilecek şekilde tanımlanır |

---

## 6. Provider Boundary

### Provider Adapter Mimarisi

Provider adapter, AI Platform içinde **internal boundary** bileşenidir. Product/Admin UI katmanı provider adapter detaylarına erişemez.

```
AI Platform Gateway (interface)
    │
    ├── MockImageGenerationProvider      — deterministic, test/staging
    ├── OpenAiImageGenerationProvider    — DALL-E 3 / gpt-image
    ├── ReplicateImageGenerationProvider — gelecek, opsiyonel
    └── StabilityImageGenerationProvider — gelecek, opsiyonel
```

### Provider Metadata Allowlist

Admin-facing yüzeylerde güvenle gösterilebilecek provider metadata alanları:

| Alan | Visibility | Açıklama |
|------|-----------|----------|
| `providerType` | ✅ Safe (internal/summary) | MOCK, OPENAI, REPLICATE vb. |
| `modelAlias` | ✅ Safe (internal/summary) | dall-e-3, sdxl vb. — friendly alias |
| `generationDurationMs` | ✅ Safe metadata | Üretim süresi |
| `normalizedErrorCode` | ✅ Safe | Normalize edilmiş hata kodu |
| `normalizedErrorMessage` | ✅ Safe | Normalize edilmiş hata mesajı |

### Provider Metadata Yasaklar

Admin-facing yüzeylerde **kesinlikle gösterilmeyecek** provider bilgileri:

| Alan | Visibility | Açıklama |
|------|-----------|----------|
| `apiKey` | ❌ Yasak | Provider API anahtarı |
| `secret` | ❌ Yasak | Herhangi bir secret/credential |
| `rawRequestPayload` | ❌ Yasak | Provider'a gönderilen HTTP body |
| `rawResponsePayload` | ❌ Yasak | Provider'dan dönen HTTP body |
| `providerRequestId` | ❌ Yasak (internal-only) | Provider tarafı request ID |
| `signedTemporaryUrl` | ❌ Yasak | Provider'ın geçici signed URL'i |
| `fullInternalPrompt` | ❌ Yasak | System/orchestration prompt |
| `stackTrace` | ❌ Yasak | Hata stack trace |
| `rawProviderErrorDump` | ❌ Yasak | Provider'ın raw hata yanıtı |

### Provider Boundary Kuralları

| Kural | Açıklama |
|-------|----------|
| Provider adapter internal boundary'dir | Product/Admin UI erişemez |
| Engineering layer provider SDK detayına bağımlı olmamalı | Interface abstraction |
| Provider request ID internal metadata olabilir | Expose edilmez |
| Provider model name/version allowlist ile tutulabilir | `modelAlias` güvenli |
| Provider raw error dump gösterilmez | Normalized error kullanılır |
| Provider response normalize edilir | Raw data dönüştürülür |

---

## 7. Normalized Response Contract

### ImageGenerationResult

Provider'dan dönen sonucun normalize edilmiş, güvenli contract'ı.

```
ImageGenerationResult
├── resultId                  : UUID         — benzersiz result id
├── requestId                 : UUID         — bağlı request
├── sourceVisualAssetPlanId   : UUID         — kaynak plan
├── sourceVisualAssetItemId   : UUID         — kaynak scene/thumbnail
├── status                    : Enum         — GENERATED_PENDING_REVIEW | FAILED
├── generatedOutput           : GeneratedOutput (nullable)
├── safePromptSummary         : SafePromptSummary
├── providerMetadataSummary   : ProviderMetadataSummary (allowlist)
├── errorCode                 : String (nullable)
├── errorMessage              : String (nullable)
└── completedAt               : Timestamp
```

### GeneratedOutput

Başarılı generation durumunda üretilen çıktının normalize edilmiş metadata'sı.

```
GeneratedOutput
├── outputId                  : UUID         — benzersiz output id
├── temporaryInternalRef      : String       — internal geçici storage/location referansı
├── mimeType                  : String       — image/png, image/webp vb.
├── width                     : Integer      — piksel genişlik
├── height                    : Integer      — piksel yükseklik
├── fileSizeBytes             : Long         — dosya boyutu
├── checksumSha256            : String       — bütünlük doğrulaması
├── safetyFlags               : List<String> — normalized güvenlik flag'leri
├── qualitySignals            : Map          — kalite sinyalleri (opsiyonel)
└── storageCandidate          : Boolean      — storage'a aktarılabilir mi
```

### ProviderMetadataSummary

Allowlist ile filtrelenmiş, güvenli provider özeti.

```
ProviderMetadataSummary
├── providerType              : String       — MOCK, OPENAI, REPLICATE
├── modelAlias                : String       — dall-e-3, sdxl
└── generationDurationMs      : Long         — üretim süresi (ms)
```

### Response Contract Kuralları

| Kural | Açıklama |
|-------|----------|
| Raw provider response yok | Normalize edilmiş contract kullanılır |
| Raw payload yok | Provider HTTP body expose edilmez |
| Signed URL admin-facing değil | `temporaryInternalRef` internal-only |
| Output henüz APPROVED değil | `GENERATED_PENDING_REVIEW` olarak döner |
| APPROVED kararı AI Platform tarafından verilmez | Human review gerekli |
| FAILED durumunda output null olabilir | Hata bilgisi error alanlarında |

---

## 8. Error Normalization

### Normalized Error Kategorileri

| Error Code | Açıklama |
|-----------|----------|
| `PROVIDER_UNAVAILABLE` | Provider erişilemez durumda |
| `PROVIDER_TIMEOUT` | Provider yanıt süresi aşıldı |
| `PROVIDER_REJECTED_REQUEST` | Provider request'i reddetti (format/kota vb.) |
| `PROVIDER_SAFETY_BLOCKED` | Provider safety policy'si engelledi |
| `INVALID_GENERATION_INPUT` | Geçersiz input (eksik prompt, geçersiz boyut vb.) |
| `STORAGE_HANDOFF_FAILED` | Üretilen görsel storage'a aktarılamadı |
| `UNKNOWN_GENERATION_FAILURE` | Sınıflandırılamayan hata |

### Error Contract

```
NormalizedError
├── errorCode       : String    — yukarıdaki kategorilerden biri
├── errorMessage    : String    — admin-friendly kısa mesaj
├── correlationId   : UUID      — internal correlation/debugging id
└── occurredAt      : Timestamp — hata zamanı
```

### Error Kuralları

| Kural | Açıklama |
|-------|----------|
| Raw provider error dump gösterilmez | Normalize edilmiş error code/message kullanılır |
| Stack trace expose edilmez | Internal logging ile sınırlı |
| Admin-facing error güvenli ve kısa | Kullanıcı dostu mesaj |
| Correlation ID debugging için kullanılabilir | Secret/payload içermez |
| Retry policy bu taskta detaylandırılmaz | Engineering Design'a bırakılır |
| FAILED output human approval'a aday olmaz | Reddedilmiş/başarısız asset onaylanamaz |

---

## 9. GeneratedAsset / MediaAsset Handoff Contract

### Handoff Sınırı

AI Platform, GeneratedAsset entity'si **yaratma kararını vermez**. Bunun yerine, Engineering layer'a güvenli bir handoff contract sağlar.

```
AI Platform                              Engineering Layer
    │                                         │
    │  ImageGenerationResult                  │
    │  ├── generatedOutput                    │
    │  │   ├── temporaryInternalRef ─────────→│ Storage'a kalıcı kayıt
    │  │   ├── mimeType              ─────────→│ MediaAsset metadata
    │  │   ├── width/height          ─────────→│ MediaAsset metadata
    │  │   ├── checksumSha256        ─────────→│ MediaAsset bütünlük
    │  │   └── storageCandidate      ─────────→│ Storage eligibility
    │  ├── sourceVisualAssetPlanId   ─────────→│ GeneratedAsset lineage
    │  ├── sourceVisualAssetItemId   ─────────→│ GeneratedAsset lineage
    │  ├── requestId                 ─────────→│ GeneratedAsset audit
    │  ├── resultId                  ─────────→│ GeneratedAsset audit
    │  └── safePromptSummary         ─────────→│ GeneratedAsset review info
    │                                         │
    └─────────────────────────────────────────┘
```

### Handoff Kuralları

| Kural | Açıklama |
|-------|----------|
| AI Platform GeneratedAsset yaratma kararını vermez | Engineering layer persistence tasarlar |
| AI Platform storage'a doğrudan sahip olmak zorunda değil | Storage handoff contract verebilir |
| GeneratedAsset başlangıçta `GENERATED_PENDING_REVIEW` olmalı | Otomatik APPROVED yasak |
| MediaAsset storage metadata güvenli allowlist ile oluşmalı | mime, dimensions, checksum |
| Lineage zorunlu | Plan, item, project, request, result ID'leri korunmalı |

### Zorunlu Lineage Alanları

| Lineage Alan | Açıklama |
|-------------|----------|
| `visualAssetPlanId` | Kaynak APPROVED plan |
| `visualAssetItemId` | Kaynak SceneVisualPlan veya ThumbnailPlan |
| `contentProjectId` | Bağlı content project |
| `requestId` | Generation request ID |
| `resultId` | Generation result ID |

---

## 10. Status Mapping

### Product Status ↔ AI Platform Sinyalleri

| Product Status | AI Platform Dokunur mu? | Açıklama |
|----------------|------------------------|----------|
| `REQUESTED` | ✅ Evet | Request accepted/created sinyali |
| `GENERATING` | ✅ Evet | Provider generation in progress sinyali |
| `GENERATED_PENDING_REVIEW` | ✅ Evet | Başarılı generation, output ready for human review |
| `FAILED` | ✅ Evet | Normalized failure sinyali |
| `APPROVED` | ❌ Hayır | Sadece human review sonrası — AI Platform dokunmaz |
| `REJECTED` | ❌ Hayır | Sadece human review/admin decision — AI Platform dokunmaz |

### Status Kuralları

| Kural | Açıklama |
|-------|----------|
| AI Platform yalnızca `REQUESTED → GENERATING → GENERATED_PENDING_REVIEW` veya `FAILED` geçişlerini üretir | Otomatik APPROVED yok |
| APPROVED kararı sadece human review sonrası olur | Engineering layer / review service |
| REJECTED kararı sadece human/admin decision sonrası olur | Engineering layer / review service |
| AI output otomatik APPROVED olamaz | Product Design kuralı |
| `READY_FOR_PUBLISH` hiçbir yerde kullanılmaz | ❌ Yasak |
| Generic status update yoktur | ❌ Yasak |

---

## 11. Security / Privacy / Governance Rules

### Açık Yasak Listesi

| # | Yasak |
|---|-------|
| Y1 | No API key exposure |
| Y2 | No secret exposure |
| Y3 | No raw provider request payload exposure |
| Y4 | No raw provider response payload exposure |
| Y5 | No internal/system prompt exposure |
| Y6 | No stack trace exposure |
| Y7 | No signed temporary URL exposure to admin/user-facing surfaces |
| Y8 | No generic status update |
| Y9 | No automatic approval |
| Y10 | No publishing status (`READY_FOR_PUBLISH` yasak) |

### Zorunlu Governance Kuralları

| # | Kural |
|---|-------|
| G1 | **Allowlist metadata** — admin'e yalnızca güvenli metadata gösterilir |
| G2 | **Redacted prompt summary** — SafePromptSummary yalnızca Layer 1 alanlarından türetilir |
| G3 | **Lineage preservation** — her generated asset'in kaynak plan/item/project ilişkisi korunur |
| G4 | **Audit-friendly request/result IDs** — izlenebilir, secret içermeyen ID'ler |
| G5 | **Provider abstraction** — engineering layer provider SDK detayına bağımlı olmaz |
| G6 | **Safe normalized errors** — hata bilgisi güvenli error code/message ile temsil edilir |
| G7 | **Human review gate** — `GENERATED_PENDING_REVIEW` zorunlu ara aşamadır |

---

## 12. Non-Goals

Bu AI Platform Contract Design taskının kapsamında **kesinlikle olmayan** konular:

| # | Kapsam Dışı |
|---|-------------|
| NG1 | Real provider SDK integration (OpenAI, Replicate, Stability vb.) |
| NG2 | Database schema design |
| NG3 | Storage bucket/object lifecycle implementation |
| NG4 | Controller/API endpoint design |
| NG5 | Admin UI design |
| NG6 | Human review implementation |
| NG7 | Video/audio generation |
| NG8 | Publishing/export automation |
| NG9 | Bulk generation |
| NG10 | Regeneration/variation flow |
| NG11 | Multi-provider failover |
| NG12 | Cost optimization |
| NG13 | CDN optimization |
| NG14 | Image editing/cropping tools |
| NG15 | Moderation automation beyond normalized safety flags |

---

## 13. Acceptance Criteria

| # | Kriter |
|---|--------|
| AC1 | AI Platform contract design dokümanı hedef dosyada mevcut |
| AC2 | Product Design kaynağı referans verilmiş |
| AC3 | Provider-agnostic request contract tanımlanmış |
| AC4 | Normalized response contract tanımlanmış |
| AC5 | Prompt safety/redaction strategy tanımlanmış |
| AC6 | Provider metadata allowlist tanımlanmış |
| AC7 | Raw provider payload/response exposure açıkça yasaklanmış |
| AC8 | API key/secret exposure açıkça yasaklanmış |
| AC9 | GeneratedAsset/MediaAsset handoff contract tanımlanmış |
| AC10 | Status mapping tanımlanmış |
| AC11 | APPROVED/REJECTED AI Platform automated transition dışında |
| AC12 | Human review gate korunmuş |
| AC13 | READY_FOR_PUBLISH ve generic status update dışarıda bırakılmış |
| AC14 | Scope documentation-only |

---

## 14. Future Engineering Handoff — FP-007 Task 03

Bu AI Platform Contract Design tamamlandıktan sonra, **FP-007 Task 03** olarak Engineering Design & Task Breakdown dokümanı oluşturulacaktır.

### Engineering Dokümanında Ele Alınacak Konular

| # | Konu | Açıklama |
|---|------|----------|
| E1 | Domain model proposal | `ImageGenerationRequest`, `GeneratedAsset`, `MediaAsset` entity tasarımı |
| E2 | Repository/persistence boundaries | JPA entity/repository ayrımı |
| E3 | Storage reference design | MinIO/S3 object lifecycle, bucket strategy |
| E4 | Review workflow design | Human review service, approve/reject akışı |
| E5 | Safe admin API design | REST endpoint'ler, DTO'lar, güvenlik sınırları |
| E6 | Endpoint naming | Generic status update olmadan aksiyon bazlı endpoint'ler |
| E7 | Idempotency strategy | Request idempotency key implementasyonu |
| E8 | Provider adapter interface | Gateway interface + mock/real adapter tasarımı |
| E9 | Error persistence | Normalized error kayıt stratejisi |
| E10 | Audit fields | who/when/lineage audit alanları |
| E11 | Tests and migration plan | Unit/integration test stratejisi + Flyway migration planı |

### Task Sıralaması

```
Task 01 — Product Design                  ✅ DONE
Task 02 — AI Platform Contract Design     ✅ DONE (bu doküman)
Task 03 — Engineering Design & Task Breakdown (sonraki)
Task 04+ — Implementation tasks
```

---

## Document History

| Versiyon | Tarih | Açıklama |
|----------|-------|----------|
| v1.0 | 2026-07-04 | FP-007 AI Platform contract design — documentation-only task |
