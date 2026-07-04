# FP-008 — Safe Storage Handoff & Preview Proxy — Product Design

> **Feature**: FP-008 — Safe Storage Handoff & Preview Proxy  
> **Status**: Draft  
> **Task**: FP-008 Task 01 — Product Design Document  
> **Scope**: Documentation-only  
> **Predecessor**: FP-007 — Image Generation & Media Asset Pipeline (CLOSED, commit `9b758b5`)  
> **FP-007 Closure Doc**: [58-fp007-final-closure-operational-baseline.md](../05-engineering/58-fp007-final-closure-operational-baseline.md)  
> **FP-007 Storage Planning**: [57-fp007-safe-provider-output-storage-handoff-planning.md](../05-engineering/57-fp007-safe-provider-output-storage-handoff-planning.md)

---

## 1. Feature Overview

FP-008, FP-007'de bilinçli olarak deferred bırakılan **storage handoff** ve **preview proxy** konusunu güvenli şekilde ele alır.

### FP-007 Final State (Referans)

FP-007'de provider integration **metadata-only** olarak kapandı:

- Provider image URL/base64 persist edilmiyordu
- MediaAsset kaydı mock/safe metadata ile oluşuyordu
- Preview endpoint 501 olarak bilerek kapalı kaldı
- Storage upload/download intentionally deferred idi
- Human review gate aktifti; AI output otomatik APPROVED olmuyordu

### FP-008 Amacı

FP-008, bu deferred sınırı güvenli şekilde açar:

- Provider output'u **server-side kontrollü** şekilde ingest eder
- MIME, boyut ve bütünlük doğrulaması yapar
- Güvenli storage metadata üretir
- Admin review ekranına **backend-controlled preview** sağlar

### FP-008 Değildir

- Publish/export pipeline değildir
- Public media serving değildir
- CDN/distribution çözümü değildir
- Human review gate'i bypass eden bir mekanizma değildir
- **Preview, approval anlamına gelmez**

---

## 2. Product Problem

### Mevcut Durum

FP-007'de `GeneratedAsset` review kuyruğuna düşüyor. Admin approve/reject yapabiliyor. Ancak:

- Admin, review sırasında **gerçek görseli göremiyor** — preview endpoint 501
- Provider URL'leri geçici ömürlüdür; kalıcı saklanması güvenli değildir
- Base64/image binary veritabanında veya DTO'da taşınmamalıdır
- Doğrudan public/signed URL exposure güvenlik riski oluşturur
- Storage lifecycle, validation ve access control product seviyesinde netleşmemiştir

### Sonuç

Admin, AI'ın ürettiği görseli **görmeden** approve/reject kararı vermek zorunda kalıyor. Bu, review kalitesini düşürür ve human review gate'in amacını zayıflatır.

---

## 3. Product Goals

| # | Hedef |
|---|-------|
| 1 | Provider output'u server-side güvenli ingestion akışına almak |
| 2 | MIME type, dosya boyutu, image dimensions ve checksum validation kurallarını tanımlamak |
| 3 | MediaAsset safe metadata'yı gerçek storage handoff sonrası güncelleyebilmek |
| 4 | Backend-controlled preview proxy ile admin review ekranına güvenli preview sağlamak |
| 5 | Public/signed URL exposure'ı engellemek |
| 6 | Storage object key'i admin/user-facing response'lardan gizlemek |
| 7 | Human review gate'i korumak — preview ≠ approval |
| 8 | Approved/rejected review workflow'u bozmamak |
| 9 | Publish/export scope dışında tutmak |

---

## 4. Non-Goals

Aşağıdakiler FP-008 scope'u dışındadır:

| Non-Goal | Neden |
|----------|-------|
| Publish/export automation | Ayrı feature konusu |
| READY_FOR_PUBLISH status | Scope dışı |
| Generic status update endpoint | Güvenlik riski — explicit action tercih |
| Public media gallery | User-facing serving ayrı alan |
| User-facing preview/media serving | Sadece admin review preview |
| Direct signed URL exposure | Güvenlik riski |
| Public URL generation | Güvenlik riski |
| CDN optimization | Dağıtım altyapısı ayrı konu |
| Image editing/cropping | Post-processing ayrı feature |
| Video/audio storage | FP-008 sadece image scope |
| Bulk ingestion | Kontrollü tek-item handoff tercih |
| Regeneration/variation flow | Future enhancement |
| Multi-provider failover | Future enhancement |
| Provider cost optimization | Future enhancement |
| Advanced moderation automation | Human review gate korunuyor |
| AI output automatic approval | Kesinlikle kapsam dışı |
| Raw provider payload/response inspection | Güvenlik yasağı |

---

## 5. Core Product Flow

```
┌──────────────────────────────────────────────────────────────┐
│  FP-007 OUTPUT                                               │
│  GeneratedAsset (GENERATED_PENDING_REVIEW)                   │
│  + Provider output reference (internal, short-lived)         │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  SERVER-SIDE STORAGE HANDOFF                                 │
│                                                              │
│  1. Provider output reference internal-only olarak işlenir   │
│  2. Backend server-side download/ingestion yapar             │
│  3. MIME type validation                                     │
│  4. File size validation                                     │
│  5. Image dimension (width/height) okunur                    │
│  6. Checksum (SHA-256) hesaplanır                            │
│  7. Storage object key server-side üretilir                  │
│  8. Object store'a upload                                    │
│  9. MediaAsset safe metadata güncellenir                     │
│ 10. Geçici provider reference temizlenir                     │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  ADMIN PREVIEW                                               │
│                                                              │
│  • Backend-controlled proxy endpoint                         │
│  • Admin raw storage key, signed URL, public URL görmez      │
│  • Preview auth-controlled                                   │
│  • Preview ≠ approval                                        │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│  HUMAN REVIEW (FP-007 — unchanged)                           │
│                                                              │
│  • Admin preview ile görseli inceler                          │
│  • Explicit approve action → APPROVED                        │
│  • Explicit reject action → REJECTED (reason zorunlu)        │
│  • Review audit kaydı oluşturulur                            │
│  • Publish/export hâlâ yok                                   │
└──────────────────────────────────────────────────────────────┘
```

### Akış Kuralları

- Provider output reference **internal-only** tutulur; kısa ömürlü olarak işlenir
- Download/ingestion **sadece backend** tarafında yapılır
- Admin hiçbir zaman raw provider URL veya storage key görmez
- Storage handoff tamamlanınca asset **otomatik APPROVED olmaz**
- Preview görmek asset'i **APPROVED yapmaz**
- APPROVED sadece explicit approve action ile olur

---

## 6. Storage Safety Principles

### Kesin Kurallar

| Kural | Açıklama |
|-------|----------|
| Provider image URL persist edilmez | Geçici reference olarak kullanılır, sonra temizlenir |
| Provider image URL admin/user-facing olmaz | Internal-only |
| Base64 DB'ye yazılmaz | Performans ve güvenlik riski |
| Base64 DTO'ya dönmez | Admin/user response'a çıkmaz |
| Image binary loglanmaz | Log boyutu ve güvenlik |
| Storage object key admin response'a çıkmaz | Internal-only metadata |
| Signed URL kalıcı metadata olarak saklanmaz | Her request'te yeni üretilmeli |
| Public URL üretilmez | Erişim kontrolü gerekli |
| Storage provider detayları admin UI'a taşınmaz | Internal infrastructure |
| MediaAsset sadece safe metadata taşır | Allowlist yaklaşımı |

### Preview Güvenliği

- Preview, **backend proxy** veya **authorization-controlled short-lived internal mechanism** ile yapılmalıdır
- Doğrudan storage URL expose etmek yasaktır
- Preview endpoint auth check gerektirir
- Preview access audit/observability amaçlı izlenebilir olmalıdır

---

## 7. Validation Requirements

### MIME Type Validation

| Rule | Detail |
|------|--------|
| Allowed MIME types | `image/png`, `image/jpeg`, `image/webp` |
| MIME detection | Content-Type header + magic bytes (content sniffing) |
| Content type spoofing | Sadece header'a güvenme; dosya içeriğinden doğrula |
| Non-image content | Reddedilir — storage handoff failure |

### File Size Validation

| Rule | Detail |
|------|--------|
| Max file size | Product-level limit tanımlanır; kesin değer Engineering'de belirlenir |
| Zero-byte file | Reddedilir |
| Oversized file | Reddedilir — storage handoff failure |

### Image Dimension Validation

| Rule | Detail |
|------|--------|
| Width/height | Okunur ve metadata'ya kaydedilir |
| Min/max dimension | Product-level sınır tanımlanabilir; Engineering'de finalize |
| Invalid/corrupt image | Reddedilir |

### Integrity / Checksum

| Rule | Detail |
|------|--------|
| Hash algorithm | SHA-256 |
| Checksum hesaplama | İndirme sırasında veya hemen sonra |
| Checksum persistence | MediaAsset safe metadata'da tutulur |

### Failure Handling

| Rule | Detail |
|------|--------|
| Provider output erişilemez | Safe storage handoff failure |
| Validation failure | Asset'i otomatik REJECTED yapmaz |
| Storage upload failure | Storage handoff failure olarak kalır |
| Human review gate | Korunur — validation failure review kararı değildir |

---

## 8. MediaAsset Metadata

### Admin-Facing Safe Metadata

| Field | Açıklama | Admin DTO? |
|-------|----------|------------|
| `mimeType` | image/png, image/jpeg, image/webp | ✅ Evet |
| `width` | Pixel cinsinden genişlik | ✅ Evet |
| `height` | Pixel cinsinden yükseklik | ✅ Evet |
| `fileSizeBytes` | Dosya boyutu (bytes) | ✅ Evet |
| `checksumSha256` | SHA-256 integrity hash | ✅ Evet |
| `storedAt` | Storage handoff tamamlanma zamanı | ✅ Evet |
| `storageHandoffStatus` | Storage durumu (product concept) | ✅ Evet |

### Internal-Only Metadata (Admin/User DTO'da YOK)

| Field | Açıklama | Admin DTO? |
|-------|----------|------------|
| `storageProviderAlias` | S3/MinIO/GCS vs. | ⛔ Internal only |
| `storageObjectKey` | Bucket-relative path | ⛔ Internal only |
| `storageBucket` | Bucket adı | ⛔ Internal only |
| `providerImageUrl` | Geçici provider URL — handoff sonrası temizlenir | ⛔ Internal only |

### Kesinlikle Olmayacak Alanlar

- `signedUrl` — kalıcı field olarak tutulmaz
- `publicUrl` — üretilmez
- `cdnUrl` — üretilmez
- `base64` — DB'ye yazılmaz
- `imageBinary` — DB'ye yazılmaz
- `rawProviderResponse` — persist edilmez
- `rawProviderRequest` — persist edilmez
- `internalFullPrompt` — persist edilmez
- `apiKey` / `secret` — asla

---

## 9. Preview Proxy Product Expectations

### Admin Preview Deneyimi

| Beklenti | Açıklama |
|----------|----------|
| Erişim kontrolü | Sadece admin/review yetkili kullanıcıya açık |
| Backend-controlled | Preview endpoint doğrudan storage reference expose etmez |
| Content delivery | Image binary stream veya safe response — implementation Engineering'e bırakılır |
| No URL exposure | Storage key, signed URL, public URL döndürülmez |
| Traceability | Preview access correlation/asset ID ile izlenebilir |
| No approval side-effect | Preview görmek review kararı üretmez |
| Unavailable handling | Preview hazır değilse safe error/status gösterilir |

### Preview Durumları

| Durum | Admin Deneyimi |
|-------|----------------|
| Storage handoff tamamlanmamış | "Preview henüz hazır değil" mesajı |
| Storage handoff başarısız | "Preview oluşturulamadı" safe error |
| Storage handoff başarılı | Preview görüntülenebilir |
| Asset approved/rejected | Preview hâlâ erişilebilir (review reference) |

---

## 10. Human Review Preservation

FP-007'de kurulan human review gate FP-008'de **hiçbir şekilde** değişmez:

| Kural | Durum |
|-------|-------|
| Storage handoff tamamlanınca GeneratedAsset otomatik APPROVED olmaz | ✅ Korunur |
| Preview görüldüğünde GeneratedAsset otomatik APPROVED olmaz | ✅ Korunur |
| APPROVED sadece explicit approve action ile olur | ✅ Korunur |
| REJECTED sadece explicit reject action ile olur | ✅ Korunur |
| Reject reason zorunlu kalır (`@NotBlank`) | ✅ Korunur |
| `GeneratedAssetReview` audit kaydı oluşturulur | ✅ Korunur |
| `ImageMediaAsset` review status güncellenir | ✅ Korunur |
| Publish/export hâlâ yok | ✅ Korunur |

---

## 11. Governance / Privacy / Security Rules

### Açık Yasak Listesi

| Yasak | Kategori |
|-------|----------|
| Raw provider payload exposure | Provider güvenliği |
| Raw provider response exposure | Provider güvenliği |
| Internal prompt exposure | Prompt güvenliği |
| API key/secret exposure | Secret management |
| Provider image URL persistence | URL lifecycle riski |
| Provider image URL exposure | Admin güvenliği |
| Public URL generation | Erişim kontrolü |
| Signed URL exposure | Kalıcı persist riski |
| Storage object key exposure | Internal infrastructure |
| Base64 in DTO | Performans + güvenlik |
| Image binary DB persistence | Performans + güvenlik |
| Generic status update | Güvenlik riski |
| READY_FOR_PUBLISH | Scope dışı |
| Automatic approval | Human review ihlali |
| Publish/export | Scope dışı |
| Video/audio | Scope dışı |

### Zorunlu Güvenlik Kuralları

| Kural | Açıklama |
|-------|----------|
| Server-side ingestion | Provider output sadece backend indirir |
| Metadata allowlist | MediaAsset sadece allowlist metadata taşır |
| Backend-controlled preview | Proxy veya auth-controlled mekanizma |
| Authorization check | Preview endpoint auth gerektirir |
| Safe normalized errors | Raw error dump yok |
| Audit-friendly lineage | CorrelationId, assetId izlenebilirlik |
| Human review gate | Approve/reject sadece explicit action |

---

## 12. Admin Experience

### Admin Product Flow

```
1. Admin → Generated Assets Pending Review listesi
2. Admin → Asset detayına girer
3. System → Storage handoff durumunu gösterir
   ├── STORAGE_PENDING: "Storage işleniyor..."
   ├── STORAGE_STORED: Preview hazır
   └── STORAGE_FAILED: "Storage hatası" safe mesaj
4. Admin → Preview açar (storage hazırsa)
   → Backend proxy üzerinden image stream
   → Raw storage key / URL görünmez
5. Admin → Approve veya Reject
   → Explicit action, preview'dan bağımsız
   → Reject: reason zorunlu
6. System → Review audit kaydı oluşturur
```

### Admin Görmeyecekleri

- Provider URL
- Signed URL
- Storage object key
- Storage bucket/prefix
- Raw provider response
- Base64 data
- API key/config

---

## 13. Status / State Language

### GeneratedAsset Status (FP-007 — değişmez)

| Status | Anlamı |
|--------|--------|
| `GENERATED_PENDING_REVIEW` | Review kuyruğunda |
| `APPROVED` | Admin explicit approve |
| `REJECTED` | Admin explicit reject |

### Storage Handoff Status (FP-008 — yeni product concept)

| Status | Anlamı |
|--------|--------|
| `STORAGE_PENDING` | Handoff henüz başlamadı veya devam ediyor |
| `STORAGE_VALIDATING` | MIME/size/dimension validation sürüyor |
| `STORAGE_STORED` | Başarıyla depolandı, preview hazır |
| `STORAGE_FAILED` | Handoff başarısız — safe error |

### Preview Availability (Derived State)

| Status | Anlamı |
|--------|--------|
| `PREVIEW_UNAVAILABLE` | Storage henüz tamamlanmamış veya başarısız |
| `PREVIEW_AVAILABLE` | Storage tamamlandı, preview proxy hazır |

### Kurallar

- Storage status, **review status değildir**
- Preview status, **approval status değildir**
- `READY_FOR_PUBLISH` kullanılmaz
- `COMPLETED` gibi generic status kullanılmaz
- Generic status update endpoint kullanılmaz
- Storage failure asset'i otomatik REJECTED yapmaz

---

## 14. Acceptance Criteria

| Kriter | Durum |
|--------|-------|
| Product design doc target path'te oluşturuldu | ✅ |
| FP-007 closure doc referans alındı | ✅ |
| FP-008 scope safe storage handoff + preview proxy ile sınırlı | ✅ |
| Publishing/export dışarıda | ✅ |
| READY_FOR_PUBLISH dışarıda | ✅ |
| Generic status update dışarıda | ✅ |
| Provider URL/base64 persistence yasaklandı | ✅ |
| Public/signed URL exposure yasaklandı | ✅ |
| Storage object key exposure yasaklandı | ✅ |
| Backend-controlled preview product expectation tanımlandı | ✅ |
| Human review gate korunuyor | ✅ |
| AI output automatic approval yok | ✅ |
| Video/audio dışarıda | ✅ |
| Next-step handoff to AI Platform / Media Contract Design eklendi | ✅ |

---

## 15. Future Handoff — FP-008 Task 02

FP-008 Task 02, **AI Platform / Media Contract Design** olarak planlanmaktadır.

Task 02'de ele alınacak başlıklar:

| Konu | Açıklama |
|------|----------|
| Provider output reference boundary | Internal-only, short-lived, temizleme kuralı |
| Storage handoff request/result contract | Ingestion trigger, completion signal |
| Media validation result contract | MIME/size/dimension/checksum sonuçları |
| Preview proxy contract | Auth, streaming, error response |
| Safe metadata allowlist | MediaAsset update allowlist |
| Storage error normalization | Normalized error codes |
| No raw provider URL exposure | Contract-level yasak |
| No signed/public URL exposure | Contract-level yasak |
| No storage object key admin exposure | Contract-level yasak |
| Human review gate preservation | Contract-level garanti |

---

*Bu doküman FP-008 Task 01 kapsamında oluşturulmuştur. Documentation-only — kod/config/runtime değişikliği yoktur.*
