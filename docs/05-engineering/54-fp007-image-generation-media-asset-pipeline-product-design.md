# 54 — FP-007 Image Generation & Media Asset Pipeline — Product Design

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-007 — Image Generation & Media Asset Pipeline
>
> **Prerequisites**: FP-001 (CLOSED), FP-002 (CLOSED), FP-003 (CLOSED), FP-004 (CLOSED), FP-005 (CLOSED), FP-006 (CLOSED)
>
> **Task**: Task 01 — Product Design Document (documentation-only)

---

## 1. Feature Overview

FP-007, **gerçek image generation ve media asset pipeline** özelliğidir.

FP-006'da tasarlanan, düzenlenen ve onaylanan görsel prompt planlarını (**APPROVED VisualAssetPlan**) kaynak alarak kontrollü şekilde gerçek image generation sürecine geçişi sağlar. Üretilen görseller, metadata ve storage referanslarıyla birlikte kaydedilir ve zorunlu insan review sürecinden geçer.

### Temel Pozisyon

| Alan | Açıklama |
|------|----------|
| **Başlangıç noktası** | Yalnızca **APPROVED VisualAssetPlan** |
| **Çıktı** | Gerçek image dosyası + metadata + storage referansı |
| **Review** | Zorunlu insan review — AI çıktısı asla otomatik onaylanmaz |
| **Publish** | ❌ Bu feature'da publish yok |
| **Kapsam** | Görsel üretim + storage + review — kontrollü ve gated |

### FP-006'dan Devir

FP-006 aşağıdakileri tamamlayarak kapandı (commit: `3e00dfa`):

- VisualAssetPlan / SceneVisualPlan / ThumbnailPlan domain modeli
- AI ile prompt planlama (mock + OpenAI text provider)
- Admin review/edit/approve/reject workflow'u
- `englishImagePrompt`, `thumbnailPrompt` gibi ürün çıktıları
- Local ve staging validasyon (31 + 32 smoke test)

FP-007, bu planlanmış ve onaylanmış prompt metinlerini gerçek image generation tool'larına ileterek somut görsel çıktı üretmeye başlar.

> **Önemli**: FP-007, gerçek görsel üretimi başlatır ancak hâlâ **kontrollü**, **review-gated** ve **publish dışı** bir feature'dır. Üretilen görseller admin review'dan geçmeden hiçbir dış sisteme aktarılamaz.

---

## 2. Product Problem

### Mevcut Durum

FP-006 ile görsel planlama süreci tamamlandı. Artık elimizde şunlar var:

- Her sahne için `englishImagePrompt` — image generation tool'a doğrudan verilebilecek prompt
- `negativePrompt` — istenmeyen elementleri belirten negatif yönlendirme
- `styleDirective`, `compositionNotes`, `cameraFraming`, `lightingNotes` — görsel stil yönlendirmeleri
- `thumbnailPrompt` — thumbnail görseli için optimize edilmiş prompt
- Tüm bu planlar **APPROVED** durumunda, insan tarafından onaylanmış

### Problem

1. **Prompt → görsel dönüşüm boşluğu**: Onaylanmış prompt'lar mevcut ama bunları gerçek görsele çevirecek pipeline yok.

2. **Governance gereksinimi**: AI image generation çıktısı doğrudan kullanıcıya veya publish akışına gitmemeli. Her üretilen görsel insan tarafından kalite, uygunluk ve plan uyumu açısından kontrol edilmeli.

3. **Metadata ve izlenebilirlik**: Üretilen görsellerin hangi plan, sahne ve prompt'tan türediği takip edilebilmeli. Provider detayları güvenli şekilde izole edilmeli.

4. **Storage yönetimi**: Üretilen görsellerin güvenli şekilde saklanması, referanslanması ve lifecycle'ının yönetilmesi gerekiyor.

5. **Güvenlik sınırları**: API key, secret, raw provider payload/response ve internal prompt bilgilerinin hiçbir kullanıcı/admin yüzeyine sızmaması garanti altına alınmalı.

---

## 3. Product Goals

| # | Hedef |
|---|-------|
| G1 | APPROVED VisualAssetPlan üzerinden kontrollü image generation request oluşturmak |
| G2 | Image generation çıktısını **GeneratedAsset** kavramıyla takip etmek |
| G3 | Üretilen görselleri **MediaAsset** olarak storage referansı ve metadata ile saklamak |
| G4 | Her üretilen görselde zorunlu **admin review** süreci uygulamak |
| G5 | Generated image'lar için **approve/reject** workflow'u desteklemek |
| G6 | Her generated image'ın kaynak VisualAssetPlan ve SceneVisualPlan/ThumbnailPlan ile **izlenebilir** ilişkisini korumak |
| G7 | Provider detaylarını (API key, raw payload, raw response) admin-facing yüzeylerden **tamamen izole** etmek |
| G8 | Secret, API key, raw payload, raw response ve internal prompt sızıntısını **engellemek** |
| G9 | Güvenli hata yönetimi — provider/storage hatalarını kullanıcıya güvenli error code/message ile göstermek |

---

## 4. Non-Goals

Aşağıdakiler FP-007 kapsamında **kesinlikle yoktur**:

| # | Kapsam Dışı | Açıklama |
|---|-------------|----------|
| NG1 | Video generation | Görsel üretim yalnızca statik image |
| NG2 | Audio generation | Ses/müzik üretimi yok |
| NG3 | Publishing automation | Otomatik yayın yok |
| NG4 | `READY_FOR_PUBLISH` status | Bu status FP-007'de kullanılmaz |
| NG5 | Generic status update endpoint | `PUT .../status` tarzı endpoint yok |
| NG6 | AI output otomatik APPROVED | AI çıktısı asla otomatik onaylanmaz |
| NG7 | Public media gallery | Kullanıcıya açık görsel galerisi yok |
| NG8 | User-facing image generation UI | Kullanıcı tarafı generation UI yok |
| NG9 | Bulk generation | Toplu üretim yok |
| NG10 | Regeneration variations | Varyasyon/alternatif üretimi yok |
| NG11 | Prompt marketplace / editing UI | Prompt düzenleme FP-006'da tamamlandı |
| NG12 | Provider cost optimization | Maliyet optimizasyonu yok |
| NG13 | Advanced moderation automation | Otomatik moderasyon yok |
| NG14 | CDN optimization | CDN dağıtım optimizasyonu yok |
| NG15 | Asset cropping/editing tools | Görsel düzenleme araçları yok |
| NG16 | Multi-provider failover | Birden fazla provider arası otomatik geçiş yok |
| NG17 | Direct provider payload/response inspection | Raw provider verisi admin'e gösterilmez |

---

## 5. Core Product Flow

### Akış Diyagramı

```
APPROVED VisualAssetPlan (FP-006)
    │
    ▼
[Admin: Generate Images]
    │
    ▼
SceneVisualPlan #1 ──→ Image Generation Request ──→ Provider
SceneVisualPlan #2 ──→ Image Generation Request ──→ Provider
    ...
ThumbnailPlan ────→ Image Generation Request ──→ Provider
    │
    ▼
Provider Response (image data/URL)
    │
    ▼
Storage'a kaydet (MinIO/S3)
    │
    ▼
MediaAsset kaydı oluştur (storage ref, mime, dimensions)
    │
    ▼
GeneratedAsset kaydı oluştur (source lineage, status: GENERATED_PENDING_REVIEW)
    │
    ▼
[Admin: Review Generated Image]
    │
    ├── APPROVE ──→ APPROVED (production handoff adayı — ama FP-007'de publish yok)
    │
    └── REJECT ──→ REJECTED (reason kaydedilir)
```

### Akış Kuralları

1. **Yalnızca APPROVED VisualAssetPlan** için generation başlatılabilir. Draft, NEEDS_REVIEW veya REJECTED plan kaynak olamaz.

2. Her SceneVisualPlan ve ThumbnailPlan için **ayrı ve kontrollü** image generation request oluşturulur.

3. Provider sonucu **doğrudan APPROVED olmaz** — mutlaka `GENERATED_PENDING_REVIEW` ara durumundan geçer.

4. Generated image metadata kaydedilir: provider type, model, dimensions, mime type, creation timestamp.

5. Storage'da object referansı oluşturulur (bucket, key, checksum).

6. Admin generated image'ı **preview** olarak görür, kalite ve plan uyumunu kontrol eder.

7. Admin **approve** veya **reject** kararı verir. Reject halinde reason kaydedilir.

8. Approved generated image, ileride production handoff/export süreçlerine aday olabilir ama **FP-007'de publish edilmez**.

---

## 6. State Model / Status Language

### Kaynak Durumu (Precondition)

Image generation başlatılabilmesi için:

- VisualAssetPlan status = **APPROVED**
- SceneVisualPlan / ThumbnailPlan'ın bağlı olduğu VisualAssetPlan APPROVED olmalı

### GeneratedAsset Status Lifecycle

```
REQUESTED ──→ GENERATING ──→ GENERATED_PENDING_REVIEW ──→ APPROVED
                    │                    │
                    ▼                    ▼
                  FAILED              REJECTED
```

| Status | Açıklama |
|--------|----------|
| `REQUESTED` | Image generation talebi oluşturuldu, henüz provider'a gönderilmedi |
| `GENERATING` | Provider'a gönderildi, yanıt bekleniyor |
| `GENERATED_PENDING_REVIEW` | Görsel üretildi, storage'a kaydedildi, admin review bekliyor |
| `APPROVED` | Admin tarafından onaylandı — production handoff adayı |
| `REJECTED` | Admin tarafından reddedildi — publish/export adayı olamaz |
| `FAILED` | Provider/storage/generation hatası — güvenli error code ile temsil edilir |

### Durum Kuralları

| Kural | Açıklama |
|-------|----------|
| AI çıktısı asla otomatik APPROVED olamaz | `GENERATED_PENDING_REVIEW` zorunlu ara aşamadır |
| APPROVED sadece human review sonrası | Admin onayı olmadan APPROVED'a geçilemez |
| REJECTED asset publish/export adayı olamaz | Reddedilen asset kullanılamaz |
| FAILED provider/storage hatasını temsil eder | Teknik hata, admin'e güvenli mesajla gösterilir |
| `READY_FOR_PUBLISH` kullanılmaz | ❌ FP-007'de bu status yok |
| Generic status update yok | ❌ `PUT .../status` endpoint'i yok |

---

## 7. MediaAsset / GeneratedAsset Product Concepts

### Kavram Ayrımı

FP-007 iki farklı ama ilişkili kavram tanımlar:

#### GeneratedAsset

AI provider tarafından üretilmiş, review bekleyen veya review edilmiş **üretim çıktısı kaydıdır**.

| Özellik | Açıklama |
|---------|----------|
| **Amaç** | AI image generation çıktısının lifecycle takibi |
| **Kaynak ilişkisi** | Mutlaka kaynak VisualAssetPlan + SceneVisualPlan veya ThumbnailPlan ile ilişkili |
| **Status** | REQUESTED → GENERATING → GENERATED_PENDING_REVIEW → APPROVED/REJECTED/FAILED |
| **Review** | Human review zorunlu |
| **Lineage** | Hangi plan, sahne, prompt'tan türediği izlenebilir |

#### MediaAsset

Sistemde saklanan medya varlığı için **genel, yönetilebilir asset kaydıdır**.

| Özellik | Açıklama |
|---------|----------|
| **Amaç** | Storage referansı ve teknik metadata |
| **İçerik** | Storage key, mime type, dimensions (width/height), file size, checksum/hash |
| **İlişki** | GeneratedAsset'in fiziksel dosya referansı |
| **Güvenlik** | Doğrudan provider response veya raw data içermez |

### Governance Sınırları

| Kural | Açıklama |
|-------|----------|
| GeneratedAsset mutlaka kaynak plan ile ilişkilendirilmeli | Orphan asset oluşmamalı |
| MediaAsset güvenli metadata taşır | mime type, dimensions, checksum — güvenli |
| Raw provider response saklanmamalı veya admin-facing olmamalı | Provider JSON/payload güvenlik sınırı dışında |
| Internal prompt admin-facing olmamalı | AI orchestration prompt'u kullanıcıya gösterilmez |
| Güvenli prompt summary düşünülebilir | Redacted/safe özet yaklaşımı — detay AI Platform doc'da |
| Provider request ID internal olabilir | Debugging için saklanabilir ama admin UI'da expose edilmez |
| API key/secret hiçbir yüzeyde gösterilmez | Dokümanda bile örnek olarak yazılmaz |

---

## 8. Human Review Requirements

### Zorunlu Review Süreci

Her generated image, admin tarafından insan review'dan geçmek **zorundadır**.

| Gereksinim | Açıklama |
|------------|----------|
| **Kalite kontrolü** | Görsel teknik kalite, çözünürlük ve netlik açısından yeterli mi? |
| **Plan uyumu** | Üretilen görsel, kaynak SceneVisualPlan/ThumbnailPlan'daki prompt ve yönlendirmelere uygun mu? |
| **Kültürel uygunluk** | Anadolu/kültürel bağlam doğru yansıtılmış mı? Hassas veya yanlış temsil var mı? |
| **Güvenlik** | Uygunsuz, hassas veya zararlı içerik var mı? |

### Approve / Reject Kararı

| Aksiyon | Detay |
|---------|-------|
| **Approve** | Admin görseli uygun bulur → status = APPROVED |
| **Reject** | Admin görseli uygun bulmaz → status = REJECTED |
| **Rejection reason** | **Zorunlu** — reddedilen her asset için neden belirtilmelidir |
| **Audit** | Approve/reject kararı auditlenebilir olmalıdır (who, when, reason) |

### Kısıtlamalar

- Human review tamamlanmadan hiçbir image **production/export/publish** adayı olamaz.
- AI çıktısı hiçbir zaman otomatik APPROVED olamaz.
- Review süreci yalnızca admin tarafından gerçekleştirilebilir.

---

## 9. Governance, Safety and Privacy Rules

### Güvenlik Kuralları

| # | Kural |
|---|-------|
| S1 | **Secrets expose edilmez** — API key, token, credential hiçbir yüzeyde gösterilmez |
| S2 | **API key expose edilmez** — env variable, config veya DB'den admin UI/API'ye aktarılmaz |
| S3 | **Raw provider payload expose edilmez** — provider'a gönderilen HTTP request body gösterilmez |
| S4 | **Raw provider response expose edilmez** — provider'dan dönen HTTP response body gösterilmez |
| S5 | **Internal prompt expose edilmez** — AI orchestration system/user prompt gösterilmez |
| S6 | **Provider-specific implementation details** product UI'a taşınmaz |
| S7 | **Admin sadece güvenli metadata** ve image preview görür |
| S8 | **Source lineage korunur** — her generated image'ın hangi plan/sahne/prompt'tan türediği izlenebilir |
| S9 | **Audit trail gerekir** — generation request, review kararı, timing bilgisi |
| S10 | **Failed generation detayları** güvenli error code/message ile temsil edilir — raw error dump gösterilmez |

### İzin Verilenler

| Alan | Durum |
|------|-------|
| Generated image preview | ✅ Admin'e gösterilir |
| Dimensions (width × height) | ✅ Güvenli metadata |
| Mime type | ✅ Güvenli metadata |
| File size | ✅ Güvenli metadata |
| Provider type (MOCK, OPENAI, REPLICATE vb.) | ✅ Güvenli metadata |
| Model name (dall-e-3, stable-diffusion-xl vb.) | ✅ Güvenli metadata |
| Generation timestamp | ✅ Güvenli metadata |
| Source plan/scene reference | ✅ Lineage bilgisi |
| Güvenli error code + message | ✅ Normalized hata bilgisi |

### Yasaklananlar

| Alan | Durum |
|------|-------|
| API key / secret / token | ❌ Kesinlikle gösterilmez |
| Raw HTTP request body | ❌ Provider payload gösterilmez |
| Raw HTTP response body | ❌ Provider response gösterilmez |
| System prompt / orchestration prompt | ❌ Internal prompt gösterilmez |
| Raw error stack trace / dump | ❌ Provider hata detayı gösterilmez |
| Storage credentials (access key, secret key) | ❌ Storage secret gösterilmez |
| Internal request ID (provider-side) | ❌ Admin'e expose edilmez |

---

## 10. Admin Experience

### Beklenen Admin Ekran / Akış

#### 10.1 Generation Başlatma

- Admin, APPROVED VisualAssetPlan'ın detay ekranında **"Generate Images"** aksiyonunu görür.
- Yalnızca APPROVED plan için bu aksiyon aktiftir.
- Tıklandığında, planın tüm SceneVisualPlan ve ThumbnailPlan'ları için generation request oluşturulur.
- Admin, generation'ın başlatıldığını onaylayan bir bildirim görür.

#### 10.2 Generation Status Takibi

- Admin, plan detayında her sahne/thumbnail için generation status'unu görür.
- Status badge'leri: REQUESTED, GENERATING, GENERATED_PENDING_REVIEW, APPROVED, REJECTED, FAILED.
- GENERATING durumunda loading/progress göstergesi.
- FAILED durumunda güvenli hata mesajı.

#### 10.3 Generated Image Preview

- Admin, `GENERATED_PENDING_REVIEW` veya `APPROVED` durumundaki görselleri preview olarak görür.
- Preview, storage'dan serve edilen gerçek image dosyasıdır.
- Metadata summary görünür: dimensions, mime type, provider type, model name.
- Raw provider payload/response **görünmez**.

#### 10.4 Review Kararı

- Admin, `GENERATED_PENDING_REVIEW` durumundaki her görsel için:
  - **Approve** butonu ile onaylayabilir.
  - **Reject** butonu ile reddedebilir — rejection reason **zorunludur**.
- APPROVED/REJECTED durumundaki görseller için bu butonlar gizlenir.
- Approve/reject kararı ve timestamp kaydedilir (audit).

#### 10.5 Failed Request

- FAILED durumundaki görseller için güvenli hata mesajı gösterilir.
- Raw error dump, stack trace veya provider error body **gösterilmez**.
- Normalized error code + user-friendly message yaklaşımı.

#### 10.6 Gösterilmeyenler

- ❌ Provider secret / API key
- ❌ Raw provider request payload
- ❌ Raw provider response payload
- ❌ Internal/system prompt
- ❌ Storage credentials
- ❌ Publish / schedule aksiyonu

---

## 11. Acceptance Criteria

| # | Kriter | Durum |
|---|--------|-------|
| AC1 | Product design doc `docs/05-engineering/54-fp007-image-generation-media-asset-pipeline-product-design.md` dosyasında mevcut | ✅ |
| AC2 | FP-007 kapsamı image generation ve media asset pipeline ile sınırlı | ✅ |
| AC3 | FP-006 APPROVED VisualAssetPlan tek geçerli kaynak olarak tanımlanmış | ✅ |
| AC4 | Human review zorunlu olarak belirtilmiş | ✅ |
| AC5 | AI output asla otomatik APPROVED olamayacağı belirtilmiş | ✅ |
| AC6 | `READY_FOR_PUBLISH` açıkça dışarıda bırakılmış | ✅ |
| AC7 | Generic status update endpoint açıkça dışarıda bırakılmış | ✅ |
| AC8 | Video/audio/publishing kapsam dışı | ✅ |
| AC9 | Secret/raw provider payload/response/internal prompt exposure yasaklanmış | ✅ |
| AC10 | MediaAsset ve GeneratedAsset kavramları governance sınırlarıyla tanımlanmış | ✅ |
| AC11 | AI Platform design handoff notu eklenmiş | ✅ |

---

## 12. Future Handoff — FP-007 Task 02: AI Platform Contract Design

Bu product design dokümanı tamamlandıktan sonra, **FP-007 Task 02** olarak AI Platform Contract Design dokümanı oluşturulacaktır.

### AI Platform Dokümanında Ele Alınacak Konular

| # | Konu | Açıklama |
|---|------|----------|
| H1 | Provider-agnostic image generation contract | Image generation gateway interface tasarımı |
| H2 | Safe request/response DTO boundaries | Provider'a gönderilen ve alınan DTO'ların güvenlik sınırları |
| H3 | Prompt/input redaction strategy | Internal prompt'ların admin-facing yüzeylerde redacted gösterimi |
| H4 | Provider metadata allowlist | Admin'e güvenle gösterilebilecek metadata alanları |
| H5 | Error normalization | Provider hatalarının normalized error code/message'a dönüştürülmesi |
| H6 | Generated asset handoff contract | Provider çıktısının GeneratedAsset/MediaAsset'e dönüşüm sözleşmesi |
| H7 | No raw provider payload/response exposure | Provider verisi hiçbir admin yüzeyinde gösterilmez |
| H8 | No secrets exposure | API key/secret hiçbir kontrata dahil edilmez |

### Sıralama

```
Task 01 — Product Design ✅ (bu doküman)
Task 02 — AI Platform Contract Design (sonraki)
Task 03 — Engineering Design & Task Breakdown
Task 04+ — Implementation tasks
```

---

## Appendix A: Feature Chain

| Feature | Durum | Açıklama |
|---------|-------|----------|
| FP-001 | CLOSED | AI Assisted Content Creation |
| FP-002 | CLOSED | Scenario Generation Workflow |
| FP-003 | CLOSED | Storyboard Workflow |
| FP-004 | CLOSED | Multi-Platform Content Packaging |
| FP-005 | CLOSED | Content Export & Production Handoff |
| FP-006 | CLOSED | Visual Asset Planning & Image Generation Handoff |
| **FP-007** | **DESIGN** | **Image Generation & Media Asset Pipeline** |

---

## Document History

| Versiyon | Tarih | Açıklama |
|----------|-------|----------|
| v1.0 | 2026-07-04 | FP-007 product design — documentation-only task |
