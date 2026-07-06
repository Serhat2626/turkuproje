# FP-016 Task 05 — E2E Pipeline Visibility Regression & Safety Hardening

## Status

| Field | Value |
|-------|-------|
| Task | FP-016 Task 05 — E2E Pipeline Visibility Regression & Safety Hardening |
| Type | Tests + documentation only |
| Production Code Changed | No |
| Endpoints Changed | None — endpoint count remains 19 (9 POST + 10 GET) |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |

---

## Scope

Cross-layer regression verification. Tests + docs only.
No production code changes. No endpoints. No migrations.

---

## Backend Regression Test

### FP016PipelineVisibilityRegressionTest.java — 38 tests across 10 groups

#### A. Full Stage Mapping Regression (5 tests)
- Exact 9-value count
- All expected stage names present
- No forbidden publish/export stages
- GENERATED stage for DRAFT/AI_GENERATED/null
- UNAVAILABLE stage for missing/error

#### B. Target Type Coverage (3 tests)
- Exact 5-value count
- All expected types present
- All types usable in SafePipelineAggregateItem DTO

#### C. DTO Safety Regression (6 tests)
- SafePipelineAggregateItem: no forbidden fields (22 checked)
- SafePipelineOverview: no forbidden fields (22 checked)
- unavailable() factory: safe defaults
- empty() factory: safe defaults
- toString() on both DTOs: no forbidden strings

#### D. Combined Endpoint Inventory (8 tests)
- AdminReviewController: 9 POST, 8 GET
- AdminPipelineController: 0 POST, 2 GET, 0 PUT, 0 DELETE
- Total: 19 = 9 POST + 10 GET
- Pipeline GET paths: /overview, /{targetType}/{targetId}

#### E. Forbidden Endpoint Path Regression (2 tests)
- AdminPipelineController paths: 15 forbidden fragments checked
- AdminReviewController paths: 15 forbidden fragments checked

#### F. Service Read-Only Regression (5 tests)
- @Service annotation present
- No @RestController/@Controller
- Public methods @Transactional(readOnly=true)
- No ReviewApplicationService dependency
- Controller single dependency (PipelineAggregationService)

#### G. ReviewState Coverage Regression (3 tests)
- Exact 7-value count (DRAFT, AI_GENERATED, NEEDS_REVIEW, IN_REVIEW, CHANGES_REQUESTED, APPROVED_FOR_INTERNAL_USE, REJECTED)
- No forbidden states (COMPLETED, PUBLISHED, EXPORTED, etc.)

#### H. Readiness Integration Regression (2 tests)
- READY means internal-only
- No forbidden availability values

#### I. Cross-Controller Isolation (3 tests)
- Pipeline at /api/admin/pipeline
- Review at /api/admin/reviews
- Different base paths

#### J. AI_GENERATED State Safety (1 test)
- AI_GENERATED not in SafePipelineStage (mapped to GENERATED)

---

## Frontend Regression Coverage

Frontend tests remain at 477 (from Task 04). Coverage includes:

### PipelineOverviewPanel.test.ts (85 tests)
- Safe rendering (5): no dangerouslySetInnerHTML, innerHTML, __html
- Internal-only labels (7): Dahili, İç Kullanım, disclaimer
- Stage labels (11): all 9 stages, no forbidden stages
- Target type labels (5): all 5 types
- Loading/error/empty (4): safe states
- Summary bar (3): counts, total
- Item card (5): title, stage, blocking, hints
- Filters (3): targetType, stage, API call
- Forbidden fields (19): all forbidden strings checked
- No forbidden controls (5): no publish/export/regenerate/auto-approve
- API client (3): correct endpoints, GET-only
- Types (4): type definitions, no forbidden fields
- ReviewWorklist integration (8): import, render, existing panels preserved
- Image asset safety (3): no img, no URL, no base64

### reviewApi.test.ts (22 tests)
- 19 function count (was 17, +2 pipeline)
- Pipeline function presence
- Forbidden function names
- Forbidden strings

### ReviewWorklist.test.ts (39 tests)
- Pipeline integration (3 tests): import, render, FP-016 comment
- All existing panels verified

---

## Stage Mapping Coverage

| ReviewState | Readiness | Revision | → SafePipelineStage |
|-------------|-----------|----------|---------------------|
| null / DRAFT / AI_GENERATED | — | — | GENERATED |
| NEEDS_REVIEW | — | — | REVIEW_PENDING |
| IN_REVIEW | — | — | IN_REVIEW |
| REJECTED | — | — | REJECTED |
| APPROVED_FOR_INTERNAL_USE | READY | — | INTERNAL_READY |
| APPROVED_FOR_INTERNAL_USE | other | — | INTERNALLY_APPROVED |
| CHANGES_REQUESTED | — | true | INTERNAL_REVISION_REQUESTED |
| CHANGES_REQUESTED | — | false | CHANGES_REQUESTED |
| (missing/error) | — | — | UNAVAILABLE |

---

## Target Coverage

All 5 target types verified in both backend and frontend:
- CONTENT_PROJECT
- SCENARIO
- STORYBOARD
- PACKAGE
- IMAGE_ASSET

---

## Endpoint Inventory Result

| Controller | POST | GET | Total |
|------------|------|-----|-------|
| AdminReviewController | 9 | 8 | 17 |
| AdminPipelineController | 0 | 2 | 2 |
| **Combined** | **9** | **10** | **19** |

No PUT, DELETE, or PATCH endpoints.

---

## Forbidden Behavior Regression

### Checked across all layers (38 backend + 477 frontend tests):
- No publish/export actions/buttons/handlers
- No public URL / signed URL / CDN delivery
- No provider/storage inspection
- No media library / regenerate / auto-approve
- No generic status update
- No raw payload / base64 / bytes / secrets
- No stack traces in responses
- No unsafe HTML rendering
- No forbidden fields in DTOs/types
- No forbidden path fragments in endpoints
- No forbidden stage values

---

## Safety Boundaries

- Standing Anadolu Atlası safety baseline preserved
- No production code changes in Task 05
- No migration/schema/config changes
- Endpoint count unchanged at 19

---

## Final Closure Readiness

FP-016 is ready for final closure:
- Task 01: Design document ✅
- Task 02: Backend DTO/enum/service foundation ✅
- Task 03: Pipeline overview admin API ✅
- Task 04: Pipeline overview admin UI ✅
- Task 05: E2E regression & safety hardening ✅

---

## Recommended Next

**Task 06 — Final Closure / Operational Baseline**

Documentation-only. Create final closure document for FP-016.
