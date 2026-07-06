# FP-014 Task 03 — Internal Approval Readiness Admin API

## 1. Task Status

| Field | Value |
|-------|-------|
| Task | Task 03 — Internal Approval Readiness Admin API |
| Status | **COMPLETED** |
| Type | Backend API + tests |
| Endpoints Added | 2 GET |
| Endpoint Count Before | 14 (8 POST + 6 GET) |
| Endpoint Count After | **16 (8 POST + 8 GET)** |
| Migrations Added | 0 |
| Frontend Changed | No |

## 2. Scope

Backend API endpoints exposing FP-014 internal approval readiness through safe
admin review controller. Read-only, no state mutation, no publish/export.

## 3. Changed Files

### Backend Production

| File | Status | Description |
|------|--------|-------------|
| `AdminReviewController.java` | **MODIFIED** | +2 GET endpoints, +1 exception handler, +1 service injection |

### Backend Tests

| File | Status | Description |
|------|--------|-------------|
| `InternalApprovalReadinessAdminApiTest.java` | **NEW** | 25+ tests across 7 categories |
| `AdminReviewHandoffPreviewControllerTest.java` | **MODIFIED** | Endpoint count 14 → 16 |
| `FP013EndToEndReviewGatedWorkflowRegressionTest.java` | **MODIFIED** | Endpoint count 14 → 16 |
| `GenerationReviewHandoffAdapterTest.java` | **MODIFIED** | Endpoint count 14 → 16 |
| `InternalApprovalReadinessServiceTest.java` | **MODIFIED** | Endpoint count 14 → 16 |

### Frontend/Admin
Not touched.

### Documentation
| File | Status |
|------|--------|
| `91-fp014-internal-approval-readiness-admin-api.md` | **NEW** |

## 4. New Endpoints

| # | Method | Path | Purpose |
|---|--------|------|---------|
| 15 | GET | `/api/admin/reviews/{targetType}/{targetId}/internal-readiness` | Single target readiness |
| 16 | GET | `/api/admin/reviews/approved/internal-readiness` | Approved readiness list |

## 5. Endpoint Count Before / After

| Metric | Before (FP-013) | After (FP-014 Task 03) |
|--------|-----------------|----------------------|
| POST | 8 | 8 |
| GET | 6 | **8** |
| Total | 14 | **16** |

## 6. Request/Response Behavior

### GET /{targetType}/{targetId}/internal-readiness

**Request:** Path variables `targetType` (ReviewTargetType) and `targetId` (UUID).

**Response:** `ApiResponse<SafeInternalApprovalReadiness>` containing:
- targetType, targetId, reviewState
- availability (READY / NOT_APPROVED / REJECTED / MISSING_TARGET / UNAVAILABLE)
- safeTitle, safeSummary, safeReasonCode, safeMessage
- safePreviewAvailable
- approvedAt, approvedByRole (when approved)
- safeNextInternalActions (safe enum list)
- safeWarnings
- updatedAt

### GET /approved/internal-readiness

**Request:** Optional query parameter `targetType` for filtering.

**Response:** `ApiResponse<List<SafeInternalApprovalReadiness>>` containing
only APPROVED_FOR_INTERNAL_USE candidates.

## 7. Approved Readiness List Behavior

- Returns only records with `ReviewState.APPROVED_FOR_INTERNAL_USE`
- Filterable by target type
- Empty list returns safe empty response
- No DRAFT/AI_GENERATED/NEEDS_REVIEW/IN_REVIEW/CHANGES_REQUESTED/REJECTED items

## 8. Single Target Readiness Behavior

| Input State | Availability | Actions |
|-------------|-------------|---------|
| APPROVED_FOR_INTERNAL_USE | READY | All 5 internal actions |
| REJECTED | REJECTED | REVIEW_APPROVAL_DETAILS |
| DRAFT/AI_GENERATED/NEEDS_REVIEW/IN_REVIEW/CHANGES_REQUESTED | NOT_APPROVED | VIEW_SAFE_PREVIEW |
| Missing record | MISSING_TARGET | Empty |

## 9. Safe Action Taxonomy in API

**Included:** VIEW_SAFE_PREVIEW, REVIEW_APPROVAL_DETAILS, REQUEST_INTERNAL_REVISION,
PREPARE_FOR_FUTURE_EXPORT, MARK_AS_INTERNAL_READY_CANDIDATE

**Excluded:** PUBLISH, EXPORT, PUBLIC_DELIVERY, CREATE_PUBLIC_URL, CREATE_SIGNED_URL,
CDN_DELIVERY, USER_MEDIA_LIBRARY, READY_FOR_PUBLISH, PUBLISHED, EXPORTED, COMPLETED

## 10. Error/Failure Behavior

| Error | HTTP Status | Response |
|-------|-------------|----------|
| InternalApprovalReadinessException | 404 | Safe error message |
| IllegalArgumentException | 400 | Safe error message |
| Missing target | 200 | MISSING_TARGET availability in response |
| No stack trace | ✅ | Safe messages only |
| No provider detail | ✅ | No storage/provider/config in errors |

## 11. No Mutation / No Side Effects

- ✅ GET endpoints are read-only
- ✅ No state changes
- ✅ No audit record creation
- ✅ No external provider/storage calls
- ✅ No publish/export/URL/CDN creation
- ✅ Service methods are `@Transactional(readOnly = true)`
- ✅ Controller does not interact with ReviewApplicationService for readiness

## 12. Safety Boundaries

- ❌ No publish/export/CDN/signed URL/auto-approval
- ❌ No provider/storage/key/URL/base64/bytes exposure
- ❌ No new POST endpoints
- ❌ No new migrations
- ❌ No frontend changes
- ✅ All responses use SafeInternalApprovalReadiness DTO
- ✅ Safe exception handler added

## 13. Test Coverage

| Category | Tests |
|----------|-------|
| A. Single target readiness | 6 |
| B. Approved readiness list | 4 |
| C. Exception handling | 2 |
| D. Endpoint inventory | 3 |
| E. No mutation | 3 |
| F. Safe action taxonomy | 2 |
| G. Migration regression | 2 |

Plus 4 existing test files updated for endpoint count (14 → 16).

## 14. Migration/Schema Confirmation

- ❌ No migrations added
- ❌ No schema changes
- ❌ No new entities
- ❌ No JPA annotations on new DTOs

## 15. Frontend/Admin Status

- Not touched in Task 03
- Frontend tests not run

## 16. Deferred Items

| Item | Status |
|------|--------|
| Admin UI readiness panel | Task 04 |
| POST request-internal-revision | Deferred |
| Pipeline readiness aggregation | Future |
| VisualAssetPlan readiness | Deferred |

## 17. Recommendation for Task 04

**Task 04 — Admin UI Internal Approval Follow-through Panel**

- Add admin-web readiness integration
- Approved detail panel, readiness badge, next-action hints
- Calls new readiness endpoints
- Safe rendering — no URL/base64/HTML injection
