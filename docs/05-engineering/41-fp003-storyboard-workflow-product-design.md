# 41 — FP-003 Storyboard Workflow — Product Design

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-003 — Storyboard Workflow
>
> **Prerequisites**: FP-001 — AI Assisted Content Creation (CLOSED), FP-002 — Scenario Generation Workflow (CLOSED)

---

## 1. Status

**Design / Planning**

Bu doküman FP-003'ün ürün ve iş akışı tasarımıdır.
İmplementasyon henüz başlamadı. Backend, frontend, schema, API ve provider prompt
oluşturulmamıştır.

---

## 2. Feature Objective

FP-003, onaylanmış (APPROVED) senaryolardan yapılandırılmış storyboard üretmeyi sağlar.

**Amaçlar:**

- Onaylanmış senaryodaki beat'leri görsel/sahne bazlı storyboard scene'lere dönüştürme
- Her sahne için kamera yönü, görsel açıklama, narration, mood gibi ayrıntıları yapılandırma
- İnsan review sürecinin korunması (FP-001 / FP-002 governance devam)
- İlerideki media/image/video üretim workflow'larına temiz handoff hazırlığı
- FP-001 / FP-002'de inşa edilen AI provider altyapısının yeniden kullanımı
- Provider sınırlarının değiştirilmemesi

**Ürün Değeri:**

Kullanıcı bir konu girer → AI draft oluşturulur (FP-001) → draft onaylanır →
senaryo üretilir (FP-002) → senaryo onaylanır → **FP-003 ile bu senaryodan
yapılandırılmış storyboard üretilir**: her beat için sahne açıklaması, kamera yönü,
narration metni, görsel açıklama, mood ve geçiş notları. Bu storyboard, ileride
image/video üretim sürecinin girdisi olacaktır.

---

## 3. Terminology

| Terim | Tanım |
|-------|-------|
| Content Draft | FP-001'de AI tarafından üretilen ve admin tarafından onaylanan metin taslağı |
| Scenario / ScenarioDraft | FP-002'de Content Draft'tan üretilen yapılandırılmış video senaryosu |
| Beat / ScenarioBeat | Senaryo içindeki anlatım birimi (HOOK, INTRODUCTION, vb.) |
| **Storyboard** | Senaryodan üretilen sahne bazlı görsel planlama — **FP-003 ana ürün kavramı** |
| **Scene** | Storyboard içindeki tekil sahne birimi |

### Entity İsimlendirme — Engineering Design'a Ertelenmiştir

Bu doküman ürün tasarımıdır. Java entity/class adı burada sabitlenmez.
Nihai isimlendirme FP-003 engineering design task'ında belirlenecektir.

**Aday isimler** (engineering design'da değerlendirilecek):

| Aday | Not |
|------|-----|
| `Storyboard` | Ürün diliyle tam uyumlu |
| `StoryboardDraft` | FP-002 `ScenarioDraft` pattern'ıyla tutarlı |
| `StoryboardCandidate` | Version/candidate semantiğini vurgular |

---

## 4. In Scope

| # | Kapsam |
|---|--------|
| 1 | Approved scenario'dan storyboard generation |
| 2 | Storyboard adayı oluşturma |
| 3 | Storyboard scene yapısı tanımlama |
| 4 | Admin tarafından storyboard scene review/edit |
| 5 | Admin tarafından explicit storyboard approve |
| 6 | Admin tarafından storyboard reject veya regeneration |
| 7 | Admin UI'da storyboard list/detail görünürlüğü |
| 8 | Product-level storyboard lifecycle |
| 9 | Product-level validation kuralları |
| 10 | Content Project → ContentDraft → ScenarioDraft → Storyboard izlenebilirliği |

---

## 5. Out of Scope

| # | Kapsam Dışı | Neden |
|---|-------------|-------|
| 1 | Image generation | Sonraki feature (media generation) |
| 2 | Video generation | Sonraki feature (media generation) |
| 3 | Audio / TTS generation | Sonraki feature (media generation) |
| 4 | Real media file creation | Sonraki feature |
| 5 | Public publishing | Sonraki feature (publishing) |
| 6 | Scheduled publishing | Sonraki feature |
| 7 | Prompt management UI | Ayrı OPS/admin feature |
| 8 | Cost dashboard | Ayrı OPS/analytics feature |
| 9 | Multi-provider selection UI | Ayrı admin feature |
| 10 | Automated approval | Governance ihlali — asla yapılmayacak |
| 11 | Automated ready-for-publish | Governance ihlali — asla yapılmayacak |
| 12 | Bulk storyboard generation | Gelecek optimizasyon |
| 13 | Public-facing storyboard pages | Sonraki feature |
| 14 | Scene-level image rendering | Media generation scope |
| 15 | Storyboard-to-video pipeline | Media generation scope |

---

## 6. Storyboard Lifecycle

### Status Flow

```
[Generate] → AI_GENERATED → [Edit] → NEEDS_REVIEW → [Approve] → APPROVED
                                                    → [Reject]  → REJECTED

[Generate failure]                                   → FAILED
```

### Statuses

| Status | Açıklama | Editable | Transition |
|--------|----------|----------|------------|
| `AI_GENERATED` | AI tarafından üretildi, insan onayı bekliyor | ✅ | → NEEDS_REVIEW (edit) / APPROVED (approve) / REJECTED (reject) |
| `NEEDS_REVIEW` | Admin tarafından düzenlendi, tekrar review gerekiyor | ✅ | → APPROVED (approve) / REJECTED (reject) |
| `APPROVED` | İnsan editör tarafından onaylandı | ❌ Read-only | Final |
| `REJECTED` | İnsan editör tarafından reddedildi | ❌ Read-only | Final |
| `FAILED` | AI üretimi başarısız oldu | ❌ Read-only | Admin regeneration tetikleyebilir |

### Lifecycle Rules

| # | Kural |
|---|-------|
| 1 | AI tarafından üretilen storyboard `AI_GENERATED` statüsünde oluşturulur |
| 2 | AI çıktısı asla otomatik olarak `APPROVED` yapılamaz |
| 3 | AI çıktısı asla otomatik olarak `READY_FOR_PUBLISH` yapılamaz |
| 4 | Edit/save `AI_GENERATED` veya `NEEDS_REVIEW` statüsünden `NEEDS_REVIEW`'a taşır |
| 5 | Edit/save asla `APPROVED` yapamaz |
| 6 | Approve yalnızca explicit admin action ile yapılabilir |
| 7 | Reject yalnızca explicit admin action ile yapılabilir |
| 8 | Regeneration mevcut `APPROVED` storyboard'u overwrite edemez — yeni versiyon oluşturur |
| 9 | `APPROVED` / `REJECTED` / `FAILED` storyboard'lar read-only'dir |

> **Not:** Final status enum kararı engineering design task'ında (Task 03) onaylanacaktır.

---

## 7. Human Review Rules

| # | Kural | Gerekçe |
|---|-------|---------|
| 1 | Storyboard generation yalnızca approved scenario'dan yapılabilir | Onaysız senaryo çıktısından storyboard üretilemez |
| 2 | Storyboard generation unapproved scenario output'undan yapılamaz | FP-002 governance korunur |
| 3 | Storyboard approval yalnızca explicit admin action ile gerçekleşir | Governance: otomatik onay yasak |
| 4 | Storyboard rejection yalnızca explicit admin action ile gerçekleşir | Governance: otomatik red yasak |
| 5 | Storyboard edit approval öncesinde izinlidir | Admin scene içeriklerini düzenleyebilir |
| 6 | Save/edit asla approve anlamına gelmez | FP-002 ile tutarlı |
| 7 | Regeneration mevcut approved storyboard'u overwrite edemez | Approved versiyonlar korunur |
| 8 | Approved scenario, storyboard kaynağı olarak traceable kalmalıdır | İzlenebilirlik |
| 9 | Per-project one-approved-storyboard kuralı uygulanır | FP-002 ile tutarlı |
| 10 | Storyboard detail'de source scenario referansı görünür olmalıdır | İzlenebilirlik |

---

## 8. Admin UX Flow

### 8.1. Storyboard Generation Başlangıcı

Admin, Content Project detail sayfasındaki Scenario bölümünde `APPROVED` statüsündeki
senaryoyu görür. Approved senaryo altında **"Storyboard Oluştur"** butonu yer alır.

**Precondition:** Approved scenario mevcut olmalıdır.

**Storyboard oluşturma mümkün değilse:**
- Senaryo henüz onaylanmamışsa → buton disabled, tooltip: "Önce senaryoyu onaylayın"
- Senaryo REJECTED ise → buton gösterilmez
- Aktif bir approved storyboard varsa → buton label: "Yeni Versiyon Oluştur"

### 8.2. Generation Sırasında

- "Storyboard Oluştur" butonuna basıldığında loading spinner görünür
- Buton disabled olur, çift tıklama engellenir
- Progress mesajı: "Storyboard oluşturuluyor…"
- Hata durumunda error toast/banner, admin retry yapabilir

### 8.3. Generation Sonrası

- Storyboard oluşturulduğunda otomatik olarak detail sayfası açılır
- Storyboard statüsü `AI_GENERATED` badge ile gösterilir
- Scene listesi sıralı olarak görünür (scene 1, 2, 3…)
- Her scene kartında: scene başlığı, görsel açıklama özeti, mood badge

### 8.4. Scene Review

Admin storyboard detail sayfasında tüm scene'leri sıralı görür:

| Alan | Gösterim |
|------|----------|
| Scene numarası | Badge (1, 2, 3…) |
| Kaynak beat tipi | Label (HOOK, INTRODUCTION, vb.) |
| Scene başlığı | Başlık text |
| Görsel açıklama | Çok satırlı metin alanı |
| Narration / voiceover | Çok satırlı metin alanı |
| Kamera yönü | Metin alanı |
| Mood / emotion | Badge veya tag |
| Süre (saniye) | Sayı |

### 8.5. Scene Editing

- `AI_GENERATED` veya `NEEDS_REVIEW` statüsündeki storyboard'lar düzenlenebilir
- Admin herhangi bir scene alanını düzenleyebilir
- "Kaydet" butonu değişiklikleri persist eder
- Kaydet sonrası storyboard statüsü `NEEDS_REVIEW` olur (ilk kez edit ise)
- Kaydet **asla** approve yapmaz

### 8.6. Approve / Reject

- **Approve butonu:** `AI_GENERATED` veya `NEEDS_REVIEW` statüsünde görünür
- Approve tıklanınca confirmation dialog: "Bu storyboard'u onaylıyor musunuz?"
- Onay sonrası storyboard `APPROVED` olur, tüm alanlar read-only
- **Reject butonu:** `AI_GENERATED` veya `NEEDS_REVIEW` statüsünde görünür
- Reject tıklanınca reason input alanı açılır (zorunlu)
- Red sonrası storyboard `REJECTED` olur, tüm alanlar read-only

### 8.7. Read-Only States

- `APPROVED` → yeşil badge, tüm alanlar read-only, "Onaylandı" bilgi mesajı
- `REJECTED` → kırmızı badge, tüm alanlar read-only, red sebebi görünür
- `FAILED` → turuncu badge, hata mesajı görünür, "Tekrar Dene" butonu aktif

### 8.8. Storyboard Version List

Content Project detail sayfasında, senaryo bölümünün altında storyboard versiyonları listesi:

| Kolon | Açıklama |
|-------|----------|
| Versiyon | v1, v2, v3… |
| Statü | AI_GENERATED / NEEDS_REVIEW / APPROVED / REJECTED / FAILED |
| Oluşturma tarihi | Timestamp |
| Kaynak senaryo | Source scenario referansı |

---

## 9. Storyboard Content Shape

### Storyboard (Top-level)

| Alan | Tip | Açıklama |
|------|-----|----------|
| title | String | Storyboard başlığı (AI tarafından üretilebilir, admin düzenleyebilir) |
| summary | String | Storyboard özeti |
| targetPlatform | String (optional) | Hedef platform (YouTube, Instagram Reels, vb.) |
| aspectRatio | String (optional) | Görüntü oranı (16:9, 9:16, 1:1) |
| estimatedDurationSeconds | Integer | Toplam tahmini süre (tüm scene süreleri toplamı) |
| sourceScenarioReference | Reference | Kaynak ScenarioDraft ID |
| reviewNotes | String (optional) | Admin review notları |
| scenes | List | Sıralı scene listesi |

### Storyboard Scene

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| sceneNumber | Integer | ✅ | Sıra numarası (1-based) |
| sourceBeatType | String | ✅ | Kaynak beat tipi (HOOK, INTRODUCTION, vb.) |
| sourceBeatReference | Reference (optional) | | Kaynak ScenarioBeat ID (izlenebilirlik) |
| sceneTitle | String | ✅ | Sahne başlığı |
| visualDescription | String | ✅ | Sahnenin görsel açıklaması — ne gösterilecek |
| narrationText | String | | Voiceover / narration metni |
| onScreenText | String (optional) | | Ekranda gösterilecek metin (alt yazı, başlık vb.) |
| cameraDirection | String (optional) | | Kamera hareketi / açısı (wide shot, close-up, pan, zoom) |
| transitionNote | String (optional) | | Sahne geçiş notu (fade, cut, dissolve) |
| mood | String (optional) | | Sahne atmosferi / duygu (heyecanlı, dramatik, huzurlu) |
| locationSetting | String (optional) | | Sahne mekanı (ör. "Göbeklitepe harabeleri", "stüdyo") |
| charactersSubjects | String (optional) | | Sahnedeki karakterler/özneler |
| estimatedDurationSeconds | Integer | ✅ | Sahne tahmini süresi (saniye) |
| mediaGenerationNotes | String (optional) | | Gelecek media üretimi için notlar — yalnızca handoff metadata |

**Önemli notlar:**

- Scene içeriği yalnızca **descriptive** (açıklayıcı) metindir
- FP-003 gerçek image/video/audio **üretmez**
- `mediaGenerationNotes` yalnızca gelecek media generation handoff'u için metadata olarak capture edilir
- Scene sırası (`sceneNumber`) deterministiktir ve beat sırasını takip eder

> **Not:** Nihai alan isimleri ve tipleri engineering design task'ında (Task 03) belirlenecektir.

---

## 10. Validation Rules

### Storyboard-Level

| # | Kural | Gerekçe |
|---|-------|---------|
| 1 | Storyboard bir Content Project'e bağlı olmalıdır | İzlenebilirlik |
| 2 | Storyboard bir approved source ScenarioDraft'a bağlı olmalıdır | Prerequisite enforcement |
| 3 | Storyboard generation yalnızca approved scenario'dan yapılabilir | Governance |
| 4 | Storyboard approval scene content'in mevcut olmasını gerektirir | Boş storyboard onaylanamaz |
| 5 | Storyboard otomatik olarak approved yapılamaz | Governance |
| 6 | Storyboard source scenario ve source project'e traceable kalmalıdır | Audit trail |
| 7 | Storyboard raw provider payload expose etmemeli | Data safety |
| 8 | Storyboard API keys/secrets store etmemeli | Security |
| 9 | Storyboard provider-specific field'ları REST model'de gerektirmemeli | API contract cleanliness |

### Scene-Level

| # | Kural | Gerekçe |
|---|-------|---------|
| 10 | Scene sırası deterministik olmalıdır | Tutarlılık |
| 11 | Zorunlu scene alanları (sceneTitle, visualDescription, sourceBeatType, estimatedDurationSeconds) boş olamaz | Data quality |
| 12 | Scene numarası 1-based ve boşluksuz olmalıdır | UI tutarlılığı |
| 13 | Scene sayısı beat sayısıyla en az eşit olmalıdır | Her beat en az bir scene'e map edilmeli |

---

## 11. Error and Retry Behavior

### AI Provider Failure

| Durum | Davranış |
|-------|----------|
| OpenAI API timeout | Storyboard `FAILED` statüsüne geçer, hata mesajı kaydedilir |
| OpenAI rate limit | Storyboard `FAILED`, admin retry yapabilir |
| Invalid API key | Storyboard `FAILED`, auth error log |
| Network error | Storyboard `FAILED`, admin retry yapabilir |

### Missing Approved Scenario

| Durum | Davranış |
|-------|----------|
| Senaryo approved değil | HTTP 409 — "Storyboard üretimi için onaylanmış senaryo gereklidir" |
| Senaryo bulunamadı | HTTP 404 |
| Content project bulunamadı | HTTP 404 |

### Invalid or Empty Storyboard Output

| Durum | Davranış |
|-------|----------|
| AI boş response döndü | Storyboard `FAILED`, "Boş response" hata mesajı |
| AI eksik scene üretimi | Scene count validation failure → `FAILED` |
| AI JSON parse hatası | `FAILED`, parse error log |
| AI beklenmeyen format | `FAILED`, format error log |

### User Retry

| Durum | Davranış |
|-------|----------|
| `FAILED` storyboard'dan retry | Aynı approved scenario ile yeni generation tetiklenir |
| Yeni storyboard oluşturulur | `AI_GENERATED` statüsünde, yeni version numarası |
| Eski `FAILED` storyboard korunur | Overwrite yapılmaz, tarihçe kalır |

### Regeneration

| Durum | Davranış |
|-------|----------|
| Mevcut `APPROVED` storyboard var | Yeni versiyon oluşturulur (v2, v3…) |
| Approved storyboard overwrite | ❌ Yasak — approved versiyon korunur |
| Yeni versiyon oluşturulduktan sonra | One-approved rule: yeni versiyon approve edilemez (approved zaten var) |

---

## 12. Relationship to FP-001 and FP-002

### Pipeline Akışı

```
FP-001                    FP-002                    FP-003
──────────────────────    ──────────────────────    ──────────────────────
Knowledge Item            Approved Content Draft    Approved Scenario
    ↓                         ↓                         ↓
Content Project           Scenario Generation       Storyboard Generation
    ↓                         ↓                         ↓
AI Content Draft          ScenarioDraft              Storyboard
    ↓                     (6 beats)                  (N scenes)
Human Review                  ↓                         ↓
    ↓                     Human Review              Human Review
Approved Draft                ↓                         ↓
                          Approved Scenario          Approved Storyboard
                                                        ↓
                                                    [Future: Media Gen]
```

### Bağımlılık Kuralları

| # | Kural |
|---|-------|
| 1 | FP-001 approved source content draft sağlar |
| 2 | FP-002 approved scenario sağlar |
| 3 | FP-003 approved scenario'yu consume eder |
| 4 | FP-003 mevcut AI provider architecture'ını yeniden kullanır (AiGatewayConfig, OpenAI client) |
| 5 | FP-003 FP-001/FP-002 safety rules'u zayıflatmaz |
| 6 | FP-003 internal AI contract'ları expose etmez |
| 7 | FP-003 raw prompt veya raw provider payload store/expose etmez |
| 8 | FP-003 mevcut ScenarioDraft/ScenarioBeat entity'lerine dokunmaz |
| 9 | FP-003 mevcut ContentDraft entity'sine dokunmaz |
| 10 | FP-003 mevcut gateway pattern'ını takip eder (interface + openai impl + mock fallback) |

### Provider Architecture (Reuse)

FP-003, FP-001/FP-002 ile aynı provider altyapısını kullanacaktır:

```
AiGatewayConfig
  ├── @ConditionalOnProperty(ai.provider=openai)  → OpenAiStoryboardAgentGateway [NEW]
  └── @ConditionalOnMissingBean                   → MockAiStoryboardAgentGateway [NEW]
```

Mevcut FP-001 ve FP-002 gateway'leri etkilenmez.

---

## 13. Future Handoff: Multi-Platform Content Packaging

FP-003 storyboard tasarımı, gelecekte çok platformlu içerik paketleme workflow'unu destekleyecek şekilde düşünülmüştür.

### Gelecek Platform Hedefleri

| Platform | Format | Özellikler |
|----------|--------|-----------|
| **YouTube Long-form** | Yatay 16:9 | Uzun hikâye anlatımı, zengin narration, detaylı scene'ler |
| **YouTube Shorts** | Dikey 9:16 | Kısa hook-driven cut, özlü hikâye anı, hızlı ritim |
| **Instagram Reels** | Dikey 9:16 | Hızlı görsel ritim, duygusal/merak hook'u |
| **Facebook Reels** | Dikey 9:16 | Erişilebilir hikâye anlatımı, hafif nostaljik/açıklayıcı ton |
| **Social Metadata** | Metin | Başlık, açıklama, hashtag'ler, CTA |

### Platform-Spesifik Çıktı Şekli (Gelecek)

Her platform paketi şu çıktıları içerebilir:

- **Video package:** Platform-optimize edilmiş scene seçimi + süre + format
- **Thumbnail/cover:** Platform boyutunda kapak görseli
- **Title + caption:** Platform diline uygun başlık ve açıklama
- **Hashtags:** Platform algoritmasına uygun etiketler
- **CTA:** Platforma özel izleyici eylemi çağrısı

### FP-003 ile İlişki

| # | Kural |
|---|-------|
| 1 | Multi-platform content packaging **FP-004 scope'udur**, FP-003 kapsamında değildir |
| 2 | FP-003 gerçek video dosyası **üretmez** |
| 3 | FP-003 platformlara **yayınlamaz** |
| 4 | FP-003 social media postları **oluşturmaz** |
| 5 | FP-003 `targetPlatform`, `aspectRatio`, `estimatedDurationSeconds` gibi product-level alanları tutar — gelecek paketleme için handoff kolaylığı sağlar |
| 6 | FP-003 storyboard, FP-004'ün girdi kaynağı olarak tasarlanmıştır: **Approved StoryboardDraft → platform-spesifik içerik paketleri** |

---

## 14. Future Follow-ups

Aşağıdakiler yalnızca gelecek scope olarak listelenmiştir, FP-003 kapsamında değildir:

| # | Follow-up | Sprint |
|---|-----------|--------|
| 1 | FP-003 Task 02 — AI platform contract design | FP-003 |
| 2 | FP-003 Task 03 — Engineering design & task breakdown | FP-003 |
| 3 | Backend storyboard domain/schema implementation | FP-003 |
| 4 | Admin storyboard UI implementation | FP-003 |
| 5 | **FP-004 — Multi-Platform Content Packaging Workflow** | **Future FP** |
| 6 | Image generation workflow | Future FP |
| 7 | Video generation workflow | Future FP |
| 8 | Audio / TTS generation workflow | Future FP |
| 9 | Media asset management | Future FP |
| 10 | Cost dashboard | Future FP |
| 11 | Prompt management UI | Future FP |
| 12 | Analytics | Future FP |

---

## 15. Acceptance Criteria

| # | Kriter | Durum |
|---|--------|-------|
| 1 | FP-003 Task 01 product/workflow design dokümanı oluşturuldu | ✅ |
| 2 | Backend kodu değiştirilmedi | ✅ |
| 3 | Frontend kodu değiştirilmedi | ✅ |
| 4 | Schema/migration oluşturulmadı | ✅ |
| 5 | API endpoint oluşturulmadı | ✅ |
| 6 | Provider prompt text yazılmadı | ✅ |
| 7 | Storyboard lifecycle belgelendi | ✅ |
| 8 | Human review rules belgelendi | ✅ |
| 9 | Admin UX flow product level'da belgelendi | ✅ |
| 10 | Storyboard content shape product level'da belgelendi | ✅ |
| 11 | FP-001/FP-002 safety rules korundu | ✅ |
| 12 | Out-of-scope items açıkça exclude edildi | ✅ |
| 13 | FP-003 yalnızca Storyboard Workflow'a odaklı | ✅ |
| 14 | No secrets/raw prompt/raw provider payload | ✅ |
| 15 | Multi-platform content packaging gelecek FP-004 olarak belgelendi | ✅ |
