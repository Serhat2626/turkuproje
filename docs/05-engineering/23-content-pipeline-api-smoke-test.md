# 23 — FP-001 API Smoke Test & Manuel QA Kılavuzu

> **Sonraki Doküman:** —
> **Önceki Doküman:** [22 — Content Pipeline Review Checklist](./22-content-pipeline-review-checklist.md)

## Amaç

FP-001 AI destekli içerik üretim pipeline'ının uçtan uca çalıştığını doğrulamak.
Bu doküman hem **manuel QA** (curl/Postman/httpie) hem de **otomatik smoke test** için referanstır.

## Ön Koşullar

- Backend ayağa kalkmış: `mvn spring-boot:run` veya Docker
- PostgreSQL bağlantısı aktif
- Mock AI gateway kullanılıyor (gerçek provider yok)
- Admin API güvenlik filtresi devre dışı veya test token mevcut
- Base URL: `http://localhost:8080`

## Mock AI Gateway Varsayımları

| Durum | Davranış |
|-------|----------|
| Varsayılan senaryo | `SUCCESS` — deterministic başlık + 100+ karakter body |
| Timeout senaryosu | `TIMEOUT` — retryable hata |
| Unsafe content | `UNSAFE_CONTENT` — non-retryable hata |

Mock gateway senaryo kontrolü şu an REST API'den yapılamaz (scope dışı).
Test sırasında gateway her zaman varsayılan SUCCESS senaryosunu döndürür.

---

## Happy Path — Tam Workflow

### Adım 1: Knowledge Item Oluştur

```http
POST /api/admin/knowledge-items
Content-Type: application/json

{
  "title": "Çanakkale Türküsü",
  "type": "FOLK_SONG",
  "language": "tr",
  "summary": "Çanakkale Savaşı'nı anlatan ünlü türkü",
  "description": "Çanakkale Türküsü, I. Dünya Savaşı sırasında Çanakkale Savaşı'nda yaşanan acıları ve kahramanlıkları anlatan halk türküsüdür.",
  "category": "türkü",
  "city": "Çanakkale"
}
```

**Beklenen:**
- Status: `201 Created`
- Response'ta `status: "DRAFT"`, `id` mevcut

```
✅ Knowledge Item oluşturuldu → status: DRAFT
```

---

### Adım 2: Knowledge Item'ı READY Yap

```http
POST /api/admin/knowledge-items/{knowledgeItemId}/mark-ready
```

**Beklenen:**
- Status: `200 OK`
- Response'ta `status: "READY"`

```
✅ Knowledge Item → READY
```

---

### Adım 3: Content Project Oluştur

```http
POST /api/admin/content-projects
Content-Type: application/json

{
  "knowledgeItemId": "{knowledgeItemId}",
  "contentPurpose": "INFORMATIONAL_TEXT",
  "targetAudience": "GENERAL_USER",
  "tone": "INFORMATIVE",
  "language": "tr",
  "formatHint": "madde listesi",
  "lengthHint": "300 kelime"
}
```

**Beklenen:**
- Status: `201 Created`
- Response'ta `status: "CREATED"`, `id` mevcut

```
✅ Content Project oluşturuldu → status: CREATED
```

---

### Adım 4: AI Draft Üret (Mock Gateway)

```http
POST /api/admin/content-projects/{contentProjectId}/generate-ai-draft
Content-Type: application/json

{}
```

**Beklenen:**
- Status: `200 OK`
- `success: true`
- `projectStatus: "DRAFT_READY"`
- `contentDraftId` mevcut
- `draftTitle` mevcut

```
✅ AI draft üretildi → projectStatus: DRAFT_READY
⚠️ KRİTİK KONTROL: projectStatus APPROVED değil
⚠️ KRİTİK KONTROL: projectStatus READY_FOR_PUBLISH değil
```

---

### Adım 5: Content Draft Detayını Doğrula

```http
GET /api/admin/content-drafts/{contentDraftId}
```

**Beklenen:**
- `status: "GENERATED"`
- `generatedBy: "AI_CONTENT_AGENT"`
- `title` ve `body` dolu

```
✅ Draft status: GENERATED, generatedBy: AI_CONTENT_AGENT
```

---

### Adım 6: Content Project Detayını Doğrula

```http
GET /api/admin/content-projects/{contentProjectId}
```

**Beklenen:**
- `status: "DRAFT_READY"`
- `retryCount: 0` veya `1` (ilk denemede artmamış olmalı)

```
✅ Project status: DRAFT_READY
⚠️ KRİTİK KONTROL: status APPROVED değil
⚠️ KRİTİK KONTROL: status READY_FOR_PUBLISH değil
```

---

### Adım 7: Draft'ı Manuel Düzenle

```http
PUT /api/admin/content-drafts/{contentDraftId}
Content-Type: application/json

{
  "title": "Çanakkale Türküsü — Editör Düzenlemesi",
  "body": "Bu metin editör tarafından düzenlenmiştir. Çanakkale Savaşı hakkında detaylı bilgi içerir."
}
```

**Beklenen:**
- Status: `200 OK`
- `status: "EDITED"`
- `title` güncellenmiş

```
✅ Draft → EDITED
```

---

### Adım 8: Draft Düzenleme Sonrası Project Kontrolü

```http
GET /api/admin/content-projects/{contentProjectId}
```

**Beklenen:**
- `status: "DRAFT_READY"` (değişmemiş)

```
✅ Project hâlâ DRAFT_READY — draft update project statüsünü değiştirmedi
⚠️ KRİTİK KONTROL: status APPROVED değil
⚠️ KRİTİK KONTROL: status READY_FOR_PUBLISH değil
```

---

### Adım 9: Review Başlat

```http
POST /api/admin/content-projects/{contentProjectId}/start-review
```

**Beklenen:**
- `status: "IN_REVIEW"`

```
✅ Project → IN_REVIEW
```

---

### Adım 10: Onayla

```http
POST /api/admin/content-projects/{contentProjectId}/approve
Content-Type: application/json

{
  "reviewNote": "İçerik uygun, yayına hazır"
}
```

**Beklenen:**
- `status: "APPROVED"`

```
✅ Project → APPROVED (insan onayı ile)
```

---

### Adım 11: Yayına Hazır İşaretle

```http
POST /api/admin/content-projects/{contentProjectId}/mark-ready-for-publish
```

**Beklenen:**
- `status: "READY_FOR_PUBLISH"`

```
✅ Project → READY_FOR_PUBLISH (sadece APPROVED sonrası ulaşılabilir)
```

---

## Negative Path Checklist

### NP-1: DRAFT KI'dan Content Project Oluşturulamaz

```http
POST /api/admin/content-projects
Content-Type: application/json

{
  "knowledgeItemId": "{draftKnowledgeItemId}",
  "language": "tr"
}
```

**Beklenen:** `400` veya `422` — BusinessException

```
✅ DRAFT KI → Content Project oluşturma engellendi
```

---

### NP-2: AI Generation Otomatik Approve Yapamaz

Adım 4'te doğrulandı:
- `projectStatus: "DRAFT_READY"` — hiçbir koşulda `APPROVED` değil

```
✅ AI output → DRAFT_READY (asla APPROVED değil)
```

---

### NP-3: AI Generation Otomatik READY_FOR_PUBLISH Yapamaz

Adım 4'te doğrulandı:
- `projectStatus: "DRAFT_READY"` — hiçbir koşulda `READY_FOR_PUBLISH` değil

```
✅ AI output → DRAFT_READY (asla READY_FOR_PUBLISH değil)
```

---

### NP-4: Draft Update Approve Yapamaz

Adım 8'de doğrulandı:
- Draft update sonrası project hâlâ `DRAFT_READY`

```
✅ Draft update → Project status değişmedi
```

---

### NP-5: DRAFT_READY'den Direkt READY_FOR_PUBLISH Yapılamaz

```http
POST /api/admin/content-projects/{contentProjectId}/mark-ready-for-publish
```

Project `DRAFT_READY` iken çağrılırsa:

**Beklenen:** `400` veya `422` — BusinessException

```
✅ DRAFT_READY → READY_FOR_PUBLISH engellendi (human review zorunlu)
```

---

### NP-6: CREATED'den Direkt Approve Yapılamaz

```http
POST /api/admin/content-projects/{contentProjectId}/approve
```

Project `CREATED` iken çağrılırsa:

**Beklenen:** `400` veya `422` — BusinessException

```
✅ CREATED → APPROVED engellendi (AI draft + review zorunlu)
```

---

### NP-7: Generic Status Update Endpoint Yok

Aşağıdaki endpoint **mevcut değildir ve asla eklenmemelidir:**

```http
PUT /api/admin/content-projects/{id}/status
PATCH /api/admin/content-projects/{id}

{ "status": "APPROVED" }
```

**Beklenen:** `404 Not Found` veya `405 Method Not Allowed`

```
✅ Generic status update endpoint yok — action endpoint'ler kullanılmalı
```

---

### NP-8: Retry Max 3 Sonrası Retry Yapılamaz

`retryCount >= 3` olan FAILED projede:

```http
POST /api/admin/content-projects/{contentProjectId}/retry-ai-draft
```

**Beklenen:** `400` veya `422` — BusinessException

```
✅ Max retry aşımı engellendi
```

---

### NP-9: Non-Retryable Hata Retry Edilemez

`lastErrorType: "AI_RESPONSE_UNSAFE_CONTENT"` olan FAILED projede:

```http
POST /api/admin/content-projects/{contentProjectId}/retry-ai-draft
```

**Beklenen:** `400` veya `422` — BusinessException

```
✅ Non-retryable hata retry engellendi
```

---

## Status Geçiş Özet Tablosu

```
CREATED ─── generate-ai-draft ──→ AI_REQUESTED → AI_PROCESSING → DRAFT_READY
                                                                 ↓ (failure)
                                                                FAILED ── retry ──→ (tekrar AI_REQUESTED)

DRAFT_READY ── start-review ──→ IN_REVIEW
IN_REVIEW ──── approve ────────→ APPROVED
IN_REVIEW ──── request-revision → REVISION_REQUESTED → generate-ai-draft → ...
IN_REVIEW ──── reject ─────────→ REJECTED
APPROVED ───── mark-ready ─────→ READY_FOR_PUBLISH

Herhangi ara statü ── cancel ──→ CANCELLED
```

---

## Human Review Güvenlik Kuralları Özeti

| Kural | Durum |
|-------|-------|
| AI output → DRAFT_READY (asla APPROVED) | ✅ Zorunlu |
| AI output → DRAFT_READY (asla READY_FOR_PUBLISH) | ✅ Zorunlu |
| Draft update → project status değişmez | ✅ Zorunlu |
| READY_FOR_PUBLISH sadece APPROVED sonrası | ✅ Zorunlu |
| requestRevision ve reject zorunlu reviewNote | ✅ Zorunlu |
| Generic status update endpoint yok | ✅ Zorunlu |

---

## Kapsam Dışı

- Gerçek AI provider entegrasyonu (OpenAI, Gemini, Anthropic)
- Frontend/admin panel
- Otomatik yayınlama
- Generic status update endpoint'leri
- Storyboard, görsel/video üretimi
- Mobil API
- Public API
