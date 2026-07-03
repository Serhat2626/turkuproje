# 50 — FP-006 Visual Asset Planning & Image Generation Handoff — Product Design

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-006 — Visual Asset Planning & Image Generation Handoff
>
> **Prerequisites**: FP-001 (CLOSED), FP-002 (CLOSED), FP-003 (CLOSED), FP-004 (CLOSED), FP-005 (CLOSED)

---

## 1. Status

**Design / Planning**

Bu doküman FP-006'nın ürün ve iş akışı tasarımıdır.
İmplementasyon henüz başlamadı. Backend, frontend, schema, API, migration, gateway
ve runtime config oluşturulmamıştır.

---

## 2. Feature Objective

FP-006, onaylanmış (APPROVED) storyboard taslaklarındaki sahne bazlı görsel
açıklamalardan yapılandırılmış görsel varlık planları ve image-generation-ready
prompt handoff'ları üretmeyi sağlar.

**Amaçlar:**

- Onaylanmış storyboard sahnelerinden AI-ready image prompt planları üretme
- Sahne bazlı görsel varlık planlama (gerekli görsel, stil, kompozisyon, en-boy oranı, mood, görsel kısıtlamalar)
- Gelecek görsel üretim araçları için yapılandırılmış image prompt handoff üretme
- Sahne-görsel izlenebilirliğini (traceability) koruma
- Üretilen görsel prompt planlarının insan review/edit/approve/reject desteği
- Gelecekteki gerçek görsel üretim için altyapı hazırlığı (ilk fazda gerçek görsel üretimi yok)

**Ürün Değeri:**

Kullanıcı bir konu girer → AI draft oluşturulur (FP-001) → draft onaylanır →
senaryo üretilir (FP-002) → senaryo onaylanır → storyboard üretilir (FP-003) →
storyboard onaylanır → platform paketleri üretilir (FP-004) → paketler onaylanır →
export yapılır (FP-005) → **FP-006 ile onaylanmış storyboard'dan sahne bazlı
görsel varlık planları ve image-generation-ready prompt'lar üretilir**. Bu çıktılar
bir görsel sanatçıya, AI image generation aracına veya thumbnail tasarımcısına
doğrudan teslim edilebilir.

**Kritik Kısıtlamalar:**

| # | Kısıtlama |
|---|-----------|
| 1 | Kaynak **yalnızca APPROVED** StoryboardDraft olabilir |
| 2 | AI_GENERATED, NEEDS_REVIEW, REJECTED, FAILED storyboard'lar kaynak olarak kullanılamaz |
| 3 | İlk fazda **gerçek görsel üretimi yapılmaz** — yalnızca prompt planlama ve handoff |
| 4 | İlk fazda DALL-E, Midjourney, Stable Diffusion veya herhangi bir image generation API çağrılmaz |
| 5 | İlk fazda MediaAsset/GeneratedAsset kaydı oluşturulmaz |
| 6 | İlk fazda görsel dosya indirme/yükleme/depolama yapılmaz |
| 7 | Üretilen image prompt metni ürün çıktısıdır — gösterilir, düzenlenir, onaylanır |
| 8 | İç AI orchestration prompt'u, provider isteği/yanıtı, API key/secret asla expose edilmez |

**Önemli Ayrım — İki Farklı "Prompt" Kavramı:**

| Kavram | Görünürlük | Açıklama |
|--------|-----------|----------|
| **Image prompt (ürün çıktısı)** | ✅ Görünür | AI tarafından üretilen, admin tarafından düzenlenen/onaylanan image-generation-ready metin. Bu bir product output'tur |
| **Internal AI orchestration prompt** | ❌ Gizli | AI'a gönderilen iç sistem prompt'u. Asla expose edilmez |

---

## 3. Terminology

| Terim | Açıklama |
|-------|----------|
| **Visual Asset Plan** | Bir storyboard'un tüm sahneleri için üretilen görsel varlık planı (parent entity). Stil rehberi, renk paleti, karakter/mekan sürekliliği kararlarını içerir |
| **Scene Visual Plan** | Tek bir storyboard sahnesi için üretilen detaylı görsel plan (child entity). Image prompt, negatif prompt, stil, kompozisyon bilgilerini içerir |
| **Image Prompt** | Image generation aracına giriş olarak kullanılabilecek İngilizce metin. Sahne görselinin ne olması gerektiğini tanımlar. Bu bir ürün çıktısıdır |
| **Negative Prompt** | Image generation aracına "bu olmasın" yönlendirmesi veren metin. Kalite ve tutarlılık için kullanılır |
| **Style Directive** | Görsel stil yönlendirmesi (realistic, illustrated, cinematic, watercolor, oil painting, etc.) |
| **Thumbnail Prompt** | Video thumbnail'i için özel görsel plan. Yüksek etkili, dikkat çekici, metin yerleşimli kompozisyon |
| **Visual Handoff** | Görsel planın bir tasarımcıya, sanatçıya veya AI image generation aracına teslim edilmesi |
| **Composition Notes** | Çerçeveleme, perspektif, odak noktası, derinlik gibi görsel yapı notları |
| **Cultural Authenticity Notes** | Anadolu/Türk kültürel doğruluğu için yönlendirmeler |

---

## 4. Product Context

### Content Pipeline Zinciri

```
Knowledge Item
    └── Content Project
            └── Content Draft (FP-001)
                    └── Scenario Draft (FP-002)
                            └── Storyboard Draft (FP-003)
                                    ├── Platform Package Draft (FP-004)
                                    │       └── Export / Handoff (FP-005)
                                    └── Visual Asset Plan (FP-006) ← YENİ
```

FP-006, pipeline'da FP-003 (Storyboard) çıktısını doğrudan kaynak olarak kullanır.
FP-004/FP-005 zincirinden bağımsız, paralel bir daldır.

**Gerekçe:** Görsel varlık planlaması storyboard seviyesinde yapılır çünkü
görsel süreklilik ve karakter/mekan tutarlılığı storyboard'a aittir.
Platform paketleri (FP-004) dağıtım adaptasyonlarıdır ve görsel planlamayı
doğrudan yönetmezler. Gelecekte platform-spesifik görsel adaptasyonlar
PlatformPackageSceneUsage referansıyla eklenebilir.

### Önceki Feature'ların Durumu

| Feature | Status | Çıktısı |
|---------|--------|---------| 
| FP-001 | CLOSED | Onaylanmış Content Draft |
| FP-002 | CLOSED | Onaylanmış Scenario Draft |
| FP-003 | CLOSED | Onaylanmış Storyboard Draft |
| FP-004 | CLOSED | Onaylanmış Platform Package Draft |
| FP-005 | CLOSED | Export / Production Handoff |
| FP-006 | DESIGN | Visual Asset Plan & Image Prompt Handoff |

---

## 5. Source Prerequisite

FP-006'nın birincil kaynağı **APPROVED** `StoryboardDraft` entity'sidir.

### İzin Verilen Kaynak Status

| Status | İzin | Açıklama |
|--------|------|----------|
| **APPROVED** | ✅ Evet | Tek izin verilen kaynak |

### Yasaklanan Kaynak Statuslar

| Status | İzin | Açıklama |
|--------|------|----------|
| AI_GENERATED | ❌ Hayır | Henüz insan review'ından geçmemiş |
| NEEDS_REVIEW | ❌ Hayır | Review süreci devam ediyor |
| REJECTED | ❌ Hayır | Reddedilmiş storyboard görsel plan kaynağı olamaz |
| FAILED | ❌ Hayır | Başarısız üretim görsel plan kaynağı olamaz |

**Governance kuralı:** Görsel varlık planı üretimi, FP-003 human review sürecini
atlayamaz. Yalnızca bir admin tarafından açıkça onaylanmış storyboard'lar görsel
plan kaynağı olabilir.

### Kaynak Verisi — StoryboardDraft'tan Okunan Alanlar

**Storyboard seviyesi (parent context):**

| Alan | Kullanım |
|------|----------|
| title | Proje/hikâye adı context'i |
| summary | Genel hikâye özeti |
| visualStyleGuide | Görsel stil rehberi — doğrudan visual plan'a aktarılır |
| targetPlatform | Hedef platform bilgisi (aspect ratio kararı etkiler) |
| aspectRatio | Varsayılan en-boy oranı |
| narrationStyle | Anlatım stili — mood etkiler |

**Sahne seviyesi (per-scene context):**

| Alan | Kullanım | Öncelik |
|------|----------|---------|
| visualDescription | Sahnenin görsel tanımı — **birincil input** | 🔴 Kritik |
| sceneTitle | Sahne başlığı — context | Yüksek |
| mood | Atmosfer/duygu — stil ve renk paletini etkiler | Yüksek |
| location | Sahne mekânı — arka plan/ortam tanımı | Yüksek |
| characters | Karakterler/özneler — karakter sürekliliği | Yüksek |
| cameraDirection | Kamera yönlendirmesi — kompozisyon etkiler | Orta |
| narrationText | Seslendirme metni — görsel-anlatı uyumu | Orta |
| onScreenText | Ekran metni — overlay yönlendirmesi | Orta |
| transitionNote | Geçiş notu — önceki/sonraki sahne ilişkisi | Düşük |
| mediaGenerationNotes | Medya üretim notları — ek yönlendirme | Orta |
| sourceBeatType | Kaynak beat tipi — sahne duygusal ağırlığını belirler | Orta |
| estimatedDurationSeconds | Sahne süresi — dinamizm ve detay seviyesi | Düşük |

### PlatformPackageDraft ile İlişki

**İlk faz kararı:** PlatformPackageDraft, FP-006 ilk fazında kaynak prerequisite
olarak kullanılmaz.

**Gerekçe:**
- Görsel süreklilik ve karakter/mekan tutarlılığı storyboard seviyesinde yönetilir
- Platform paketleri dağıtım adaptasyonlarıdır, görsel planlama değil
- İlk fazda storyboard-level tek görsel plan yeterlidir

**Gelecek faz:** Platform-spesifik görsel adaptasyonlar (16:9 → 9:16 crop/reframe
gibi) PlatformPackageSceneUsage referansıyla ayrı bir task olarak eklenebilir.

---

## 6. In Scope

| # | Kapsam |
|---|--------|
| 1 | APPROVED StoryboardDraft kaynağından görsel varlık planı (VisualAssetPlan) üretimi |
| 2 | Sahne bazlı scene visual plan (SceneVisualPlan) üretimi |
| 3 | Her sahne için İngilizce image-generation-ready prompt |
| 4 | Her sahne için Türkçe görsel bağlam açıklaması |
| 5 | Her sahne için negatif prompt |
| 6 | Stil yönlendirmesi (realistic, cinematic, illustrated, etc.) |
| 7 | Kompozisyon notları (çerçeveleme, perspektif, odak noktası) |
| 8 | Kamera/çerçeveleme rehberliği |
| 9 | Renk paleti / mood rehberliği |
| 10 | Karakter/özne süreklilik notları |
| 11 | Mekan/ortam sürekliliği notları |
| 12 | Kültürel/tarihsel doğruluk notları |
| 13 | Thumbnail prompt planlaması (image-generation-ready prompt, overlay text rehberliği) |
| 14 | Aspect ratio stratejisi (16:9, 9:16, 1:1) |
| 15 | AI destekli görsel plan üretimi (yeni AI gateway, mock fallback) |
| 16 | İnsan review workflow (AI_GENERATED → edit → approve/reject) |
| 17 | Admin UI görsel plan workflow |
| 18 | REST API (engineering design doc'ta detaylandırılacak) |
| 19 | Sahne-görsel izlenebilirliği (sceneNumber, sourceBeatType referansı) |
| 20 | Image generation tool notları (hangi araç için optimize edildiği bilgisi) |

---

## 7. Out of Scope

| # | Kapsam Dışı | Açıklama |
|---|-------------|----------|
| 1 | Gerçek görsel üretimi (image generation) | DALL-E, Midjourney, SD çağrısı FP-006'da yok |
| 2 | DALL-E API entegrasyonu | Gelecek FP-007+ |
| 3 | Midjourney entegrasyonu | Gelecek feature |
| 4 | Stable Diffusion entegrasyonu | Gelecek feature |
| 5 | Görsel dosya yükleme/depolama | MediaAsset/storage FP-006'da yok |
| 6 | MediaAsset kaydı oluşturma | Mevcut entity kullanılmaz, gelecek faz |
| 7 | GeneratedAsset kaydı oluşturma | Mevcut entity kullanılmaz, gelecek faz |
| 8 | Video üretimi / rendering | Gelecek feature |
| 9 | Ses / seslendirme üretimi | Gelecek feature |
| 10 | Müzik üretimi | Gelecek feature |
| 11 | Thumbnail görsel dosyası oluşturma | Sadece prompt planlaması, dosya üretimi yok |
| 12 | YouTube/Instagram/Facebook'a yayın | Publishing FP-006'da yok |
| 13 | OAuth / sosyal hesap bağlantısı | Gelecek feature |
| 14 | Zamanlama (scheduling) | FP-006'da yok |
| 15 | Analitik / dashboard | FP-006'da yok |
| 16 | Medya dosya yönetimi UI | FP-006'da yok |
| 17 | Görsel içeren ZIP paketi | FP-006'da yok |
| 18 | Maliyet izleme dashboard'u | İlk fazda image generation yok, maliyet izleme yok |
| 19 | READY_FOR_PUBLISH status | FP-006'da tanımlanmaz |
| 20 | Generic status update endpoint | Olmayacak |
| 21 | Otomatik onay (auto-approval) | AI çıktısı asla otomatik onaylanmaz |
| 22 | İç AI prompt / provider payload exposure | Asla expose edilmez |

---

## 8. Visual Asset Plan Content Shape

### 8.1. Parent Entity — VisualAssetPlan

Bir storyboard'un tüm sahneleri için üst düzey görsel varlık planı.

| Kategori | Alan | Tip | Açıklama |
|----------|------|-----|----------|
| **Kimlik** | id | UUID | PK |
| **İlişkiler** | contentProject | → ContentProject | İçerik projesi referansı |
| | sourceStoryboardDraft | → StoryboardDraft | Kaynak storyboard (APPROVED) |
| **Versiyon** | version | Integer | Plan versiyonu |
| **Status** | status | VisualAssetPlanStatus | Yaşam döngüsü durumu |
| **Genel Stil** | title | String | Plan başlığı |
| | summary | Text | Genel görsel plan özeti |
| | overallVisualStyle | Text | Bütünsel görsel stil yönlendirmesi (cinematic, illustrated, etc.) |
| | colorPaletteGuidance | Text | Genel renk paleti rehberliği |
| | culturalAuthenticityGuidance | Text | Kültürel/tarihsel doğruluk yönlendirmesi |
| | characterContinuityGuidance | Text | Karakter görsel süreklilik notları |
| | locationContinuityGuidance | Text | Mekan görsel süreklilik notları |
| | aspectRatioStrategy | String | Varsayılan aspect ratio stratejisi |
| **AI Metadata** | generatedBy | GeneratedBy | Üretim kaynağı |
| | providerType | String | Provider tipi (safe metadata) |
| | modelName | String | Model adı (safe metadata) |
| **Review** | reviewNotes | Text | Admin review notları |
| | editedAt, editedBy | DateTime, String | Düzenleme bilgisi |
| | approvedAt, approvedBy | DateTime, String | Onay bilgisi |
| | rejectedAt, rejectedBy | DateTime, String | Red bilgisi |
| | rejectionReason | Text | Red gerekçesi |
| **Children** | sceneVisualPlans | List | Sahne görsel planları (ordered by sceneNumber) |
| | thumbnailPlans | List | Thumbnail planları |

### 8.2. Child Entity — SceneVisualPlan

Her storyboard sahnesi için detaylı görsel plan ve image-generation-ready prompt.

| Kategori | Alan | Tip | Açıklama |
|----------|------|-----|----------|
| **Kimlik** | id | UUID | PK |
| **İlişkiler** | visualAssetPlan | → VisualAssetPlan | Parent plan |
| | sourceStoryboardScene | → StoryboardScene | Kaynak sahne |
| **Sahne Ref** | sceneNumber | Integer | Sahne numarası (1-based) |
| | sourceBeatType | ScenarioBeatType | Kaynak beat tipi |
| | sceneTitle | String | Sahne başlığı (storyboard'dan) |
| **Image Prompt** | englishImagePrompt | Text | 🔴 İngilizce image-generation-ready prompt — **ürün çıktısı** |
| | turkishVisualContext | Text | Türkçe görsel bağlam açıklaması |
| | negativePrompt | Text | Negatif prompt (ne olmaması gerektiği) |
| **Stil** | styleDirective | String | Görsel stil (realistic, cinematic, illustrated, watercolor, etc.) |
| | aspectRatio | String | Bu sahne için aspect ratio (16:9, 9:16, 1:1) |
| **Kompozisyon** | compositionNotes | Text | Çerçeveleme, perspektif, odak noktası |
| | cameraFraming | String | Kamera açısı/çerçeveleme (wide, medium, close-up, bird-eye, etc.) |
| | lightingNotes | Text | Aydınlatma yönlendirmesi (doğal, dramatik, alacakaranlık, etc.) |
| **Mood / Renk** | colorPalette | Text | Sahne renk paleti |
| | mood | String | Atmosfer/duygu |
| **İçerik** | location | Text | Mekan/ortam tanımı |
| | characters | Text | Karakter/özne tanımı ve görsel notları |
| | culturalDetails | Text | Kültürel/tarihsel doğruluk detayları |
| **Overlay** | textOverlayGuidance | Text | Ekran metni/overlay yönlendirmesi |
| **Tool Notes** | imageGenerationToolNotes | Text | Hangi AI image tool için optimize edildiği notları |
| | safetyNotes | Text | İçerik güvenliği notları (hassas görsel yönlendirmeler) |

### 8.3. Child Entity — ThumbnailPlan

Video thumbnail'i için özel görsel plan.

| Kategori | Alan | Tip | Açıklama |
|----------|------|-----|----------|
| **Kimlik** | id | UUID | PK |
| **İlişkiler** | visualAssetPlan | → VisualAssetPlan | Parent plan |
| | sourceSceneReference | → StoryboardScene | Referans sahne (opsiyonel) |
| **Prompt** | thumbnailPrompt | Text | 🔴 Thumbnail image-generation-ready prompt — **ürün çıktısı** |
| | negativePrompt | Text | Negatif prompt |
| **Kompozisyon** | textOverlayGuidance | Text | Thumbnail üzerindeki metin yerleşimi rehberliği |
| | compositionNotes | Text | Kompozisyon notları (odak noktası, denge) |
| | emotionalHook | String | Duygusal çekicilik notu (merak, şaşkınlık, nostalji) |
| **Stil** | aspectRatio | String | Aspect ratio (genellikle 16:9) |
| | styleDirective | String | Stil yönlendirmesi |
| | colorPalette | Text | Renk paleti |
| **Platform** | targetPlatform | String | Hedef platform (YouTube, IG, etc.) |

---

## 9. Lifecycle / Status Model

### 9.1. Status Enum — VisualAssetPlanStatus

| Status | Açıklama |
|--------|----------|
| `AI_GENERATED` | AI tarafından üretildi, henüz insan review'ından geçmemiş |
| `NEEDS_REVIEW` | Admin tarafından düzenlendi, tekrar inceleme gerekiyor |
| `APPROVED` | Admin tarafından açıkça onaylandı |
| `REJECTED` | Admin tarafından açıkça reddedildi |
| `FAILED` | AI üretimi başarısız oldu |

### 9.2. Status Flow

```
                    ┌────────────────────────┐
                    │   Generate Request     │
                    └──────────┬─────────────┘
                               │
                    ┌──────────▼─────────────┐
              ┌─────│     AI_GENERATED       │─────┐
              │     └──────────┬─────────────┘     │
              │                │                    │
         (AI failure)     (Admin edit)         (Admin review)
              │                │                    │
    ┌─────────▼────┐  ┌───────▼──────────┐        │
    │    FAILED    │  │   NEEDS_REVIEW   │◄───┐   │
    └──────────────┘  └───────┬──────────┘    │   │
                              │               │   │
                         (Admin review)  (Admin edit)
                              │               │   │
                    ┌─────────▼───────────────┘   │
                    │                              │
              ┌─────▼──────┐              ┌───────▼──────┐
              │  APPROVED  │              │   REJECTED   │
              └────────────┘              └──────────────┘
```

### 9.3. Lifecycle Rules

| # | Kural | Açıklama |
|---|-------|----------|
| 1 | AI üretimi → AI_GENERATED | AI çıktısı yalnızca AI_GENERATED statüsünde oluşturulur |
| 2 | AI üretimi asla APPROVED üretmez | Otomatik onay yasaktır |
| 3 | Admin edit → NEEDS_REVIEW | Düzenleme otomatik olarak NEEDS_REVIEW'a geçer |
| 4 | Admin approve → APPROVED | Onay açık admin eylemidir |
| 5 | Admin reject → REJECTED | Red açık admin eylemidir |
| 6 | AI hatası → FAILED | Provider hatası veya geçersiz çıktı |
| 7 | APPROVED plan tekrar düzenlenebilir | Edit → NEEDS_REVIEW'a döner |
| 8 | REJECTED plan tekrar düzenlenebilir | Edit → NEEDS_REVIEW'a döner |
| 9 | FAILED plan düzenlenemez | Yeni üretim gerekir |
| 10 | Generic status endpoint yok | Status değişikliği yalnızca dedicated action'lar ile |
| 11 | READY_FOR_PUBLISH yok | Bu status FP-006'da tanımlanmaz |

### 9.4. Versioning

| Karar | Açıklama |
|-------|----------|
| Bir storyboard için birden fazla versiyon üretilebilir | Regenerate → yeni version |
| Aynı anda yalnızca bir plan APPROVED olabilir | Yeni onay, eski APPROVED'ı override edebilir veya önceki onayı iptal eder |
| Version numarası otomatik artar | Her yeni üretimde version++ |

---

## 10. User Workflow

### 10.1. Generation Flow

```
1. Admin, ContentProject detay sayfasını açar
2. Admin, onaylanmış storyboard'u görür (FP-003 UI'dan)
3. Admin, "Görsel Plan Üret" butonuna tıklar
4. Sistem, APPROVED storyboard kontrolü yapar
5. Sistem, AI'a storyboard + scenes verisini gönderir
6. AI, sahne bazlı image prompt planları üretir
7. Sistem, VisualAssetPlan + SceneVisualPlans + ThumbnailPlan(s) oluşturur
8. Status: AI_GENERATED
9. Admin, üretilen görsel planı inceler
```

### 10.2. Review Flow

```
1. Admin, visual asset plan detay sayfasını açar
2. Sahne bazlı image prompt'ları inceler:
   • İngilizce image prompt
   • Türkçe görsel bağlam
   • Negatif prompt
   • Stil / kompozisyon / renk notları
3. Thumbnail prompt'u inceler
4. Genel stil rehberini inceler
5. Admin onaylar veya reddeder
```

### 10.3. Edit Flow

```
1. Admin, AI_GENERATED veya NEEDS_REVIEW planı açar
2. Admin, sahne image prompt'larını düzenler:
   • İngilizce prompt'u iyileştirir
   • Negatif prompt ekler/düzenler
   • Stil yönlendirmesini değiştirir
   • Kompozisyon notlarını günceller
3. Admin, thumbnail prompt'unu düzenler
4. Admin, genel stil rehberini düzenler
5. Kaydet → status NEEDS_REVIEW'a geçer
6. Admin tekrar inceler ve onaylar/reddeder
```

### 10.4. Approve/Reject Flow

```
Onay:
1. Admin, incelemeyi tamamlar
2. "✅ Onayla" butonuna tıklar
3. Status → APPROVED
4. approvedAt, approvedBy kaydedilir
5. Plan artık export/handoff için hazırdır

Red:
1. Admin, incelemeyi tamamlar
2. "❌ Reddet" butonuna tıklar
3. Red gerekçesi girilir
4. Status → REJECTED
5. rejectedAt, rejectedBy, rejectionReason kaydedilir
6. Admin yeni versiyon üretebilir veya mevcut planı düzenleyebilir
```

---

## 11. Human Review Rules

| # | Kural | Uygulama |
|---|-------|----------|
| 1 | AI çıktısı asla otomatik APPROVED olmaz | VisualAssetPlan AI_GENERATED status ile oluşturulur |
| 2 | Onay açık admin eylemidir | Dedicated approve action |
| 3 | Red açık admin eylemidir | Dedicated reject action |
| 4 | Düzenleme onay vermez | Save/edit → NEEDS_REVIEW, asla APPROVED |
| 5 | Generic status update yok | PUT /status veya PATCH /status mevcut değil |
| 6 | Generic status dropdown yok | UI yalnızca dedicated approve/reject butonları kullanır |
| 7 | Admin her sahne prompt'unu ayrı ayrı inceleyebilir | Sahne bazlı detay görünümü |
| 8 | Admin thumbnail prompt'unu ayrı inceleyebilir | Thumbnail detay görünümü |
| 9 | Admin genel stil rehberini inceleyebilir | Parent plan detay görünümü |
| 10 | Onay/red kararı tüm plan için geçerlidir | Tekil sahne onayı/reddi ilk fazda yok |

---

## 12. Governance Rules

### 12.1. Source Governance

| # | Kural |
|---|-------|
| 1 | Kaynak StoryboardDraft **APPROVED** statüsünde olmalıdır |
| 2 | AI_GENERATED, NEEDS_REVIEW, REJECTED, FAILED storyboard kaynak olamaz |
| 3 | Storyboard'un sahneleri (scenes) yüklü olmalıdır |
| 4 | Görsel plan üretimi, storyboard human review sürecini atlayamaz |

### 12.2. AI Output Governance

| # | Kural |
|---|-------|
| 5 | AI çıktısı AI_GENERATED ile oluşturulur, asla APPROVED |
| 6 | AI çıktısı asla otomatik onaylanmaz |
| 7 | Mock provider fallback zorunludur |
| 8 | AI gateway provider-agnostic contract kullanır |

### 12.3. First Phase Boundary Governance

| # | Kural |
|---|-------|
| 9 | Gerçek görsel üretimi (image generation) FP-006'da yapılmaz |
| 10 | DALL-E, Midjourney, Stable Diffusion API çağrısı FP-006'da yapılmaz |
| 11 | MediaAsset kaydı FP-006'da oluşturulmaz |
| 12 | GeneratedAsset kaydı FP-006'da oluşturulmaz |
| 13 | Görsel dosya indirme/yükleme/depolama FP-006'da yapılmaz |
| 14 | Video/ses/müzik üretimi FP-006'da yapılmaz |

### 12.4. Status Governance

| # | Kural |
|---|-------|
| 15 | READY_FOR_PUBLISH status FP-006'da tanımlanmaz |
| 16 | Generic status update endpoint yoktur |
| 17 | Publishing/upload/schedule FP-006'da yapılmaz |
| 18 | Analitik FP-006'da yoktur |

### 12.5. Data Safety Governance

| # | Kural |
|---|-------|
| 19 | İç AI orchestration prompt'u API/UI'da expose edilmez |
| 20 | Provider request/response API/UI'da expose edilmez |
| 21 | Provider payload API/UI'da expose edilmez |
| 22 | API key/secret hiçbir zaman expose edilmez |
| 23 | Üretilen image prompt metni ürün çıktısıdır — gösterilir |
| 24 | Üretilen image prompt, iç AI system prompt'u ile karıştırılmamalıdır |

### 12.6. Human Review Governance

| # | Kural |
|---|-------|
| 25 | İnsan review zorunludur |
| 26 | Görsel prompt handoff, production media değildir |
| 27 | Onaylanmış görsel plan, gelecekte image generation input'u olabilir |

---

## 13. Validation Rules

### 13.1. Pre-Generation Validation

| # | Kural | Başarısız → |
|---|-------|-------------|
| 1 | Storyboard ID ile kayıt bulunmalı | 404 Not Found |
| 2 | Storyboard status = APPROVED olmalı | 409 Conflict |
| 3 | Storyboard en az 1 sahne içermeli | 400 Bad Request |
| 4 | ContentProject aktif olmalı | 409 Conflict |

### 13.2. Plan Integrity Rules

| # | Kural |
|---|-------|
| 5 | Her VisualAssetPlan bir sourceStoryboardDraft referansı içermelidir |
| 6 | Her SceneVisualPlan bir sourceStoryboardScene referansı içermelidir |
| 7 | SceneVisualPlan.sceneNumber, kaynak storyboard scene ile eşleşmelidir |
| 8 | En az bir ThumbnailPlan olmalıdır |
| 9 | englishImagePrompt boş olamaz (non-null, non-blank) |

### 13.3. Status Transition Rules

| # | Kural |
|---|-------|
| 10 | AI_GENERATED → NEEDS_REVIEW (edit) |
| 11 | AI_GENERATED → APPROVED (approve) |
| 12 | AI_GENERATED → REJECTED (reject) |
| 13 | NEEDS_REVIEW → APPROVED (approve) |
| 14 | NEEDS_REVIEW → REJECTED (reject) |
| 15 | APPROVED → NEEDS_REVIEW (edit) |
| 16 | REJECTED → NEEDS_REVIEW (edit) |
| 17 | FAILED → yalnızca yeni üretim |

---

## 14. Error and Retry Behavior

### 14.1. Error Cases

| Hata | HTTP | Açıklama | Admin UI Mesajı |
|------|------|----------|-----------------|
| Storyboard bulunamadı | 404 | Kaynak storyboard mevcut değil | "Storyboard bulunamadı" |
| Storyboard APPROVED değil | 409 | Storyboard henüz onaylanmamış | "Yalnızca onaylanmış storyboard'lardan görsel plan üretilebilir" |
| Storyboard sahnesiz | 400 | Storyboard'da sahne yok | "Storyboard'da sahne bulunamadı" |
| AI provider hatası | 500 | Provider yanıt vermedi | "Görsel plan üretimi başarısız. Tekrar deneyebilirsiniz" |
| AI invalid output | 500 | Provider geçersiz çıktı üretti | "AI çıktısı geçersiz. Tekrar deneyebilirsiniz" |

### 14.2. Retry Behavior

| Durum | Retryable? | Davranış |
|-------|-----------|----------|
| Provider timeout | ✅ Evet | Admin "Tekrar Üret" butonuyla yeniden deneyebilir |
| Provider error | ✅ Evet | Admin "Tekrar Üret" butonuyla yeniden deneyebilir |
| Invalid AI output | ✅ Evet | Yeni versiyon olarak tekrar üretim |
| Source not approved | ❌ Hayır | Önce storyboard onaylanmalı |
| Source not found | ❌ Hayır | Geçerli storyboard gerekli |

### 14.3. Mock Fallback

| Durum | Davranış |
|-------|----------|
| `ai.provider=openai` | OpenAI gateway aktif |
| `ai.provider` tanımsız veya farklı değer | Mock gateway fallback |
| Mock gateway | Önceden tanımlı örnek prompt planı döndürür |
| OpenAI hata | Mock'a otomatik fallback yok — hata raporlanır, admin tekrar dener |

### 14.4. Failed Visual Plans

- AI hatası durumunda status → FAILED
- FAILED planlar düzenlenemez
- FAILED planlar onaylanamaz/reddedilemez
- Admin yeni üretim başlatabilir (yeni version)
- Hata detayları admin'e gösterilir (provider tipine kadar — raw error body gösterilmez)

---

## 15. Dependencies

### 15.1. Entity Chain

```
Knowledge Item
    └── ContentProject
            └── ContentDraft (FP-001)
                    └── ScenarioDraft (FP-002)
                            └── StoryboardDraft (FP-003) ← FP-006 SOURCE
                                    ├── StoryboardScene (FP-003) ← PER-SCENE SOURCE
                                    ├── PlatformPackageDraft (FP-004) — parallel branch
                                    │       └── PlatformPackageSceneUsage (FP-004)
                                    │               └── Export (FP-005)
                                    └── VisualAssetPlan (FP-006) ← YENİ
                                            ├── SceneVisualPlan (FP-006) ← YENİ
                                            └── ThumbnailPlan (FP-006) ← YENİ
```

### 15.2. Doğrudan Bağımlılıklar

| Bağımlılık | Entity | Status Gereksinimi |
|------------|--------|-------------------|
| FP-003 | StoryboardDraft | APPROVED |
| FP-003 | StoryboardScene | (storyboard ile birlikte) |

### 15.3. Dolaylı Bağımlılıklar (Zincir)

| Feature | Entity | Açıklama |
|---------|--------|----------|
| FP-001 | ContentDraft | Orijinal metin taslağı (onaylanmış) |
| FP-002 | ScenarioDraft | Senaryo taslağı (onaylanmış) |
| FP-003 | StoryboardDraft | Storyboard taslağı (onaylanmış) — **doğrudan kaynak** |

### 15.4. Runtime Bağımlılıklar

| Bağımlılık | Açıklama |
|------------|----------|
| AI Gateway | Yeni AiVisualAssetAgentGateway (interface + OpenAI + mock) |
| PromptTemplate | IMAGE/THUMBNAIL kategorisinde yeni prompt şablonları |
| AiGenerationLog | Visual plan üretim logları |
| Admin UI | Yeni görsel plan workflow bölümü |

---

## 16. Platform-Specific Considerations

### 16.1. İlk Faz Yaklaşımı

İlk fazda görsel varlık planı storyboard seviyesinde üretilir. Platform-spesifik
görsel adaptasyonlar gelecek faza bırakılır.

| Platform | İlk Faz Yaklaşımı |
|----------|--------------------|
| YouTube Long-Form | 16:9 cinematic scene planning (primary) |
| YouTube Shorts | Gelecek: 9:16 vertical crop/reframe adaptasyonu |
| Instagram Reels | Gelecek: 9:16 vertical adaptasyon |
| Facebook Reels | Gelecek: 9:16 vertical adaptasyon |

### 16.2. Aspect Ratio Strategy

| Karar | Açıklama |
|-------|----------|
| Varsayılan aspect ratio | Storyboard'un `aspectRatio` değerinden alınır |
| Sahne bazlı override | Her SceneVisualPlan kendi aspect ratio'sunu belirleyebilir |
| Thumbnail | Genellikle 16:9 (YouTube standart) |
| Platform adaptasyonları | Gelecek fazda PlatformPackageSceneUsage referansıyla |

### 16.3. Gelecek Platform-Spesifik Adaptasyonlar

Gelecek fazda şu özellikler eklenebilir:

- Aynı sahne için 16:9 ve 9:16 versiyonlarının otomatik adaptasyonu
- PlatformPackageSceneUsage.usageType'a göre görsel varyasyonlar (PRIMARY → tam, ADAPTED → crop, PARTIAL → detay, OMITTED → atla)
- Platform-spesifik thumbnail varyasyonları

---

## 17. AI Generation Context

### 17.1. AI'a Gönderilecek Veriler (Kavramsal)

AI visual asset plan üretimi sırasında aşağıdaki veriler context olarak kullanılır:

**Storyboard düzeyinde context:**

| Veri | Kaynak | Kullanım |
|------|--------|----------|
| Hikâye başlığı | StoryboardDraft.title | Genel tema ve atmosfer |
| Hikâye özeti | StoryboardDraft.summary | Bütünsel görsel yönlendirme |
| Görsel stil rehberi | StoryboardDraft.visualStyleGuide | Stil tutarlılığı |
| Hedef platform | StoryboardDraft.targetPlatform | Aspect ratio, format kararları |
| Aspect ratio | StoryboardDraft.aspectRatio | Varsayılan oran |
| Anlatım stili | StoryboardDraft.narrationStyle | Mood etkisi |

**Sahne düzeyinde context:**

| Veri | Kaynak | Kullanım |
|------|--------|----------|
| Görsel açıklama | StoryboardScene.visualDescription | **Birincil input** |
| Sahne başlığı | StoryboardScene.sceneTitle | Context |
| Mood | StoryboardScene.mood | Atmosfer/renk paleti |
| Mekan | StoryboardScene.location | Arka plan/ortam |
| Karakterler | StoryboardScene.characters | Karakter tanımı |
| Kamera | StoryboardScene.cameraDirection | Kompozisyon |
| Seslendirme | StoryboardScene.narrationText | Görsel-anlatı uyumu |
| Ekran metni | StoryboardScene.onScreenText | Overlay yönlendirmesi |
| Beat tipi | StoryboardScene.sourceBeatType | Duygusal ağırlık |
| Medya notları | StoryboardScene.mediaGenerationNotes | Ek yönlendirme |

### 17.2. AI'dan Beklenen Çıktı (Kavramsal)

AI, yapılandırılmış bir yanıt döndürür:

**Plan düzeyinde çıktı:**
- Genel görsel stil özeti
- Renk paleti rehberliği
- Karakter süreklilik notları
- Mekan süreklilik notları
- Kültürel doğruluk yönlendirmesi

**Sahne düzeyinde çıktı (her sahne için):**
- İngilizce image-generation-ready prompt
- Türkçe görsel bağlam
- Negatif prompt
- Stil yönlendirmesi
- Kompozisyon notları
- Kamera/çerçeveleme
- Aydınlatma notları
- Renk paleti
- Karakter/mekan detayları
- Kültürel doğruluk notları

**Thumbnail çıktısı:**
- Thumbnail image prompt
- Negatif prompt
- Metin overlay rehberliği
- Kompozisyon notları
- Duygusal hook notu

> **Not:** Bu bölüm kavramsal AI input/output tanımıdır.
> Ham AI prompt metni, provider request/response veya API anahtarı bu dokümanda yer almaz.
> AI contract detayları ayrı bir engineering design dokümanında tanımlanacaktır.

---

## 18. Safety and Security

### 18.1. API/UI'da Bulunmaması Gereken Veriler

| # | Yasak Veri | Açıklama |
|---|-----------|----------|
| 1 | İç AI orchestration prompt | Sistem prompt'u, AI'a gönderilen yönlendirme metni |
| 2 | Raw provider request | OpenAI/alternatif API'ye gönderilen ham istek |
| 3 | Raw provider response | OpenAI/alternatif API'den dönen ham yanıt |
| 4 | Provider payload | Provider'a özgü request/response payload'lar |
| 5 | API key | OpenAI veya herhangi bir servis API anahtarı |
| 6 | Secret | Uygulama veya servis gizli bilgileri |
| 7 | Stack trace | İç hata izleme detayları |
| 8 | Debug data | Güvenli olmayan debug bilgileri |
| 9 | System prompt | Özel sistem prompt'ları |
| 10 | Unreviewed error bodies | Provider'dan dönen ham hata mesajları |

### 18.2. API/UI'da Bulunabilecek Safe Alanlar

| # | Safe Alan | Açıklama |
|---|----------|----------|
| 1 | englishImagePrompt | AI tarafından üretilen, admin tarafından onaylanan image prompt — **ürün çıktısı** |
| 2 | turkishVisualContext | Türkçe görsel bağlam — **ürün çıktısı** |
| 3 | negativePrompt | Negatif yönlendirme — **ürün çıktısı** |
| 4 | thumbnailPrompt | Thumbnail prompt — **ürün çıktısı** |
| 5 | styleDirective | Stil yönlendirmesi — **ürün çıktısı** |
| 6 | compositionNotes | Kompozisyon notları — **ürün çıktısı** |
| 7 | providerType | Provider tipi (OPENAI, MOCK) — safe metadata |
| 8 | modelName | Model adı (gpt-4o-mini) — safe metadata |
| 9 | generatedBy | Üretim kaynağı (AI_CONTENT_AGENT) — safe metadata |
| 10 | approvedBy / approvedAt | Onay bilgisi — safe metadata |

### 18.3. Önemli Ayrım

| Kavram | Görünürlük | Örnek |
|--------|-----------|-------|
| **Image prompt (ürün çıktısı)** | ✅ Admin görebilir, düzenleyebilir, onaylayabilir | "A cinematic wide shot of an ancient Anatolian village at sunset, warm golden tones..." |
| **Internal AI system prompt** | ❌ Asla gösterilmez | AI'a verilen yönlendirme: "You are a visual planning assistant. Given the following storyboard..." |
| **Provider request body** | ❌ Asla gösterilmez | `{"model": "gpt-4o-mini", "messages": [...]}` |
| **Provider response body** | ❌ Asla gösterilmez | `{"choices": [{"message": {"content": "..."}}]}` |

---

## 19. Future Handoff

Aşağıdaki özellikler FP-006 kapsamı dışındadır ancak gelecek feature'lar olarak planlanabilir:

| # | Gelecek Feature | Açıklama | Bağımlılık |
|---|-----------------|----------|------------|
| 1 | **FP-007: Actual Image Generation** | APPROVED visual plan prompt'larından DALL-E/SD ile gerçek görsel üretimi | FP-006 |
| 2 | Image storage / management | Üretilen görsellerin MediaAsset olarak saklanması | FP-007 |
| 3 | Voiceover / Audio handoff | Seslendirme metin teslimi ve TTS entegrasyonu | FP-002/FP-003 |
| 4 | Video assembly / render pipeline | Görsel + ses + metin birleştirme | FP-007 + Audio |
| 5 | Publishing workflow | YouTube/IG/FB yayın otomasyonu | FP-004/FP-005 + media |
| 6 | Platform-specific visual adaptations | 16:9 → 9:16 crop/reframe | FP-006 + FP-004 |
| 7 | Visual asset export (FP-005 extension) | Onaylanmış görsel planların export/handoff'u | FP-005 + FP-006 |
| 8 | Cost tracking | AI çağrı maliyetleri dashboard'u | Tüm AI features |
| 9 | Character/location library | Karakter ve mekan tutarlılığı kütüphanesi | FP-006 |

### Content Pipeline Vision (Tamamlanmış + Planlanan)

```
Knowledge Item
    └── Content Project
            └── Content Draft (FP-001) ✅
                    └── Scenario Draft (FP-002) ✅
                            └── Storyboard Draft (FP-003) ✅
                                    ├── Platform Package (FP-004) ✅
                                    │       └── Export (FP-005) ✅
                                    └── Visual Asset Plan (FP-006) ← CURRENT
                                            └── Image Generation (FP-007?) ← FUTURE
                                                    └── Video Assembly ← FUTURE
                                                            └── Publishing ← FUTURE
```

---

## 20. Open Questions

| # | Soru | Önerilen Cevap |
|---|------|----------------|
| 1 | Kaynak StoryboardDraft mı PlatformPackageDraft mı olmalı? | İlk fazda StoryboardDraft. Görsel süreklilik storyboard seviyesinde yönetilir |
| 2 | Storyboard başına mı platform paket başına mı görsel plan olmalı? | İlk fazda storyboard başına tek plan. Platform adaptasyonları gelecek fazda |
| 3 | FP-006 gerçek görsel üretmeli mi? | Hayır. Sadece prompt planlama ve handoff. Gerçek üretim FP-007+ |
| 4 | MediaAsset/GeneratedAsset kullanılmalı mı? | İlk fazda hayır. Mevcut entity'ler gelecek image generation fazı için |
| 5 | Image prompt'lar İngilizce mi Türkçe mi olmalı? | İngilizce image prompt + Türkçe bağlam açıklaması. Image gen araçları İngilizce optimize |
| 6 | Thumbnail planı dahil mi? | Evet. Prompt planlaması dahil, görsel dosya üretimi yok |
| 7 | Tek onaylanmış plan kuralı mı olmalı? | Evet. Bir storyboard için aynı anda yalnızca bir plan APPROVED olabilir |
| 8 | Image generation maliyeti takip edilmeli mi? | İlk fazda hayır. Gerçek image generation yok, maliyet yok |
| 9 | İç AI prompt/provider payload görünür olmalı mı? | Hayır. Yalnızca ürün çıktısı olan image prompt görünür |
| 10 | Görsel prompt çıktısı daha sonra export edilebilir olmalı mı? | Evet. FP-005 export altyapısının genişletilmesiyle gelecekte |
| 11 | Aynı storyboard'dan birden fazla görsel plan versiyonu üretilebilir mi? | Evet. Regenerate → yeni version, ama aynı anda yalnızca bir APPROVED |
| 12 | Sahne bazlı onay/red mümkün mü? | İlk fazda hayır. Tüm plan tek birim olarak onaylanır/reddedilir |
| 13 | SceneVisualPlan sıralaması storyboard scene sırasını takip mi etmeli? | Evet. sceneNumber ile eşleşmeli, aynı sırada |

---

## 21. Document History

| Versiyon | Tarih | Durum | Açıklama |
|----------|-------|-------|----------|
| 1.0 | 2026-07-04 | Design / Planning | İlk FP-006 ürün tasarım dokümanı oluşturuldu |

---

> **Doküman Kuralları:**
> - İç AI orchestration prompt, provider request/response, API key veya secret bu dokümanda yer almaz.
> - Bu doküman yalnızca ürün tasarım kararlarını içerir.
> - Kod implementasyonu bu dokümanın kapsamı dışındadır.
> - Mevcut doküman numaraları değiştirilmez.
> - Üretilen image prompt metni ürün çıktısıdır ve gösterilebilir.
> - İç AI system prompt'u ile ürün image prompt çıktısı ayrı kavramlardır.
