# FP-016 Task 04 — Pipeline Overview Admin UI

## Status

| Field | Value |
|-------|-------|
| Task | FP-016 Task 04 — Pipeline Overview Admin UI |
| Type | Frontend/admin production + frontend tests |
| Endpoints Changed | None — endpoint count remains 19 (9 POST + 10 GET) |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |
| Backend Changes | None |

---

## Scope

Frontend/admin-web only. Adds pipeline visibility UI components, types, API methods, and tests.
No backend changes. No new endpoints. No migrations.

---

## Types Added

### SafePipelineStage (union type)
9 internal-only values: GENERATED, REVIEW_PENDING, IN_REVIEW, CHANGES_REQUESTED, INTERNALLY_APPROVED, INTERNAL_READY, INTERNAL_REVISION_REQUESTED, REJECTED, UNAVAILABLE

### SafePipelineAggregateItem (interface)
Fields: targetType, targetId, pipelineStage, reviewState, previewAvailable, readinessAvailability, revisionRequested, safeTitle, safeSummary, safeBlockingReasons, safeNextInternalHints, updatedAt

### SafePipelineOverview (interface)
Fields: items, totalCount, countsByStage, countsByTargetType, safeWarnings, updatedAt

---

## API Methods Added

| Method | Endpoint | HTTP | Purpose |
|--------|----------|------|---------|
| getPipelineOverview | /admin/pipeline/overview | GET | Overview with filters |
| getPipelineDetail | /admin/pipeline/{targetType}/{targetId} | GET | Single target detail |

Total API functions: 19 (was 17, +2 new)

---

## UI Components

### PipelineOverviewPanel (new)
- Internal-only pipeline visibility panel
- Turkish safe labels throughout
- Disclaimer: "Sadece İç Kullanım — Bu görünüm yayın/export değildir"
- Summary bar with stage counts and total
- Filter controls: targetType dropdown, stage dropdown
- Item cards with: target type label, safe title, stage badge, preview badge, revision badge, summary, blocking reasons, next internal hints
- Loading/error/empty states
- Safe text rendering (safeText function)
- No dangerouslySetInnerHTML, no innerHTML, no img src
- No forbidden fields in source

### ReviewWorklist Integration
- PipelineOverviewPanel added above the approved inventory panel
- All existing panels preserved: ReviewConsole, ReviewPreviewPanel, InternalApprovalReadinessPanel, ApprovedInternalInventoryPanel, InternalRevisionRequestPanel

---

## Safe Labels

| Key | Turkish Label |
|-----|---------------|
| GENERATED | Üretildi |
| REVIEW_PENDING | İnceleme Bekliyor |
| IN_REVIEW | İnceleniyor |
| CHANGES_REQUESTED | Değişiklik İstendi |
| INTERNALLY_APPROVED | Dahili Onaylandı |
| INTERNAL_READY | Dahili Hazır |
| INTERNAL_REVISION_REQUESTED | Revizyon İstendi |
| REJECTED | Reddedildi |
| UNAVAILABLE | Kullanılamıyor |
| CONTENT_PROJECT | İçerik Projesi |
| SCENARIO | Senaryo |
| STORYBOARD | Hikâye Tahtası |
| PACKAGE | Platform Paketi |
| IMAGE_ASSET | Görsel Varlık |

---

## Filter Behavior

- Both filters optional, default to "Tümü"
- Filters passed as query params to API
- Reload on filter change
- Safe empty state for no results

---

## Loading/Error/Empty Behavior

| State | Behavior |
|-------|----------|
| Loading | "Yükleniyor…" indicator |
| Error | Safe error message with safeText() |
| Empty | "Pipeline öğesi bulunamadı." |
| UNAVAILABLE stage | Kullanılamıyor badge |

---

## Safety Boundaries

- No dangerouslySetInnerHTML / innerHTML / __html
- Safe text rendering via safeText()
- No img src / image URL / base64 image
- No forbidden fields in source: publicUrl, signedUrl, cdnUrl, providerUrl, storageKey, bucket, objectPath, rawPayload, base64, bytes, secret, apiKey, token, stackTrace, exceptionClass, publishStatus, exportStatus, publishedAt, exportedAt
- No publish/export action buttons/handlers
- No regenerate / auto-approve controls
- API uses GET only, no POST/PUT/DELETE for pipeline

---

## Changed Files

### Modified Production (2)
- `types/index.ts` — added SafePipelineStage, SafePipelineAggregateItem, SafePipelineOverview
- `api/reviewApi.ts` — added getPipelineOverview, getPipelineDetail

### New Production (1)
- `components/review/PipelineOverviewPanel.tsx` — pipeline overview panel

### Modified Integration (1)
- `components/review/ReviewWorklist.tsx` — integrated PipelineOverviewPanel

### New Tests (1)
- `components/review/__tests__/PipelineOverviewPanel.test.ts`

### Modified Tests (2)
- `api/__tests__/reviewApi.test.ts` — updated function count 17→19, added pipeline function presence
- `components/review/__tests__/ReviewWorklist.test.ts` — added pipeline integration tests

---

## Test Coverage

### PipelineOverviewPanel.test.ts — 14 test groups:
- A. Safe Rendering (5)
- B. Internal-Only Labels (7)
- C. Stage Labels (11)
- D. Target Type Labels (5)
- E. Loading/Error/Empty States (4)
- F. Summary Bar (3)
- G. Item Card (5)
- H. Filters (3)
- I. Forbidden Fields/Strings (19)
- J. No Forbidden Controls (5)
- K. API Client (3)
- L. Types (4)
- M. ReviewWorklist Integration (8)
- N. Image Asset Safety (3)

### Updated tests:
- reviewApi.test.ts: +4 tests (19 function count, 2 presence, 1 list)
- ReviewWorklist.test.ts: +3 tests (import, render, FP-016 comment)

---

## Endpoint / Migration Confirmation

- Endpoints: 19 (unchanged) = 9 POST + 10 GET
- No new endpoints in Task 04
- No migrations / schema / config changes
- Backend not changed

---

## Recommended Next

**Task 05 — E2E Pipeline Visibility Regression & Safety Hardening**

Cross-layer integration testing and safety regression verification.
