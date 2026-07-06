# FP-017 Task 03 — Workflow Observability Admin API

## Status

| Field | Value |
|-------|-------|
| Task | FP-017 Task 03 — Workflow Observability Admin API |
| Type | Backend API + backend tests |
| Endpoints Before | 19 = 9 POST + 10 GET |
| Endpoints After | **21 = 9 POST + 12 GET** |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |
| Frontend Changes | None |

---

## Scope

Backend API only. Adds 2 GET endpoints for workflow observability.
No POST endpoints. No frontend changes. No migrations.

---

## New Endpoints

### GET /api/admin/workflow-observability/summary
- Returns SafeWorkflowObservabilitySummary
- Aggregate counts: review state, pipeline stage, readiness, preview
- Diagnostic items needing attention
- Safe warnings for degraded sources
- No filters (returns full summary)
- No mutation, no side effects

### GET /api/admin/workflow-observability/diagnostics
- Returns List<SafeWorkflowDiagnosticItem>
- Optional filter: targetType (ReviewTargetType)
- Filtered diagnostic items from summary
- No filter → all diagnostics
- No mutation, no side effects

---

## Endpoint Count

| Controller | POST | GET | Total |
|------------|------|-----|-------|
| AdminReviewController | 9 | 8 | 17 |
| AdminPipelineController | 0 | 2 | 2 |
| AdminWorkflowObservabilityController | 0 | 2 | 2 |
| **Combined** | **9** | **12** | **21** |

---

## Controller

### AdminWorkflowObservabilityController
- Base path: /api/admin/workflow-observability
- Single dependency: WorkflowObservabilityService
- No ReviewApplicationService dependency
- No PipelineAggregationService direct dependency
- GET-only — no POST, PUT, DELETE
- Safe error handling via service degraded behavior

---

## Filter Behavior

| Endpoint | Filter | Required | Behavior |
|----------|--------|----------|----------|
| /summary | (none) | — | Returns full summary |
| /diagnostics | targetType | No | Filters diagnostics by target type |
| /diagnostics | (no filter) | — | Returns all diagnostics |

---

## Failure Behavior

| Scenario | Behavior |
|----------|----------|
| Pipeline service fails | Summary returns with PIPELINE_QUERY_FAILED warning |
| Stale check fails | Summary returns with STALE_CHECK_DEGRADED warning |
| No diagnostics | Empty list, 200 OK |
| Invalid targetType | Spring 400 Bad Request (safe) |

---

## Safety Boundaries

- No mutation / no side effects / no audit writes
- GET-only controller
- No provider/storage calls
- No URL generation
- No forbidden fields in response DTOs
- No forbidden path fragments in endpoint paths
- No publish/export/approve/regenerate controls

---

## Changed Files

### New Production (1)
- `AdminWorkflowObservabilityController.java` — 2 GET endpoints

### New Test (1)
- `WorkflowObservabilityAdminApiTest.java` — 31 tests

### New Documentation (1)
- `107-fp017-workflow-observability-admin-api.md` (this file)

---

## Test Coverage

### WorkflowObservabilityAdminApiTest — 31 tests across 11 groups

| Group | Tests | Coverage |
|-------|-------|----------|
| A. Controller Annotations | 2 | @RestController, @RequestMapping |
| B. Endpoint Inventory | 8 | 21 = 9P + 12G, per-controller counts |
| C. Endpoint Paths | 2 | /summary, /diagnostics |
| D. Forbidden Paths | 1 | 15 fragments checked (incl. base path) |
| E. Controller Dependency | 3 | Single dep, no ReviewApp/PipelineAgg |
| F. No Mutation | 3 | No POST, PUT, DELETE |
| G. Forbidden Response Fields | 3 | 19 fields × 2 DTOs + toString |
| H. Forbidden Enum Values | 2 | DiagnosticType + ControlHint |
| I. Cross-Controller Isolation | 2 | 3 unique base paths |
| J. ReviewController Unchanged | 3 | 9P, 8G, base path |
| K. PipelineController Unchanged | 2 | 0P, 2G |

---

## Migration / Schema Confirmation

- No migrations in Task 03
- No schema changes
- No config changes

---

## Frontend Status

- Not changed (477 tests, 0 failures)

---

## Recommended Next

**Task 04 — Admin Observability UI**

Add admin-web UI for workflow observability dashboard.
