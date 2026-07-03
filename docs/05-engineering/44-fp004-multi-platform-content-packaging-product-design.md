# 44 — FP-004 Multi-Platform Content Packaging — Product Design

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-004 — Multi-Platform Content Packaging
>
> **Prerequisites**: FP-001 — AI Assisted Content Creation (CLOSED), FP-002 — Scenario Generation Workflow (CLOSED), FP-003 — Storyboard Workflow (CLOSED)

---

## 1. Status

**Design / Planning**

Bu doküman FP-004'ün ürün ve iş akışı tasarımıdır.
İmplementasyon henüz başlamadı. Backend, frontend, schema, API ve provider prompt
oluşturulmamıştır.

---

## 2. Feature Objective

FP-004, onaylanmış (APPROVED) storyboard taslaklarından platform-spesifik içerik paketleri üretmeyi sağlar.

**Amaçlar:**

- Onaylanmış storyboard'u farklı platformlara uygun içerik paketlerine dönüştürme
- Her platform için optimize edilmiş başlık, açıklama, hashtag, CTA ve yapısal notlar üretme
- Storyboard'daki sahne bilgilerini platform formatına uygun şekilde yeniden yapılandırma
- İnsan review sürecinin korunması (FP-001 / FP-002 / FP-003 governance devam)
- İlerideki yayın (publishing) workflow'una temiz handoff hazırlığı
- FP-001 / FP-002 / FP-003'te inşa edilen AI provider altyapısının yeniden kullanımı

**Ürün Değeri:**

Kullanıcı bir konu girer → AI draft oluşturulur (FP-001) → draft onaylanır →
senaryo üretilir (FP-002) → senaryo onaylanır → storyboard üretilir (FP-003) →
storyboard onaylanır → **FP-004 ile bu storyboard'dan platform-spesifik içerik
paketleri üretilir**: YouTube Long-form video paketi, YouTube Shorts paketi,
Instagram Reels paketi ve Facebook Reels paketi. Her paket kendi platformunun
format, süre ve stil gereksinimlerine uygun olarak yapılandırılır.

**Kritik Kısıtlamalar:**

| # | Kısıtlama |
|---|-----------|
| 1 | Kaynak **yalnızca APPROVED** storyboard olabilir |
| 2 | AI_GENERATED, NEEDS_REVIEW, REJECTED, FAILED storyboard'lar kaynak olarak kullanılamaz |
| 3 | Paketleme, storyboard onayından sonraki ayrı bir adımdır |
| 4 | Paketleme platformlara yayın yapmaz |
| 5 | Paketleme video/audio/image dosyası üretmez |
| 6 | Paketleme insan onayını atlayamaz |

---

## 3. Terminology

| Terim | Tanım |
|-------|-------|
| Content Draft | FP-001'de AI tarafından üretilen ve admin tarafından onaylanan metin taslağı |
| Scenario / ScenarioDraft | FP-002'de Content Draft'tan üretilen yapılandırılmış video senaryosu |
| Storyboard / StoryboardDraft | FP-003'te senaryodan üretilen sahne bazlı görsel planlama |
| Scene / StoryboardScene | Storyboard içindeki tekil sahne birimi |
| **Platform Package** | Storyboard'dan üretilen platform-spesifik içerik paketi — **FP-004 ana ürün kavramı** |
| **Package Type** | Hedef platform formatı (YOUTUBE_LONG_FORM, YOUTUBE_SHORTS, vb.) |

### Entity İsimlendirme — Engineering Design'a Ertelenmiştir

Bu doküman ürün tasarımıdır. Java entity/class adı burada sabitlenmez.
Nihai isimlendirme FP-004 engineering design task'ında belirlenecektir.

**Aday isimler** (engineering design'da değerlendirilecek):

| Aday | Not |
|------|-----|
| `PlatformPackage` | Ürün diliyle tam uyumlu |
| `PlatformPackageDraft` | FP-002/FP-003 pattern'ıyla tutarlı (draft semantiği) |
| `ContentPackage` | Daha genel, platform-agnostic |

---

## 4. In Scope

| # | Kapsam |
|---|--------|
| 1 | Approved storyboard'dan platform-spesifik paket üretimi |
| 2 | 4 platform tipi: YouTube Long-form, YouTube Shorts, Instagram Reels, Facebook Reels |
| 3 | Her platform için optimize edilmiş metin çıktıları (başlık, açıklama, hashtag, CTA) |
| 4 | Platform-spesifik yapısal notlar (süre, aspect ratio, sahne kullanımı) |
| 5 | Admin tarafından paket review/edit |
| 6 | Admin tarafından explicit paket approve |
| 7 | Admin tarafından paket reject |
| 8 | Admin UI'da paket list/detail görünürlüğü |
| 9 | Product-level paket lifecycle |
| 10 | Product-level validation kuralları |
| 11 | ContentProject → ContentDraft → ScenarioDraft → StoryboardDraft → PlatformPackage izlenebilirliği |

---

## 5. Out of Scope

| # | Kapsam Dışı | Neden |
|---|-------------|-------|
| 1 | YouTube/Instagram/Facebook'a gerçek yayın | Sonraki feature (publishing) |
| 2 | OAuth / sosyal hesap entegrasyonu | Sonraki feature (publishing) |
| 3 | Video rendering | Media generation scope |
| 4 | Audio / TTS generation | Media generation scope |
| 5 | Image generation | Media generation scope |
| 6 | Thumbnail dosyası üretimi | Media generation scope |
| 7 | Media asset storage | Media generation scope |
| 8 | Post zamanlama (scheduling) | Publishing scope |
| 9 | Analytics ingestion | Analytics scope |
| 10 | Automated approval | Governance ihlali — asla yapılmayacak |
| 11 | Automated publish readiness | Governance ihlali — asla yapılmayacak |
| 12 | Generic status update endpoint | Governance ihlali |
| 13 | Prompt yönetim UI | Ayrı OPS/admin feature |
| 14 | Cost dashboard | Ayrı OPS/analytics feature |
| 15 | Raw prompt / provider payload / API key / secret saklanması | Güvenlik ihlali |

---

## 6. Supported Package Types

### 6.1. YOUTUBE_LONG_FORM

YouTube ana video formatı. Storyboard'daki tüm sahne yapısını korur.

| Alan | Açıklama | Zorunlu |
|------|----------|---------|
| `videoTitle` | SEO-optimize edilmiş video başlığı (≤100 karakter) | ✅ |
| `longDescription` | Detaylı video açıklaması (linkler, kaynak referansları, anahtar kelimeler) | ✅ |
| `chapterOutline` | YouTube chapters formatında zaman damgalı bölüm listesi | Opsiyonel |
| `pinnedCommentSuggestion` | İlk sabitlenecek yorum önerisi (etkileşim artırma) | Opsiyonel |
| `thumbnailTextSuggestion` | Thumbnail üzerinde kullanılacak metin önerisi (≤6 kelime) | Opsiyonel |
| `tags` | Video etiketleri / anahtar kelimeler | ✅ |
| `hashtags` | Video hashtag'leri | ✅ |
| `cta` | Call-to-action metni | ✅ |
| `targetDurationGuidance` | Hedef video süresi notları | ✅ |
| `aspectRatioGuidance` | Aspect ratio rehberi (16:9) | ✅ |
| `contentStructureNotes` | İçerik yapısı notları (intro, main, outro akışı) | Opsiyonel |
| `sceneUsageNotes` | Storyboard sahnelerinin kullanım rehberi | Opsiyonel |

### 6.2. YOUTUBE_SHORTS

YouTube kısa form dikey video formatı. Storyboard'dan seçilmiş en etkili sahneler kullanılır.

| Alan | Açıklama | Zorunlu |
|------|----------|---------|
| `shortTitle` | Kısa ve dikkat çekici başlık (≤50 karakter) | ✅ |
| `shortCaption` | Kısa açıklama metni | ✅ |
| `hookText` | İlk 3 saniyede gösterilecek hook metni | ✅ |
| `overlayTextSuggestions` | Ekran üzeri metin önerileri (liste) | Opsiyonel |
| `hashtags` | Hashtag seti | ✅ |
| `cta` | Call-to-action metni | ✅ |
| `targetDurationGuidance` | Hedef süre rehberi (≤60 saniye) | ✅ |
| `aspectRatioGuidance` | Aspect ratio rehberi (9:16) | ✅ |
| `sceneSelectionNotes` | Hangi storyboard sahnelerinin kullanılacağına dair rehber | ✅ |
| `contentStructureNotes` | Kısa form yapısal notlar | Opsiyonel |

### 6.3. INSTAGRAM_REELS

Instagram Reels dikey video formatı. Instagram'a özgü hashtag stratejisi ve caption stili.

| Alan | Açıklama | Zorunlu |
|------|----------|---------|
| `reelTitle` | Reel başlığı | ✅ |
| `caption` | Instagram caption (emoji, satır boşlukları, hashtag'ler dahil) | ✅ |
| `hookText` | İlk 3 saniyede gösterilecek hook metni | ✅ |
| `overlayTextSuggestions` | Ekran üzeri metin önerileri | Opsiyonel |
| `hashtags` | Instagram hashtag seti (platform-spesifik, ≤30 hashtag) | ✅ |
| `cta` | Call-to-action metni | ✅ |
| `targetDurationGuidance` | Hedef süre rehberi (≤90 saniye) | ✅ |
| `aspectRatioGuidance` | Aspect ratio rehberi (9:16) | ✅ |
| `sceneSelectionNotes` | Sahne seçim rehberi | ✅ |
| `contentStructureNotes` | Instagram'a özgü içerik yapısı notları | Opsiyonel |

### 6.4. FACEBOOK_REELS

Facebook Reels dikey video formatı. Facebook'a özgü caption ve hedefleme notları.

| Alan | Açıklama | Zorunlu |
|------|----------|---------|
| `reelTitle` | Reel başlığı | ✅ |
| `caption` | Facebook caption (daha uzun açıklama formatı) | ✅ |
| `hookText` | İlk 3 saniyede gösterilecek hook metni | ✅ |
| `overlayTextSuggestions` | Ekran üzeri metin önerileri | Opsiyonel |
| `hashtags` | Facebook hashtag seti | ✅ |
| `cta` | Call-to-action metni | ✅ |
| `targetDurationGuidance` | Hedef süre rehberi (≤90 saniye) | ✅ |
| `aspectRatioGuidance` | Aspect ratio rehberi (9:16) | ✅ |
| `sceneSelectionNotes` | Sahne seçim rehberi | ✅ |
| `contentStructureNotes` | Facebook'a özgü içerik yapısı notları | Opsiyonel |

---

## 7. Package Lifecycle

### Status Flow

```
[Generate] → AI_GENERATED → [Edit] → NEEDS_REVIEW → [Approve] → APPROVED
                                                    → [Reject]  → REJECTED

[Generate failure]                                  → FAILED
```

### Statuses

| Status | Açıklama | Editable | Transition |
|--------|----------|----------|------------|
| `AI_GENERATED` | AI tarafından üretildi, insan onayı bekliyor | ✅ | → NEEDS_REVIEW (edit) / APPROVED (approve) / REJECTED (reject) |
| `NEEDS_REVIEW` | Admin tarafından düzenlendi, tekrar review gerekiyor | ✅ | → APPROVED (approve) / REJECTED (reject) |
| `APPROVED` | İnsan editör tarafından onaylandı | ❌ Read-only | Final |
| `REJECTED` | İnsan editör tarafından reddedildi | ❌ Read-only | Final |
| `FAILED` | AI üretimi başarısız oldu | ❌ Read-only | Admin regeneration tetikleyebilir |

> **Not:** FP-002/FP-003 ile tutarlı status modeli kullanılır.
> `READY_FOR_PUBLISH` statüsü eklenmez — onaylanan paketler downstream
> workflow'lar (publishing feature) tarafından kullanılacaktır.

### Lifecycle Rules

| # | Kural |
|---|-------|
| 1 | AI tarafından üretilen paket `AI_GENERATED` statüsünde oluşturulur |
| 2 | AI çıktısı asla otomatik olarak `APPROVED` yapılamaz |
| 3 | AI çıktısı asla otomatik olarak `READY_FOR_PUBLISH` yapılamaz |
| 4 | Edit/save `AI_GENERATED` veya `NEEDS_REVIEW` statüsünden `NEEDS_REVIEW`'a taşır |
| 5 | Edit/save asla `APPROVED` yapamaz |
| 6 | Approve yalnızca explicit admin action ile yapılabilir |
| 7 | Reject yalnızca explicit admin action ile yapılabilir — reject reason zorunlu |
| 8 | Regeneration mevcut `APPROVED` paketi overwrite edemez — yeni versiyon oluşturur |
| 9 | `APPROVED` / `REJECTED` / `FAILED` paketler read-only'dir |
| 10 | Paket üretimi platformlara yayın yapmaz |
| 11 | Paket onayı otomatik yayın tetiklemez |

---

## 8. High-Level Workflow

### 8.1. Üretim Akışı

```
Admin → Content Project seçer
     → Approved StoryboardDraft belirlenir
     → Admin paket üretimi tetikler (hedef platformlar seçilir veya tümü varsayılan)
     → Sistem her hedef platform için bir paket adayı üretir
     → Her paket AI_GENERATED statüsünde oluşturulur
     → Admin her paketi bağımsız olarak review/edit eder
     → Admin explicit approve veya reject yapar
     → Onaylanan paketler downstream kullanıma hazır
```

### 8.2. Detaylı Adımlar

| Adım | Aktör | Açıklama |
|------|-------|----------|
| 1 | Admin | Content Project seçer |
| 2 | Sistem | Projede APPROVED StoryboardDraft olup olmadığını kontrol eder |
| 3 | Admin | Hedef platformları seçer (veya varsayılan olarak tümü) |
| 4 | Admin | Paket üretimini tetikler |
| 5 | Sistem | AI provider'a istek gönderir (her platform için ayrı veya toplu) |
| 6 | Sistem | Her platform için bir PlatformPackage kaydı oluşturur (AI_GENERATED) |
| 7 | Admin | Her paketi bağımsız olarak inceler |
| 8 | Admin | Gerekirse düzenler (status → NEEDS_REVIEW) |
| 9 | Admin | Her paketi bağımsız olarak approve veya reject eder |
| 10 | Sistem | APPROVED paketler downstream workflow'lar için hazır |

### 8.3. Validation Kuralları

| # | Kural | Hata Durumu |
|---|-------|-------------|
| 1 | Content Project mevcut olmalı | 404 Not Found |
| 2 | StoryboardDraft mevcut olmalı | 404 Not Found |
| 3 | StoryboardDraft status = APPROVED olmalı | 422 Unprocessable Entity |
| 4 | En az bir hedef platform seçilmiş olmalı | 400 Bad Request |
| 5 | Aynı storyboard version + platform için duplicate üretim engellenir | 409 Conflict (veya yeni versiyon) |

---

## 9. Human Review Rules

| # | Kural | Gerekçe |
|---|-------|---------|
| 1 | AI ürettiği tüm paketler insan review'una tabi olmalıdır | İçerik kalitesi ve marka bütünlüğü |
| 2 | Approve yalnızca explicit admin action ile yapılabilir | Governance tutarlılığı |
| 3 | Reject yalnızca explicit admin action ile yapılabilir | Governance tutarlılığı |
| 4 | Reject işlemi reason (gerekçe) zorunlu kılar | Audit trail |
| 5 | Generic status update endpoint oluşturulamaz | Governance bypass önlemi |
| 6 | Otomatik publishing tetiklenemez | İnsan kontrolü zorunlu |
| 7 | Her platform paketi bağımsız olarak review edilir | Platform-spesifik kalite kontrolü |

---

## 10. Governance Rules

| # | Kural |
|---|-------|
| 1 | Kaynak StoryboardDraft **APPROVED** statüsünde olmalıdır |
| 2 | AI ürettiği paketler otomatik onaylanamaz |
| 3 | İnsan review zorunludur |
| 4 | Approve explicit admin action'dır |
| 5 | Reject explicit admin action'dır |
| 6 | Generic status update endpoint oluşturulmaz |
| 7 | Otomatik publishing yapılmaz |
| 8 | Platform API entegrasyonu FP-004 kapsamında değildir |
| 9 | Media generation FP-004 kapsamında değildir |
| 10 | Raw prompt / provider payload / API key / secret expose edilmez |
| 11 | FP-001 / FP-002 / FP-003 governance kuralları korunur ve değiştirilmez |

---

## 11. Dependencies

### Direct Dependencies

| Feature | Entity | Gerekli Status |
|---------|--------|----------------|
| FP-001 | ContentDraft | APPROVED |
| FP-002 | ScenarioDraft | APPROVED |
| FP-003 | StoryboardDraft | **APPROVED** (birincil kaynak) |

### Dependency Chain

```
ContentProject
  └─ ContentDraft (FP-001, APPROVED)
       └─ ScenarioDraft (FP-002, APPROVED)
            └─ StoryboardDraft (FP-003, APPROVED)
                 └─ PlatformPackage (FP-004) ← yeni
```

**Birincil doğrudan kaynak**: APPROVED StoryboardDraft

FP-004, paket üretimi için doğrudan StoryboardDraft'ı kullanır.
StoryboardDraft zaten ScenarioDraft → ContentDraft → ContentProject zincirini içerir.

---

## 12. Platform-Specific Content Guidelines

### 12.1. YouTube Long-Form (16:9)

| Özellik | Değer |
|---------|-------|
| Süre | 5-15 dakika (storyboard toplam süresi) |
| Aspect Ratio | 16:9 |
| Başlık Limiti | ≤100 karakter |
| Açıklama Limiti | ≤5000 karakter |
| Tag Limiti | ≤500 karakter toplam |
| Özel Özellikler | Chapters, pinned comment, end screen CTA |

### 12.2. YouTube Shorts (9:16)

| Özellik | Değer |
|---------|-------|
| Süre | ≤60 saniye |
| Aspect Ratio | 9:16 |
| Başlık Limiti | ≤100 karakter |
| Açıklama Limiti | ≤100 karakter (kısa) |
| Özel Özellikler | Hook ilk 3 saniye, overlay text, tek CTA |

### 12.3. Instagram Reels (9:16)

| Özellik | Değer |
|---------|-------|
| Süre | ≤90 saniye |
| Aspect Ratio | 9:16 |
| Caption Limiti | ≤2200 karakter |
| Hashtag Limiti | ≤30 hashtag |
| Özel Özellikler | Emoji kullanımı, satır boşlukları, trend hashtag'ler |

### 12.4. Facebook Reels (9:16)

| Özellik | Değer |
|---------|-------|
| Süre | ≤90 saniye |
| Aspect Ratio | 9:16 |
| Caption Limiti | ≤2200 karakter |
| Özel Özellikler | Daha uzun açıklama formatı, cross-posting notları |

---

## 13. AI Generation Context

AI provider'a gönderilecek paket üretimi istek yapısı aşağıdaki verileri içerir:

| Kaynak | Veri |
|--------|------|
| ContentProject | Konu, niş, dil, hedef platformlar |
| ContentDraft | Araştırma özeti, ana içerik |
| ScenarioDraft | Senaryo yapısı, beat'ler, karakter bilgisi |
| StoryboardDraft | Sahne listesi, görsel açıklamalar, narration metni, mood |
| Hedef Platform | Platform tipi, format kısıtlamaları |

> **Not:** AI istek/yanıt yapısı FP-004 engineering design'da tanımlanacaktır.
> Bu doküman raw prompt veya provider payload içermez.

---

## 14. Future Handoff

FP-004 onaylanan paketler aşağıdaki gelecek feature'lar için hazır olacaktır:

| Gelecek Feature | Açıklama |
|-----------------|----------|
| Media Generation | Paket bilgilerinden video/audio/image üretimi |
| Publishing Workflow | Platform API'leri ile otomatik yayın |
| Analytics | Yayınlanan içeriklerin performans takibi |
| A/B Testing | Farklı başlık/açıklama varyasyonları |

---

## 15. Open Questions

Aşağıdaki sorular ürün seviyesinde açık olup engineering design veya admin review
sırasında karar verilecektir:

| # | Soru | Etki |
|---|------|------|
| 1 | Dört platform paketi varsayılan olarak hepsi birden mi üretilmeli, yoksa admin platform seçimi yapmalı mı? | UX, maliyet |
| 2 | Platform paketi onayı her platform için ayrı ayrı mı (per-platform), yoksa toplu (batch) mı yapılmalı? | Workflow karmaşıklığı |
| 3 | YouTube Long-form paketi varsayılan olarak chapters içermeli mi? | İçerik zenginliği |
| 4 | Shorts/Reels paketleri storyboard'daki tüm sahnelerden mi yoksa seçilmiş sahnelerden mi derive edilmeli? | AI prompt tasarımı |
| 5 | Dil ve ton seçenekleri platform bazında farklı mı olmalı? | Lokalizasyon, AI prompt |
| 6 | Aynı storyboard'dan birden fazla paket versiyonu üretilebilmeli mi (A/B varyasyon)? | Gelecek esneklik |
| 7 | Paket üretimi tek bir AI çağrısında mı (tüm platformlar birden) yoksa platform başına ayrı çağrı mı yapılmalı? | Maliyet, latency, prompt boyutu |
| 8 | Regeneration akışında sadece tek bir platform mi yoksa tüm paketler mi yeniden üretilmeli? | UX, maliyet |

---

## 16. Document History

| Tarih | Değişiklik |
|-------|-----------|
| 2026-07-03 | İlk oluşturma — Product Design |
