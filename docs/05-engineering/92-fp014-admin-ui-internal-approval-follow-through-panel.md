# FP-014 Task 04 — Admin UI Internal Approval Follow-through Panel

## 1. Task Status

| Field | Value |
|-------|-------|
| Task | Task 04 — Admin UI Internal Approval Follow-through Panel |
| Status | **COMPLETED** |
| Type | Frontend/admin only |
| Backend changed | No |
| Backend endpoints | 16 (unchanged) |
| Migrations | 0 |

## 2. Scope

Frontend-only task adding safe internal approval readiness UI to admin-web.
Read-only display of readiness information derived from FP-014 Task 02/03 backend.

## 3. Changed Files

### Frontend/Admin Production

| File | Status | Description |
|------|--------|-------------|
| `types/index.ts` | **MODIFIED** | +3 types: SafeInternalAction, InternalApprovalReadinessAvailability, SafeInternalApprovalReadiness |
| `api/reviewApi.ts` | **MODIFIED** | +2 API methods: getInternalReadiness, getApprovedInternalReadiness |
| `components/review/InternalApprovalReadinessPanel.tsx` | **NEW** | Readiness panel component |
| `components/review/ReviewWorklist.tsx` | **MODIFIED** | Integrated readiness panel |

### Frontend/Admin Tests

| File | Status | Description |
|------|--------|-------------|
| `components/review/__tests__/InternalApprovalReadinessPanel.test.ts` | **NEW** | 82 tests, 11 categories |
| `api/__tests__/reviewApi.test.ts` | **MODIFIED** | Updated export count 14→16, +2 function assertions |

### Backend Production
Not touched.

### Backend Tests
Not touched.

### Documentation
| File | Status |
|------|--------|
| `docs/05-engineering/92-fp014-admin-ui-internal-approval-follow-through-panel.md` | **NEW** |

## 4. Frontend Types Added

### SafeInternalAction
```
VIEW_SAFE_PREVIEW | REVIEW_APPROVAL_DETAILS | REQUEST_INTERNAL_REVISION |
PREPARE_FOR_FUTURE_EXPORT | MARK_AS_INTERNAL_READY_CANDIDATE
```

### InternalApprovalReadinessAvailability
```
READY | NOT_APPROVED | REJECTED | MISSING_TARGET |
UNSAFE_TARGET | PREVIEW_UNAVAILABLE | UNAVAILABLE
```

### SafeInternalApprovalReadiness
```typescript
{
  targetType, targetId, reviewState, availability,
  safeTitle, safeSummary, safeReasonCode, safeMessage,
  safePreviewAvailable, approvedAt, approvedByRole,
  safeNextInternalActions, safeWarnings, updatedAt
}
```

No forbidden fields: publicUrl, signedUrl, providerUrl, storageKey,
base64, rawPayload, bytes, secret, apiKey, stackTrace.

## 5. API Client Methods Added

| Method | HTTP | Path |
|--------|------|------|
| `getInternalReadiness(targetType, targetId)` | GET | `/admin/reviews/{targetType}/{targetId}/internal-readiness` |
| `getApprovedInternalReadiness(targetType?)` | GET | `/admin/reviews/approved/internal-readiness` |

API client export count: 14 → **16**.

## 6. Readiness Panel Behavior

| Availability | Display | Message |
|-------------|---------|---------|
| READY | ✅ Dahili Hazır | "Bu durum yayın/export anlamına gelmez — sadece dahili onay" |
| NOT_APPROVED | ⏳ Onay Bekliyor | "Manuel onay hâlâ gereklidir" |
| REJECTED | ❌ Reddedildi | "Bu öğe dahili olarak hazır değildir" |
| MISSING_TARGET | ⚠ Hedef Bulunamadı | Safe unavailable message |
| UNSAFE_TARGET | ⚠ Güvenli Değil | Safe unavailable message |
| PREVIEW_UNAVAILABLE | ⚠ Önizleme Kullanılamıyor | Safe unavailable message |
| UNAVAILABLE | ⚠ Kullanılamıyor | Safe unavailable message |

### Action Display

| Action | Label | Behavior |
|--------|-------|----------|
| VIEW_SAFE_PREVIEW | Güvenli Önizleme | Active |
| REVIEW_APPROVAL_DETAILS | Onay Detayları | Active |
| REQUEST_INTERNAL_REVISION | Dahili Revizyon İsteği (yakında) | Deferred/disabled |
| PREPARE_FOR_FUTURE_EXPORT | Gelecekte Export Hazırlığı — bilgilendirme | Informational only |
| MARK_AS_INTERNAL_READY_CANDIDATE | Dahili Hazır Adayı Olarak İşaretle | Deferred |

## 7. Integration Point in Admin UI

- **ReviewWorklist** integrates InternalApprovalReadinessPanel
- Panel appears between ReviewPreviewPanel and ReviewConsole
- Loads readiness when a worklist item is selected
- Error in readiness does not break ReviewConsole or ReviewPreviewPanel
- Existing review actions remain unchanged

## 8. Approved Readiness List Status

- API client method `getApprovedInternalReadiness` added and tested
- Full approved inventory UI deferred to a later task
- No new screen added

## 9. Safe Label Behavior

All labels in Turkish matching existing admin-web style:
- Dahili Hazır / Onay Bekliyor / Reddedildi / Hedef Bulunamadı
- Önizleme Kullanılamıyor / Güvenli Değil / Kullanılamıyor
- Dahili işlem önerileri / bilgilendirme amaçlıdır
- Bu durum yayın/export anlamına gelmez

## 10. Error/Loading Behavior

- Loading state: safe "Yükleniyor..." message
- API failure: safe error text via safeText()
- No stack trace, no exception class, no provider/storage/config detail
- Error in readiness panel does not break ReviewConsole or ReviewPreviewPanel

## 11. Safety Boundaries

- ❌ No publish/export/CDN/signed URL/auto-approval
- ❌ No provider/storage/key/URL/base64/bytes exposure
- ❌ No dangerouslySetInnerHTML, innerHTML, __html
- ❌ No img src from readiness response
- ❌ No raw HTML injection
- ❌ No POST request-internal-revision behavior
- ❌ No new backend endpoints
- ❌ No backend migrations/schema changes
- ✅ All text rendered via safeText()
- ✅ All deferred actions clearly marked as informational

## 12. Frontend Test Coverage

| Category | Tests |
|----------|-------|
| A. Safe Rendering | 5 |
| B. Availability State Rendering | 5 |
| C. Safe Action Taxonomy | 5 |
| D. Forbidden Values | 11 |
| E. IMAGE_ASSET Safety | 8 |
| F. Error/Loading Safety | 5 |
| G. Safe Labels | 6 |
| H. Data TestIDs | 13 |
| I. ReviewWorklist Integration | 6 |
| J. Types Safety | 15 |
| K. API Client Safety | 3 |
| **Total new tests** | **82** |

Plus 2 existing reviewApi.test.ts tests updated (export count 14→16, +2 function assertions).

**Total frontend tests: 303 passing.**

## 13. Backend Status

- Backend production not touched
- Backend tests not run (not required)
- Backend endpoint count: **16** (unchanged)

## 14. Endpoint Count Confirmation

| Metric | Value |
|--------|-------|
| Backend POST endpoints | 8 |
| Backend GET endpoints | 8 |
| Backend total | **16** |
| Frontend API client functions | **16** |

## 15. Migration/Schema Confirmation

- ❌ No migrations added
- ❌ No schema changes
- ❌ No new entities
- ❌ No runtime config changes

## 16. Deferred Items

| Item | Status |
|------|--------|
| Full approved readiness inventory UI | Deferred |
| POST request-internal-revision | Deferred |
| Pipeline readiness aggregation | Future |
| VisualAssetPlan readiness | Deferred |
| Readiness panel CSS styling | Future polish |

## 17. Recommendation for Task 05

**Task 05 — FP-014 Closure / Operational Baseline**

- Verify end-to-end integration
- Run full backend + frontend test suites
- Confirm endpoint count (16)
- Confirm migration count
- Document deferred items for FP-015
- Create final FP-014 closure document
