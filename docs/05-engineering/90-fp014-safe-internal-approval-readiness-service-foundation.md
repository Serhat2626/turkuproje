# FP-014 Task 02 — Safe Internal Approval Readiness Service Foundation

## 1. Task Status

| Field | Value |
|-------|-------|
| Task | Task 02 — Safe Internal Approval Readiness Service + DTO Foundation |
| Status | **COMPLETED** |
| Type | Backend service/DTO/test |
| Endpoints Added | 0 |
| Migrations Added | 0 |
| Frontend Changed | No |

## 2. Scope

Backend-only foundation for internal approval readiness:
- Safe DTO for derived readiness views
- Safe enum for internal-only action hints
- Safe availability enum
- Safe exception
- Read-only service deriving readiness from existing review records
- Comprehensive tests

## 3. Changed Files

### Backend Production

| File | Status | Description |
|------|--------|-------------|
| `SafeInternalAction.java` | **NEW** | Internal-only next action hints enum (5 values) |
| `InternalApprovalReadinessAvailability.java` | **NEW** | Readiness availability enum (7 values) |
| `SafeInternalApprovalReadiness.java` | **NEW** | Immutable readiness DTO with factory methods |
| `InternalApprovalReadinessException.java` | **NEW** | Safe exception with error code |
| `InternalApprovalReadinessService.java` | **NEW** | Read-only service deriving readiness |

### Backend Tests

| File | Status | Description |
|------|--------|-------------|
| `InternalApprovalReadinessServiceTest.java` | **NEW** | 35+ tests across 11 categories |

### Frontend/Admin
Not touched.

### Documentation
| File | Status |
|------|--------|
| `90-fp014-safe-internal-approval-readiness-service-foundation.md` | **NEW** |

## 4. DTOs/Enums/Services Added

### SafeInternalAction (5 values)
- `VIEW_SAFE_PREVIEW` — Read-only preview
- `REVIEW_APPROVAL_DETAILS` — Read-only audit
- `REQUEST_INTERNAL_REVISION` — State change (re-review)
- `PREPARE_FOR_FUTURE_EXPORT` — Informational only, non-executing
- `MARK_AS_INTERNAL_READY_CANDIDATE` — Metadata-only marker

### InternalApprovalReadinessAvailability (7 values)
- `READY` — Internally approved
- `NOT_APPROVED` — Review not complete
- `REJECTED` — Rejected by reviewer
- `MISSING_TARGET` — Record not found
- `UNSAFE_TARGET` — Not safe for evaluation
- `PREVIEW_UNAVAILABLE` — Preview not available
- `UNAVAILABLE` — Generic unavailable

### SafeInternalApprovalReadiness DTO
- Immutable, `@Getter @Builder`
- Factory method: `unavailable()` for missing/error cases
- Safe `toString()` — no forbidden fields
- Fields: targetType, targetId, reviewState, availability, safeTitle, safeSummary,
  safeReasonCode, safeMessage, safePreviewAvailable, approvedAt, approvedByRole,
  safeNextInternalActions, safeWarnings, updatedAt

### InternalApprovalReadinessException
- Safe error code + message
- No stack trace/class/provider/storage detail in toString

### InternalApprovalReadinessService
- `@Service` with `@Transactional(readOnly = true)`
- `getReadiness(targetType, targetId)` — single target readiness
- `getApprovedReadinessList(targetType)` — list approved by type or all

## 5. Readiness Behavior by Review State

| State | Availability | Actions | Message |
|-------|-------------|---------|---------|
| `APPROVED_FOR_INTERNAL_USE` | READY | All 5 internal actions | Internal-only readiness |
| `REJECTED` | REJECTED | REVIEW_APPROVAL_DETAILS only | Rejected, not ready |
| `DRAFT` | NOT_APPROVED | VIEW_SAFE_PREVIEW only | Manual approval required |
| `AI_GENERATED` | NOT_APPROVED | VIEW_SAFE_PREVIEW only | Human review required |
| `NEEDS_REVIEW` | NOT_APPROVED | VIEW_SAFE_PREVIEW only | Manual approval required |
| `IN_REVIEW` | NOT_APPROVED | VIEW_SAFE_PREVIEW only | Manual approval required |
| `CHANGES_REQUESTED` | NOT_APPROVED | VIEW_SAFE_PREVIEW only | Revision required |
| Missing record | MISSING_TARGET | Empty | Record not found |
| Null targetType | UNAVAILABLE | Empty | Target type not specified |
| Null targetId | MISSING_TARGET | Empty | Target ID not specified |

## 6. Target Type Coverage

| Target Type | Safe Title Prefix | Metadata-only Warning |
|-------------|------------------|-----------------------|
| CONTENT_PROJECT | Proje | — |
| SCENARIO | Senaryo | — |
| STORYBOARD | Storyboard | — |
| PACKAGE | Paket | — |
| IMAGE_ASSET | Görsel | ✅ "Görsel varlık önizlemesi yalnızca meta veri içerir" |

## 7. Safe Internal Action Taxonomy Confirmation

| Value | Present | Forbidden |
|-------|---------|-----------|
| VIEW_SAFE_PREVIEW | ✅ | |
| REVIEW_APPROVAL_DETAILS | ✅ | |
| REQUEST_INTERNAL_REVISION | ✅ | |
| PREPARE_FOR_FUTURE_EXPORT | ✅ (non-executing) | |
| MARK_AS_INTERNAL_READY_CANDIDATE | ✅ (metadata-only) | |
| PUBLISH | | ❌ Does not exist |
| EXPORT | | ❌ Does not exist |
| PUBLIC_DELIVERY | | ❌ Does not exist |
| CREATE_PUBLIC_URL | | ❌ Does not exist |
| CREATE_SIGNED_URL | | ❌ Does not exist |
| CDN_DELIVERY | | ❌ Does not exist |
| USER_MEDIA_LIBRARY | | ❌ Does not exist |

## 8. Preview Integration Behavior

- Service calls `AdminPreviewService.getPreview()` to check preview availability
- Preview failure is non-fatal — `safePreviewAvailable` is set to `false`
- IMAGE_ASSET preview returns metadata/availability only — no bytes/URL/base64
- No storage/provider calls from readiness service

## 9. Failure Behavior

| Scenario | Behavior |
|----------|----------|
| Missing review record | Safe unavailable result, MISSING_TARGET |
| Null targetType | Safe unavailable result, UNAVAILABLE |
| Null targetId | Safe unavailable result, MISSING_TARGET |
| Preview exception | Non-fatal, previewAvailable=false |
| Storage/provider failure | Not applicable — no storage/provider calls |

## 10. Safety Boundaries

- ❌ No publish/export/CDN/signed URL/auto-approval
- ❌ No provider/storage/key/URL/base64/bytes exposure
- ❌ No state mutation or write side effects
- ❌ No new API endpoints
- ❌ No new migrations
- ❌ No frontend changes
- ✅ All text sanitized via ReviewTextSafety
- ✅ All @Transactional methods are readOnly=true
- ✅ Safe toString on all classes
- ✅ No forbidden fields in DTO

## 11. Test Coverage

| Category | Tests |
|----------|-------|
| Approved readiness | 7 |
| Non-approved states | 5 |
| Rejected state | 3 |
| Missing/unavailable target | 4 |
| Target type coverage | 5 |
| Preview safety integration | 3 |
| Safe DTO serialization | 3 |
| Enum safety | 4 |
| Endpoint/migration regression | 3 |
| No write side effects | 3 |
| Approved readiness list | 2 |

## 12. Endpoint/Migration Confirmation

- Admin review endpoint count: **14** (unchanged)
- Migrations: **0** added
- Schema changes: **0**
- Runtime config changes: **0**

## 13. Deferred Items

| Item | Status |
|------|--------|
| Internal readiness API endpoints | Task 03 |
| Admin UI readiness panel | Task 04 |
| Pipeline completeness aggregation | Future |
| REQUEST_INTERNAL_REVISION execution | Task 03 (candidate) |
| VisualAssetPlan readiness | Deferred (no ReviewTargetType) |
| Production auth integration | Deferred |

## 14. Recommendation for Task 03

**Task 03 — Internal Approval Readiness Admin API**

Add safe GET endpoints for readiness queries:
- `GET /api/admin/reviews/approved/internal-readiness`
- `GET /api/admin/reviews/{targetType}/{targetId}/internal-readiness`

Optional POST for revision request:
- `POST /api/admin/reviews/{targetType}/{targetId}/request-internal-revision`

Expected endpoint delta: +2 or +3 (14 → 16 or 17).
