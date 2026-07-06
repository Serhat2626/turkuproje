# FP-013 Final Closure / Operational Baseline

## 1. Status

| Field | Value |
|-------|-------|
| Feature | FP-013 — Review-Gated Orchestration Integration & Admin Preview Workflow |
| Status | **CLOSED** |
| Closure Date | 2026-07-06 |
| Closure Type | Documentation-only |
| Tasks Completed | 7 / 7 |
| Final Backend Baseline | 2142 tests, 0 failures |
| Final Frontend/Admin Baseline | 219 tests passing |
| Final Endpoint Count | 14 |
| Migrations Added | 0 |
| Schema Changes | 0 |

---

## 2. Feature Intent

FP-013 safely connects AI-generated content and media assets into the FP-012 human review
workflow. It ensures that:

- **Generated outputs are review-gated.** Every successfully generated reviewable entity
  (scenario, storyboard, platform package, content project, image asset) is automatically
  handed off to the review workflow with state `NEEDS_REVIEW`.
- **Admin preview is safe and internal.** Admin users can inspect safe metadata summaries
  of review targets without exposing provider URLs, storage keys, raw payloads, base64 data,
  or any sensitive infrastructure detail.
- **Internal approval remains distinct from publish/export.** `APPROVED_FOR_INTERNAL_USE`
  grants internal editorial approval only. No publish, export, public URL, signed URL, CDN
  delivery, or user-facing media library behavior exists.

---

## 3. Completed Task Timeline

### Task 01 — Combined Product / AI Platform / Engineering Design + Integration Plan

| Field | Value |
|-------|-------|
| Commit | `5780454` |
| Type | Documentation-only |
| Document | `docs/05-engineering/84-fp013-review-gated-orchestration-admin-preview-combined-design.md` |

Combined product, AI platform, and engineering design covering:
- Review handoff service design
- Safe admin preview DTO design
- Orchestration integration plan
- Admin preview API integration plan
- Safety boundaries and non-goals

---

### Task 02 — Review Handoff Service, Idempotency & Safe Audit Integration

| Field | Value |
|-------|-------|
| Backend Baseline | 1982 tests passing |

**Files added:**
- `ReviewHandoffSource.java` — Enum: sources that trigger review handoff
- `ReviewHandoffRequest.java` — Immutable request with `createSafe()` factory
- `ReviewHandoffResult.java` — Immutable result with safe fields only
- `ReviewHandoffException.java` — Safe exception (no provider/storage/config detail)
- `ReviewHandoffService.java` — Internal service creating/preserving review records
- `ReviewHandoffServiceTest.java` — Comprehensive handoff service tests

**Key behavior:**
- Idempotent handoff: repeated calls preserve existing state
- Terminal states (REJECTED) and active states (IN_REVIEW, CHANGES_REQUESTED) are preserved
- SYSTEM actor can create handoff but cannot approve/reject
- All text sanitized via `ReviewTextSafety`
- Safe audit trail via `ReviewAuditEventEntity`

---

### Task 03 — Safe Admin Preview DTO, Target Context Resolver & Preview Service Foundation

| Field | Value |
|-------|-------|
| Backend Baseline | 2018 tests passing |

**Files added:**
- `AdminPreviewMode.java` — Enum: TEXT_SUMMARY, SCENARIO_SUMMARY, STORYBOARD_SUMMARY, PACKAGE_SUMMARY, IMAGE_ASSET, UNAVAILABLE
- `AdminPreviewAvailability.java` — Enum: AVAILABLE, UNAVAILABLE, NOT_REVIEWABLE, MISSING_TARGET, UNSAFE_TARGET, STORAGE_UNAVAILABLE, PROVIDER_UNAVAILABLE
- `SafeAdminPreview.java` — Immutable preview DTO with safe fields only
- `AdminPreviewException.java` — Safe preview exception
- `ReviewTargetContextResolver.java` — Resolves target entity to safe preview context
- `AdminPreviewService.java` — Internal preview service (no storage/provider calls)
- `AdminPreviewServiceTest.java` — Comprehensive preview service tests

**Key behavior:**
- Preview returns metadata/availability only
- IMAGE_ASSET preview: label and availability indicator — no bytes, URL, base64
- Null/missing targets return safe unavailable response
- No provider or storage calls during preview

---

### Task 04 — Review Handoff + Safe Preview API Integration for Admin Review

| Field | Value |
|-------|-------|
| Commit | `0a72c1a` |
| Backend Endpoint Delta | +2 (12 → 14) |
| Frontend/Admin Baseline | 219 tests passing |
| Document | `docs/05-engineering/85-fp013-review-handoff-preview-api-integration.md` |

**New endpoints:**
- `POST /api/admin/reviews/handoff` — Internal admin handoff trigger
- `GET /api/admin/reviews/{targetType}/{targetId}/preview` — Safe admin preview

**Frontend files added:**
- `ReviewPreviewPanel.tsx` — Safe preview rendering component
- `reviewApi.ts` — API client (handoffReview, getPreview only)
- `reviewTypes.ts` — TypeScript types
- `ReviewConsole.test.ts` — Frontend test suite

**Key behavior:**
- Admin can trigger manual review handoff
- Admin can inspect safe preview of any review target
- Preview panel renders: safePreviewLabel, safeTitle, safeSummary, availability, currentReviewState
- IMAGE_ASSET preview: metadata/availability only — no img src, no URL, no bytes
- No `dangerouslySetInnerHTML` or `innerHTML` usage

---

### Task 05 — Review-Gated Orchestration Integration for Generated Content/Assets

| Field | Value |
|-------|-------|
| Commit | `9a375c8` |
| Backend Baseline | 2085 tests passing |
| Document | `docs/05-engineering/86-fp013-review-gated-orchestration-integration.md` |

**Files added:**
- `GenerationReviewHandoffAdapter.java` — Safe adapter wrapping ReviewHandoffService

**Files modified (integration):**
- `ScenarioGenerationService.java` — handoffScenario after generation success
- `StoryboardGenerationService.java` — handoffStoryboard after generation success
- `PlatformPackageGenerationService.java` — handoffPackage per package in loop
- `AiGenerationService.java` — handoffContentProject after draft persist
- `ImageGenerationOrchestrationService.java` — handoffImageAsset after asset creation
- `VisualAssetPlanGenerationService.java` — deferred with comment (no ReviewTargetType)

**Tests added:**
- `GenerationReviewHandoffAdapterTest.java` — 32 tests across 6 categories

**Key behavior:**
- Non-fatal handoff: generation succeeds even if handoff fails
- SYSTEM actor: cannot approve, reject, or perform human review
- 5 generation services wired; VisualAssetPlan explicitly deferred
- No new endpoints, migrations, or schema changes

---

### Task 06 — End-to-End Regression, Preview Contract Hardening & Deferred Target Guardrails

| Field | Value |
|-------|-------|
| Commit | `486d8f5` |
| Production Code Changed | None |
| Backend Baseline | 2142 tests passing |
| Document | `docs/05-engineering/87-fp013-end-to-end-review-gated-workflow-regression.md` |

**Tests added:**
- `FP013EndToEndReviewGatedWorkflowRegressionTest.java` — 57 tests across 8 categories:
  - A. End-to-end workflow per target type
  - A2. Terminal & active state preservation
  - A3. Preview after handoff safe response contract
  - B. Deferred VisualAssetPlan guardrails
  - C. API endpoint inventory regression
  - D. Safe payload regression — DTO field inspection
  - E. Failure behavior regression — non-fatal handoff
  - F. ReviewState enum safety — forbidden values
  - G. Handoff source to target type mapping completeness
  - H. Forbidden behavior — no publish/export/CDN/delivery

---

### Task 07 — Final Closure / Operational Baseline

| Field | Value |
|-------|-------|
| Type | Documentation-only |
| Document | `docs/05-engineering/88-fp013-final-closure-operational-baseline.md` (this document) |

---

## 4. Final Backend Baseline

| Metric | Value |
|--------|-------|
| Total tests | **2142** |
| Failures | **0** |
| Build result | **BUILD SUCCESS** |
| Admin review endpoints | **14** |
| POST endpoints | 8 |
| GET endpoints | 6 |
| Migrations added in FP-013 | 0 |
| Schema changes in FP-013 | 0 |
| Runtime config changes in FP-013 | 0 |

---

## 5. Final Frontend/Admin Baseline

| Metric | Value |
|--------|-------|
| Preview integration added | Task 04 |
| Test files | 11 |
| Total tests | **219** |
| Last frontend change | Task 04 |
| Task 06 frontend change | None |
| Preview panel | Safe — renders metadata/labels only |
| IMAGE_ASSET preview | Metadata/availability only — no bytes/URL |

---

## 6. Final Endpoint Inventory

### FP-012 Existing Admin Review Endpoints (12)

| # | Method | Path | Purpose |
|---|--------|------|---------|
| 1 | POST | `/api/admin/reviews/submit` | Submit for review |
| 2 | POST | `/api/admin/reviews/start` | Start review |
| 3 | POST | `/api/admin/reviews/changes` | Request changes |
| 4 | POST | `/api/admin/reviews/approve` | Approve for internal use |
| 5 | POST | `/api/admin/reviews/reject` | Reject |
| 6 | POST | `/api/admin/reviews/return-to-draft` | Return to draft |
| 7 | POST | `/api/admin/reviews/note` | Add review note |
| 8 | GET | `/api/admin/reviews/worklist` | Review worklist |
| 9 | GET | `/api/admin/reviews/{targetType}/{targetId}/state` | Review state summary |
| 10 | GET | `/api/admin/reviews/{targetType}/{targetId}/actions` | Available actions |
| 11 | GET | `/api/admin/reviews/{targetType}/{targetId}/timeline` | Audit timeline |
| 12 | GET | `/api/admin/reviews/{targetType}/{targetId}/context` | Target context |

### FP-013 New Endpoints (+2)

| # | Method | Path | Purpose |
|---|--------|------|---------|
| 13 | POST | `/api/admin/reviews/handoff` | Internal handoff trigger |
| 14 | GET | `/api/admin/reviews/{targetType}/{targetId}/preview` | Safe admin preview |

### Forbidden Endpoint Confirmation

| Forbidden Path | Present | Status |
|----------------|---------|--------|
| publish | ❌ | Safe |
| export | ❌ | Safe |
| public | ❌ | Safe |
| signed | ❌ | Safe |
| cdn | ❌ | Safe |
| provider | ❌ | Safe |
| storage-inspection | ❌ | Safe |
| raw | ❌ | Safe |
| base64 | ❌ | Safe |
| media-library | ❌ | Safe |
| video | ❌ | Safe |
| audio | ❌ | Safe |

---

## 7. Final Review Target Coverage

### CONTENT_PROJECT

| Field | Value |
|-------|-------|
| Handoff Source | `CONTENT_PROJECT_GENERATION` |
| Adapter Method | `handoffContentProject()` |
| Wired Service | `AiGenerationService` |
| Preview Mode | `TEXT_SUMMARY` |
| Safety | No raw AI response, no provider detail |

### SCENARIO

| Field | Value |
|-------|-------|
| Handoff Source | `SCENARIO_GENERATION` |
| Adapter Method | `handoffScenario()` |
| Wired Service | `ScenarioGenerationService` |
| Preview Mode | `SCENARIO_SUMMARY` |
| Safety | No beat raw content in preview, safe title/summary |

### STORYBOARD

| Field | Value |
|-------|-------|
| Handoff Source | `STORYBOARD_GENERATION` |
| Adapter Method | `handoffStoryboard()` |
| Wired Service | `StoryboardGenerationService` |
| Preview Mode | `STORYBOARD_SUMMARY` |
| Safety | No scene raw content in preview, safe title/summary |

### PACKAGE

| Field | Value |
|-------|-------|
| Handoff Source | `PACKAGE_GENERATION` |
| Adapter Method | `handoffPackage()` |
| Wired Service | `PlatformPackageGenerationService` |
| Preview Mode | `PACKAGE_SUMMARY` |
| Safety | Per-package handoff (4 platforms), no platform credential exposure |

### IMAGE_ASSET

| Field | Value |
|-------|-------|
| Handoff Source | `IMAGE_GENERATION` / `STORAGE_HANDOFF` |
| Adapter Method | `handoffImageAsset()` |
| Wired Service | `ImageGenerationOrchestrationService` |
| Preview Mode | `IMAGE_ASSET` |
| Safety | Metadata/availability only — no bytes, no URL, no base64, no provider URL, no storage key |

---

## 8. Final Handoff Source Coverage

| Source | Target Type | Wired |
|--------|------------|-------|
| `CONTENT_PROJECT_GENERATION` | CONTENT_PROJECT | ✅ |
| `SCENARIO_GENERATION` | SCENARIO | ✅ |
| `STORYBOARD_GENERATION` | STORYBOARD | ✅ |
| `PACKAGE_GENERATION` | PACKAGE | ✅ |
| `IMAGE_GENERATION` | IMAGE_ASSET | ✅ |
| `STORAGE_HANDOFF` | IMAGE_ASSET | ✅ |

**Forbidden source values confirmed absent:**
No `PUBLISH`, `EXPORT`, `CDN`, `PUBLIC_DELIVERY`, `SIGNED_URL`, `READY_FOR_PUBLISH` values exist.

---

## 9. Final State Model

```
DRAFT → AI_GENERATED → NEEDS_REVIEW → IN_REVIEW → CHANGES_REQUESTED
                                                  → APPROVED_FOR_INTERNAL_USE
                                                  → REJECTED (terminal)
```

### FP-013 Handoff Behavior

| State | Handoff Behavior |
|-------|-----------------|
| (new) | Creates review record with `NEEDS_REVIEW` |
| `NEEDS_REVIEW` | Preserved — not overwritten |
| `IN_REVIEW` | Preserved — not overwritten |
| `CHANGES_REQUESTED` | Preserved — not overwritten |
| `APPROVED_FOR_INTERNAL_USE` | Preserved — not overwritten |
| `REJECTED` | Preserved — terminal state |

### Critical State Rules

- `AI_GENERATED` does **NOT** imply approval
- `APPROVED_FOR_INTERNAL_USE` is internal editorial approval only — **NOT** publish/export
- `REJECTED` is terminal for the current version
- `READY_FOR_PUBLISH`, `COMPLETED`, `PUBLISHED`, `EXPORTED` do **NOT** exist
- No automatic transitions to `APPROVED_FOR_INTERNAL_USE`

---

## 10. Final Actor Model

| Role | Can Handoff | Can Approve | Can Reject | Can Review |
|------|-------------|-------------|------------|------------|
| `CREATOR` | No | No | No | No |
| `REVIEWER` | No | Yes | Yes | Yes |
| `ADMIN` | Yes (manual) | Yes | Yes | Yes |
| `SYSTEM` | Yes (auto) | **No** | **No** | **No** |

### Critical Actor Rules

- `SYSTEM` can create internal handoff (generation completion)
- `SYSTEM` **cannot** approve, reject, or perform human review actions
- `CREATOR` **cannot** self-approve
- `actorRole` request field is **not** authentication — production auth integration is deferred
- All actor references are safe strings (e.g., `system:scenario-generation`)

---

## 11. Admin Preview Contract

### SafeAdminPreview Fields

| Field | Type | Description |
|-------|------|-------------|
| `targetType` | ReviewTargetType | What is being previewed |
| `targetId` | UUID | Entity identifier |
| `previewMode` | AdminPreviewMode | How preview is rendered |
| `availability` | AdminPreviewAvailability | Whether preview is available |
| `previewAvailable` | boolean | Quick availability check |
| `safeTitle` | String | Sanitized title |
| `safeSummary` | String | Sanitized summary |
| `safeMessage` | String | Sanitized message |
| `safeReasonCode` | String | Sanitized reason code |
| `safePreviewLabel` | String | UI display label |
| `safeThumbnailAvailable` | boolean | Thumbnail availability indicator |
| `currentReviewState` | ReviewState | Current review state |
| `updatedAt` | LocalDateTime | Last update timestamp |

### Forbidden Fields Confirmed Absent

`providerUrl`, `storageKey`, `bucket`, `objectPath`, `signedUrl`, `publicUrl`, `cdnUrl`,
`rawPayload`, `base64`, `secret`, `apiKey`, `accessKey`, `token`, `stackTrace`,
`exceptionClass`, `bytes`, `imageBytes`, `imageUrl`, `imageData`, `rawContent`, `rawImageBytes`

### IMAGE_ASSET Preview

- Returns metadata and availability indicator only
- No actual image bytes
- No base64 encoded data
- No public URL
- No signed URL
- No provider URL
- No storage key
- `safeThumbnailAvailable` indicates availability without exposing storage detail

---

## 12. Orchestration Integration Baseline

### GenerationReviewHandoffAdapter

| Method | Target Type | Source |
|--------|------------|--------|
| `handoffScenario()` | SCENARIO | SCENARIO_GENERATION |
| `handoffStoryboard()` | STORYBOARD | STORYBOARD_GENERATION |
| `handoffPackage()` | PACKAGE | PACKAGE_GENERATION |
| `handoffImageAsset()` | IMAGE_ASSET | IMAGE_GENERATION |
| `handoffContentProject()` | CONTENT_PROJECT | CONTENT_PROJECT_GENERATION |

### Integration Properties

- **Non-fatal:** Generation succeeds even if handoff fails (try-catch, returns null)
- **Idempotent:** Repeated handoff preserves existing state
- **Sanitized:** All text passes through `ReviewTextSafety.sanitize()` via `createSafe()`
- **SYSTEM actor:** Cannot approve, reject, or perform human review
- **Safe failure:** Failures logged at WARN, no stack trace in API, no unsafe detail
- **Generated outputs enter review but are NOT approved**

---

## 13. VisualAssetPlan Deferred Baseline

| Check | Status |
|-------|--------|
| `ReviewTargetType.VISUAL_ASSET_PLAN` exists | ❌ Does not exist |
| `ReviewHandoffSource.VISUAL_ASSET_PLAN_GENERATION` exists | ❌ Does not exist |
| `AdminPreviewMode.VISUAL_ASSET_PLAN` exists | ❌ Does not exist |
| `GenerationReviewHandoffAdapter.handoffVisualAssetPlan()` exists | ❌ Does not exist |
| Fake targetId created for VisualAssetPlan | ❌ Not created |
| Unsafe handoff attempted for VisualAssetPlan | ❌ Not attempted |
| Preview endpoint path for VisualAssetPlan | ❌ Does not exist |
| Publish/export for VisualAssetPlan | ❌ Does not exist |

**Rationale:** VisualAssetPlan generates image prompt PLANS (text), not actual images.
Image generation handoff (`IMAGE_ASSET`) is handled at image generation completion.
Future integration requires an explicit `ReviewTargetType` and product decision.

---

## 14. Safety Baseline

FP-013 did **NOT** introduce:

| Category | Status |
|----------|--------|
| Automatic approval | ❌ Not introduced |
| Generic status update | ❌ Not introduced |
| Publish / export | ❌ Not introduced |
| Public URL delivery | ❌ Not introduced |
| Signed URL delivery | ❌ Not introduced |
| CDN / public delivery | ❌ Not introduced |
| User-facing media library | ❌ Not introduced |
| Video / audio processing | ❌ Not introduced |
| Provider URL exposure/persistence/logging | ❌ Not introduced |
| Storage key exposure/logging | ❌ Not introduced |
| Bucket/path sensitive exposure | ❌ Not introduced |
| Raw provider/storage payload exposure | ❌ Not introduced |
| Base64 DB/DTO/log/API response | ❌ Not introduced |
| Secret / API key / config exposure | ❌ Not introduced |
| Stack trace display | ❌ Not introduced |
| Unsafe HTML rendering | ❌ Not introduced |
| Production auth integration | ❌ Not introduced |
| New migration / schema change | ❌ Not introduced |
| READY_FOR_PUBLISH state | ❌ Does not exist |
| COMPLETED state | ❌ Does not exist |
| PUBLISHED state | ❌ Does not exist |
| EXPORTED state | ❌ Does not exist |

---

## 15. Operational Behavior

| Behavior | Description |
|----------|-------------|
| Generated outputs become review candidates | ✅ Automatic handoff to NEEDS_REVIEW |
| Admin users can inspect safe preview | ✅ Via `/api/admin/reviews/{type}/{id}/preview` |
| Reviewers must still act manually | ✅ No automatic approval path |
| Internal approval does not publish | ✅ APPROVED_FOR_INTERNAL_USE is internal only |
| No user-facing delivery is created | ✅ No public URL, signed URL, CDN |
| Storage/provider details remain hidden | ✅ No exposure in DTOs, logs, or API responses |
| Failures are safe/non-fatal | ✅ Generation succeeds even if handoff fails |

---

## 16. Test Baseline

| Task | Backend Tests | Frontend Tests | Notes |
|------|--------------|----------------|-------|
| Task 02 | 1982 passing | — | ReviewHandoffService baseline |
| Task 03 | 2018 passing | — | AdminPreviewService baseline |
| Task 04 | — | 219 passing | Admin preview UI baseline |
| Task 05 | 2085 passing | — | Orchestration integration baseline |
| Task 06 | **2142 passing** | — | Final regression baseline |
| Task 07 | **2142 passing** *(not rerun)* | **219 passing** *(not rerun)* | Documentation-only |

Task 07 is documentation-only. Tests were not rerun because no code, test, frontend,
migration, or config files were changed.

---

## 17. Known Deferred Items

| Item | Priority | Blocked By |
|------|----------|------------|
| VisualAssetPlan review integration | Medium | ReviewTargetType decision |
| Production auth integration | High | Auth infrastructure |
| Publish / export feature | Future | Product decision + new feature |
| Public / signed URL feature | Future | Publish feature |
| CDN / public delivery feature | Future | Publish feature |
| User-facing media library | Future | Product decision |
| Richer preview rendering | Low | Beyond safe metadata/summary |
| Video / audio processing | Future | Out of scope |

---

## 18. Rollback / Operational Notes

- **Orchestration handoff:** Can be disabled at `GenerationReviewHandoffAdapter` level
  by removing adapter injection from generation services. No DB rollback required.
- **Admin preview API:** Can be disabled by removing the two FP-013 endpoints from
  `AdminReviewController`. No migration rollback needed.
- **No DB rollback required:** FP-013 added zero migrations. All review records use
  the FP-012 schema.
- **No public delivery rollback:** No public delivery exists to roll back.
- **Admin preview remains internal only:** No public-facing preview endpoint exists.

---

## 19. Final Closure Statement

> **FP-013 — Review-Gated Orchestration Integration & Admin Preview Workflow is CLOSED.**
>
> The final baseline is stable with 2142 backend tests and 219 frontend tests passing.
> All 5 reviewable target types are covered with automated review handoff.
> Admin preview is safe and internal. No publish/export/CDN/public delivery behavior exists.
>
> **Recommended next feature:**
>
> **FP-014 — Review Decision Execution & Internal Approval Follow-through**
>
> This feature would define what happens after `APPROVED_FOR_INTERNAL_USE`:
> internal content finalization, version locking, or preparation for a future
> publish/export feature — while still not implementing actual publish/export
> unless explicitly scoped and approved.
