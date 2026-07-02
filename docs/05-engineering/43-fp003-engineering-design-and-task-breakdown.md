# 43 — FP-003 Engineering Design & Implementation Task Breakdown

> **Status**: 📋 DESIGN / PLANNING
>
> **Feature**: FP-003 — Storyboard Workflow
>
> **Prerequisites**: FP-003 Task 01 (Product Design), Task 02 (AI Contract Design)
>
> **Scope**: Engineering design + implementation task breakdown only.
> No backend/frontend/schema/API/runtime implementation.

---

## 1. Input Documents

| # | Doküman | Açıklama |
|---|---------|----------|
| 1 | [41-fp003-storyboard-workflow-product-design.md](./41-fp003-storyboard-workflow-product-design.md) | Product/workflow design |
| 2 | [42-fp003-ai-platform-contract-design.md](./42-fp003-ai-platform-contract-design.md) | AI platform contract design |
| 3 | [40-fp002-final-closure-operational-baseline.md](./40-fp002-final-closure-operational-baseline.md) | FP-002 closure & operational baseline |
| 4 | [34-fp001-final-closure-operational-baseline.md](./34-fp001-final-closure-operational-baseline.md) | FP-001 closure & operational baseline |

---

## 2. Engineering Objective

FP-003, FP-001 ve FP-002'yi zayıflatmadan storyboard generation workflow'unu ekler.

### Target Workflow

```
1. Sistem approved human-reviewed source ScenarioDraft'ı doğrular
2. Admin storyboard generation başlatır
3. AI gateway provider-agnostic storyboard scene content üretir
4. Backend storyboard candidate/version olarak saklar
5. Admin storyboard candidate'ı görüntüler
6. Admin storyboard scene'lerini düzenler
7. Admin explicit olarak approve veya reject eder
8. Approved storyboard source Content Project, ContentDraft ve ScenarioDraft'a izlenebilir kalır
```

### FP-001 / FP-002 Koruması

FP-003 implementasyonu sırasında:

- `ContentProjectStatus` enum değiştirilmez
- `ContentDraftStatus` enum değiştirilmez
- `ScenarioDraftStatus` enum değiştirilmez
- `ScenarioBeatType` enum değiştirilmez
- Mevcut FP-001/FP-002 API endpoints değiştirilmez
- Mevcut workflow transitions değiştirilmez
- `AiContentAgentGateway` interface dokunulmaz
- `AiScenarioAgentGateway` interface dokunulmaz
- `MockAiContentAgentGateway` dokunulmaz
- `MockAiScenarioAgentGateway` dokunulmaz
- FP-001 safety guarantees zayıflatılmaz
- FP-002 safety guarantees zayıflatılmaz

---

## 3. Domain Model Design Direction

### Entity Naming Evaluation

| Aday | Pro | Con | Uygunluk |
|------|-----|-----|----------|
| `StoryboardDraft` | FP-001 `ContentDraft` + FP-002 `ScenarioDraft` pattern tutarlılığı, reviewable candidate semantiği | Biraz uzun | ✅ **Önerilen** |
| `Storyboard` | Kısa, ürün diliyle uyumlu | Approved ve candidate ayrımı belirsiz, ileride final storyboard entity gerekirse çakışma | ⚠️ İkincil |
| `StoryboardCandidate` | Candidate semantiği açık | FP-001/FP-002 pattern'ıyla uyumsuz | ❌ |
| `GeneratedStoryboard` | AI üretim bağlamı | Review/edit sonrasını kapsamaz | ❌ |

### ✅ Önerilen: `StoryboardDraft`

**Gerekçe:**

1. **Product uyumu**: Ürün kavramı "Storyboard" — entity adı bunu yansıtır
2. **FP-001/FP-002 pattern**: `ContentDraft` → `ScenarioDraft` → `StoryboardDraft` tutarlı naming chain
3. **Semantik**: AI üretir → admin review/edit eder → approve/reject — bu "draft" semantiğidir
4. **İleride genişleme**: Approved storyboard → media/image/video generation input olabilir
5. **Karışıklık engeli**: `ContentDraft` (FP-001) vs `ScenarioDraft` (FP-002) vs `StoryboardDraft` (FP-003) açık ayrım
6. **Naming consistency**: Tüm üç feature aynı `{Concept}Draft` pattern'ını kullanır

### Scene Entity: `StoryboardScene`

Child entity. `StoryboardDraft` ile parent-child ilişkisi.

### Package Yapısı

```
com.anadoluatlasi.backend
  ├── ai
  │   ├── config/           ← AiGatewayConfig (mevcut — FP-003 bean'leri eklenir)
  │   ├── contract/         ← StoryboardAgentRequest, StoryboardAgentResponse, StoryboardSceneOutput
  │   ├── entity/           ← AiErrorType (mevcut — değiştirilmez)
  │   └── gateway/
  │       ├── AiStoryboardAgentGateway.java              [NEW]
  │       ├── mock/
  │       │   └── MockAiStoryboardAgentGateway.java      [NEW]
  │       └── openai/
  │           ├── OpenAiStoryboardAgentGateway.java       [NEW]
  │           ├── OpenAiStoryboardPromptBuilder.java      [NEW]
  │           └── OpenAiStoryboardResponseParser.java     [NEW]
  ├── application/
  │   └── storyboard/
  │       ├── StoryboardGenerationService.java            [NEW]
  │       └── StoryboardReviewService.java                [NEW]
  └── storyboard/
      ├── controller/
      │   └── AdminStoryboardController.java              [NEW]
      ├── entity/
      │   ├── StoryboardDraft.java                        [NEW]
      │   ├── StoryboardDraftStatus.java                  [NEW]
      │   └── StoryboardScene.java                        [NEW]
      └── repository/
          └── StoryboardDraftRepository.java              [NEW]
```

---

## 4. Storyboard Lifecycle Engineering Direction

### Status Enum: `StoryboardDraftStatus`

| Status | Açıklama | Kimler Oluşturur |
|--------|----------|-----------------|
| `AI_GENERATED` | AI tarafından üretildi | `StoryboardGenerationService` |
| `NEEDS_REVIEW` | Admin tarafından düzenlendi | `StoryboardReviewService.edit()` |
| `APPROVED` | Admin tarafından onaylandı | `StoryboardReviewService.approve()` |
| `REJECTED` | Admin tarafından reddedildi | `StoryboardReviewService.reject()` |
| `FAILED` | AI üretimi başarısız | `StoryboardGenerationService` |

### Lifecycle Rules

| # | Kural |
|---|-------|
| 1 | AI generation yalnızca `AI_GENERATED` veya `FAILED` oluşturabilir |
| 2 | AI asla `APPROVED` oluşturamaz |
| 3 | Admin: `AI_GENERATED` → `NEEDS_REVIEW` (edit), `APPROVED` (approve), `REJECTED` (reject) |
| 4 | Admin: `NEEDS_REVIEW` → `APPROVED` (approve), `REJECTED` (reject) |
| 5 | Edit dedicated `EDITED` status gerektirmez — metadata (editedBy, editedAt) ile takip |
| 6 | Generic status update behavior yok — yalnızca explicit action methods |
| 7 | `APPROVED` / `REJECTED` / `FAILED` read-only — edit/approve/reject yapılamaz |

### State Diagram

```
[Generate]
    │
    ├─ success ──→ AI_GENERATED ──┬── [Edit]    → NEEDS_REVIEW ──┬── [Approve] → APPROVED
    │                             │                               └── [Reject]  → REJECTED
    │                             ├── [Approve] → APPROVED
    │                             └── [Reject]  → REJECTED
    │
    └─ failure ──→ FAILED
```

---

## 5. Data Model Planning

### StoryboardDraft (Conceptual)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `id` | UUID | ✅ | PK |
| `contentProjectId` | UUID | ✅ | FK → content_projects |
| `sourceDraftId` | UUID | ✅ | Kaynak ContentDraft referansı |
| `sourceScenarioDraftId` | UUID | ✅ | FK → scenario_drafts |
| `sourceScenarioVersion` | Integer | ✅ | Kaynak senaryo versiyonu |
| `version` | Integer | ✅ | Storyboard versiyon numarası |
| `status` | Enum | ✅ | StoryboardDraftStatus |
| `title` | String | ❌ | Storyboard başlığı |
| `summary` | String | ❌ | Storyboard özeti |
| `targetPlatform` | String | ❌ | Hedef platform |
| `aspectRatio` | String | ❌ | Görüntü oranı |
| `estimatedDurationSeconds` | Integer | ❌ | Toplam tahmini süre |
| `visualStyleGuide` | String | ❌ | Genel görsel stil kılavuzu |
| `narrationStyle` | String | ❌ | Narration stil notu |
| `reviewNotes` | String | ❌ | Admin review notları |
| `generatedBy` | String | ❌ | Üreten agent bilgisi |
| `providerType` | String | ❌ | Safe metadata: OPENAI / MOCK |
| `modelName` | String | ❌ | Safe metadata: model adı |
| `createdAt` | Timestamp | ✅ | JPA auditing |
| `updatedAt` | Timestamp | ✅ | JPA auditing |
| `approvedAt` | Timestamp | ❌ | Onay zamanı |
| `rejectedAt` | Timestamp | ❌ | Red zamanı |
| `editedAt` | Timestamp | ❌ | Son düzenleme zamanı |
| `createdBy` | String | ❌ | Oluşturan admin |
| `approvedBy` | String | ❌ | Onaylayan admin |
| `rejectedBy` | String | ❌ | Reddeden admin |
| `editedBy` | String | ❌ | Son düzenleyen admin |

### StoryboardScene (Conceptual)

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `id` | UUID | ✅ | PK |
| `storyboardDraftId` | UUID | ✅ | FK → storyboard_drafts |
| `sceneNumber` | Integer | ✅ | Sıra numarası (1-based) |
| `sourceBeatType` | String | ✅ | Kaynak beat tipi (HOOK, INTRODUCTION, vb.) |
| `sourceBeatOrder` | Integer | ✅ | Kaynak beat sıra numarası |
| `sceneTitle` | String | ✅ | Sahne başlığı |
| `visualDescription` | String | ✅ | Sahne görsel açıklaması |
| `narrationText` | String | ❌ | Voiceover / narration metni |
| `onScreenText` | String | ❌ | Ekranda gösterilecek metin |
| `cameraDirection` | String | ❌ | Kamera yönü / hareketi |
| `transitionNote` | String | ❌ | Sahne geçiş notu |
| `mood` | String | ❌ | Sahne atmosferi |
| `location` | String | ❌ | Sahne mekanı |
| `characters` | String | ❌ | Sahnedeki karakterler |
| `estimatedDurationSeconds` | Integer | ✅ | Sahne tahmini süresi |
| `mediaGenerationNotes` | String | ❌ | Gelecek media üretimi handoff — yalnızca metadata |

### Data Rules

| # | Kural |
|---|-------|
| 1 | Gerçek image/video/audio/media file alanları yok |
| 2 | Raw provider payload saklanmaz |
| 3 | Raw prompt saklanmaz |
| 4 | API keys/secrets saklanmaz |
| 5 | StoryboardDraft, source approved ScenarioDraft'a referans verir |
| 6 | Version, generation/regeneration'da increment eder |
| 7 | Approved storyboard overwrite edilmez |
| 8 | StoryboardScene, child table olarak persist edilir (embedded değil) |

### One-Approved Rule

**Önerilen:** Max one approved storyboard per ContentProject.

- Service-level enforcement (`StoryboardReviewService.approve()`)
- DB constraint gerekmez — FP-002 pattern ile tutarlı
- Yeni approve isteği varken mevcut approved varsa → HTTP 409

---

## 6. Source Beat Type Decision

### Options

| Option | Açıklama | Pro | Con |
|--------|----------|-----|-----|
| A: Reuse `ScenarioBeatType` | StoryboardScene `sourceBeatType` olarak `ScenarioBeatType` enum kullanır | Type-safe, reuse | FP-002 entity'sine bağımlılık |
| B: String field | `sourceBeatType` String olarak saklar | Decoupled | Type safety yok |
| C: New enum | `StoryboardSceneSourceBeatType` ayrı enum | Full isolation | Duplikasyon |

### ✅ Önerilen: Option A — Reuse `ScenarioBeatType`

**Gerekçe:**

1. Beat tipleri (HOOK, INTRODUCTION, vb.) FP-002'de tanımlı ve sabit
2. Storyboard scene'ler doğrudan bu beat'lere referans veriyor
3. Type-safe mapping sağlar
4. `ScenarioBeatType` enum'u `scenario.entity` package'ında — read-only referans, modify yok
5. FP-002 enum'una yeni değer eklenmez

---

## 7. API Design Direction

### Endpoint Design

| # | Operation | Method | Path |
|---|-----------|--------|------|
| 1 | Generate | POST | `/api/admin/scenarios/{scenarioId}/storyboards/generate` |
| 2 | List | GET | `/api/admin/scenarios/{scenarioId}/storyboards` |
| 3 | Detail | GET | `/api/admin/storyboards/{storyboardId}` |
| 4 | Edit | PUT | `/api/admin/storyboards/{storyboardId}` |
| 5 | Approve | POST | `/api/admin/storyboards/{storyboardId}/approve` |
| 6 | Reject | POST | `/api/admin/storyboards/{storyboardId}/reject` |

### API Rules

| # | Kural |
|---|-------|
| 1 | No generic `PATCH /status` endpoint |
| 2 | No generic status dropdown |
| 3 | REST DTOs provider-agnostic |
| 4 | Internal AI DTOs REST'e expose edilmez |
| 5 | Error messages sanitized |
| 6 | No media generation endpoints in FP-003 |
| 7 | Controller thin — business logic service layer'da |
| 8 | Generate endpoint approved scenario prerequisite check yapar |

### Swagger Path Note

GET ve PUT `/api/admin/storyboards/{storyboardId}` aynı path'i paylaşır.
Swagger'da 5 path entry, 6 operation gösterilecektir (FP-002 pattern ile tutarlı).

---

## 8. Admin UI Design Direction

### Expected UI Additions

| # | Component | Açıklama |
|---|-----------|----------|
| 1 | Storyboard section | Scenario detail sayfasında, approved senaryo altında storyboard bölümü |
| 2 | Generate button | Approved scenario'da "Storyboard Oluştur" butonu |
| 3 | Version list | Storyboard versiyonları tablosu (version, status, tarih) |
| 4 | Detail panel | Storyboard detail — scene listesi ile |
| 5 | Scene edit form | Her scene için inline edit form (14 alan) |
| 6 | Approve button | Explicit onay butonu + confirmation dialog |
| 7 | Reject button | Explicit red butonu + reason input |
| 8 | Loading state | Generation sırasında spinner |
| 9 | Error state | Hata mesajı + retry butonu |
| 10 | Read-only state | APPROVED/REJECTED/FAILED storyboard'lar read-only |

### UI Rules

| # | Kural |
|---|-------|
| 1 | Generate button yalnızca approved scenario'da görünür |
| 2 | Edit yalnızca AI_GENERATED / NEEDS_REVIEW'da aktif |
| 3 | Save asla approve yapmaz |
| 4 | Approve/reject yalnızca explicit buton tıklamasıyla |
| 5 | Approved storyboard varken generate buton label'ı "Yeni Versiyon Oluştur" |

---

## 9. AI Gateway Implementation Direction

### Direction

| # | Karar |
|---|-------|
| 1 | Ayrı `AiStoryboardAgentGateway` interface oluştur |
| 2 | Mock implementation'ı önce yap |
| 3 | OpenAI implementation'ı contract/service testlerinden sonra ekle |
| 4 | Model config-driven tut (baseline: `gpt-4o-mini`) |
| 5 | Raw prompt document etme |
| 6 | Raw provider payload expose etme |
| 7 | AI output'u persist etmeden önce validate et |
| 8 | Mevcut `AiErrorType` enum'u reuse et — yeterli kategori mevcut |
| 9 | Mevcut `OpenAiClient` reuse et |
| 10 | `AiGatewayConfig.java`'ya storyboard bean'lerini ekle |

### Config Pattern

```java
// AiGatewayConfig.java — FP-003 eklentisi
@Bean
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public AiStoryboardAgentGateway openAiStoryboardAgentGateway(AiProviderProperties properties) { ... }

@Bean
@ConditionalOnMissingBean(AiStoryboardAgentGateway.class)
public AiStoryboardAgentGateway mockAiStoryboardAgentGateway() { ... }
```

---

## 10. Test Strategy

### Backend Tests

| Category | Test Cases |
|----------|-----------|
| Domain/Repository | StoryboardDraft CRUD, scene cascade, version increment |
| Generation prerequisite | Approved scenario required, unapproved → 409, missing → 404 |
| Mock generation | Successful mock creates AI_GENERATED with scenes |
| Failed generation | Provider error → FAILED status |
| Regeneration | Creates new version, previous version untouched |
| One-approved rule | Approve blocked if approved version exists → 409 |
| Edit | AI_GENERATED → NEEDS_REVIEW, save ≠ approve |
| Approve | Explicit action, AI_GENERATED/NEEDS_REVIEW → APPROVED |
| Reject | Explicit action with reason, → REJECTED |
| Read-only | APPROVED/REJECTED/FAILED → edit/approve/reject blocked |
| Auto-approval guard | AI cannot create APPROVED |
| No generic status | No status transition endpoint |

### API/Controller Tests

| Category | Test Cases |
|----------|-----------|
| Generate | POST returns storyboard result, provider-agnostic DTO |
| List | GET returns version list |
| Detail | GET returns full storyboard with scenes |
| Edit | PUT updates scenes, status → NEEDS_REVIEW |
| Approve/Reject | POST explicit action |
| Error response | Sanitized, no provider details |
| DTO safety | No internal AI DTOs exposed |

### Gateway Tests

| Category | Test Cases |
|----------|-----------|
| Mock provider | Deterministic scene output, correct count |
| OpenAI mapper | Valid structured output → response contract |
| Invalid output | Empty/broken → INVALID_AI_RESPONSE_FORMAT |
| Provider errors | Timeout/auth/rate-limit → AiErrorType mapping |
| Gateway safety | `@ConditionalOnMissingBean` fallback test |

### Frontend/Manual QA

| Category | Test Cases |
|----------|-----------|
| Generate button | Visible only for approved scenario |
| Storyboard list | Versions displayed correctly |
| Scene edit | All 14 fields editable |
| Approve/reject | Explicit actions only |
| Read-only | APPROVED/REJECTED/FAILED non-editable |
| Error states | Error toast, retry button |

---

## 11. Implementation Task Breakdown

### Task 04 — Backend Domain & Schema

| Item | Detail |
|------|--------|
| Scope | Entity/status/scene model, migration, repository |
| Entity | `StoryboardDraft`, `StoryboardScene`, `StoryboardDraftStatus` |
| Migration | `V5__fp003_storyboard_tables.sql` |
| AI call | ❌ No AI call yet |
| Tests | Repository/entity tests |
| Governance | No auto-approve, no generic status |

### Task 05 — Backend Storyboard Service with Mock Generation

| Item | Detail |
|------|--------|
| Scope | Gateway interface, contracts, mock gateway, generation service |
| Gateway | `AiStoryboardAgentGateway` interface |
| Contracts | `StoryboardAgentRequest`, `StoryboardAgentResponse`, `StoryboardSceneOutput` |
| Mock | `MockAiStoryboardAgentGateway` — deterministic 8 scenes |
| Service | `StoryboardGenerationService` — approved scenario check, versioning |
| Config | `AiGatewayConfig` — mock bean with `@ConditionalOnMissingBean` |
| Tests | Generation from approved scenario, failed scenario, versioning |
| Governance | AI_GENERATED only, no auto-approve |

### Task 06 — Storyboard Review Actions Backend

| Item | Detail |
|------|--------|
| Scope | Edit, approve, reject actions |
| Service | `StoryboardReviewService` |
| Edit | Scene content update → NEEDS_REVIEW |
| Approve | Explicit action → APPROVED, one-approved rule |
| Reject | Explicit action with reason → REJECTED |
| Tests | Edit/approve/reject, one-approved rule, read-only states |
| Governance | No generic status, save ≠ approve |

### Task 07 — Admin API Endpoints

| Item | Detail |
|------|--------|
| Scope | 6 REST endpoints, thin controller, provider-agnostic DTOs |
| Controller | `AdminStoryboardController` |
| Endpoints | Generate, list, detail, edit, approve, reject |
| DTOs | Provider-agnostic, no internal AI contract exposure |
| Tests | Controller/integration tests |
| Governance | Sanitized errors, no raw payload |

### Task 08 — Admin UI Storyboard Workflow

| Item | Detail |
|------|--------|
| Scope | Frontend storyboard section in scenario detail |
| Components | Generate button, version list, detail panel, scene edit, approve/reject |
| Build | TypeScript clean, Vite build passing |
| QA | Manual flow verification |
| Governance | UI-level safety (no auto-approve, explicit buttons) |

### Task 09 — OpenAI Storyboard Provider

| Item | Detail |
|------|--------|
| Scope | Real OpenAI implementation |
| Gateway | `OpenAiStoryboardAgentGateway` |
| Builder | `OpenAiStoryboardPromptBuilder` — storyboard-specific prompt |
| Parser | `OpenAiStoryboardResponseParser` — scene parsing |
| Reuse | `OpenAiClient` (shared), `AiProviderProperties` |
| Tests | Unit tests with mocked OpenAI client |
| Governance | No raw prompt/payload committed, provider boundary maintained |

### Task 10 — Local Smoke Test

| Item | Detail |
|------|--------|
| Scope | Local mock + optional real provider smoke |
| Flow | Approved scenario → generate storyboard → edit scenes → approve/reject |
| Provider | Mock first, real OpenAI if locally configured |
| Governance | Full workflow validation |

### Task 11 — Staging Execution Plan

| Item | Detail |
|------|--------|
| Scope | Deployment checklist, migration checks, rollback plan |
| Stack | `anadolu-atlasi-staging` (no rename) |
| Governance | Security, regression, governance checks |

### Task 12 — Staging Execution Report

| Item | Detail |
|------|--------|
| Scope | Execute staging validation |
| Provider | Real OpenAI storyboard generation |
| Regression | FP-001 + FP-002 regression check |
| Governance | No secret/raw prompt/raw payload exposure |

### Task 13 — Final Closure & Operational Baseline

| Item | Detail |
|------|--------|
| Scope | Documentation-only closure |
| Content | Operational baseline, validation results, follow-ups |
| Code change | ❌ None |

### Task Dependency Graph

```
Task 04 (Domain/Schema)
    │
    ▼
Task 05 (Service + Mock Gateway)
    │
    ▼
Task 06 (Review Actions)
    │
    ▼
Task 07 (API Endpoints)
    │
    ├──────────────────┐
    ▼                  ▼
Task 08 (Admin UI)   Task 09 (OpenAI Provider)
    │                  │
    └────────┬─────────┘
             ▼
Task 10 (Local Smoke)
             │
             ▼
Task 11 (Staging Plan)
             │
             ▼
Task 12 (Staging Report)
             │
             ▼
Task 13 (Final Closure)
```

---

## 12. Risk Review

| # | Risk | Mitigation |
|---|------|------------|
| 1 | ScenarioDraft vs StoryboardDraft naming karışıklığı | Tutarlı `{Concept}Draft` pattern, açık package separation |
| 2 | Media generation'ın erken tanıtılması | `mediaGenerationNotes` yalnızca future metadata — gerçek media üretimi yok |
| 3 | Internal AI DTO exposure | Ayrı DTO layer, controller'da mapping |
| 4 | Raw provider payload/prompt storage | Response contract'a normalize et, DB'de raw alan yok |
| 5 | Unapproved scenario'dan storyboard generation | Service layer prerequisite check — approved ScenarioDraft zorunlu |
| 6 | Accidental auto-approval | Generation service yalnızca AI_GENERATED/FAILED oluşturur, test coverage |
| 7 | Generic status endpoint | No PATCH/PUT status endpoint, explicit action methods only |
| 8 | Approved storyboard overwrite | Regeneration yeni version oluşturur, approved dokunulmaz |
| 9 | OpenAI invalid output shape | Response parser + validation, FAILED fallback |
| 10 | Cost/rate-limit | `gpt-4o-mini` (düşük maliyet), rate limit error mapping |
| 11 | Scope creep → image/video/audio | Explicit out-of-scope, no media file fields in schema |
| 12 | FP-001/FP-002 regression | No existing code modified, integration tests |

---

## 13. Open Decisions for Implementation

| # | Karar | Önerilen | Kesinleşme |
|---|-------|----------|------------|
| 1 | Entity name | `StoryboardDraft` | ✅ Confirmed |
| 2 | Scene persistence | Child table (`storyboard_scenes`) | ✅ Confirmed |
| 3 | One-approved rule scope | Per ContentProject, service-level | ✅ Confirmed |
| 4 | Source beat type | Reuse `ScenarioBeatType` enum | ✅ Confirmed |
| 5 | Approved scenario check | `ScenarioDraftStatus.APPROVED` from DB | Task 05'te confirm |
| 6 | Error type | Reuse existing `AiErrorType` enum | Task 05'te confirm |
| 7 | Admin route structure | `/storyboards` resource-based | Task 07'de confirm |

---

## 14. Non-Goals

| # | Non-Goal |
|---|----------|
| 1 | Image generation |
| 2 | Video generation |
| 3 | Audio generation |
| 4 | Media asset creation/storage |
| 5 | Public publishing |
| 6 | Prompt management UI |
| 7 | Cost dashboard |
| 8 | Multi-provider UI |
| 9 | Bulk storyboard generation |
| 10 | Production provider switching |
| 11 | Stack naming cleanup (OPS-001) |
| 12 | Multi-platform content packaging (FP-004) |

---

## 15. Future Follow-up: FP-004 — Multi-Platform Content Packaging Workflow

Aşağıdaki scope FP-003 Task 04–13 implementation listesine dahil **değildir**.
Bu yalnızca gelecek feature sprint referansıdır.

### Hedef

Approved StoryboardDraft → platform-spesifik içerik paketleri

### Potansiyel Platform Paketleri

| # | Paket | Format | Açıklama |
|---|-------|--------|----------|
| 1 | YouTube Long-form | 16:9 | Uzun hikâye anlatımı, detaylı scene seçimi, zengin narration |
| 2 | YouTube Shorts | 9:16 | Kısa hook-driven cut, özlü hikâye anı |
| 3 | Instagram Reels | 9:16 | Hızlı görsel ritim, duygusal/merak hook'u |
| 4 | Facebook Reels | 9:16 | Erişilebilir hikâye anlatımı, nostaljik/açıklayıcı ton |
| 5 | Social Metadata | Metin | Başlık, açıklama, hashtag'ler, CTA |

### FP-003 Handoff Noktaları

FP-003 storyboard modeli şu alanlarla FP-004'e handoff sağlar:

- `targetPlatform` — gelecek platform-spesifik paketleme için ipucu
- `aspectRatio` — görsel composition planlaması
- `estimatedDurationSeconds` — platform süre optimizasyonu
- `StoryboardScene.mediaGenerationNotes` — scene bazlı media üretim metadata'sı

### Scope Sınırı

| # | Kural |
|---|-------|
| 1 | FP-004 scope'u FP-003 implementation task listesine (Task 04–13) dahil değildir |
| 2 | FP-003 yalnızca storyboard generation/review/approval yapar |
| 3 | FP-004 için backend/frontend/schema kararları FP-003 closure sonrasına ertelenir |
| 4 | FP-003 mevcut task listesi değiştirilmez |

---

## 16. Acceptance Criteria

| # | Kriter | Durum |
|---|--------|-------|
| 1 | FP-003 Task 03 engineering design dokümanı oluşturuldu | ✅ |
| 2 | Backend/frontend/schema/API/runtime implementation yapılmadı | ✅ |
| 3 | Domain model direction belgelendi (`StoryboardDraft`) | ✅ |
| 4 | Gateway direction belgelendi (ayrı `AiStoryboardAgentGateway`) | ✅ |
| 5 | API direction belgelendi (6 endpoint) | ✅ |
| 6 | Admin UI direction belgelendi (10 component) | ✅ |
| 7 | Test strategy belgelendi (4 category) | ✅ |
| 8 | Implementation tasks küçük reviewable task'lara bölündü (10 task) | ✅ |
| 9 | FP-001/FP-002/FP-003 governance rules korundu | ✅ |
| 10 | Media generation scope creep açıkça engellendi | ✅ |
| 11 | No raw prompt/provider payload/secret documented | ✅ |
| 12 | FP-004 multi-platform future direction belgelendi | ✅ |
