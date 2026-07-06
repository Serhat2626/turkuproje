# FP-016 Task 03 — Pipeline Overview Admin API

## Status

| Field | Value |
|-------|-------|
| Task | FP-016 Task 03 — Pipeline Overview Admin API |
| Type | Backend API + backend tests |
| Endpoints Before | 17 (9 POST + 8 GET) |
| Endpoints After | **19 (9 POST + 10 GET)** |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |
| Frontend Changes | None |

---

## Scope

Backend-only. Adds a new `AdminPipelineController` with 2 GET endpoints.
No POST endpoints. No frontend changes. No migrations.

---

## New Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/admin/pipeline/overview` | Pipeline overview with optional filters |
| GET | `/api/admin/pipeline/{targetType}/{targetId}` | Single target pipeline detail |

### Overview Endpoint

**GET /api/admin/pipeline/overview**

Query parameters:
- `targetType` (optional): Filter by ReviewTargetType
- `stage` (optional): Filter by SafePipelineStage
- `limit` (optional, default 50, max 200): Max items returned

Response: `ApiResponse<SafePipelineOverview>`

### Detail Endpoint

**GET /api/admin/pipeline/{targetType}/{targetId}**

Path parameters:
- `targetType`: ReviewTargetType
- `targetId`: UUID

Response: `ApiResponse<SafePipelineAggregateItem>`

Missing targets return UNAVAILABLE stage with blocking reason (200 OK with safe degraded response).

---

## Endpoint Inventory (After FP-016 Task 03)

### AdminReviewController — 17 endpoints (unchanged)

**POST (9):** submit, start, request-changes, approve-internal, reject, return-to-draft, note, handoff, request-internal-revision

**GET (8):** state, audit, worklist, context, actions, preview, internal-readiness, approved/internal-readiness

### AdminPipelineController — 2 endpoints (new)

**GET (2):** overview, {targetType}/{targetId}

### Combined Total: 19 (9 POST + 10 GET)

---

## Changed Files

### New Production (1)
- `AdminPipelineController.java` — 2 GET endpoints, read-only, safe responses

### New Test (1)
- `PipelineAggregationAdminApiTest.java` — comprehensive API tests

### New Documentation (1)
- `101-fp016-pipeline-overview-admin-api.md` (this file)

---

## Safety Boundaries

- No POST/PUT/DELETE endpoints
- No mutation from pipeline controller
- No ReviewApplicationService action calls
- No audit record creation
- No provider/storage calls
- No URL generation
- No forbidden fields in response
- No forbidden path fragments in endpoint paths
- Controller depends only on PipelineAggregationService (not repositories)

---

## Test Coverage

PipelineAggregationAdminApiTest — 11 test groups:
- A. Controller Annotations (2 tests)
- B. Endpoint Inventory (8 tests) — count, paths, combined total
- C. Overview Endpoint (5 tests) — responses, filters, empty
- D. Detail Endpoint (2 tests) — existing target, missing target
- E. Target Type Coverage (1 test) — all 5 types
- F. Stage Serialization (1 test) — all 9 stages
- G. No-Mutation Verification (3 tests) — no POST, no cross-calls
- H. Forbidden Path Fragments (1 test) — no publish/export/cdn/etc
- I. Forbidden Response Fields (3 tests) — DTOs clean
- J. Controller Constructor (3 tests) — single dependency
- K. AdminReviewController Unchanged (3 tests) — 9 POST, 8 GET, base path

---

## Migration / Schema / Config Confirmation

- No migrations
- No schema changes
- No config changes
- No new database tables

---

## Recommended Next

**Task 04 — Pipeline Overview Admin UI**

Add frontend components for pipeline overview panel with filters, summary counts, and item detail cards.
