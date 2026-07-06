# FP-019 — Türkü Story-to-Video Vertical Slice — Combined Product + Engineering Design

## 1. Feature Status & Baseline

| Field | Value |
|-------|-------|
| Feature | FP-019 — Türkü Story-to-Video Vertical Slice |
| Status | **PLANNED / DESIGN** |
| Starting Commit | b95bd7e |
| Previous Feature | FP-018 (CLOSED) |

| Metric | Baseline |
|--------|:--------:|
| Backend tests | 2775 / 0 failures |
| Frontend tests | 697 / 0 failures |
| Combined tests | 3472 / 0 failures |
| Endpoints | 21 = 9 POST + 12 GET |
| Migrations | None |
| Schema changes | None |
| Config changes | None |

---

## 2. Product Goal

**Admin enters a türkü name → system prepares a controlled internal video production package.**

The first vertical slice of the Anadolu Atlası platform targets the **Türkü Hikâyeleri** niche: short-form narrative videos that tell the cultural story behind well-known Anatolian folk songs (türküler).

### Outcome

- Admin provides a türkü name (e.g., "Mihriban", "Çanakkale Türküsü", "Uzun İnce Bir Yoldayım").
- System creates a **draft video project** and orchestrates an internal production workflow.
- The workflow prepares: **story context → scenario → storyboard → scene asset plan → image batch → internal preview**.
- All outputs are **admin-reviewable** through the existing safe admin review workflow.
- **No public publishing.** The output is an internal production package, not a delivered video.

### Why This Slice First

1. Türkü stories have strong cultural resonance and clear narrative structure (origin, emotion, geography, character).
2. The türkü-to-video pattern exercises every major pipeline stage without requiring real-time interactivity.
3. Internal-only delivery allows controlled quality iteration before any public exposure.

---

## 3. User Workflow

```
Admin opens "Yeni Türkü Projesi" panel
  ↓
Enters türkü name: "Mihriban"
Optionally fills:
  - region: "Erzurum"
  - source notes: "Aşık Sümmani'ye atfedilir"
  - mood: "hüzünlü, özlem"
  - target duration: "3-5 dakika"
  - narration style: "hikâyeci"
  - visual style: "suluboya Anadolu manzaraları"
  ↓
Clicks "Proje Oluştur"
  ↓
System creates DRAFT project
  ↓
Admin reviews → clicks "Hikâye Hazırla"
  ↓
System generates story context package
  (origin, theme, emotional arc, cultural notes, confidence flags)
  ↓
Admin reviews story → clicks "Senaryo Hazırla"
  ↓
System generates scenario with narrative beats
  (Hook → Giriş → Hikâye → Duygusal Zirve → Kapanış → CTA)
  ↓
Admin reviews scenario → clicks "Storyboard Hazırla"
  ↓
System generates storyboard scenes
  (scene_no, duration, narration, visual, mood, camera, transition)
  ↓
Admin reviews storyboard → clicks "Sahne Görselleri Planla"
  ↓
System generates scene asset plan + image generation prompts
  ↓
Admin reviews prompts → clicks "Görsel Üretim Hazırla"
  ↓
System prepares image generation batch (safe references only)
  ↓
Admin views internal preview (progress + generated outputs)
  ↓
Admin can request revision at any stage
  ↓
No publish/export step exists
```

### Key Principles

- **Step-by-step admin gate**: each stage requires admin review before next stage.
- **No auto-approval**: system prepares, admin decides.
- **Revision-friendly**: admin can request revision using existing FP-018 review workflow.
- **No public output**: all previews are admin-internal only.

---

## 4. Domain Model Proposal

> Design-level only. No implementation in Task 01.

### 4.1 TurkuVideoProject

**Purpose**: Root aggregate for a single türkü video production workflow.

**Likely fields**:
- `id` (UUID)
- `turkuName` (string, required) — the folk song name
- `region` (string, optional) — geographic origin
- `sourceNotes` (string, optional) — attribution, historical notes
- `mood` (string, optional) — emotional tone
- `targetDurationMinutes` (int, optional) — target video length
- `narrationStyle` (string, optional) — hikâyeci, belgesel, etc.
- `visualStyle` (string, optional) — suluboya, sinematik, etc.
- `workflowStage` (TurkuVideoWorkflowStage)
- `safetyState` (TurkuVideoWorkflowSafetyState)
- `sourceConfidenceLevel` (LOW / MEDIUM / HIGH)
- `copyrightReviewRequired` (boolean)
- `createdAt`, `updatedAt`

**Must NOT store/expose**: provider API keys, raw AI responses, JWT/tokens, base64 media, public URLs, signed URLs, CDN URLs, copyrighted lyrics.

**Relation**: Maps to existing `projects` table concept. May extend or use a dedicated table. Links to `research_notes`, `scripts`, `scenes`, `scene_prompts` from existing schema.

### 4.2 TurkuVideoBootstrapRequest

**Purpose**: Admin input to start a new project.

**Likely fields**: `turkuName`, `region`, `sourceNotes`, `mood`, `targetDurationMinutes`, `narrationStyle`, `visualStyle`.

**Must NOT contain**: actorRole (handled by resolver), tokens, credentials.

### 4.3 TurkuStoryPackage

**Purpose**: AI-researched cultural/historical context about the türkü.

**Likely fields**:
- `turkuOriginSummary` — where/when the türkü emerged
- `emotionalTheme` — core emotional arc
- `geographicContext` — Anatolian landscape/setting
- `characterNotes` — people/characters referenced
- `culturalSignificance` — why this türkü matters
- `sourceConfidence` (LOW/MEDIUM/HIGH)
- `needsHumanReview` (boolean)
- `safeWarnings` (list of strings) — hallucination or uncertainty flags

**Must NOT store**: copyrighted lyrics, raw AI provider response, provider model name/version beyond safe reference.

**Relation**: Extends `research_notes` concept from existing schema.

### 4.4 TurkuNarrativeContext

**Purpose**: Distilled narrative seed for scenario generation.

**Likely fields**: `hookAngle`, `emotionalArc`, `narrativeStructure`, `targetAudience`, `toneGuide`.

### 4.5 TurkuScenarioPlan

**Purpose**: Full video scenario following the 6-beat structure.

**Likely fields**:
- `hook` — opening 10-15 seconds
- `introduction` — context setting
- `storyBody` — main narrative
- `emotionalPeak` — climax
- `closing` — resolution
- `callToAction` — ending CTA
- `estimatedDurationSeconds`
- `narrationText` — full voiceover script

**Relation**: Maps to `scripts` table.

### 4.6 TurkuStoryboardPlan

**Purpose**: Scene-by-scene visual plan.

**Likely fields**: list of scenes, each with `sceneNo`, `durationSeconds`, `narrationText`, `visualDescription`, `mood`, `cameraDirection`, `transitionType`, `musicNote`.

**Relation**: Maps to `scenes` table.

### 4.7 TurkuSceneAssetPlan

**Purpose**: Per-scene image/video generation plan.

**Likely fields**: list of scene assets, each with `sceneNo`, `turkishDescription`, `englishPrompt`, `negativePrompt`, `aspectRatio`, `visualStyle`, `expectedOutputType` (IMAGE/VIDEO_PLACEHOLDER).

**Must NOT store**: provider-specific generation parameters, API keys, raw payloads.

**Relation**: Maps to `scene_prompts` table.

### 4.8 TurkuSceneImageBatch

**Purpose**: Batch tracking for image generation across scenes.

**Likely fields**:
- `batchId` (UUID)
- `projectId` (UUID)
- `totalScenes` (int)
- `completedScenes` (int)
- `failedScenes` (int)
- `batchStatus` (PENDING / IN_PROGRESS / COMPLETED / PARTIAL / FAILED)
- `safeProgressSummary` (string)

**Must NOT store**: raw provider responses, base64 image data, provider URLs, storage keys.

**Relation**: Maps to `generated_assets` tracking concept.

### 4.9 TurkuInternalPreviewPackage

**Purpose**: Admin-internal view of the production state.

**Likely fields**:
- `projectId`
- `storySummary`
- `scenarioBeats` (list)
- `storyboardScenes` (list with thumbnails if available through safe proxy)
- `generationProgress`
- `safetyFlags`
- `copyrightReviewRequired`
- `overallConfidence`

**Must NOT contain**: public URLs, signed URLs, CDN URLs, downloadable exports, raw media data.

### 4.10 TurkuVideoWorkflowStage

**Purpose**: Controlled state machine for video production.

Values defined in §5.

### 4.11 TurkuVideoWorkflowSafetyState

**Purpose**: Safety overlay on workflow state.

**Likely values**: `SAFE`, `NEEDS_REVIEW`, `COPYRIGHT_REVIEW_REQUIRED`, `LOW_CONFIDENCE`, `BLOCKED_BY_POLICY`.

---

## 5. Workflow Stages

### Allowed States

| Stage | Description | Entry Condition |
|-------|-------------|-----------------|
| `DRAFT_CREATED` | Project bootstrapped, no generation yet | Bootstrap request accepted |
| `STORY_CONTEXT_PREPARED` | Story package generated | Admin triggered prepare-story |
| `SCENARIO_PREPARED` | Scenario beats generated | Story reviewed + admin triggered |
| `STORYBOARD_PREPARED` | Scene-by-scene plan generated | Scenario reviewed + admin triggered |
| `SCENE_ASSET_PLAN_PREPARED` | Per-scene prompts generated | Storyboard reviewed + admin triggered |
| `IMAGE_BATCH_PREPARED` | Image generation batch ready | Scene asset plan reviewed |
| `IMAGE_BATCH_COMPLETED` | All scene images generated | Batch completed successfully |
| `IMAGE_BATCH_PARTIAL` | Some images generated, some failed | Batch partially completed |
| `INTERNAL_PREVIEW_READY` | Internal preview package assembled | Images available (full or partial) |
| `NEEDS_REVISION` | Admin requested revision at any stage | Admin revision request |
| `BLOCKED` | Blocked by policy, copyright, or error | System or admin block |
| `ARCHIVED` | Project archived (not deleted) | Admin archive action |

### Explicitly Forbidden States

These states **MUST NOT** exist in FP-019:

| Forbidden State | Reason |
|----------------|--------|
| `PUBLISHED` | No public publishing in FP-019 |
| `EXPORTED` | No export in FP-019 |
| `PUBLICLY_DELIVERED` | No public delivery in FP-019 |
| `AUTO_APPROVED` | No auto-approval ever |
| `COMPLETED_AS_PUBLIC_VIDEO` | No final public video in FP-019 |

### Compatibility with Existing Review States

The existing review state machine (PENDING_REVIEW → IN_REVIEW → CHANGES_REQUESTED → APPROVED_FOR_INTERNAL_USE → REJECTED → RETURNED_TO_DRAFT) can be used alongside türkü workflow stages. The türkü stage tracks production progress; the review state tracks admin review status. They are complementary, not competing.

---

## 6. API Design Proposal

> **Future proposal only.** No implementation in Task 01. Current endpoint count remains 21.

### Proposed Endpoints

| Method | Path | Purpose | Notes |
|--------|------|---------|-------|
| POST | `/admin/turku-video-projects/bootstrap` | Create new project from türkü name | Returns draft project |
| GET | `/admin/turku-video-projects/{id}` | Get project details | Safe fields only |
| GET | `/admin/turku-video-projects/{id}/progress` | Get workflow progress | Stage + completion |
| POST | `/admin/turku-video-projects/{id}/prepare-story` | Trigger story package generation | Async-safe |
| POST | `/admin/turku-video-projects/{id}/prepare-scenario` | Trigger scenario generation | Requires story reviewed |
| POST | `/admin/turku-video-projects/{id}/prepare-storyboard` | Trigger storyboard generation | Requires scenario reviewed |
| POST | `/admin/turku-video-projects/{id}/prepare-scene-assets` | Trigger scene asset plan | Requires storyboard reviewed |
| POST | `/admin/turku-video-projects/{id}/prepare-image-batch` | Trigger image generation batch | Requires scene assets reviewed |
| GET | `/admin/turku-video-projects/{id}/internal-preview` | Get internal preview package | Admin-only, not public |

**Endpoint count change**: Current 21 + 9 proposed = 30 total (when implemented across Tasks 02–04).

### API Design Constraints

- All endpoints under `/admin/` prefix — no public API.
- All POST endpoints must route through AdminActorResolver (FP-018 pattern).
- No endpoint returns public/signed/CDN URLs.
- No endpoint returns raw provider responses.
- No endpoint accepts JWT/token/credential in body.
- Error responses use existing safe ApiResponse pattern.

---

## 7. AI/Provider Orchestration Design

### Provider Interaction Rules

| Rule | Detail |
|------|--------|
| No raw provider payload in DTOs | Store safe summaries and internal references only |
| No provider request/response dumps | Log safe summaries only; full payloads in structured internal logs if needed |
| No secrets/tokens in logs | All provider keys via environment/config; never in request/response |
| Safe error codes for failures | `PROVIDER_UNAVAILABLE`, `GENERATION_FAILED`, `GENERATION_TIMEOUT` |
| Reviewable outputs | All AI-generated story/scenario/storyboard visible to admin before next stage |
| Source confidence tracking | LOW/MEDIUM/HIGH per generation; LOW triggers `needsHumanReview=true` |

### Cultural/Historical Claim Safety

Türküler carry deep cultural significance. AI generation must handle claims carefully:

| Concern | Mitigation |
|---------|------------|
| Fabricated origin stories | Mark confidence level; if uncertain, flag `needsHumanReview` |
| Incorrect geographic attribution | Include `sourceNotes` from admin input as ground truth |
| Definitive historical claims | Preface uncertain claims with hedging language |
| Cultural misrepresentation | Admin review gate before each stage advancement |
| Attribution to wrong artist/poet | Cross-reference known attributions; flag when unsure |

### Prompt Template Strategy

- Use versioned prompt templates (existing `prompt_templates` concept from schema).
- Each generation stage has a dedicated prompt template.
- Templates include:
  - System context: "You are a cultural historian specializing in Anatolian folk music."
  - Safety instructions: "Do not fabricate definitive historical facts when uncertain."
  - Output format specifications.
  - Confidence self-assessment instructions.

---

## 8. Copyright / Cultural Safety Design

### Copyright Constraints

| Constraint | Detail |
|-----------|--------|
| Türkü names reference folk tradition | The song itself may be traditional/public domain |
| Performances may be copyrighted | Specific recordings, arrangements, and performances have separate copyright |
| No audio/video download/copy | FP-019 must not download, copy, or redistribute copyrighted recordings |
| No lyrics dumping | Do not reproduce full lyrics unless future legal policy explicitly allows |
| Safe narrative use | Story, context, mood, and original narration planning are acceptable |
| Music handling deferred | Final audio/music composition is out of scope for FP-019 |
| Source/copyright review flag | `copyrightReviewRequired: boolean` on every project |

### Cultural Safety

| Concern | Design Response |
|---------|----------------|
| Sacred/religious türküler | Flag for admin review; do not auto-generate irreverent content |
| Regional sensitivity | Allow admin to specify region context; system should respect it |
| Political/controversial content | AI prompts should avoid political interpretation of folk songs |
| Living artists/poets | Do not generate content that could misrepresent living persons |

### Audio/Music Placeholder Strategy

Since final audio handling is deferred:
- Storyboard scenes include `musicNote` field (text description, not audio file).
- Internal preview shows music direction as text labels.
- No audio file generation, download, or playback in FP-019.
- Future FP may introduce safe, licensed audio integration.

---

## 9. Internal Preview Design

### What Internal Preview IS

- Admin-only view of generated plan and assets.
- Rendered in the admin UI panel.
- Includes: story summary, scenario beats, storyboard scenes, image thumbnails (if generated), generation progress, safety flags.

### What Internal Preview IS NOT

| Not | Reason |
|-----|--------|
| Public URL | No public exposure |
| Signed URL | No signed URL generation |
| CDN delivery | No CDN integration |
| Downloadable export | No export functionality |
| Final rendered video | No video composition in FP-019 |
| Streamable media | No streaming endpoint |

### Preview Content Structure

```
Internal Preview Package
├── Project metadata (name, region, mood, stage)
├── Story summary (if prepared)
├── Scenario beats (if prepared)
│   ├── Hook
│   ├── Introduction
│   ├── Story body
│   ├── Emotional peak
│   ├── Closing
│   └── CTA
├── Storyboard scenes (if prepared)
│   ├── Scene 1: narration, visual desc, mood
│   ├── Scene 2: ...
│   └── Scene N: ...
├── Image thumbnails (if generated)
│   └── Via existing safe internal preview proxy
├── Generation progress
│   ├── Current stage
│   ├── Batch progress (X/Y completed)
│   └── Failed items (if any)
└── Safety flags
    ├── Source confidence
    ├── Copyright review required
    └── Needs human review
```

### Image Thumbnail Access

If image thumbnails are generated, they must be accessed through the existing safe internal preview mechanism (FP-013/FP-014 pattern):
- Admin-only preview proxy.
- No direct storage/provider URLs exposed.
- No base64 inline data in API responses.
- No signed URLs in responses.

---

## 10. Admin Actor & Review Boundary

### FP-018 Carryover

All FP-019 admin workflow actions must follow FP-018 conventions:

| Rule | Detail |
|------|--------|
| actorRole is not production auth | Request-body actorRole remains TEST_FALLBACK compatibility input |
| AdminActorResolver integration | All new POST endpoints must route through resolver |
| SYSTEM cannot auto-approve | `canSystemApprove()` remains false |
| No auto-approval | Every stage advancement requires admin action |
| Review gates are human-controlled | System prepares, admin decides |
| CREATOR restrictions enforced | CREATOR cannot perform review/handoff/revision actions |
| Boundary policy enforcement | New action categories should be added to AdminActorBoundaryPolicy |

### Proposed New Action Categories

```java
TURKU_PROJECT_BOOTSTRAP    // allowed: ADMIN, REVIEWER
TURKU_STAGE_PREPARATION    // allowed: ADMIN, REVIEWER
TURKU_INTERNAL_PREVIEW     // allowed: ADMIN, REVIEWER, CREATOR (read-only)
```

SYSTEM is NOT allowed for any türkü project action except read-only visibility, consistent with existing patterns.

---

## 11. Safety Baseline

The following safety baseline from FP-018 is **explicitly preserved** in FP-019:

| Safety Rule | Status |
|-------------|:------:|
| No publish/export | ✅ Preserved |
| No public/signed/CDN delivery | ✅ Preserved |
| No auto-approval | ✅ Preserved |
| No generic status update | ✅ Preserved |
| No provider/storage/raw/base64/secret exposure | ✅ Preserved |
| No unsafe HTML | ✅ Preserved |
| No token/JWT/session/principal/authorizationHeader exposure | ✅ Preserved |
| No migration/schema/config changes unless explicitly scoped | ✅ Preserved |
| No production auth implementation | ✅ Preserved |
| No copyrighted audio/video download/copy | ✅ New for FP-019 |
| No lyrics reproduction | ✅ New for FP-019 |
| Source confidence tracking | ✅ New for FP-019 |
| Copyright review flag | ✅ New for FP-019 |

---

## 12. Six-Task Implementation Plan

### Task 01 — Combined Product + Engineering Design ← **THIS DOCUMENT**

- Documentation-only.
- No code/test/endpoint/migration/schema/config changes.

---

### Task 02 — Türkü Video Project Bootstrap Backend

**Scope**: Backend production + backend tests + docs.

**Deliverables**:
- `TurkuVideoProject` entity/DTO.
- `TurkuVideoBootstrapRequest` / `TurkuVideoBootstrapResponse` DTOs.
- `TurkuVideoWorkflowStage` enum.
- `TurkuVideoProjectService` — bootstrap logic, validation, safe defaults.
- `TurkuVideoProjectController` — bootstrap endpoint + get project + get progress.
- Validation: türkü name required, safe length limits, no HTML, no credential patterns.
- AdminActorResolver integration for bootstrap POST.
- Tests: bootstrap happy path, validation, actor boundary, endpoint inventory, safety.
- Doc: implementation note.

**Endpoint inventory change**: 21 → 24 (add 3: bootstrap POST, get project GET, get progress GET).

**No AI generation yet** — bootstrap creates a DRAFT_CREATED project only.

---

### Task 03 — Story/Scenario/Storyboard Orchestration Backend

**Scope**: Backend production + backend tests + docs.

**Deliverables**:
- `TurkuStoryPackage` / `TurkuScenarioPlan` / `TurkuStoryboardPlan` DTOs.
- `TurkuNarrativeContext` internal value object.
- Story preparation service (AI adapter integration or safe stub).
- Scenario preparation service.
- Storyboard preparation service.
- Confidence/review flags on all generated packages.
- Three POST endpoints: prepare-story, prepare-scenario, prepare-storyboard.
- AdminActorResolver integration.
- Tests: generation happy path, stage transitions, confidence flags, actor boundary, safety.
- Doc: implementation note.

**Endpoint inventory change**: 24 → 27 (add 3 POST).

---

### Task 04 — Scene Asset Plan + Image Generation Batch Integration

**Scope**: Backend production + backend tests + docs.

**Deliverables**:
- `TurkuSceneAssetPlan` / `TurkuSceneImageBatch` DTOs.
- Scene asset plan preparation service.
- Image batch preparation service (controlled execution or safe stub).
- Batch progress tracking.
- Two POST + one GET endpoint: prepare-scene-assets, prepare-image-batch, internal-preview.
- Safe references only — no raw/base64/provider/storage exposure.
- Tests: batch tracking, partial completion, failure handling, safety.
- Doc: implementation note.

**Endpoint inventory change**: 27 → 30 (add 2 POST + 1 GET).

---

### Task 05 — Admin UI Start/Progress/Internal Preview Integration

**Scope**: Frontend/admin production + frontend tests + docs.

**Deliverables**:
- TypeScript types for all türkü video DTOs.
- API client methods for all türkü video endpoints.
- Bootstrap panel: türkü name input + optional fields + create button.
- Progress view panel: workflow stage + completion status.
- Internal preview panel: story/scenario/storyboard/image summary.
- No publish/export UI.
- Tests: type safety, API client, UI component source checks, safety.
- Doc: implementation note.

**No backend changes in Task 05.**

---

### Task 06 — E2E Safety Regression + Final Closure

**Scope**: Tests + docs primarily.

**Deliverables**:
- Backend E2E regression: full workflow from bootstrap to internal preview.
- Frontend regression: type safety, API client, UI safety.
- Endpoint inventory verification: expected final count.
- Safety inventory verification: all safety rules preserved.
- Final closure doc.
- FP-019 CLOSED recommendation.

**No production code changes unless tests expose real gaps.**

---

## 13. Acceptance Criteria for FP-019 Closure

FP-019 can be marked **CLOSED** only when ALL of the following are true:

- [ ] Admin can start a Türkü video project from a türkü name.
- [ ] Story/scenario/storyboard/scene asset workflow stages are represented.
- [ ] Image batch preparation/integration is controlled and tracked.
- [ ] Admin can view internal progress and preview.
- [ ] All backend tests pass (0 failures).
- [ ] All frontend tests pass (0 failures).
- [ ] Endpoint inventory is documented and verified.
- [ ] Safety baseline is explicitly preserved.
- [ ] No publish/export/public delivery is introduced.
- [ ] No copyrighted audio/video is downloaded/copied/redistributed.
- [ ] Source confidence tracking is implemented.
- [ ] Copyright review flag exists on projects.
- [ ] Final closure doc exists.

---

## 14. Explicit Non-Goals

| Non-Goal | Reason |
|----------|--------|
| Final public video publishing | Deferred to future feature |
| Public video export/download | Not in FP-019 scope |
| CDN/signed/public URL delivery | Safety baseline |
| Copyrighted recording download/copy/redistribution | Legal/copyright constraint |
| Full production auth implementation | Deferred (FP-018 preparation only) |
| Payment/billing | Not in scope |
| Mobile app work | Not in scope |
| Schema/migration/config changes in Task 01 | Task 01 is documentation-only |
| Code changes in Task 01 | Task 01 is documentation-only |
| Full video rendering/composition | Deferred to future feature |
| Audio/music generation | Deferred to future feature |
| TTS (text-to-speech) integration | Deferred to future feature |
| Multi-language support | Single language (Turkish) for now |

---

## 15. Task 02 Readiness Checklist

### Proposed Endpoint(s)

| Method | Path | Action |
|--------|------|--------|
| POST | `/admin/turku-video-projects/bootstrap` | Create project |
| GET | `/admin/turku-video-projects/{id}` | Get project |
| GET | `/admin/turku-video-projects/{id}/progress` | Get progress |

### DTOs Required

| DTO | Direction | Key Fields |
|-----|-----------|------------|
| `TurkuVideoBootstrapRequest` | Request | turkuName (required), region, sourceNotes, mood, targetDurationMinutes, narrationStyle, visualStyle |
| `TurkuVideoBootstrapResponse` | Response | id, turkuName, workflowStage, createdAt, safeMessage |
| `TurkuVideoProjectResponse` | Response | id, turkuName, region, mood, workflowStage, safetyState, sourceConfidence, copyrightReviewRequired, createdAt, updatedAt |
| `TurkuVideoProgressResponse` | Response | id, currentStage, stagesCompleted (list), nextAvailableAction, safeProgressSummary |

### Service Responsibilities

- Validate bootstrap request (türkü name required, length limits, no HTML/credential patterns).
- Create project entity with DRAFT_CREATED stage.
- Set default safety state and confidence.
- Retrieve project by ID with safe field projection.
- Calculate progress from workflow stage.

### Validation Rules

| Field | Rule |
|-------|------|
| turkuName | Required, 2–200 chars, no HTML tags, no credential patterns |
| region | Optional, max 100 chars |
| sourceNotes | Optional, max 500 chars |
| mood | Optional, max 200 chars |
| targetDurationMinutes | Optional, 1–30 range |
| narrationStyle | Optional, max 100 chars |
| visualStyle | Optional, max 100 chars |

### Endpoint Inventory Expectation

After Task 02: **24 endpoints = 10 POST + 14 GET** (was 21 = 9 POST + 12 GET, +1 POST +2 GET).

### Tests Required

- Bootstrap happy path with valid türkü name.
- Bootstrap with all optional fields.
- Bootstrap validation: empty name, too long, HTML injection, credential patterns.
- Get project by ID: exists, not found.
- Get progress: each stage value.
- AdminActorResolver integration for bootstrap POST.
- CREATOR restriction for bootstrap.
- SYSTEM restriction for bootstrap.
- Endpoint inventory: 24 = 10 POST + 14 GET.
- No forbidden action types.
- No forbidden DTO fields.
- Safety checks: no publish/export, no public URL, no secrets.

### Docs Required

- `118-fp019-turku-video-bootstrap-implementation.md`

### Safety Checks

- [ ] No public/signed/CDN URLs in responses
- [ ] No raw/base64/provider data in DTOs
- [ ] No credentials in request/response
- [ ] No unsafe HTML accepted or returned
- [ ] AdminActorResolver integration verified
- [ ] SYSTEM/CREATOR restrictions verified
- [ ] Endpoint inventory documented
- [ ] No publish/export behavior
