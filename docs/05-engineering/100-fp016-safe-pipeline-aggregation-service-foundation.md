# FP-016 Task 02 — Safe Pipeline Aggregation Service Foundation

## Status

| Field | Value |
|-------|-------|
| Task | FP-016 Task 02 — Safe Pipeline Aggregation DTO + Service Foundation |
| Type | Backend production + backend tests |
| Endpoints Changed | None — endpoint count remains 17 (9 POST + 8 GET) |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |
| Frontend Changes | None |

---

## Scope

Backend-only. Creates the DTO/enum/service foundation for internal pipeline aggregation.
No new endpoints. No frontend changes. No migrations.

---

## New Production Classes

### SafePipelineStage (Enum)
- 9 internal-only values: GENERATED, REVIEW_PENDING, IN_REVIEW, CHANGES_REQUESTED, INTERNALLY_APPROVED, INTERNAL_READY, INTERNAL_REVISION_REQUESTED, REJECTED, UNAVAILABLE
- Forbidden values absent: READY_FOR_PUBLISH, PUBLISHABLE, PUBLISHED, EXPORTABLE, EXPORTED, CDN_READY, PUBLIC_DELIVERY, SIGNED_URL_READY

### SafePipelineAggregateItem (DTO)
- Immutable (Lombok @Builder @Getter)
- Fields: targetType, targetId, pipelineStage, reviewState, previewAvailable, readinessAvailability, revisionRequested, safeTitle, safeSummary, safeBlockingReasons, safeNextInternalHints, updatedAt
- Factory: unavailable() for missing/error targets
- Safe toString — no forbidden fields

### SafePipelineOverview (DTO)
- Immutable (Lombok @Builder @Getter)
- Fields: items, totalCount, countsByStage, countsByTargetType, safeWarnings, updatedAt
- Factory: empty() for no-result cases
- Safe toString — no forbidden fields

### PipelineAggregationService (Service)
- Read-only derived service (@Service, @Transactional(readOnly = true))
- Dependencies: ReviewRecordRepository, ReviewAuditEventRepository, InternalApprovalReadinessService, AdminPreviewService
- Methods:
  - getOverview(targetTypeFilter?, stageFilter?, limit) → SafePipelineOverview
  - getItemDetail(targetType, targetId) → SafePipelineAggregateItem

---

## Stage Mapping

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

---

## Revision Detection

1. If current state is not CHANGES_REQUESTED → false
2. If lastActionType == REQUEST_INTERNAL_REVISION → true
3. Fallback: scan audit trail for APPROVE_FOR_INTERNAL_USE followed by REQUEST_INTERNAL_REVISION → true
4. Audit trail exception → false (safe default)

---

## Target Type Coverage

All 5 target types: CONTENT_PROJECT, SCENARIO, STORYBOARD, PACKAGE, IMAGE_ASSET

---

## Failure / Degradation Behavior

| Failure | Behavior |
|---------|----------|
| Record query fails | Empty overview with RECORD_QUERY_FAILED warning |
| Item aggregation fails | UNAVAILABLE item with AGGREGATION_FAILED reason |
| Preview service fails | previewAvailable = false |
| Readiness service fails | readinessAvailability = UNAVAILABLE |
| Audit trail fails | revisionRequested = false (safe default) |
| Result exceeds limit | Truncated with RESULT_TRUNCATED warning |

---

## Safety Boundaries

No forbidden fields in DTOs: publicUrl, signedUrl, cdnUrl, providerUrl, storageKey, bucket, objectPath, rawPayload, base64, bytes, secret, apiKey, token, stackTrace, exceptionClass, publishStatus, exportStatus, publishedAt, exportedAt.

No mutation: no save/delete/update calls on repositories.

No external calls: no provider API, no storage API, no generation service.

---

## Test Coverage

PipelineAggregationServiceTest — 11 test groups:
- A. SafePipelineStage Enum (3 tests)
- B. Stage Derivation (11 tests)
- C. Revision Detection (6 tests)
- D. Single Item Detail (4 tests)
- E. Overview (7 tests)
- F. Target Type Coverage (2 tests)
- G. No-Mutation Verification (3 tests)
- H. Forbidden Field Regression (5 tests)
- I. DTO Factory Methods (3 tests)
- J. Endpoint Count Regression (4 tests)
- K. Preview/Readiness Integration (3 tests)

---

## Endpoint / Migration Confirmation

- Endpoints: 17 (unchanged) = 9 POST + 8 GET
- No new endpoints in Task 02
- No migrations
- No schema changes
- No config changes

---

## Recommended Next

**Task 03 — Pipeline Overview Admin API**

Add 2 GET endpoints:
- GET /api/admin/pipeline/overview
- GET /api/admin/pipeline/{targetType}/{targetId}

Endpoint count: 17 → 19 (9 POST + 10 GET)
