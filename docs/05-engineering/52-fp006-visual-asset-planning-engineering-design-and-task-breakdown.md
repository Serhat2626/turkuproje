# 52 — FP-006 Visual Asset Planning — Engineering Design and Task Breakdown

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-006 — Visual Asset Planning & Image Generation Handoff
>
> **Prerequisites**: FP-001 (CLOSED), FP-002 (CLOSED), FP-003 (CLOSED), FP-004 (CLOSED), FP-005 (CLOSED)
>
> **Product Design**: [50-fp006-visual-asset-planning-image-generation-handoff-product-design.md](./50-fp006-visual-asset-planning-image-generation-handoff-product-design.md)
>
> **AI Platform Contract**: [51-fp006-ai-platform-contract-design.md](./51-fp006-ai-platform-contract-design.md)

---

## 1. Status

| Özellik | Değer |
|---------|-------|
| **Feature** | FP-006 — Visual Asset Planning & Image Generation Handoff |
| **Task** | 03 — Engineering Design & Task Breakdown |
| **Type** | Engineering Design / Task Breakdown |
| **Status** | Design / Planning |

Bu doküman, FP-006 Product Design (doc 50) ve AI Platform Contract (doc 51)
kararlarını implementasyon görevlerine dönüştüren mühendislik tasarımıdır.
İmplementasyon henüz başlamadı.

---

## 2. Purpose

Bu doküman aşağıdaki görevleri yerine getirir:

1. Product Design (doc 50) ve AI Platform Contract (doc 51) kararlarını teknik mimariye çevirir
2. Yeni backend entity'leri, migration, service ve REST API tasarımını tanımlar
3. AI gateway mühendislik yönünü tanımlar
4. Frontend/admin UI entegrasyonunu tanımlar
5. Validation, error handling ve güvenlik kurallarını belirler
6. Test stratejisini tanımlar
7. İmplementasyon görevlerini sıralı task'lara ayırır

**Kritik Not — Metin Prompt Planlaması, Görsel Üretimi Değil:**

FP-006 **görsel planlama metni ve image-generation-ready prompt** üreten bir
feature'dır. Gerçek görsel üretimi (DALL-E, Midjourney, Stable Diffusion),
MediaAsset/GeneratedAsset kaydı oluşturma, dosya depolama, video/ses üretimi
veya publishing FP-006 kapsamında değildir.

---

## 3. Architecture Overview

### 3.1. High-Level Backend Flow

```
APPROVED StoryboardDraft (FP-003 DB)
  → StoryboardDraft + StoryboardScene[] (mevcut — FP-003)
    → VisualAssetPlanGenerationService (YENİ)
      → AiVisualAssetAgentGateway (YENİ — provider-agnostic)
        ├── MockAiVisualAssetAgentGateway (YENİ — deterministic)
        └── OpenAiVisualAssetAgentGateway (YENİ — chat completion only)
      → VisualAssetPlan aggregate (YENİ — DB persist)
        ├── SceneVisualPlan[] (child — per scene)
        └── ThumbnailPlan (child — thumbnail)
    → VisualAssetPlanReviewService (YENİ)
      → edit / approve / reject
    → VisualAssetPlanQueryService (YENİ)
      → list / detail
  → Admin REST Controller (YENİ — admin endpoints)
    → HTTP Response (JSON body)
  → Admin UI (YENİ — visual plan workflow section)
```

### 3.2. Architecture Kararları

| # | Karar | Gerekçe |
|---|-------|---------| 
| 1 | **Metin prompt planlaması only** | Gerçek görsel üretimi yok |
| 2 | **Yeni AI gateway** | Mevcut gateway'lerden bağımsız, aynı pattern |
| 3 | **Yeni entity'ler** | Review lifecycle gerekli, persistence gerekli |
| 4 | **Yeni migration** | 3 yeni tablo |
| 5 | **Mevcut storyboard altyapısını reuse** | Kaynak storyboard/scene query |
| 6 | **FP-004/FP-005'ten bağımsız** | Parallel branch, hard dependency yok |
| 7 | **MediaAsset/GeneratedAsset kullanılmaz** | Gelecek faz |

### 3.3. FP-003/FP-004 ile Karşılaştırma

| Özellik | FP-003 | FP-004 | FP-006 |
|---------|--------|--------|--------|
| AI çağrısı | ✅ Storyboard gen | ✅ Package gen | ✅ Visual plan gen |
| AI tipi | Chat completion | Chat completion | Chat completion |
| Image API | ❌ | ❌ | ❌ |
| Yeni entity | ✅ StoryboardDraft/Scene | ✅ PlatformPackageDraft/SceneUsage | ✅ VisualAssetPlan/SceneVisualPlan/ThumbnailPlan |
| Migration | ✅ | ✅ | ✅ |
| Status lifecycle | ✅ | ✅ | ✅ |
| Human review | ✅ | ✅ | ✅ |
| Side effects | DB write | DB write | DB write |
| Media files | ❌ | ❌ | ❌ |
| Source | APPROVED ScenarioDraft | APPROVED StoryboardDraft | APPROVED StoryboardDraft |

---

## 4. Source Model

### 4.1. Source Aggregate: StoryboardDraft

FP-006'nın kaynağı FP-003'te tanımlanan mevcut `StoryboardDraft` entity'sidir.

**Gerekli source status:** APPROVED

**Kullanılacak storyboard alanları:**

| Alan | Kullanım |
|------|----------|
| id | Kaynak referans |
| contentProject | İçerik projesi referansı |
| title | Plan başlığı context |
| summary | Genel özet context |
| visualStyleGuide | Doğrudan stil rehberi input |
| targetPlatform | Aspect ratio kararı |
| aspectRatio | Varsayılan ratio |
| narrationStyle | Mood/stil etkisi |
| scenes | Sahne listesi — per-scene plan kaynağı |

### 4.2. Source Child: StoryboardScene

Her sahne için `SceneVisualPlan` üretimi.

**Kullanılacak scene alanları:**

| Alan | Öncelik | Kullanım |
|------|---------|----------|
| id | Kritik | sourceStoryboardScene referansı |
| sceneNumber | Kritik | Sıralama ve eşleştirme |
| visualDescription | 🔴 Kritik | Birincil AI input |
| sceneTitle | Yüksek | Context |
| mood | Yüksek | Atmosfer/renk paleti |
| location | Yüksek | Mekan tanımı |
| characters | Yüksek | Karakter tanımı |
| cameraDirection | Orta | Kompozisyon etkisi |
| narrationText | Orta | Görsel-anlatı uyumu |
| onScreenText | Orta | Overlay yönlendirmesi |
| sourceBeatType | Orta | Duygusal ağırlık |
| mediaGenerationNotes | Orta | Ek yönlendirme |
| estimatedDurationSeconds | Düşük | Dinamizm bilgisi |
| transitionNote | Düşük | Sahne geçiş bağlamı |

### 4.3. Mevcut Altyapı Reuse

| Bileşen | Package | Reuse |
|---------|---------|-------|
| `StoryboardDraftRepository` | `storyboard.repository` | Storyboard sorgulama |
| `StoryboardDraft` entity | `storyboard.entity` | Source data |
| `StoryboardScene` entity | `storyboard.entity` | Per-scene source data |
| `BaseEntity` | `common.entity` | ID, createdAt, updatedAt |
| `AiGatewayConfig` | `ai.config` | Gateway conditional wiring |

### 4.4. PlatformPackageDraft Bağımlılığı

**İlk faz:** Yok. FP-006, FP-004/FP-005'ten bağımsızdır.

**Gelecek faz:** Platform-spesifik görsel adaptasyonlar (16:9 → 9:16) için
`PlatformPackageSceneUsage` referansı eklenebilir.

---

## 5. Domain Model Design

### 5.1. VisualAssetPlan Entity

Bir storyboard'un tüm sahneleri için üst düzey görsel varlık planı.

**Table:** `visual_asset_plans`

**Extends:** `BaseEntity` (id UUID, createdAt, updatedAt — application-side UUID)

| Kategori | Alan | Tip | Nullable | Açıklama |
|----------|------|-----|----------|----------|
| **İlişkiler** | contentProject | → ContentProject | NOT NULL | FK |
| | sourceStoryboardDraft | → StoryboardDraft | NOT NULL | FK |
| **Versiyon** | version | Integer | NOT NULL | Plan versiyonu |
| **Status** | status | VisualAssetPlanStatus | NOT NULL | Enum STRING |
| **Genel Stil** | title | String(500) | NULL | Plan başlığı |
| | summary | Text | NULL | Genel özet |
| | overallVisualStyle | Text | NULL | Bütünsel görsel stil |
| | aspectRatioStrategy | String(100) | NULL | Aspect ratio stratejisi |
| | culturalAuthenticityGuidance | Text | NULL | Kültürel doğruluk rehberliği |
| | characterContinuityGuidance | Text | NULL | Karakter süreklilik notları |
| | locationContinuityGuidance | Text | NULL | Mekan süreklilik notları |
| | colorPaletteGuidance | Text | NULL | Renk paleti rehberliği |
| **AI Metadata** | generatedBy | GeneratedBy | NOT NULL | Enum STRING |
| | providerType | String(50) | NULL | Safe metadata |
| | modelName | String(100) | NULL | Safe metadata |
| **Review** | reviewNotes | Text | NULL | Admin review notları |
| | editedAt | LocalDateTime | NULL | |
| | editedBy | String(100) | NULL | |
| | approvedAt | LocalDateTime | NULL | |
| | approvedBy | String(100) | NULL | |
| | rejectedAt | LocalDateTime | NULL | |
| | rejectedBy | String(100) | NULL | |
| | rejectionReason | Text | NULL | Red gerekçesi |
| **Children** | scenePlans | OneToMany | — | cascade ALL, orphanRemoval, OrderBy sceneNumber ASC |
| | thumbnailPlans | OneToMany | — | cascade ALL, orphanRemoval |

### 5.2. SceneVisualPlan Entity

Her storyboard sahnesi için detaylı görsel plan.

**Table:** `scene_visual_plans`

**Extends:** `BaseEntity`

| Kategori | Alan | Tip | Nullable | Açıklama |
|----------|------|-----|----------|----------|
| **İlişkiler** | visualAssetPlan | → VisualAssetPlan | NOT NULL | FK, parent |
| | sourceStoryboardScene | → StoryboardScene | NULL | FK, kaynak sahne |
| **Sahne Ref** | sceneNumber | Integer | NOT NULL | Sahne numarası (1-based) |
| | sourceBeatType | String(30) | NULL | Beat tipi |
| | sceneTitle | String(500) | NULL | Sahne başlığı |
| **Prompt** | englishImagePrompt | Text | NOT NULL | 🔴 İngilizce image prompt — ürün çıktısı |
| | turkishVisualContext | Text | NOT NULL | 🔴 Türkçe görsel bağlam — ürün çıktısı |
| | negativePrompt | Text | NULL | Negatif prompt |
| **Stil** | styleDirective | String(200) | NULL | Görsel stil |
| | aspectRatio | String(20) | NULL | Aspect ratio |
| **Kompozisyon** | compositionNotes | Text | NULL | Çerçeveleme notları |
| | cameraFraming | String(200) | NULL | Kamera açısı |
| | lightingNotes | Text | NULL | Aydınlatma |
| **Mood / Renk** | colorPalette | Text | NULL | Renk paleti |
| | mood | String(200) | NULL | Atmosfer/duygu |
| **İçerik** | location | Text | NULL | Mekan tanımı |
| | characters | Text | NULL | Karakter tanımı |
| | culturalDetails | Text | NULL | Kültürel detaylar |
| **Overlay** | textOverlayGuidance | Text | NULL | Overlay yönlendirmesi |
| **Tool Notes** | imageGenerationToolNotes | Text | NULL | Image gen tool notları |
| | safetyNotes | Text | NULL | İçerik güvenliği notları |

### 5.3. ThumbnailPlan Entity

Thumbnail için görsel plan.

**Table:** `thumbnail_plans`

**Extends:** `BaseEntity`

| Kategori | Alan | Tip | Nullable | Açıklama |
|----------|------|-----|----------|----------|
| **İlişkiler** | visualAssetPlan | → VisualAssetPlan | NOT NULL | FK, parent |
| | sourceStoryboardScene | → StoryboardScene | NULL | FK, referans sahne (opsiyonel) |
| **Referans** | sourceSceneNumber | Integer | NULL | Referans sahne numarası |
| **Prompt** | thumbnailPrompt | Text | NOT NULL | 🔴 Thumbnail prompt — ürün çıktısı |
| | negativePrompt | Text | NULL | Negatif prompt |
| **Kompozisyon** | textOverlayGuidance | Text | NULL | Metin yerleşimi rehberliği |
| | compositionNotes | Text | NULL | Kompozisyon notları |
| | emotionalHook | String(500) | NULL | Duygusal çekicilik |
| **Stil** | aspectRatio | String(20) | NULL | Aspect ratio |
| | styleDirective | String(200) | NULL | Stil yönlendirmesi |
| | colorPalette | Text | NULL | Renk paleti |

### 5.4. Entity Tasarım Kararları

| # | Karar | Gerekçe |
|---|-------|---------| 
| 1 | Entity'ler `BaseEntity` extend eder | Proje standardı: UUID id, createdAt, updatedAt |
| 2 | UUID application/JPA-side üretilir | PostgreSQL 12.22 uyumluluk — `gen_random_uuid()` kullanılmaz |
| 3 | Image URL alanı yok | İlk fazda görsel dosya üretilmez |
| 4 | MediaAsset referansı yok | İlk fazda media entity kullanılmaz |
| 5 | GeneratedAsset referansı yok | İlk fazda media entity kullanılmaz |
| 6 | Prompt alanları Text tipinde | Image prompt'lar uzun olabilir |
| 7 | cascade ALL + orphanRemoval | Parent silindiğinde children da silinir |
| 8 | SceneVisualPlan OrderBy sceneNumber ASC | Storyboard sahne sıralamasını korur |

---

## 6. Status Model

### 6.1. VisualAssetPlanStatus Enum

```
VisualAssetPlanStatus
├── AI_GENERATED     — AI tarafından üretildi, henüz incelenmedi
├── NEEDS_REVIEW     — Admin tarafından düzenlendi, tekrar inceleme gerekiyor
├── APPROVED         — Admin tarafından açıkça onaylandı
├── REJECTED         — Admin tarafından açıkça reddedildi
└── FAILED           — AI üretimi başarısız oldu
```

### 6.2. Status Flow

```
Generate → AI_GENERATED
               │
     ┌─────────┼──────────┐
     │         │          │
  (edit)   (approve)  (reject)
     │         │          │
     ▼         ▼          ▼
NEEDS_REVIEW  APPROVED  REJECTED
     │                    │
  (approve/reject)     (edit)
     │                    │
     ├──→ APPROVED        │
     └──→ REJECTED        │
                          ▼
                     NEEDS_REVIEW

Generate failure → FAILED (terminal — yeni üretim gerekir)
```

### 6.3. Lifecycle Rules

| # | Kural |
|---|-------|
| 1 | AI üretimi → AI_GENERATED (asla APPROVED) |
| 2 | Admin edit → NEEDS_REVIEW |
| 3 | Admin approve → APPROVED (explicit action) |
| 4 | Admin reject → REJECTED (explicit action, reason required) |
| 5 | AI hatası → FAILED |
| 6 | APPROVED plan düzenlenebilir → NEEDS_REVIEW'a döner |
| 7 | REJECTED plan düzenlenebilir → NEEDS_REVIEW'a döner |
| 8 | FAILED plan düzenlenemez → yeni üretim gerekir |
| 9 | Generic status update endpoint yok |
| 10 | READY_FOR_PUBLISH yok |
| 11 | Bir storyboard için aynı anda yalnızca bir APPROVED plan |

---

## 7. Database / Migration Design

### 7.1. Migration Numarası

Mevcut son migration: V7 (FP-004). FP-006 migration: **V8**.

### 7.2. Tablo Tasarımı (Kavramsal)

#### A. visual_asset_plans

| Kolon | Tip | Constraint | Açıklama |
|-------|-----|-----------|----------|
| id | UUID | PK | Application-side UUID |
| content_project_id | UUID | FK NOT NULL | → content_projects |
| source_storyboard_draft_id | UUID | FK NOT NULL | → storyboard_drafts |
| version | INTEGER | NOT NULL | Plan versiyonu |
| status | VARCHAR(20) | NOT NULL | Enum STRING |
| title | VARCHAR(500) | NULL | |
| summary | TEXT | NULL | |
| overall_visual_style | TEXT | NULL | |
| aspect_ratio_strategy | VARCHAR(100) | NULL | |
| cultural_authenticity_guidance | TEXT | NULL | |
| character_continuity_guidance | TEXT | NULL | |
| location_continuity_guidance | TEXT | NULL | |
| color_palette_guidance | TEXT | NULL | |
| generated_by | VARCHAR(30) | NOT NULL | Enum STRING |
| provider_type | VARCHAR(50) | NULL | |
| model_name | VARCHAR(100) | NULL | |
| review_notes | TEXT | NULL | |
| edited_at | TIMESTAMP | NULL | |
| edited_by | VARCHAR(100) | NULL | |
| approved_at | TIMESTAMP | NULL | |
| approved_by | VARCHAR(100) | NULL | |
| rejected_at | TIMESTAMP | NULL | |
| rejected_by | VARCHAR(100) | NULL | |
| rejection_reason | TEXT | NULL | |
| created_at | TIMESTAMP | NOT NULL | BaseEntity |
| updated_at | TIMESTAMP | NOT NULL | BaseEntity |

#### B. scene_visual_plans

| Kolon | Tip | Constraint | Açıklama |
|-------|-----|-----------|----------|
| id | UUID | PK | Application-side UUID |
| visual_asset_plan_id | UUID | FK NOT NULL | → visual_asset_plans |
| source_storyboard_scene_id | UUID | FK NULL | → storyboard_scenes |
| scene_number | INTEGER | NOT NULL | |
| source_beat_type | VARCHAR(30) | NULL | |
| scene_title | VARCHAR(500) | NULL | |
| english_image_prompt | TEXT | NOT NULL | |
| turkish_visual_context | TEXT | NOT NULL | |
| negative_prompt | TEXT | NULL | |
| style_directive | VARCHAR(200) | NULL | |
| aspect_ratio | VARCHAR(20) | NULL | |
| composition_notes | TEXT | NULL | |
| camera_framing | VARCHAR(200) | NULL | |
| lighting_notes | TEXT | NULL | |
| color_palette | TEXT | NULL | |
| mood | VARCHAR(200) | NULL | |
| location | TEXT | NULL | |
| characters | TEXT | NULL | |
| cultural_details | TEXT | NULL | |
| text_overlay_guidance | TEXT | NULL | |
| image_generation_tool_notes | TEXT | NULL | |
| safety_notes | TEXT | NULL | |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

#### C. thumbnail_plans

| Kolon | Tip | Constraint | Açıklama |
|-------|-----|-----------|----------|
| id | UUID | PK | Application-side UUID |
| visual_asset_plan_id | UUID | FK NOT NULL | → visual_asset_plans |
| source_storyboard_scene_id | UUID | FK NULL | → storyboard_scenes |
| source_scene_number | INTEGER | NULL | |
| thumbnail_prompt | TEXT | NOT NULL | |
| negative_prompt | TEXT | NULL | |
| text_overlay_guidance | TEXT | NULL | |
| composition_notes | TEXT | NULL | |
| emotional_hook | VARCHAR(500) | NULL | |
| aspect_ratio | VARCHAR(20) | NULL | |
| style_directive | VARCHAR(200) | NULL | |
| color_palette | TEXT | NULL | |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

### 7.3. PostgreSQL Uyumluluk Notları

| # | Not |
|---|-----|
| 1 | Staging PostgreSQL 12.22 — `gen_random_uuid()` extension'a bağlı olabilir |
| 2 | UUID default'ları DB-side KULLANILMAZ — application/JPA-side `@GeneratedValue` + `UUID.randomUUID()` |
| 3 | Bu yaklaşım mevcut `BaseEntity` stabilizasyonu ile tutarlıdır |
| 4 | Partial unique index (one-approved-per-storyboard) PostgreSQL 12'de desteklenir ancak ilk fazda service-level enforcement yeterlidir |
| 5 | Migration formatı: Flyway V8__*.sql |

### 7.4. Cascade ve Orphan Removal

| Parent | Child | Cascade | OrphanRemoval |
|--------|-------|---------|---------------|
| VisualAssetPlan | SceneVisualPlan | ALL | true |
| VisualAssetPlan | ThumbnailPlan | ALL | true |

### 7.5. One-Approved-Per-Storyboard Kuralı

| Yaklaşım | İlk Faz | Gelecek |
|----------|---------|---------|
| Service-level enforcement | ✅ Kullanılır | Devam eder |
| DB partial unique index | ❌ İlk fazda yok | PostgreSQL uyumluluk doğrulandıktan sonra opsiyonel |

Service-level rule: `VisualAssetPlanReviewService.approve()` çağrıldığında aynı
storyboard için mevcut APPROVED planı NEEDS_REVIEW'a düşürür veya reddeder.

---

## 8. AI Gateway Engineering Design

### 8.1. Gateway Interface

```
AiVisualAssetAgentGateway
  └── generateVisualAssetPlan(AiVisualAssetPlanRequest) → AiVisualAssetPlanResponse
```

**Package:** `com.anadoluatlasi.backend.ai.gateway`

### 8.2. Contract DTOs

**Package:** `com.anadoluatlasi.backend.ai.contract`

| DTO | Tip | Açıklama |
|-----|-----|----------|
| `AiVisualAssetPlanRequest` | Request | Ana istek: storyboard context + scenes + policies |
| `AiVisualSceneInput` | Request child | Per-scene input: visualDescription, mood, location, etc. |
| `AiVisualThumbnailInput` | Request child | Thumbnail context: title, hook, tone |
| `AiVisualAssetPlanResponse` | Response | Ana yanıt: success, output, metadata, error |
| `AiVisualAssetPlanOutput` | Response child | Başarılı çıktı: stil, scenePlans, thumbnailPlan |
| `AiSceneVisualPlanOutput` | Response child | Per-scene çıktı: prompts, style, composition |
| `AiThumbnailVisualPlanOutput` | Response child | Thumbnail çıktısı: prompt, composition |

DTO detayları doc 51'de (§5–§6) tanımlıdır. Burada tekrar edilmez.

### 8.3. Mock Provider

**Package:** `com.anadoluatlasi.backend.ai.gateway.mock`

| Davranış | Değer |
|----------|-------|
| providerType | "MOCK" |
| modelName | "mock-visual-asset-v1" |
| Deterministic | ✅ Aynı input → aynı output |
| External IO | ❌ Yok |
| Scene count | Input scene count ile eşleşir |
| Thumbnail | Her zaman üretilir |
| success | Her zaman `true` (hata simülasyonu ayrı) |

### 8.4. OpenAI Provider

**Package:** `com.anadoluatlasi.backend.ai.gateway.openai`

| Özellik | Değer |
|---------|-------|
| API endpoint | Chat completion (`/v1/chat/completions`) |
| **KULLANILMAZ** | Image generation (`/v1/images/generations`) |
| Model | `gpt-4o-mini` (varsayılan, runtime config ile değiştirilebilir) |
| Response format | Structured JSON |
| Raw prompt/response | Expose edilmez |
| Error handling | Sanitize edilmiş errorMessage |

### 8.5. Gateway Kuralları

| # | Kural |
|---|-------|
| 1 | Chat/text completion only — image API çağrılmaz |
| 2 | Provider SDK import'ları interface'e sızmaz |
| 3 | Exception fırlatılmaz — response DTO döndürülür |
| 4 | Raw prompt/response expose edilmez |
| 5 | API key/secret contract DTO'larında bulunmaz |
| 6 | Image URL döndürülmez |

---

## 9. Backend Service Design

### 9.1. VisualAssetPlanGenerationService

**Package:** `com.anadoluatlasi.backend.visualasset.service`

**Sorumluluklar:**

1. Source storyboard ID ile `StoryboardDraft` yükle
2. APPROVED status kontrolü — değilse hata
3. Scenes yükle (ordered by sceneNumber)
4. Scenes boşsa hata
5. Storyboard + scenes → `AiVisualAssetPlanRequest` map
6. `AiVisualAssetAgentGateway.generateVisualAssetPlan()` çağır
7. AI response başarılı mı kontrol et
8. Başarılı: response → `VisualAssetPlan` + `SceneVisualPlan[]` + `ThumbnailPlan` persist (status: AI_GENERATED)
9. Başarısız: safe error döndür veya FAILED plan kaydet
10. Asla auto-approve

**Generation Failure Kararı:**

| Seçenek | Yaklaşım |
|---------|----------|
| **Önerilen** | AI hatası → safe error response dön, FAILED plan oluşturma |
| Alternatif | AI hatası → minimal FAILED plan kaydet (title + error metadata) |

Önerilen yaklaşım gerekçesi: FAILED plan kaydı gereksiz veri oluşturur ve
admin'e temizlik yükü getirir. Hata admin'e gösterilir, admin tekrar dener.

### 9.2. VisualAssetPlanReviewService

**Package:** `com.anadoluatlasi.backend.visualasset.service`

**Sorumluluklar:**

| İşlem | Davranış |
|-------|----------|
| Edit plan fields | Status → NEEDS_REVIEW; parent stil/guidance alanları güncellenir |
| Edit scene prompt | Status → NEEDS_REVIEW; SceneVisualPlan alanları güncellenir |
| Edit thumbnail prompt | Status → NEEDS_REVIEW; ThumbnailPlan alanları güncellenir |
| Approve | Status → APPROVED; approvedAt/approvedBy kaydedilir; aynı storyboard için eski APPROVED revoke |
| Reject | Status → REJECTED; rejectedAt/rejectedBy/rejectionReason kaydedilir |

**One-Approved-Per-Storyboard kuralı:**

Approve çağrıldığında:
1. Aynı `sourceStoryboardDraft` için mevcut APPROVED plan var mı kontrol et
2. Varsa: eski planın statüsünü değiştirme kararı
   - **Önerilen:** Eski APPROVED planı dokunma, yeni onay reddet (veya eski planı NEEDS_REVIEW'a düşür)
   - İlk fazda: Aynı storyboard'da ikinci onay, eski APPROVED'ı otomatik NEEDS_REVIEW'a düşürür
3. Bu kural service-level'da enforce edilir

### 9.3. VisualAssetPlanQueryService

**Package:** `com.anadoluatlasi.backend.visualasset.service`

**Sorumluluklar:**

| İşlem | Açıklama |
|-------|----------|
| List by storyboard | Belirli storyboard'a ait tüm görsel planları listele |
| List by content project | Belirli projeye ait tüm görsel planları listele |
| Detail | Tek plan detayı — scenePlans + thumbnailPlan dahil |
| Load scene plans | sceneNumber ASC sıralı |
| Load thumbnail plan | Parent plan ile birlikte |

---

## 10. REST API Design

### 10.1. Proposed Admin Endpoints

| # | Operation | Method | Path |
|---|-----------|--------|------|
| 1 | Generate visual plan | POST | `/api/admin/storyboard-drafts/{storyboardId}/visual-asset-plans/generate` |
| 2 | List visual plans | GET | `/api/admin/storyboard-drafts/{storyboardId}/visual-asset-plans` |
| 3 | Visual plan detail | GET | `/api/admin/visual-asset-plans/{planId}` |
| 4 | Edit visual plan | PUT | `/api/admin/visual-asset-plans/{planId}` |
| 5 | Edit scene visual plan | PUT | `/api/admin/scene-visual-plans/{sceneplanId}` |
| 6 | Edit thumbnail plan | PUT | `/api/admin/thumbnail-plans/{thumbnailId}` |
| 7 | Approve visual plan | POST | `/api/admin/visual-asset-plans/{planId}/approve` |
| 8 | Reject visual plan | POST | `/api/admin/visual-asset-plans/{planId}/reject` |

### 10.2. API Kuralları

| # | Kural |
|---|-------|
| 1 | Generate: source storyboard APPROVED olmalı (409 değilse) |
| 2 | Approve/reject: explicit dedicated endpoints |
| 3 | Edit: status → NEEDS_REVIEW |
| 4 | Generic status update endpoint yok |
| 5 | Image generation endpoint yok |
| 6 | Media upload endpoint yok |
| 7 | Publishing endpoint yok |
| 8 | Response'da raw prompt/provider payload yok |
| 9 | Response'da API key/secret yok |
| 10 | Controller thin — service'e delegate |

---

## 11. DTO Design

### 11.1. REST Request DTOs

| DTO | Kullanım | Alanlar (kavramsal) |
|-----|----------|---------------------|
| `GenerateVisualAssetPlanRequest` | POST generate | (body boş olabilir — storyboardId path'ten) |
| `UpdateVisualAssetPlanRequest` | PUT edit plan | title, summary, overallVisualStyle, colorPaletteGuidance, reviewNotes, etc. |
| `UpdateSceneVisualPlanRequest` | PUT edit scene | englishImagePrompt, turkishVisualContext, negativePrompt, styleDirective, etc. |
| `UpdateThumbnailPlanRequest` | PUT edit thumbnail | thumbnailPrompt, negativePrompt, compositionNotes, etc. |
| `RejectVisualAssetPlanRequest` | POST reject | rejectionReason |

### 11.2. REST Response DTOs

| DTO | Kullanım | Alanlar (kavramsal) |
|-----|----------|---------------------|
| `VisualAssetPlanResponse` | Plan detay/list | id, version, status, title, overallVisualStyle, scenePlans[], thumbnailPlan, metadata |
| `SceneVisualPlanResponse` | Scene plan | id, sceneNumber, sceneTitle, englishImagePrompt, turkishVisualContext, negativePrompt, style, composition |
| `ThumbnailPlanResponse` | Thumbnail | id, thumbnailPrompt, negativePrompt, compositionNotes, emotionalHook |

### 11.3. Forbidden Fields

Response DTO'larında şu alanlar **kesinlikle** bulunmaz:

| Yasak Alan | Gerekçe |
|-----------|---------|
| Internal orchestration prompt | İç AI system prompt'u |
| Raw provider request | Provider SDK detayı |
| Raw provider response | Provider SDK detayı |
| Provider payload | Provider SDK detayı |
| API key | Güvenlik |
| Secret | Güvenlik |
| Stack trace | Güvenlik |
| Image URL | İlk fazda görsel üretilmez |
| MediaAsset ID | İlk fazda media entity kullanılmaz |

---

## 12. Admin UI Design

### 12.1. Visual Asset Plan Section

Mevcut ContentProject detay sayfasına veya StoryboardDraft detay sayfasına
yeni görsel plan workflow bölümü eklenir.

### 12.2. UI Bileşenleri (Planlanan)

| Bileşen | Açıklama |
|---------|----------|
| Generate button | APPROVED storyboard'da "Görsel Plan Üret" butonu |
| Plan list | Storyboard'a ait görsel plan versiyonları listesi |
| Plan detail | Genel stil, süreklilik notları, renk paleti |
| Scene prompt cards | Her sahne için image prompt, TR context, negative prompt, style |
| Thumbnail prompt card | Thumbnail prompt, composition, hook |
| Edit form | Prompt/stil/kompozisyon alanlarını düzenleme |
| Approve button | Explicit onay butonu |
| Reject button | Explicit red butonu (gerekçe ile) |
| Status badges | AI_GENERATED, NEEDS_REVIEW, APPROVED, REJECTED, FAILED |

### 12.3. Visibility Rules

| Storyboard Status | Generate Button | Plan Actions |
|-------------------|----------------|-------------|
| APPROVED | ✅ Visible + Active | ✅ Active |
| AI_GENERATED | ❌ Hidden | ❌ N/A |
| NEEDS_REVIEW | ❌ Hidden | ❌ N/A |
| REJECTED | ❌ Hidden | ❌ N/A |
| FAILED | ❌ Hidden | ❌ N/A |

### 12.4. UI'da Olmayanlar

| Yok | Gerekçe |
|-----|---------|
| Image preview (generated files) | İlk fazda görsel üretilmez |
| Upload button | Media upload yok |
| Publish button | Publishing yok |
| Schedule button | Scheduling yok |
| READY_FOR_PUBLISH action | Status yok |
| Generic status dropdown | Explicit approve/reject butonları |

---

## 13. Validation Rules

### 13.1. Pre-Generation

| # | Kural | Hata |
|---|-------|------|
| 1 | Storyboard ID ile kayıt bulunmalı | 404 |
| 2 | Storyboard status = APPROVED | 409 |
| 3 | Storyboard en az 1 sahne içermeli | 400 |

### 13.2. AI Response Validation

| # | Kural | Davranış |
|---|-------|----------|
| 4 | `success = true` ise `output` dolu olmalı | Failure olarak işle |
| 5 | Scene plan count = input scene count | Log uyarı, kabul veya reddet |
| 6 | Her scene plan `sourceStoryboardSceneId` veya `sceneNumber` ile referans | Eşleştirme hatası → log warning |
| 7 | Her scene plan `englishImagePrompt` dolu | Eksik → failure |
| 8 | Her scene plan `turkishVisualContext` dolu | Eksik → failure |
| 9 | Thumbnail plan `thumbnailPrompt` dolu | Eksik → warning, thumbnail plansız devam |

### 13.3. Status Transition Validation

| # | Kural |
|---|-------|
| 10 | Edit: AI_GENERATED / NEEDS_REVIEW / APPROVED / REJECTED → NEEDS_REVIEW |
| 11 | Edit: FAILED → ❌ Reddedilir |
| 12 | Approve: AI_GENERATED / NEEDS_REVIEW → APPROVED |
| 13 | Approve: APPROVED / REJECTED / FAILED → ❌ Reddedilir |
| 14 | Reject: AI_GENERATED / NEEDS_REVIEW → REJECTED |
| 15 | Reject: APPROVED / REJECTED / FAILED → ❌ Reddedilir |

### 13.4. Integrity Rules

| # | Kural |
|---|-------|
| 16 | Bir storyboard için aynı anda yalnızca bir APPROVED plan (service-level) |
| 17 | MediaAsset/GeneratedAsset kaydı oluşturulmaz |
| 18 | Image URL persist edilmez |

---

## 14. Error Handling

### 14.1. Error Cases

| Hata | HTTP | Açıklama |
|------|------|----------|
| Storyboard bulunamadı | 404 | `NotFoundException` |
| Storyboard APPROVED değil | 409 | `StoryboardNotApprovedException` veya mevcut pattern |
| Storyboard sahnesiz | 400 | `BadRequestException` |
| Visual plan bulunamadı | 404 | `NotFoundException` |
| Geçersiz status transition | 409 | `InvalidStatusTransitionException` |
| One-approved ihlali | 409 | Service-level kontrol |
| AI provider hatası | — | Safe error response, plan kaydetme; admin retry |
| Invalid AI response | — | Safe error response, plan kaydetme; admin retry |

### 14.2. AI Provider Hata Yaklaşımı

**Önerilen:** AI hatası durumunda FAILED plan kaydetme, safe error response dön.

| Durum | Davranış |
|-------|----------|
| Provider timeout | Error response: retryable, admin retry |
| Provider rate limit | Error response: retryable, admin retry |
| Provider invalid output | Error response: retryable, admin retry |
| Provider safety refusal | Error response: non-retryable |
| Provider auth failure | Error response: non-retryable |

### 14.3. Global Error Handling

Mevcut `@ControllerAdvice` kullanılır. FP-006'ya özgü yeni exception sınıfları
mevcut pattern'i takip eder.

---

## 15. Testing Strategy

### 15.1. Backend Unit Tests

#### Entity / Migration Tests

| # | Test |
|---|------|
| 1 | V8 migration başarılı (tablolar oluştu) |
| 2 | VisualAssetPlan entity persist/load |
| 3 | SceneVisualPlan cascade persist |
| 4 | ThumbnailPlan cascade persist |
| 5 | Orphan removal çalışır |

#### AI Contract Tests

| # | Test |
|---|------|
| 6 | Request mapper: StoryboardDraft + scenes → AiVisualAssetPlanRequest |
| 7 | Response mapper: AiVisualAssetPlanOutput → VisualAssetPlan + children |
| 8 | Mock gateway: success response |
| 9 | Mock gateway: scene count = input count |
| 10 | Mock gateway: sceneNumber preserved |
| 11 | Mock gateway: thumbnail present |
| 12 | Mock gateway: deterministic output |

#### Generation Service Tests

| # | Test |
|---|------|
| 13 | APPROVED storyboard → visual plan generated (AI_GENERATED) |
| 14 | AI_GENERATED storyboard → rejected (409) |
| 15 | NEEDS_REVIEW storyboard → rejected (409) |
| 16 | REJECTED storyboard → rejected (409) |
| 17 | FAILED storyboard → rejected (409) |
| 18 | No-scene storyboard → rejected (400) |
| 19 | Non-existent storyboard → 404 |
| 20 | AI response failure → safe error, no plan persisted |
| 21 | Generation does not auto-approve |
| 22 | SceneVisualPlan count = StoryboardScene count |

#### Review Service Tests

| # | Test |
|---|------|
| 23 | Edit AI_GENERATED → NEEDS_REVIEW |
| 24 | Edit APPROVED → NEEDS_REVIEW |
| 25 | Edit REJECTED → NEEDS_REVIEW |
| 26 | Edit FAILED → rejected |
| 27 | Approve AI_GENERATED → APPROVED |
| 28 | Approve NEEDS_REVIEW → APPROVED |
| 29 | Approve APPROVED → rejected |
| 30 | Reject AI_GENERATED → REJECTED |
| 31 | Reject NEEDS_REVIEW → REJECTED |
| 32 | Reject APPROVED → rejected |
| 33 | One-approved-per-storyboard enforced |

#### Safety Tests

| # | Test |
|---|------|
| 34 | No rawPrompt in response |
| 35 | No providerPayload in response |
| 36 | No apiKey in response |
| 37 | No imageUrl in entity |
| 38 | No MediaAsset/GeneratedAsset created |

#### REST Controller Tests

| # | Test |
|---|------|
| 39 | POST generate → 200 + AI_GENERATED plan |
| 40 | POST generate non-approved → 409 |
| 41 | GET list → 200 |
| 42 | GET detail → 200 |
| 43 | PUT edit → 200 + NEEDS_REVIEW |
| 44 | POST approve → 200 + APPROVED |
| 45 | POST reject → 200 + REJECTED |
| 46 | GET non-existent → 404 |

### 15.2. Frontend Tests

| # | Test |
|---|------|
| 47 | APPROVED storyboard → generate button visible |
| 48 | Non-APPROVED storyboard → generate button hidden |
| 49 | Scene prompt cards render |
| 50 | Thumbnail prompt card renders |
| 51 | Edit form works |
| 52 | Approve button follows status |
| 53 | Reject button follows status |
| 54 | No image/publish/upload buttons |
| 55 | No unsafe fields rendered |

---

## 16. Implementation Task Breakdown

### Task 04 — Backend Domain Model & Migration

**Amaç:** Entity'ler, enum, repository'ler, migration ve temel persistence testleri.

**Deliverable'lar:**
- `VisualAssetPlanStatus` enum
- `VisualAssetPlan` entity
- `SceneVisualPlan` entity
- `ThumbnailPlan` entity
- `VisualAssetPlanRepository`
- `SceneVisualPlanRepository`
- `ThumbnailPlanRepository`
- V8 Flyway migration
- Entity persistence unit testleri

**Kabul kriterleri:**
- 3 tablo oluşturuldu (visual_asset_plans, scene_visual_plans, thumbnail_plans)
- Entity'ler BaseEntity extend ediyor
- UUID application-side (gen_random_uuid() yok)
- Cascade ALL + orphanRemoval
- Mevcut testler kırılmaz
- Migration başarılı

---

### Task 05 — AI Gateway DTO & Mock Provider

**Amaç:** Provider-agnostic DTO'lar, gateway interface ve deterministic mock.

**Deliverable'lar:**
- `AiVisualAssetPlanRequest` + `AiVisualSceneInput` + `AiVisualThumbnailInput`
- `AiVisualAssetPlanResponse` + `AiVisualAssetPlanOutput` + `AiSceneVisualPlanOutput` + `AiThumbnailVisualPlanOutput`
- `AiVisualAssetAgentGateway` interface
- `MockAiVisualAssetAgentGateway`
- `AiGatewayConfig` güncellemesi (conditional wiring)
- Mock gateway unit testleri

**Kabul kriterleri:**
- Mock gateway deterministic çıktı üretir
- Scene count = input scene count
- Thumbnail her zaman üretilir
- providerType = "MOCK"
- Image API çağrılmaz
- Mevcut testler kırılmaz

---

### Task 06 — Visual Asset Generation Service

**Amaç:** Source validation, request mapping, gateway call, response mapping, persistence.

**Deliverable'lar:**
- `VisualAssetPlanGenerationService`
- Request mapper (StoryboardDraft → AiVisualAssetPlanRequest)
- Response mapper (AiVisualAssetPlanOutput → VisualAssetPlan aggregate)
- Source validation (APPROVED-only)
- Generation unit testleri (happy path + all rejection cases)

**Kabul kriterleri:**
- APPROVED storyboard → AI_GENERATED plan
- Non-APPROVED → exception
- No-scene → exception
- AI failure → safe error, no plan persisted
- No auto-approve
- SceneVisualPlan count = StoryboardScene count
- Mevcut testler kırılmaz

---

### Task 07 — Review/Query Service

**Amaç:** List, detail, edit, approve, reject, one-approved rule.

**Deliverable'lar:**
- `VisualAssetPlanQueryService`
- `VisualAssetPlanReviewService`
- Edit → NEEDS_REVIEW
- Approve → APPROVED (one-per-storyboard rule)
- Reject → REJECTED (with reason)
- Service unit testleri

**Kabul kriterleri:**
- Edit AI_GENERATED/NEEDS_REVIEW/APPROVED/REJECTED → NEEDS_REVIEW
- Edit FAILED → rejected
- Approve AI_GENERATED/NEEDS_REVIEW → APPROVED
- Reject AI_GENERATED/NEEDS_REVIEW → REJECTED
- One-approved-per-storyboard enforced
- No generic status update
- Mevcut testler kırılmaz

---

### Task 08 — REST API

**Amaç:** Admin endpoint'leri ve controller testleri.

**Deliverable'lar:**
- `VisualAssetPlanController` (admin endpoints)
- Generate, list, detail, edit, approve, reject endpoints
- REST DTO'lar
- Controller unit testleri

**Kabul kriterleri:**
- 8 endpoint çalışıyor
- APPROVED-only generation enforcement
- Approve/reject explicit endpoints
- No generic status endpoint
- No raw prompt/provider payload in response
- Mevcut testler kırılmaz

---

### Task 09 — OpenAI Visual Asset Provider

**Amaç:** OpenAI chat completion provider (metin üretim only).

**Deliverable'lar:**
- `OpenAiVisualAssetAgentGateway`
- Chat completion integration (not image API)
- Structured JSON output parsing
- Error sanitization
- Mocked provider response testleri

**Kabul kriterleri:**
- Chat completion API kullanır
- Image generation API **kullanmaz**
- Raw prompt/response expose etmez
- Error sanitize edilir
- Structured output parse edilir
- Mevcut testler kırılmaz

---

### Task 10 — Admin UI Workflow

**Amaç:** Frontend görsel plan workflow bölümü.

**Deliverable'lar:**
- Visual asset plan section UI
- Generate button (APPROVED storyboard only)
- Plan list/history
- Plan detail (scene prompt cards + thumbnail card)
- Edit forms
- Approve/reject actions
- Status badges

**Kabul kriterleri:**
- APPROVED storyboard'da generate butonu görünür
- Non-APPROVED storyboard'da gizli
- Scene prompt cards render
- Thumbnail card renders
- Edit/approve/reject çalışır
- No image preview from generated files
- No publish/upload/schedule buttons

---

### Task 11 — Frontend Tests & Build

**Amaç:** Frontend test suite ve build doğrulaması.

**Deliverable'lar:**
- Visibility testleri (APPROVED vs non-APPROVED)
- Prompt card render testleri
- Edit form testleri
- Approve/reject button testleri
- No unsafe fields testleri
- `npm run build` başarılı

**Kabul kriterleri:**
- Tüm yeni frontend testleri geçiyor
- Mevcut frontend testleri kırılmamış
- TypeScript clean
- Build başarılı

---

### Task 12 — Local Smoke Test

**Amaç:** Tam akış yerel doğrulaması.

**Deliverable'lar:**
- Mock veri ile lokal generate/edit/approve/reject akışı
- Tüm testler (backend + frontend) geçer
- Build başarılı

**Kabul kriterleri:**
- APPROVED storyboard → visual plan generated (AI_GENERATED)
- Edit → NEEDS_REVIEW
- Approve → APPROVED
- Reject → REJECTED
- Non-APPROVED storyboard → rejected
- No image files created
- Backend + frontend tüm testler geçiyor

---

### Task 13 — Staging Validation

**Amaç:** Staging ortamında doğrulama.

**Deliverable'lar:**
- Staging deploy
- Migration V8 başarılı
- Generate/edit/approve/reject staging'de çalışır
- Error case doğrulaması
- FP-001–FP-005 regression kontrolü

**Kabul kriterleri:**
- Staging'de tüm operasyonlar çalışıyor
- Non-APPROVED generation reddediliyor
- FP-001–FP-005 kırılmamış
- No image generation
- No unsafe exposure

---

### Task 14 — FP-006 Closure

**Amaç:** Feature closure raporu ve final doğrulama.

**Deliverable'lar:**
- Backend test sayısı raporu
- Frontend test sayısı raporu
- Staging validation raporu
- Governance compliance raporu
- Closure raporu

**Kabul kriterleri:**
- Tüm testler geçiyor
- Staging doğrulanmış
- Governance kuralları karşılanmış
- FP-006 CLOSED

---

## 17. Security and Governance

### 17.1. Reconfirmed Rules

| # | Kural | Uygulama |
|---|-------|----------|
| 1 | İç AI orchestration prompt expose edilmez | Gateway implementation detayı |
| 2 | Raw provider request expose edilmez | Gateway implementation detayı |
| 3 | Raw provider response expose edilmez | Gateway implementation detayı |
| 4 | API key/secret expose edilmez | CI/CD variables |
| 5 | Image generation API çağrılmaz | Chat completion only |
| 6 | Media dosya üretilmez | Prompt planning only |
| 7 | MediaAsset/GeneratedAsset oluşturulmaz | Gelecek faz |
| 8 | Publishing/upload/schedule yapılmaz | FP-006'da yok |
| 9 | READY_FOR_PUBLISH yok | Status tanımlanmaz |
| 10 | Explicit human review zorunlu | AI_GENERATED → admin review → APPROVED |
| 11 | Auto-approval yok | AI asla APPROVED üretmez |
| 12 | Generic status update yok | Dedicated approve/reject endpoints |
| 13 | Image prompt ürün çıktısıdır | Gösterilir, düzenlenir, onaylanır |
| 14 | İç prompt ≠ ürün image prompt | Farklı kavramlar |

---

## 18. Future Follow-up

| # | Feature | Açıklama | Bağımlılık |
|---|---------|----------|------------|
| 1 | **FP-007: Actual Image Generation** | APPROVED visual plan → DALL-E/SD ile görsel üretimi | FP-006 |
| 2 | MediaAsset/GeneratedAsset integration | Üretilen görsellerin entity olarak saklanması | FP-007 |
| 3 | Generated image storage | Minio/S3 depolama | FP-007 |
| 4 | Thumbnail image creation | Gerçek thumbnail görsel dosyası | FP-007 |
| 5 | Audio/voiceover handoff | Seslendirme teslimi | FP-003 |
| 6 | Video assembly/render pipeline | Görsel + ses + metin birleştirme | FP-007 + Audio |
| 7 | Publishing workflow | YouTube/IG/FB yayın otomasyonu | FP-004/FP-005 + Media |
| 8 | Platform-specific visual adaptations | 16:9 → 9:16 crop/reframe | FP-006 + FP-004 |
| 9 | Visual plan export (FP-005 extension) | Onaylanmış görsel planların export'u | FP-005 + FP-006 |

---

## 19. Document History

| Versiyon | Tarih | Durum | Açıklama |
|----------|-------|-------|----------|
| 1.0 | 2026-07-04 | Design / Planning | İlk FP-006 engineering design ve task breakdown dokümanı oluşturuldu |

---

> **Doküman Kuralları:**
> - İç AI orchestration prompt, provider request/response, raw payload, API key veya secret bu dokümanda yer almaz.
> - Bu doküman yalnızca mühendislik tasarım kararlarını ve görev dağılımını içerir.
> - Kod implementasyonu bu dokümanın kapsamı dışındadır.
> - Mevcut doküman numaraları değiştirilmez.
> - Üretilen image prompt metni ürün çıktısıdır ve gösterilebilir.
> - Image generation API (DALL-E) bu tasarımda asla kullanılmaz.
> - PostgreSQL 12 uyumluluk: UUID application/JPA-side, gen_random_uuid() kullanılmaz.
