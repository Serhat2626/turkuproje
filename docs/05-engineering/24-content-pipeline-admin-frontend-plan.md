# 24 — FP-001 Admin Frontend Integration Plan

> **Sonraki Doküman:** —
> **Önceki Doküman:** [23 — Content Pipeline API Smoke Test](./23-content-pipeline-api-smoke-test.md)

## 1. Amaç

Bu doküman, FP-001 AI destekli içerik üretim pipeline'ının Admin Panel entegrasyon planını tanımlar.
Frontend henüz implemente edilmeyecek — bu doküman **planlama referansıdır**.

Kapsam:

- Knowledge Item yönetimi (CRUD + status geçişleri)
- Content Project yönetimi (oluşturma + AI üretimi + review)
- AI draft görüntüleme ve düzenleme
- Human review workflow
- Ready-for-publish workflow

## 2. Admin UI Navigasyon Önerisi

```
📁 Admin Panel
├── 📋 Bilgi Bankası (Knowledge Items)
│   ├── Liste
│   └── Yeni Oluştur / Düzenle
├── 📝 İçerik Projeleri (Content Projects)
│   ├── Liste
│   └── Proje Detay (+ AI Draft + Review)
└── ✍️ Taslak İnceleme (Draft Review)
    └── Draft Detay / Düzenle
```

Sidebar menüde "İçerik Pipeline" veya "Content Pipeline" başlığı altında gruplanabilir.

---

## 3. Ekran Planı

### A) Knowledge Item Liste Ekranı

**Tablo Kolonları:**

| Kolon | Alan | Açıklama |
|-------|------|----------|
| Başlık | `title` | Konu başlığı |
| Tür | `type` | FOLK_SONG, ARTIST, REGION, vb. |
| Kategori | `category` | türkü, gelenek, tarihî olay |
| Durum | `status` | Badge: DRAFT / READY / ARCHIVED |
| Dil | `language` | tr, en |
| Oluşturma | `createdAt` | Tarih |

**Filtreler:**

- Status: DRAFT / READY / ARCHIVED / Tümü
- Tür: Dropdown
- Arama: title üzerinden

**Satır Aksiyonları:**

| Aksiyon | Görünürlük | API |
|---------|-----------|-----|
| Detay | Her zaman | `GET /api/admin/knowledge-items/{id}` |
| Düzenle | DRAFT, READY | Düzenle ekranına yönlendir |
| Hazır Yap | DRAFT | `POST .../mark-ready` |
| Arşivle | DRAFT, READY | `POST .../archive` |
| Proje Oluştur | READY | Content Project oluştur ekranına yönlendir |

**Boş Durum:** "Henüz bilgi öğesi eklenmemiş. Yeni bir konu ekleyerek başlayın."

**API Çağrısı:** `GET /api/admin/knowledge-items` (opsiyonel `?status=` filtresi)

---

### B) Knowledge Item Oluştur/Düzenle Ekranı

**Form Alanları:**

| Alan | Zorunlu | Validasyon |
|------|---------|-----------|
| Başlık (`title`) | ✅ | Max 255 karakter, boş olamaz |
| Tür (`type`) | ✅ | Dropdown: FOLK_SONG, ARTIST, REGION, TRADITION, HISTORICAL_EVENT, LEGEND |
| Dil (`language`) | ✅ | Dropdown: tr, en |
| Özet (`summary`) | ❌ | Serbest metin |
| Açıklama (`description`) | ❌ | Uzun metin (textarea) |
| Kategori (`category`) | ❌ | Serbest metin veya dropdown |
| İl (`city`) | ❌ | Serbest metin |
| İlçe (`district`) | ❌ | Serbest metin |
| Konum Metni (`locationText`) | ❌ | Serbest metin |
| Notlar (`notes`) | ❌ | Uzun metin (textarea) |

**Create Flow:**

1. Form doldur → Kaydet butonu
2. `POST /api/admin/knowledge-items`
3. Başarılı → Liste'ye dön veya detay ekranına git
4. Hata → Inline validation mesajları göster

**Update Flow:**

1. `GET /api/admin/knowledge-items/{id}` ile mevcut veri yükle
2. Form düzenle → Kaydet
3. `PUT /api/admin/knowledge-items/{id}`
4. ARCHIVED ise form disabled

**Aksiyon Butonları:**

| Buton | Görünürlük | Davranış |
|-------|-----------|----------|
| Kaydet | DRAFT, READY | Form submit |
| Hazır Yap | DRAFT | Onay dialog → `POST .../mark-ready` |
| Arşivle | DRAFT, READY | Onay dialog → `POST .../archive` |

---

### C) Content Project Liste Ekranı

**Tablo Kolonları:**

| Kolon | Alan | Açıklama |
|-------|------|----------|
| Bilgi Öğesi | `knowledgeItemTitle` | Bağlı KI başlığı |
| Amaç | `contentPurpose` | Badge |
| Durum | `status` | Renkli badge |
| Dil | `language` | tr, en |
| Oluşturma | `createdAt` | Tarih |

**Status Badge Renkleri:**

| Status | Renk | İkon |
|--------|------|------|
| CREATED | Gri | ○ |
| AI_REQUESTED | Mavi | ⟳ |
| AI_PROCESSING | Mavi (animasyonlu) | ⟳ |
| DRAFT_READY | Sarı | ✎ |
| IN_REVIEW | Turuncu | 👁 |
| REVISION_REQUESTED | Mor | ↩ |
| APPROVED | Yeşil | ✓ |
| READY_FOR_PUBLISH | Koyu Yeşil | ✓✓ |
| FAILED | Kırmızı | ✕ |
| REJECTED | Koyu Kırmızı | ✕ |
| CANCELLED | Gri (çizgili) | — |

**Filtreler:**

- Status: Dropdown (tüm ContentProjectStatus değerleri)
- Knowledge Item: Dropdown/arama
- Arama: knowledgeItemTitle üzerinden

**Satır Aksiyonları:**

| Aksiyon | Görünürlük | API |
|---------|-----------|-----|
| Detay | Her zaman | Detay ekranına git |

**Boş Durum:** "Henüz içerik projesi yok. Bilgi bankasından bir konu seçerek proje oluşturun."

**API Çağrısı:** `GET /api/admin/content-projects` (opsiyonel `?status=` ve `?knowledgeItemId=`)

---

### D) Content Project Detay Ekranı

**Layout:**

```
┌────────────────────────────────────────────────────┐
│ Proje Bilgileri                     [Durum Badge]  │
│ ─────────────────────────────────────────────────── │
│ Bilgi Öğesi: Çanakkale Türküsü                     │
│ Amaç: Bilgilendirici Metin                         │
│ Hedef Kitle: Genel Kullanıcı                       │
│ Ton: Bilgilendirici                                │
│ Dil: tr                                            │
│ Format: Madde listesi                              │
│ Uzunluk: 300 kelime                                │
│ Retry: 0/3                                         │
├────────────────────────────────────────────────────┤
│ Son Hata (varsa)                                   │
│ Tip: AI_PROVIDER_TIMEOUT                           │
│ Mesaj: ...                                         │
├────────────────────────────────────────────────────┤
│ Güncel Taslak                                      │
│ ─────────────────────────────────────────────────── │
│ Başlık: ...                                        │
│ Durum: GENERATED | EDITED                          │
│ Oluşturan: AI_CONTENT_AGENT                        │
│ [Taslağı Düzenle] [Tam Metin Görüntüle]           │
├────────────────────────────────────────────────────┤
│ Aksiyonlar                                         │
│ [AI Draft Üret] [Yeniden Dene] [Review Başlat]     │
│ [Onayla] [Revizyon Talep Et] [Reddet]              │
│ [Yayına Hazır] [İptal Et]                          │
└────────────────────────────────────────────────────┘
```

**Proje Metadata Alanları:**

- `knowledgeItemTitle`, `contentPurpose`, `targetAudience`, `tone`
- `language`, `formatHint`, `lengthHint`
- `status`, `retryCount`
- `lastErrorType`, `lastErrorMessage` (sadece FAILED statüsünde gösterilir)

**API Çağrısı:** `GET /api/admin/content-projects/{id}`

**Draft Alanı:**

- Projeye bağlı son draft gösterilir
- Draft yoksa "Henüz taslak yok" mesajı
- Draft varsa: title, body (kısaltılmış), status, generatedBy

---

### E) Content Draft Detay/Düzenle Ekranı

**Form Alanları:**

| Alan | Düzenlenebilir | Koşul |
|------|---------------|-------|
| Başlık (`title`) | ✅ | Draft GENERATED veya REVISION_NEEDED ise |
| İçerik (`body`) | ✅ | Draft GENERATED veya REVISION_NEEDED ise |
| Özet (`summary`) | ✅ | Draft GENERATED veya REVISION_NEEDED ise |
| Durum (`status`) | ❌ (sadece gösterim) | — |
| Oluşturan (`generatedBy`) | ❌ (sadece gösterim) | — |

**Düzenleme Kuralları:**

- Sadece `GENERATED` ve `REVISION_NEEDED` statüsündeki draft'lar düzenlenebilir
- `EDITED`, `APPROVED`, `REJECTED` draft'lar form disabled
- Kaydet → `PUT /api/admin/content-drafts/{id}`
- Kaydetme sonrası draft otomatik `EDITED` olur

> ⚠️ **KRİTİK:** Draft düzenleme ve kaydetme hiçbir koşulda:
> - Content Project'i APPROVED yapmaz
> - Content Project'i READY_FOR_PUBLISH yapmaz
> - Draft'ı APPROVED yapmaz
>
> Sadece draft statüsünü EDITED yapar.

**API Çağrıları:**
- Yükleme: `GET /api/admin/content-drafts/{contentDraftId}`
- Kaydetme: `PUT /api/admin/content-drafts/{contentDraftId}`

---

### F) Review Aksiyon Paneli

Content Project Detay ekranında alt kısımda veya yan panelde yer alır.

**Buton Görünürlük Matrisi (ContentProjectStatus bazlı):**

| Buton | CREATED | AI_REQUESTED | AI_PROCESSING | DRAFT_READY | IN_REVIEW | REVISION_REQUESTED | APPROVED | FAILED | REJECTED | READY_FOR_PUBLISH | CANCELLED |
|-------|---------|-------------|--------------|------------|----------|-------------------|---------|--------|---------|------------------|-----------|
| AI Draft Üret | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Yeniden Dene | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| Review Başlat | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Onayla | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Revizyon Talep | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Reddet | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Yayına Hazır | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| İptal Et | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |

**Buton → API Eşleştirmesi:**

| Buton | API | Request Body |
|-------|-----|-------------|
| AI Draft Üret | `POST .../generate-ai-draft` | `{ "humanFeedbackNote": "...", "feedbackType": "..." }` (opsiyonel) |
| Yeniden Dene | `POST .../retry-ai-draft` | `{}` |
| Review Başlat | `POST .../start-review` | — |
| Onayla | `POST .../approve` | `{ "reviewNote": "..." }` (opsiyonel) |
| Revizyon Talep | `POST .../request-revision` | `{ "reviewNote": "..." }` (**zorunlu**) |
| Reddet | `POST .../reject` | `{ "reviewNote": "..." }` (**zorunlu**) |
| Yayına Hazır | `POST .../mark-ready-for-publish` | — |
| İptal Et | `POST .../cancel` | — |

**Modal/Dialog Gereksinimleri:**

| Aksiyon | Dialog Tipi | Gereksinim |
|---------|-----------|-----------|
| AI Draft Üret | Opsiyonel feedback dialog | REVISION_REQUESTED ise feedback notu sorulmalı |
| Revizyon Talep | Zorunlu not dialog | Textarea + boş bırakılamaz |
| Reddet | Zorunlu not dialog | Textarea + boş bırakılamaz |
| Onayla | Opsiyonel not dialog | Textarea opsiyonel |
| İptal Et | Onay dialog | "Bu projeyi iptal etmek istediğinize emin misiniz?" |
| Yayına Hazır | Onay dialog | "Bu projeyi yayına hazır olarak işaretlemek istediğinize emin misiniz?" |

---

## 4. API Eşleştirme Tablosu

### Knowledge Item API

| UI Aksiyon | HTTP | Endpoint | Request Body |
|-----------|------|----------|-------------|
| Liste | GET | `/api/admin/knowledge-items` | — |
| Detay | GET | `/api/admin/knowledge-items/{id}` | — |
| Oluştur | POST | `/api/admin/knowledge-items` | `{ title, type, language, summary?, description?, ... }` |
| Güncelle | PUT | `/api/admin/knowledge-items/{id}` | `{ title, type, language, summary?, description?, ... }` |
| Hazır Yap | POST | `/api/admin/knowledge-items/{id}/mark-ready` | — |
| Arşivle | POST | `/api/admin/knowledge-items/{id}/archive` | — |

### Content Project API

| UI Aksiyon | HTTP | Endpoint | Request Body |
|-----------|------|----------|-------------|
| Liste | GET | `/api/admin/content-projects` | — |
| Detay | GET | `/api/admin/content-projects/{id}` | — |
| Oluştur | POST | `/api/admin/content-projects` | `{ knowledgeItemId, contentPurpose?, targetAudience?, tone?, language, formatHint?, lengthHint? }` |
| AI Draft Üret | POST | `.../generate-ai-draft` | `{ humanFeedbackNote?, feedbackType? }` |
| Yeniden Dene | POST | `.../retry-ai-draft` | `{}` |
| Review Başlat | POST | `.../start-review` | — |
| Onayla | POST | `.../approve` | `{ reviewNote? }` |
| Revizyon Talep | POST | `.../request-revision` | `{ reviewNote }` (**zorunlu**) |
| Reddet | POST | `.../reject` | `{ reviewNote }` (**zorunlu**) |
| Yayına Hazır | POST | `.../mark-ready-for-publish` | — |
| İptal Et | POST | `.../cancel` | — |

### Content Draft API

| UI Aksiyon | HTTP | Endpoint | Request Body |
|-----------|------|----------|-------------|
| Detay | GET | `/api/admin/content-drafts/{contentDraftId}` | — |
| Güncelle | PUT | `/api/admin/content-drafts/{contentDraftId}` | `{ title?, body?, summary? }` |

---

## 5. Status-Driven UI Kuralları

### KnowledgeItemStatus

| Status | Düzenlenebilir | Aksiyonlar |
|--------|---------------|-----------|
| `DRAFT` | ✅ | Kaydet, Hazır Yap, Arşivle |
| `READY` | ✅ | Kaydet, Arşivle, Proje Oluştur |
| `ARCHIVED` | ❌ (form disabled) | Yok |

### ContentProjectStatus

| Status | Açıklama | Kullanılabilir Aksiyonlar |
|--------|----------|-------------------------|
| `CREATED` | Yeni proje | AI Draft Üret, İptal |
| `AI_REQUESTED` | AI çağrısı yapıldı | İptal |
| `AI_PROCESSING` | AI üretiyor | İptal |
| `DRAFT_READY` | Draft hazır | Review Başlat, İptal |
| `IN_REVIEW` | Review'da | Onayla, Revizyon Talep, Reddet, İptal |
| `REVISION_REQUESTED` | Revizyon gerekli | AI Draft Üret (yeni feedback ile), İptal |
| `APPROVED` | Onaylandı | Yayına Hazır |
| `READY_FOR_PUBLISH` | Yayına hazır | — (terminal) |
| `FAILED` | AI başarısız | Yeniden Dene |
| `REJECTED` | Reddedildi | — (terminal) |
| `CANCELLED` | İptal edildi | — (terminal) |

### ContentDraftStatus

| Status | Düzenlenebilir | Açıklama |
|--------|---------------|----------|
| `GENERATED` | ✅ | AI tarafından üretildi |
| `EDITED` | ❌ | İnsan düzenledi (tekrar düzenlenemez) |
| `REVISION_NEEDED` | ✅ | Revizyon gerekiyor |
| `APPROVED` | ❌ | Onaylandı |
| `REJECTED` | ❌ | Reddedildi |

---

## 6. Human Review Güvenlik Kuralları

Frontend implementasyonunda **mutlaka** uyulması gereken kurallar:

### Zorunlu Kurallar

| # | Kural | Detay |
|---|-------|-------|
| HR-1 | AI üretim sonucu sadece draft olarak gösterilmelidir | UI'da "onaylandı" veya "yayına hazır" gibi yanıltıcı etiket kullanılmamalı |
| HR-2 | AI draft doğrudan yayınlanamaz | Publish butonu AI üretimi sonrası görünmemeli |
| HR-3 | Admin açıkça review başlatmalıdır | Review otomatik başlamamalı |
| HR-4 | Admin açıkça onaylamalıdır | Approve butonu sadece IN_REVIEW'da |
| HR-5 | Admin açıkça yayına hazır işaretlemeli | mark-ready-for-publish sadece APPROVED sonrası |
| HR-6 | Draft düzenleme onay/yayın yapmaz | Draft edit sonrası project status değişmemeli |
| HR-7 | UI lokal status geçişi yapmamalı | Backend response'undaki status kullanılmalı |
| HR-8 | UI backend status'a güvenmeli | Her aksiyon sonrası response'tan gelen status'u kullanmalı |
| HR-9 | Generic status düzenleme UI'da olmamalı | Dropdown ile status seçtirme yok |

### UI Anti-Pattern'ler (YAPILMAMALI)

```
❌ Status dropdown → "APPROVED" seçip kaydet
❌ Draft düzenle → "Kaydet ve Onayla" butonu
❌ AI Draft Üret → Response'u beklemeden lokal "DRAFT_READY" göster
❌ Tek butonla CREATED → READY_FOR_PUBLISH atlama
❌ PATCH /api/admin/content-projects/{id} { "status": "APPROVED" }
```

---

## 7. Hata Yönetimi Planı

### Hata Tipleri ve UI Davranışı

| Hata Tipi | HTTP Status | UI Davranışı |
|-----------|-----------|-------------|
| Validation Error | 400 | Form alanlarında inline hata mesajları |
| Invalid State Transition | 400/422 | Toast/alert: "Bu işlem şu anda yapılamaz" + mevcut status göster |
| Not Found | 404 | "Kayıt bulunamadı" sayfası veya listeye yönlendir |
| AI Generation Failure | 200 (success: false) | Hata detayını göster + retry butonu (retryable ise) |
| Retry Limit Exceeded | 400/422 | "Maksimum deneme sayısına ulaşıldı" mesajı + retry butonu disabled |
| Non-Retryable AI Failure | 400/422 | "Bu hata tipi yeniden denenemez" + retry butonu disabled |
| Server Error | 500 | Genel hata mesajı + tekrar dene önerisi |

### AI Generation Sonucu Gösterimi

```
Başarılı:
┌──────────────────────────────────────┐
│ ✅ AI draft başarıyla oluşturuldu    │
│ Başlık: Çanakkale Türküsü — ...     │
│ [Draft'ı Görüntüle]                 │
└──────────────────────────────────────┘

Başarısız (retryable):
┌──────────────────────────────────────┐
│ ⚠️ AI üretimi başarısız oldu        │
│ Hata: AI_PROVIDER_TIMEOUT           │
│ Deneme: 1/3                         │
│ [Yeniden Dene]                      │
└──────────────────────────────────────┘

Başarısız (non-retryable):
┌──────────────────────────────────────┐
│ ❌ AI üretimi başarısız oldu         │
│ Hata: AI_RESPONSE_UNSAFE_CONTENT    │
│ Bu hata tipi yeniden denenemez.     │
│ [İptal Et]                          │
└──────────────────────────────────────┘
```

---

## 8. Loading ve Optimistic Update Politikası

| Kural | Açıklama |
|-------|----------|
| Kritik aksiyonlarda optimistic update yok | Approve, reject, mark-ready-for-publish gibi aksiyonlar backend yanıtı beklenmelidir |
| Her aksiyon sonrası detay yeniden yükle | `GET .../content-projects/{id}` çağrılmalı |
| Aksiyon butonlarında loading state | Tıklandığında disabled + spinner |
| Çift tıklama engeli | Aksiyon sırasında buton disabled |
| AI generation sırasında polling | AI_REQUESTED/AI_PROCESSING sırasında periyodik detay çekme (opsiyonel — MVP'de yanıt senkron) |
| Hata sonrası butonlar tekrar aktif | Loading state kaldırılır, hata mesajı gösterilir |

---

## 9. Frontend Implementasyon Task Listesi

> ⚠️ Aşağıdaki task'lar henüz implemente edilmeyecektir. Planlama referansıdır.

| Task | Açıklama | Bağımlılık |
|------|----------|-----------|
| **Task 15** — Admin Knowledge Item UI | KI liste, oluştur/düzenle, mark-ready, archive ekranları | Task 14 (bu doküman) |
| **Task 16** — Admin Content Project UI | CP liste, detay, AI generation action butonları | Task 15 |
| **Task 17** — Admin Draft Review UI | Draft detay/düzenle, review aksiyon paneli | Task 16 |
| **Task 18** — Frontend API Client & Status Components | API client modülü, status badge, action button components | Task 15-17 paralel |
| **Task 19** — Manual End-to-End QA | Frontend + backend uçtan uca manuel QA | Task 15-18 |

### Task 15 — Admin Knowledge Item UI

**Ekranlar:**
- KI Liste (filtreleme, satır aksiyonları)
- KI Oluştur/Düzenle formu
- Mark Ready aksiyon
- Archive aksiyon

### Task 16 — Admin Content Project UI

**Ekranlar:**
- CP Liste (status badge, filtreleme)
- CP Detay (metadata, hata bilgisi, retry sayacı)
- CP Oluştur (KI seçimi + purpose/audience/tone/language)
- AI Draft Üret butonu + sonuç gösterimi
- Retry butonu + limit kontrolü

### Task 17 — Admin Draft Review UI

**Ekranlar:**
- Draft detay/düzenle formu
- Review aksiyon paneli (approve, revision, reject)
- Mark Ready For Publish butonu
- Cancel butonu

### Task 18 — Frontend API Client & Status Components

**Bileşenler:**
- API client (fetch wrapper, error handling)
- Status badge component (ContentProjectStatus renk/ikon)
- Action button component (visibility + loading + disabled logic)
- Error toast/alert component
- Confirmation dialog component

### Task 19 — Manual End-to-End QA

**Kapsam:**
- Happy path: KI → CP → AI Draft → Edit → Review → Approve → Publish
- Negative paths: tüm NP-1..NP-9
- Hata senaryoları: timeout, non-retryable, max retry
- Çapraz kontrol: frontend status backend status ile uyumlu mu

---

## 10. Kapsam Dışı

Aşağıdakiler bu plan ve ilk frontend implementasyonunun kapsamında **değildir:**

| Konu | Neden |
|------|-------|
| Public content publishing / rendering | Ayrı feature |
| SEO rendering | Ayrı feature |
| Real AI provider (OpenAI, Gemini, Anthropic) | Henüz entegre değil |
| Media generation (görsel, video, ses) | Sonraki faz |
| Automated approval | Human review zorunlu |
| Scheduled publishing | Ayrı feature |
| Multi-language admin UI | Mevcut UI dili tek |
| Analytics dashboard | Ayrı feature |
| Advanced CMS features | Ayrı feature |
| Generic status update endpoint | Mimari kural: yasak |
| Raw AI provider payload gösterimi | Internal contract |
| Storyboard, thumbnail, shorts | Sonraki faz |
| Mobile API / mobile app | Ayrı feature |
