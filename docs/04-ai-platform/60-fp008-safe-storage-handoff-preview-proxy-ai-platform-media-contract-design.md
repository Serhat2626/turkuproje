# FP-008 — Safe Storage Handoff & Preview Proxy — AI Platform / Media Contract Design

> **Feature**: FP-008 — Safe Storage Handoff & Preview Proxy  
> **Status**: Draft  
> **Task**: FP-008 Task 02 — AI Platform / Media Contract Design  
> **Scope**: Documentation-only  
> **Product Design**: [59-fp008-safe-storage-handoff-preview-proxy-product-design.md](../03-product-design/59-fp008-safe-storage-handoff-preview-proxy-product-design.md)  
> **FP-007 Closure**: [58-fp007-final-closure-operational-baseline.md](../05-engineering/58-fp007-final-closure-operational-baseline.md)  
> **FP-007 Storage Planning**: [57-fp007-safe-provider-output-storage-handoff-planning.md](../05-engineering/57-fp007-safe-provider-output-storage-handoff-planning.md)

---

## 1. Status / Scope

- FP-008 Task 02, **AI Platform / Media Contract Design** dokümanıdır
- **Documentation-only** — kod, migration, runtime değişikliği yoktur
- FP-008 Product Design dokümanına dayanır
- FP-007 closure baseline referans alınır
- Gerçek storage SDK integration, provider download, preview streaming implementation bu taskta yapılmaz
- Bu doküman **Engineering Design** için contract baseline sağlar

---

## 2. Contract Role in FP-008

Bu contract katmanı, FP-008 pipeline'ının güvenlik sınırlarını tanımlar:

| Rol | Açıklama |
|-----|----------|
| Provider output isolation | Provider URL/base64 gibi hassas output'ları admin/user-facing yüzeylerden izole eder |
| Internal boundary | Provider output reference'ı internal boundary içinde tutar |
| Storage handoff contract | Server-side storage handoff için safe request/result contract tanımlar |
| Media validation contract | MIME/size/dimension/checksum validation result contract tanımlar |
| Metadata allowlist | MediaAsset safe metadata allowlist tanımlar |
| Preview contract | Preview proxy'nin admin-facing ama storage-key-hidden contract'ını tanımlar |
| Review preservation | Human review gate'i korur |

### Contract Üretmez

- Publish/export status
- READY_FOR_PUBLISH
- Generic status update yaklaşımı
- Public/signed URL generation
- Automatic approval signal

---

## 3. Provider Output Reference Boundary

### Bağlam

FP-007'de provider result metadata-only idi. FP-008 için provider output storage'a alınacağından, provider output reference'ın güvenli sınırları tanımlanmalıdır.

### Internal-Only Conceptual Fields

Aşağıdaki alanlar **yalnızca storage handoff service internal input'u** olarak kullanılır:

| Field | Açıklama | Ömür |
|-------|----------|------|
| `providerOutputReference` | Provider'ın döndürdüğü output locator (URL veya identifier) | Kısa ömürlü — handoff sonrası temizlenir |
| `providerTemporaryUrl` | Provider'ın sağladığı geçici download URL'i | TTL'e tabi — kalıcı persist edilmez |
| `providerAssetId` | Provider-side asset identifier | Internal-only |
| `providerOutputExpiresAt` | Provider output'un erişilebilirlik son tarihi | Internal reference |
| `providerContentTypeHint` | Provider'ın bildirdiği content type | Validation ile doğrulanmalı |
| `providerContentLengthHint` | Provider'ın bildirdiği dosya boyutu | Validation ile doğrulanmalı |

### Kesin Kurallar

| Kural | Zorunluluk |
|-------|------------|
| Bu alanlar admin/user-facing DTO'ya **çıkmaz** | Kesin |
| Kalıcı persistence mümkünse **yapılmaz** | Kesin |
| Gerekiyorsa encrypted/internal-only ve kısa ömürlü olmalıdır | Kesin |
| Provider URL kalıcı MediaAsset metadata **değildir** | Kesin |
| Provider URL **loglanmaz** | Kesin |
| Provider URL admin response'a **dönmez** | Kesin |
| Provider base64 DB'ye **yazılmaz** | Kesin |
| Provider base64 DTO'ya **dönmez** | Kesin |
| Raw provider response **saklanmaz** | Kesin |
| Raw provider payload **saklanmaz** | Kesin |

### Contract Sınırı

```
Provider Gateway (FP-007)
  │
  │  normalized result + internal output reference
  │
  ▼
Storage Handoff Service (FP-008)
  │
  │  server-side download → validate → store
  │
  ▼
MediaAsset (safe metadata only)
```

Provider output reference, **storage handoff service için internal input'tur**. MediaAsset'e sadece storage sonrası safe metadata yazılır.

---

## 4. Storage Handoff Request Contract

Storage handoff'u tetikleyen conceptual request contract'ı:

### Conceptual Fields

| Field | Açıklama | Visibility |
|-------|----------|------------|
| `handoffRequestId` | Unique handoff request identifier | Internal |
| `imageGenerationRequestId` | Source generation request | Safe |
| `imageGenerationResultId` | Source generation result | Safe |
| `generatedAssetId` | Target generated asset | Safe |
| `mediaAssetId` | Target media asset | Safe |
| `contentProjectId` | Content project reference | Safe |
| `visualAssetPlanId` | Source visual asset plan | Safe |
| `visualAssetItemId` | Source visual asset item | Safe |
| `providerOutputReference` | Provider output locator | **Internal-only** |
| `expectedMediaKind` | GENERATED_IMAGE | Safe |
| `expectedMimeTypes` | ["image/png", "image/jpeg", "image/webp"] | Safe |
| `maxFileSizeBytes` | Validation limit | Safe |
| `validationPolicy` | Standard / strict | Safe |
| `correlationId` | End-to-end trace identifier | Safe |
| `requestedAt` | Handoff request timestamp | Safe |
| `requestedBy` | System actor / admin identifier | Safe |

### Kurallar

- `providerOutputReference` admin-facing değildir
- `signedUrl` / `publicUrl` contract'ta admin-facing olarak yer almaz
- Raw provider payload yok
- Raw provider response yok
- Internal prompt yok
- `apiKey` / `secret` yok
- Storage object key request dışından verilmez — **server-side generated** olmalıdır
- Client/admin storage path göndermez

---

## 5. Media Validation Contract

Provider output'un storage'a alınmadan önce geçmesi gereken validation contract'ı:

### Validation Kontrolleri

| Kontrol | Açıklama | Zorunluluk |
|---------|----------|------------|
| MIME type allowlist | `image/png`, `image/jpeg`, `image/webp` | Zorunlu |
| Content sniffing | Magic bytes ile actual media type doğrulama | Zorunlu |
| File size limit | Configurable max size — aşarsa rejection | Zorunlu |
| Width/height extraction | Image dimension okuma | Zorunlu |
| Zero-byte rejection | Boş dosya reddedilir | Zorunlu |
| Corrupt image rejection | Okunamayan/bozuk image reddedilir | Zorunlu |
| Checksum calculation | SHA-256 hash | Zorunlu |
| Safety flag preservation | Provider safety signal'ları meta olarak tutulabilir | Opsiyonel |
| Provider output accessibility | URL/reference erişilebilir mi kontrolü | Zorunlu |

### Validation Result Conceptual Fields

| Field | Açıklama | Visibility |
|-------|----------|------------|
| `validationStatus` | PASSED / FAILED | Safe |
| `detectedMimeType` | Actual MIME type (sniffing sonrası) | Safe |
| `fileSizeBytes` | Gerçek dosya boyutu | Safe |
| `width` | Pixel cinsinden genişlik | Safe |
| `height` | Pixel cinsinden yükseklik | Safe |
| `checksumSha256` | SHA-256 hash | Safe |
| `validationErrors` | Safe validation error list | Safe |
| `normalizedErrorCode` | Normalized error category | Safe |
| `safeErrorMessage` | Admin-safe error description | Safe |
| `validatedAt` | Validation timestamp | Safe |

### Failure Handling Kuralları

| Kural | Açıklama |
|-------|----------|
| Validation failure GeneratedAsset'i otomatik REJECTED **yapmaz** | Human review gate korunur |
| Validation failure storage handoff failure olarak ele alınır | Storage status → STORAGE_FAILED |
| Validation error raw provider/storage detail **içermez** | Safe normalized error |
| Stack trace expose **edilmez** | Admin-facing güvenlik |

---

## 6. Storage Handoff Result Contract

Storage tamamlandıktan sonra üretilen result contract'ı:

### Conceptual Fields

| Field | Açıklama | Visibility |
|-------|----------|------------|
| `handoffResultId` | Unique result identifier | Internal |
| `handoffRequestId` | Source request reference | Internal |
| `mediaAssetId` | Updated media asset | Safe |
| `storageStatus` | STORAGE_STORED / STORAGE_FAILED | Safe |
| `storageProviderAlias` | S3/MinIO/GCS/local vs. | **Internal-only** |
| `storageObjectKey` | Bucket-relative path | **Internal-only** |
| `mimeType` | Validated MIME type | Safe |
| `width` | Validated width | Safe |
| `height` | Validated height | Safe |
| `fileSizeBytes` | Validated file size | Safe |
| `checksumSha256` | Calculated hash | Safe |
| `storedAt` | Storage completion timestamp | Safe |
| `correlationId` | End-to-end trace | Safe |
| `normalizedErrorCode` | Failure error code | Safe |
| `safeErrorMessage` | Failure safe message | Safe |

### Kesin Kurallar

| Kural | Zorunluluk |
|-------|------------|
| `storageObjectKey` admin/user-facing DTO'ya **çıkmaz** | Kesin |
| Bucket/container name admin/user-facing DTO'ya **çıkmaz** | Kesin |
| Public URL **yok** | Kesin |
| Signed URL **yok** | Kesin |
| CDN URL **yok** | Kesin |
| Provider URL **yok** | Kesin |
| Base64 **yok** | Kesin |
| Image binary **yok** | Kesin |
| MediaAsset safe metadata **allowlist** ile güncellenir | Kesin |
| Failure result **safe error** kullanır | Kesin |

---

## 7. MediaAsset Safe Metadata Allowlist

### Admin-Facing Safe Fields

| Field | Açıklama | Admin DTO |
|-------|----------|-----------|
| `mediaAssetId` | Unique identifier | ✅ |
| `mediaAssetKind` | GENERATED_IMAGE | ✅ |
| `mimeType` | image/png, image/jpeg, image/webp | ✅ |
| `width` | Pixel cinsinden genişlik | ✅ |
| `height` | Pixel cinsinden yükseklik | ✅ |
| `fileSizeBytes` | Dosya boyutu (bytes) | ✅ |
| `checksumSha256` | SHA-256 integrity hash | ✅ |
| `storageHandoffStatus` | STORAGE_PENDING / STORED / FAILED | ✅ |
| `previewAvailable` | Preview hazır mı (derived) | ✅ |
| `storedAt` | Storage tamamlanma zamanı | ✅ |
| `reviewStatus` | REVIEW_APPROVED / REVIEW_REJECTED | ✅ |
| `createdAt` | Oluşturulma zamanı | ✅ |
| `updatedAt` | Son güncelleme zamanı | ✅ |

### Internal-Only Fields

| Field | Açıklama | Admin DTO |
|-------|----------|-----------|
| `storageProviderAlias` | S3/MinIO/GCS vs. | ⛔ |
| `storageObjectKey` | Bucket-relative path | ⛔ |
| `storageBucket` | Bucket/container adı | ⛔ |
| `providerOutputReference` | Geçici provider locator | ⛔ |
| `providerTemporaryUrl` | Geçici download URL | ⛔ |
| `providerAssetId` | Provider-side identifier | ⛔ |
| `providerOutputExpiresAt` | Provider output TTL | ⛔ |
| `internalValidationDetail` | Raw validation logs | ⛔ |
| `providerRequestId` | FP-007 internal | ⛔ |

### Kesin Yasak Alanlar

Bu alanlar MediaAsset entity veya DTO'sunda **hiçbir koşulda** yer almaz:

- `publicUrl`
- `signedUrl`
- `cdnUrl`
- `base64`
- `imageBinary`
- `rawProviderResponse`
- `rawProviderRequest`
- `rawStorageResponse`
- `internalFullPrompt`
- `apiKey` / `secret`
- `providerConfig`

---

## 8. Preview Proxy Contract

### Preview Request Conceptual Fields

| Field | Açıklama | Source |
|-------|----------|-------|
| `generatedAssetId` veya `mediaAssetId` | Preview target identifier | URL path parameter |
| `requesterIdentity` | Admin/reviewer identity | Auth context |
| `correlationId` | Trace identifier | Request header / generated |
| `accessContext` | Admin review context | Internal |

### Preview Response Possibilities

| Senaryo | Response |
|---------|----------|
| Storage stored, preview ready | Binary image stream (Content-Type: image/png vb.) |
| Storage pending | Safe status response: "Preview henüz hazır değil" |
| Storage failed | Safe error response: "Preview oluşturulamadı" |
| Asset not found | 404 safe error |
| Authorization denied | 403 safe error |

### Kesin Kurallar

| Kural | Zorunluluk |
|-------|------------|
| Preview endpoint `storageObjectKey` **döndürmez** | Kesin |
| Preview endpoint signed URL **döndürmez** | Kesin |
| Preview endpoint public URL **döndürmez** | Kesin |
| Preview endpoint provider URL **döndürmez** | Kesin |
| Preview authorization **zorunludur** | Kesin |
| Preview sadece admin/review context içindir | Kesin |
| Preview access review decision **değildir** | Kesin |
| Preview access asset'i APPROVED **yapmaz** | Kesin |
| Preview audit/observability safe metadata ile yapılabilir | Kesin |
| Preview unavailable → safe error döner | Kesin |

### Preview vs. Review Ayrımı

```
Preview = "Görseli gör"      → Bilgi alma eylemi
Approve = "Onayla"            → Karar eylemi (explicit action)
Reject  = "Reddet + neden"    → Karar eylemi (explicit action)

Preview ≠ Approve
Preview ≠ Reject
Preview ≠ Status change
```

---

## 9. Error Normalization

### FP-008 Normalized Error Categories

| Error Code | Açıklama | Tetikleyen |
|------------|----------|------------|
| `PROVIDER_OUTPUT_UNAVAILABLE` | Provider output reference erişilemez | Download attempt |
| `PROVIDER_OUTPUT_EXPIRED` | Provider output TTL dolmuş | Download attempt |
| `PROVIDER_OUTPUT_DOWNLOAD_FAILED` | Provider output indirilemedi | Network/IO error |
| `MEDIA_VALIDATION_FAILED` | Genel validation hatası | Validation pipeline |
| `UNSUPPORTED_MEDIA_TYPE` | MIME type allowlist dışında | MIME check |
| `MEDIA_TOO_LARGE` | Dosya boyutu limiti aşıldı | Size check |
| `MEDIA_DIMENSION_INVALID` | Width/height sınır dışı veya okunamıyor | Dimension check |
| `CHECKSUM_CALCULATION_FAILED` | Hash hesaplanamadı | Integrity check |
| `STORAGE_WRITE_FAILED` | Object store'a yazma başarısız | Storage upload |
| `STORAGE_READ_FAILED` | Object store'dan okuma başarısız | Preview read |
| `PREVIEW_NOT_AVAILABLE` | Preview henüz hazır değil veya storage failed | Preview request |
| `PREVIEW_ACCESS_DENIED` | Preview authorization başarısız | Auth check |
| `UNKNOWN_STORAGE_HANDOFF_FAILURE` | Sınıflandırılamayan hata | Catch-all |

### Error Response Kuralları

| Kural | Açıklama |
|-------|----------|
| Raw provider/storage error dump **gösterilmez** | Safe normalization |
| Stack trace admin-facing **değildir** | Güvenlik |
| Storage object key error response'a **çıkmaz** | Internal isolation |
| Provider URL error response'a **çıkmaz** | Provider isolation |
| Signed/public URL error response'a **çıkmaz** | URL isolation |
| Safe error message kısa ve normalize olur | UX |
| `correlationId` log/trace için kullanılabilir | Observability |

---

## 10. Status / State Contract

### GeneratedAsset Status (FP-007 — Korunur)

| Status | Anlamı | Değişim |
|--------|--------|---------|
| `GENERATED_PENDING_REVIEW` | Review kuyruğunda | FP-007'den korunur |
| `APPROVED` | Admin explicit approve | FP-007'den korunur |
| `REJECTED` | Admin explicit reject | FP-007'den korunur |

### Storage Handoff Status (FP-008 — Yeni)

| Status | Anlamı | Tetikleyen |
|--------|--------|------------|
| `STORAGE_PENDING` | Handoff henüz başlamadı veya devam ediyor | Initial state |
| `STORAGE_VALIDATING` | MIME/size/dimension validation sürüyor | Validation start |
| `STORAGE_STORED` | Başarıyla depolandı | Upload complete |
| `STORAGE_FAILED` | Handoff başarısız | Any failure |

### Preview Availability (Derived State)

| Status | Anlamı | Derived From |
|--------|--------|--------------|
| `PREVIEW_UNAVAILABLE` | Storage henüz tamamlanmamış veya başarısız | Storage status ≠ STORED |
| `PREVIEW_AVAILABLE` | Storage tamamlandı, preview proxy hazır | Storage status = STORED |

### Kesin Kurallar

| Kural | Açıklama |
|-------|----------|
| Storage status, **review status değildir** | Bağımsız concern |
| Preview status, **review status değildir** | Bağımsız concern |
| Storage success → automatic APPROVED **değildir** | Human review gate |
| Preview available → automatic APPROVED **değildir** | Human review gate |
| `READY_FOR_PUBLISH` **kullanılmaz** | Scope dışı |
| `COMPLETED` gibi generic status **kullanılmaz** | Scope dışı |
| Generic status update endpoint **kullanılmaz** | Güvenlik tercihi |
| Publish/export status **yok** | Scope dışı |

---

## 11. Security / Privacy / Governance Rules

### Açık Yasak Listesi

| # | Yasak |
|---|-------|
| 1 | No API key exposure |
| 2 | No secret exposure |
| 3 | No raw provider request exposure |
| 4 | No raw provider response exposure |
| 5 | No raw storage provider response exposure |
| 6 | No internal prompt exposure |
| 7 | No provider URL persistence as MediaAsset metadata |
| 8 | No provider URL admin/user exposure |
| 9 | No signed URL admin/user exposure |
| 10 | No public URL generation/exposure |
| 11 | No storage object key admin/user exposure |
| 12 | No base64 DB persistence |
| 13 | No base64 DTO response |
| 14 | No image binary DB persistence |
| 15 | No generic status update |
| 16 | No READY_FOR_PUBLISH |
| 17 | No automatic approval |
| 18 | No publish/export |
| 19 | No video/audio |

### Zorunlu Güvenlik Kuralları

| # | Kural |
|---|-------|
| 1 | Server-side ingestion only |
| 2 | Metadata allowlist |
| 3 | Internal-only storage references |
| 4 | Backend-controlled preview |
| 5 | Authorization check |
| 6 | Safe normalized errors |
| 7 | Lineage preservation (correlationId) |
| 8 | Human review gate |

---

## 12. Non-Goals

Aşağıdakiler bu AI Platform / Media Contract taskının scope dışındadır:

| Non-Goal |
|----------|
| Real storage SDK integration |
| Provider image download implementation |
| Bucket/object lifecycle implementation |
| Preview streaming implementation |
| Controller/API implementation |
| Database schema design |
| Admin frontend implementation |
| Public media serving |
| Signed URL generation |
| CDN optimization |
| Publishing/export automation |
| READY_FOR_PUBLISH transition |
| Generic status update |
| Video/audio storage |
| Bulk ingestion |
| Regeneration/variation flow |
| Multi-provider failover |
| Cost optimization |
| Image editing/cropping |
| Advanced moderation automation |

---

## 13. Acceptance Criteria

| Kriter | Durum |
|--------|-------|
| AI Platform / Media Contract Design doc target path'te oluşturuldu | ✅ |
| FP-008 Product Design kaynağı referans alındı | ✅ |
| FP-007 closure baseline referans alındı | ✅ |
| Provider output reference boundary tanımlandı | ✅ |
| Storage handoff request contract tanımlandı | ✅ |
| Media validation contract tanımlandı | ✅ |
| Storage handoff result contract tanımlandı | ✅ |
| MediaAsset safe metadata allowlist tanımlandı | ✅ |
| Preview proxy contract tanımlandı | ✅ |
| Error normalization tanımlandı | ✅ |
| Provider URL/base64 exposure yasaklandı | ✅ |
| Public/signed URL exposure yasaklandı | ✅ |
| Storage object key admin exposure yasaklandı | ✅ |
| Human review gate korundu | ✅ |
| READY_FOR_PUBLISH / generic status update / publish/export dışarıda | ✅ |
| Scope documentation-only kaldı | ✅ |

---

## 14. Future Engineering Handoff — FP-008 Task 03

FP-008 Task 03, **Engineering Design & Task Breakdown** olarak planlanmaktadır.

Engineering dokümanında ele alınacak başlıklar:

| Konu | Açıklama |
|------|----------|
| Domain/model proposal | Storage handoff status enum, MediaAsset field updates |
| MediaAsset field updates | storageObjectKey, storageProviderAlias, storageHandoffStatus, storedAt |
| Storage handoff service boundary | Ingestion trigger, download, validate, upload, metadata update |
| Provider output retrieval boundary | Internal-only download, temp reference, cleanup |
| Media validation service | MIME sniffing, size check, dimension check, checksum |
| Storage adapter interface | Provider-agnostic object store gateway |
| Preview proxy service | Backend-controlled preview, auth, streaming |
| Safe admin API update | Preview endpoint activation, storage status DTO fields |
| Authorization and audit | Preview auth, audit logging |
| Error persistence and normalization | FP-008 error codes, safe messages |
| Tests and migration plan | Schema changes, unit tests, guardrail tests |

---

*Bu doküman FP-008 Task 02 kapsamında oluşturulmuştur. Documentation-only — kod/config/runtime değişikliği yoktur.*
