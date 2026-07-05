# FP-012 Task 10 — Admin Review Worklist, Target Context API & Console Integration

**Document Status:** Implemented in Task 10  
**Feature:** FP-012 — Admin Review Console & Human Approval Workflow Hardening  
**Date:** 2026-07-06

---

## 1. Scope

Safe admin review worklist query layer, target context display, and console integration.
Backend endpoints are read-only. Frontend renders worklist with filters and integrates ReviewConsole.

## 2. Backend Worklist Query API

### New Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | /api/admin/reviews/worklist | List worklist items with filters |
| GET | /api/admin/reviews/{targetType}/{targetId}/context | Get target context |

### Existing Endpoints (unchanged)

| Method | Path |
|--------|------|
| POST | /api/admin/reviews/submit |
| POST | /api/admin/reviews/start |
| POST | /api/admin/reviews/request-changes |
| POST | /api/admin/reviews/approve-internal |
| POST | /api/admin/reviews/reject |
| POST | /api/admin/reviews/return-to-draft |
| POST | /api/admin/reviews/note |
| GET | /api/admin/reviews/{targetType}/{targetId}/state |
| GET | /api/admin/reviews/{targetType}/{targetId}/audit |

**Total: 11 endpoints (7 POST + 4 GET)**

### Worklist Query Parameters

| Param | Type | Required |
|-------|------|----------|
| state | ReviewState | No |
| targetType | ReviewTargetType | No |
| reviewer | String | No |
| limit | int (default 50, max 200) | No |

## 3. Target Context Model

`AdminReviewTargetContext` — derived from `ReviewRecordEntity` only.

Safe fields:
- targetType, targetId, currentState
- displayTitle (type + short ID)
- displaySubtitle (target type name)
- safePreviewAvailable (always false until preview is implemented)
- safePreviewLabel
- createdAt, updatedAt

**No provider/storage/external lookup.**

## 4. Frontend Worklist UI

`ReviewWorklist.tsx` component:
- State filter dropdown
- Target type filter dropdown
- Worklist items with safe state badges
- Click to select → loads target context + ReviewConsole
- Safe error display
- Safe text rendering

## 5. Console Integration Flow

1. User opens worklist page
2. Filters by state/targetType if desired
3. Selects a worklist item
4. System loads target context
5. ReviewConsole renders for selected item
6. User performs review action via console
7. Worklist refreshes on action completion

## 6. Filtering and Pagination

- State filter: all 6 ReviewState values
- Target type filter: 4 ReviewTargetType values
- Limit: bounded 1–200, default 50
- No unbounded query
- No cursor-based pagination yet

## 7. Safe Rendering Rules

- No `dangerouslySetInnerHTML`
- No `.innerHTML`
- `safeText()` applied to all displayed text
- HTML tags stripped
- `javascript:` URIs stripped
- Text capped at 2000 chars
- No raw HTML from server

## 8. Safe Response/Error Model

- 404 for missing target context — safe message
- Empty list for no worklist results
- No stack trace in responses
- No sensitive field in error body

## 9. Forbidden Fields and Endpoints

**No endpoint for:** publish, export, complete, ready-for-publish, public-url, signed-url,
cdn, provider-url, storage-key, generic-status, config

**No DTO field for:** providerUrl, storageKey, bucketName, bucketPath, rawPayload,
rawProviderPayload, rawStoragePayload, publicUrl, signedUrl, secret, apiKey, config, stackTrace

## 10. Test Coverage

### Backend Tests

| File | Tests |
|------|-------|
| AdminReviewWorklistTest.java | Worklist happy path, state/targetType filters, empty results, context happy path, context 404, DTO forbidden fields, endpoint inventory (11), forbidden paths, limit behavior, safe error, boundary regression |
| AdminReviewApiBoundaryRegressionTest.java | Updated: 7 POST + 4 GET count |

### Frontend Tests

| File | Tests |
|------|-------|
| reviewApi.test.ts | Updated: 11 exports, getWorklist/getTargetContext presence |
| ReviewWorklist.test.ts | Safe rendering, no publish/export, no forbidden fields, console integration, filters, safeText, no generic status dropdown |

## 11. Acceptance Criteria

- [x] Worklist endpoint returns safe list
- [x] Worklist filters by state/targetType/reviewer
- [x] Limit bounded at 200
- [x] Target context endpoint returns safe context
- [x] Target context 404 safe
- [x] No forbidden DTO fields
- [x] Endpoint inventory = 11 (7 POST + 4 GET)
- [x] No forbidden endpoint paths
- [x] Frontend API client has getWorklist/getTargetContext
- [x] Frontend worklist renders safely
- [x] Frontend console integration works
- [x] No migration
- [x] All tests pass

## 12. Remaining Gaps

- Preview integration: safePreviewAvailable always false
- Reviewer filter in frontend: not exposed yet
- Pagination: simple limit only, no cursor/page
- Display title: derived from type+ID only, not from actual content title

## 13. Next Step

FP-012 closure task — final baseline and closure document.
