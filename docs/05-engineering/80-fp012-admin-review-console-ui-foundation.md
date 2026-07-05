# FP-012 Task 09 — Admin Review Console UI Foundation

**Document Status:** Implemented in Task 09  
**Feature:** FP-012 — Admin Review Console & Human Approval Workflow Hardening  
**Date:** 2026-07-06

---

## 1. Scope

Initial admin review console UI foundation with safe API client, review state display,
action panel, audit timeline, and safe rendering guards.

## 2. Frontend/Admin Module Detection

- **Module:** `engineering/apps/admin-web` (React + Vite + TypeScript)
- **Test Framework:** Vitest + Testing Library
- **Existing Pattern:** Explicit action API clients, typed DTOs, component-per-domain

## 3. Implemented UI Foundation

| Component | File |
|-----------|------|
| Review types | `src/types/index.ts` (appended) |
| API client | `src/api/reviewApi.ts` |
| Review console component | `src/components/review/ReviewConsole.tsx` |
| API client tests | `src/api/__tests__/reviewApi.test.ts` |
| Component safety tests | `src/components/review/__tests__/ReviewConsole.test.ts` |

## 4. API Client Contract

9 explicit methods matching Task 07 backend endpoints:

| Method | Endpoint |
|--------|----------|
| `submitForReview` | POST /api/admin/reviews/submit |
| `startReview` | POST /api/admin/reviews/start |
| `requestChanges` | POST /api/admin/reviews/request-changes |
| `approveForInternalUse` | POST /api/admin/reviews/approve-internal |
| `rejectReview` | POST /api/admin/reviews/reject |
| `returnToDraft` | POST /api/admin/reviews/return-to-draft |
| `addReviewNote` | POST /api/admin/reviews/note |
| `getReviewState` | GET /api/admin/reviews/{targetType}/{targetId}/state |
| `getAuditTimeline` | GET /api/admin/reviews/{targetType}/{targetId}/audit |

**No generic status update method.**  
**No publish/export/public URL/signed URL/CDN/provider/storage method.**

## 5. Review State Summary UI

- Displays current state with safe badge
- APPROVED_FOR_INTERNAL_USE shown as "Dahili Onay (Sadece İç Kullanım)"
- No publish/export readiness display
- Last action type and timestamp shown

## 6. Review Action Panel

Supported actions by state:

| State | Actions |
|-------|---------|
| DRAFT | Submit for Review |
| NEEDS_REVIEW | Start Review |
| IN_REVIEW | Approve Internal, Reject, Request Changes, Add Note |
| CHANGES_REQUESTED | Return to Draft, Add Note |
| APPROVED_FOR_INTERNAL_USE | Add Note |
| REJECTED | Return to Draft, Add Note |

**No publish/export/complete/ready-for-publish/generic-status buttons.**  
**CREATOR/SYSTEM cannot see approve button.**

## 7. Audit Timeline UI

Displayed safe fields only:
- Event type label
- From → To state
- Actor role
- Occurred at
- Safe reason code
- Sanitized note preview

**No provider URL, storage key, raw payload, base64, secret, public/signed URL, stack trace.**

## 8. Safe Rendering Rules

- No `dangerouslySetInnerHTML` or equivalent
- No `.innerHTML` assignment
- All user-facing text passed through `safeText()` helper
- HTML tags stripped
- `javascript:` URIs stripped
- Event handler injection patterns stripped
- Text length limited to 2000 characters

## 9. Safe Error Handling

- Errors displayed as safe text only
- No stack trace display
- No raw exception detail
- No provider/storage/config values
- No secrets

## 10. Forbidden UI Actions

The following must NEVER appear:
- Publish button
- Export button
- Complete button
- Ready for Publish button
- Generic status dropdown
- Arbitrary status text input
- Public URL display
- Signed URL display
- Provider URL display
- Storage key display

## 11. Security/Privacy Boundaries

- actorRole is workflow input, not authentication
- CREATOR/SYSTEM approve button hidden in UI
- Backend enforces transition policy regardless of UI
- No provider/storage/secret data in any response or display

## 12. Test Coverage

| Test Group | File |
|------------|------|
| API client functions | reviewApi.test.ts |
| API client forbidden functions | reviewApi.test.ts |
| Component safe rendering | ReviewConsole.test.ts |
| Component forbidden patterns | ReviewConsole.test.ts |
| Internal-only wording | ReviewConsole.test.ts |
| CREATOR/SYSTEM approve hiding | ReviewConsole.test.ts |

## 13. Acceptance Criteria

- [x] API client with 9 explicit methods
- [x] No generic status update method
- [x] No publish/export/URL/key methods
- [x] Review console component with state, actions, timeline
- [x] Safe rendering — no dangerouslySetInnerHTML
- [x] APPROVED_FOR_INTERNAL_USE shown as internal-only
- [x] CREATOR/SYSTEM cannot see approve button
- [x] No forbidden field display
- [x] Comprehensive tests
- [x] All tests passing

## 14. Next Step

FP-012 closure task should confirm final test baseline and close the feature.
