# FP-017 — Internal Workflow Observability & Admin Operational Controls — Combined Design

---

## 1. Status

| Field | Value |
|-------|-------|
| Feature | FP-017 — Internal Workflow Observability & Admin Operational Controls |
| Status | **PLANNED / DESIGN ONLY** |
| Type | Documentation-only |
| Runtime Behavior Changes | None |
| Backend Changes | None |
| Frontend Changes | None |
| Test Changes | None |
| Migration/Schema/Config | None |
| Baseline Backend | 2431 tests, 0 failures |
| Baseline Frontend | 477 tests, 0 failures |
| Baseline Endpoints | 19 = 9 POST + 10 GET |

---

## 2. Background

FP-017 builds upon the internal workflow infrastructure established across FP-012 through FP-016:

| Feature | Contribution |
|---------|-------------|
| FP-012 | Human review workflow: ReviewDomainService, ReviewApplicationService, review actions, review states, admin review console |
| FP-013 | Review-gated orchestration: handoff, admin preview, review prerequisites |
| FP-014 | Internal readiness: InternalApprovalReadinessService, readiness availability, approved readiness API |
| FP-015 | Internal revision request: controlled revision after approval, approved inventory, revision audit |
| FP-016 | Pipeline aggregation: SafePipelineStage, PipelineAggregationService, pipeline overview API/UI |

Together, these features provide a robust internal workflow with review, preview, readiness assessment, revision control, and pipeline visibility. FP-017 adds the **observability and operational confidence layer** that gives admins a holistic view of workflow health and safe diagnostic tools.

---

## 3. Product Intent

### What FP-017 provides:
- **Internal workflow observability** — aggregate health view of all review, readiness, preview, pipeline, and revision states
- **Admin operational confidence** — clear indicators of workflow health, blockers, and stale items
- **Safe diagnostics** — targeted diagnostic views for items needing attention without exposing internal implementation details
- **Internal-only health/readiness summaries** — counts and breakdowns that help admins identify bottlenecks and issues
- **Non-destructive operational hints** — suggestions for next steps (e.g., "re-check readiness", "investigate stale item") without executing any workflow action

### What FP-017 does NOT provide:
- Workflow execution (no automatic state transitions)
- External-facing dashboards or metrics
- Production monitoring infrastructure (Prometheus/Grafana)
- Real-time event streaming

---

## 4. Explicit Non-Goals

| Non-Goal | Rationale |
|----------|-----------|
| Publish/export | Not in current feature chain |
| Public URL / signed URL / CDN delivery | No public delivery infrastructure |
| User-facing media library | Not in scope |
| Video/audio processing | Remains deferred |
| Automatic approval | Only human-driven approval |
| Generic status update | Only explicit review actions |
| Provider/storage inspection | No provider/storage detail exposure |
| Raw payload / base64 / secret exposure | Safety boundary |
| Production auth integration | Deferred to future feature |
| Endpoint/code implementation in Task 01 | Design-only in this task |

---

## 5. Observability Concept

FP-017 introduces a **safe internal observability layer** that derives aggregate health metrics from existing services without creating new data stores or calling external providers.

### Safe Internal Metrics/Views

| Metric | Source | Description |
|--------|--------|-------------|
| Review state counts | ReviewRecordRepository | Items per ReviewState (DRAFT, AI_GENERATED, NEEDS_REVIEW, IN_REVIEW, CHANGES_REQUESTED, APPROVED_FOR_INTERNAL_USE, REJECTED) |
| Pipeline stage counts | PipelineAggregationService | Items per SafePipelineStage |
| Readiness availability counts | InternalApprovalReadinessService | Items per InternalApprovalReadinessAvailability |
| Preview availability counts | AdminPreviewService | Items with/without safe preview |
| Revision request counts | ReviewAuditEventRepository | Items with active revision requests |
| Unavailable/missing target counts | PipelineAggregationService | Items in UNAVAILABLE stage |
| Safe blocker summaries | SafePipelineAggregateItem | Aggregated safeBlockingReasons across items |
| Stale item hints | ReviewRecordRepository | Items not updated within configurable threshold (e.g., 7 days) |
| Failed handoff count | ReviewAuditEventRepository | Handoff actions that resulted in error state, if safely derivable |

### Aggregation Rules
- All metrics are **derived** from existing repositories and services
- No new database tables or columns required initially
- No external API calls
- Partial failure returns degraded result, not error
- Counts are safe integers — no PII, no secrets, no provider details

---

## 6. Safe Operational Controls Concept

### Phase 1 — Non-Mutating Controls (FP-017 scope)

| Control | Type | Description |
|---------|------|-------------|
| Refresh pipeline overview | Read-only | Re-derive pipeline aggregation from current state |
| Re-check internal readiness | Read-only | Re-derive readiness for a target from current state |
| Show safe diagnostics | Read-only | Display diagnostic detail for a specific item |
| Mark diagnostic reviewed | Metadata | Admin acknowledges seeing a diagnostic (audit note only) |
| Request manual investigation | Metadata | Admin flags item for manual review (audit note only) |

### Explicitly Excluded Controls

| Excluded Control | Reason |
|-----------------|--------|
| Publish | No publish workflow exists |
| Export | No export workflow exists |
| Approve | Only through existing ReviewApplicationService actions |
| Auto-fix | No automated remediation |
| Regenerate | No generation trigger from observability |
| Provider retry | No provider interaction |
| Storage retry | No storage interaction |
| Create public/signed URL | No public delivery |
| CDN delivery | No CDN infrastructure |
| Force state transition | Only explicit review actions |

### Phase 2 — Future Safe Mutating Controls (post-FP-017, requires separate design)
- Batch re-check readiness (if justified)
- Bulk mark-as-reviewed for diagnostics
- Admin-initiated draft reset (requires safety review)

---

## 7. Backend Design Direction

### Service Layer

```
WorkflowObservabilityService
├── depends on: ReviewRecordRepository (read-only)
├── depends on: ReviewAuditEventRepository (read-only)
├── depends on: PipelineAggregationService (read-only)
├── depends on: InternalApprovalReadinessService (read-only)
├── depends on: AdminPreviewService (read-only)
├── does NOT depend on: ReviewApplicationService
├── does NOT depend on: any provider/storage service
├── does NOT call: external APIs
└── @Transactional(readOnly = true)
```

### Design Principles
- **Derived read-only service** — no writes, no audit creation, no state mutation
- **Reuse existing services** — PipelineAggregationService, InternalApprovalReadinessService, AdminPreviewService
- **No new database table initially** — all metrics derived from existing review records and audit events
- **No external provider/storage calls** — completely internal
- **No mutation in early tasks** — mark-as-reviewed / request-investigation controls defer to later tasks if needed
- **Safe degraded behavior** — partial failures return degraded summary, not exceptions

### Package Location
```
com.anadoluatlasi.backend.review.WorkflowObservabilityService
```

Same `review` package as PipelineAggregationService — no new module required.

---

## 8. API Design Direction

### Proposed Future GET Endpoints (Task 03)

| Method | Path | Purpose | Task |
|--------|------|---------|------|
| GET | `/api/admin/workflow-observability/summary` | Aggregate observability summary | Task 03 |
| GET | `/api/admin/workflow-observability/diagnostics` | Diagnostic items needing attention | Task 03 |

### Expected Endpoint Count After Task 03
- Before: 19 = 9 POST + 10 GET
- After: **21 = 9 POST + 12 GET**

### POST Controls — Deferred
No POST endpoints in initial design. Safe non-mutating controls (mark-as-reviewed, request-investigation) require separate safety review before implementation. If included in Task 05, expected count would become 23 = 11 POST + 12 GET.

### API Safety Rules
- All endpoints return safe wrapper DTOs
- No provider/storage/raw/base64/secret fields in response
- Missing data returns degraded summary, not 500
- No forbidden path fragments in endpoint paths
- No generic status update endpoint

---

## 9. Frontend/Admin Design Direction

### New Components (Task 04)

| Component | Purpose |
|-----------|---------|
| WorkflowObservabilitySummaryPanel | Aggregate health dashboard with safe counters |
| WorkflowDiagnosticCard | Individual diagnostic item with safe detail |
| StaleItemWarningBanner | Warning for items not updated recently |

### UI Behavior
- Summary panel shows safe counters: review states, pipeline stages, readiness, preview, revisions
- Diagnostic cards show items needing attention with safe title, summary, reason, and suggested action
- Stale item warnings highlight items not progressed within threshold
- Links to existing pipeline/review/readiness panels for drill-down
- No publish/export/approve/regenerate controls
- No provider/storage/URL details
- Safe text rendering (safeText function)
- Turkish internal-only labels
- Loading/error/empty states with safe fallbacks

### Integration Point
- Add to ReviewWorklist or adjacent admin section
- Non-blocking — does not interfere with existing panels
- Additive only — can be removed without breaking existing UI

---

## 10. Safe DTO Concepts

### SafeWorkflowObservabilitySummary

```
SafeWorkflowObservabilitySummary
├── totalTargets: int
├── countsByReviewState: Map<String, Integer>
├── countsByPipelineStage: Map<String, Integer>
├── countsByReadinessAvailability: Map<String, Integer>
├── previewAvailableCount: int
├── previewUnavailableCount: int
├── revisionRequestedCount: int
├── unavailableCount: int
├── staleItemCount: int
├── safeWarnings: List<String>
├── updatedAt: LocalDateTime
└── NO forbidden fields
```

### SafeWorkflowDiagnosticItem

```
SafeWorkflowDiagnosticItem
├── targetType: ReviewTargetType
├── targetId: UUID
├── diagnosticType: SafeDiagnosticType
├── safeTitle: String
├── safeSummary: String
├── safeReasonCode: String
├── safeMessage: String
├── severity: SafeSeverity (INFO, WARNING, ACTION_NEEDED)
├── suggestedAction: String
├── detectedAt: LocalDateTime
└── NO forbidden fields
```

### SafeOperationalControlHint

```
SafeOperationalControlHint
├── controlType: SafeControlType
├── safeLabel: String
├── safeDescription: String
├── enabled: boolean
├── safeReasonIfDisabled: String
└── NO forbidden fields
```

### Forbidden Fields (applied to all DTOs)

| Forbidden Field | Status |
|----------------|--------|
| publicUrl | ❌ Never included |
| signedUrl | ❌ Never included |
| cdnUrl | ❌ Never included |
| providerUrl | ❌ Never included |
| storageKey | ❌ Never included |
| bucket | ❌ Never included |
| objectPath | ❌ Never included |
| rawPayload | ❌ Never included |
| base64 | ❌ Never included |
| bytes | ❌ Never included |
| secret | ❌ Never included |
| apiKey | ❌ Never included |
| token | ❌ Never included |
| stackTrace | ❌ Never included |
| exceptionClass | ❌ Never included |

---

## 11. Observability and Logging Safety

| Rule | Description |
|------|-------------|
| No high-cardinality unsafe labels | Metric labels limited to ReviewTargetType, SafePipelineStage, ReviewState, availability |
| No targetId in metric labels | Target IDs used in log messages only if existing logging policy allows; never in metric dimensions |
| No provider/storage/key/url/secret in logs | Existing safety boundary preserved |
| Safe counters only | All observability metrics are integer counts — no PII, no content, no provider details |
| No raw user messages in metric labels | User-generated text never used as metric dimension |
| Structured logging | SLF4J with safe parameter placeholders |

---

## 12. Failure Behavior

| Scenario | Behavior |
|----------|----------|
| One service partially fails | Summary returns with available data + safeWarning noting degraded source |
| All services fail | Summary returns with zero counts + safeWarning "Tüm kaynaklar kullanılamıyor" |
| Missing target in diagnostics | Diagnostic item with UNAVAILABLE + safe reason |
| Database timeout | Degraded summary with safeWarning, not 500 |
| No stack traces | Error messages are safe strings, no exception class/stack exposure |
| No provider/storage/config leaks | Failure messages do not reveal infrastructure details |

---

## 13. Test Strategy

| Test Area | Coverage |
|-----------|----------|
| Service summary tests | WorkflowObservabilityService returns safe summary from mocked repos |
| Diagnostic DTO safety tests | No forbidden fields, safe toString, safe defaults |
| API endpoint tests | Correct paths, GET-only, safe responses, filter safety |
| Frontend render tests | Safe rendering, no unsafe HTML, internal labels, loading/error/empty |
| Endpoint inventory regression | Total count verification (21 after Task 03) |
| Forbidden field regression | All DTOs, all endpoints, all UI components |
| No mutation tests | Service is read-only, controller has no POST/PUT/DELETE |
| No publish/export/public/signed/CDN tests | Forbidden strings/paths absent across all layers |
| Cross-controller isolation | Observability controller has separate base path |
| Existing panel preservation | ReviewWorklist integration does not break existing panels |

---

## 14. Dense FP-017 Task Sequence

| Task | Title | Type | Key Deliverables |
|------|-------|------|-----------------|
| **Task 01** | Combined Design (this document) | Documentation | Design doc |
| **Task 02** | Safe Workflow Observability DTO + Service Foundation | Backend + tests | SafeWorkflowObservabilitySummary, SafeWorkflowDiagnosticItem, WorkflowObservabilityService, unit tests |
| **Task 03** | Workflow Observability Admin API | Backend API + tests | 2 GET endpoints, API tests, endpoint inventory regression |
| **Task 04** | Admin Observability UI | Frontend + tests | Summary panel, diagnostic cards, stale warnings, integration |
| **Task 05** | Operational Controls Guardrail Hardening | Tests + docs (or safe non-mutating controls if justified) | Safety regression, forbidden behavior coverage |
| **Task 06** | E2E Regression & Safety Hardening | Tests + docs | Cross-layer regression, combined verification |
| **Task 07** | Final Closure / Operational Baseline | Documentation | Closure document, final baselines |

### Expected Baseline Progression

| Task | Backend Tests | Frontend Tests | Endpoints |
|------|--------------|----------------|-----------|
| Task 01 | 2431 | 477 | 19 |
| Task 02 | ~2490+ | 477 | 19 |
| Task 03 | ~2530+ | 477 | 21 |
| Task 04 | ~2530+ | ~560+ | 21 |
| Task 05 | ~2560+ | ~570+ | 21 |
| Task 06 | ~2590+ | ~570+ | 21 |
| Task 07 | ~2590+ | ~570+ | 21 |

---

## 15. Final Recommendation

**Proceed with Task 02 — Safe Workflow Observability DTO + Service Foundation**

Task 02 should:
- Create SafeWorkflowObservabilitySummary DTO
- Create SafeWorkflowDiagnosticItem DTO
- Create SafeDiagnosticType enum
- Create SafeSeverity enum
- Create WorkflowObservabilityService (read-only, derived)
- Add comprehensive unit tests
- Create task documentation
- Not add endpoints (Task 03)
- Not touch frontend (Task 04)
- Not add migrations/schema/config

---

## Safety Baseline Confirmation

This design document preserves the standing Anadolu Atlası safety baseline:

| Safety Rule | Status |
|------------|--------|
| No publish/export | ✅ Preserved |
| No public/signed/CDN delivery | ✅ Preserved |
| No auto-approval | ✅ Preserved |
| No generic status update | ✅ Preserved |
| No provider/storage exposure | ✅ Preserved |
| No raw/base64/secret exposure | ✅ Preserved |
| No unsafe HTML | ✅ Preserved |
| No migration/schema/config | ✅ Preserved |

---

## Closed Feature Chain

| Feature | Status |
|---------|--------|
| FP-001 through FP-016 | CLOSED |
| **FP-017** | **PLANNED / DESIGN ONLY** |
