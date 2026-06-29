# FP-001 — End-to-End QA Guide

> İçerik Pipeline — AI Assisted Content Creation — Final QA Dokümanı

## 1. Amaç

Bu doküman, FP-001 AI Assisted Content Creation özelliğinin mock AI gateway ile uçtan uca test edilmesi için referans QA rehberidir.

Kapsam:
- Backend API (Spring Boot)
- Admin Frontend UI (React + Vite)
- Mock AI Gateway (deterministic)
- Knowledge Item → Content Project → AI Draft → Human Review → Publish workflow

Gerçek AI provider, yayınlama, SEO, medya üretimi ve zamanlama **kapsam dışıdır**.

---

## 2. Ön Koşullar

| Gereksinim | Detay |
|-----------|-------|
| Backend çalışıyor | `mvn spring-boot:run` veya IDE |
| Frontend çalışıyor | `npm run dev` (admin-web) |
| PostgreSQL aktif | Migration'lar uygulanmış |
| Mock AI Gateway | Aktif (`@Component` bean — gerçek provider yok) |
| API Base URL | `http://localhost:8080/api/admin` |
| Admin UI URL | `http://localhost:5173` |

---

## 3. Build Doğrulama Komutları

### Backend

```bash
cd engineering/apps/backend
mvn compile -q     # Beklenen: BUILD SUCCESS
mvn test           # Beklenen: 70 test, 0 failure, 0 error
```

### Frontend

```bash
cd engineering/apps/admin-web
npx tsc -b --noEmit   # Beklenen: 0 hata
npx vite build         # Beklenen: 62 modül, build başarılı
```

---

## 4. Happy Path — Admin UI Uçtan Uca

### Adım 1 — Knowledge Item Oluştur

| Alan | Değer |
|------|-------|
| Ekran | Bilgi Öğeleri (`/knowledge`) |
| Aksiyon | "+ Yeni Bilgi Öğesi" → formu doldur → "Oluştur" |
| API | `POST /api/admin/knowledge-items` |
| Beklenen | Item oluşturulur, status = `DRAFT` |

### Adım 2 — Knowledge Item'ı READY Yap

| Alan | Değer |
|------|-------|
| Ekran | Bilgi Öğesi Detay |
| Aksiyon | "Hazıra Al" butonu |
| API | `POST /api/admin/knowledge-items/{id}/mark-ready` |
| Beklenen | Status → `READY` |
| Güvenlik | `DRAFT` → `READY` geçişi. Diğer status'lardan geçiş **engellenmeli**. |

### Adım 3 — Content Project Oluştur

| Alan | Değer |
|------|-------|
| Ekran | İçerik Projeleri (`/projects`) → "+ Yeni Proje" |
| Aksiyon | READY bilgi öğesini seç, formu doldur → "Oluştur" |
| API | `POST /api/admin/content-projects` |
| Beklenen | Proje oluşturulur, status = `CREATED` |
| Güvenlik | Sadece `READY` KI'lar dropdown'da görünmeli. |

### Adım 4 — AI Draft Üret

| Alan | Değer |
|------|-------|
| Ekran | Content Project Detay |
| Aksiyon | "🤖 AI Draft Üret" butonu |
| API | `POST /api/admin/content-projects/{id}/generate-ai-draft` |
| Beklenen | Mock AI başarılı → Proje status = `DRAFT_READY`, AI result kartı gösterilir |
| Güvenlik | ⚠️ **Proje status `APPROVED` veya `READY_FOR_PUBLISH` OLMAMALI** |

### Adım 5 — Draft'ı Aç ve Görüntüle

| Alan | Değer |
|------|-------|
| Ekran | Content Project Detay → "📄 Taslağı Aç" butonu |
| Navigasyon | `/drafts/{contentDraftId}` |
| Beklenen | Draft detay sayfası açılır, status = `GENERATED` |
| Güvenlik | Draft sayfasında approve/publish butonu **olmamalı**. |

### Adım 6 — Draft Düzenle

| Alan | Değer |
|------|-------|
| Ekran | Draft Review (`/drafts/{id}`) |
| Aksiyon | "✏️ Düzenle" → başlık/body/summary değiştir → "Kaydet" |
| API | `PUT /api/admin/content-drafts/{id}` |
| Beklenen | Draft status → `EDITED` |
| Güvenlik | ⚠️ **Proje status değişmemeli.** Draft edit proje onayı yapmaz. |

### Adım 7 — Review Başlat

| Alan | Değer |
|------|-------|
| Ekran | Content Project Detay |
| Aksiyon | "👁 Review Başlat" butonu |
| API | `POST /api/admin/content-projects/{id}/start-review` |
| Beklenen | Proje status → `IN_REVIEW` |
| Ön koşul | Proje `DRAFT_READY` durumunda olmalı |

### Adım 8 — Onayla

| Alan | Değer |
|------|-------|
| Ekran | Content Project Detay |
| Aksiyon | "✓ Onayla" butonu → ConfirmDialog |
| API | `POST /api/admin/content-projects/{id}/approve` |
| Beklenen | Proje status → `APPROVED` |
| Ön koşul | Proje `IN_REVIEW` durumunda olmalı |
| Güvenlik | ⚠️ **Yalnızca insan onayı ile geçilir.** |

### Adım 9 — Yayına Hazır İşaretle

| Alan | Değer |
|------|-------|
| Ekran | Content Project Detay |
| Aksiyon | "✓✓ Yayına Hazır" butonu → ConfirmDialog |
| API | `POST /api/admin/content-projects/{id}/mark-ready-for-publish` |
| Beklenen | Proje status → `READY_FOR_PUBLISH` |
| Ön koşul | Proje `APPROVED` durumunda olmalı |
| Güvenlik | ⚠️ **IN_REVIEW veya DRAFT_READY'den doğrudan geçiş engellenmeli.** |

---

## 5. Alternatif Akışlar

### Revizyon Talebi

| Alan | Değer |
|------|-------|
| Ön koşul | Proje `IN_REVIEW` |
| Aksiyon | "↩ Revizyon" → Not girişi (zorunlu) |
| API | `POST /api/admin/content-projects/{id}/request-revision` |
| Beklenen | Proje status → `REVISION_REQUESTED` |
| Sonraki | "🤖 AI Draft Üret" butonu tekrar görünür |

### Reddetme

| Alan | Değer |
|------|-------|
| Ön koşul | Proje `IN_REVIEW` |
| Aksiyon | "✕ Reddet" → Not girişi (zorunlu) |
| API | `POST /api/admin/content-projects/{id}/reject` |
| Beklenen | Proje status → `REJECTED` (final) |

### AI Hatası ve Retry

| Alan | Değer |
|------|-------|
| Durum | Mock AI gateway failure senaryosu |
| Beklenen | Proje status → `FAILED` |
| Aksiyon | "🔄 Yeniden Dene" butonu |
| API | `POST /api/admin/content-projects/{id}/retry-ai-draft` |
| Güvenlik | Max retry (3) aşılırsa retry engellenir |

### Proje İptali

| Alan | Değer |
|------|-------|
| İzin verilen status'lar | `CREATED`, `AI_REQUESTED`, `AI_PROCESSING`, `DRAFT_READY`, `IN_REVIEW`, `REVISION_REQUESTED` |
| Aksiyon | "— İptal" → ConfirmDialog |
| API | `POST /api/admin/content-projects/{id}/cancel` |
| Beklenen | Proje status → `CANCELLED` (final) |
| Güvenlik | `APPROVED`, `REJECTED`, `READY_FOR_PUBLISH`, `FAILED`, `CANCELLED`'dan iptal **engellenir** |

---

## 6. Negatif QA Kontrol Listesi

| # | Kontrol | Beklenen |
|---|---------|----------|
| N-01 | `DRAFT` KI ile Content Project oluşturma | ❌ Backend hata — READY olmalı |
| N-02 | `ARCHIVED` KI ile Content Project oluşturma | ❌ Backend hata — READY olmalı |
| N-03 | AI generation sonrası proje `APPROVED` olmamalı | ✅ Status = `DRAFT_READY` |
| N-04 | AI generation sonrası proje `READY_FOR_PUBLISH` olmamalı | ✅ Status = `DRAFT_READY` |
| N-05 | Draft edit sonrası proje approve olmamalı | ✅ Proje status değişmez |
| N-06 | Draft edit sonrası proje publish olmamalı | ✅ Proje status değişmez |
| N-07 | `DRAFT_READY`'den doğrudan `READY_FOR_PUBLISH` geçişi | ❌ Engellenmeli |
| N-08 | `DRAFT_READY`'den doğrudan `APPROVED` geçişi | ❌ Engellenmeli — önce `IN_REVIEW` |
| N-09 | Revizyon talebi boş not ile | ❌ Backend validation hatası |
| N-10 | Reddetme boş not ile | ❌ Backend validation hatası |
| N-11 | Max retry aşılmış projede retry | ❌ Backend engeller |
| N-12 | Non-retryable AI hatasında retry | ❌ Backend engeller |
| N-13 | UI'da generic status dropdown | ❌ Yok — hiçbir ekranda |
| N-14 | Generic status update API çağrısı | ❌ Yok — fonksiyon tanımlı değil |
| N-15 | Raw AI provider payload gösterimi | ❌ Yok — sadece `AiGenerationSummaryResponse` |
| N-16 | `EDITED` draft düzenleme | ❌ Backend engeller — sadece `GENERATED`/`REVISION_NEEDED` |
| N-17 | `APPROVED` draft düzenleme | ❌ Backend engeller |
| N-18 | `REJECTED` draft düzenleme | ❌ Backend engeller |

---

## 7. Status Matrisi

### KnowledgeItemStatus

| Status | Anlamı | Badge |
|--------|--------|-------|
| `DRAFT` | Yeni oluşturulmuş, düzenlenebilir | `badge-draft` |
| `READY` | Content Project oluşturmaya hazır | `badge-success` |
| `ARCHIVED` | Arşivlenmiş, pasif | `badge-muted` |

### ContentProjectStatus

| Status | Anlamı | Badge |
|--------|--------|-------|
| `CREATED` | Proje oluşturuldu, AI çağrısı yapılmadı | `badge-draft` |
| `AI_REQUESTED` | AI üretim talebi gönderildi | `badge-info` |
| `AI_PROCESSING` | AI üretimi devam ediyor | `badge-info` |
| `DRAFT_READY` | AI draft hazır, insan review bekliyor | `badge-draft` |
| `IN_REVIEW` | İnsan review sürecinde | `badge-info` |
| `REVISION_REQUESTED` | Revizyon talep edildi | `badge-info` |
| `APPROVED` | İnsan tarafından onaylandı | `badge-success` |
| `READY_FOR_PUBLISH` | Yayına hazır | `badge-success` |
| `FAILED` | AI üretimi başarısız | `badge-error` |
| `REJECTED` | İnsan tarafından reddedildi | `badge-error` |
| `CANCELLED` | Proje iptal edildi | `badge-muted` |

> ⚠️ `GENERATING` status'u **yoktur**. Backend enum'unda tanımlı değildir. Kullanılmamalıdır.

### ContentDraftStatus

| Status | Anlamı | Badge |
|--------|--------|-------|
| `GENERATED` | AI tarafından üretildi, düzenlenebilir | `badge-info` |
| `EDITED` | İnsan editör tarafından düzenlendi | `badge-draft` |
| `REVISION_NEEDED` | Revizyon gerekiyor, düzenlenebilir | `badge-info` |
| `APPROVED` | Onaylandı | `badge-success` |
| `REJECTED` | Reddedildi | `badge-error` |

---

## 8. Action Visibility Matrisi

### Knowledge Item Aksiyonları

| Aksiyon | Buton Görünür | API Endpoint |
|---------|:------------:|--------------|
| Hazıra Al | `DRAFT` | `POST .../mark-ready` |
| Arşivle | `DRAFT`, `READY` | `POST .../archive` |

### Content Project Aksiyonları

| Aksiyon | Buton Görünür | API Endpoint |
|---------|:------------:|--------------|
| AI Draft Üret | `CREATED`, `REVISION_REQUESTED` | `POST .../generate-ai-draft` |
| Yeniden Dene | `FAILED` | `POST .../retry-ai-draft` |
| Review Başlat | `DRAFT_READY` | `POST .../start-review` |
| Onayla | `IN_REVIEW` | `POST .../approve` |
| Revizyon Talep | `IN_REVIEW` | `POST .../request-revision` |
| Reddet | `IN_REVIEW` | `POST .../reject` |
| Yayına Hazır | `APPROVED` | `POST .../mark-ready-for-publish` |
| İptal | `CREATED`, `AI_REQUESTED`, `AI_PROCESSING`, `DRAFT_READY`, `IN_REVIEW`, `REVISION_REQUESTED` | `POST .../cancel` |

### Content Draft Aksiyonları

| Aksiyon | Buton Görünür | API Endpoint |
|---------|:------------:|--------------|
| Düzenle | `GENERATED`, `REVISION_NEEDED` | `PUT /api/admin/content-drafts/{id}` |

---

## 9. Bilinen Kısıtlamalar

### KL-001 — ContentProjectResponse latestDraftId içermiyor

**Etki**: Content Project detay sayfası yenilendikten sonra "Taslağı Aç" butonu kaybolur. Draft'a erişim sadece AI üretim sonucundaki `contentDraftId` üzerinden mümkündür.

**Geçici Çözüm**: AI üretim sonrası hemen "Taslağı Aç" butonuna tıklayın.

**Kalıcı Çözüm**: Backend DTO genişletmesi — `ContentProjectResponse`'a `latestDraftId` veya `latestDraftSummary` alanı eklenmesi. Ayrı bir backend/frontend task olarak planlanabilir.

---

## 10. Kapsam Dışı

Aşağıdaki özellikler FP-001 mock-backed sprint kapsamında **değildir**:

- ❌ Gerçek AI provider entegrasyonu (OpenAI, Gemini vb.)
- ❌ Herkese açık yayınlama (public publishing)
- ❌ SEO rendering / meta tag üretimi
- ❌ Zamanlanmış yayınlama (scheduled publishing)
- ❌ Medya üretimi (görsel, video, ses)
- ❌ Analytics dashboard
- ❌ Generic workflow/status editörü
- ❌ Otomatik onay (automatic approval)
- ❌ Otomatik yayına hazır işaretleme (automatic ready-for-publish)
- ❌ Kullanıcı yetkilendirme / rol bazlı erişim kontrolü

---

## 11. İlgili Dokümanlar

| Doküman | Dosya |
|---------|-------|
| Content Pipeline Engineering | `18-content-pipeline-engineering.md` |
| API Design | `19-content-pipeline-api-design.md` |
| Implementation Plan | `20-content-pipeline-implementation-plan.md` |
| Coding Tasks | `21-content-pipeline-coding-tasks.md` |
| Review Checklist | `22-content-pipeline-review-checklist.md` |
| API Smoke Test | `23-content-pipeline-api-smoke-test.md` |
| Admin Frontend Plan | `24-content-pipeline-admin-frontend-plan.md` |
