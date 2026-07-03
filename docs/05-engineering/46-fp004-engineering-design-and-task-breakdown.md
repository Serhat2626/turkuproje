# 46 — FP-004 Multi-Platform Content Packaging — Engineering Design and Task Breakdown

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-004 — Multi-Platform Content Packaging
>
> **Prerequisites**: FP-001 (CLOSED), FP-002 (CLOSED), FP-003 (CLOSED)
>
> **Product Design**: [44-fp004-multi-platform-content-packaging-product-design.md](./44-fp004-multi-platform-content-packaging-product-design.md)
>
> **AI Contract Design**: [45-fp004-ai-platform-contract-design.md](./45-fp004-ai-platform-contract-design.md)

---

## 1. Status

**Design / Planning**

Bu doküman FP-004'ün engineering tasarımı ve implementasyon görev dağılımıdır.
İmplementasyon henüz başlamadı.

---

## 2. Engineering Objective

Onaylanmış (APPROVED) StoryboardDraft'tan platform-spesifik içerik paketleri üretmek
için gereken tüm backend, AI gateway, REST API, admin UI ve test katmanlarını
tasarlamak ve implementasyon görevlerini tanımlamak.

**Akış:**

```
APPROVED StoryboardDraft
  → PlatformPackageGenerationService
    → AiPlatformPackageAgentGateway (mock/openai)
      → AI üretim
    → 4 PlatformPackageDraft kaydı (AI_GENERATED)
  → Admin review/edit
    → Explicit approve veya reject (per-platform)
  → APPROVED paketler downstream kullanıma hazır
```

---

## 3. Architecture Overview

### Katman Yapısı

```
┌─────────────────────────────────────────────────────┐
│  Admin UI (React)                                   │
│  Platform Packages sekmesi / list / detail / actions│
├─────────────────────────────────────────────────────┤
│  Admin REST Controller                              │
│  AdminPlatformPackageController                     │
│  REST DTO'ları (AI contract DTO'larından ayrı)      │
├─────────────────────────────────────────────────────┤
│  Application Services                               │
│  PlatformPackageGenerationService                   │
│  PlatformPackageReviewService                       │
│  PlatformPackageQueryService                        │
├─────────────────────────────────────────────────────┤
│  AI Gateway Interface                               │
│  AiPlatformPackageAgentGateway                      │
│  ├─ MockAiPlatformPackageAgentGateway               │
│  └─ OpenAiPlatformPackageAgentGateway               │
│     ├─ OpenAiPlatformPackagePromptBuilder            │
│     └─ OpenAiPlatformPackageResponseParser           │
├─────────────────────────────────────────────────────┤
│  Domain / Entity                                    │
│  PlatformPackageDraft                               │
│  PlatformPackageSceneUsage                          │
│  PlatformPackageType (enum)                         │
│  PlatformPackageStatus (enum)                       │
├─────────────────────────────────────────────────────┤
│  Repository                                         │
│  PlatformPackageDraftRepository                     │
│  PlatformPackageSceneUsageRepository (gerekirse)    │
├─────────────────────────────────────────────────────┤
│  Database (PostgreSQL)                              │
│  platform_package_drafts                            │
│  platform_package_scene_usages                      │
│  Flyway V7 migration                                │
└─────────────────────────────────────────────────────┘
```

### Tasarım İlkeleri

| # | İlke |
|---|------|
| 1 | Application service **interface'e** bağımlıdır — provider'a değil |
| 2 | AI contract DTO'ları **dahilidir** — REST'te expose edilmez |
| 3 | REST katmanı **ayrı safe DTO'lar** kullanır |
| 4 | FP-001/FP-002/FP-003 gateway'leri ve davranışları **değiştirilmez** |
| 5 | BaseEntity pattern kullanılır (id, createdAt, updatedAt) |
| 6 | `open-in-view=false` — lazy load service içinde yapılır |

---

## 4. Proposed Domain Model

### 4.1. PlatformPackageDraft (Ana Entity)

Tek bir platform için üretilmiş içerik paketi taslağı.

#### Alanlar

| Alan | Tip | Nullable | Açıklama |
|------|-----|----------|----------|
| *id* | UUID | NO | BaseEntity'den miras |
| *createdAt* | Timestamp | NO | BaseEntity'den miras |
| *updatedAt* | Timestamp | YES | BaseEntity'den miras |
| `contentProject` | ManyToOne → ContentProject | NO | Ait olduğu proje |
| `sourceStoryboardDraft` | ManyToOne → StoryboardDraft | NO | Kaynak storyboard |
| `packageType` | Enum (PlatformPackageType) | NO | Platform tipi |
| `status` | Enum (PlatformPackageStatus) | NO | Lifecycle status |
| `version` | Integer | NO | Versiyon (aynı proje+platform+storyboard) |
| `title` | String | NO | Paket başlığı |
| `captionOrDescription` | Text | NO | Platform açıklaması |
| `hashtagsJson` | Text (JSON) | YES | Hashtag listesi (JSON array string) |
| `callToAction` | String | YES | CTA metni |
| `targetDurationSeconds` | Integer | YES | Hedef süre |
| `aspectRatio` | String | YES | Aspect ratio (ör. "16:9") |
| `contentStructureNotes` | Text | YES | İçerik yapısı notları |
| `sceneUsageNotes` | Text | YES | Sahne kullanım rehberi |
| `reviewNotes` | Text | YES | Review notları |
| `videoTitle` | String | YES | YT Long: SEO başlık |
| `longDescription` | Text | YES | YT Long: detaylı açıklama |
| `chapterOutlineJson` | Text (JSON) | YES | YT Long: chapters listesi |
| `pinnedCommentSuggestion` | Text | YES | YT Long: sabitlenecek yorum |
| `thumbnailTextSuggestion` | String | YES | YT Long: thumbnail metin |
| `tagsJson` | Text (JSON) | YES | YT Long: video etiketleri |
| `shortHook` | String | YES | Short/Reels: hook metni |
| `shortCaption` | Text | YES | Short/Reels: kısa açıklama |
| `overlayTextSuggestionsJson` | Text (JSON) | YES | Short/Reels: ekran metinleri |
| `shortHashtagSetJson` | Text (JSON) | YES | Short/Reels: hashtag seti |
| `shortCta` | String | YES | Short/Reels: kısa CTA |
| `durationGuidance` | String | YES | Short/Reels: süre rehberi |
| `providerType` | String | YES | Provider tipi (MOCK/OPENAI) |
| `modelName` | String | YES | Model adı (gpt-4o-mini) |
| `generatedBy` | Enum | YES | AI_CONTENT_AGENT / null |
| `editedAt` | Timestamp | YES | Son düzenleme |
| `editedBy` | String | YES | Düzenleyen admin |
| `approvedAt` | Timestamp | YES | Onay zamanı |
| `approvedBy` | String | YES | Onaylayan admin |
| `rejectedAt` | Timestamp | YES | Red zamanı |
| `rejectedBy` | String | YES | Reddeden admin |
| `rejectionReason` | Text | YES | Red gerekçesi |

#### Metadata Stratejisi — KARAR: Flat Entity

**Seçim:** Platform-spesifik metadata alanları doğrudan `PlatformPackageDraft` entity'sine gömülür.

**Gerekçe:**

| Avantaj | Açıklama |
|---------|----------|
| Basitlik | Ayrı child entity/join gereksiz |
| Sorgu performansı | Tek tablo, tek sorgu |
| CRUD kolaylığı | Edit/update/read tek entity |
| Lazy loading riski yok | Child entity olmadığı için `open-in-view=false` sorunu yok |
| FP-003 pattern | FP-003 StoryboardScene'de de sahne alanları flat |

**Trade-off:**

Tablo 35+ kolon içerecek. Platform başına ~6 kolon nullable. PostgreSQL'de nullable
kolonlar disk maliyeti düşük (null = 1 bit per null bitmap). Alan sayısı yönetilebilir.

**Gelecek:** Platform sayısı veya alan sayısı çok artarsa JSONB veya child entity'ye
refactor edilebilir. İlk implementasyonda basitlik tercih edilir.

### 4.2. PlatformPackageSceneUsage (Child Entity)

Her storyboard sahnesinin belirli bir pakette nasıl kullanıldığını tanımlar.

#### Alanlar

| Alan | Tip | Nullable | Açıklama |
|------|-----|----------|----------|
| *id* | UUID | NO | BaseEntity'den miras |
| *createdAt* | Timestamp | NO | BaseEntity'den miras |
| *updatedAt* | Timestamp | YES | BaseEntity'den miras |
| `platformPackageDraft` | ManyToOne → PlatformPackageDraft | NO | Ait olduğu paket |
| `sceneNumber` | Integer | NO | Storyboard sahne numarası |
| `sourceStoryboardScene` | ManyToOne → StoryboardScene | YES | Kaynak sahne referansı |
| `usageType` | String | NO | PRIMARY / ADAPTED / PARTIAL / OMITTED |
| `included` | Boolean | NO | Bu pakette kullanılıyor mu |
| `adaptationNote` | Text | YES | Adaptasyon notu |

### 4.3. Entity İlişkileri

```
ContentProject (1) ──── (N) PlatformPackageDraft
StoryboardDraft (1) ──── (N) PlatformPackageDraft
PlatformPackageDraft (1) ──── (N) PlatformPackageSceneUsage
StoryboardScene (1) ──── (N) PlatformPackageSceneUsage
```

---

## 5. Proposed Enums

### 5.1. PlatformPackageType

```java
YOUTUBE_LONG_FORM,
YOUTUBE_SHORTS,
INSTAGRAM_REELS,
FACEBOOK_REELS
```

### 5.2. PlatformPackageStatus

```java
AI_GENERATED,
NEEDS_REVIEW,
APPROVED,
REJECTED,
FAILED
```

> FP-002/FP-003 ile tutarlı. `READY_FOR_PUBLISH` eklenmez.

### 5.3. PlatformPackageSceneUsageType (Opsiyonel — String olarak da saklanabilir)

İlk implementasyonda `usageType` basit `String` olarak saklanır.
Enum tanımı engineering task sırasında kesinleştirilir.

Aday değerler:

```
PRIMARY       — Sahne olduğu gibi kullanılır
ADAPTED       — Sahne adapte edilerek kullanılır
PARTIAL       — Sahnenin bir kısmı kullanılır
OMITTED       — Sahne bu pakette kullanılmaz
```

---

## 6. Database Design

### 6.1. platform_package_drafts

```sql
CREATE TABLE platform_package_drafts (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_project_id          UUID NOT NULL REFERENCES content_projects(id),
    source_storyboard_draft_id  UUID NOT NULL REFERENCES storyboard_drafts(id),
    package_type                VARCHAR(30) NOT NULL,
    status                      VARCHAR(20) NOT NULL,
    version                     INTEGER NOT NULL DEFAULT 1,

    -- Common fields
    title                       VARCHAR(255) NOT NULL,
    caption_or_description      TEXT NOT NULL,
    hashtags_json               TEXT,
    call_to_action              VARCHAR(500),
    target_duration_seconds     INTEGER,
    aspect_ratio                VARCHAR(10),
    content_structure_notes     TEXT,
    scene_usage_notes           TEXT,
    review_notes                TEXT,

    -- YouTube Long-form metadata
    video_title                 VARCHAR(255),
    long_description            TEXT,
    chapter_outline_json        TEXT,
    pinned_comment_suggestion   TEXT,
    thumbnail_text_suggestion   VARCHAR(100),
    tags_json                   TEXT,

    -- Short-form / Reels metadata
    short_hook                  VARCHAR(500),
    short_caption               TEXT,
    overlay_text_suggestions_json TEXT,
    short_hashtag_set_json      TEXT,
    short_cta                   VARCHAR(500),
    duration_guidance           VARCHAR(100),

    -- AI metadata
    provider_type               VARCHAR(20),
    model_name                  VARCHAR(50),
    generated_by                VARCHAR(30),

    -- Review lifecycle
    edited_at                   TIMESTAMP,
    edited_by                   VARCHAR(100),
    approved_at                 TIMESTAMP,
    approved_by                 VARCHAR(100),
    rejected_at                 TIMESTAMP,
    rejected_by                 VARCHAR(100),
    rejection_reason            TEXT,

    -- Audit (BaseEntity)
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 6.2. platform_package_scene_usages

```sql
CREATE TABLE platform_package_scene_usages (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    platform_package_draft_id   UUID NOT NULL REFERENCES platform_package_drafts(id) ON DELETE CASCADE,
    scene_number                INTEGER NOT NULL,
    source_storyboard_scene_id  UUID REFERENCES storyboard_scenes(id),
    usage_type                  VARCHAR(20) NOT NULL,
    included                    BOOLEAN NOT NULL DEFAULT true,
    adaptation_note             TEXT,

    -- Audit (BaseEntity)
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 6.3. Indexes

```sql
CREATE INDEX idx_ppd_content_project ON platform_package_drafts(content_project_id);
CREATE INDEX idx_ppd_source_storyboard ON platform_package_drafts(source_storyboard_draft_id);
CREATE INDEX idx_ppd_package_type ON platform_package_drafts(package_type);
CREATE INDEX idx_ppd_status ON platform_package_drafts(status);
CREATE INDEX idx_ppd_project_type_status ON platform_package_drafts(content_project_id, package_type, status);
CREATE INDEX idx_ppsu_draft ON platform_package_scene_usages(platform_package_draft_id);
```

### 6.4. Constraints

| Kısıtlama | Açıklama |
|-----------|----------|
| `package_type NOT NULL` | Platform tipi zorunlu |
| `status NOT NULL` | Status zorunlu |
| `version NOT NULL` | Versiyon zorunlu |
| `title NOT NULL` | Başlık zorunlu |
| `caption_or_description NOT NULL` | Açıklama zorunlu |
| FK `content_project_id` | ContentProject'e referans |
| FK `source_storyboard_draft_id` | StoryboardDraft'a referans |
| FK `platform_package_draft_id` ON DELETE CASCADE | Parent silinince scene usage'lar da silinir |
| Unique approval | Application service'te enforce: content_project + package_type başına max 1 APPROVED |

### 6.5. Migration

Flyway `V7__fp004_platform_package_tables.sql` olarak oluşturulacak.

### 6.6. Saklanmaması Gerekenler

| Hariç | Gerekçe |
|-------|---------|
| Raw prompt | Güvenlik |
| Raw provider response | Güvenlik |
| API key / secret | Güvenlik |
| Media file paths | FP-004 kapsamı dışı |
| Publishing metadata | FP-004 kapsamı dışı |

---

## 7. Versioning Strategy

### Kurallar

| # | Kural |
|---|-------|
| 1 | Versiyon grubu: `ContentProject + sourceStoryboardDraft + packageType` |
| 2 | İlk üretim → version = 1 |
| 3 | Regeneration → `MAX(version) + 1` per package type |
| 4 | Tek AI isteği 4 paket üretir, her biri kendi package type'ı için doğru versiyon alır |
| 5 | Mevcut APPROVED paketler overwrite edilmez |
| 6 | Eski APPROVED paketler auto-unapprove edilmez |
| 7 | Aynı package type için eski versiyonlar okunabilir kalır |

### Örnek

```
ContentProject: cp-001
StoryboardDraft: sb-001 (APPROVED)

İlk üretim:
  YOUTUBE_LONG_FORM v1 → AI_GENERATED
  YOUTUBE_SHORTS v1    → AI_GENERATED
  INSTAGRAM_REELS v1   → AI_GENERATED
  FACEBOOK_REELS v1    → AI_GENERATED

Admin YOUTUBE_LONG_FORM v1'i APPROVED yapar.
Admin YOUTUBE_SHORTS v1'i REJECTED yapar.

Regeneration:
  YOUTUBE_LONG_FORM v2 → AI_GENERATED (v1 APPROVED kalır)
  YOUTUBE_SHORTS v2    → AI_GENERATED (v1 REJECTED kalır)
  INSTAGRAM_REELS v2   → AI_GENERATED (v1 read-only)
  FACEBOOK_REELS v2    → AI_GENERATED (v1 read-only)
```

---

## 8. Approval Rule

### Kurallar

| # | Kural |
|---|-------|
| 1 | Aynı ContentProject + PlatformPackageType için **en fazla 1 APPROVED** paket |
| 2 | İkinci bir paketi approve etmek → business error (`409 Conflict`) |
| 3 | Approval per-platform bağımsızdır |
| 4 | Batch auto-approval yoktur |
| 5 | `READY_FOR_PUBLISH` yoktur |
| 6 | Approve yalnızca `AI_GENERATED` veya `NEEDS_REVIEW` statüsünden yapılabilir |
| 7 | Reject yalnızca `AI_GENERATED` veya `NEEDS_REVIEW` statüsünden yapılabilir |
| 8 | Reject `rejectionReason` zorunlu kılar |
| 9 | `APPROVED` / `REJECTED` / `FAILED` → read-only, edit yapılamaz |

### Status Transitions

```
AI_GENERATED ──[edit]──→ NEEDS_REVIEW
AI_GENERATED ──[approve]──→ APPROVED
AI_GENERATED ──[reject]──→ REJECTED

NEEDS_REVIEW ──[edit]──→ NEEDS_REVIEW
NEEDS_REVIEW ──[approve]──→ APPROVED
NEEDS_REVIEW ──[reject]──→ REJECTED

APPROVED ──→ (read-only, final)
REJECTED ──→ (read-only, final)
FAILED ──→ (read-only, admin regeneration tetikleyebilir)
```

---

## 9. Application Services

### 9.1. PlatformPackageGenerationService

| Sorumluluk | Açıklama |
|-----------|----------|
| Validation | StoryboardDraft var mı? Status = APPROVED mı? |
| Request build | AI contract request oluştur |
| Gateway call | `AiPlatformPackageAgentGateway.generate()` |
| Response validation | All-or-nothing doğrulama |
| Persist | 4 PlatformPackageDraft + SceneUsage kayıtları |
| Failure | FAILED kayıt oluşturma (opsiyonel) |
| Precondition | Gateway, prerequisite failure'da çağrılmaz |

**Bağımlılıklar:** `StoryboardDraftRepository`, `PlatformPackageDraftRepository`, `AiPlatformPackageAgentGateway`

### 9.2. PlatformPackageReviewService

| Sorumluluk | Açıklama |
|-----------|----------|
| Edit | Editable alanları güncelle, status → NEEDS_REVIEW |
| Approve | Status → APPROVED, approve metadata kaydet |
| Reject | Status → REJECTED, rejection reason zorunlu |
| Status guard | AI_GENERATED/NEEDS_REVIEW'dan izin ver, diğerlerinden reddet |
| One-approved | ContentProject + packageType başına max 1 APPROVED enforce |
| No AI | Gateway bağımlılığı yok |

**Bağımlılıklar:** `PlatformPackageDraftRepository`

### 9.3. PlatformPackageQueryService

| Sorumluluk | Açıklama |
|-----------|----------|
| List by project | ContentProject'e ait paketleri listele |
| List by storyboard | StoryboardDraft'a ait paketleri listele |
| Detail | Paket detayı + scene usage'lar |
| Lazy load | `open-in-view=false` — service'te force load |

**Bağımlılıklar:** `PlatformPackageDraftRepository`

---

## 10. AI Gateway Design

### 10.1. Interface

```
AiPlatformPackageAgentGateway
  └─ generate(PlatformPackageAgentRequest) → PlatformPackageAgentResponse
```

### 10.2. Implementasyonlar

| Class | Açıklama | Aktivasyon |
|-------|----------|------------|
| `MockAiPlatformPackageAgentGateway` | Deterministik mock yanıt | `@ConditionalOnMissingBean` (fallback) |
| `OpenAiPlatformPackageAgentGateway` | OpenAI API çağrısı | `ai.provider=openai` |

### 10.3. OpenAI Alt Bileşenler

| Class | Açıklama |
|-------|----------|
| `OpenAiPlatformPackagePromptBuilder` | System + user prompt oluşturma |
| `OpenAiPlatformPackageResponseParser` | JSON yanıt parse + validation |

### 10.4. Bean Konfigürasyon Kuralları

| # | Kural |
|---|-------|
| 1 | FP-001/FP-002/FP-003 gateway bean'leri **değiştirilmez** |
| 2 | FP-004 gateway kendi bean tanımlarını kullanır |
| 3 | Mock gateway `@ConditionalOnMissingBean` ile fallback |
| 4 | OpenAI gateway `@Conditional` + `ai.provider=openai` ile aktif |
| 5 | Her provider `providerType` ve `modelName` response'a ekler |
| 6 | Prompt template versiyonlama prompt_templates tablosunda |

---

## 11. REST API Design

### 11.1. Endpoints

| Method | Path | Açıklama |
|--------|------|----------|
| `POST` | `/api/admin/storyboards/{storyboardId}/platform-packages/generate` | Paket üretimi tetikle |
| `GET` | `/api/admin/content-projects/{projectId}/platform-packages` | Proje paketlerini listele |
| `GET` | `/api/admin/storyboards/{storyboardId}/platform-packages` | Storyboard paketlerini listele |
| `GET` | `/api/admin/platform-packages/{packageId}` | Paket detayı |
| `PUT` | `/api/admin/platform-packages/{packageId}` | Paket düzenle |
| `POST` | `/api/admin/platform-packages/{packageId}/approve` | Explicit approve |
| `POST` | `/api/admin/platform-packages/{packageId}/reject` | Explicit reject |

### 11.2. REST DTO'ları (AI Contract'tan Ayrı)

#### Request DTO'ları

| DTO | Açıklama |
|-----|----------|
| `PlatformPackageGenerateRequest` | Üretim tetikleme (targetPlatforms, toneGuidance, vb.) |
| `PlatformPackageEditRequest` | Düzenleme (title, caption, hashtags, vb.) |
| `PlatformPackageRejectRequest` | Red (rejectionReason zorunlu) |

#### Response DTO'ları

| DTO | Açıklama |
|-----|----------|
| `PlatformPackageDraftResponse` | Detay yanıtı |
| `PlatformPackageDraftSummaryResponse` | Liste yanıtı |
| `PlatformPackageSceneUsageResponse` | Scene usage detayı |
| `ActionStatusResponse` | Approve/reject aksiyon yanıtı (FP-003 ile ortak) |

### 11.3. REST Kuralları

| # | Kural |
|---|-------|
| 1 | Generic status update endpoint yok |
| 2 | `READY_FOR_PUBLISH` endpoint yok |
| 3 | AI contract DTO'ları REST'te expose edilmez |
| 4 | Raw prompt / provider payload / API key alanları yok |
| 5 | Media generation / publishing alanları yok |
| 6 | `ApiResponse<T>` wrapper kullanılır (FP-003 ile tutarlı) |

---

## 12. Admin UI Design

### 12.1. Erişim Noktası

Content Project detay sayfasında, Storyboard bölümünün altında
**"Platform Paketleri"** sekmesi olarak eklenir.

### 12.2. UI Davranışları

| Durum | Davranış |
|-------|----------|
| Approved storyboard yok | Uyarı göster: "Önce storyboard onaylayın" |
| Approved storyboard var | "Paket Üret" butonu göster |
| Paket üretimi tamamlandı | 4 platform paketi kartları göster |
| Paket AI_GENERATED/NEEDS_REVIEW | Edit, Approve, Reject butonları aktif |
| Paket APPROVED | Read-only, yeşil badge |
| Paket REJECTED | Read-only, kırmızı badge |
| Paket FAILED | Read-only, gri badge |

### 12.3. UI Listeleme

Paketler platform tipine göre gruplandırılır:

```
YouTube Long-form    [v1 AI_GENERATED] [Düzenle] [Onayla] [Reddet]
YouTube Shorts       [v1 APPROVED ✅]
Instagram Reels      [v1 NEEDS_REVIEW] [Düzenle] [Onayla] [Reddet]
Facebook Reels       [v1 REJECTED ❌]
```

### 12.4. UI Kısıtlamaları

| # | Kısıtlama |
|---|-----------|
| 1 | Generic status dropdown yok |
| 2 | `READY_FOR_PUBLISH` aksiyonu yok |
| 3 | Publishing UI yok |
| 4 | Media generation UI yok |
| 5 | Raw prompt / provider payload görünmez |
| 6 | Approve/reject salt explicit butonlar ile |

---

## 13. Testing Strategy

### 13.1. Backend Test Katmanları

| Katman | Test Kapsamı | Tahmini Test Sayısı |
|--------|-------------|---------------------|
| Domain | Entity builder, status transition, validation | ~15 |
| Repository | CRUD, query methods | ~8 |
| GenerationService | Mock gateway + persistence + failure | ~20 |
| ReviewService | Edit/approve/reject + status guard + one-approved | ~18 |
| QueryService | List/detail + lazy load safety | ~8 |
| REST Controller | Endpoint routing, DTO mapping, error handling | ~20 |
| Mock Gateway | Deterministic output, contract compliance | ~5 |
| OpenAI PromptBuilder | Request → prompt transformation | ~6 |
| OpenAI ResponseParser | JSON → DTO parse + validation | ~12 |
| Bean Config | Provider toggling, conditional beans | ~4 |
| **Toplam Backend** | | **~116** |

### 13.2. Frontend Test Katmanları

| Katman | Test Kapsamı |
|--------|-------------|
| API Client | Endpoint çağrıları, error handling |
| Component | List, detail, edit form, approve/reject |
| Workflow | Generate → review → approve/reject akışı |
| UI Guardrails | Status-based buton visibility, read-only guard |

### 13.3. Runtime / Staging

| Test | Açıklama |
|------|----------|
| Mock smoke | Mock provider ile tam akış (generate → list → detail → edit → approve) |
| OpenAI smoke | Gerçek AI ile paket üretimi doğrulama |
| One-approved rule | Aynı tip için ikinci approve'un hata döndürdüğünü doğrula |
| API contract | List/detail/edit response shape doğrulama |

---

## 14. Implementation Task Breakdown

### Task 04 — Backend Domain & Schema

**Amaç:** Entity, enum, repository ve Flyway migration oluşturma.

**Deliverable'lar:**
- `PlatformPackageDraft` entity (BaseEntity extends)
- `PlatformPackageSceneUsage` entity (BaseEntity extends)
- `PlatformPackageType` enum
- `PlatformPackageStatus` enum
- `PlatformPackageDraftRepository`
- `V7__fp004_platform_package_tables.sql`
- Domain unit testleri

**Out of scope:** Service, gateway, REST, UI, OpenAI.

**Kabul kriterleri:**
- Entity'ler compile ediyor
- Migration clean DB'de çalışıyor
- Repository CRUD testleri geçiyor
- Mevcut 480+ test kırılmıyor

---

### Task 05 — Backend Service with Mock Generation

**Amaç:** PlatformPackageGenerationService + AI contract DTO'ları + mock gateway.

**Deliverable'lar:**
- AI contract DTO'ları (request + response)
- `AiPlatformPackageAgentGateway` interface
- `MockAiPlatformPackageAgentGateway`
- `PlatformPackageGenerationService`
- All-or-nothing failure handling
- Versioning logic
- Unit + integration testleri

**Out of scope:** REST, UI, OpenAI, review/query service.

**Kabul kriterleri:**
- Mock ile 4 paket üretimi başarılı
- APPROVED olmayan storyboard'dan üretim reddedilir
- Version doğru hesaplanır
- Scene usage kayıtları oluşturuluyor
- All-or-nothing failure testi geçiyor

---

### Task 06 — Backend Review/Query Service

**Amaç:** Edit, approve, reject workflow + query service.

**Deliverable'lar:**
- `PlatformPackageReviewService`
- `PlatformPackageQueryService`
- Edit → NEEDS_REVIEW transition
- Approve + one-approved-per-platform enforcement
- Reject + rejectionReason zorunluluğu
- Status guard (read-only enforcement)
- Lazy load safety (open-in-view=false)
- Unit testleri

**Out of scope:** REST, UI, OpenAI.

**Kabul kriterleri:**
- Edit AI_GENERATED → NEEDS_REVIEW
- Approve AI_GENERATED/NEEDS_REVIEW → APPROVED
- İkinci approve aynı tip → 409 Conflict
- Reject + reason zorunlu
- APPROVED/REJECTED/FAILED edit engellenir
- List/detail lazy load hatası yok

---

### Task 07 — Admin REST API

**Amaç:** REST controller + DTO'lar + endpoint testleri.

**Deliverable'lar:**
- `AdminPlatformPackageController`
- REST request/response DTO'ları
- 7 endpoint implementasyonu
- Controller unit testleri
- DTO mapping testleri

**Out of scope:** UI, OpenAI.

**Kabul kriterleri:**
- Tüm 7 endpoint doğru çalışıyor
- REST DTO'ları AI contract DTO'larından ayrı
- ApiResponse wrapper kullanılıyor
- Raw prompt/provider payload expose yok
- Generic status endpoint yok

---

### Task 08 — Admin UI Workflow

**Amaç:** Frontend platform package yönetim arayüzü.

**Deliverable'lar:**
- Platform Packages sekmesi
- Generate buton + platform seçimi
- Paket list (platform tipine göre gruplu)
- Paket detail view
- Edit form
- Approve/reject butonları
- Status-based UI guardrails

**Out of scope:** OpenAI, publishing UI, media UI.

**Kabul kriterleri:**
- Approved storyboard yokken uyarı gösterir
- Generate 4 paket üretir
- List/detail doğru veri gösterir
- Edit → NEEDS_REVIEW
- Approve → APPROVED (read-only olur)
- Reject → REJECTED (read-only olur)
- APPROVED/REJECTED/FAILED'de edit/approve/reject gizli

---

### Task 09 — OpenAI Platform Package Provider

**Amaç:** Gerçek OpenAI provider implementasyonu.

**Deliverable'lar:**
- `OpenAiPlatformPackageAgentGateway`
- `OpenAiPlatformPackagePromptBuilder`
- `OpenAiPlatformPackageResponseParser`
- Prompt template (prompt_templates tablosunda)
- PromptBuilder testleri
- ResponseParser testleri
- Bean config testleri

**Out of scope:** UI değişikliği, REST değişikliği.

**Kabul kriterleri:**
- Mock fallback çalışıyor
- OpenAI gateway 4 platform paketi üretiyor
- JSON parse + validation başarılı
- Invalid response → controlled FAILED
- Prompt template versiyonlanmış

---

### Task 10 — End-to-End Validation & Closure Readiness

**Amaç:** Tam akış doğrulama, staging smoke, regression kontrolü.

**Deliverable'lar:**
- Mock smoke test (tam akış)
- Staging deploy + Flyway V7 doğrulama
- OpenAI smoke test (gerçek AI)
- One-approved-per-platform doğrulama
- List/detail/edit/approve/reject API doğrulama
- Regression kontrolü (FP-001/FP-002/FP-003 kırılmamış)

**Out of scope:** Yeni feature, publishing, media.

**Kabul kriterleri:**
- Tüm testler geçiyor
- Mock smoke başarılı
- OpenAI smoke başarılı (4 platform paketi)
- Staging API'ler çalışıyor
- FP-001/FP-002/FP-003 regression yok

---

### Final OPS Follow-up — Staging Real OpenAI Smoke

**Amaç:** Production-like ortamda real OpenAI doğrulama.

**Deliverable'lar:**
- Staging'de gerçek OpenAI ile paket üretimi
- 4 platform paketi doğrulama
- Edit/approve/reject flow doğrulama
- Maliyet kontrolü
- Closure raporu

---

## 15. Risks and Mitigations

| # | Risk | Etki | Mitigation |
|---|------|------|------------|
| 1 | Scope creep → publishing/media | Feature sızması | Out of scope listesi strict, her task'ta tekrarla |
| 2 | Platform metadata modeli çok büyük | 35+ kolon tablo | İlk fazda flat, gerekirse JSONB refactor |
| 3 | All-or-nothing provider failure | 1 platform hatası tümünü etkiler | İlk fazda basitlik, gelecekte partial success |
| 4 | Versioning ambiguity | Hangi paket güncel? | ContentProject + packageType + version unique |
| 5 | Lazy loading `open-in-view=false` | LazyInitializationException | Service'te force load (FP-003 pattern) |
| 6 | AI contract REST'te sızma | Güvenlik | DTO ayrımı strict, code review |
| 7 | Raw prompt/payload saklanma | Güvenlik | Entity'de prompt alanı yok, review checklist |
| 8 | Approval ambiguity | İkinci approve ne olur? | One-approved-per-platform, service enforcement |
| 9 | Migration conflict | V7 vs mevcut V6 | Flyway version sequential, clean DB test |
| 10 | FP-003 regression | Mevcut feature kırılması | FP-003 testleri korunur, regression suite |

---

## 16. Out of Scope

| # | Kapsam Dışı | Gerekçe |
|---|-------------|---------|
| 1 | Publishing integrations (YouTube/IG/FB API) | Gelecek feature |
| 2 | OAuth / social account integration | Gelecek feature |
| 3 | Video/audio/image generation | Media generation scope |
| 4 | Media storage | Media generation scope |
| 5 | Scheduling | Publishing scope |
| 6 | Analytics | Analytics scope |
| 7 | `READY_FOR_PUBLISH` status | Governance kararı |
| 8 | Generic status update endpoint | Governance kararı |
| 9 | Partial success (per-platform) | Gelecek iterasyon |
| 10 | Single-platform regeneration | Gelecek iterasyon |
| 11 | Prompt management UI | Ayrı OPS feature |
| 12 | Cost dashboard | Ayrı analytics feature |
| 13 | Raw prompt / provider payload storage | Güvenlik |
| 14 | FP-001/FP-002/FP-003 davranış değişikliği | Scope restriction |

---

## 17. Document History

| Tarih | Değişiklik |
|-------|-----------|
| 2026-07-03 | İlk oluşturma — Engineering Design and Task Breakdown |
