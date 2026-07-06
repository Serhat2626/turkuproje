# FP-016 — Controlled Pipeline Aggregation & Internal Workflow Visibility — Combined Design

## 1. Status

| Field | Value |
|-------|-------|
| Feature | FP-016 — Controlled Pipeline Aggregation & Internal Workflow Visibility |
| Status | **PLANNED / DESIGN ONLY** |
| Task | Task 01 — Combined Product / AI Platform / Engineering Design |
| Type | Documentation-only |
| Runtime Changes | None — no backend, frontend, test, migration, schema, config, or endpoint changes |

---

## 2. Background

FP-016 builds on the internal workflow capabilities established in FP-012 through FP-015:

| Feature | Capability | Key Endpoints |
|---------|-----------|---------------|
| **FP-012** | Human review workflow, review actions, state machine, audit trail, worklist | 7 POST + 5 GET |
| **FP-013** | Review-gated orchestration, safe admin preview, handoff | +1 POST + 1 GET |
| **FP-014** | Internal approval readiness, safe next actions, approved inventory listing | +2 GET |
| **FP-015** | Internal revision request, approved inventory UI, revision panel | +1 POST |
| **Total** | 17 endpoints | 9 POST + 8 GET |

The platform now has individual capabilities to:
- Submit, review, approve, reject, return-to-draft, add notes, handoff
- Preview safe content summaries
- Check internal readiness per target
- List approved internal inventory
- Request internal revision with audited note

**What is missing**: A single aggregated view that shows where every artifact stands across the entire pipeline — from generation through review to internal readiness. Admins currently must check each target individually across multiple panels.

---

## 3. Product Intent

### Problem
Admins managing content through the Anadolu Atlası pipeline lack a unified internal view. For a single content project with scenario, storyboard, package, and image assets, an admin must navigate to each target's review state, preview, readiness, and revision status individually. This slows decision-making and increases the risk of overlooked blockers.

### Solution
An **internal-only pipeline overview dashboard** that aggregates safe workflow status across all generated artifacts for a content project or across all targets of a given type.

### Key Use Cases

1. **Pipeline overview** — Admin sees all targets with their current pipeline stage, review state, preview availability, internal readiness, and revision status in a single view.

2. **Blocker identification** — Admin identifies targets that are blocked (e.g., review pending, changes requested, revision requested, preview unavailable) without navigating to each individually.

3. **Readiness summary** — Admin sees how many targets are internally ready vs. pending vs. blocked, enabling prioritization.

4. **Revision tracking** — Admin sees which approved targets have had internal revisions requested and their current state.

### Not a Dashboard for External Users
This is an **internal operational tool** for REVIEWER/ADMIN actors. It is not a public-facing analytics dashboard, not a publishing queue, and not a media library.

---

## 4. Explicit Non-Goals

FP-016 does **NOT** include:

| Non-Goal | Rationale |
|----------|-----------|
| Publish/export | Not in scope for current phase |
| Public URL generation | Requires publish/export feature |
| Signed URL generation | Requires publish/export feature |
| CDN/public delivery | Requires publish/export feature |
| User-facing media library | Requires publish/export and CDN |
| Video/audio pipeline | Out of scope for current phase |
| Automatic approval | Safety baseline violation |
| Automatic generation/regeneration | Safety baseline violation |
| Generic status update | Safety baseline violation |
| Provider/storage inspection | Safety baseline violation |
| Raw payload/base64/bytes exposure | Safety baseline violation |
| Secret/token exposure | Safety baseline violation |
| Stack trace display | Safety baseline violation |
| Unsafe HTML rendering | Safety baseline violation |
| New database tables (Task 01) | Derived aggregation only |
| Endpoint/code implementation (Task 01) | Design document only |
| Mutation/write operations from pipeline view | Read-only aggregation |

---

## 5. Pipeline Aggregation Concept

### Safe Aggregate View

The pipeline aggregation produces a safe, read-only view for each target by combining data from existing services:

```
┌─────────────────────────────────────────────────────────┐
│              SafePipelineAggregateItem                   │
├─────────────────────────────────────────────────────────┤
│ targetType          : ReviewTargetType                  │
│ targetId            : UUID                              │
│ pipelineStage       : SafePipelineStage                 │
│ reviewState         : ReviewState | null                │
│ previewAvailability : AdminPreviewAvailability | null    │
│ readinessAvailability: InternalApprovalReadinessAvail.  │
│ revisionRequested   : boolean                           │
│ safeTitle           : String | null                     │
│ safeSummary         : String | null                     │
│ safeBlockingReasons : List<String>                      │
│ safeNextInternalHints: List<String>                     │
│ updatedAt           : LocalDateTime | null              │
└─────────────────────────────────────────────────────────┘
```

### Target Types Aggregated

| Target Type | Source |
|-------------|--------|
| CONTENT_PROJECT | Project entity |
| SCENARIO | Scenario generation |
| STORYBOARD | Storyboard generation |
| PACKAGE | Platform package |
| IMAGE_ASSET | Visual asset plan / image generation |

### Data Sources (Read-Only)

| Field | Source Service | Method |
|-------|---------------|--------|
| reviewState | ReviewApplicationService | getState() |
| previewAvailability | AdminPreviewService | getPreview() |
| readinessAvailability | InternalApprovalReadinessService | checkReadiness() |
| revisionRequested | ReviewApplicationService | state == CHANGES_REQUESTED after internal approval |
| safeTitle | ReviewApplicationService / target context | getTargetContext() |
| safeSummary | ReviewApplicationService / target context | getTargetContext() |
| safeBlockingReasons | Derived from review + readiness | Computed |
| safeNextInternalHints | Derived from readiness | Computed |

### Safe Blocking Reasons (Examples)

| Reason | Condition |
|--------|-----------|
| `REVIEW_PENDING` | reviewState == NEEDS_REVIEW |
| `REVIEW_IN_PROGRESS` | reviewState == IN_REVIEW |
| `CHANGES_REQUESTED` | reviewState == CHANGES_REQUESTED |
| `REJECTED` | reviewState == REJECTED |
| `PREVIEW_UNAVAILABLE` | previewAvailability != AVAILABLE |
| `READINESS_NOT_APPROVED` | readinessAvailability == NOT_APPROVED |
| `READINESS_UNAVAILABLE` | readinessAvailability == UNAVAILABLE |
| `TARGET_MISSING` | readinessAvailability == MISSING_TARGET |

All blocking reasons are safe string enums — no provider detail, no storage path, no stack trace.

---

## 6. Safe Pipeline Stage Taxonomy

### Internal-Only Stages

| Stage | Meaning | Derived From |
|-------|---------|-------------|
| `GENERATED` | Artifact exists, no review submitted | reviewState == DRAFT |
| `REVIEW_PENDING` | Review submitted, awaiting reviewer | reviewState == NEEDS_REVIEW |
| `IN_REVIEW` | Reviewer actively reviewing | reviewState == IN_REVIEW |
| `CHANGES_REQUESTED` | Reviewer requested changes | reviewState == CHANGES_REQUESTED, not post-approval |
| `INTERNALLY_APPROVED` | Approved for internal use | reviewState == APPROVED_FOR_INTERNAL_USE |
| `INTERNAL_READY` | Approved + readiness == READY | APPROVED + readinessAvailability == READY |
| `INTERNAL_REVISION_REQUESTED` | Approved then revision requested | reviewState == CHANGES_REQUESTED, after prior APPROVED |
| `REJECTED` | Reviewer rejected | reviewState == REJECTED |
| `UNAVAILABLE` | Target missing or unsafe | readinessAvailability in (MISSING_TARGET, UNSAFE_TARGET, UNAVAILABLE) |

### Stage Derivation Logic

```
if (target missing)               → UNAVAILABLE
if (reviewState == null)           → GENERATED
if (reviewState == DRAFT)          → GENERATED
if (reviewState == NEEDS_REVIEW)   → REVIEW_PENDING
if (reviewState == IN_REVIEW)      → IN_REVIEW
if (reviewState == REJECTED)       → REJECTED
if (reviewState == APPROVED_FOR_INTERNAL_USE)
  if (readinessAvailability == READY) → INTERNAL_READY
  else                                → INTERNALLY_APPROVED
if (reviewState == CHANGES_REQUESTED)
  if (wasApprovedBefore)              → INTERNAL_REVISION_REQUESTED
  else                                → CHANGES_REQUESTED
```

### Forbidden Stages — NOT Included

| Forbidden Stage | Reason |
|-----------------|--------|
| `READY_FOR_PUBLISH` | No publish feature |
| `PUBLISHABLE` | No publish feature |
| `PUBLISHED` | No publish feature |
| `EXPORTABLE` | No export feature |
| `EXPORTED` | No export feature |
| `CDN_READY` | No CDN delivery |
| `PUBLIC_AVAILABLE` | No public delivery |
| `MEDIA_LIBRARY_READY` | No media library |

---

## 7. Backend Design Direction

### Service Architecture

```
┌─────────────────────────────────────────┐
│       PipelineAggregationService        │
│  (derived, read-only, no mutation)      │
├─────────────────────────────────────────┤
│ getOverview(filters?)                   │
│   → List<SafePipelineAggregateItem>     │
│                                         │
│ getItemDetail(targetType, targetId)     │
│   → SafePipelineAggregateItem           │
├─────────────────────────────────────────┤
│ Dependencies (read-only injection):     │
│ ├── ReviewApplicationService            │
│ ├── AdminPreviewService                 │
│ ├── InternalApprovalReadinessService    │
│ └── (no repositories directly)          │
└─────────────────────────────────────────┘
```

### Design Principles

| Principle | Detail |
|-----------|--------|
| Read-only | No mutations, no state changes, no write operations |
| Derived | All data derived from existing services — no new database table |
| No external calls | No provider API calls, no storage API calls |
| Safe output | No URL, storage key, provider detail, base64, raw payload, secret |
| Cacheable | Results can be cached short-term since they derive from existing state |
| Fail-safe | Missing targets produce UNAVAILABLE, not exceptions |
| No new repository | Reuses existing service layer |

### DTOs

**SafePipelineAggregateItem** (response DTO):
- `targetType: ReviewTargetType`
- `targetId: UUID`
- `pipelineStage: SafePipelineStage`
- `reviewState: ReviewState` (nullable)
- `previewAvailable: boolean`
- `readinessAvailability: InternalApprovalReadinessAvailability` (nullable)
- `revisionRequested: boolean`
- `safeTitle: String` (nullable)
- `safeSummary: String` (nullable)
- `safeBlockingReasons: List<String>`
- `safeNextInternalHints: List<String>`
- `updatedAt: LocalDateTime` (nullable)

**SafePipelineOverview** (wrapper DTO):
- `items: List<SafePipelineAggregateItem>`
- `totalCount: int`
- `readyCount: int`
- `pendingCount: int`
- `blockedCount: int`
- `safeMessage: String` (nullable)
- `updatedAt: LocalDateTime`

No forbidden fields: no publicUrl, signedUrl, cdnUrl, providerUrl, storageKey, rawPayload, base64, bytes, secret, apiKey, stackTrace, publishStatus, exportStatus.

### `wasApprovedBefore` Derivation

The `INTERNAL_REVISION_REQUESTED` stage requires knowing if a target was previously approved. This can be derived from:
1. Audit timeline — check for `INTERNAL_APPROVAL_GRANTED` event before current `CHANGES_REQUESTED` state
2. This is read-only (no mutation needed)

---

## 8. API Design Direction

### Proposed Future Endpoints (GET only)

| Method | Path | Purpose | Task |
|--------|------|---------|------|
| GET | `/api/admin/pipeline/overview` | Aggregated pipeline overview with optional filters | Task 03 |
| GET | `/api/admin/pipeline/{targetType}/{targetId}` | Single target pipeline detail | Task 03 |

### Query Parameters (overview)

| Parameter | Type | Default |
|-----------|------|---------|
| `targetType` | ReviewTargetType | all types |
| `stage` | SafePipelineStage | all stages |
| `reviewState` | ReviewState | all states |
| `limit` | int | 50 |

### Confirmed API Constraints

| Constraint | Status |
|-----------|--------|
| No POST endpoints in FP-016 | ✅ Unless explicitly scoped in a later task |
| No PUT/DELETE endpoints | ✅ |
| No mutation from pipeline view | ✅ |
| No publish/export endpoint | ✅ |
| No public/signed URL endpoint | ✅ |
| No provider/storage endpoint | ✅ |

### Endpoint Count Projection

After FP-016:
- **19 endpoints** (17 existing + 2 new GET)
- 9 POST + 10 GET
- 0 PUT, 0 DELETE

---

## 9. Frontend/Admin Design Direction

### Pipeline Overview Panel

```
┌────────────────────────────────────────────────────────┐
│  Dahili Pipeline Görünümü (Sadece İç Kullanım)        │
├────────────────────────────────────────────────────────┤
│  Filtreler: [Hedef Tipi ▼] [Pipeline Aşaması ▼]      │
│             [İnceleme Durumu ▼]                        │
├────────────────────────────────────────────────────────┤
│  Özet: Hazır: 5  Bekliyor: 8  Engelli: 3              │
├────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────┐  │
│  │ [SCENARIO] Çanakkale Türküsü Senaryosu           │  │
│  │ Aşama: INTERNAL_READY  │ İnceleme: Dahili Onay  │  │
│  │ Önizleme: ✓  │ Hazırlık: Dahili Hazır           │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ [IMAGE_ASSET] Sahne 3 Görseli                    │  │
│  │ Aşama: REVIEW_PENDING  │ İnceleme: İnceleme Bek │  │
│  │ Engel: İnceleme bekleniyor                       │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ [STORYBOARD] Hikâye Tahtası                      │  │
│  │ Aşama: INTERNAL_REVISION_REQUESTED               │  │
│  │ Engel: Dahili revizyon istendi                    │  │
│  └──────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────┤
│  Seçili öğe detayında → Önizleme / Hazırlık / Revizyon│
│  panellerine bağlantılar (mevcut paneller)             │
└────────────────────────────────────────────────────────┘
```

### UI Design Principles

| Principle | Detail |
|-----------|--------|
| Internal-only labels | "Dahili Pipeline Görünümü", "Sadece İç Kullanım" |
| No publish/export buttons | Read-only aggregation view |
| Safe text rendering | safeText() for all displayed content |
| No unsafe HTML | No dangerouslySetInnerHTML, no innerHTML |
| No image rendering | No img src, no base64, no URL display |
| Filterable | By targetType, pipelineStage, reviewState |
| Summary counts | Ready / Pending / Blocked counts |
| Detail integration | Links to existing preview, readiness, revision panels |
| Loading/error/empty states | Standard safe states for all sections |

### Component Plan

| Component | Purpose |
|-----------|---------|
| `PipelineOverviewPanel` | Main overview with filters, summary, item list |
| `PipelineItemCard` | Individual target card with stage, review, readiness |
| `PipelineSummaryBar` | Aggregated counts (ready, pending, blocked) |

### Integration with Existing Components

Selected pipeline item opens existing panels (no new mutation panels):
- `ReviewPreviewPanel` — safe preview
- `InternalApprovalReadinessPanel` — readiness detail
- `InternalRevisionRequestPanel` — revision request (if eligible)
- `ReviewConsole` — review actions (if navigated from worklist)

---

## 10. Observability / Audit Direction

| Concern | Approach |
|---------|----------|
| Metrics | Safe counts only: pipeline_overview_requests_total, pipeline_items_by_stage |
| Labels | Safe enum labels only (targetType, pipelineStage) — no high-cardinality |
| Logging | Safe structured logs: targetType, targetId, pipelineStage — no provider/storage/secret |
| Audit | No new audit events — pipeline view is read-only, no mutations |
| Forbidden in metrics/logs | providerUrl, storageKey, publicUrl, signedUrl, rawPayload, base64, secret, apiKey, stackTrace |

---

## 11. Failure Behavior

| Failure | Behavior |
|---------|----------|
| Missing target | pipelineStage = UNAVAILABLE, safeBlockingReasons = ["TARGET_MISSING"] |
| Review service unavailable | Partial result with safeBlockingReasons = ["REVIEW_UNAVAILABLE"] |
| Preview service unavailable | previewAvailable = false, no error propagation |
| Readiness service unavailable | readinessAvailability = UNAVAILABLE |
| Partial aggregation | Return available items, mark failed items as UNAVAILABLE |
| Exception in aggregation | Safe error response with safeMessage, no stack trace, no provider/storage leak |
| Empty result | Return empty list with totalCount = 0, no error |

---

## 12. Test Strategy

### Service Tests (Task 02)
- Aggregation produces correct SafePipelineStage for each ReviewState combination
- Missing targets produce UNAVAILABLE
- Partial failures degrade gracefully
- revisionRequested correctly derived from audit history
- safeBlockingReasons correctly populated
- No forbidden fields in output (publicUrl, signedUrl, storageKey, etc.)
- No mutation methods exist
- No repository injection

### API Tests (Task 03)
- GET overview returns correct items
- Filters work (targetType, stage, reviewState)
- No POST/PUT/DELETE endpoints on pipeline controller
- Endpoint count regression (17 → 19)
- No forbidden fields in response
- Error responses are safe (no stack trace, no provider detail)

### Frontend Tests (Task 04)
- Panel renders with safe text
- No dangerouslySetInnerHTML/innerHTML
- No img src/base64/storage/provider rendering
- Filters functional
- Summary counts displayed
- Loading/error/empty states present
- No publish/export buttons
- Integration with existing panels

### E2E Regression (Task 05)
- Full endpoint inventory regression
- All forbidden field checks across all components
- No mutation from pipeline endpoints
- Standing safety baseline preserved

---

## 13. FP-016 Task Sequence

| Task | Title | Type | Scope |
|------|-------|------|-------|
| **Task 01** | Combined Design (this document) | Doc-only | Design |
| **Task 02** | Safe Pipeline Aggregation DTO + Service Foundation | Backend | DTOs, service, service tests |
| **Task 03** | Pipeline Overview Admin API | Backend | Controller, API tests, endpoint inventory |
| **Task 04** | Pipeline Overview Admin UI | Frontend | Components, types, API client, frontend tests |
| **Task 05** | E2E Pipeline Visibility Regression & Safety Hardening | Both | Integration tests, safety regression, hardening |
| **Task 06** | Final Closure / Operational Baseline | Doc-only | Closure document |

### Task Dependencies

```
Task 01 (design) → Task 02 (service) → Task 03 (API) → Task 04 (UI)
                                                              ↓
                                                        Task 05 (regression)
                                                              ↓
                                                        Task 06 (closure)
```

---

## 14. Final Recommendation

**Proceed with Task 02 — Safe Pipeline Aggregation DTO + Service Foundation.**

Task 02 should deliver:
- `SafePipelineStage` enum
- `SafePipelineAggregateItem` DTO (immutable, no forbidden fields)
- `SafePipelineOverview` DTO (wrapper with counts)
- `PipelineAggregationService` (read-only, derived from existing services)
- Comprehensive service tests (aggregation, stage derivation, blocking reasons, failure handling, forbidden field checks)
- Engineering doc

No migration, no schema change, no endpoint, no frontend changes in Task 02.

---

## Safety Baseline Confirmation

This design preserves the standing Anadolu Atlası safety baseline:

| Constraint | FP-016 Status |
|-----------|---------------|
| No publish/export | ✅ Preserved |
| No public URL | ✅ Preserved |
| No signed URL | ✅ Preserved |
| No CDN/public delivery | ✅ Preserved |
| No user-facing media library | ✅ Preserved |
| No automatic approval | ✅ Preserved |
| No generic status update | ✅ Preserved |
| No provider/storage exposure | ✅ Preserved |
| No raw/base64/secret exposure | ✅ Preserved |
| No unsafe HTML | ✅ Preserved |
| No migration/schema/config (Task 01) | ✅ Preserved |

---

## Baselines at FP-016 Entry

| Metric | Value |
|--------|-------|
| Backend tests | 2309 passing, 0 failures |
| Frontend/admin tests | 387 passing, 0 failures |
| Admin review endpoints | 17 (9 POST + 8 GET) |
| Closed features | FP-001 through FP-015 |
| Latest closure doc | docs/05-engineering/98-fp015-final-closure-operational-baseline.md |
