# FP-017 Task 04 — Admin Observability UI

## Status

| Field | Value |
|-------|-------|
| Task | FP-017 Task 04 — Admin Observability UI |
| Type | Frontend/admin production + frontend tests |
| Endpoints Changed | None — endpoint count remains 21 (9 POST + 12 GET) |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |
| Backend Changes | None |

---

## Scope

Frontend/admin-web only. Adds workflow observability types, API methods, UI components, and tests.
No backend changes. No new endpoints. No migrations.

---

## Types Added (4 new in types/index.ts)

- SafeWorkflowDiagnosticType: 8-value union
- SafeOperationalControlHint: 5-value union
- SafeWorkflowDiagnosticItem: diagnostic item interface
- SafeWorkflowObservabilitySummary: aggregate summary interface

---

## API Methods Added (2 new in reviewApi.ts)

| Method | Endpoint | HTTP |
|--------|----------|------|
| getWorkflowObservabilitySummary | /admin/workflow-observability/summary | GET |
| getWorkflowObservabilityDiagnostics | /admin/workflow-observability/diagnostics | GET |

Total API functions: 21 (was 19, +2 new)

---

## UI Components

### WorkflowObservabilityPanel (new)
- Internal-only workflow observability dashboard
- Disclaimer: "Bu panel işlem çalıştırmaz. Yayın/export değildir."
- Summary cards: review state counts, pipeline stage counts, readiness counts
- Quick counts: preview available/unavailable, revision requested, unavailable
- Safe warnings section
- Diagnostic cards with type badge, message, reason code, pipeline stage, hints
- Diagnostic filter by target type
- Control hints rendered as non-mutating labels only
- Loading/error/empty states
- Safe text rendering
- No dangerouslySetInnerHTML, no innerHTML, no img src

### ReviewWorklist Integration
- WorkflowObservabilityPanel added above PipelineOverviewPanel
- All existing panels preserved

---

## Changed Files

### Modified Production (3)
- types/index.ts — +4 types
- api/reviewApi.ts — +2 functions, +2 imports
- components/review/ReviewWorklist.tsx — +import, +render, +comment

### New Production (1)
- components/review/WorkflowObservabilityPanel.tsx

### New Tests (1)
- components/review/__tests__/WorkflowObservabilityPanel.test.ts

### Modified Tests (2)
- api/__tests__/reviewApi.test.ts — function count 19→21
- components/review/__tests__/ReviewWorklist.test.ts — +3 integration tests

---

## Test Coverage

### WorkflowObservabilityPanel.test.ts — 14 groups:
- A. Safe Rendering (5)
- B. Internal-Only Labels (7)
- C. Diagnostic Type Labels (8)
- D. Control Hint Labels (5)
- E. Loading/Error/Empty (4)
- F. Summary Counts (6)
- G. Diagnostic Card (5)
- H. Diagnostic Filter (3)
- I. Forbidden Fields (19)
- J. No Forbidden Controls (5)
- K. API Client (3)
- L. Types (5)
- M. ReviewWorklist Integration (9)
- N. Image Asset Safety (3)

---

## Endpoint / Migration Confirmation

- Endpoints: 21 (unchanged) = 9 POST + 12 GET
- No new endpoints in Task 04
- No migrations / schema / config changes

---

## Recommended Next

**Task 05 — Operational Controls Guardrail Hardening**
