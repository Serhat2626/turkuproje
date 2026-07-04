# FP-007 — Safe Provider Output Storage Handoff Planning

> **Status**: Planning-only — implementation deferred  
> **Feature**: FP-007 — Image Generation & Media Asset Pipeline  
> **Task**: Task 13 — Safe Provider Output Storage Handoff Planning  
> **Parent Doc**: [56-fp007-engineering-design-and-task-breakdown.md](./56-fp007-image-generation-media-asset-pipeline-engineering-design-and-task-breakdown.md)

---

## 1. Current State

FP-007 Task 01–12 tamamlandı. Mevcut akış:

| Katman | Durum |
|--------|-------|
| Domain model | ✅ Entity/enum/DTO/mapper tamamlandı |
| Persistence | ✅ Migration V9 + V10 aktif |
| Admin API | ✅ Request/execute/list/detail/approve/reject aktif |
| Mock provider | ✅ Deterministic mock, external call yok |
| OpenAI provider | ✅ Metadata-only adapter, config-driven, default kapalı |
| Error handling | ✅ Normalized error codes, safe messages, guardrail tests |
| Review workflow | ✅ Human review gate aktif |
| Preview endpoint | ⛔ 501 — intentionally not implemented |
| Storage | ⛔ Intentionally not opened |
| Publish/export | ⛔ Not in scope |

### Provider Integration Model

Provider integration **metadata-only** çalışıyor:

- Provider'dan dönen response'tan yalnızca safe metadata extract edilir
- Image URL, base64, binary **persist edilmez**
- Image URL, base64, binary **DTO'ya çıkmaz**
- Image URL, base64, binary **loglanmaz**
- MediaAsset kaydı mock/safe metadata ile oluşuyor
- `ImageProviderGenerationResult` sadece allowlist metadata taşır

### Aktif Allowlist Metadata

```
success, providerAlias, modelName, outputCount, generationDurationMs,
mimeType, width, height, fileSizeBytes, checksumSha256,
errorCode, errorMessage, correlationId, completedAt,
qualitySignalsSummary, safetyFlagsSummary
```

---

## 2. Why Storage Is Not Opened Yet

Storage açılmamasının güvenlik gerekçeleri:

### A. Provider Output Güvenliği

- Provider image URL **geçici** bir kaynaktır — kalıcı field olarak persist edilmemeli
- Provider URL doğrudan admin UI'da render edilirse **URL sızıntısı** riski var
- Base64/binary büyük boyutlu veri — DB'de persist etmek veri tabanı performansını bozar
- Provider URL'inin TTL'si (yaşam süresi) provider'a bağlıdır ve değişebilir

### B. Storage Lifecycle Güvenliği

- Storage object key generation server-side yapılmalı, client input almamalı
- MIME type validation gerekli — provider'ın döndüğü dosya gerçekten image mi?
- File size limit — DoS koruması
- Image dimension validation — beklenen boyutlar mı?
- Checksum/hash doğrulama — integrity check
- Storage bucket/prefix isolation — tenant separation (multi-tenant durumda)

### C. Preview/Proxy Güvenliği

- Preview endpoint **backend proxy** veya **short-lived authorized token** ile yapılmalı
- Doğrudan storage URL expose etmek güvenlik ihlali
- Signed URL kalıcı field olarak tutulmamalı — her request'te yeniden üretilmeli
- Public URL güvenlik riski — erişim kontrolü yok

### D. Review Gate Bütünlüğü

- Storage eklenmesi review gate'i bypass etmemeli
- Asset storage sonrası otomatik APPROVED olmamalı
- Storage sadece metadata handoff — approval ayrı admin action

---

## 3. Future Safe Storage Handoff Contract

Aşağıdaki contract **henüz implement edilmemiştir**. Gelecek task için tasarım referansıdır.

### A. Storage Service Boundary

```
┌───────────────┐     ┌──────────────────┐     ┌────────────────┐
│  Provider GW  │ ──► │  Storage Service  │ ──► │  Object Store  │
│  (metadata +  │     │  (download,       │     │  (S3/MinIO/    │
│   temp ref)   │     │   validate,       │     │   GCS/local)   │
│               │     │   upload,         │     │                │
│               │     │   key gen)        │     │                │
└───────────────┘     └──────────────────┘     └────────────────┘
                              │
                              ▼
                      ┌──────────────────┐
                      │  MediaAsset      │
                      │  (safe metadata  │
                      │   only)          │
                      └──────────────────┘
```

### B. Storage Service Sorumlulukları

| Sorumluluk | Açıklama |
|------------|----------|
| Provider output download | Server-side, temp buffer, never persisted raw |
| MIME validation | Content-Type + magic bytes check |
| Size limit | Configurable max file size |
| Dimension validation | Width/height within expected range |
| Checksum | SHA-256 hash hesaplama |
| Object key generation | Server-side deterministic key, no client input |
| Upload to object store | Stream-based, temp file cleanup |
| MediaAsset metadata update | storageObjectKey internal-only |

### C. MediaAsset Safe Metadata (Future)

MediaAsset entity'de tutulabilecek **internal** storage metadata:

| Field | Açıklama | DTO'da? |
|-------|----------|---------|
| `mimeType` | image/png, image/jpeg | ✅ |
| `width` | Pixel cinsinden genişlik | ✅ |
| `height` | Pixel cinsinden yükseklik | ✅ |
| `fileSizeBytes` | Dosya boyutu | ✅ |
| `checksumSha256` | Integrity hash | ✅ |
| `storageProviderAlias` | S3/MinIO/GCS vs. | ⛔ Internal only |
| `storageObjectKey` | Bucket-relative path | ⛔ Internal only |
| `storageBucket` | Bucket adı | ⛔ Internal only |

### D. Preview Contract (Future)

| Yöntem | Açıklama | Güvenlik |
|--------|----------|----------|
| Backend proxy | `/api/admin/.../preview` endpoint image binary stream döner | Auth + rate limit |
| Short-lived signed URL | Her request'te yeni signed URL üretilir, kalıcı tutulmaz | TTL ≤ 15 dk |

Preview endpoint aktif edilene kadar **501** kalmalıdır.

---

## 4. Forbidden Storage Behaviors

Aşağıdaki davranışlar **kesinlikle yasaktır**:

### Provider Output

- ⛔ Provider image URL'i persist etme
- ⛔ Provider image URL'i admin DTO'da expose etme
- ⛔ Provider image URL'i log'a yazma
- ⛔ Base64 image data'yı DB'de persist etme
- ⛔ Base64 image data'yı DTO'da döndürme
- ⛔ Base64 image data'yı log'a yazma
- ⛔ Image binary'yi log'a yazma

### Storage References

- ⛔ Signed URL'i kalıcı entity field olarak persist etme
- ⛔ Signed URL'i admin response DTO'da expose etme
- ⛔ Public URL üretme veya expose etme
- ⛔ CDN URL üretme veya expose etme
- ⛔ Storage object key'i admin response DTO'da döndürme
- ⛔ Storage bucket/prefix bilgisini DTO'da döndürme

### Workflow

- ⛔ Storage sonrası automatic APPROVED yapma
- ⛔ Storage sonrası automatic READY_FOR_PUBLISH yapma
- ⛔ Storage sonrası publish/export tetikleme
- ⛔ Review gate'i bypass etme
- ⛔ Generic status update endpoint ekleme

### Logging

- ⛔ Storage object key'i admin-facing error message'da gösterme
- ⛔ API key/secret loglama
- ⛔ Provider config loglama
- ⛔ Image binary/base64 loglama

---

## 5. Future Task Candidate

Storage implementation **ayrı kontrollü task** olarak planlanmalıdır:

| Task | Açıklama | Bağımlılık |
|------|----------|------------|
| Safe Storage Handoff Implementation | Provider output → object store pipeline | Storage SDK decision |
| Safe Preview Proxy | Backend proxy endpoint for admin preview | Storage ready |
| FP-007 Final Closure | Feature operational baseline | All above |

### FP-007 Closure Decision

FP-007 closure **iki şekilde** yapılabilir:

**Option A — Storage Deferred Closure:**
- FP-007'yi mevcut metadata-only state ile kapatır
- Storage implementation ayrı feature'a (FP-008 veya benzeri) taşınır
- Avantaj: Clean closure, minimal risk
- Dezavantaj: Storage ayrı feature track

**Option B — Storage Within FP-007:**
- Storage implementation FP-007 içinde küçük task olarak eklenir
- Preview proxy ayrı task
- Closure storage sonrası
- Avantaj: End-to-end pipeline tek feature'da
- Dezavantaj: FP-007 scope genişlemesi

Her iki durumda da **forbidden behaviors** korunmalıdır.

---

## 6. Guardrail Summary

Mevcut test guardrail'leri storage handoff planning ile uyumlu:

| Guardrail | Test Sınıfı | Durum |
|-----------|-------------|-------|
| DTO forbidden fields | `ImageGenerationSafetyGuardrailTest.DtoSafety` | ✅ Aktif |
| Error normalizer safety | `ImageGenerationSafetyGuardrailTest.ErrorNormalizerTests` | ✅ Aktif |
| Status enum guards | `ImageGenerationSafetyGuardrailTest.StatusGuards` | ✅ Aktif |
| Provider result no URL/base64 | `ImageGenerationStorageHandoffGuardrailTest` | ✅ **Task 13 eklendi** |
| Provider adapter safety | `OpenAiImageGenerationProviderGatewayTest` | ✅ Aktif |

---

*Bu doküman FP-007 Task 13 kapsamında oluşturulmuştur. Implementation yok — planning-only.*
