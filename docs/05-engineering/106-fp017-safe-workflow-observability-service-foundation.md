# FP-017 Task 02 — Safe Workflow Observability DTO + Service Foundation

## Status

| Field | Value |
|-------|-------|
| Task | FP-017 Task 02 — Safe Workflow Observability DTO + Service Foundation |
| Type | Backend production + backend tests |
| Endpoints Changed | None — endpoint count remains 19 (9 POST + 10 GET) |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |
| Frontend Changes | None |

---

## Scope

Backend-only. Adds enums, DTOs, and read-only service for internal workflow observability.
No endpoints added (Task 03). No frontend changes (Task 04). No migrations.

---

## New Enums

### SafeWorkflowDiagnosticType (8 values)
| Value | Description |
|-------|-------------|
| STALE_REVIEW | Review not updated within threshold |
| PREVIEW_UNAVAILABLE | Target preview not available |
| READINESS_UNAVAILABLE | Internal readiness undetermined |
| PIPELINE_UNAVAILABLE | Pipeline stage undetermined |
| REVISION_REQUESTED | Active revision request |
| MISSING_TARGET | Target entity missing |
| BLOCKED_INTERNAL_WORKFLOW | Multiple blocking reasons |
| DEGRADED_VISIBILITY | Partial visibility only |

**Forbidden values absent:** PUBLISH_FAILED, EXPORT_FAILED, CDN_FAILED, PROVIDER_ERROR, STORAGE_ERROR, SIGNED_URL_ERROR

### SafeOperationalControlHint (5 values)
| Value | Description |
|-------|-------------|
| REFRESH_OVERVIEW | Suggest refreshing pipeline overview |
| RECHECK_READINESS | Suggest re-checking readiness |
| VIEW_SAFE_PREVIEW | Suggest viewing safe preview |
| REVIEW_PIPELINE_DETAIL | Suggest reviewing pipeline detail |
| REQUEST_MANUAL_INVESTIGATION | Suggest flagging for investigation |

**Forbidden values absent:** PUBLISH, EXPORT, APPROVE, AUTO_FIX, REGENERATE, PROVIDER_RETRY, STORAGE_RETRY, CREATE_PUBLIC_URL, CREATE_SIGNED_URL, CDN_DELIVERY

---

## New DTOs

### SafeWorkflowDiagnosticItem
Safe fields: diagnosticType, targetType, targetId, pipelineStage, reviewState, safeReasonCode, safeMessage, safeRecommendedHints, updatedAt

### SafeWorkflowObservabilitySummary
Safe fields: countsByReviewState, countsByPipelineStage, countsByReadinessAvailability, previewAvailableCount, previewUnavailableCount, revisionRequestedCount, unavailableCount, diagnostics, safeWarnings, updatedAt

Factory: `empty(String... warnings)` for degraded/empty result.

---

## WorkflowObservabilityService

### Dependencies
- PipelineAggregationService (read-only)
- ReviewRecordRepository (read-only)

### Does NOT depend on
- ReviewApplicationService
- Any provider/storage service
- Any external API

### Methods
| Method | Behavior |
|--------|----------|
| getSummary() | Aggregate counts + diagnostics from pipeline overview + stale check |
| getDiagnostics(targetType) | Filtered diagnostics from summary |

### Diagnostic Generation
- PIPELINE_UNAVAILABLE: items in UNAVAILABLE stage
- PREVIEW_UNAVAILABLE: non-generated items without preview
- READINESS_UNAVAILABLE: approved items with unavailable readiness
- REVISION_REQUESTED: items with active revision
- BLOCKED_INTERNAL_WORKFLOW: items with >1 blocking reason
- STALE_REVIEW: items not updated within 7 days (non-rejected, non-approved)

---

## Degraded/Failure Behavior

| Scenario | Behavior |
|----------|----------|
| Pipeline query fails | Returns empty summary with PIPELINE_QUERY_FAILED warning |
| Stale check fails | Summary returns with STALE_CHECK_DEGRADED warning |
| Partial data | Available counts + degraded warning |
| No stack traces | Error messages are safe strings |

---

## Safety Boundaries

- No mutation / no side effects
- No ReviewApplicationService actions
- No audit record creation
- No provider/storage calls
- No URL generation
- @Transactional(readOnly = true)
- Safe toString() on all DTOs
- No forbidden fields in any DTO

---

## Changed Files

### New Production (5)
- `SafeWorkflowDiagnosticType.java` — 8-value diagnostic type enum
- `SafeOperationalControlHint.java` — 5-value control hint enum
- `SafeWorkflowDiagnosticItem.java` — diagnostic item DTO
- `SafeWorkflowObservabilitySummary.java` — observability summary DTO
- `WorkflowObservabilityService.java` — read-only observability service

### New Test (1)
- `WorkflowObservabilityServiceTest.java` — comprehensive tests

### New Documentation (1)
- `106-fp017-safe-workflow-observability-service-foundation.md` (this file)

---

## Test Coverage

### WorkflowObservabilityServiceTest — 37 tests across 10 groups

| Group | Tests | Coverage |
|-------|-------|----------|
| A. DiagnosticType Enum | 3 | 8 values, no forbidden |
| B. ControlHint Enum | 4 | 5 values, no forbidden, non-mutating names |
| C. DiagnosticItem DTO | 4 | Forbidden fields, builder, toString, all types |
| D. Summary DTO | 5 | Forbidden fields, empty(), builder, toString, warnings |
| E. Service Structure | 6 | @Service, no controller, deps, readOnly |
| F. Target Type Coverage | 2 | All 5 types + all 9 stages in diagnostic |
| G. Endpoint Inventory | 4 | 19 = 9P + 10G |
| H. Forbidden Enum Strings | 2 | No publish/export/CDN in enum names |
| I. Summary Count Types | 4 | Map/List field types |
| J. Degraded Behavior | 3 | Empty, warnings, null reviewState |

---

## Endpoint / Migration Confirmation

- Endpoints: 19 (unchanged) = 9 POST + 10 GET
- No new endpoints in Task 02
- No migrations / schema / config changes
- Backend not touched for endpoints

---

## Recommended Next

**Task 03 — Workflow Observability Admin API**

Add 2 GET endpoints:
- GET /api/admin/workflow-observability/summary
- GET /api/admin/workflow-observability/diagnostics

Expected endpoint count: 21 = 9 POST + 12 GET
