# 56 — FP-007 Image Generation & Media Asset Pipeline — Engineering Design & Task Breakdown

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-007 — Image Generation & Media Asset Pipeline
>
> **Task**: Task 03 — Engineering Design & Task Breakdown (documentation-only)
>
> **Prerequisites**: FP-007 Task 01 Product Design (DONE), FP-007 Task 02 AI Platform Contract Design (DONE)
>
> **Source Documents**:
> - [54 — Product Design](54-fp007-image-generation-media-asset-pipeline-product-design.md)
> - [55 — AI Platform Contract Design](55-fp007-image-generation-media-asset-pipeline-ai-platform-contract-design.md)

---

## 1. Status / Scope

| Alan | Değer |
|------|-------|
| **Task** | FP-007 Task 03 — Engineering Design & Task Breakdown |
| **Tür** | Documentation-only |
| **Kaynak** | FP-007 Task 01 Product Design + Task 02 AI Platform Contract Design |
| **Kod değişikliği** | ❌ Yok |
| **Runtime değişikliği** | ❌ Yok |
| **Migration** | ❌ Yok |
| **Provider implementation** | ❌ Bu taskta yapılmaz |
| **Hedef** | Sonraki coding tasklar için engineering baseline sağlamak |

Bu doküman, FP-007 Product Design ve AI Platform Contract Design dokümanlarına dayanarak **engineering seviyesinde domain model, persistence, service boundaries, safe admin API, provider adapter, storage reference, review workflow, audit/error/idempotency yaklaşımı ve coding task breakdown** tasarımını içerir.

Gerçek kod, migration, provider SDK, storage implementation veya test bu taskta **yapılmaz**.

---

## 2. Engineering Objectives

| # | Hedef |
|---|-------|
| O1 | APPROVED VisualAssetPlan üzerinden image generation request başlatılabilmesi |
| O2 | ImageGenerationRequest kayıtlarının izlenebilir (traceable) olması |
| O3 | Provider adapter boundary'nin gerçek provider entegrasyonuna hazır ama güvenli olması |
| O4 | Normalized provider result'ın GeneratedAsset / MediaAsset kayıtlarına dönüştürülebilmesi |
| O5 | Generated output'un başlangıçta `GENERATED_PENDING_REVIEW` olması |
| O6 | Human review approve/reject workflow'unun tasarlanması |
| O7 | Storage reference ve media metadata'nın güvenli şekilde tutulması |
| O8 | Secret/raw payload/raw response/internal prompt exposure'ın engellenmesi |
| O9 | `READY_FOR_PUBLISH` ve generic status update yaklaşımının engellenmesi |

---

## 3. Proposed Domain Model

### 3.1 ImageGenerationRequest

Bir SceneVisualPlan veya ThumbnailPlan için oluşturulan image generation talebi.

| Alan | Tip | Admin-Facing | Açıklama |
|------|-----|-------------|----------|
| `id` | UUID | ✅ | PK — BaseEntity |
| `contentProjectId` | UUID | ✅ | Lineage — bağlı content project |
| `visualAssetPlanId` | UUID | ✅ | Lineage — kaynak APPROVED plan |
| `visualAssetItemId` | UUID | ✅ | Lineage — kaynak SceneVisualPlan veya ThumbnailPlan |
| `itemType` | Enum | ✅ | `SCENE_IMAGE` veya `THUMBNAIL_IMAGE` |
| `status` | Enum | ✅ | REQUESTED / GENERATING / COMPLETED / FAILED |
| `idempotencyKey` | String | ❌ | Duplicate generation engelleme |
| `providerType` | String | ✅ (safe) | MOCK, OPENAI vb. |
| `modelName` | String | ✅ (safe) | dall-e-3, sdxl vb. |
| `safePromptSummary` | String | ✅ (redacted) | Redacted prompt özeti |
| `errorCode` | String | ✅ (safe) | Normalized hata kodu |
| `errorMessage` | String | ✅ (safe) | Normalized hata mesajı |
| `correlationId` | UUID | ❌ | Internal observability |
| `requestedBy` | String | ✅ | Talebi başlatan |
| `createdAt` | Timestamp | ✅ | JPA auditing |
| `updatedAt` | Timestamp | ✅ | JPA auditing |
| `completedAt` | Timestamp | ✅ | Üretim tamamlanma zamanı |

**Güvenlik**: `rawProviderRequestPayload`, `rawProviderResponsePayload`, `fullInternalPrompt`, `apiKey`, `secret` alanları **yoktur**.

### 3.2 GeneratedAsset

AI provider tarafından üretilmiş, review bekleyen veya review edilmiş üretim çıktısı.

| Alan | Tip | Admin-Facing | Açıklama |
|------|-----|-------------|----------|
| `id` | UUID | ✅ | PK — BaseEntity |
| `imageGenerationRequestId` | UUID | ✅ | Lineage — bağlı request |
| `contentProjectId` | UUID | ✅ | Lineage — content project |
| `visualAssetPlanId` | UUID | ✅ | Lineage — plan |
| `visualAssetItemId` | UUID | ✅ | Lineage — scene/thumbnail |
| `mediaAssetId` | UUID | ✅ | İlişkili MediaAsset |
| `status` | Enum | ✅ | GENERATED_PENDING_REVIEW / APPROVED / REJECTED |
| `reviewedBy` | String | ✅ | Reviewer |
| `reviewedAt` | Timestamp | ✅ | Review zamanı |
| `rejectionReason` | String | ✅ | Red gerekçesi |
| `createdAt` | Timestamp | ✅ | JPA auditing |
| `updatedAt` | Timestamp | ✅ | JPA auditing |

**Güvenlik**: Raw provider veya prompt alanı yoktur. Lineage alanları zorunludur.

### 3.3 MediaAsset

Sistemde saklanan medya dosyasının metadata ve storage referansı.

| Alan | Tip | Admin-Facing | Açıklama |
|------|-----|-------------|----------|
| `id` | UUID | ✅ | PK — BaseEntity |
| `storageKey` | String | ❌ (internal) | Storage object key/path |
| `storageBucket` | String | ❌ (internal) | Storage bucket adı |
| `originalFileName` | String | ✅ | Orijinal dosya adı |
| `mimeType` | String | ✅ | image/png, image/webp vb. |
| `width` | Integer | ✅ | Piksel genişlik |
| `height` | Integer | ✅ | Piksel yükseklik |
| `fileSizeBytes` | Long | ✅ | Dosya boyutu |
| `checksumSha256` | String | ✅ | Bütünlük doğrulaması |
| `assetType` | Enum | ✅ | AI_GENERATED vb. |
| `createdAt` | Timestamp | ✅ | JPA auditing |
| `updatedAt` | Timestamp | ✅ | JPA auditing |

**Güvenlik**: `storageKey` ve `storageBucket` admin-facing API response'a dahil **edilmez**. Public URL otomatik üretilmez. Signed temporary URL kalıcı metadata olarak saklanmaz.

### 3.4 Entity İlişkileri

```
ContentProject
    │
    ▼
VisualAssetPlan (APPROVED) ──── FP-006'dan miras
    │
    ├── SceneVisualPlan ────→ ImageGenerationRequest (1:1 veya 1:N)
    │                              │
    │                              ├── GeneratedAsset ──→ MediaAsset
    │                              │
    └── ThumbnailPlan ─────→ ImageGenerationRequest (1:1 veya 1:N)
                                   │
                                   └── GeneratedAsset ──→ MediaAsset
```

### 3.5 Zorunlu Lineage Alanları

Her katmanda korunması gereken izlenebilirlik zinciri:

```
contentProjectId
  └── visualAssetPlanId
        └── visualAssetItemId (SceneVisualPlan veya ThumbnailPlan)
              └── imageGenerationRequestId
                    └── generatedAssetId
                          └── mediaAssetId
```

---

## 4. Status Model

### 4.1 ImageGenerationRequest Status

```
REQUESTED ──→ GENERATING ──→ COMPLETED ──→ (GeneratedAsset oluşur)
                  │
                  ▼
                FAILED
```

| Status | Açıklama | AI Platform tarafından üretilebilir? |
|--------|----------|-------------------------------------|
| `REQUESTED` | Talep oluşturuldu, provider'a henüz gönderilmedi | ✅ |
| `GENERATING` | Provider'a gönderildi, yanıt bekleniyor | ✅ |
| `COMPLETED` | Provider başarılı sonuç döndü, GeneratedAsset oluşturulacak | ✅ |
| `FAILED` | Provider/storage hatası, normalized error kaydedildi | ✅ |

### 4.2 GeneratedAsset Status

```
GENERATED_PENDING_REVIEW ──→ APPROVED
         │
         ▼
      REJECTED
```

| Status | Açıklama | Kim tarafından? |
|--------|----------|----------------|
| `GENERATED_PENDING_REVIEW` | Üretildi, human review bekliyor | System (otomatik) |
| `APPROVED` | Admin tarafından onaylandı | Admin (human review) |
| `REJECTED` | Admin tarafından reddedildi | Admin (human review) |

### 4.3 Status Kuralları

| Kural | Açıklama |
|-------|----------|
| AI output otomatik APPROVED olamaz | `GENERATED_PENDING_REVIEW` zorunlu ara aşama |
| APPROVED sadece human review sonrası | Admin onayı gerekli |
| REJECTED sadece human review sonrası | Admin kararı gerekli |
| FAILED asset approve adayı olamaz | Başarısız üretim onaylanamaz |
| `READY_FOR_PUBLISH` kullanılmaz | ❌ FP-007'de bu status yok |
| Generic status update endpoint yok | ❌ Explicit action endpoint'ler |
| Status transition explicit action odaklı | approve/reject/generate endpoint'leri |

---

## 5. Persistence / Repository Boundaries

### 5.1 Repository Önerileri

| Repository | Açıklama |
|-----------|----------|
| `ImageGenerationRequestRepository` | Generation request CRUD + status sorgulama |
| `GeneratedAssetRepository` | Generated asset CRUD + review queue |
| `MediaAssetRepository` | Media storage metadata CRUD |

### 5.2 Temel Sorgu Gereksinimleri

| Sorgu | Açıklama |
|-------|----------|
| Lookup by VisualAssetPlan ID | Bir plana ait tüm generation request'leri |
| Lookup by VisualAssetItem ID | Bir sahne/thumbnail için request/asset |
| Lookup by status | Status bazlı filtreleme (ör. GENERATED_PENDING_REVIEW) |
| Review queue | Pending review asset listesi |
| Idempotency key check | Duplicate generation engelleme |
| Lookup by ContentProject ID | Proje bazlı tüm generated asset'ler |

### 5.3 Idempotency Key Stratejisi

```
idempotencyKey = hash(visualAssetPlanId + visualAssetItemId + itemType)
```

Aynı VisualAssetItem için aynı type'ta aktif bir `REQUESTED` veya `GENERATING` request varsa yeni request oluşturulmaz; mevcut request döndürülür veya safe conflict response verilir.

### 5.4 Audit Fields

Tüm entity'lerde JPA auditing:

- `createdAt` — `@CreatedDate`
- `updatedAt` — `@LastModifiedDate`
- `requestedBy` / `reviewedBy` — admin identity
- `correlationId` — internal observability

### 5.5 Persistence Güvenlik Kuralları

| Kural | Açıklama |
|-------|----------|
| `rawProviderRequestPayload` column yok | Provider request body persist edilmez |
| `rawProviderResponsePayload` column yok | Provider response body persist edilmez |
| `apiKey` / `secret` column yok | Credential persist edilmez |
| `fullInternalPrompt` column yok | System/orchestration prompt persist edilmez |
| `safePromptSummary` sınırlı/redacted | Yalnızca Layer 1 (ürün çıktısı) bilgisi |
| `providerMetadata` allowlist summary | providerType + modelName + durationMs |

---

## 6. Storage Reference Design

### 6.1 MediaAsset Storage Yaklaşımı

MediaAsset, gerçek dosyanın storage reference'ını tutar. Storage implementation (MinIO/S3) ayrı bir concern'dür.

### 6.2 Storage Alanları

| Alan | Visibility | Açıklama |
|------|-----------|----------|
| `storageKey` | ❌ Internal | Object key/path |
| `storageBucket` | ❌ Internal | Bucket adı |
| `mimeType` | ✅ Admin-facing | image/png, image/webp |
| `width` / `height` | ✅ Admin-facing | Piksel boyutları |
| `fileSizeBytes` | ✅ Admin-facing | Dosya boyutu |
| `checksumSha256` | ✅ Admin-facing | Bütünlük hash |

### 6.3 Storage Kuralları

| Kural | Açıklama |
|-------|----------|
| Public URL otomatik üretilmez | Generated image public olarak sunulmaz |
| Signed temporary URL kalıcı metadata olarak saklanmaz | Geçici review preview için kullanılabilir |
| Storage bucket/key admin-facing API response'ta yok | Internal storage detayı |
| Storage lifecycle ve CDN optimization kapsam dışı | FP-007'de yok |
| Generated image publish edilmez | Sadece admin review preview |
| Preview serve mekanizması | Backend proxy endpoint veya short-lived signed URL |

### 6.4 Preview Serve Stratejisi

Admin'in generated image'ı review ederken görmesi için:

- **Seçenek A**: Backend proxy endpoint — `GET /api/admin/generated-assets/{id}/preview` backend üzerinden serve
- **Seçenek B**: Short-lived signed URL — backend kısa ömürlü URL üretir, admin kullanır

Her iki durumda da URL kalıcı metadata olarak persist edilmez. Seçenek A tercih edilir çünkü storage bilgisi dışarı çıkmaz.

---

## 7. Provider Adapter Boundary

### 7.1 Gateway Interface

```
ImageGenerationProviderGateway (interface)
    │
    ├── MockImageGenerationProvider      — deterministic, test/staging
    ├── OpenAiImageGenerationProvider    — DALL-E 3 / gpt-image-1
    └── (gelecek provider'lar)
```

### 7.2 Sorumluluklar

| Sorumluluk | Açıklama |
|-----------|----------|
| Request dönüşümü | Provider-agnostic request → provider-specific internal call |
| Response normalizasyonu | Provider response → normalized result contract |
| Payload izolasyonu | Raw payload/response dışarı verilmez |
| Error normalizasyonu | Provider hatası → normalized error code/message |
| Secret izolasyonu | Secrets sadece server-side config'den (env variable) |

### 7.3 Provider Adapter Kuralları

| Kural | Açıklama |
|-------|----------|
| SDK implementation coding tasklarda gelir | Bu dokümanda implementation yok |
| Provider config/secrets domain model/API response'a girmez | Server-side only |
| Provider-specific raw payload loglanmaz | Structured safe logging |
| Provider error stack trace admin-facing olmaz | Normalized error |
| Provider adapter output'u normalized contract'tır | ImageGenerationResult |
| Mock provider deterministic olur | Test ve staging doğrulaması |

### 7.4 Mock Provider Davranışı

| Mod | Açıklama |
|-----|----------|
| `SUCCESS` | Deterministic fake image bytes + metadata döndürür |
| `TIMEOUT` | Simulated timeout hatası |
| `SAFETY_BLOCKED` | Simulated safety policy hatası |
| `PROVIDER_ERROR` | Simulated generic provider hatası |

Mock provider gerçek external API çağrısı **yapmaz**. Test ve staging ortamlarında güvenli doğrulama sağlar.

---

## 8. Service Boundary Design

### 8.1 ImageGenerationRequestService

| Özellik | Değer |
|---------|-------|
| **Sorumluluk** | Generation request oluşturma, idempotency kontrolü |
| **Girdi** | APPROVED VisualAssetPlan ID, item bilgisi |
| **Çıktı** | ImageGenerationRequest (status: REQUESTED) |
| **Validation** | VisualAssetPlan APPROVED mi kontrol |
| **Idempotency** | Aynı item için aktif request varsa conflict/existing döndür |
| **Güvenlik** | Raw payload yok, safe prompt summary üretilir |

### 8.2 ImageGenerationOrchestrationService

| Özellik | Değer |
|---------|-------|
| **Sorumluluk** | Request → provider gateway → result → handoff orchestration |
| **Girdi** | ImageGenerationRequest |
| **Çıktı** | COMPLETED request + GeneratedAsset + MediaAsset; veya FAILED request |
| **İş akışı** | REQUESTED → GENERATING → provider call → COMPLETED/FAILED |
| **Transaction** | Provider call dışarıda, persist işlemleri transactional |
| **Güvenlik** | Provider adapter üzerinden normalize edilmiş result alır |

### 8.3 GeneratedAssetService

| Özellik | Değer |
|---------|-------|
| **Sorumluluk** | GeneratedAsset yaratma, sorgulama |
| **Girdi** | Normalized provider result + lineage bilgisi |
| **Çıktı** | GeneratedAsset (status: GENERATED_PENDING_REVIEW) |
| **Güvenlik** | Raw provider data persist etmez; lineage alanları zorunlu |

### 8.4 MediaAssetService

| Özellik | Değer |
|---------|-------|
| **Sorumluluk** | MediaAsset kaydı oluşturma, storage metadata yönetimi |
| **Girdi** | Storage handoff bilgisi (key, bucket, mime, dimensions, checksum) |
| **Çıktı** | MediaAsset kaydı |
| **Güvenlik** | Storage key/bucket admin-facing olmaz |

### 8.5 GeneratedAssetReviewService

| Özellik | Değer |
|---------|-------|
| **Sorumluluk** | Approve/reject workflow |
| **Girdi** | GeneratedAsset ID + approve/reject kararı |
| **Çıktı** | Updated GeneratedAsset (APPROVED veya REJECTED) |
| **Validation** | Yalnızca GENERATED_PENDING_REVIEW status'ten approve/reject |
| **Reject** | Rejection reason **zorunlu** |
| **Audit** | reviewedBy, reviewedAt, rejectionReason kaydedilir |
| **Güvenlik** | Generic status update method yok; explicit approve/reject |

### 8.6 MediaStorageService

| Özellik | Değer |
|---------|-------|
| **Sorumluluk** | Provider'dan gelen image data'yı storage'a kaydetme |
| **Girdi** | Image bytes/stream + metadata |
| **Çıktı** | Storage reference (key, bucket, checksum) |
| **Güvenlik** | Storage credentials internal; admin-facing değil |

---

## 9. Safe Admin API Design

### 9.1 Önerilen Endpoint'ler

| Method | Path | Açıklama |
|--------|------|----------|
| `POST` | `/api/admin/visual-asset-plans/{planId}/generate-images` | APPROVED plan için image generation başlat |
| `GET` | `/api/admin/visual-asset-plans/{planId}/generation-requests` | Plana ait generation request'leri listele |
| `GET` | `/api/admin/content-projects/{projectId}/generated-assets` | Projeye ait generated asset'leri listele |
| `GET` | `/api/admin/generated-assets/pending-review` | Review bekleyen asset'leri listele |
| `GET` | `/api/admin/generated-assets/{assetId}` | Generated asset detayı (safe metadata + lineage) |
| `GET` | `/api/admin/generated-assets/{assetId}/preview` | Generated image preview (backend proxy) |
| `POST` | `/api/admin/generated-assets/{assetId}/approve` | Generated asset'i onayla |
| `POST` | `/api/admin/generated-assets/{assetId}/reject` | Generated asset'i reddet (reason zorunlu) |

### 9.2 Olmayan Endpoint'ler

| Yasak Endpoint | Neden |
|----------------|-------|
| `PATCH /api/admin/generated-assets/{id}/status` | ❌ Generic status update yok |
| `POST .../publish` | ❌ Publishing yok |
| `POST .../export` | ❌ Export automation yok |
| `POST .../schedule` | ❌ Scheduling yok |
| `GET .../raw-provider-response` | ❌ Raw payload yok |
| `GET .../internal-prompt` | ❌ Internal prompt yok |

### 9.3 API Response Kuralları

| Alan | Admin API'de var mı? | Açıklama |
|------|----------------------|----------|
| Generated asset metadata | ✅ | status, lineage, review info |
| Preview reference | ✅ | Backend proxy veya short-lived URL |
| Media dimensions/mime/size | ✅ | Güvenli metadata |
| Provider type / model name | ✅ | Safe summary |
| Safe prompt summary | ✅ | Redacted Layer 1 özeti |
| Safe error code/message | ✅ | Normalized hata |
| Raw provider request | ❌ | Yasak |
| Raw provider response | ❌ | Yasak |
| Internal prompt | ❌ | Yasak |
| Storage key/bucket | ❌ | Yasak |
| API key/secret | ❌ | Yasak |
| Stack trace | ❌ | Yasak |

---

## 10. Review Workflow Design

### 10.1 Akış

```
GeneratedAsset (GENERATED_PENDING_REVIEW)
    │
    ▼
Admin: Pending Review Queue'dan asset seçer
    │
    ▼
Admin: Preview image + safe metadata + lineage görür
    │
    ├── Admin: Approve
    │     │
    │     ▼
    │   GeneratedAsset → APPROVED
    │   (reviewedBy, reviewedAt kaydedilir)
    │
    └── Admin: Reject
          │
          ▼
        GeneratedAsset → REJECTED
        (reviewedBy, reviewedAt, rejectionReason kaydedilir)
```

### 10.2 Review Kuralları

| Kural | Açıklama |
|-------|----------|
| Her generated image human review'dan geçer | Zorunlu |
| Approve yalnızca `GENERATED_PENDING_REVIEW`'dan | Diğer status'lerden geçiş yok |
| Reject yalnızca `GENERATED_PENDING_REVIEW`'dan | Diğer status'lerden geçiş yok |
| Rejection reason zorunlu | Boş reason 400 Bad Request |
| Review decision auditlenir | who, when, reason |
| Review olmadan production/export/publish adayı olamaz | Gate korunur |
| FP-007'de publish/export yapılmaz | Yalnızca approve/reject |
| APPROVED edit → tekrar GENERATED_PENDING_REVIEW yapılmaz | FP-007'de re-review yok |

### 10.3 Review Decision Alanları

| Alan | Açıklama |
|------|----------|
| `reviewedBy` | Reviewer admin identity |
| `reviewedAt` | Review timestamp |
| `rejectionReason` | Red gerekçesi (reject durumunda zorunlu) |

---

## 11. Error Handling Design

### 11.1 Normalized Error Kategorileri

| Error Code | Açıklama |
|-----------|----------|
| `PROVIDER_UNAVAILABLE` | Provider erişilemez |
| `PROVIDER_TIMEOUT` | Provider yanıt süresi aşıldı |
| `PROVIDER_REJECTED_REQUEST` | Provider request reddetti |
| `PROVIDER_SAFETY_BLOCKED` | Provider safety policy engelledi |
| `INVALID_GENERATION_INPUT` | Geçersiz input |
| `STORAGE_HANDOFF_FAILED` | Storage'a aktarım hatası |
| `UNKNOWN_GENERATION_FAILURE` | Sınıflandırılamayan hata |

### 11.2 Error Kuralları

| Kural | Açıklama |
|-------|----------|
| Raw provider error dump yok | Normalized code/message |
| Stack trace admin-facing yok | Internal logging only |
| Error code/message safe | Kullanıcı dostu |
| Correlation ID debugging için | Secret/payload içermez |
| FAILED generation review/approval adayı olmaz | Başarısız asset onaylanamaz |
| Retry policy conceptual not | Advanced retry FP-007'de büyütülmez |

### 11.3 Retry Notu

İlk iterasyonda retry otomatik yapılmaz. Admin, başarısız bir plan için yeni generation başlatabilir. Advanced retry/exponential backoff gelecek feature'dır.

---

## 12. Idempotency and Duplicate Prevention

### 12.1 Idempotency Key

```
idempotencyKey = sha256(visualAssetPlanId + ":" + visualAssetItemId + ":" + itemType)
```

### 12.2 Duplicate Davranışı

| Durum | Davranış |
|-------|----------|
| Aynı item için aktif `REQUESTED` veya `GENERATING` request var | Mevcut request döndürülür (200 OK) |
| Aynı item için `COMPLETED` request var | Mevcut GeneratedAsset döndürülür (200 OK) |
| Aynı item için `FAILED` request var | Yeni request oluşturulabilir |
| Bulk generation | ❌ FP-007'de yok |
| Regeneration/variation | ❌ FP-007'de yok |

### 12.3 Kapsam Dışı

- Bulk generation (toplu üretim) FP-007'de **yoktur**.
- Regeneration/variation akışı FP-007'de **yoktur**.
- İleri seviye idempotency token management gelecek feature'dır.

---

## 13. Audit and Observability

### 13.1 Audit Alanları

| Alan | Entity | Açıklama |
|------|--------|----------|
| `createdAt` | Tüm entity'ler | Oluşturulma zamanı |
| `updatedAt` | Tüm entity'ler | Güncellenme zamanı |
| `requestedBy` | ImageGenerationRequest | Talebi başlatan |
| `requestedAt` | ImageGenerationRequest | Talep zamanı |
| `reviewedBy` | GeneratedAsset | Reviewer identity |
| `reviewedAt` | GeneratedAsset | Review zamanı |
| `rejectionReason` | GeneratedAsset | Red gerekçesi |
| `completedAt` | ImageGenerationRequest | Tamamlanma zamanı |
| `correlationId` | ImageGenerationRequest | Internal correlation |

### 13.2 Observability Kuralları

| Kural | Açıklama |
|-------|----------|
| Metrics/logging safe metadata ile | Güvenli alanlar loglanır |
| Raw provider payload/response loglanmaz | Provider internal |
| Prompt loglanmaz | Privacy/security |
| Secret/config loglanmaz | Credential güvenliği |
| Correlation ID güvenli | Debug için kullanılabilir |
| Generation duration loglanabilir | Performans metrikleri |
| Status transition loglanabilir | Workflow audit |

---

## 14. Security / Privacy / Governance Rules

### 14.1 Açık Yasak Listesi

| # | Yasak |
|---|-------|
| Y1 | No API key exposure |
| Y2 | No secret exposure |
| Y3 | No raw provider request persistence |
| Y4 | No raw provider response persistence |
| Y5 | No raw provider payload in API response |
| Y6 | No internal prompt in API response |
| Y7 | No stack trace admin-facing |
| Y8 | No signed provider URL as persistent public metadata |
| Y9 | No generic status update endpoint |
| Y10 | No automatic approval |
| Y11 | No `READY_FOR_PUBLISH` |
| Y12 | No publishing/export automation |

### 14.2 Zorunlu Governance Kuralları

| # | Kural |
|---|-------|
| G1 | **Metadata allowlist** — admin'e yalnızca güvenli metadata |
| G2 | **SafePromptSummary redaction** — Layer 1 alanlarından türetilir |
| G3 | **Provider abstraction** — engineering layer SDK detayına bağımlı olmaz |
| G4 | **Human review gate** — GENERATED_PENDING_REVIEW zorunlu ara aşama |
| G5 | **Lineage preservation** — plan/item/project/request/result ID'leri korunur |
| G6 | **Audit trail** — who/when/reason kaydedilir |
| G7 | **Explicit action endpoints** — approve/reject/generate, generic update yok |

---

## 15. Non-Goals

| # | Kapsam Dışı |
|---|-------------|
| NG1 | Real provider SDK integration |
| NG2 | Actual image generation code |
| NG3 | Database migration creation |
| NG4 | Storage bucket/object implementation |
| NG5 | Admin frontend implementation |
| NG6 | Public media gallery |
| NG7 | User-facing generation UI |
| NG8 | Publishing/export automation |
| NG9 | `READY_FOR_PUBLISH` transition |
| NG10 | Generic status update endpoint |
| NG11 | Video generation |
| NG12 | Audio generation |
| NG13 | Bulk generation |
| NG14 | Regeneration/variation flow |
| NG15 | Multi-provider failover |
| NG16 | Cost optimization |
| NG17 | CDN optimization |
| NG18 | Image editing/cropping tools |
| NG19 | Advanced moderation automation |

---

## 16. Coding Task Breakdown

### Task 04 — Domain Enums and Model Skeletons

| Özellik | Değer |
|---------|-------|
| **Amaç** | FP-007 status enum'ları ve entity skeleton'ları |
| **Scope** | Enum + entity class'ları (boş veya minimal), BaseEntity miras |
| **Dosya/dizin** | `imagegen/entity/`, `imagegen/entity/` altında enum + entity |
| **Provider integration** | ❌ Yok |
| **Migration** | ❌ Task 05'e bırakılır |
| **Güvenlik** | Raw payload/prompt/secret alanı olmayan entity'ler |

**Acceptance Criteria:**
- ImageGenerationRequestStatus enum mevcut (REQUESTED, GENERATING, COMPLETED, FAILED)
- GeneratedAssetStatus enum mevcut (GENERATED_PENDING_REVIEW, APPROVED, REJECTED)
- ImageGenerationRequest entity skeleton mevcut (lineage + safe alanlar)
- GeneratedAsset entity skeleton mevcut (lineage + review alanlar)
- MediaAsset entity skeleton mevcut (storage ref + safe metadata)
- Raw payload/response/secret/prompt alanı **yok**
- READY_FOR_PUBLISH **yok**

---

### Task 05 — Persistence Migration and Repositories

| Özellik | Değer |
|---------|-------|
| **Amaç** | Flyway migration + JPA repository'ler |
| **Scope** | V9 migration, repository interface'leri |
| **Dosya/dizin** | `db/migration/V9__*`, `imagegen/repository/` |
| **Güvenlik** | Raw payload/response column **yok**, idempotency key column mevcut |

**Acceptance Criteria:**
- V9 migration image_generation_requests, generated_assets, media_assets tabloları oluşturur
- rawProviderRequestPayload/rawProviderResponsePayload/apiKey/secret/fullInternalPrompt column **yok**
- Idempotency key column mevcut
- Audit fields (created_at, updated_at) mevcut
- Repository interface'leri mevcut
- PostgreSQL 12 uyumu korunur

---

### Task 06 — Safe DTOs and Admin API Contract

| Özellik | Değer |
|---------|-------|
| **Amaç** | Request/response DTO'lar, controller endpoint tanımları |
| **Scope** | DTO class'ları + controller skeleton'ları |
| **Dosya/dizin** | `imagegen/controller/`, `imagegen/controller/dto/` |
| **Güvenlik** | Safe metadata only, explicit action endpoint'ler |

**Acceptance Criteria:**
- DTO'larda raw payload/response/internal prompt/secret alanı **yok**
- Generic status update endpoint **yok**
- Explicit action endpoint'ler: generate, approve, reject, list, detail, preview
- Reject endpoint'te reason zorunlu
- READY_FOR_PUBLISH endpoint **yok**

---

### Task 07 — Generation Request Service

| Özellik | Değer |
|---------|-------|
| **Amaç** | Generation request oluşturma servisi |
| **Scope** | APPROVED plan validation + idempotency + REQUESTED status |
| **Dosya/dizin** | `imagegen/service/` |
| **Provider call** | ❌ Henüz yok — sadece request oluşturma |

**Acceptance Criteria:**
- VisualAssetPlan APPROVED değilse generation başlatılamaz
- Idempotency key ile duplicate request engellenir
- ImageGenerationRequest status: REQUESTED olarak oluşturulur
- Lineage alanları zorunlu ve doldurulur
- SafePromptSummary üretilir (Layer 1 alanlarından)
- Raw payload/secret persist edilmez

---

### Task 08 — Provider Gateway Interface and Mock Provider

| Özellik | Değer |
|---------|-------|
| **Amaç** | Provider-agnostic gateway interface + mock/fake provider |
| **Scope** | Gateway interface + MockImageGenerationProvider |
| **Dosya/dizin** | `ai/gateway/`, `ai/gateway/mock/` |
| **Real provider** | ❌ Henüz yok |

**Acceptance Criteria:**
- ImageGenerationProviderGateway interface mevcut
- MockImageGenerationProvider deterministic sonuç döndürür
- Mock provider SUCCESS/TIMEOUT/SAFETY_BLOCKED/PROVIDER_ERROR modları
- Normalized result contract (ImageGenerationResult) döndürülür
- Raw payload/response expose edilmez
- Mock provider external API çağrısı yapmaz

---

### Task 09 — GeneratedAsset, MediaAsset and Storage Handoff

| Özellik | Değer |
|---------|-------|
| **Amaç** | Normalized output → GeneratedAsset + MediaAsset kaydı |
| **Scope** | Handoff service + storage service (mock/local) |
| **Dosya/dizin** | `imagegen/service/`, `media/service/` |
| **Storage** | MinIO/local mock — basit storage handoff |

**Acceptance Criteria:**
- Normalized provider output'tan GeneratedAsset oluşturulur
- GeneratedAsset başlangıç status: GENERATED_PENDING_REVIEW
- MediaAsset storage reference + safe metadata ile oluşturulur
- Lineage alanları korunur
- Storage key/bucket admin-facing API'de **yok**
- Otomatik APPROVED **yok**

---

### Task 10 — Human Review Workflow

| Özellik | Değer |
|---------|-------|
| **Amaç** | Approve/reject review service + endpoint'ler |
| **Scope** | GeneratedAssetReviewService + controller endpoint'ler |
| **Dosya/dizin** | `imagegen/service/`, `imagegen/controller/` |

**Acceptance Criteria:**
- Pending review queue sorgulama çalışır
- Approve: GENERATED_PENDING_REVIEW → APPROVED
- Reject: GENERATED_PENDING_REVIEW → REJECTED
- Rejection reason zorunlu, boş ise 400
- reviewedBy, reviewedAt, rejectionReason kaydedilir
- FAILED asset approve/reject adayı olmaz
- Generic status update endpoint **yok**

---

### Task 11 — Safe Error Handling and Observability

| Özellik | Değer |
|---------|-------|
| **Amaç** | Normalized error handling + safe logging |
| **Scope** | Error normalization + @ControllerAdvice + safe logging |
| **Dosya/dizin** | `imagegen/exception/`, `imagegen/service/` |

**Acceptance Criteria:**
- Normalized error code/message FAILED request'lerde kaydedilir
- Stack trace admin-facing **yok**
- Raw payload/response loglanmaz
- Prompt loglanmaz
- Secret loglanmaz
- Correlation ID mevcut
- Admin-facing error mesajları güvenli ve kısa

---

### Task 12 — Controlled Real Provider Integration

| Özellik | Değer |
|---------|-------|
| **Amaç** | Gerçek image generation provider (OpenAI DALL-E veya benzeri) |
| **Scope** | OpenAI Image API adapter + server-side secrets |
| **Prerequisite** | Task 08 mock/gateway boundary stabil |
| **Dosya/dizin** | `ai/gateway/openai/` |

**Acceptance Criteria:**
- Server-side secrets only (env variable)
- Raw payload/response admin-facing **yok**
- Normalized result contract döndürülür
- Small generation count (tek image per request)
- Human review gate korunur
- Prompt/secret loglanmaz
- Mock provider fallback çalışır

---

### Task 13 — Admin UI Integration

| Özellik | Değer |
|---------|-------|
| **Amaç** | Admin web'de FP-007 generation + review workflow |
| **Scope** | Frontend API client + UI component'ler |
| **Dosya/dizin** | `admin-web/src/api/`, `admin-web/src/components/imagegen/` |

**Acceptance Criteria:**
- Generate images butonu (APPROVED plan'da)
- Generation status takibi
- Pending review queue
- Image preview (backend proxy)
- Approve/reject butonları
- Safe metadata görüntüleme
- Raw payload/response/internal prompt **yok**
- Publish/schedule butonu **yok**

---

### Task 14 — Frontend Tests & Build Validation

| Özellik | Değer |
|---------|-------|
| **Amaç** | Frontend test + TypeScript + build doğrulaması |
| **Scope** | Vitest, tsc, vite build |

---

### Task 15 — Backend Tests & Build Validation

| Özellik | Değer |
|---------|-------|
| **Amaç** | Backend unit/integration test + Maven build doğrulaması |
| **Scope** | JUnit, MockMvc, Maven surefire |

---

### Task 16 — Local Smoke Test

| Özellik | Değer |
|---------|-------|
| **Amaç** | Local ortamda end-to-end smoke test |
| **Scope** | MOCK provider ile full workflow |
| **Validation only** | Kod değişikliği gerekmeyebilir |

---

### Task 17 — Staging Validation

| Özellik | Değer |
|---------|-------|
| **Amaç** | Staging ortamda full validation |
| **Scope** | MOCK provider ile staging smoke test |
| **Validation only** | Kod değişikliği gerekmeyebilir |

---

### Task 18 — FP-007 Final Closure & Operational Baseline

| Özellik | Değer |
|---------|-------|
| **Amaç** | FP-007 closure dokümanı |
| **Scope** | Documentation-only |
| **Tür** | Final closure + operational baseline |

---

## 17. Acceptance Criteria

| # | Kriter |
|---|--------|
| AC1 | Engineering Design doc hedef dosyada mevcut |
| AC2 | Product ve AI Platform dokümanları referans verilmiş |
| AC3 | Domain model proposal mevcut |
| AC4 | Status model mevcut ve READY_FOR_PUBLISH dışarıda |
| AC5 | GeneratedAsset otomatik APPROVED olamaz |
| AC6 | Human review workflow zorunlu |
| AC7 | Safe admin API design generic status update içermiyor |
| AC8 | Provider adapter boundary tanımlı |
| AC9 | Storage reference design tanımlı |
| AC10 | Persistence kuralları raw payload/response/secret yasaklamış |
| AC11 | Prompt exposure yasaklanmış |
| AC12 | Error normalization design mevcut |
| AC13 | Idempotency strategy mevcut |
| AC14 | Audit/observability kuralları mevcut |
| AC15 | Coding task breakdown mevcut |
| AC16 | Scope documentation-only |

---

## 18. Future Coding Handoff — FP-007 Task 04

Bu Engineering Design dokümanı tamamlandıktan sonra, **FP-007 Task 04 — Domain Enums and Model Skeletons** kodlama aşamasına geçilebilir.

### Task 04 Kapsamı

- Kod başlayacak **ilk task**
- Yalnızca enum'lar ve entity skeleton'ları
- Provider integration **yok**
- Admin API **yok**
- Storage implementation **yok**
- Migration Task 05'e bırakılır
- Security boundaries bu dokümandaki kurallarla korunur

### Task Sıralaması

```
Task 01 — Product Design                       ✅ DONE
Task 02 — AI Platform Contract Design          ✅ DONE
Task 03 — Engineering Design & Task Breakdown  ✅ DONE (bu doküman)
Task 04 — Domain Enums and Model Skeletons     (sonraki — coding başlar)
Task 05 — Persistence Migration and Repositories
Task 06 — Safe DTOs and Admin API Contract
Task 07 — Generation Request Service
Task 08 — Provider Gateway Interface and Mock Provider
Task 09 — GeneratedAsset, MediaAsset and Storage Handoff
Task 10 — Human Review Workflow
Task 11 — Safe Error Handling and Observability
Task 12 — Controlled Real Provider Integration
Task 13 — Admin UI Integration
Task 14 — Frontend Tests & Build Validation
Task 15 — Backend Tests & Build Validation
Task 16 — Local Smoke Test
Task 17 — Staging Validation
Task 18 — FP-007 Final Closure & Operational Baseline
```

---

## Document History

| Versiyon | Tarih | Açıklama |
|----------|-------|----------|
| v1.0 | 2026-07-04 | FP-007 Engineering Design & Task Breakdown — documentation-only task |
