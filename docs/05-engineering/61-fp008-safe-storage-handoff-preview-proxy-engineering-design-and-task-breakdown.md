# FP-008 — Safe Storage Handoff & Preview Proxy — Engineering Design & Task Breakdown

> **Feature**: FP-008 — Safe Storage Handoff & Preview Proxy  
> **Status**: Draft  
> **Task**: FP-008 Task 03 — Engineering Design & Task Breakdown  
> **Scope**: Documentation-only  
> **Product Design**: [59-fp008-safe-storage-handoff-preview-proxy-product-design.md](../03-product-design/59-fp008-safe-storage-handoff-preview-proxy-product-design.md)  
> **AI Platform / Media Contract**: [60-fp008-safe-storage-handoff-preview-proxy-ai-platform-media-contract-design.md](../04-ai-platform/60-fp008-safe-storage-handoff-preview-proxy-ai-platform-media-contract-design.md)  
> **FP-007 Closure**: [58-fp007-final-closure-operational-baseline.md](./58-fp007-final-closure-operational-baseline.md)  
> **FP-007 Storage Planning**: [57-fp007-safe-provider-output-storage-handoff-planning.md](./57-fp007-safe-provider-output-storage-handoff-planning.md)

---

## 1. Status / Scope

- FP-008 Task 03, **Engineering Design & Task Breakdown** dokümanıdır
- **Documentation-only** — kod, migration, runtime değişikliği yoktur
- Product Design ve AI Platform / Media Contract Design dokümanlarına dayanır
- FP-007 closure baseline referans alınır
- Bu doküman sonraki coding tasklar için engineering baseline sağlar

---

## 2. Engineering Objectives

| # | Hedef |
|---|-------|
| 1 | FP-007'de 501 kalan preview/storage boşluğunu güvenli şekilde kapatmak |
| 2 | Provider output reference'ı internal boundary içinde işlemek |
| 3 | Server-side storage handoff başlatmak |
| 4 | MIME/size/dimension/checksum validation yapmak |
| 5 | `ImageMediaAsset` safe metadata'yı storage sonrası güncellemek |
| 6 | Backend-controlled preview proxy tasarlamak |
| 7 | Admin response'larda storage key, provider URL, signed/public URL sızıntısını engellemek |
| 8 | Human review gate'i korumak |
| 9 | READY_FOR_PUBLISH / generic status update / publish-export dışarıda tutmak |

---

## 3. Proposed Domain / Model Changes

### 3.1 Mevcut FP-007 Entity Yapısı

| Entity | Tablo | Açıklama |
|--------|-------|----------|
| `ImageGenerationRequest` | `image_generation_requests` | Generation isteği |
| `ImageGenerationResult` | `image_generation_results` | Provider sonucu |
| `ImageGeneratedAsset` | `fp007_generated_assets` | Review-gated asset |
| `ImageMediaAsset` | `fp007_image_media_assets` | Media metadata |
| `GeneratedAssetReview` | `fp007_generated_asset_reviews` | Review audit |

### 3.2 Yeni Enum: `ImageStorageHandoffStatus`

Storage handoff lifecycle durumu.

| Değer | Anlamı |
|-------|--------|
| `STORAGE_PENDING` | Handoff henüz başlamadı veya devam ediyor |
| `STORAGE_VALIDATING` | MIME/size/dimension validation sürüyor |
| `STORAGE_STORED` | Başarıyla depolandı, preview hazır |
| `STORAGE_FAILED` | Handoff başarısız |

- Package: `com.anadoluatlasi.backend.imagegen.entity`
- Admin-facing: ✅ (safe enum string olarak)
- GeneratedAsset review status ile ilişkisi yok — bağımsız concern

### 3.3 ImageMediaAsset Genişletme

Mevcut `ImageMediaAsset` entity'sine FP-008 storage handoff alanları eklenmesi önerilir:

| Yeni Alan | Tip | Açıklama | Admin DTO? |
|-----------|-----|----------|------------|
| `storageHandoffStatus` | `ImageStorageHandoffStatus` | Handoff durumu | ✅ |
| `storedAt` | `LocalDateTime` | Storage tamamlanma zamanı | ✅ |
| `storageErrorCode` | `String` | Normalized hata kodu | ✅ |
| `storageErrorMessage` | `String` | Safe error message | ✅ |

Mevcut internal-only alanlar (zaten entity'de var):

| Alan | Admin DTO? | Not |
|------|------------|-----|
| `storageProvider` | ⛔ Internal-only | Zaten mevcut |
| `storageBucket` | ⛔ Internal-only | Zaten mevcut |
| `storageObjectKey` | ⛔ Internal-only | Zaten mevcut |

**Ayrı tablo kararı**: `ImageMediaAsset` entity'sine inline ekleme tercih edilir. Ayrı `ImageStorageHandoffRecord` tablosu gereksiz karmaşıklık ekler — storage handoff 1:1 media asset ilişkisidir.

### 3.4 ImageMediaAssetResponse DTO Genişletme

Mevcut DTO'ya eklenecek safe alanlar:

| Yeni Alan | Tip | Açıklama |
|-----------|-----|----------|
| `storageHandoffStatus` | `String` | STORAGE_PENDING / STORED / FAILED |
| `previewAvailable` | `boolean` | Derived: status == STORAGE_STORED |
| `storedAt` | `LocalDateTime` | Storage completion timestamp |
| `storageErrorCode` | `String` | Normalized error code (null if success) |
| `storageErrorMessage` | `String` | Safe error message (null if success) |

Kesinlikle eklenmeyecek alanlar:

- `storageObjectKey`, `storageBucket`, `storageProvider`
- `publicUrl`, `signedUrl`, `cdnUrl`
- `providerUrl`, `providerOutputReference`
- `base64`, `imageBinary`
- `rawProviderResponse`, `rawStorageResponse`

### 3.5 FP-008 Error Code Enum: `ImageStorageErrorCode`

| Değer | Tetikleyen |
|-------|------------|
| `PROVIDER_OUTPUT_UNAVAILABLE` | Provider output reference erişilemez |
| `PROVIDER_OUTPUT_EXPIRED` | Provider output TTL dolmuş |
| `PROVIDER_OUTPUT_DOWNLOAD_FAILED` | Download network/IO error |
| `MEDIA_VALIDATION_FAILED` | Genel validation hatası |
| `UNSUPPORTED_MEDIA_TYPE` | MIME type allowlist dışında |
| `MEDIA_TOO_LARGE` | Dosya boyutu limiti aşıldı |
| `MEDIA_DIMENSION_INVALID` | Width/height sınır dışı |
| `CHECKSUM_CALCULATION_FAILED` | Hash hesaplanamadı |
| `STORAGE_WRITE_FAILED` | Object store'a yazma başarısız |
| `STORAGE_READ_FAILED` | Object store'dan okuma başarısız |
| `PREVIEW_NOT_AVAILABLE` | Preview hazır değil |
| `PREVIEW_ACCESS_DENIED` | Authorization başarısız |
| `UNKNOWN_STORAGE_HANDOFF_FAILURE` | Sınıflandırılamayan hata |

- Package: `com.anadoluatlasi.backend.imagegen.entity`
- Admin DTO'da: safe error code string olarak
- Raw error dump: ⛔ yasak

### 3.6 Preview Audit (Opsiyonel)

Preview erişim audit'i basit log-based tutulabilir — ayrı entity/tablo ilk fazda gereksiz. Eğer gelecekte detaylı audit gerekirse ayrı `PreviewAccessAuditLog` entity tasarlanır.

İlk faz audit: structured log with `generatedAssetId`, `mediaAssetId`, `accessedBy`, `accessedAt`, `correlationId`.

---

## 4. Status Model

### GeneratedAsset Status (FP-007 — Korunur, Değişmez)

```
GENERATED_PENDING_REVIEW → APPROVED  (explicit admin approve)
GENERATED_PENDING_REVIEW → REJECTED  (explicit admin reject)
```

### Storage Handoff Status (FP-008 — Yeni)

```
STORAGE_PENDING → STORAGE_VALIDATING → STORAGE_STORED  (success)
STORAGE_PENDING → STORAGE_VALIDATING → STORAGE_FAILED  (validation failure)
STORAGE_PENDING → STORAGE_FAILED                       (download failure)
```

### Preview Availability (Derived — Computed, Not Persisted)

```
storageHandoffStatus == STORAGE_STORED  → previewAvailable = true
storageHandoffStatus != STORAGE_STORED  → previewAvailable = false
```

### Kesin Kurallar

| Kural | Açıklama |
|-------|----------|
| Storage status, review status **değildir** | Bağımsız concern |
| Preview status, review status **değildir** | Bağımsız concern |
| Storage success → automatic APPROVED **değildir** | Human review gate |
| Preview available → automatic APPROVED **değildir** | Human review gate |
| `READY_FOR_PUBLISH` **kullanılmaz** | Scope dışı |
| `COMPLETED` **kullanılmaz** | Scope dışı |
| Generic status update endpoint **tasarlanmaz** | Güvenlik tercihi |
| Publish/export status **yok** | Scope dışı |

---

## 5. Persistence / Migration Design

### Planlanan Migration: `V11__fp008_storage_handoff_fields.sql`

`fp007_image_media_assets` tablosuna eklenecek kolonlar:

```
storage_handoff_status  VARCHAR(30)    DEFAULT 'STORAGE_PENDING'
stored_at               TIMESTAMP
storage_error_code      VARCHAR(60)
storage_error_message   TEXT
```

### Index/Constraint Önerileri

| Index | Hedef |
|-------|-------|
| `idx_media_asset_handoff_status` | `storage_handoff_status` — pending/failed query |
| `idx_media_asset_stored_at` | `stored_at` — timeline query |

### Persistence Yasakları

| Yasak | Açıklama |
|-------|----------|
| Provider image URL kalıcı kolon | ⛔ |
| Signed URL kalıcı kolon | ⛔ |
| Public URL kalıcı kolon | ⛔ |
| Base64 kolon | ⛔ |
| Binary/BLOB kolon | ⛔ |
| Raw provider payload/response kolon | ⛔ |
| Raw storage provider response kolon | ⛔ |
| apiKey/secret/config kolon | ⛔ |
| Internal full prompt kolon | ⛔ |
| READY_FOR_PUBLISH kolon | ⛔ |
| Publish/export status kolon | ⛔ |

### Mevcut Alanlar Yeterli

`ImageMediaAsset` entity zaten `storageProvider`, `storageBucket`, `storageObjectKey`, `mimeType`, `width`, `height`, `fileSizeBytes`, `checksumSha256` alanlarına sahip. FP-008 bu alanları populate edecek — yeni storage kolon eklenmesi gerekmez.

---

## 6. Provider Output Retrieval Boundary

### Tasarım

Provider output reference (URL veya identifier), `ImageGenerationResult` veya orchestration context'ten alınır ve **yalnızca server-side** storage handoff service tarafından kullanılır.

### Önerilen İç Yapı

```
ImageStorageHandoffService
  │
  │  providerOutputReference (internal, from result/context)
  │
  ▼
ProviderOutputRetriever (internal helper)
  │  server-side HTTP download
  │  timeout + size limit
  │  redirect handling
  │  content-type sniffing
  │
  ▼
byte[] / InputStream (temp, not persisted)
```

### Kurallar

| Kural | Açıklama |
|-------|----------|
| Provider output reference sadece internal input | Admin/user DTO'ya çıkmaz |
| Provider URL **loglanmaz** | Safe metadata loglanır |
| Provider URL **DTO'ya dönmez** | Internal-only |
| Provider URL **MediaAsset metadata olarak saklanmaz** | Geçici reference |
| Server-side download | Client/admin download yapmaz |
| Timeout/size limit | DoS koruması |
| Redirect handling güvenli | Infinite redirect koruması |
| Content-type **trust edilmez** | Actual content validation yapılır |
| Base64 varsa decode çok kontrollü | URL/download stream tercih |
| Raw provider response **saklanmaz** | Safe metadata only |

### Provider Output Erişim Notu

FP-007 `OpenAiImageGenerationProviderGateway` şu an provider URL'i extract etmiyor (metadata-only). FP-008'de provider output reference'ı `ImageProviderGenerationResult`'a opsiyonel internal-only field olarak eklemek veya orchestration context'te taşımak gerekebilir. Bu karar Task 08 (Storage Handoff Service) sırasında finalize edilir.

---

## 7. Media Validation Service Design

### Önerilen Service: `ImageMediaValidationService`

Package: `com.anadoluatlasi.backend.imagegen.service`

### Sorumluluklar

| Sorumluluk | Açıklama |
|------------|----------|
| MIME allowlist kontrolü | image/png, image/jpeg, image/webp |
| Content sniffing | Magic bytes ile actual image verification |
| Max file size check | Configurable limit — aşarsa rejection |
| Width/height extraction | ImageIO veya metadata reader |
| Zero-byte rejection | Boş dosya reddedilir |
| Corrupt image rejection | Okunamayan/bozuk image reddedilir |
| Checksum calculation | SHA-256 hash |
| Safe validation error üretimi | Normalized error code + message |

### Validation Output Contract

```java
// Conceptual — not real code
ValidationResult {
    boolean passed;
    String detectedMimeType;
    int width;
    int height;
    long fileSizeBytes;
    String checksumSha256;
    ImageStorageErrorCode errorCode;    // null if passed
    String safeErrorMessage;            // null if passed
}
```

### Kurallar

| Kural | Açıklama |
|-------|----------|
| Validation failure GeneratedAsset'i otomatik REJECTED **yapmaz** | Human review gate |
| Validation failure storage handoff failure'dır | STORAGE_FAILED |
| Human review gate **korunur** | Bağımsız concern |
| Raw binary **loglanmaz** | Güvenlik |

---

## 8. Storage Adapter Boundary

### Önerilen Interface: `ImageStorageAdapter`

Package: `com.anadoluatlasi.backend.imagegen.storage`

### Sorumluluklar

| Sorumluluk | Açıklama |
|------------|----------|
| Validated image stream/object alır | Input: validated bytes + metadata |
| Storage object key server-side üretir | UUID-based, deterministic |
| Dosyayı private/internal storage'a yazar | Provider-agnostic |
| Storage metadata döner | objectKey, bucket, provider alias |
| Preview için storage'dan okuma | Stream döner |

### Interface Tasarımı

```java
// Conceptual — not real code
interface ImageStorageAdapter {
    StorageWriteResult store(StorageWriteRequest request);
    InputStream read(String storageObjectKey);
    String getProviderAlias();
}
```

### Üretmez / Döndürmez

| Yasak | Açıklama |
|-------|----------|
| Public URL | ⛔ |
| Signed URL | ⛔ |
| CDN URL | ⛔ |
| Storage key admin-facing | ⛔ — internal-only |

### Mock Storage Stratejisi

| Faz | Strateji |
|-----|----------|
| İlk implementation | `MockImageStorageAdapter` — in-memory veya temp file |
| Default config | `imagegen.storage.mode=mock` |
| Real storage | Ayrı task ile kontrollü eklenir — S3/MinIO/GCS/local |

---

## 9. Storage Handoff Service Design

### Önerilen Service: `ImageStorageHandoffService`

Package: `com.anadoluatlasi.backend.imagegen.service`

### Orchestration Flow

```
ImageStorageHandoffService.executeHandoff(generatedAssetId)
  │
  ├── 1. Load GeneratedAsset + MediaAsset
  ├── 2. Validate storage handoff eligibility
  │      └── storageHandoffStatus == STORAGE_PENDING
  ├── 3. Update status → STORAGE_VALIDATING
  ├── 4. Retrieve provider output (internal)
  │      └── ProviderOutputRetriever.download(reference)
  ├── 5. Validate media
  │      └── ImageMediaValidationService.validate(bytes)
  ├── 6. Store to object store
  │      └── ImageStorageAdapter.store(validated bytes)
  ├── 7. Update MediaAsset safe metadata
  │      ├── storageProvider, storageBucket, storageObjectKey (internal)
  │      ├── mimeType, width, height, fileSizeBytes, checksumSha256 (safe)
  │      ├── storageHandoffStatus → STORAGE_STORED
  │      └── storedAt → now()
  └── 8. Failure path
         ├── storageHandoffStatus → STORAGE_FAILED
         ├── storageErrorCode → normalized code
         └── storageErrorMessage → safe message
```

### Kurallar

| Kural | Açıklama |
|-------|----------|
| GeneratedAsset APPROVED **yapılmaz** | Service scope dışı |
| GeneratedAsset REJECTED **yapılmaz** | Service scope dışı |
| Review action **üretmez** | Service scope dışı |
| Publish/export **yok** | Scope dışı |
| Generic status update **yok** | Explicit handoff action |
| Provider URL **response edilmez** | Internal-only |
| storageObjectKey **admin-facing olmaz** | Internal-only |

---

## 10. Preview Proxy Service Design

### Önerilen Service: `ImagePreviewProxyService`

Package: `com.anadoluatlasi.backend.imagegen.service`

### Flow

```
ImagePreviewProxyService.getPreviewStream(generatedAssetId, adminIdentity)
  │
  ├── 1. Load GeneratedAsset + MediaAsset
  ├── 2. Check storageHandoffStatus == STORAGE_STORED
  │      └── If not → PREVIEW_NOT_AVAILABLE error
  ├── 3. Authorization check
  │      └── If denied → PREVIEW_ACCESS_DENIED error
  ├── 4. Read from storage adapter
  │      └── ImageStorageAdapter.read(storageObjectKey)
  ├── 5. Return image stream + content-type
  │      └── Content-Type: image/png (from MediaAsset.mimeType)
  └── 6. Audit log (structured log)
         └── generatedAssetId, mediaAssetId, accessedBy, correlationId
```

### Kurallar

| Kural | Açıklama |
|-------|----------|
| Preview access review decision **değildir** | Bilgi alma eylemi |
| Preview GeneratedAsset status **değiştirmez** | Bağımsız concern |
| Preview MediaAsset status **değiştirmez** | Bağımsız concern |
| Preview **APPROVED yapmaz** | Human review gate |
| Preview unavailable → safe error | PREVIEW_NOT_AVAILABLE |
| Authorization **zorunludur** | Admin/review context |
| storageObjectKey **döndürülmez** | Internal-only |
| Signed/public URL **döndürülmez** | Güvenlik |
| Provider URL **döndürülmez** | Internal-only |

---

## 11. Safe Admin API Design

### Mevcut FP-007 Preview Endpoint (501)

```java
// AdminImageGenerationController.java — line ~199
@GetMapping("/generated-assets/{generatedAssetId}/preview")
// Currently returns 501
```

### FP-008 Preview Endpoint Aktivasyonu

| Method | Endpoint | FP-008 Davranış |
|--------|----------|-----------------|
| `GET` | `/api/admin/image-generation/generated-assets/{id}/preview` | Binary image stream (backend proxy) |

Response:
- **Success**: `200 OK`, `Content-Type: image/<type>`, binary stream body
- **Not available**: `422` + safe error DTO
- **Not found**: `404` + safe error DTO
- **Auth denied**: `403` + safe error DTO

### Olası Yeni Endpoint: Storage Handoff Trigger

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| `POST` | `/api/admin/image-generation/generated-assets/{id}/storage-handoff` | Explicit storage handoff başlat |

**Tasarım Kararı**: İlk implementation'da **explicit admin/system action** ile storage handoff daha kontrollü. Otomatik handoff (generation success sonrası) daha sonra düşünülebilir. Hangi yaklaşım seçilirse generic status update olmamalı.

### Yasak Endpointler

FP-008'de eklenmeyecek endpointler:

| Endpoint | Neden |
|----------|-------|
| `PATCH /status` | Generic status update yasağı |
| `/ready-for-publish` | Scope dışı |
| `/publish` | Scope dışı |
| `/export` | Scope dışı |
| `/signed-url` | URL exposure yasağı |
| `/public-url` | URL exposure yasağı |
| `/storage-key` | Internal-only |
| `/provider-url` | Provider isolation |
| `/raw-response` | Raw data yasağı |
| `/base64` | Binary DTO yasağı |

### Response Güvenliği

Admin response DTO'larında kesinlikle **yer almayacak** alanlar:

- `storageObjectKey`, `storageBucket`, `storageProvider`
- `providerUrl`, `providerOutputReference`
- `signedUrl`, `publicUrl`, `cdnUrl`
- `base64`, `imageBinary`
- `rawProviderResponse`, `rawStorageResponse`

---

## 12. Authorization / Access Control

| Gereksinim | Açıklama |
|------------|----------|
| Preview sadece admin/review authorized kullanıcılar için | Auth check zorunlu |
| Storage handoff sadece admin/system authorized context için | Auth check zorunlu |
| Direct user-facing media serving **yok** | Scope dışı |
| Preview endpoint auth bypass **yapmamalı** | Güvenlik kuralı |
| Access denied → safe error | `PREVIEW_ACCESS_DENIED` |
| Preview access **auditlenebilir** | Structured log |

İlk fazda auth mekanizması mevcut Spring Security context'e dayanır (admin endpoint zaten auth altında). Özel role/permission check ileride eklenebilir.

---

## 13. Error Handling / Normalization

### FP-008 Error Categories

| Error Code | Açıklama | Tetikleyen |
|------------|----------|------------|
| `PROVIDER_OUTPUT_UNAVAILABLE` | Provider output reference erişilemez | Download |
| `PROVIDER_OUTPUT_EXPIRED` | Provider output TTL dolmuş | Download |
| `PROVIDER_OUTPUT_DOWNLOAD_FAILED` | Download network/IO error | Download |
| `MEDIA_VALIDATION_FAILED` | Genel validation hatası | Validation |
| `UNSUPPORTED_MEDIA_TYPE` | MIME type allowlist dışında | MIME check |
| `MEDIA_TOO_LARGE` | Dosya boyutu limiti aşıldı | Size check |
| `MEDIA_DIMENSION_INVALID` | Width/height sınır dışı | Dimension |
| `CHECKSUM_CALCULATION_FAILED` | Hash hesaplanamadı | Integrity |
| `STORAGE_WRITE_FAILED` | Object store'a yazma başarısız | Upload |
| `STORAGE_READ_FAILED` | Object store'dan okuma başarısız | Preview |
| `PREVIEW_NOT_AVAILABLE` | Preview henüz hazır değil | Preview |
| `PREVIEW_ACCESS_DENIED` | Authorization başarısız | Auth |
| `UNKNOWN_STORAGE_HANDOFF_FAILURE` | Sınıflandırılamayan hata | Catch-all |

### Normalization Kuralları

| Kural | Açıklama |
|-------|----------|
| Raw provider/storage error dump **gösterilmez** | Safe normalization |
| Stack trace admin-facing **değildir** | Güvenlik |
| Storage key error response'a **çıkmaz** | Internal isolation |
| Provider URL error response'a **çıkmaz** | Provider isolation |
| Signed/public URL error response'a **çıkmaz** | URL isolation |
| Safe error message kısa ve normalize olur | UX + güvenlik |
| `correlationId` **korunur** | Observability |

FP-007 `ImageGenerationErrorNormalizer` pattern'i referans alınır; FP-008 storage error normalization benzer yaklaşımla implement edilir.

---

## 14. Observability / Audit

### Audit Alanları

| Alan | Açıklama | Log? | DTO? |
|------|----------|------|------|
| `generatedAssetId` | Asset identifier | ✅ | ✅ |
| `mediaAssetId` | Media identifier | ✅ | ✅ |
| `correlationId` | End-to-end trace | ✅ | ✅ |
| `storageHandoffStatus` | Handoff durumu | ✅ | ✅ |
| `storageErrorCode` | Normalized error | ✅ | ✅ |
| `handoffRequestedAt` | İstek zamanı | ✅ | ⛔ |
| `handoffCompletedAt` | Tamamlanma zamanı | ✅ | ⛔ |
| `previewAccessedBy` | Preview erişen | ✅ | ⛔ |
| `previewAccessedAt` | Preview erişim zamanı | ✅ | ⛔ |
| `fileSizeBytes` | Dosya boyutu | ✅ | ✅ |
| `width` / `height` | Dimensions | ✅ | ✅ |

### Loglanmayacaklar

| Yasak | Açıklama |
|-------|----------|
| Provider URL | Internal-only |
| Signed/public URL | Güvenlik |
| Storage object key | Internal-only |
| Base64/binary | Performans + güvenlik |
| Raw provider/storage payload | Güvenlik |
| Secret/API key/config | Güvenlik |
| Internal prompt | Güvenlik |

---

## 15. Tests / Guardrails Plan

### Unit Test Plan

| Test Konusu | Açıklama |
|-------------|----------|
| Provider URL DTO'ya çıkmaz | DTO field reflection guardrail |
| Storage object key DTO'ya çıkmaz | DTO field reflection guardrail |
| Signed/public URL DTO'ya çıkmaz | DTO field reflection guardrail |
| Base64 DTO'ya çıkmaz | DTO field reflection guardrail |
| Storage success approval yapmaz | Service logic test |
| Preview access approval yapmaz | Service logic test |
| Preview endpoint auth gerektirir | Controller test |
| Invalid MIME rejected | Validation service test |
| Oversized image rejected | Validation service test |
| Corrupt image rejected | Validation service test |
| Zero-byte rejected | Validation service test |
| Checksum calculated | Validation service test |
| Storage write failure → safe error | Handoff service test |
| Storage read failure → safe error | Preview service test |
| Preview not available → safe error | Preview service test |
| READY_FOR_PUBLISH/COMPLETED yok | Enum guardrail test |
| Generic status update yok | Endpoint absence test |

### Mevcut Guardrail Testleri (Korunur)

- `ImageGenerationSafetyGuardrailTest` — 20 test
- `ImageGenerationStorageHandoffGuardrailTest` — 4 test
- `OpenAiImageGenerationProviderGatewayTest` — 11 test
- Diğer FP-007 testleri — 53 test
- **Toplam mevcut: 88 test**

---

## 16. Non-Goals

| Non-Goal | Neden |
|----------|-------|
| Publishing/export automation | Ayrı feature |
| READY_FOR_PUBLISH | Scope dışı |
| Generic status update endpoint | Güvenlik tercihi |
| Public media serving | User-facing ayrı alan |
| User-facing preview/media serving | Sadece admin preview |
| Direct signed URL exposure | Güvenlik riski |
| Public URL generation | Güvenlik riski |
| CDN optimization | Dağıtım altyapısı |
| Image editing/cropping | Post-processing |
| Video/audio storage | Sadece image scope |
| Bulk ingestion | Kontrollü tek-item |
| Regeneration/variation | Future enhancement |
| Multi-provider failover | Future enhancement |
| Provider cost optimization | Future enhancement |
| Advanced moderation automation | Human review gate korunuyor |
| AI output automatic approval | Kesinlikle scope dışı |

---

## 17. Coding Task Breakdown

### Task 04 — Storage Status Enums and Persistence Skeleton

**Amaç**: Storage/preview status enumları ve MediaAsset entity field hazırlığı.

**Scope**:
- `ImageStorageHandoffStatus` enum
- `ImageStorageErrorCode` enum
- `ImageMediaAsset` entity'ye yeni alanlar (storageHandoffStatus, storedAt, storageErrorCode, storageErrorMessage)
- `ImageMediaAssetResponse` DTO'ya safe alanlar ekleme
- Migration: `V11__fp008_storage_handoff_fields.sql`

**Non-Goals**: Storage SDK yok, provider download yok, preview implementation yok, public/signed URL yok.

**Acceptance Criteria**:
- Enum compile eder
- Entity field'lar compile eder
- Migration syntax doğru
- DTO'da storageObjectKey/signedUrl/publicUrl yok
- READY_FOR_PUBLISH yok
- Tests updated/passing

---

### Task 05 — Media Validation Service

**Amaç**: MIME/size/dimension/checksum validation service.

**Scope**:
- `ImageMediaValidationService` class
- MIME allowlist (image/png, image/jpeg, image/webp)
- Content sniffing (magic bytes)
- File size limit (configurable)
- Width/height extraction (ImageIO)
- Zero-byte / corrupt image rejection
- SHA-256 checksum calculation
- Unit tests with in-memory byte arrays

**Non-Goals**: Provider URL download yok, storage upload yok, preview yok.

**Acceptance Criteria**:
- Valid PNG/JPEG/WebP geçer
- Invalid MIME reddedilir
- Oversized reddedilir
- Zero-byte reddedilir
- Checksum hesaplanır
- Safe error codes kullanılır
- Tests passing

---

### Task 06 — Storage Adapter Interface and Mock Storage

**Amaç**: Provider-agnostic storage adapter interface + mock implementation.

**Scope**:
- `ImageStorageAdapter` interface
- `MockImageStorageAdapter` — in-memory storage
- Storage object key server-side generation
- Config: `imagegen.storage.mode=mock`
- Storage config class
- Unit tests

**Non-Goals**: Real storage provider (S3/MinIO/GCS) yok, public/signed URL yok, CDN yok.

**Acceptance Criteria**:
- Mock storage write/read çalışıyor
- Object key server-side üretiliyor
- Public/signed URL üretilmiyor
- Config-driven provider selection
- Tests passing

---

### Task 07 — Storage Handoff Service

**Amaç**: End-to-end storage handoff orchestration.

**Scope**:
- `ImageStorageHandoffService` class
- Provider output retrieval (internal helper)
- Validation → storage → metadata update flow
- `ImageMediaAsset` safe metadata update
- Storage status lifecycle (PENDING → VALIDATING → STORED / FAILED)
- Safe error handling
- Unit tests

**Non-Goals**: Preview yok, API endpoint yok, review status change yok, automatic approval yok.

**Acceptance Criteria**:
- Success: MediaAsset metadata updated, status = STORAGE_STORED
- Failure: status = STORAGE_FAILED, safe error code/message
- GeneratedAsset review status değişmez
- Provider URL persist edilmez
- Tests passing

---

### Task 08 — Safe Admin API Updates for Storage Handoff

**Amaç**: Storage handoff trigger endpoint + DTO updates.

**Scope**:
- `POST /api/admin/image-generation/generated-assets/{id}/storage-handoff`
- `ImageMediaAssetResponse` DTO safe field güncellemeleri
- `ImageGeneratedAssetResponse` previewAvailable derived field
- Mapper updates
- Controller tests

**Non-Goals**: Preview implementation yok, generic status update yok, signed/public URL yok, storageObjectKey exposure yok.

**Acceptance Criteria**:
- Storage handoff trigger endpoint çalışıyor
- DTO'da storageHandoffStatus, previewAvailable, storedAt mevcut
- DTO'da storageObjectKey/signedUrl/publicUrl yok
- Generic status update endpoint yok
- Tests passing

---

### Task 09 — Backend-Controlled Preview Proxy

**Amaç**: Preview endpoint aktif etme — binary stream response.

**Scope**:
- `ImagePreviewProxyService` class
- Preview endpoint: `GET /api/admin/image-generation/generated-assets/{id}/preview`
- 501 → binary image stream response
- Authorization check
- Preview audit structured log
- Safe error responses (not available, access denied, not found)
- Unit tests

**Non-Goals**: Signed/public URL yok, storage key exposure yok, review status change yok, automatic approval yok.

**Acceptance Criteria**:
- Preview endpoint image stream döner
- storageObjectKey/signedUrl/publicUrl döndürülmez
- Authorization zorunlu
- Preview access GeneratedAsset status değiştirmez
- Storage not ready → safe error
- Tests passing

---

### Task 10 — Error Normalization and Observability

**Amaç**: Storage/preview error normalizer + audit guardrails.

**Scope**:
- `ImageStorageErrorNormalizer` class (FP-007 pattern'i referans)
- Safe error message generation
- Storage/preview structured audit logs
- Log guardrail tests (no URL/key/binary in logs)
- DTO guardrail test updates

**Non-Goals**: Provider URL log yok, storage key log yok, raw dump yok.

**Acceptance Criteria**:
- Normalized error codes tüm failure path'lerde kullanılıyor
- Raw provider/storage error admin-facing'e dönmüyor
- Audit logs safe metadata kullanıyor
- Guardrail tests passing

---

### Task 11 — Optional Real Storage Provider Integration

**Amaç**: Mock storage'dan real storage'a geçiş (opsiyonel).

**Scope**:
- Real storage adapter (S3/MinIO/GCS/local filesystem)
- `imagegen.storage.mode=s3|minio|local` config
- Server-side config only
- No signed/public URL exposure
- Integration tests with test containers veya local

**Non-Goals**: CDN yok, public URL yok, signed URL yok.

**Acceptance Criteria**:
- Real storage write/read çalışıyor
- Mock storage hâlâ default
- Public/signed URL üretilmiyor
- Config-driven selection
- Tests passing

---

### Task 12 — FP-008 Final Closure / Operational Baseline

**Amaç**: Feature closure documentation.

**Scope**:
- Closure doc: `docs/05-engineering/XX-fp008-final-closure-operational-baseline.md`
- Final test baseline
- Endpoint baseline
- Security confirmation
- Deferred work documentation

**Non-Goals**: Kod değişikliği yok.

**Acceptance Criteria**:
- FP-008 CLOSED olarak kaydedildi
- Tüm teyitler raporlandı
- Final test count raporlandı

---

## 18. Acceptance Criteria

| Kriter | Durum |
|--------|-------|
| Engineering Design doc target path'te oluşturuldu | ✅ |
| Product ve AI Platform / Media Contract docs referans alındı | ✅ |
| FP-007 closure baseline referans alındı | ✅ |
| Domain/model proposal var | ✅ |
| Storage/preview status model var | ✅ |
| Persistence/migration design var | ✅ |
| Provider output retrieval boundary var | ✅ |
| Media validation service design var | ✅ |
| Storage adapter boundary var | ✅ |
| Storage handoff service design var | ✅ |
| Preview proxy service design var | ✅ |
| Safe admin API design var | ✅ |
| Authorization/audit design var | ✅ |
| Error normalization design var | ✅ |
| Tests/guardrails plan var | ✅ |
| Coding task breakdown var (9 task) | ✅ |
| Provider URL/base64/public/signed URL/storage key exposure yasakları korunuyor | ✅ |
| Human review gate korunuyor | ✅ |
| READY_FOR_PUBLISH / generic status update / publish/export dışarıda | ✅ |
| Scope documentation-only | ✅ |

---

## 19. Future Coding Handoff — FP-008 Task 04

FP-008 Task 04 önerisi: **Storage Status Enums and Persistence Skeleton**

Task 04'te yapılacaklar:
- İlk coding task — enum/entity/DTO/migration hazırlığı
- `ImageStorageHandoffStatus` ve `ImageStorageErrorCode` enum'ları
- `ImageMediaAsset` entity'ye storage handoff alanları
- `ImageMediaAssetResponse` DTO'ya safe alanlar
- `V11__fp008_storage_handoff_fields.sql` migration

Task 04'te yapılmayacaklar:
- Storage SDK yok
- Provider output download yok
- Preview implementation yok
- Public/signed URL yok
- Automatic approval yok
- READY_FOR_PUBLISH yok
- Generic status update yok

Security boundaries bu dokümandaki kurallarla korunmalıdır.

---

*Bu doküman FP-008 Task 03 kapsamında oluşturulmuştur. Documentation-only — kod/config/runtime değişikliği yoktur.*
